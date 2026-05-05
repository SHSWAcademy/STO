import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Coins,
  MonitorPlay,
  Sparkles,
  RefreshCw,
  Radio,
  TrendingUp,
  UserPlus,
  Users,
} from "lucide-react";
import api from "../../lib/api.js";
import { cn } from "../../lib/utils.js";
import { useApp } from "../../context/AppContext.jsx";
import { useAdminDashboardSocket } from "../../hooks/useAdminDashboardSocket.js";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];

const emptyPage = {
  content: [],
  number: 0,
  size: DEFAULT_PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function toNumber(value) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function firstDefined(...values) {
  return values.find((value) => value !== undefined && value !== null);
}

function formatNumber(value) {
  return toNumber(value).toLocaleString();
}

function formatCurrency(value) {
  return `${formatNumber(value)}원`;
}

function formatDateTime(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ");

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function getPageNumber(value) {
  return Number.isInteger(value) && value >= 0 ? value : 0;
}

function normalizeTradePage(pageData, fallbackPage, fallbackSize) {
  const nextTradePage = pageData ?? emptyPage;

  return {
    content: nextTradePage.content ?? [],
    number: getPageNumber(nextTradePage.number ?? fallbackPage),
    size: nextTradePage.size ?? fallbackSize,
    totalElements: nextTradePage.totalElements ?? 0,
    totalPages: nextTradePage.totalPages ?? 0,
    first: Boolean(nextTradePage.first),
    last: Boolean(nextTradePage.last),
  };
}

function normalizeDashboardPayload(payload, prevDashboard) {
  const body = payload?.data ?? payload?.dashboard ?? payload;

  if (Array.isArray(body)) {
    return {
      ...(prevDashboard ?? {}),
      tokenList: body,
    };
  }

  if (!body || typeof body !== "object") {
    return prevDashboard ?? null;
  }

  return {
    ...(prevDashboard ?? {}),
    ...body,
    tokenList: body.tokenList ?? prevDashboard?.tokenList ?? [],
  };
}

function PageButton({ children, active, disabled, onClick, ariaLabel }) {
  return (
    <button
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "flex h-9 min-w-9 items-center justify-center rounded-lg border px-3 text-xs font-bold transition-colors",
        active
          ? "border-stone-800 bg-stone-800 text-white"
          : "border-stone-200 bg-white text-stone-500 hover:bg-stone-100",
        disabled && "cursor-not-allowed opacity-40 hover:bg-white",
      )}
    >
      {children}
    </button>
  );
}

function StatCard({
  title,
  value,
  helper,
  icon: Icon,
  iconClassName,
  iconBgClassName,
  highlighted,
}) {
  return (
    <div
      className={cn(
        "rounded-lg border bg-white p-6 transition-all duration-700",
        highlighted
          ? "scale-[1.015] border-brand-gold bg-[#fff7dc] shadow-[0_0_0_2px_rgba(201,168,76,0.28),0_18px_34px_rgba(201,168,76,0.22)]"
          : "border-stone-200",
      )}
    >
      <div className="mb-4 flex items-center justify-between">
        <div className={cn("rounded-lg p-3", iconBgClassName)}>
          <Icon className={cn("h-6 w-6", iconClassName)} />
        </div>
      </div>
      <p className="mb-1 text-xs font-bold text-stone-400">{title}</p>
      <h3 className="text-2xl font-semibold text-stone-800">{value}</h3>
      {helper && (
        <p className="mt-2 text-xs font-medium text-stone-400">{helper}</p>
      )}
    </div>
  );
}

function SettlementBadge({ value }) {
  const normalized = String(value ?? "-");
  const isSuccess =
    normalized.includes("성공") || normalized.toUpperCase() === "SUCCESS";
  const isFail =
    normalized.includes("실패") || normalized.toUpperCase() === "FAILED";

  return (
    <span
      className={cn(
        "inline-flex rounded-md px-2 py-1 text-[10px] font-semibold",
        isSuccess && "bg-[#e8f4ee] text-[#3d7a58]",
        isFail && "bg-brand-red-light text-brand-red-dk",
        !isSuccess && !isFail && "bg-[#fef6dc] text-[#a07828]",
      )}
    >
      {normalized}
    </span>
  );
}

