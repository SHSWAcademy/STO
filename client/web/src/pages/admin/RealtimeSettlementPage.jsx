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

const NODE_POPUPS = {
  ob: {
    title: "호가창",
    sub: "Order Book",
    rows: [
      ["역할", "주문 접수/집계"],
      ["이벤트", "PENDING"],
    ],
  },
  match: {
    title: "매칭 엔진",
    sub: "Off-Chain",
    rows: [
      ["방식", "가격-시간 우선"],
      ["표시값", "pendingCount"],
    ],
  },
  outbox: {
    title: "DB Outbox",
    sub: "Transactional Outbox Pattern",
    rows: [
      ["이벤트", "OUTBOX_PROCESSING"],
      ["처리", "BlockchainWorkerService"],
    ],
  },
  erc20: {
    title: "ERC-20 컨트랙트",
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
    blockStack: 0,
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
    const token = latestRef.current.displayTokens.find(
      (item) =>
        item.key === getFlowKey(event) ||
        (event.tokenId !== null && String(item.tokenId) === String(event.tokenId)),
    );
    return token?.color ?? STAGE_META[event.stage]?.color ?? "#7eb8f7";
  }

  function getNodeX(key) {
    const width = stateRef.current.width;
    return {
      ob: width * 0.14,
      match: width * 0.34,
      outbox: width * 0.55,
      erc20: width * 0.78,
    }[key];
  }

  function getNodeY() {
    return stateRef.current.height * 0.46;
  }

  function enqueueSignal(fromKey, toKey, event, color) {
    const state = stateRef.current;
    const yOffset = (toNumber(event.tradeId) % 3 - 1) * 9;
    state.signals.push({
      x1: getNodeX(fromKey),
      y1: getNodeY() + yOffset,
      x2: getNodeX(toKey),
      y2: getNodeY() + yOffset,
      color,
      label: event.tokenSymbol || event.stage,
      tokenKey: getFlowKey(event),
      t: 0,
      duration: event.stage === "SUCCESS" ? 96 : 42,
    });
  }

  function enqueueFlowAnimation(event) {
    const color = getFlowColor(event);
    const state = stateRef.current;

    if (event.stage === "PENDING") {
      state.glows.ob = 1;
      state.glows.match = 1;
      enqueueSignal("ob", "match", event, color);
      return;
    }

    if (event.stage === "OUTBOX_PROCESSING") {
      state.glows.outbox = 1;
      enqueueSignal("match", "outbox", event, color);
      return;
    }

    if (event.stage === "SUCCESS") {
      state.glows.erc20 = 1;
      state.blockStack += 1;
      state.blockFlashes.push({
        x: getNodeX("erc20"),
        y: getNodeY(),
        r: 0,
        alpha: 0.9,
        color: "#6ad98d",
      });
      enqueueSignal("outbox", "erc20", event, color);
      return;
    }

    if (event.stage === "FAILED") {
      state.glows.outbox = 1;
      state.blockFlashes.push({
        x: getNodeX("outbox"),
        y: getNodeY(),
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
      const filtered =
        event.tradeId === null
          ? prev
          : prev.filter((item) => item.tradeId !== event.tradeId);
      return [event, ...filtered].slice(0, 80);
    });
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

    function drawLayer(cx, fill, color, label) {
      const { width, height } = stateRef.current;
      ctx.fillStyle = fill;
      ctx.fillRect(cx - width * 0.15, height * 0.12, width * 0.3, height * 0.72);
      ctx.fillStyle = color;
      ctx.globalAlpha = 0.24;
      ctx.font = "900 9px Noto Sans KR, sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(label, cx, height * 0.19);
      ctx.globalAlpha = 1;
    }

    function drawLayers() {
      const width = stateRef.current.width;
      drawLayer(width * 0.24, "rgba(74,114,160,.04)", "#7eb8f7", "OFF-CHAIN");
      drawLayer(width * 0.55, "rgba(160,120,40,.04)", "#c9a84c", "BRIDGE");
      drawLayer(width * 0.78, "rgba(42,96,64,.04)", "#6ad98d", "ON-CHAIN");
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
        const gradient = ctx.createRadialGradient(x, y, 10, x, y, 70);
        gradient.addColorStop(0, `${glowColor}${Math.round(glowPower * 85).toString(16).padStart(2, "0")}`);
        gradient.addColorStop(1, "transparent");
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(x, y, 70, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.beginPath();
      ctx.roundRect(x - 58, y - 34, 116, 68, 8);
      ctx.fillStyle = `${baseColor}22`;
      ctx.fill();
      ctx.strokeStyle = `${glowColor}${glowPower > 0.12 ? "aa" : "55"}`;
      ctx.lineWidth = glowPower > 0.12 ? 1.5 : 0.8;
      ctx.stroke();

      ctx.fillStyle = "rgba(255,255,255,.9)";
      ctx.font = "700 11px Noto Sans KR, sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(label, x, y - 6);
      ctx.fillStyle = "rgba(255,255,255,.32)";
      ctx.font = "400 8px Noto Sans KR, sans-serif";
      ctx.fillText(sub, x, y + 9);
    }

    function drawMetricPill(x, y, value, color) {
      ctx.font = "700 8px Noto Sans KR, sans-serif";
      const text = formatNumber(value);
      const textWidth = ctx.measureText(text).width;
      ctx.beginPath();
      ctx.roundRect(x - textWidth / 2 - 8, y, textWidth + 16, 18, 6);
      ctx.fillStyle = "rgba(8,10,16,.78)";
      ctx.fill();
      ctx.strokeStyle = `${color}55`;
      ctx.stroke();
      ctx.fillStyle = color;
      ctx.textAlign = "center";
      ctx.fillText(text, x, y + 12);
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
        const x = signal.x1 + (signal.x2 - signal.x1) * progress;
        const y = signal.y1 + (signal.y2 - signal.y1) * progress;

        for (let index = 0; index < (active ? 5 : 2); index += 1) {
          const tailProgress = Math.max(0, progress - index * 0.04);
          const tailX = signal.x1 + (signal.x2 - signal.x1) * tailProgress;
          const tailY = signal.y1 + (signal.y2 - signal.y1) * tailProgress;
          ctx.beginPath();
          ctx.arc(tailX, tailY, 5 - index * 0.4, 0, Math.PI * 2);
          ctx.fillStyle = signal.color;
          ctx.globalAlpha = (0.62 - index * 0.1) * alphaScale;
          ctx.fill();
        }

        ctx.globalAlpha = 0.95 * alphaScale;
        ctx.beginPath();
        ctx.arc(x, y, 5, 0, Math.PI * 2);
        ctx.fillStyle = signal.color;
        ctx.fill();
        ctx.globalAlpha = 1;

        if (active && progress < 0.78) {
          ctx.font = "700 8px Noto Sans KR, sans-serif";
          const width = ctx.measureText(signal.label).width;
          ctx.fillStyle = "rgba(12,14,20,.86)";
          ctx.fillRect(x - width / 2 - 4, y - 20, width + 8, 13);
          ctx.fillStyle = signal.color;
          ctx.textAlign = "center";
          ctx.fillText(signal.label, x, y - 10);
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
      const stack = Math.min(stateRef.current.blockStack, 10);
      const x = getNodeX("erc20");
      const y = getNodeY();

      for (let index = 0; index < stack; index += 1) {
        ctx.beginPath();
        ctx.roundRect(x - 36, y + 44 + index * 6, 72, 5, 2);
        ctx.fillStyle = `rgba(106,217,141,${Math.min(0.07 + (stack - index) * 0.06, 0.5)})`;
        ctx.fill();
      }

      if (stateRef.current.blockStack > 0) {
        ctx.fillStyle = "rgba(106,217,141,.55)";
        ctx.font = "700 8px Noto Sans KR, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(`${stateRef.current.blockStack}블록`, x, y + 51 + stack * 6);
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
      const offChainGlow = Math.max(state.glows.ob, state.glows.match, hasData ? 0.12 + basePulse * 0.18 : 0.02);
      const bridgeGlow = Math.max(state.glows.outbox, hasData ? 0.08 + basePulse * 0.15 : 0.02);
      const onChainGlow = Math.max(state.glows.erc20, hasData ? 0.12 + basePulse * 0.18 : 0.02);

      ctx.clearRect(0, 0, state.width, state.height);
      drawLayers();
      drawArrow(getNodeX("ob") + 62, getNodeY(), getNodeX("match") - 62, getNodeY(), "#7eb8f7", 0.18);
      drawArrow(getNodeX("match") + 62, getNodeY(), getNodeX("outbox") - 62, getNodeY(), "#c9a84c", 0.18);
      drawArrow(getNodeX("outbox") + 62, getNodeY(), getNodeX("erc20") - 62, getNodeY(), "#c9a84c", 0.14);

      drawNode(getNodeX("ob"), getNodeY(), "호가창", "Order Book", "#3a5a80", "#6a9acf", offChainGlow);
      drawNode(getNodeX("match"), getNodeY(), "매칭 엔진", "Off-Chain", "#4a72a0", "#7eb8f7", offChainGlow);
      drawNode(getNodeX("outbox"), getNodeY(), "DB Outbox", "@Async Web3j", "#a07828", "#c9a84c", bridgeGlow);
      drawNode(getNodeX("erc20"), getNodeY(), "ERC-20", "transferFrom", "#2a6040", "#4ad98d", onChainGlow);

      drawMetricPill(getNodeX("match"), getNodeY() + 48, currentSettlement.pendingCount, "#f2c94c");
      drawMetricPill(getNodeX("erc20"), getNodeY() + 48, currentSettlement.successCount, "#6ad98d");
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
        { key: "ob", x: getNodeX("ob"), y: getNodeY() },
        { key: "match", x: getNodeX("match"), y: getNodeY() },
        { key: "outbox", x: getNodeX("outbox"), y: getNodeY() },
        { key: "erc20", x: getNodeX("erc20"), y: getNodeY() },
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
        Math.hypot(mouseX - getNodeX(key), mouseY - getNodeY()) < 58,
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
      className="relative h-screen min-h-[720px] w-screen overflow-hidden bg-[#0c0e14] font-sans text-white"
    >
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" />

      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-4 top-3 flex items-center gap-2">
          <svg width="17" height="17" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <path d="M16 2L30 16L16 30L2 16Z" stroke="#c9a84c" strokeWidth="2" fill="none" />
            <path d="M16 2L30 16L2 16Z" fill="#c9a84c" fillOpacity="0.28" />
            <line x1="2" y1="16" x2="30" y2="16" stroke="#c9a84c" strokeWidth="1.5" />
          </svg>
          <div>
            <div className="text-xs font-black">STONE</div>
            <div className="mt-px text-[7px] font-bold tracking-[0.1em] text-white/25">
              REALTIME SETTLEMENT
            </div>
          </div>
        </div>

        <div
          className={`absolute right-4 top-3 flex items-center gap-2 rounded-full border px-3 py-1 text-[9px] font-bold ${
            socketConnected
              ? "border-[#6ad98d]/20 bg-[#6ad98d]/10 text-[#6ad98d]"
              : "border-[#f2c94c]/20 bg-[#f2c94c]/10 text-[#f2c94c]"
          }`}
        >
          <span
            className={`h-1.5 w-1.5 animate-pulse rounded-full ${
              socketConnected ? "bg-[#6ad98d]" : "bg-[#f2c94c]"
            }`}
          />
          {socketConnected ? "Trade Flow Live" : `Flow ${socketStatus}`}
        </div>

        <div className="absolute right-4 top-12 max-w-[420px] rounded-md border border-white/10 bg-black/35 px-3 py-2 font-mono text-[9px] text-white/35">
          <div>connect: {WS_ENDPOINT}</div>
          <div>subscribe: {WS_SUBSCRIPTION}</div>
        </div>

        {(statsError || socketError) && (
          <div className="absolute right-4 top-[92px] max-w-xs rounded-md border border-[#b85450]/30 bg-[#b85450]/15 px-3 py-2 text-[10px] font-bold text-[#f2a0a0]">
            {statsError || socketError}
          </div>
        )}

        <div className="pointer-events-auto absolute left-0 right-0 top-12 flex h-10 items-center border-b border-white/5 bg-black/25">
          <div className="flex flex-1 items-center gap-1 overflow-x-auto px-3 scrollbar-hide">
            <button
              type="button"
              onClick={() => setActiveFilter("ALL")}
              className={`shrink-0 rounded-md border px-3 py-1.5 text-[10px] font-bold transition ${
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
                  className="shrink-0 rounded-md border px-3 py-1.5 text-[10px] font-bold transition hover:bg-white/5"
                  style={{
                    color: active ? token.color : "rgba(255,255,255,.42)",
                    borderColor: active ? `${token.color}66` : "transparent",
                    background: active ? "rgba(255,255,255,.08)" : "transparent",
                  }}
                >
                  {token.tokenSymbol}
                  {token.pending > 0 && (
                    <span className="ml-1 rounded-[3px] bg-[#f2c94c]/20 px-1 text-[8px] text-[#f2c94c]">
                      {formatNumber(token.pending)}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        <div className="absolute left-1/2 top-28 flex -translate-x-1/2 gap-2">
          {[
            ["총 체결", formatNumber(kpi.totalTx), "#7eb8f7"],
            ["OFF-CHAIN", formatNumber(kpi.pendingCount), "#f2c94c"],
            ["ON-CHAIN", formatNumber(kpi.successCount), "#6ad98d"],
            ["누적", formatWon(kpi.totalAmount), "#c9a84c"],
          ].map(([label, value, color]) => (
            <div
              key={label}
              className="min-w-[76px] rounded-md border border-white/10 bg-white/[0.04] px-3 py-2 text-center"
            >
              <div className="text-base font-black tracking-tight" style={{ color }}>
                {value}
              </div>
              <div className="mt-px text-[7px] font-bold uppercase text-white/30">{label}</div>
            </div>
          ))}
        </div>

        {activeToken && (
          <div className="pointer-events-auto absolute right-4 top-40 w-44 rounded-lg border border-white/10 bg-white/[0.04] p-4">
            <div className="text-center text-xs font-black" style={{ color: activeToken.color }}>
              {activeToken.tokenSymbol}
            </div>
            <div className="mb-3 mt-1 text-center text-[10px] text-white/35">
              Token ID {activeToken.tokenId ?? "-"}
            </div>
            {[
              ["총 체결", `${formatNumber(activeToken.count)}건`],
              ["OFF-CHAIN", formatNumber(activeToken.pending)],
              ["ON-CHAIN", formatNumber(Math.max(0, activeToken.count - activeToken.pending))],
              ["누적금액", formatWon(activeToken.amount)],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between border-b border-white/5 py-1.5 text-[9px] last:border-0">
                <span className="text-white/35">{label}</span>
                <span className="font-bold text-white/75">{value}</span>
              </div>
            ))}
          </div>
        )}

        <div className="pointer-events-auto absolute bottom-0 left-0 right-0 h-24 border-t border-white/5 bg-black/45">
          <div className="px-4 pb-1 pt-2 text-[8px] font-bold uppercase tracking-widest text-white/25">
            실시간 체결 플로우
          </div>
          <div className="flex h-16 items-center gap-2 overflow-x-auto px-4 pb-2 scrollbar-hide">
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
                    className="min-w-[156px] shrink-0 rounded-md border border-white/10 bg-white/[0.04] px-2 py-1.5 text-left transition hover:bg-white/[0.08]"
                  >
                    <div className="flex items-center gap-1">
                      <span className="text-[9px] font-black" style={{ color: getFlowColor(event) }}>
                        {event.tokenSymbol}
                      </span>
                      <span className={`ml-auto rounded-[3px] px-1 text-[7px] font-bold ${stage.badge}`}>
                        {stage.label}
                      </span>
                    </div>
                    <div className="mt-1 text-[8px] font-bold text-white/50">
                      #{event.tradeId ?? "-"} / {formatWon(event.amount)} / {formatNumber(event.qty)}개
                    </div>
                    <div className="truncate font-mono text-[7px] text-white/30">
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
            className="pointer-events-auto absolute z-20 w-56 rounded-lg border border-white/15 bg-[#0a0c12]/95 p-4"
            style={{ left: popup.left, top: popup.top }}
          >
            <button
              type="button"
              onClick={() => setPopup(null)}
              className="absolute right-2 top-1 text-sm text-white/35 hover:text-white"
              aria-label="닫기"
            >
              x
            </button>
            <div className="text-xs font-black text-white">{popup.title}</div>
            <div className="mb-2 mt-1 text-[9px] text-white/30">{popup.sub}</div>
            {popup.rows.map(([label, value]) => (
              <div key={label} className="flex justify-between border-b border-white/5 py-1 text-[9px] last:border-0">
                <span className="text-white/35">{label}</span>
                <span className="max-w-[130px] truncate font-mono font-bold text-white/80">{value}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
