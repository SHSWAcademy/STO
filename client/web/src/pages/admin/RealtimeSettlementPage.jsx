import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import api from "../../lib/api.js";
import { API_BASE_URL } from "../../lib/config.js";

const EMPTY_SETTLEMENT = {
  totalTx: 0,
  pendingCount: 0,
  successCount: 0,
  totalAmount: 0,
  tokenStatsList: [],
};

const WS_ENDPOINT = `${API_BASE_URL}/ws/admin/trade`;
const WS_SUBSCRIPTION = "/topic/admin/trade/flow";

const TOKEN_COLORS = [
  "#7eb8f7",
  "#c9a84c",
  "#6ad98d",
  "#f2a0d0",
  "#a0d4f2",
  "#f2c9a0",
  "#b0f2a0",
  "#d4a0f2",
  "#f2f0a0",
  "#f2a0a0",
];

const EVENT_COLORS = [
  "#7eb8f7",
  "#f2c94c",
  "#6ad98d",
  "#f58cc8",
  "#8fc7ff",
  "#f39b6b",
  "#97e3b0",
  "#c7a6ff",
  "#ffd36e",
  "#ff8f8f",
  "#76e1d4",
  "#a9b7ff",
];

const STAGE_META = {
  PENDING: {
    label: "PENDING",
    color: "#f2c94c",
    badge: "bg-[#f2c94c]/15 text-[#f2c94c]",
  },
  OUTBOX_PROCESSING: {
    label: "OUTBOX",
    color: "#c9a84c",
    badge: "bg-[#c9a84c]/15 text-[#c9a84c]",
  },
  SUCCESS: {
    label: "SUCCESS",
    color: "#6ad98d",
    badge: "bg-[#6ad98d]/15 text-[#6ad98d]",
  },
  FAILED: {
    label: "FAILED",
    color: "#f2a0a0",
    badge: "bg-[#b85450]/20 text-[#f2a0a0]",
  },
};

const SIGNAL_PATHS = {
  PENDING: ["ob", "match"],
  OUTBOX_PROCESSING: ["ob", "match", "outbox"],
  SUCCESS: ["outbox", "erc20"],
  FAILED: ["ob", "match", "outbox"],
};

const NODE_POPUPS = {
  ob: {
    title: "주문",
    sub: "Order",
    rows: [
      ["역할", "주문 접수"],
      ["이벤트", "PENDING"],
    ],
  },
  match: {
    title: "체결",
    sub: "Matching",
    rows: [
      ["방식", "가격-시간 우선 체결"],
      ["표시값", "pendingCount"],
    ],
  },
  outbox: {
    title: "DB Queue",
    sub: "Transactional Queue",
    rows: [
      ["이벤트", "OUTBOX_PROCESSING"],
      ["처리", "BlockchainWorkerService"],
    ],
  },
  erc20: {
    title: "Blockchain",
    sub: "On-Chain",
    rows: [
      ["이벤트", "SUCCESS / FAILED"],
      ["표시값", "successCount"],
    ],
  },
};

function toNumber(value) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function normalizeSettlementPayload(payload) {
  const body = payload?.data ?? payload?.settlement ?? payload ?? {};
  const tokenStatsList = Array.isArray(body.tokenStatsList)
    ? body.tokenStatsList
    : [];

  return {
    totalTx: toNumber(body.totalTx),
    pendingCount: toNumber(body.pendingCount),
    successCount: toNumber(body.successCount),
    totalAmount: toNumber(body.totalAmount),
    tokenStatsList: tokenStatsList.map((item) => ({
      tokenId: item.tokenId ?? null,
      tokenSymbol: item.tokenSymbol ?? "-",
      count: toNumber(item.count),
      pending: toNumber(item.pending),
      amount: toNumber(item.amount),
    })),
  };
}

function normalizeFlowEvent(payload) {
  const body = payload?.data ?? payload?.event ?? payload ?? {};

  return {
    stage: String(body.stage ?? "").toUpperCase(),
    tradeId: body.tradeId ?? null,
    tokenId: body.tokenId ?? null,
    tokenSymbol: body.tokenSymbol ?? "-",
    amount: toNumber(body.amount),
    qty: toNumber(body.qty),
    buyerName: body.buyerName ?? body.buyerAddr ?? null,
    sellerName: body.sellerName ?? body.sellerAddr ?? null,
    receivedAt: new Date().toISOString(),
  };
}

function getTokenKey(token) {
  return token.tokenSymbol || String(token.tokenId ?? "");
}

function getFlowKey(event) {
  return event.tokenSymbol || String(event.tokenId ?? "");
}

function formatNumber(value) {
  return toNumber(value).toLocaleString();
}

function formatWon(value) {
  return `${formatNumber(value)}원`;
}

function getEventColorSeed(event) {
  return String(event.tradeId ?? event.tokenId ?? event.tokenSymbol ?? event.receivedAt ?? "0");
}

