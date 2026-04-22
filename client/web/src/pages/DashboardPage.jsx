import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ExternalLink, Heart, Newspaper, Sparkles, TrendingUp, TrendingDown } from "lucide-react";
import { ResponsiveContainer, ComposedChart, Bar, YAxis, XAxis, Tooltip } from "recharts";
import { useApp } from "../context/AppContext.jsx";
import api from "../lib/api.js";
import { cn } from "../lib/utils.js";
import { API_BASE_URL } from "../lib/config.js";
import { TabSwitcher } from "../components/ui/TabSwitcher.jsx";
import { AssetAvatar } from "../components/ui/AssetAvatar.jsx";
import { useDashboardSocket } from "../hooks/useDashboardSocket.js";

const API = API_BASE_URL;
const PAGE_SIZE = 10;
const CANDLE_COUNT = 20;

const SORT_ITEMS = ["전체", "거래대금", "거래량"];

const SELECT_TYPE_MAP = {
  전체: "BASIC",
  거래대금: "TOTAL_TRADE_VALUE",
  거래량: "TOTAL_TRADE_QUANTITY",
};

function recalculateFluctuationRate(currentPrice, basePrice) {
  if (!basePrice || basePrice <= 0) return 0;
  return Math.round((((currentPrice - basePrice) / basePrice) * 100) * 100) / 100;
}

// ?? 罹붾뱾 ?좏떥 ?????????????????????????????????????????????????????
function formatCandleTime(candleTime) {
  if (!candleTime) return '';
  const d = new Date(candleTime);
  return d.toTimeString().slice(0, 5);
}

function mapCandle(dto) {
  const open  = dto.openPrice  != null ? Math.round(dto.openPrice)  : null;
  const high  = dto.highPrice  != null ? Math.round(dto.highPrice)  : null;
  const low   = dto.lowPrice   != null ? Math.round(dto.lowPrice)   : null;
  const close = dto.closePrice != null ? Math.round(dto.closePrice) : null;
  const vol   = Math.round(dto.volume     || 0);
  return {
    ts:   dto.candleTime ? new Date(dto.candleTime).getTime() : 0,
    time: formatCandleTime(dto.candleTime),
    open, high, low, close, vol,
    isSynthetic: vol === 0 && open > 0 && open === close && open === high && open === low,
  };
}

function buildChartData(fetchedCandles) {
  return [...fetchedCandles]
    .filter(c => c?.ts)
    .sort((a, b) => a.ts - b.ts)
    .slice(-CANDLE_COUNT);
}