function TokenOwnershipCard({ token }) {
  const totalSupply = toNumber(
    firstDefined(token.totalSupply, token.total_supply),
  );
  const platformSupply = toNumber(
    firstDefined(token.holdingSupply, token.holding_supply),
  );
  const userSupply = toNumber(
    firstDefined(
      token.currentQuantity,
      token.current_quantity,
      token.userQuantity,
      token.user_quantity,
      token.userHoldingQuantity,
      token.user_holding_quantity,
    ),
  );
  const ownedSupply = userSupply + platformSupply;
  const userPercent = totalSupply > 0 ? (userSupply / totalSupply) * 100 : 0;
  const platformPercent =
    totalSupply > 0 ? (platformSupply / totalSupply) * 100 : 0;
  const ownedPercent = totalSupply > 0 ? (ownedSupply / totalSupply) * 100 : 0;
  const filledSquares = Math.round(Math.min(userPercent, 100));

  return (
    <div className="rounded-lg border border-stone-200 bg-white p-6">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate text-base font-black text-stone-800">
            {token.tokenName || "-"}
          </p>
          <p className="mt-1 text-xs font-bold text-stone-400">
            {token.tokenSymbol || "-"}
          </p>
        </div>
        <span className="shrink-0 rounded-md bg-stone-100 px-2 py-1 text-[10px] font-black text-stone-400">
          ID {token.tokenId ?? "-"}
        </span>
      </div>

      <div className="grid items-start gap-10 lg:grid-cols-[minmax(0,2fr)_minmax(260px,0.9fr)]">
        <div className="space-y-6">
          <div className="flex flex-col gap-8 md:flex-row md:items-center">
            <div className="grid w-full max-w-[320px] aspect-square shrink-0 grid-cols-10 gap-1.5 rounded-lg border border-stone-200 bg-stone-100 p-3">
              {Array.from({ length: 100 }).map((_, index) => (
                <div
                  key={index}
                  className={cn(
                    "rounded-[3px]",
                    index < filledSquares ? "bg-brand-blue" : "bg-stone-200",
                  )}
                />
              ))}
            </div>

            <div className="min-w-[220px] flex-1 space-y-5">
              <div>
                <span className="block text-[10px] font-black uppercase tracking-widest text-stone-400">
                  발행 및 보유 현황
                </span>
                <p className="mt-1 text-sm font-black text-stone-800">
                  총 {formatNumber(totalSupply)} 토큰
                </p>
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <div className="h-3 w-3 rounded-[2px] bg-brand-blue" />
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">
                      유저 보유
                    </p>
                    <p className="text-sm font-black text-stone-800">
                      {formatNumber(userSupply)} 토큰 · {userPercent.toFixed(1)}
                      %
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <div className="h-3 w-3 rounded-[2px] bg-stone-200" />
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">
                      플랫폼 보유
                    </p>
                    <p className="text-sm font-black text-stone-800">
                      {formatNumber(platformSupply)} 토큰 ·{" "}
                      {platformPercent.toFixed(1)}%
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-2 rounded-lg border border-stone-200 bg-stone-100 p-4">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold text-stone-400">
                    보유량 구성
                  </span>
                  <span className="text-[10px] font-black text-brand-blue">
                    {ownedPercent.toFixed(1)}%
                  </span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-stone-200">
                  <div
                    className="h-full rounded-full bg-brand-blue"
                    style={{ width: `${Math.min(ownedPercent, 100)}%` }}
                  />
                </div>
                <p className="text-[10px] font-bold text-stone-400">
                  유저 + 플랫폼 보유량 {formatNumber(ownedSupply)} / 총 발행량{" "}
                  {formatNumber(totalSupply)}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-3 rounded-lg border border-stone-200 bg-stone-50 p-5">
          <p className="text-xs font-black uppercase tracking-widest text-stone-400">
            토큰 요약
          </p>
          {[
            ["총 발행량", totalSupply],
            ["플랫폼 보유량", platformSupply],
            ["유저 보유량", userSupply],
          ].map(([label, value]) => (
            <div
              key={label}
              className="flex items-center justify-between border-b border-stone-200 py-3 last:border-b-0"
            >
              <span className="text-xs font-bold text-stone-400">{label}</span>
              <span className="text-sm font-black text-stone-800">
                {formatNumber(value)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export function AdminDashboard() {
  const { user } = useApp();
  const [dashboard, setDashboard] = useState(null);
  const [tradePage, setTradePage] = useState(emptyPage);
  const [liveTrades, setLiveTrades] = useState([]);
  const [dashboardUpdatedAt, setDashboardUpdatedAt] = useState(null);
  const [tradeUpdatedAt, setTradeUpdatedAt] = useState(null);
  const [dashboardHighlighted, setDashboardHighlighted] = useState(false);
  const [highlightedTradeId, setHighlightedTradeId] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [tradeListLoading, setTradeListLoading] = useState(true);
  const [error, setError] = useState("");
  const [tradeListRefreshKey, setTradeListRefreshKey] = useState(0);
  const [selectedTokenId, setSelectedTokenId] = useState("");

  const handleDashboardMessage = useCallback((nextDashboard) => {
    setDashboard((prev) => normalizeDashboardPayload(nextDashboard, prev));
    setDashboardUpdatedAt(new Date());
    setDashboardHighlighted(true);
  }, []);

  const handleTradeMessage = useCallback((trade) => {
    if (!trade) return;

    setLiveTrades((prev) => {
      const filtered = prev.filter((item) => item.tradeId !== trade.tradeId);
      return [trade, ...filtered].slice(0, 5);
    });
    setTradeUpdatedAt(new Date());
    setHighlightedTradeId(trade.tradeId ?? `${trade.executedAt ?? trade.createdAt ?? Date.now()}`);
  }, []);

  useAdminDashboardSocket({
    token: user?.accessToken ?? localStorage.getItem("token"),
    onDashboard: handleDashboardMessage,
    onTrade: handleTradeMessage,
  });

  useEffect(() => {
    let mounted = true;

    async function loadDashboard() {
      setDashboardLoading(true);
      setError("");

      try {
        const { data } = await api.get("/admin/dashboard");
        if (!mounted) return;
        setDashboard(data ?? null);
        setDashboardUpdatedAt(new Date());
      } catch (loadError) {
        console.error("[AdminDashboard] dashboard load failed:", loadError);
        if (!mounted) return;
        setDashboard(null);
        setError("대시보드 데이터를 불러오지 못했습니다.");
      } finally {
        if (mounted) setDashboardLoading(false);
      }
    }

    loadDashboard();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;

    async function loadTradeList() {
      setTradeListLoading(true);
      setError("");

      try {
        const { data } = await api.get("/admin/dashboard/list", {
          params: { page, size },
        });
        if (!mounted) return;
        setTradePage(normalizeTradePage(data, page, size));
      } catch (loadError) {
        console.error("[AdminDashboard] trade list load failed:", loadError);
        if (!mounted) return;
        setTradePage(emptyPage);
        setError("거래내역 데이터를 불러오지 못했습니다.");
      } finally {
        if (mounted) setTradeListLoading(false);
      }
    }

    loadTradeList();

    return () => {
      mounted = false;
    };
  }, [page, size, tradeListRefreshKey]);

  useEffect(() => {
    if (!dashboardHighlighted) return undefined;
    const timeoutId = window.setTimeout(() => {
      setDashboardHighlighted(false);
    }, 1800);
    return () => window.clearTimeout(timeoutId);
  }, [dashboardHighlighted]);

  useEffect(() => {
    if (!highlightedTradeId) return undefined;
    const timeoutId = window.setTimeout(() => {
      setHighlightedTradeId(null);
    }, 2400);
    return () => window.clearTimeout(timeoutId);
  }, [highlightedTradeId]);

  const tokenList = dashboard?.tokenList ?? [];
  const trades = tradePage.content ?? [];

  const selectedToken = useMemo(() => {
    if (tokenList.length === 0) return null;
    return (
      tokenList.find(
        (token) => String(token.tokenId ?? "") === selectedTokenId,
      ) ?? tokenList[0]
    );
  }, [selectedTokenId, tokenList]);

  const tokenSymbolById = useMemo(() => {
    return new Map(
      tokenList.map((token) => [
        String(token.tokenId ?? ""),
        token.tokenSymbol || "-",
      ]),
    );
  }, [tokenList]);

  useEffect(() => {
    if (tokenList.length === 0) {
      if (selectedTokenId) setSelectedTokenId("");
      return;
    }

    const hasSelectedToken = tokenList.some(
      (token) => String(token.tokenId ?? "") === selectedTokenId,
    );

    if (!hasSelectedToken) {
      setSelectedTokenId(String(tokenList[0].tokenId ?? ""));
    }
  }, [selectedTokenId, tokenList]);

  const pageNumbers = useMemo(() => {
    const totalPages = tradePage.totalPages || 1;
    const current = tradePage.number || 0;
    const start = Math.max(0, Math.min(current - 2, totalPages - 5));
    const end = Math.min(totalPages, start + 5);

    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [tradePage.number, tradePage.totalPages]);

  function handleSizeChange(event) {
    setSize(Number(event.target.value));
    setPage(0);
  }

  function goToPage(nextPage) {
    if (nextPage < 0 || nextPage >= tradePage.totalPages || nextPage === page)
      return;
    setPage(nextPage);
  }

  const tradeFrom =
    tradePage.totalElements === 0 ? 0 : tradePage.number * tradePage.size + 1;
  const tradeTo = Math.min(
    (tradePage.number + 1) * tradePage.size,
    tradePage.totalElements,
  );

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">
            어드민 대시보드
          </h1>
          <p className="text-sm text-stone-400">
            사용자, 체결, 토큰 발행량과 최근 거래내역을 확인합니다.
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <Link
            to="/admin-console/settlement-live"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-stone-800 px-4 py-2 text-sm font-bold text-white transition-colors hover:bg-stone-700"
          >
            <MonitorPlay className="h-4 w-4" />
            실시간 정산 보기
          </Link>
          <span className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-stone-400">
            요약{" "}
            {dashboardUpdatedAt
              ? `${formatDateTime(dashboardUpdatedAt)} 갱신`
              : "연결 대기중"}
          </span>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 rounded-lg border border-red-100 bg-red-50 px-6 py-4 text-sm font-medium text-red-600">
          <AlertCircle className="h-5 w-5" />
          {error}
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
        <StatCard
          title="총 사용자"
          value={`${formatNumber(dashboard?.totalUserCount)}명`}
          icon={Users}
          iconClassName="text-brand-blue"
          iconBgClassName="bg-brand-blue-light"
          highlighted={dashboardHighlighted}
        />
        <StatCard
          title="신규 가입자"
          value={`${formatNumber(dashboard?.newUserCount)}명`}
          helper="오늘 기준"
          icon={UserPlus}
          iconClassName="text-brand-green"
          iconBgClassName="bg-brand-green-light"
          highlighted={dashboardHighlighted}
        />
        <StatCard
          title="일일 체결수"
          value={`${formatNumber(dashboard?.dailyExecutionCount)}건`}
          helper="오늘 기준"
          icon={TrendingUp}
          iconClassName="text-brand-red"
          iconBgClassName="bg-brand-red-light"
          highlighted={dashboardHighlighted}
        />
        <StatCard
          title="누적 체결수"
          value={`${formatNumber(dashboard?.totalExecutionCount)}건`}
          icon={TrendingUp}
          iconClassName="text-stone-600"
          iconBgClassName="bg-stone-200"
          highlighted={dashboardHighlighted}
        />
        <StatCard
          title="일일 체결금액"
          value={formatCurrency(dashboard?.dailyExecutionAmount)}
          helper="오늘 기준"
          icon={Coins}
          iconClassName="text-brand-gold"
          iconBgClassName="bg-[#fef6dc]"
          highlighted={dashboardHighlighted}
        />
        <StatCard
          title="누적 체결금액"
          value={formatCurrency(dashboard?.totalExecutionAmount)}
          icon={Coins}
          iconClassName="text-brand-blue"
          iconBgClassName="bg-[#e8f0fa]"
          highlighted={dashboardHighlighted}
        />
      </div>

      {liveTrades.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
          <div className="flex flex-col gap-3 border-b border-stone-200 px-6 py-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <Radio className="h-5 w-5 text-brand-red" />
                <h3 className="text-base font-semibold text-stone-800">
                  실시간 체결 내역
                </h3>
              </div>
              <p className="mt-1 text-xs font-medium text-stone-400">
                거래가 체결되면 이 영역에 최신 체결이 먼저 표시됩니다.
              </p>
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-stone-100 px-3 py-2 text-xs font-bold text-stone-500">
              <Clock3 className="h-4 w-4" />
              {tradeUpdatedAt
                ? `${formatDateTime(tradeUpdatedAt)} 갱신`
                : "체결 대기중"}
            </div>
          </div>

          <div className="grid gap-0 divide-y divide-stone-200">
            {liveTrades.map((trade) => {
              const highlightKey =
                trade.tradeId ??
                `${trade.executedAt ?? trade.createdAt ?? ""}`;

              return (
                <div
                  key={`${trade.tradeId ?? "trade"}-${trade.executedAt ?? trade.createdAt ?? ""}`}
                  className={cn(
                    "relative grid gap-3 px-6 py-3 transition-all duration-700 hover:bg-stone-50 lg:grid-cols-[1.2fr_1fr_1fr_1fr_0.8fr]",
                    highlightedTradeId === highlightKey &&
                      "scale-[1.01] bg-[#fff4cf] shadow-[inset_6px_0_0_#c9a84c,0_0_0_2px_rgba(201,168,76,0.65),0_18px_36px_rgba(201,168,76,0.28)]",
                  )}
                >
                  <div className="min-w-0">
                    <div className="flex min-w-0 items-center gap-2">
                      <p className="truncate text-sm font-black text-stone-800">
                        {trade.tokenName ?? `Token ${trade.tokenId ?? "-"}`}
                      </p>
                      {highlightedTradeId === highlightKey && (
                        <span className="inline-flex shrink-0 items-center gap-1 rounded-md bg-brand-gold px-2 py-0.5 text-[10px] font-black text-white shadow-sm">
                          <Sparkles className="h-3 w-3" />
                          NEW
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-[10px] font-bold text-stone-400">
                      체결 ID {trade.tradeId ?? "-"} ·{" "}
                      {trade.tokenSymbol ??
                        tokenSymbolById.get(String(trade.tokenId ?? "")) ??
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-stone-400">
                      매도자
                    </p>
                    <p className="mt-1 text-sm font-bold text-stone-600">
                      {trade.sellerName ?? "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-stone-400">
                      매수자
                    </p>
                    <p className="mt-1 text-sm font-bold text-stone-600">
                      {trade.buyerName ?? "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-stone-400">
                      체결 금액
                    </p>
                    <p className="mt-1 text-sm font-black text-stone-800">
                      {formatCurrency(trade.totalTradePrice)}
                    </p>
                    <p className="mt-1 text-[10px] font-bold text-stone-400">
                      {formatNumber(trade.tradeQuantity)}개 ·{" "}
                      {formatCurrency(trade.tradePrice)}
                    </p>
                  </div>
                  <div className="flex flex-col items-start gap-2 lg:items-end">
                    <SettlementBadge value={trade.settlementStatus} />
                    <p className="text-xs font-bold text-stone-500">
                      {formatDateTime(trade.executedAt ?? trade.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div className="rounded-lg border border-stone-200 bg-white">
        <div className="flex flex-col gap-4 border-b border-stone-200 p-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h3 className="text-lg font-semibold text-stone-800">
              토큰 발행 및 소유권 분석
            </h3>
            <p className="mt-1 text-xs font-medium text-stone-400">
              거래중인 토큰의 총 발행량, 플랫폼 보유량, 유저 보유량을
              표시합니다.
            </p>
          </div>

          {tokenList.length > 0 && (
            <label className="flex items-center gap-2 text-xs font-bold text-stone-400">
              토큰 선택
              <select
                value={String(selectedToken?.tokenId ?? "")}
                onChange={(event) => setSelectedTokenId(event.target.value)}
                className="min-w-[220px] rounded-lg border border-stone-200 bg-white px-3 py-2 text-sm font-semibold text-stone-700 outline-none focus:border-brand-blue"
              >
                {tokenList.map((token) => (
                  <option
                    key={token.tokenId}
                    value={String(token.tokenId ?? "")}
                  >
                    {token.tokenName || "-"} ({token.tokenSymbol || "-"})
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>

        <div className="p-6">
          {dashboardLoading ? (
            <div className="py-16 text-center text-sm text-stone-400">
              불러오는 중...
            </div>
          ) : tokenList.length === 0 ? (
            <div className="py-16 text-center text-sm text-stone-400">
              표시할 토큰 데이터가 없습니다.
            </div>
          ) : selectedToken ? (
            <TokenOwnershipCard
              key={[
                selectedToken.tokenId,
                selectedToken.totalSupply,
                selectedToken.holdingSupply,
                selectedToken.currentQuantity,
              ].join("-")}
              token={selectedToken}
            />
          ) : (
            <div className="py-16 text-center text-sm text-stone-400">
              표시할 토큰 데이터가 없습니다.
            </div>
          )}
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="flex flex-col gap-3 border-b border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-lg font-semibold text-stone-800">
              최근 거래내역
            </h3>
            <p className="text-xs font-medium text-stone-400">
              전체 {tradePage.totalElements.toLocaleString()}건 중{" "}
              {tradeFrom.toLocaleString()}-{tradeTo.toLocaleString()} 표시
            </p>
          </div>

          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <button
              type="button"
              onClick={() => setTradeListRefreshKey((prev) => prev + 1)}
              className="flex items-center justify-center gap-2 rounded-lg border border-stone-200 bg-white px-4 py-2 text-sm font-semibold text-stone-500 transition-colors hover:bg-stone-100"
            >
              <RefreshCw
                className={cn("h-4 w-4", tradeListLoading && "animate-spin")}
              />
              거래내역 새로고침
            </button>

            <label className="flex items-center gap-2 text-xs font-bold text-stone-400">
              페이지당 표시
              <select
                value={size}
                onChange={handleSizeChange}
                className="rounded-lg border border-stone-200 bg-white px-3 py-2 text-sm font-semibold text-stone-700 outline-none focus:border-brand-blue"
              >
                {PAGE_SIZE_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}개
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 bg-stone-100">
                {[
                  "체결ID",
                  "토큰",
                  "매도자",
                  "매수자",
                  "체결가",
                  "수량",
                  "총 체결금액",
                  "정산상태",
                  "체결시간",
                ].map((header) => (
                  <th
                    key={header}
                    className={cn(
                      "px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400",
                      header === "정산상태" && "text-center",
                    )}
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {tradeListLoading ? (
                <tr>
                  <td
                    colSpan={9}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : trades.length === 0 ? (
                <tr>
                  <td
                    colSpan={9}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    표시할 거래내역이 없습니다.
                  </td>
                </tr>
              ) : (
                trades.map((trade) => (
                  <tr
                    key={trade.tradeId}
                    className="transition-colors hover:bg-stone-50"
                  >
                    <td className="px-6 py-4 text-sm font-mono font-bold text-stone-500">
                      {trade.tradeId ?? "-"}
                    </td>
                    <td className="px-6 py-4">
                      <p className="whitespace-nowrap text-sm font-black text-stone-800">
                        ID {trade.tokenName ?? "-"}
                      </p>
                      <p className="text-[10px] font-bold text-stone-400">
                        {trade.tokenSymbol ??
                          tokenSymbolById.get(String(trade.tokenId ?? "")) ??
                          "-"}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-sm font-bold text-stone-500">
                      {trade.sellerName ?? "-"}
                    </td>
                    <td className="px-6 py-4 text-sm font-bold text-stone-500">
                      {trade.buyerName ?? "-"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-stone-500">
                      {formatCurrency(trade.tradePrice)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-stone-500">
                      {formatNumber(trade.tradeQuantity)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-black text-stone-800">
                      {formatCurrency(trade.totalTradePrice)}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <SettlementBadge value={trade.settlementStatus} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-xs font-bold text-stone-500">
                      {formatDateTime(trade.executedAt ?? trade.createdAt)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-4 border-t border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs font-bold text-stone-400">
            {tradePage.totalPages > 0
              ? `${(tradePage.number + 1).toLocaleString()} / ${tradePage.totalPages.toLocaleString()} 페이지`
              : "0 / 0 페이지"}
          </p>

          <div className="flex flex-wrap items-center gap-2">
            <PageButton
              disabled={
                tradePage.first || tradeListLoading || tradePage.totalPages === 0
              }
              onClick={() => goToPage(tradePage.number - 1)}
              ariaLabel="이전 페이지"
            >
              <ChevronLeft className="h-4 w-4" />
            </PageButton>

            {pageNumbers.map((pageNumber) => (
              <PageButton
                key={pageNumber}
                active={pageNumber === tradePage.number}
                disabled={tradeListLoading}
                onClick={() => goToPage(pageNumber)}
                ariaLabel={`${pageNumber + 1} 페이지`}
              >
                {pageNumber + 1}
              </PageButton>
            ))}

            <PageButton
              disabled={tradePage.last || tradeListLoading || tradePage.totalPages === 0}
              onClick={() => goToPage(tradePage.number + 1)}
              ariaLabel="다음 페이지"
            >
              <ChevronRight className="h-4 w-4" />
            </PageButton>
          </div>
        </div>
      </div>
    </div>
  );
}