function getHashedEventColor(event) {
  const seed = getEventColorSeed(event);
  let hash = 0;

  for (let index = 0; index < seed.length; index += 1) {
    hash = (hash * 31 + seed.charCodeAt(index)) >>> 0;
  }

  return EVENT_COLORS[hash % EVENT_COLORS.length];
}

function buildDisplayTokens(tokenStatsList) {
  return tokenStatsList.map((token, index) => ({
    ...token,
    key: getTokenKey(token),
    color: TOKEN_COLORS[index % TOKEN_COLORS.length],
  }));
}

function upsertTokenStat(list, event, updater) {
  const key = getFlowKey(event);
  let found = false;
  const next = list.map((token) => {
    const matches =
      getTokenKey(token) === key ||
      (event.tokenId !== null && String(token.tokenId) === String(event.tokenId));

    if (!matches) return token;
    found = true;
    return updater(token);
  });

  if (found) return next;

  return [
    ...next,
    updater({
      tokenId: event.tokenId,
      tokenSymbol: event.tokenSymbol || "-",
      count: 0,
      pending: 0,
      amount: 0,
    }),
  ];
}

export function RealtimeSettlementPage() {
  const canvasRef = useRef(null);
  const frameRef = useRef(null);
  const clientRef = useRef(null);
  const tradeStagesRef = useRef(new Map());
  const latestRef = useRef({
    activeFilter: "ALL",
    displayTokens: [],
    settlement: EMPTY_SETTLEMENT,
  });
  const stateRef = useRef({
    width: 0,
    height: 0,
    last: 0,
    pulse: 0,
    signals: [],
    glows: { ob: 0, match: 0, outbox: 0, erc20: 0 },
    blockFlashes: [],
    queueBlocks: [],
  });

  const [settlement, setSettlement] = useState(EMPTY_SETTLEMENT);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError, setStatsError] = useState("");
  const [socketConnected, setSocketConnected] = useState(false);
  const [socketStatus, setSocketStatus] = useState("DISCONNECTED");
  const [socketError, setSocketError] = useState("");
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [flowEvents, setFlowEvents] = useState([]);
  const [popup, setPopup] = useState(null);

  const displayTokens = useMemo(
    () => buildDisplayTokens(settlement.tokenStatsList),
    [settlement.tokenStatsList],
  );

  const activeToken = useMemo(() => {
    if (activeFilter === "ALL") return null;
    return displayTokens.find((token) => token.key === activeFilter) ?? null;
  }, [activeFilter, displayTokens]);

  const visibleFlowEvents = useMemo(() => {
    if (activeFilter === "ALL") return flowEvents;
    return flowEvents.filter((event) => getFlowKey(event) === activeFilter);
  }, [activeFilter, flowEvents]);

  const kpi = activeToken
    ? {
        totalTx: activeToken.count,
        pendingCount: activeToken.pending,
        successCount: Math.max(0, activeToken.count - activeToken.pending),
        totalAmount: activeToken.amount,
      }
    : settlement;

  useEffect(() => {
    latestRef.current = { activeFilter, displayTokens, settlement };
  }, [activeFilter, displayTokens, settlement]);

  useEffect(() => {
    if (activeFilter === "ALL") return;
    const exists = displayTokens.some((token) => token.key === activeFilter);
    if (!exists) setActiveFilter("ALL");
  }, [activeFilter, displayTokens]);

  function getFlowColor(event) {
    return getHashedEventColor(event) ?? STAGE_META[event.stage]?.color ?? "#7eb8f7";
  }

  function getNodePosition(key) {
    const width = stateRef.current.width;
    const height = stateRef.current.height;

    return {
      ob: { x: width * 0.14, y: height * 0.48 },
      match: { x: width * 0.34, y: height * 0.48 },
      outbox: { x: width * 0.52, y: height * 0.48 },
      erc20: { x: width * 0.8, y: height * 0.48 },
    }[key];
  }

  function getNodeX(key) {
    return getNodePosition(key).x;
  }

  function getNodeY(key = "match") {
    return getNodePosition(key).y;
  }

  function enqueueSignal(pathKeys, event, color) {
    if (!Array.isArray(pathKeys) || pathKeys.length < 2) return;

    const state = stateRef.current;
    const yOffset = (toNumber(event.tradeId) % 3 - 1) * 12;

    if (event.tradeId !== null) {
      state.signals = state.signals.filter((signal) => signal.tradeId !== event.tradeId);
    }

    state.signals.push({
      tradeId: event.tradeId,
      stage: event.stage,
      event,
      queueDeposited: false,
      path: pathKeys.map((key) => ({
        x: getNodeX(key),
        y: getNodeY(key) + yOffset,
      })),
      color,
      label: event.tokenSymbol || event.stage,
      tokenKey: getFlowKey(event),
      t: 0,
      duration:
        event.stage === "SUCCESS"
          ? 176
          : event.stage === "FAILED"
            ? 150
            : 132,
    });
  }

  function upsertQueueBlock(event, color) {
    if (event.tradeId === null) return;

    const state = stateRef.current;
    const existingIndex = state.queueBlocks.findIndex((item) => item.tradeId === event.tradeId);
    const nextBlock = {
      tradeId: event.tradeId,
      tokenKey: getFlowKey(event),
      tokenSymbol: String(event.tokenSymbol || "?").slice(0, 6),
      qty: event.qty,
      color,
    };

    if (existingIndex >= 0) {
      state.queueBlocks[existingIndex] = nextBlock;
      return;
    }

    state.queueBlocks.push(nextBlock);
    state.queueBlocks = state.queueBlocks.slice(-10);
  }

  function removeQueueBlock(event) {
    if (event.tradeId === null) return;
    stateRef.current.queueBlocks = stateRef.current.queueBlocks.filter(
      (item) => item.tradeId !== event.tradeId,
    );
  }

  function enqueueFlowAnimation(event) {
    const color = getFlowColor(event);
    const state = stateRef.current;
    const path = SIGNAL_PATHS[event.stage];

    if (path) {
      enqueueSignal(path, event, color);
    }

    if (event.stage === "PENDING") {
      state.glows.ob = 1;
      state.glows.match = 1;
      return;
    }

    if (event.stage === "OUTBOX_PROCESSING") {
      state.glows.ob = 0.65;
      state.glows.match = 1;
      state.glows.outbox = 1;
      return;
    }

    if (event.stage === "SUCCESS") {
      state.glows.ob = 0.55;
      state.glows.match = 0.72;
      state.glows.outbox = 0.9;
      state.glows.erc20 = 1;
      removeQueueBlock(event);
      state.blockFlashes.push({
        x: getNodeX("outbox"),
        y: getNodeY("outbox") + 74,
        r: 0,
        alpha: 0.9,
        color: "#d9b25a",
      });
      return;
    }

    if (event.stage === "FAILED") {
      state.glows.ob = 0.45;
      state.glows.match = 0.6;
      state.glows.outbox = 1;
      removeQueueBlock(event);
      state.blockFlashes.push({
        x: getNodeX("outbox"),
        y: getNodeY("outbox"),
        r: 0,
        alpha: 0.9,
        color: "#f2a0a0",
      });
    }
  }

  function applyFlowToStats(event) {
    const previousStage = tradeStagesRef.current.get(event.tradeId);

    setSettlement((prev) => {
      if (event.stage === "PENDING" && previousStage !== "PENDING") {
        return {
          ...prev,
          totalTx: prev.totalTx + 1,
          pendingCount: prev.pendingCount + 1,
          totalAmount: prev.totalAmount + event.amount,
          tokenStatsList: upsertTokenStat(prev.tokenStatsList, event, (token) => ({
            ...token,
            tokenId: token.tokenId ?? event.tokenId,
            tokenSymbol: token.tokenSymbol || event.tokenSymbol,
            count: token.count + 1,
            pending: token.pending + 1,
            amount: token.amount + event.amount,
          })),
        };
      }

      if (event.stage === "SUCCESS" && previousStage !== "SUCCESS") {
        return {
          ...prev,
          pendingCount: Math.max(0, prev.pendingCount - 1),
          successCount: prev.successCount + 1,
          tokenStatsList: upsertTokenStat(prev.tokenStatsList, event, (token) => ({
            ...token,
            tokenId: token.tokenId ?? event.tokenId,
            tokenSymbol: token.tokenSymbol || event.tokenSymbol,
            pending: Math.max(0, token.pending - 1),
          })),
        };
      }

      if (event.stage === "FAILED" && previousStage !== "FAILED") {
        return {
          ...prev,
          pendingCount: Math.max(0, prev.pendingCount - 1),
          tokenStatsList: upsertTokenStat(prev.tokenStatsList, event, (token) => ({
            ...token,
            tokenId: token.tokenId ?? event.tokenId,
            tokenSymbol: token.tokenSymbol || event.tokenSymbol,
            pending: Math.max(0, token.pending - 1),
          })),
        };
      }

      return prev;
    });

    if (event.tradeId !== null) {
      tradeStagesRef.current.set(event.tradeId, event.stage);
    }
  }

  const handleFlowEvent = useCallback((payload) => {
    const event = normalizeFlowEvent(payload);
    if (!event.stage || !STAGE_META[event.stage]) return;

    applyFlowToStats(event);
    enqueueFlowAnimation(event);
    setFlowEvents((prev) => {
      const now = Date.now();
      const filtered =
        event.tradeId === null
          ? prev
          : prev.filter(
              (item) =>
                item.tradeId !== event.tradeId &&
                !(item.stage === "SUCCESS" && Date.parse(item.receivedAt) + 10000 <= now),
            );
      return [event, ...filtered].slice(0, 80);
    });
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => {
      const now = Date.now();
      setFlowEvents((prev) =>
        prev.filter(
          (event) => !(event.stage === "SUCCESS" && Date.parse(event.receivedAt) + 10000 <= now),
        ),
      );
    }, 1000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    let mounted = true;

    async function loadSettlementStats() {
      setStatsLoading(true);
      setStatsError("");

      try {
        const { data } = await api.get("/admin/trade/stats");
        if (!mounted) return;
        setSettlement(normalizeSettlementPayload(data));
      } catch (error) {
        console.error("[RealtimeSettlementPage] settlement stats load failed:", error);
        if (!mounted) return;
        setSettlement(EMPTY_SETTLEMENT);
        setStatsError("정산 통계 데이터를 불러오지 못했습니다.");
      } finally {
        if (mounted) setStatsLoading(false);
      }
    }

    loadSettlementStats();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const authHeader = token ? { Authorization: `Bearer ${token}` } : {};
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_ENDPOINT),
      connectHeaders: authHeader,
      reconnectDelay: 5000,
      debug: (message) => {
        if (import.meta.env.DEV) {
          console.debug("[RealtimeSettlementWS]", message);
        }
      },
      beforeConnect: () => {
        setSocketStatus("CONNECTING");
        setSocketError("");
        if (import.meta.env.DEV) {
          console.debug("[RealtimeSettlementWS] connecting:", WS_ENDPOINT);
        }
      },
      onConnect: () => {
        setSocketConnected(true);
        setSocketStatus("CONNECTED");
        setSocketError("");
        if (import.meta.env.DEV) {
          console.debug("[RealtimeSettlementWS] connected, subscribing:", WS_SUBSCRIPTION);
        }
        client.subscribe(
          WS_SUBSCRIPTION,
          (message) => {
            try {
              handleFlowEvent(JSON.parse(message.body));
            } catch (error) {
              console.warn("[RealtimeSettlementWS] flow parse failed:", error);
            }
          },
          authHeader,
        );
      },
      onDisconnect: () => {
        setSocketConnected(false);
        setSocketStatus("DISCONNECTED");
      },
      onWebSocketClose: () => {
        setSocketConnected(false);
        setSocketStatus("DISCONNECTED");
      },
      onWebSocketError: (error) => {
        setSocketConnected(false);
        setSocketStatus("ERROR");
        setSocketError("웹소켓 엔드포인트에 연결하지 못했습니다.");
        console.warn("[RealtimeSettlementWS] websocket error:", error);
      },
      onStompError: (frame) => {
        setSocketStatus("ERROR");
        setSocketError(frame.headers?.message ?? "웹소켓 연결 오류가 발생했습니다.");
      },
    });

    setSocketStatus("CONNECTING");
    if (import.meta.env.DEV) {
      console.debug("[RealtimeSettlementWS] activate:", {
        endpoint: WS_ENDPOINT,
        subscription: WS_SUBSCRIPTION,
        hasToken: Boolean(token),
      });
    }
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [handleFlowEvent]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;

    const ctx = canvas.getContext("2d");

    function resize() {
      const ratio = window.devicePixelRatio || 1;
      stateRef.current.width = canvas.clientWidth;
      stateRef.current.height = canvas.clientHeight;
      canvas.width = canvas.clientWidth * ratio;
      canvas.height = canvas.clientHeight * ratio;
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    }

    function drawLayer(x, y, w, h, fill, color, label) {
      ctx.fillStyle = fill;
      ctx.fillRect(x, y, w, h);
      ctx.fillStyle = color;
      ctx.globalAlpha = 0.52;
      ctx.font = "900 18px Noto Sans KR, sans-serif";
      ctx.textAlign = "left";
      ctx.fillText(label, x + 26, y + 34);
      ctx.globalAlpha = 1;
    }

    function drawLayers() {
      const { width, height } = stateRef.current;
      drawLayer(
        width * 0.04,
        height * 0.16,
        width * 0.56,
        height * 0.64,
        "rgba(74,114,160,.12)",
        "#9bc4ff",
        "OFF-CHAIN",
      );
      drawLayer(
        width * 0.66,
        height * 0.16,
        width * 0.24,
        height * 0.64,
        "rgba(42,96,64,.14)",
        "#7ff0b0",
        "ON-CHAIN",
      );

      ctx.strokeStyle = "rgba(255,255,255,.08)";
      ctx.setLineDash([8, 10]);
      ctx.beginPath();
      ctx.moveTo(width * 0.63, height * 0.17);
      ctx.lineTo(width * 0.63, height * 0.8);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    function drawArrow(x1, y1, x2, y2, color, opacity) {
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.strokeStyle = color;
      ctx.globalAlpha = opacity;
      ctx.lineWidth = 1;
      ctx.setLineDash([4, 4]);
      ctx.stroke();
      ctx.setLineDash([]);

      const angle = Math.atan2(y2 - y1, x2 - x1);
      ctx.beginPath();
      ctx.moveTo(x2, y2);
      ctx.lineTo(x2 - 7 * Math.cos(angle - 0.4), y2 - 7 * Math.sin(angle - 0.4));
      ctx.lineTo(x2 - 7 * Math.cos(angle + 0.4), y2 - 7 * Math.sin(angle + 0.4));
      ctx.closePath();
      ctx.fillStyle = color;
      ctx.fill();
      ctx.globalAlpha = 1;
    }

    function drawNode(x, y, label, sub, baseColor, glowColor, glowPower) {
      if (glowPower > 0.02) {
        const gradient = ctx.createRadialGradient(x, y, 12, x, y, 86);
        gradient.addColorStop(0, `${glowColor}${Math.round(glowPower * 85).toString(16).padStart(2, "0")}`);
        gradient.addColorStop(1, "transparent");
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(x, y, 86, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.beginPath();
      ctx.roundRect(x - 72, y - 42, 144, 84, 10);
      ctx.fillStyle = `${baseColor}22`;
      ctx.fill();
      ctx.strokeStyle = `${glowColor}${glowPower > 0.12 ? "aa" : "55"}`;
      ctx.lineWidth = glowPower > 0.12 ? 1.8 : 1;
      ctx.stroke();

      ctx.fillStyle = "rgba(255,255,255,.9)";
      ctx.font = "700 14px Noto Sans KR, sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(label, x, y - 8);
      ctx.fillStyle = "rgba(255,255,255,.32)";
      ctx.font = "400 10px Noto Sans KR, sans-serif";
      ctx.fillText(sub, x, y + 13);
    }

    function drawMetricPill(x, y, value, color) {
      ctx.font = "700 10px Noto Sans KR, sans-serif";
      const text = formatNumber(value);
      const textWidth = ctx.measureText(text).width;
      ctx.beginPath();
      ctx.roundRect(x - textWidth / 2 - 10, y, textWidth + 20, 22, 7);
      ctx.fillStyle = "rgba(8,10,16,.78)";
      ctx.fill();
      ctx.strokeStyle = `${color}55`;
      ctx.stroke();
      ctx.fillStyle = color;
      ctx.textAlign = "center";
      ctx.fillText(text, x, y + 15);
    }

    function drawSignals() {
      const { activeFilter: currentFilter } = latestRef.current;

      stateRef.current.signals = stateRef.current.signals.filter(
        (signal) => signal.t < signal.duration + 6,
      );

      stateRef.current.signals.forEach((signal) => {
        signal.t += 1;
        const progress = Math.min(signal.t / signal.duration, 1);
        const active = currentFilter === "ALL" || currentFilter === signal.tokenKey;
        const alphaScale = active ? 1 : 0.15;
        const segmentCount = signal.path.length - 1;
        const scaledProgress = progress * segmentCount;
        const segmentIndex = Math.min(Math.floor(scaledProgress), segmentCount - 1);
        const segmentProgress = Math.min(scaledProgress - segmentIndex, 1);
        const from = signal.path[segmentIndex];
        const to = signal.path[segmentIndex + 1];
        const x = from.x + (to.x - from.x) * segmentProgress;
        const y = from.y + (to.y - from.y) * segmentProgress;

        for (let index = 0; index < (active ? 5 : 2); index += 1) {
          const tailProgress = Math.max(0, progress - index * 0.032);
          const tailScaledProgress = tailProgress * segmentCount;
          const tailSegmentIndex = Math.min(Math.floor(tailScaledProgress), segmentCount - 1);
          const tailSegmentProgress = Math.min(tailScaledProgress - tailSegmentIndex, 1);
          const tailFrom = signal.path[tailSegmentIndex];
          const tailTo = signal.path[tailSegmentIndex + 1];
          const tailX = tailFrom.x + (tailTo.x - tailFrom.x) * tailSegmentProgress;
          const tailY = tailFrom.y + (tailTo.y - tailFrom.y) * tailSegmentProgress;
          ctx.beginPath();
          ctx.arc(tailX, tailY, 6.5 - index * 0.55, 0, Math.PI * 2);
          ctx.fillStyle = signal.color;
          ctx.globalAlpha = (0.62 - index * 0.1) * alphaScale;
          ctx.fill();
        }

        ctx.globalAlpha = 0.95 * alphaScale;
        ctx.beginPath();
        ctx.arc(x, y, 7, 0, Math.PI * 2);
        ctx.fillStyle = signal.color;
        ctx.fill();
        ctx.globalAlpha = 1;

        const symbol = String(signal.label || "?").slice(0, 4).toUpperCase();
        ctx.fillStyle = "rgba(12,14,20,.9)";
        ctx.font = "700 5px Noto Sans KR, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(symbol, x, y + 2);

        if (
          signal.stage === "OUTBOX_PROCESSING" &&
          !signal.queueDeposited &&
          progress >= 0.995
        ) {
          upsertQueueBlock(signal.event, signal.color);
          signal.queueDeposited = true;
        }

        if (active && progress < 0.78) {
          ctx.font = "700 7px Noto Sans KR, sans-serif";
          const width = ctx.measureText(signal.label).width;
          ctx.fillStyle = "rgba(12,14,20,.86)";
          ctx.fillRect(x - width / 2 - 4, y - 21, width + 8, 12);
          ctx.fillStyle = signal.color;
          ctx.textAlign = "center";
          ctx.fillText(signal.label, x, y - 12);
        }
      });
    }

    function drawBlockFlashes() {
      stateRef.current.blockFlashes = stateRef.current.blockFlashes.filter(
        (flash) => flash.alpha > 0.01,
      );
      stateRef.current.blockFlashes.forEach((flash) => {
        flash.r += 2;
        flash.alpha *= 0.91;
        ctx.beginPath();
        ctx.arc(flash.x, flash.y, flash.r, 0, Math.PI * 2);
        ctx.strokeStyle = flash.color;
        ctx.globalAlpha = flash.alpha;
        ctx.lineWidth = 1.5;
        ctx.stroke();
        ctx.globalAlpha = 1;
      });
    }

    function drawBlockStack() {
      const allBlocks = stateRef.current.queueBlocks;
      const blocks = allBlocks.slice(0, 10);
      const stack = allBlocks.length;
      const overflow = Math.max(0, stack - blocks.length);
      const x = getNodeX("outbox");
      const y = getNodeY("outbox");

      blocks.forEach((block, index) => {
        const top = y + 66 + index * 18;
        ctx.beginPath();
        ctx.roundRect(x - 68, top, 136, 14, 4);
        ctx.fillStyle = `${block.color}55`;
        ctx.fill();
        ctx.strokeStyle = `${block.color}aa`;
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.beginPath();
        ctx.roundRect(x - 73, top, 14, 14, 4);
        ctx.fillStyle = `${block.color}cc`;
        ctx.fill();
        ctx.fillStyle = "rgba(12,14,20,.88)";
        ctx.font = "700 8px Noto Sans KR, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(`${index + 1}`, x - 66, top + 10);

        ctx.fillStyle = "rgba(12,14,20,.88)";
        ctx.font = "700 7px Noto Sans KR, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(`TRADE ${block.tradeId ?? "-"}`, x - 48, top + 10);
        ctx.textAlign = "right";
        ctx.fillText(`${formatNumber(block.qty)}개`, x + 58, top + 10);
      });

      if (stack > 0) {
        ctx.fillStyle = "rgba(217,178,90,.75)";
        ctx.font = "700 9px Noto Sans KR, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(`${stack} in queue`, x + 70, y + 78);
      }

      if (overflow > 0) {
        ctx.fillStyle = "rgba(255,255,255,.72)";
        ctx.font = "700 8px Noto Sans KR, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(`+${overflow} more`, x + 70, y + 92);
      }
    }

    function loop(now) {
      const state = stateRef.current;
      const { settlement: currentSettlement } = latestRef.current;
      const delta = state.last ? now - state.last : 0;
      state.last = now;
      state.pulse += delta / 1000;

      const basePulse = (Math.sin(state.pulse * 2) + 1) / 2;
      const hasData =
        currentSettlement.totalTx > 0 ||
        currentSettlement.pendingCount > 0 ||
        currentSettlement.successCount > 0;
      const offChainGlow = Math.max(
        state.glows.ob,
        state.glows.match,
        state.glows.outbox,
        hasData ? 0.12 + basePulse * 0.18 : 0.02,
      );
      const queueGlow = Math.max(state.glows.outbox, hasData ? 0.09 + basePulse * 0.16 : 0.03);
      const onChainGlow = Math.max(state.glows.erc20, hasData ? 0.12 + basePulse * 0.18 : 0.02);

      ctx.clearRect(0, 0, state.width, state.height);
      drawLayers();
      drawArrow(getNodeX("ob") + 76, getNodeY("ob"), getNodeX("match") - 76, getNodeY("match"), "#7eb8f7", 0.22);
      drawArrow(getNodeX("match") + 76, getNodeY("match"), getNodeX("outbox") - 76, getNodeY("outbox"), "#c9a84c", 0.22);
      drawArrow(getNodeX("outbox") + 76, getNodeY("outbox"), getNodeX("erc20") - 76, getNodeY("erc20"), "#6ad98d", 0.2);

      drawNode(getNodeX("ob"), getNodeY("ob"), "주문", "Order", "#3a5a80", "#6a9acf", offChainGlow);
      drawNode(getNodeX("match"), getNodeY("match"), "체결", "Matching", "#4a72a0", "#7eb8f7", offChainGlow);
      drawNode(getNodeX("outbox"), getNodeY("outbox"), "DB Queue", "Queue", "#86611f", "#d9b25a", queueGlow);
      drawNode(getNodeX("erc20"), getNodeY("erc20"), "Blockchain", "transferFrom", "#2a6040", "#4ad98d", onChainGlow);

      drawMetricPill(getNodeX("erc20"), getNodeY("erc20") + 58, currentSettlement.successCount, "#6ad98d");
      drawBlockStack();
      drawSignals();
      drawBlockFlashes();

      Object.keys(state.glows).forEach((key) => {
        state.glows[key] = Math.max(0, state.glows[key] - 0.02);
      });

      frameRef.current = requestAnimationFrame(loop);
    }

    function handleCanvasClick(event) {
      const rect = canvas.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;
        const nodes = [
        { key: "ob", x: getNodeX("ob"), y: getNodeY("ob") },
        { key: "match", x: getNodeX("match"), y: getNodeY("match") },
        { key: "outbox", x: getNodeX("outbox"), y: getNodeY("outbox") },
        { key: "erc20", x: getNodeX("erc20"), y: getNodeY("erc20") },
      ];
      const node = nodes.find((item) => Math.hypot(mouseX - item.x, mouseY - item.y) < 58);

      if (!node) {
        setPopup(null);
        return;
      }

      setPopup({
        ...NODE_POPUPS[node.key],
        left: Math.min(event.clientX - rect.left + 14, stateRef.current.width - 230),
        top: Math.max(event.clientY - rect.top - 80, 86),
      });
    }

    function handleMouseMove(event) {
      const rect = canvas.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;
      const hover = ["ob", "match", "outbox", "erc20"].some((key) =>
        Math.hypot(mouseX - getNodeX(key), mouseY - getNodeY(key)) < 58,
      );
      canvas.style.cursor = hover ? "pointer" : "default";
    }

    resize();
    window.addEventListener("resize", resize);
    canvas.addEventListener("click", handleCanvasClick);
    canvas.addEventListener("mousemove", handleMouseMove);
    frameRef.current = requestAnimationFrame(loop);

    return () => {
      window.removeEventListener("resize", resize);
      canvas.removeEventListener("click", handleCanvasClick);
      canvas.removeEventListener("mousemove", handleMouseMove);
      if (frameRef.current) cancelAnimationFrame(frameRef.current);
    };
  }, []);

  return (
    <div
      data-settlement-root
      className="relative h-screen min-h-[820px] w-screen overflow-hidden bg-[#0c0e14] font-sans text-white"
    >
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" />

      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-5 top-4 flex items-center gap-3">
          <svg width="21" height="21" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <path d="M16 2L30 16L16 30L2 16Z" stroke="#c9a84c" strokeWidth="2" fill="none" />
            <path d="M16 2L30 16L2 16Z" fill="#c9a84c" fillOpacity="0.28" />
            <line x1="2" y1="16" x2="30" y2="16" stroke="#c9a84c" strokeWidth="1.5" />
          </svg>
          <div>
            <div className="text-sm font-black">STONE</div>
            <div className="mt-px text-[9px] font-bold tracking-[0.14em] text-white/25">
              REALTIME SETTLEMENT
            </div>
          </div>
        </div>

        <div
          className={`absolute right-5 top-4 flex items-center gap-2 rounded-full border px-4 py-1.5 text-[11px] font-bold ${
            socketConnected
              ? "border-[#6ad98d]/20 bg-[#6ad98d]/10 text-[#6ad98d]"
              : "border-[#f2c94c]/20 bg-[#f2c94c]/10 text-[#f2c94c]"
          }`}
        >
          <span
            className={`h-2 w-2 animate-pulse rounded-full ${
              socketConnected ? "bg-[#6ad98d]" : "bg-[#f2c94c]"
            }`}
          />
          {socketConnected ? "Trade Flow Live" : `Flow ${socketStatus}`}
        </div>

        <div className="absolute right-5 top-[60px] max-w-[460px] rounded-md border border-white/10 bg-black/35 px-4 py-2.5 font-mono text-[10px] text-white/35">
          <div>connect: {WS_ENDPOINT}</div>
          <div>subscribe: {WS_SUBSCRIPTION}</div>
        </div>

        {(statsError || socketError) && (
          <div className="absolute right-5 top-[112px] max-w-sm rounded-md border border-[#b85450]/30 bg-[#b85450]/15 px-4 py-3 text-[11px] font-bold text-[#f2a0a0]">
            {statsError || socketError}
          </div>
        )}

        <div className="pointer-events-auto absolute left-0 right-0 top-[60px] flex h-14 items-center border-b border-white/5 bg-black/25">
          <div className="flex flex-1 items-center gap-2 overflow-x-auto px-5 scrollbar-hide">
            <button
              type="button"
              onClick={() => setActiveFilter("ALL")}
              className={`shrink-0 rounded-lg border px-4 py-2 text-sm font-bold transition ${
                activeFilter === "ALL"
                  ? "border-white/15 bg-white/10 text-white"
                  : "border-transparent text-white/40 hover:bg-white/5 hover:text-white/70"
              }`}
            >
              전체 <span className="ml-1 text-white/45">{formatNumber(settlement.totalTx)}</span>
            </button>
            {displayTokens.map((token) => {
              const active = activeFilter === token.key;

              return (
                <button
                  key={`${token.tokenId ?? "token"}-${token.key}`}
                  type="button"
                  onClick={() => setActiveFilter(token.key)}
                  className="shrink-0 rounded-lg border px-4 py-2 text-sm font-bold transition hover:bg-white/5"
                  style={{
                    color: active ? token.color : "rgba(255,255,255,.42)",
                    borderColor: active ? `${token.color}66` : "transparent",
                    background: active ? "rgba(255,255,255,.08)" : "transparent",
                  }}
                >
                  {token.tokenSymbol}
                  {token.pending > 0 && (
                    <span className="ml-1.5 rounded-[4px] bg-[#f2c94c]/20 px-1.5 py-0.5 text-[10px] text-[#f2c94c]">
                      {formatNumber(token.pending)}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        <div className="absolute left-1/2 top-26 flex -translate-x-1/2 gap-3">
          {[
            ["총 체결", formatNumber(kpi.totalTx), "#7eb8f7"],
            ["OFF-CHAIN", formatNumber(kpi.pendingCount), "#f2c94c"],
            ["ON-CHAIN", formatNumber(kpi.successCount), "#6ad98d"],
            ["누적", formatWon(kpi.totalAmount), "#c9a84c"],
          ].map(([label, value, color]) => (
            <div
              key={label}
              className="min-w-[108px] rounded-lg border border-white/10 bg-white/[0.04] px-4 py-3 text-center"
            >
              <div className="text-[22px] font-black tracking-tight" style={{ color }}>
                {value}
              </div>
              <div className="mt-1 text-[10px] font-bold uppercase text-white/30">{label}</div>
            </div>
          ))}
        </div>

        {activeToken && (
          <div className="pointer-events-auto absolute right-5 top-40 w-56 rounded-xl border border-white/10 bg-white/[0.04] p-5">
            <div className="text-center text-sm font-black" style={{ color: activeToken.color }}>
              {activeToken.tokenSymbol}
            </div>
            <div className="mb-4 mt-1.5 text-center text-xs text-white/35">
              Token ID {activeToken.tokenId ?? "-"}
            </div>
            {[
              ["총 체결", `${formatNumber(activeToken.count)}건`],
              ["OFF-CHAIN", formatNumber(activeToken.pending)],
              ["ON-CHAIN", formatNumber(Math.max(0, activeToken.count - activeToken.pending))],
              ["누적금액", formatWon(activeToken.amount)],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between border-b border-white/5 py-2 text-[11px] last:border-0">
                <span className="text-white/35">{label}</span>
                <span className="font-bold text-white/75">{value}</span>
              </div>
            ))}
          </div>
        )}

        <div className="pointer-events-auto absolute bottom-0 left-0 right-0 h-36 border-t border-white/5 bg-black/45">
          <div className="px-5 pb-1 pt-3 text-[10px] font-bold uppercase tracking-widest text-white/25">
            실시간 체결 플로우
          </div>
          <div className="flex h-[108px] items-center gap-3 overflow-x-auto px-5 pb-4 scrollbar-hide">
            {statsLoading ? (
              <div className="text-xs font-bold text-white/25">정산 통계 로딩 중</div>
            ) : visibleFlowEvents.length === 0 ? (
              <div className="text-xs font-bold text-white/25">아직 수신한 체결 플로우가 없습니다.</div>
            ) : (
              visibleFlowEvents.map((event) => {
                const stage = STAGE_META[event.stage] ?? STAGE_META.PENDING;
                return (
                  <button
                    key={`${event.tradeId ?? "trade"}-${event.stage}-${event.receivedAt}`}
                    type="button"
                    className="min-w-[204px] shrink-0 rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2.5 text-left transition hover:bg-white/[0.08]"
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-black" style={{ color: getFlowColor(event) }}>
                        {event.tokenSymbol}
                      </span>
                      <span className={`ml-auto rounded-[4px] px-1.5 py-0.5 text-[9px] font-bold ${stage.badge}`}>
                        {stage.label}
                      </span>
                    </div>
                    <div className="mt-1.5 text-[10px] font-bold text-white/50">
                      #{event.tradeId ?? "-"} / {formatWon(event.amount)} / {formatNumber(event.qty)}개
                    </div>
                    <div className="truncate font-mono text-[9px] text-white/30">
                      {event.sellerName ?? "sellerName: null"} -&gt; {event.buyerName ?? "buyerName: null"}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {popup && (
          <div
            className="pointer-events-auto absolute z-20 w-64 rounded-xl border border-white/15 bg-[#0a0c12]/95 p-5"
            style={{ left: popup.left, top: popup.top }}
          >
            <button
              type="button"
              onClick={() => setPopup(null)}
              className="absolute right-3 top-2 text-base text-white/35 hover:text-white"
              aria-label="닫기"
            >
              x
            </button>
            <div className="text-sm font-black text-white">{popup.title}</div>
            <div className="mb-3 mt-1 text-[11px] text-white/30">{popup.sub}</div>
            {popup.rows.map(([label, value]) => (
              <div key={label} className="flex justify-between border-b border-white/5 py-1.5 text-[11px] last:border-0">
                <span className="text-white/35">{label}</span>
                <span className="max-w-[144px] truncate font-mono font-bold text-white/80">{value}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