function formatAiSummaryUpdatedAt(value) {
  if (!value) return "";

  const date = Array.isArray(value)
    ? new Date(
        value[0],
        (value[1] ?? 1) - 1,
        value[2] ?? 1,
        value[3] ?? 0,
        value[4] ?? 0,
        value[5] ?? 0,
      )
    : new Date(value);

  if (Number.isNaN(date.getTime())) return "";

  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

function formatNewsDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

// ?? 罹붾뱾?ㅽ떛 shape ????????????????????????????????????????????????
function CandlestickShape(props) {
  const { x, y, width, height } = props;
  const open  = props.payload?.open;
  const close = props.payload?.close;
  const high  = props.payload?.high;
  const low   = props.payload?.low;
  const isSynthetic = props.payload?.isSynthetic;

  if (open == null || close == null || high == null || low == null) return null;
  if (width <= 0) return null;

  const priceRange = high - low;
  const isUp  = close >= open;
  const color = isSynthetic ? '#a8a29e' : (isUp ? '#e54d4d' : '#3b82f6');
  const cx    = x + width / 2;

  if (priceRange <= 0 || height <= 0) {
    const flatY = height > 0 ? y + height / 2 : y;
    return (
      <g>
        <line x1={x + 1} y1={flatY} x2={x + Math.max(width - 1, 1)} y2={flatY}
              stroke={color} strokeWidth={isSynthetic ? 1 : 1.4} strokeLinecap="round" />
      </g>
    );
  }

  const ratio        = height / priceRange;
  const highPx       = y;
  const lowPx        = y + height;
  const bodyTopPx    = y + (high - Math.max(open, close)) * ratio;
  const bodyBottomPx = y + (high - Math.min(open, close)) * ratio;
  const bodyH        = Math.max(bodyBottomPx - bodyTopPx, 1);

  return (
    <g>
      <line x1={cx} y1={highPx}       x2={cx} y2={bodyTopPx}    stroke={color} strokeWidth={1.2} />
      <line x1={cx} y1={bodyBottomPx} x2={cx} y2={lowPx}        stroke={color} strokeWidth={1.2} />
      <rect x={x + 1} y={bodyTopPx} width={Math.max(width - 2, 1)} height={bodyH} fill={color} rx={1} />
    </g>
  );
}

// ?? 硫붿씤 而댄룷?뚰듃 ?????????????????????????????????????????????????
export function DashboardPage() {
  const navigate = useNavigate();
  const { likedTokenIds, toggleLike, user, showGuestBanner } = useApp();

  const [chartFilter, setChartFilter] = useState("전체");
  const [page, setPage] = useState(0);
  const [tokens, setTokens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [previewTokenId, setPreviewTokenId] = useState(null);
  const [priceFlash, setPriceFlash] = useState({});
  const flashTimersRef = useRef({});
  const [candleData, setCandleData] = useState([]);
  const [candleLoading, setCandleLoading] = useState(false);
  const [newsItems, setNewsItems] = useState([]);
  const [newsLoading, setNewsLoading] = useState(false);
  const [summary, setSummary] = useState(null);
  const [newsError, setNewsError] = useState("");

  const tokenIds = useMemo(() => tokens.map((token) => token.tokenId), [tokens]);

  useEffect(() => {
    setPage(0);
  }, [chartFilter]);

  useEffect(() => {
    api.get("/api/token/summary")
      .then(({ data }) => setSummary(data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    let mounted = true;

    async function loadNews() {
      setNewsLoading(true);
      setNewsError("");
      try {
        const headers = {};
        if (user?.accessToken) {
          headers.Authorization = `Bearer ${user.accessToken}`;
        }
        const { data } = await api.get("/api/news/sto", { headers });
        if (!mounted) return;
        setNewsItems(Array.isArray(data) ? data : []);
      } catch (error) {
        console.warn("[Dashboard] news fetch failed:", error);
        if (!mounted) return;
        setNewsItems([]);
        setNewsError(
          error?.response?.status === 401
            ? "뉴스 조회 권한이 없습니다."
            : "뉴스를 불러오지 못했습니다.",
        );
      } finally {
        if (mounted) {
          setNewsLoading(false);
        }
      }
    }

    loadNews();
    return () => {
      mounted = false;
    };
  }, [user?.accessToken]);

  // ?좏겙 紐⑸줉
  useEffect(() => {
    const selectType = SELECT_TYPE_MAP[chartFilter];
    setLoading(true);
    const headers = {};
    if (user?.accessToken) headers.Authorization = `Bearer ${user.accessToken}`;
    fetch(`${API}/api/token?page=${page}&selectType=${selectType}`, { headers })
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((data) => {
        const nextTokens = Array.isArray(data) ? data : [];
        setTokens(nextTokens);
        setHasNext(nextTokens.length === PAGE_SIZE);
        setPreviewTokenId((prev) => {
          if (!nextTokens.length) return null;
          const hasCurrent = prev != null && nextTokens.some((token) => token.tokenId === prev);
          return hasCurrent ? prev : nextTokens[0].tokenId;
        });
      })
      .catch((error) => console.warn("[Dashboard] token list fetch failed:", error))
      .finally(() => setLoading(false));
  }, [page, chartFilter, user]);

  // 1遺꾨큺 罹붾뱾 珥덇린 濡쒕뱶 (?좏깮 ?좏겙 蹂寃???
  useEffect(() => {
    if (!previewTokenId) return;
    const controller = new AbortController();
    setCandleData([]);
    setCandleLoading(true);
    const headers = {};
    if (user?.accessToken) headers.Authorization = `Bearer ${user.accessToken}`;
    fetch(`${API}/api/token/${previewTokenId}/candle?type=MINUTE`, { headers, signal: controller.signal })
      .then(r => r.ok ? r.json() : Promise.reject(r.status))
      .then(data => {
        const candles = (Array.isArray(data) ? data : []).map(d => mapCandle(d));
        setCandleData(buildChartData(candles));
      })
      .catch(e => {
        if (e.name === 'AbortError') return;
        console.warn('[Dashboard] candle fetch failed:', e);
      })
      .finally(() => setCandleLoading(false));
    return () => controller.abort();
  }, [previewTokenId, user?.accessToken]);

  useDashboardSocket({
    tokenIds,
    candleType: 'MINUTE',
    token: user?.accessToken,
    onTrade: ({ tokenId, trade }) => {
      setTokens((prev) =>
        prev.map((token) => {
          if (token.tokenId !== tokenId) return token;
          const currentPrice = trade.tradePrice ?? token.currentPrice ?? 0;
          const tradeQuantity = trade.tradeQuantity ?? 0;
          const tradeAmount = currentPrice * tradeQuantity;
          return {
            ...token,
            currentPrice,
            totalTradeValue: (token.totalTradeValue ?? 0) + tradeAmount,
            totalTradeQuantity: (token.totalTradeQuantity ?? 0) + tradeQuantity,
            fluctuationRate: recalculateFluctuationRate(currentPrice, token.basePrice),
          };
        }),
      );
      clearTimeout(flashTimersRef.current[tokenId]);
      setPriceFlash((prev) => ({ ...prev, [tokenId]: trade.isBuy ? 'red' : 'blue' }));
      flashTimersRef.current[tokenId] = setTimeout(() => {
        setPriceFlash((prev) => ({ ...prev, [tokenId]: null }));
      }, 500);
    },
    onCandle: ({ tokenId, candle }) => {
      if (tokenId !== previewTokenId) return;
      const incoming = mapCandle(candle);
      setCandleData(prev => {
        const idx = prev.findIndex(c => c.ts === incoming.ts);
        if (idx >= 0) {
          const next = [...prev];
          next[idx] = { ...incoming, isSynthetic: false };
          return next;
        }
        const next = [...prev, { ...incoming, isSynthetic: false }];
        return next.length > CANDLE_COUNT ? next.slice(-CANDLE_COUNT) : next;
      });
    },
  });

  const previewToken = useMemo(
    () => tokens.find((token) => token.tokenId === previewTokenId) ?? null,
    [tokens, previewTokenId],
  );

  const validData = candleData.filter(d => d.open != null && d.open > 0);
  const yMin = validData.length > 0 ? Math.min(...validData.map(d => d.low))  : 0;
  const yMax = validData.length > 0 ? Math.max(...validData.map(d => d.high)) : 100;
  const yPad = Math.max((yMax - yMin) * 0.08, 1);
  const aiSummaryUpdatedAtText = formatAiSummaryUpdatedAt(previewToken?.aiSummaryUpdatedAt);
  const flatCount = summary ? Math.max((summary.totalAssets ?? 0) - (summary.upCount ?? 0) - (summary.downCount ?? 0), 0) : 0;
  const upRatio = summary?.totalAssets ? Math.round(((summary.upCount ?? 0) / summary.totalAssets) * 100) : 0;
  const downRatio = summary?.totalAssets ? Math.round(((summary.downCount ?? 0) / summary.totalAssets) * 100) : 0;
  const flatRatio = summary?.totalAssets ? Math.max(100 - upRatio - downRatio, 0) : 0;
  const topUpItems = summary?.topUp?.slice(0, 2) ?? [];
  const topDownItems = summary?.topDown?.slice(0, 2) ?? [];

  return (
    <div className="w-full bg-stone-100 px-4 py-4 lg:px-5">
      <div className="mx-auto grid max-w-[1760px] gap-4 xl:grid-cols-[minmax(0,1fr)_390px_360px]">
        <section className="rounded-[10px] bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-[17px] font-bold text-stone-900">자산 리스트</h2>
          <div className="mb-3">
            <TabSwitcher items={SORT_ITEMS} active={chartFilter} onChange={setChartFilter} variant="pill" />
          </div>

          <div className="grid grid-cols-[2.2fr_1fr_1fr_1.1fr] border-b border-stone-200 px-2 py-3 text-[13px] font-bold text-stone-500">
            <span className="font-bold text-stone-600">종목</span>
            <span className="text-right font-bold text-stone-600">현재가</span>
            <span className="text-right font-bold text-stone-600">등락률</span>
            <span className="text-right font-bold text-stone-600">당일 거래대금</span>
          </div>

          <div>
            {loading ? (
              <div className="py-12 text-center text-base font-semibold text-stone-400">데이터를 불러오는 중입니다.</div>
            ) : tokens.length === 0 ? (
              <div className="py-12 text-center text-base font-semibold text-stone-400">표시할 자산이 없습니다.</div>
            ) : (
              tokens.map((token, index) => (
                <div
                  key={token.tokenId}
                  className={cn(
                    "grid cursor-pointer grid-cols-[2.2fr_1fr_1fr_1.1fr] items-center border-b border-stone-100 px-2 py-3 transition-colors hover:bg-stone-50",
                    (token.fluctuationRate ?? 0) > 0 && "bg-[#FFF8F0]",
                    previewTokenId === token.tokenId && "bg-stone-100",
                  )}
                  onMouseEnter={() => setPreviewTokenId(token.tokenId)}
                  onClick={() => navigate(`/token/${token.tokenId}`)}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="w-5 shrink-0 text-[13px] font-bold text-stone-400">{page * PAGE_SIZE + index + 1}</span>
                    <button
                      onClick={async (event) => {
                        event.stopPropagation();
                        if (!user) {
                          showGuestBanner("관심 종목과 계좌 기능은 로그인 후 이용할 수 있습니다.");
                          return;
                        }
                        try {
                          await toggleLike(token.tokenId);
                        } catch (error) {
                          console.error("[Dashboard] like toggle failed:", error);
                        }
                      }}
                      className={cn(
                        "flex h-9 w-9 shrink-0 items-center justify-center rounded-md transition-colors",
                        likedTokenIds.includes(token.tokenId)
                          ? "bg-brand-red-light/70 text-brand-red"
                          : "bg-stone-100 text-stone-400 hover:bg-stone-200 hover:text-brand-red",
                      )}
                    >
                      <Heart size={14} fill={likedTokenIds.includes(token.tokenId) ? "currentColor" : "none"} />
                    </button>
                    <AssetAvatar
                      symbol={token.tokenSymbol}
                      src={token.imgUrl}
                      alt={token.assetName}
                      size="md"
                      variant="light"
                      className="shrink-0"
                    />
                    <div className="min-w-0">
                      <p className="truncate text-[15px] font-bold text-stone-900">{token.assetName}</p>
                      <p className="truncate text-[12px] font-semibold text-stone-400">{token.tokenSymbol || "-"}</p>
                    </div>
                  </div>
                  <span className="text-right text-[15px] font-bold text-stone-800">{(token.currentPrice ?? 0).toLocaleString()}원</span>
                  <span className={cn("text-right text-[15px] font-bold", (token.fluctuationRate ?? 0) > 0 ? "text-brand-red" : (token.fluctuationRate ?? 0) < 0 ? "text-brand-blue" : "text-stone-500")}>
                    {(token.fluctuationRate ?? 0) > 0 ? "+" : ""}{token.fluctuationRate ?? 0}%
                  </span>
                  <span className="text-right text-[15px] font-semibold text-stone-600">{(token.totalTradeValue ?? 0).toLocaleString()}원</span>
                </div>
              ))
            )}
          </div>

          <div className="flex items-center justify-center gap-4 pt-4 text-[14px] font-semibold text-stone-400">
            <button onClick={() => setPage((prev) => Math.max(0, prev - 1))} disabled={page === 0 || loading}>이전</button>
            <span className="font-bold text-stone-900">{page + 1}</span>
            <button onClick={() => setPage((prev) => prev + 1)} disabled={!hasNext || loading}>다음</button>
          </div>
        </section>

        <section className="rounded-[10px] bg-white p-5 shadow-sm">
          {previewToken ? (
            <>
              <div className="mb-4 flex items-center gap-3">
                <AssetAvatar
                  symbol={previewToken.tokenSymbol}
                  src={previewToken.imgUrl}
                  alt={previewToken.assetName}
                  size="lg"
                  variant="light"
                  className="shrink-0"
                />
                <div className="min-w-0">
                  <div className="text-[18px] font-bold text-stone-900">{previewToken.assetName}</div>
                  <div className="text-[13px] font-semibold text-stone-400">{previewToken.tokenSymbol || "-"}</div>
                </div>
              </div>

              <div>
                <span className="text-[28px] font-bold text-stone-900">{(previewToken.currentPrice ?? 0).toLocaleString()}원</span>
                <span className={cn(
                  "ml-2 text-[16px] font-bold",
                  (previewToken.fluctuationRate ?? 0) > 0 ? "text-brand-red" : (previewToken.fluctuationRate ?? 0) < 0 ? "text-brand-blue" : "text-stone-500",
                )}>
                  {(previewToken.fluctuationRate ?? 0) > 0 ? "+" : ""}{previewToken.fluctuationRate ?? 0}%
                </span>
              </div>
              <div className="mb-4 mt-1 text-[12px] font-semibold text-stone-400">기준가 대비 변동</div>

              <div className="mb-4">
                <div className="mb-2 flex items-center justify-between text-[13px] font-bold text-stone-500">
                  <span>1분봉 차트</span>
                  <span className="inline-flex items-center gap-1 text-[11px] font-bold text-green-500">
                    <span className="h-1.5 w-1.5 rounded-full bg-green-500" />
                    실시간
                  </span>
                </div>
                {validData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={176} minWidth={0}>
                    <ComposedChart data={candleData} margin={{ top: 6, right: 8, bottom: 4, left: 0 }}>
                      <YAxis
                        orientation="right"
                        width={58}
                        domain={[yMin - yPad, yMax + yPad]}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 11, fontWeight: 700, fill: "#78716c" }}
                        tickFormatter={(value) => `${Math.round(value).toLocaleString()}`}
                      />
                      <XAxis
                        dataKey="time"
                        height={24}
                        tickLine={false}
                        axisLine={false}
                        minTickGap={20}
                        tickMargin={8}
                        tick={{ fontSize: 11, fontWeight: 700, fill: "#78716c" }}
                      />
                      <Tooltip
                        cursor={{ stroke: "#d6d3d1", strokeWidth: 1 }}
                        content={({ active, payload, label }) => {
                          if (!active || !payload?.[0]?.payload) return null;
                          const d = payload[0].payload;
                          return (
                            <div style={{ background: "#fff", border: "1px solid #e7e5e4", borderRadius: 8, fontSize: 10, padding: "6px 10px" }}>
                              <p style={{ color: "#a8a29e", fontWeight: 700, marginBottom: 2 }}>{label}</p>
                              <p style={{ color: d.close >= d.open ? "#e54d4d" : "#3b82f6", fontFamily: "monospace", fontWeight: 700 }}>
                                시 {d.open?.toLocaleString()} 고 {d.high?.toLocaleString()} 저 {d.low?.toLocaleString()} 종 {d.close?.toLocaleString()}
                              </p>
                            </div>
                          );
                        }}
                      />
                      <Bar dataKey={d => d.open == null ? [0, 0] : [d.low, d.high]} shape={<CandlestickShape />} isAnimationActive={false} />
                    </ComposedChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex h-[176px] items-center justify-center rounded-lg bg-[#F8F8F6] text-[13px] font-semibold text-stone-300">
                    체결 기록이 없습니다.
                  </div>
                )}
              </div>

              <div className="rounded-lg bg-stone-50 p-4">
                <div className="mb-3 text-[13px] font-bold text-stone-800">토큰 상세</div>
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-4 text-[13px]">
                    <span className="font-semibold text-stone-500">총발행량</span>
                    <span className="font-bold text-stone-900">{(previewToken.totalSupply ?? 0).toLocaleString()}</span>
                  </div>
                  <div className="flex items-center justify-between gap-4 text-[13px]">
                    <span className="font-semibold text-stone-500">유통량</span>
                    <span className="font-bold text-stone-900">{(previewToken.circulatingSupply ?? 0).toLocaleString()}</span>
                  </div>
                  <div className="flex items-center justify-between gap-4 text-[13px]">
                    <span className="font-semibold text-stone-500">초기발행가</span>
                    <span className="font-bold text-stone-900">{(previewToken.initPrice ?? 0).toLocaleString()}원</span>
                  </div>
                </div>
              </div>

              <div className="mt-4 rounded-lg bg-[#F5F0FF] p-4">
                <div className="mb-3 flex items-center gap-2">
                  <div className="flex h-7 w-7 items-center justify-center rounded-md bg-violet-500 text-white">
                    <Sparkles size={14} />
                  </div>
                  <div>
                    <div className="text-[12px] font-bold text-violet-500">GEMINI 요약</div>
                    <div className="text-[11px] font-semibold text-stone-500">AI가 정리한 자산 핵심 정보</div>
                  </div>
                </div>
                <div className="text-[13px] font-bold leading-6 text-stone-700">{previewToken.aiSummary || "AI 요약이 아직 없습니다."}</div>
                <div className="mt-2 text-[11px] font-semibold text-stone-400">업데이트 {aiSummaryUpdatedAtText || "-"}</div>
              </div>
            </>
          ) : (
            <div className="flex h-full min-h-[400px] items-center justify-center text-base font-semibold text-stone-300">자산을 선택해 주세요.</div>
          )}
        </section>

        <aside className="flex flex-col gap-4">
          <section className="rounded-[10px] bg-white p-5 shadow-sm">
            <h3 className="mb-3 text-[16px] font-bold text-stone-900">오늘의 요약</h3>
            <div className="mb-2 text-[13px] font-bold text-stone-500">시장 분포</div>
            <div className="mb-4 flex h-2 overflow-hidden rounded-full bg-stone-200">
              <div className="bg-brand-red" style={{ width: `${upRatio}%` }} />
              <div className="bg-stone-300" style={{ width: `${flatRatio}%` }} />
              <div className="bg-brand-blue" style={{ width: `${downRatio}%` }} />
            </div>

            <div className="mb-2 text-[13px] font-bold text-brand-red">상승 TOP{topUpItems.length}</div>
            {topUpItems.length > 0 ? topUpItems.map((item) => (
              <div
                key={item.tokenId}
                className="mb-2 flex cursor-pointer items-center justify-between rounded-md bg-[#FFF5F5] px-4 py-3"
                onClick={() => navigate(`/token/${item.tokenId}`)}
              >
                <span className="text-[14px] font-bold text-stone-800">{item.assetName}</span>
                <span className="text-[15px] font-bold text-brand-red">+{item.fluctuationRate}%</span>
              </div>
            )) : null}

            <div className="mt-4 mb-2 text-[13px] font-bold text-brand-blue">하락 TOP{topDownItems.length}</div>
            {topDownItems.length > 0 ? topDownItems.map((item) => (
              <div
                key={item.tokenId}
                className="mb-2 flex cursor-pointer items-center justify-between rounded-md bg-[#EFF6FF] px-4 py-3"
                onClick={() => navigate(`/token/${item.tokenId}`)}
              >
                <span className="text-[14px] font-bold text-stone-800">{item.assetName}</span>
                <span className="text-[15px] font-bold text-brand-blue">{item.fluctuationRate}%</span>
              </div>
            )) : (
              <div className="rounded-md bg-[#F8F8F6] px-4 py-4 text-center text-[14px] font-semibold text-stone-400">하락 종목 없음</div>
            )}
          </section>

          <section className="rounded-[10px] bg-white p-5 shadow-sm">
            <h3 className="mb-3 text-[16px] font-bold text-stone-900">STO 뉴스</h3>
            {newsLoading ? (
              <div className="py-8 text-center text-base font-semibold text-stone-400">뉴스를 불러오는 중입니다.</div>
            ) : newsError ? (
              <div className="py-8 text-center text-base font-semibold text-stone-400">{newsError}</div>
            ) : newsItems.length === 0 ? (
              <div className="py-8 text-center text-base font-semibold text-stone-400">표시할 뉴스가 없습니다.</div>
            ) : (
              <div className="max-h-[290px] overflow-y-auto pr-1">
                {newsItems.map((item, index) => (
                  <a
                    key={`${item.link}-${index}`}
                    href={item.link}
                    target="_blank"
                    rel="noreferrer"
                    className={cn(
                      "mb-3 block rounded-md border-l-4 bg-[#F8F8F6] px-4 py-3 last:mb-0",
                      index % 3 === 0 && "border-l-brand-gold",
                      index % 3 === 1 && "border-l-blue-600",
                      index % 3 === 2 && "border-l-green-500",
                    )}
                  >
                    <div className="mb-1 text-[14px] font-bold text-stone-900">{item.title}</div>
                    <div
                      className="mb-2 overflow-hidden text-[12px] font-semibold leading-5 text-stone-500"
                      style={{
                        display: "-webkit-box",
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: "vertical",
                      }}
                    >
                      {item.description || "기사 요약이 없습니다."}
                    </div>
                    <div className="text-[11px] font-semibold text-stone-400">{formatNewsDate(item.pubDate)}</div>
                  </a>
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>
    </div>
  );
}

