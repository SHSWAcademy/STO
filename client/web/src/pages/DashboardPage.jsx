import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Heart } from "lucide-react";
import { ResponsiveContainer, ComposedChart, Bar, YAxis, XAxis, Tooltip } from "recharts";
import { useApp } from "../context/AppContext.jsx";
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

// ── 캔들 유틸 ─────────────────────────────────────────────────────
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

// ── 캔들스틱 shape ────────────────────────────────────────────────
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

// ── 메인 컴포넌트 ─────────────────────────────────────────────────
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

  const tokenIds = useMemo(() => tokens.map((token) => token.tokenId), [tokens]);

  useEffect(() => {
    setPage(0);
  }, [chartFilter]);

  // 토큰 목록
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

  // 1분봉 캔들 초기 로드 (선택 토큰 변경 시)
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

  return (
    <div className="mx-auto max-w-[1200px] space-y-6">
      <div className="flex flex-col gap-8 pt-4 lg:flex-row">
        <div className="min-w-0 flex-1 space-y-6">
          <h2 className="text-xl font-black text-stone-800">차트</h2>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-stone-200 pb-4">
              <TabSwitcher items={SORT_ITEMS} active={chartFilter} onChange={setChartFilter} />
            </div>

            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-stone-200 text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  <th className="py-4 text-left font-medium">종목</th>
                  <th className="py-4 text-right font-medium">현재가</th>
                  <th className="py-4 text-right font-medium">
                    <span className="block">등락률</span>
                    <span className="block text-[9px] font-bold normal-case tracking-normal text-stone-700">(전날 종가 대비 현재 가격)</span>
                  </th>
                  <th className="py-4 text-right font-medium">당일 총거래대금</th>
                  <th className="py-4 text-right font-medium">당일 총거래량</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-200">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-stone-400">
                      데이터를 불러오는 중입니다.
                    </td>
                  </tr>
                ) : tokens.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-stone-400">
                      표시할 종목이 없습니다.
                    </td>
                  </tr>
                ) : (
                  tokens.map((token, index) => (
                    <tr
                      key={token.tokenId}
                      className={cn(
                        "group cursor-pointer border-y border-transparent transition-all duration-200 hover:border-stone-200 hover:bg-white hover:shadow-[0_10px_30px_rgba(28,25,23,0.06)]",
                        previewTokenId === token.tokenId && "border-stone-200 bg-white shadow-[0_12px_32px_rgba(28,25,23,0.08)]",
                      )}
                      onMouseEnter={() => setPreviewTokenId(token.tokenId)}
                      onClick={() => navigate(`/token/${token.tokenId}`)}
                    >
                      <td className="py-0">
                        <div className="flex min-h-[72px] items-stretch">
                          <div
                            className="flex w-20 shrink-0 items-center justify-center"
                            onMouseDown={(event) => event.stopPropagation()}
                            onClick={(event) => event.stopPropagation()}
                          >
                            <button
                              onClick={async (event) => {
                                event.stopPropagation();
                                if (!user) {
                                  showGuestBanner("관심 종목, 내 계좌 등 개인 기능은 로그인 후 이용할 수 있어요.");
                                  return;
                                }
                                try {
                                  await toggleLike(token.tokenId);
                                } catch (error) {
                                  console.error("[Dashboard] like toggle failed:", error);
                                }
                              }}
                              className={cn(
                                "flex h-10 w-10 items-center justify-center rounded-full transition-colors",
                                likedTokenIds.includes(token.tokenId)
                                  ? "bg-brand-red-light/70 text-brand-red"
                                  : "text-stone-400 hover:bg-stone-200 hover:text-brand-red",
                              )}
                            >
                              <Heart
                                size={16}
                                fill={likedTokenIds.includes(token.tokenId) ? "currentColor" : "none"}
                              />
                            </button>
                          </div>

                          <div className="flex min-w-0 items-center gap-4 py-4">
                            <span className="w-4 font-mono text-stone-400">{page * PAGE_SIZE + index + 1}</span>
                            <AssetAvatar
                              symbol={token.tokenSymbol}
                              src={token.imgUrl}
                              alt={token.assetName}
                              size="md"
                              variant="light"
                              className="shrink-0"
                            />
                            <div className="min-w-0">
                              <p className="truncate font-bold text-stone-800 transition-colors group-hover:text-stone-900">
                                {token.assetName}
                              </p>
                              <p className="mt-0.5 truncate font-mono text-[11px] font-bold text-stone-400">
                                {token.tokenSymbol || "-"}
                              </p>
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className={cn(
                        "py-4 text-right font-mono font-bold text-stone-800",
                        priceFlash[token.tokenId] === 'red'  && 'hoga-flash-red',
                        priceFlash[token.tokenId] === 'blue' && 'hoga-flash-blue',
                      )}>
                        {(token.currentPrice ?? 0).toLocaleString()}원
                      </td>
                      <td className={cn("py-4 text-right font-bold", (token.fluctuationRate ?? 0) >= 0 ? "text-brand-red" : "text-brand-blue")}>
                        {(token.fluctuationRate ?? 0) >= 0 ? "+" : ""}
                        {token.fluctuationRate ?? 0}%
                      </td>
                      <td className="py-4 text-right font-mono text-stone-500">
                        {(token.totalTradeValue ?? 0).toLocaleString()}원
                      </td>
                      <td className="py-4 text-right font-mono text-stone-500">
                        {(token.totalTradeQuantity ?? 0).toLocaleString()}주
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            <div className="flex items-center justify-center gap-4 pt-2">
              <button
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                disabled={page === 0 || loading}
                className="rounded-lg border border-stone-200 px-4 py-2 text-sm font-bold transition-colors hover:bg-stone-100 disabled:opacity-30"
              >
                이전
              </button>
              <span className="font-mono text-sm text-stone-500">{page + 1}</span>
              <button
                onClick={() => setPage((prev) => prev + 1)}
                disabled={!hasNext || loading}
                className="rounded-lg border border-stone-200 px-4 py-2 text-sm font-bold transition-colors hover:bg-stone-100 disabled:opacity-30"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        {/* ── 우측 미리보기 패널 ──────────────────────────────────── */}
        <div className="w-full min-w-0 lg:w-[380px]">
          <div className="sticky top-24 rounded-xl border border-stone-200 bg-white p-8 shadow-sm">
            {previewToken ? (
              <>
                <div className="mb-6 flex items-start gap-4">
                  <AssetAvatar
                    symbol={previewToken.tokenSymbol}
                    src={previewToken.imgUrl}
                    alt={previewToken.assetName}
                    size="lg"
                    variant="light"
                    className="shrink-0"
                  />
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate text-xl font-black text-stone-800">{previewToken.assetName}</h3>
                    <p className="mt-1 font-mono text-xs font-bold text-stone-400">
                      {previewToken.tokenSymbol || "-"}
                    </p>
                    <p className="mt-2 text-sm font-bold">
                      <span className="font-mono text-stone-800">
                        {(previewToken.currentPrice ?? 0).toLocaleString()}원
                      </span>
                      <span className={cn("ml-2", (previewToken.fluctuationRate ?? 0) >= 0 ? "text-brand-red" : "text-brand-blue")}>
                        {(previewToken.fluctuationRate ?? 0) >= 0 ? "+" : ""}
                        {previewToken.fluctuationRate ?? 0}%
                      </span>
                    </p>
                    <p className="mt-0.5 text-[9px] font-bold text-stone-400">(전날 종가 대비)</p>
                  </div>
                </div>

                {/* 1분봉 캔들차트 */}
                <div className="mb-6">
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">1분봉 차트</p>
                    <span className="inline-flex items-center gap-1 text-[9px] font-bold text-green-500">
                      <span className="inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-green-500" />
                      실시간
                    </span>
                  </div>
                  {validData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={200} minWidth={0}>
                      <ComposedChart data={candleData} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
                        <YAxis
                          domain={[yMin - yPad, yMax + yPad]}
                          tick={{ fontSize: 9, fill: '#a8a29e' }}
                          tickFormatter={v => v.toLocaleString()}
                          width={54}
                          axisLine={false}
                          tickLine={false}
                          tickCount={4}
                        />
                        <XAxis
                          dataKey="time"
                          tick={{ fontSize: 9, fill: '#a8a29e' }}
                          interval="preserveStartEnd"
                          axisLine={false}
                          tickLine={false}
                        />
                        <Tooltip
                          cursor={{ stroke: '#d6d3d1', strokeWidth: 1 }}
                          content={({ active, payload, label }) => {
                            if (!active || !payload?.[0]?.payload) return null;
                            const d = payload[0].payload;
                            if (d.open == null) return null;
                            const isUp = d.close >= d.open;
                            return (
                              <div style={{ background: '#fff', border: '1px solid #e7e5e4', borderRadius: 8, fontSize: 10, padding: '6px 10px' }}>
                                <p style={{ color: '#a8a29e', fontWeight: 700, marginBottom: 2 }}>{label}</p>
                                <p style={{ color: isUp ? '#e54d4d' : '#3b82f6', fontFamily: 'monospace', fontWeight: 700 }}>
                                  시 {d.open?.toLocaleString()}  고 {d.high?.toLocaleString()}  저 {d.low?.toLocaleString()}  종 {d.close?.toLocaleString()}
                                </p>
                              </div>
                            );
                          }}
                        />
                        <Bar
                          dataKey={d => d.open == null ? [0, 0] : [d.low, d.high]}
                          shape={<CandlestickShape />}
                          isAnimationActive={false}
                        />
                      </ComposedChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="flex h-[200px] items-center justify-center text-sm text-stone-300">
                      {candleLoading ? '로딩 중..' : '체결 기록이 없습니다.'}
                    </div>
                  )}
                </div>

                <button
                  onClick={() => navigate(`/token/${previewToken.tokenId}`)}
                  className="w-full rounded-lg bg-stone-800 py-4 font-black uppercase tracking-widest text-white transition-colors hover:bg-stone-700"
                >
                  거래하기
                </button>
              </>
            ) : (
              <div className="flex h-64 items-center justify-center text-sm text-stone-300">
                종목을 선택해 주세요.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
