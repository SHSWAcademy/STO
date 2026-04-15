// TokenDetailPage - 종목 상세 페이지
// /token/:tokenId 로 접근
// 실데이터 기반 상세 화면

import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams } from 'react-router-dom';
import {
  ResponsiveContainer, ComposedChart, BarChart, Bar, Cell,
  XAxis, YAxis, Tooltip, CartesianGrid,
} from 'recharts';
import {
  Maximize2, X, Target,
} from 'lucide-react';

import { useApp }            from '../context/AppContext.jsx';
import { useTradingSocket }  from '../hooks/useTradingSocket.js';
import { AssetHeader }       from '../components/trading/AssetHeader.jsx';
import { SecureOrderPanel }  from '../components/trading/SecureOrderPanel.jsx';
import { HogaRow }           from '../components/trading/HogaRow.jsx';
import { PriceRow }          from '../components/trading/PriceRow.jsx';
import { cn } from '../lib/utils.js';
import { API_BASE_URL } from '../lib/config.js';
import api from '../lib/api.js';

const API = API_BASE_URL;

// JWT payload에서 memberId(sub 클레임) 추출
function parseJwtMemberId(token) {
  if (!token) return null;
  try {
    const payloadPart = token.split('.')[1];
    if (!payloadPart) return null;
    const base64 = payloadPart
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(payloadPart.length / 4) * 4, '=');
    const payload = JSON.parse(atob(base64));
    const memberId = Number(payload?.sub);
    return Number.isFinite(memberId) && Number.isInteger(memberId) ? memberId : null;
  } catch {
    return null;
  }
}

const PERIOD_TO_TYPE = {
  '분':  'MINUTE',
  '시간': 'HOUR',
  '일':  'DAY',
  '월':  'MONTH',
  '년':  'YEAR',
};
const CHART_PERIODS = ['분', '시간', '일', '월', '년'];
const CHART_SYNC_ID = 'token-detail-candle-volume';

// ── 유틸 ────────────────────────────────────────────────────────
function formatCandleTime(candleTime, period) {
  if (!candleTime) return '';
  const d = new Date(candleTime);
  if (period === '분' || period === '시간') return d.toTimeString().slice(0, 5);
  if (period === '일') return `${d.getMonth() + 1}/${d.getDate()}`;
  if (period === '월') return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}`;
  if (period === '년') return `${d.getFullYear()}`;
  return d.toTimeString().slice(0, 5);
}

// WebSocket tradeTime 포맷 (Jackson 배열 [y,mo,d,h,m,s] 또는 ISO 문자열 모두 처리)
function formatTradeTime(t) {
  if (!t) return '';
  if (Array.isArray(t)) {
    const [, , , h = 0, m = 0, s = 0] = t;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  try {
    return new Date(t).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  } catch { return String(t); }
}

function mapCandle(dto, period) {
  return {
    ts:    dto.candleTime ? new Date(dto.candleTime).getTime() : 0,
    time:  formatCandleTime(dto.candleTime, period),
    open:  Math.round(dto.openPrice  || 0),
    high:  Math.round(dto.highPrice  || 0),
    low:   Math.round(dto.lowPrice   || 0),
    close: Math.round(dto.closePrice || 0),
    vol:   Math.round(dto.volume     || 0),
    isSynthetic: false,
  };
}

function getBucketStart(date, period) {
  const d = new Date(date);
  if (period === '분') {
    d.setSeconds(0, 0);
  } else if (period === '시간') {
    d.setMinutes(0, 0, 0);
  } else if (period === '일') {
    d.setHours(0, 0, 0, 0);
  } else if (period === '월') {
    d.setDate(1);
    d.setHours(0, 0, 0, 0);
  } else if (period === '년') {
    d.setMonth(0, 1);
    d.setHours(0, 0, 0, 0);
  }
  return d;
}

function shiftBucket(date, period, amount) {
  const d = new Date(date);
  if (period === '분') d.setMinutes(d.getMinutes() + amount);
  else if (period === '시간') d.setHours(d.getHours() + amount);
  else if (period === '일') d.setDate(d.getDate() + amount);
  else if (period === '월') d.setMonth(d.getMonth() + amount);
  else if (period === '년') d.setFullYear(d.getFullYear() + amount);
  return d;
}

function generateRecentSlots(period, count = 35) {
  const currentBucket = getBucketStart(new Date(), period);
  const slots = [];
  for (let i = count - 1; i >= 0; i--) {
    const d = shiftBucket(currentBucket, period, -i);
    slots.push({
      ts: d.getTime(),
      time: formatCandleTime(d, period),
    });
  }
  return slots;
}

function aggregateCandlesFromDay(dayCandles, targetPeriod) {
  const groups = new Map();
  dayCandles
      .filter(c => c?.ts)
      .sort((a, b) => a.ts - b.ts)
      .forEach((candle) => {
        const bucket = getBucketStart(new Date(candle.ts), targetPeriod);
        const key = bucket.getTime();
        const existing = groups.get(key);
        if (!existing) {
          groups.set(key, {
            ts: key,
            time: formatCandleTime(bucket, targetPeriod),
            open: candle.open,
            high: candle.high,
            low: candle.low,
            close: candle.close,
            vol: candle.vol ?? 0,
            isSynthetic: false,
          });
          return;
        }
        existing.high = Math.max(existing.high, candle.high);
        existing.low = Math.min(existing.low, candle.low);
        existing.close = candle.close;
        existing.vol += candle.vol ?? 0;
      });
  return [...groups.values()].sort((a, b) => a.ts - b.ts);
}

function buildChartData(fetchedCandles, period, seedClose = null) {
  const slots = generateRecentSlots(period);
  const candleByTs = new Map(
      fetchedCandles
          .filter(c => c?.ts)
          .sort((a, b) => a.ts - b.ts)
          .map(c => [c.ts, { ...c, time: formatCandleTime(c.ts, period), isSynthetic: false }])
  );

  let previousClose = seedClose;
  return slots.map((slot) => {
    const candle = candleByTs.get(slot.ts);
    if (candle) {
      previousClose = candle.close;
      return candle;
    }
    if (previousClose == null) {
      return { ...slot, open: null, high: null, low: null, close: null, vol: null, isSynthetic: true };
    }
    return {
      ...slot,
      open: previousClose,
      high: previousClose,
      low: previousClose,
      close: previousClose,
      vol: 0,
      isSynthetic: true,
    };
  });
}

// ── 캔들스틱 shape ───────────────────────────────────────────────
// dataKey={d => [d.low, d.high]} 와 함께 사용
// Recharts: y = high 의 픽셀 위치(위쪽), y+height = low 의 픽셀 위치(아래쪽)
// → Y축 auto domain이 high/low 전체를 포함하므로 wick 이 잘리지 않음
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
          <line
              x1={x + 1}
              y1={flatY}
              x2={x + Math.max(width - 1, 1)}
              y2={flatY}
              stroke={color}
              strokeWidth={isSynthetic ? 1 : 1.4}
              strokeLinecap="round"
          />
        </g>
    );
  }

  const ratio = height / priceRange;  // px per price unit

  // y      = high 픽셀 (차트 위쪽)
  // y+height = low 픽셀 (차트 아래쪽)
  const highPx = y;
  const lowPx  = y + height;

  const bodyTopPx    = y + (high - Math.max(open, close)) * ratio;
  const bodyBottomPx = y + (high - Math.min(open, close)) * ratio;
  const bodyH        = Math.max(bodyBottomPx - bodyTopPx, 1);

  return (
      <g>
        {/* 위 꼬리: high → body 상단 */}
        <line x1={cx} y1={highPx} x2={cx} y2={bodyTopPx}
              stroke={color} strokeWidth={1.2} />
        {/* 아래 꼬리: body 하단 → low */}
        <line x1={cx} y1={bodyBottomPx} x2={cx} y2={lowPx}
              stroke={color} strokeWidth={1.2} />
        {/* 몸통 */}
        <rect x={x + 1} y={bodyTopPx} width={Math.max(width - 2, 1)} height={bodyH}
              fill={color} rx={1} />
      </g>
  );
}

function CandleVolumeChart({
  chartData,
  loading,
  displayData,
  yDomain,
  yTicks,
  rightMargin,
  yAxisWidth,
  volumeTickGap,
  emptyMessage,
  onHover,
  onLeave,
  resetKey,
}) {
  const [visibleRange, setVisibleRange] = useState({ startIndex: 0, endIndex: 0 });
  const dragRef = useRef(null);
  const prevResetKeyRef = useRef(resetKey);
  const hasInitializedRef = useRef(false);
  const [visibleCount, setVisibleCount] = useState(20);
  const minVisibleCount = 12;
  const maxVisibleCount = 35;

  useEffect(() => {
    const endIndex = Math.max(chartData.length - 1, 0);
    const startIndex = Math.max(endIndex - (visibleCount - 1), 0);
    const resetKeyChanged = prevResetKeyRef.current !== resetKey;

    if (!hasInitializedRef.current || resetKeyChanged) {
      hasInitializedRef.current = true;
      prevResetKeyRef.current = resetKey;
      setVisibleRange({ startIndex, endIndex });
      return;
    }

    setVisibleRange((current) => {
      if (chartData.length === 0) return { startIndex: 0, endIndex: 0 };
      if (current.startIndex === 0 && current.endIndex === 0 && endIndex > 0) {
        return { startIndex, endIndex };
      }
      if (current.endIndex <= endIndex && current.endIndex >= 0) {
        const clampedStart = Math.min(current.startIndex, Math.max(chartData.length - visibleCount, 0));
        const clampedEnd = Math.min(clampedStart + visibleCount - 1, endIndex);
        return { startIndex: clampedStart, endIndex: clampedEnd };
      }
      return { startIndex, endIndex };
    });
  }, [chartData.length, visibleCount, resetKey]);

  useEffect(() => {
    const handleMouseMove = (event) => {
      const drag = dragRef.current;
      if (!drag) return;
      const deltaX = event.clientX - drag.startX;
      const candleWidth = 18;
      const shift = Math.round(deltaX / candleWidth);
      if (shift === 0) return;

      const maxStart = Math.max(chartData.length - visibleCount, 0);
      const nextStart = Math.min(Math.max(drag.startRange.startIndex - shift, 0), maxStart);
      const nextEnd = Math.min(nextStart + visibleCount - 1, Math.max(chartData.length - 1, 0));
      setVisibleRange({ startIndex: nextStart, endIndex: nextEnd });
    };

    const handleMouseUp = () => {
      dragRef.current = null;
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [chartData.length, visibleCount]);

  const visibleData = chartData.slice(visibleRange.startIndex, visibleRange.endIndex + 1);

  const beginDrag = (event) => {
    if (chartData.length <= visibleCount) return;
    dragRef.current = {
      startX: event.clientX,
      startRange: visibleRange,
    };
  };

  const handleWheelZoom = (event) => {
    event.preventDefault();
    if (chartData.length <= minVisibleCount) return;

    const direction = event.deltaY > 0 ? 1 : -1;
    setVisibleCount((prev) => {
      const next = Math.min(Math.max(prev + direction * 2, minVisibleCount), Math.min(maxVisibleCount, Math.max(chartData.length, minVisibleCount)));
      if (next === prev) return prev;

      setVisibleRange((current) => {
        const center = Math.floor((current.startIndex + current.endIndex) / 2);
        const half = Math.floor(next / 2);
        const maxStart = Math.max(chartData.length - next, 0);
        const startIndex = Math.min(Math.max(center - half, 0), maxStart);
        const endIndex = Math.min(startIndex + next - 1, Math.max(chartData.length - 1, 0));
        return { startIndex, endIndex };
      });
      return next;
    });
  };

  return (
      <>
        <div className="px-6 pt-4 pb-1 flex items-center gap-3">
          {[
            { label: '시', val: displayData?.open },
            { label: '고', val: displayData?.high, color: 'var(--color-brand-red)' },
            { label: '저', val: displayData?.low,  color: 'var(--color-brand-blue)' },
            { label: '종', val: displayData?.close },
          ].map(({ label, val, color }) => (
              <div key={label} className="flex items-center gap-1">
                <span className="text-[10px] font-bold text-stone-400">{label}</span>
                <span
                    className="text-[11px] font-mono font-bold"
                    style={{ color: color ?? 'var(--color-stone-800)' }}
                >
                  {val?.toLocaleString() ?? '-'}
                </span>
              </div>
          ))}
        </div>

        {chartData.length === 0 ? (
            <div className="flex-1 flex items-center justify-center text-stone-400 text-sm font-bold">
              {loading ? '데이터 로딩 중...' : emptyMessage}
            </div>
        ) : (
            <>
              <div className="flex-1 px-2 cursor-grab active:cursor-grabbing" onMouseDown={beginDrag} onWheel={handleWheelZoom}>
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart
                      syncId={CHART_SYNC_ID}
                      data={visibleData}
                      margin={{ top: 8, right: rightMargin, left: 0, bottom: 0 }}
                      onMouseMove={e => {
                        if (e?.activePayload?.length > 0) onHover(e.activePayload[0].payload);
                      }}
                      onMouseLeave={onLeave}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0efee" vertical={false} />
                    <XAxis dataKey="time" hide />
                    <YAxis
                        orientation="right"
                        domain={yDomain}
                        ticks={yTicks}
                        tick={{ fontSize: 10, fill: '#a8a29e', fontWeight: 'bold' }}
                        axisLine={false}
                        tickLine={false}
                        tickFormatter={v => v.toLocaleString()}
                        width={yAxisWidth}
                    />
                    <Tooltip
                        contentStyle={{
                          background: '#fafaf9',
                          border: '1px solid #e7e5e4',
                          borderRadius: 12,
                          fontSize: 11,
                          color: '#292524',
                          boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
                        }}
                        formatter={(value, name, props) => {
                          const d = props?.payload;
                          if (!d) return [value, name];
                          return [
                            `시 ${d.open?.toLocaleString()}  고 ${d.high?.toLocaleString()}  저 ${d.low?.toLocaleString()}  종 ${d.close?.toLocaleString()}`,
                            d.time,
                          ];
                        }}
                        labelFormatter={() => ''}
                    />
                    <Bar
                        dataKey={d => [d.low, d.high]}
                        shape={<CandlestickShape />}
                        animationDuration={0}
                        isAnimationActive={false}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>

              <div className="h-[72px] px-2 border-t border-stone-100 cursor-grab active:cursor-grabbing" onMouseDown={beginDrag} onWheel={handleWheelZoom}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                      syncId={CHART_SYNC_ID}
                      data={visibleData}
                      margin={{ top: 2, right: rightMargin, left: 0, bottom: 0 }}
                  >
                    <XAxis
                        dataKey="time"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fontSize: 9, fill: '#a8a29e' }}
                        minTickGap={volumeTickGap}
                        interval="preserveStartEnd"
                    />
                    <YAxis
                        orientation="right"
                        domain={[0, 'auto']}
                        axisLine={false}
                        tickLine={false}
                        tick={false}
                        width={yAxisWidth}
                    />
                    <Bar dataKey="vol" radius={[2, 2, 0, 0]} isAnimationActive={false}>
                      {visibleData.map((d, i) => (
                          <Cell
                              key={i}
                              fill={
                                d.close == null || d.open == null ? '#d6d3d1'
                                  : d.close >= d.open ? 'rgba(239,68,68,0.45)'
                                  : 'rgba(59,130,246,0.45)'
                              }
                          />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </>
        )}
      </>
  );
}

// ── 로그인 필요 팝업 ────────────────────────────────────────────
function LoginRequiredModal({ message, onClose }) {
  return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
           onClick={onClose}>
        <div className="bg-white rounded-2xl border border-stone-200 shadow-xl p-8 w-80 text-center"
             onClick={e => e.stopPropagation()}>
          <p className="text-sm font-bold text-stone-800 mb-6">{message}</p>
          <button
              onClick={onClose}
              className="w-full py-3 bg-stone-800 text-white text-sm font-black rounded-xl hover:bg-stone-700 transition-colors"
          >
            확인
          </button>
        </div>

      </div>
  );
}

// ── TokenDetailPage ───────────────────────────────────────────────────
export function TokenDetailPage() {
  const { tokenId }    = useParams();
  const { user, likedTokenIds, toggleLike } = useApp();
  const TOKEN_ID = Number(tokenId) || 1;
  const memberId = parseJwtMemberId(user?.accessToken);

  // ── 로그인 필요 팝업 ─────────────────────────────────────────
  const [loginModal, setLoginModal] = useState(null); // null | string(message)

  // ── 탭 상태 ─────────────────────────────────────────────────
  const [activeTab, setActiveTab] = useState('chart');

  // ── 토큰 상세 정보 (백엔드) ──────────────────────────────────
  const [tokenInfo, setTokenInfo] = useState(null);
  const [selectedOrderPrice, setSelectedOrderPrice] = useState(null);
  const [hoveredAskIndex, setHoveredAskIndex] = useState(null);
  const [hoveredBidIndex, setHoveredBidIndex] = useState(null);

  useEffect(() => {
    api.get(`/api/token/${TOKEN_ID}/chart`)
        .then(r => setTokenInfo(r.data))
        .catch(e => console.warn('[TokenDetailPage] 토큰 상세 조회 실패:', e));
  }, [TOKEN_ID, user?.accessToken]);

  // ── 차트 상태 ────────────────────────────────────────────────
  const [chartPeriod, setChartPeriod]   = useState('분');
  const [chartData, setChartData]       = useState([]);
  const [hoveredData, setHoveredData]   = useState(null);
  const [loading, setLoading]           = useState(false);
  const [chartModalOpen, setChartModalOpen] = useState(false);

  // ── 종목 정보 탭 데이터 ─────────────────────────────────────
  const [tokenAssetInfo, setTokenAssetInfo] = useState(null);

  useEffect(() => {
    if (activeTab !== 'info' || tokenAssetInfo !== null) return;
    api.get(`/api/token/${TOKEN_ID}/info`)
        .then(r => setTokenAssetInfo(r.data))
        .catch(e => { console.warn('[TokenDetailPage] 종목정보 조회 실패:', e); setTokenAssetInfo({}); });
  }, [activeTab, TOKEN_ID, user?.accessToken]);

  // ── 배당금 / 공시 탭 데이터 ──────────────────────────────────
  const [allocations, setAllocations]   = useState(null);
  const [disclosures, setDisclosures]   = useState(null);

  useEffect(() => {
    if (activeTab !== 'dividend' || allocations !== null) return;
    api.get(`/api/token/${TOKEN_ID}/allocation`)
        .then(r => setAllocations(r.data))
        .catch(e => { console.warn('[TokenDetailPage] 배당금 조회 실패:', e); setAllocations([]); });
  }, [activeTab, TOKEN_ID, user?.accessToken]);

  useEffect(() => {
    if (activeTab !== 'news' || disclosures !== null) return;
    api.get(`/api/token/${TOKEN_ID}/disclosure`)
        .then(r => setDisclosures(r.data))
        .catch(e => { console.warn('[TokenDetailPage] 공시 조회 실패:', e); setDisclosures([]); });
  }, [activeTab, TOKEN_ID, user?.accessToken]);

  // ── 호가 / 체결 상태 ────────────────────────────────────────
  // 호가: WebSocket snapshot이 구독 즉시 전송하므로 빈 배열로 초기화
  const [asks, setAsks]             = useState([]);
  const [bids, setBids]             = useState([]);
  const [executions, setExecutions] = useState([]);
  const [trades, setTrades]         = useState([]);
  const [todayHigh, setTodayHigh]   = useState(null);
  const [todayLow, setTodayLow]     = useState(null);

  useEffect(() => {
    setTodayHigh(tokenInfo?.todayHighPrice ?? null);
    setTodayLow(tokenInfo?.todayLowPrice ?? null);
  }, [tokenInfo]);

  // ── 체결 목록 초기 REST 로드 ─────────────────────────────────
  useEffect(() => {
    api.get(`/api/token/${TOKEN_ID}/trades`)
        .then(r => {
          setTrades(r.data.map(d => ({
            price:      d.tradePrice,
            qty:        d.tradeQuantity,
            changeRate: d.percentageChange ?? 0,
            vol:        d.totalVolume ?? 0,
            time:       d.executedAt
                ? new Date(d.executedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
                : '',
          })));
        })
        .catch(e => console.warn('[TokenDetailPage] 체결 목록 조회 실패:', e));
  }, [TOKEN_ID, user?.accessToken]);

  // ── 현재가 — REST 초기값 + 체결 WebSocket으로만 갱신 (차트 주기와 무관)
  const [currentPrice, setCurrentPrice] = useState(0);

  useEffect(() => {
    if (tokenInfo?.currentPrice) setCurrentPrice(tokenInfo.currentPrice);
  }, [tokenInfo]);

  // 차트 hover용 — 차트 주기 전환에 영향받지 않도록 currentPrice와 분리
  const lastWithData = useMemo(
    () => [...chartData].reverse().find(c => c.close != null),
    [chartData]
  );

  const basePrice = tokenInfo?.yesterdayClosePrice || currentPrice || 1;
  const displayData = hoveredData || lastWithData || null;

  const validHighs = useMemo(() => chartData.map(c => c.high).filter(v => v != null), [chartData]);
  const validLows  = useMemo(() => chartData.map(c => c.low).filter(v  => v != null), [chartData]);
  const chartHigh  = validHighs.length > 0 ? Math.max(...validHighs) : 0;
  const chartLow   = validLows.length  > 0 ? Math.min(...validLows)  : 0;
  const dailyHigh  = todayHigh ?? chartHigh;
  const dailyLow   = todayLow  ?? chartLow;

  // Y축 도메인 ? null 슬롯 제외한 실제 가격 기준
  // 호가 단위 정책 (TickSizePolicy.java 동일 기준)
  const getTickSize = (price) => {
    if (price < 100)   return 10;
    if (price < 1000)  return 50;
    if (price < 10000) return 100;
    return 500;
  };

  const { yDomain, yTicks } = useMemo(() => {
    const prices = chartData.flatMap(d => [d.low, d.high]).filter(v => v != null);
    if (prices.length === 0) return { yDomain: ['auto', 'auto'], yTicks: undefined };

    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const mid = (min + max) / 2;
    const tickSize = getTickSize(mid);

    // 패딩: 최소 tickSize 1칸, 범위의 10%
    const pad = Math.max(tickSize, Math.ceil((max - min) * 0.1 / tickSize) * tickSize);
    const domainMin = Math.floor((min - pad) / tickSize) * tickSize;
    const domainMax = Math.ceil((max + pad) / tickSize) * tickSize;

    // 5~7개 tick ? 호가 단위 배수로 정렬
    const range = domainMax - domainMin;
    const interval = Math.ceil(range / (5 * tickSize)) * tickSize;
    const ticks = [];
    for (let t = domainMin; t <= domainMax; t += interval) ticks.push(t);

    return { yDomain: [domainMin, domainMax], yTicks: ticks };
  }, [chartData]);

  const maxAskAmount = Math.max(...asks.map(r => r.amount), 1);
  const maxBidAmount = Math.max(...bids.map(r => r.amount), 1);
  const askWaitingTotal = asks.reduce((sum, row) => sum + row.amount, 0);
  const bidWaitingTotal = bids.reduce((sum, row) => sum + row.amount, 0);
  const waitingTotal = askWaitingTotal + bidWaitingTotal;
  const askWaitingRatio = waitingTotal > 0 ? (askWaitingTotal / waitingTotal) * 100 : 50;
  const bidWaitingRatio = waitingTotal > 0 ? (bidWaitingTotal / waitingTotal) * 100 : 50;

  // AssetHeader 가 기대하는 asset 객체 형태로 변환
  // tokenInfo 로드 전에도 탭이 보여야 하므로 항상 객체를 반환
  const changeRate = basePrice > 0 && currentPrice > 0
      ? Math.round(((currentPrice - basePrice) / basePrice) * 10000) / 100
      : 0;

  const asset = {
    id:     TOKEN_ID,
    name:   tokenInfo?.tokenName  || tokenInfo?.assetName || '-',
    symbol: tokenInfo?.tokenSymbol || '-',
    change: changeRate,
    high:   dailyHigh || currentPrice || 0,
    low:    dailyLow  || currentPrice || 0,
    price:  currentPrice,
    issued: tokenInfo?.totalSupply || 0,
    desc:   tokenInfo?.assetName   || '',
    pdfUrl: null,
  };

  const isLiked = likedTokenIds.includes(TOKEN_ID);

  // ── 캔들 API 조회 ────────────────────────────────────────────
  const fetchCandles = useCallback(async () => {
    setLoading(true);
    try {
      const type = PERIOD_TO_TYPE[chartPeriod];
      let candles = [];

      if (type === 'MONTH' || type === 'YEAR') {
        const [res, dayRes] = await Promise.all([
          api.get(`/api/token/${TOKEN_ID}/candle?type=${type}`),
          api.get(`/api/token/${TOKEN_ID}/candle?type=DAY`),
        ]);
        const baseCandles = res.data.map(d => mapCandle(d, chartPeriod));
        const dayCandles = dayRes.data.map(d => mapCandle(d, '일'));
        candles = baseCandles.length > 0 ? baseCandles : aggregateCandlesFromDay(dayCandles, chartPeriod);
      } else {
        const res = await api.get(`/api/token/${TOKEN_ID}/candle?type=${type}`);
        candles = res.data.map(d => mapCandle(d, chartPeriod));
      }

      setChartData(buildChartData(candles, chartPeriod, null));
    } catch (e) {
      console.warn('[TokenDetailPage] 캔들 조회 실패:', e.message);
    } finally {
      setLoading(false);
    }
  }, [chartPeriod, TOKEN_ID, tokenInfo?.currentPrice, tokenInfo?.yesterdayClosePrice, user?.accessToken]);

  useEffect(() => {
    setChartData([]);   // 기간 변경 시 이전 기간 데이터 초기화 (분/시/일 혼재 방지)
    fetchCandles();
  }, [fetchCandles]);

  // ── 대기 주문 WS 업데이트 ────────────────────────────────────
  // match 서버가 pendingOrders:{tokenId}:{memberId} publish 시 주문 패널로 전달
  const [wsPendingData, setWsPendingData] = useState(null);

  // ── WebSocket 연동 ───────────────────────────────────────────
  // /topic/candle/live/{tokenId}/{candleType} 구독
  // 현재 선택된 주기(chartPeriod)의 타입만 구독 → 차트 전환 시 재연결
  useTradingSocket({
    tokenId:    TOKEN_ID,
    candleType: PERIOD_TO_TYPE[chartPeriod],
    token:      user?.accessToken,
    memberId,
    onOrderBook: (data) => {
      if (data.asks) setAsks(data.asks.map(r => ({ price: r.price, amount: r.quantity })));
      if (data.bids) setBids(data.bids.map(r => ({ price: r.price, amount: r.quantity })));
    },
    onTrades: (data) => {
      if (data.tradePrice) setCurrentPrice(data.tradePrice);
      setExecutions(prev =>
          [{ price: data.tradePrice, qty: data.tradeQuantity, isBuy: data.isBuy }, ...prev].slice(0, 15)
      );
      setTrades(prev => {
        const cumulativeVol = (prev[0]?.vol ?? 0) + (data.tradeQuantity ?? 0);
        // 전날 종가 대비 등락률 ? REST API와 동일한 기준
        const base = tokenInfo?.yesterdayClosePrice;
        const rate = base > 0 ? ((data.tradePrice - base) / base) * 100 : 0;
        return [{
          price:      data.tradePrice,
          qty:        data.tradeQuantity,
          changeRate: Math.round(rate * 100) / 100,
          vol:        cumulativeVol,
          time:       formatTradeTime(data.tradeTime),
        }, ...prev].slice(0, 50);
      });
    },
    onCandle: (data) => {
      const newCandle = mapCandle(data, chartPeriod);
      setChartData(prev => {
        const actualCandles = prev.filter(c => c.close != null && !c.isSynthetic);
        const idx = actualCandles.findIndex(c => c.ts === newCandle.ts);
        const merged = idx >= 0
            ? actualCandles.map((c, i) => (i === idx ? newCandle : c))
            : [...actualCandles, newCandle];
        return buildChartData(merged, chartPeriod, null);
      });
    },
    onDayCandle: (data) => {
      if (data.highPrice != null) setTodayHigh(prev => prev == null ? data.highPrice : Math.max(prev, data.highPrice));
      if (data.lowPrice  != null) setTodayLow(prev => prev == null ? data.lowPrice : Math.min(prev, data.lowPrice));
    },
    onPendingOrders: (data) => {
      setWsPendingData(data);
    },
  });

  // ── 렌더 ────────────────────────────────────────────────────
  return (
      <div className="h-full flex flex-col bg-stone-100 text-stone-800 overflow-hidden">

        {loginModal && (
            <LoginRequiredModal message={loginModal} onClose={() => setLoginModal(null)} />
        )}

        {/* 상단: 종목 헤더 ? tokenInfo 로드 여부와 관계없이 항상 렌더 (탭 접근 보장) */}
        <AssetHeader
            asset={asset}
            currentPrice={currentPrice}
            basePrice={basePrice}
            activeTab={activeTab}
            onTabChange={setActiveTab}
            isLiked={isLiked}
            onToggleLike={async () => {
                try {
                    await toggleLike(TOKEN_ID);
                } catch (err) {
                    console.error('[TokenDetailPage] like toggle failed:', err);
                }
            }}
            hideStats
        />
        {/* 메인 콘텐츠 */}
        <div className="flex-1 flex overflow-hidden p-6 gap-6">

          {activeTab === 'chart' ? (
              <>
                {/* ────────── 차트 패널 ────────── */}
                <div className="flex-[2] flex flex-col gap-6 overflow-hidden min-h-0">

                  {/* 캔들 차트 */}
                  <div className="flex-none bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">
                    <div className="p-4 border-b border-stone-200 flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className="flex bg-stone-200 p-1 rounded-lg">
                          {CHART_PERIODS.map(p => (
                              <button
                                  key={p}
                                  onClick={() => setChartPeriod(p)}
                                  className={cn(
                                      'px-3 py-1 rounded-md text-[11px] font-bold transition-all',
                                      chartPeriod === p
                                          ? 'bg-white text-stone-800 shadow-sm'
                                          : 'text-stone-400 hover:text-stone-500'
                                  )}
                              >
                                {p}
                              </button>
                          ))}
                        </div>
                      </div>
                      <button
                          onClick={() => setChartModalOpen(true)}
                          className="flex items-center gap-1 text-[11px] font-bold text-stone-400 hover:text-stone-800 transition-colors"
                      >
                        차트 크게보기 <Maximize2 size={14} />
                      </button>
                    </div>

                    <div className="h-[260px] flex flex-col">
                      <CandleVolumeChart
                          resetKey={`main-${chartPeriod}`}
                          chartData={chartData}
                          loading={loading}
                          displayData={displayData}
                          yDomain={yDomain}
                          yTicks={yTicks}
                          rightMargin={48}
                          yAxisWidth={56}
                          volumeTickGap={40}
                          emptyMessage="캔들 데이터 없음 (백엔드 확인 필요)"
                          onHover={setHoveredData}
                          onLeave={() => setHoveredData(null)}
                      />
                    </div>
                  </div>

                  {/* 시세 섹션 */}
                  <div className="flex-1 min-h-0 bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">
                    <div className="p-4 border-b border-stone-200 flex items-center justify-between">
                      <h3 className="text-sm font-bold text-stone-800">시세</h3>
                      <span className="text-xs font-bold text-stone-400">실시간</span>
                      <X size={14} className="text-stone-400" />
                    </div>
                    <div className="flex-1 overflow-y-auto">
                      <table className="w-full text-sm">
                        <thead className="text-stone-400 border-b border-stone-200 sticky top-0 bg-white">
                        <tr>
                          <th className="text-left p-4 font-bold">체결가</th>
                          <th className="text-right p-4 font-bold">체결량(주)</th>
                          <th className="text-right p-4 font-bold">등락률</th>
                          <th className="text-right p-4 font-bold">거래량(주)</th>
                          <th className="text-right p-4 font-bold">시간</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-stone-200">
                        {trades.map((row, i) => (
                            <PriceRow key={i} {...row} />
                        ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>

                {/* ────────── 호가 패널 ────────── */}
                <div className="w-[400px] min-h-0 bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm text-stone-800">
                  <div className="p-4 border-b border-stone-200 flex items-center justify-between bg-stone-100">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-black text-stone-800">호가</h3>
                    </div>
                    <div className="flex gap-2">
                      <button type="button" className="p-1 text-stone-400">
                        <Target size={14} />
                      </button>
                      <X size={14} className="text-stone-400" />
                    </div>
                  </div>

                  {/* 토스 스타일 3열 호가창 */}
                  {(() => {
                    // 체결강도 계산
                    const buyVol  = executions.filter(e => e.isBuy).reduce((s, e) => s + e.qty, 0);
                    const sellVol = executions.filter(e => !e.isBuy).reduce((s, e) => s + e.qty, 0);
                    const strengthNum = sellVol > 0 ? Math.round((buyVol / sellVol) * 100) : buyVol > 0 ? 200 : null;
                    const strengthVal = strengthNum != null ? `${strengthNum}%` : '-';
                    const strengthColor = strengthNum == null ? 'text-stone-400' : strengthNum >= 100 ? 'text-brand-red' : 'text-brand-blue';

                    // 매도 우측 통계 항목
                    const statItems = [
                      // TODO: 백엔드 로직 미구현 ? 전일 종가 기준 ±30% 하드코딩
                      { label: '상한가', value: basePrice > 0 ? Math.round(basePrice * 1.3).toLocaleString() : '-', color: 'var(--color-brand-red)', unimplemented: true },
                      { label: '하한가', value: basePrice > 0 ? Math.round(basePrice * 0.7).toLocaleString() : '-', color: 'var(--color-brand-blue)', unimplemented: true },
                      { label: '시작',   value: tokenInfo?.todayOpenPrice != null ? tokenInfo.todayOpenPrice.toLocaleString() : '체결 기록 없음', color: tokenInfo?.todayOpenPrice != null ? '#292524' : 'var(--color-stone-400)' },
                      { label: '1일최고', value: dailyHigh != null && dailyHigh > 0 ? dailyHigh.toLocaleString() : '체결 기록 없음', color: dailyHigh != null ? 'var(--color-brand-red)' : 'var(--color-stone-400)' },
                      { label: '1일최저', value: dailyLow  != null && dailyLow  > 0 ? dailyLow.toLocaleString()  : '체결 기록 없음', color: dailyLow  != null ? 'var(--color-brand-blue)' : 'var(--color-stone-400)' },
                    ];

                    // 높은 가격 → 낮은 가격 순 (현재가 바 바로 위가 가장 낮은 매도호가)
                    const reversedAsks = [...asks].reverse();

                    return (
                      <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                        <div className="grid grid-cols-[146px_108px_146px] border-b border-stone-200 bg-stone-100">
                          <span aria-hidden="true" />
                          <span className="text-[10px] font-bold text-stone-400 text-center py-1.5">호가</span>
                          <span aria-hidden="true" />
                        </div>

                        <div className="grid grid-cols-[146px_108px_146px] flex-1 min-h-0">
                          <div className="col-span-2 min-h-0 flex flex-col justify-end">
                            {reversedAsks.map((row, i) => {
                              const dp = maxAskAmount > 0 ? (row.amount / maxAskAmount) * 100 : 0;
                              const cp = basePrice > 0 ? ((row.price - basePrice) / basePrice) * 100 : 0;
                              return (
                                <button
                                    key={`ask-row-${i}`}
                                    type="button"
                                    onClick={() => setSelectedOrderPrice(row.price)}
                                    onMouseEnter={() => setHoveredAskIndex(i)}
                                    onMouseLeave={() => setHoveredAskIndex(null)}
                                    className={cn(
                                      'grid grid-cols-[146px_108px] h-9 border-b border-stone-100 transition-colors text-left',
                                      hoveredAskIndex === i ? 'bg-blue-100/60' : 'hover:bg-blue-100/60'
                                    )}
                                >
                                  <div className="relative flex items-center justify-end pr-3 pl-3 overflow-hidden border-r border-stone-100">
                                    <div
                                        className="absolute right-3 top-[6px] bottom-[6px] rounded-l-md bg-brand-blue/10"
                                        style={{ width: `${Math.min(dp * 0.58, 58)}%` }}
                                    />
                                    <span className="relative z-10 text-[11px] font-mono font-bold text-brand-blue">{row.amount.toLocaleString()}</span>
                                  </div>
                                  <div className="flex flex-col items-center justify-center">
                                    <span className="text-[12px] font-mono font-black text-brand-blue">{row.price.toLocaleString()}</span>
                                    <span className="text-[9px] font-bold text-brand-blue/60">{cp >= 0 ? '+' : ''}{cp.toFixed(2)}%</span>
                                  </div>
                                </button>
                              );
                            })}
                          </div>

                          <div className="min-h-0 flex flex-col justify-end border-l border-stone-100">
                            {statItems.map((stat, i) => (
                              <div key={`ask-stat-${i}`} className="h-9 border-b border-stone-100 px-2 flex items-center">
                                <div className="flex flex-col w-full gap-0.5">
                                  <div className="flex justify-between w-full">
                                    <span className="text-[9px] font-bold text-stone-400">{stat.label}</span>
                                    <span className="text-[9px] font-bold" style={{ color: stat.color }}>{stat.value}</span>
                                  </div>
                                  {stat.unimplemented && (
                                    <span className="text-[8px] font-bold text-amber-400 text-right">미구현</span>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>

                        <div className="h-10 bg-stone-200 flex items-center justify-center relative flex-none border-y border-stone-300">
                          <div className="absolute left-2 w-5 h-5 bg-brand-blue rounded flex items-center justify-center text-[9px] font-black text-white">현</div>
                          <div className="flex items-baseline gap-2">
                            <span className="text-sm font-black text-stone-800 font-mono tracking-tight">
                              {currentPrice > 0 ? currentPrice.toLocaleString() : '-'}
                            </span>
                            {currentPrice > 0 && basePrice > 0 && (() => {
                              const r = ((currentPrice - basePrice) / basePrice) * 100;
                              return (
                                <span className={`text-[10px] font-bold ${r >= 0 ? 'text-brand-red' : 'text-brand-blue'}`}>
                                  {r >= 0 ? '+' : ''}{r.toFixed(2)}%
                                </span>
                              );
                            })()}
                          </div>
                        </div>

                        <div className="grid grid-cols-[146px_108px_146px] flex-1 min-h-0">
                          <div className="min-h-0 flex flex-col border-r border-stone-100">
                            <div className="h-9 border-b border-stone-100 bg-stone-50/60 px-3 flex items-center">
                              <div>
                                <p className="text-[9px] font-bold text-stone-400 leading-none">실시간 체결강도</p>
                                <p className={`text-[11px] font-black leading-none mt-0.5 ${strengthColor}`}>{strengthVal}</p>
                              </div>
                            </div>
                            {bids.map((row, i) => {
                              const ex = executions[i];
                              return (
                                <div
                                    key={`bid-ex-${i}`}
                                    className="h-9 border-b border-stone-100 flex items-center px-2 overflow-hidden"
                                >
                                  {ex && (
                                    <div className="flex justify-between w-full">
                                      <span className="text-[10px] font-mono text-stone-400 truncate">{ex.price.toLocaleString()}</span>
                                      <span className={`text-[10px] font-bold ml-1 flex-shrink-0 ${ex.isBuy ? 'text-brand-red' : 'text-brand-blue'}`}>{ex.qty}</span>
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>

                          <div className="col-span-2 min-h-0 flex flex-col">
                            {bids.map((row, i) => {
                              const cp = basePrice > 0 ? ((row.price - basePrice) / basePrice) * 100 : 0;
                              const dp = maxBidAmount > 0 ? (row.amount / maxBidAmount) * 100 : 0;
                              return (
                                <button
                                    key={`bid-row-${i}`}
                                    type="button"
                                    onClick={() => setSelectedOrderPrice(row.price)}
                                    onMouseEnter={() => setHoveredBidIndex(i)}
                                    onMouseLeave={() => setHoveredBidIndex(null)}
                                    className={cn(
                                      'grid grid-cols-[108px_146px] h-9 border-b border-stone-100 transition-colors text-left',
                                      hoveredBidIndex === i ? 'bg-red-100/60' : 'hover:bg-red-100/60'
                                    )}
                                >
                                  <div className="flex flex-col items-center justify-center border-r border-stone-100">
                                    <span className="text-[12px] font-mono font-black text-brand-red">{row.price.toLocaleString()}</span>
                                    <span className="text-[9px] font-bold text-brand-red/60">{cp >= 0 ? '+' : ''}{cp.toFixed(2)}%</span>
                                  </div>
                                  <div className="relative flex items-center justify-start pl-3 pr-3 overflow-hidden">
                                    <div
                                        className="absolute left-3 top-[6px] bottom-[6px] rounded-r-md bg-brand-red/10"
                                        style={{ width: `${Math.min(dp * 0.58, 58)}%` }}
                                    />
                                    <span className="relative z-10 text-[11px] font-mono font-bold text-brand-red">{row.amount.toLocaleString()}</span>
                                  </div>
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      </div>
                    );
                  })()}

                  {/* 하단 요약 바 */}
                  <div className="h-12 bg-stone-100 border-t border-stone-200 px-4 py-2 text-[9px] font-black">
                    <div className="relative h-2 overflow-hidden rounded-full bg-stone-200/80">
                      <div
                          className="absolute left-0 top-0 bottom-0 bg-brand-blue/35 transition-all duration-300"
                          style={{ width: `${askWaitingRatio}%` }}
                      />
                      <div
                          className="absolute right-0 top-0 bottom-0 bg-brand-red/35 transition-all duration-300"
                          style={{ width: `${bidWaitingRatio}%` }}
                      />
                      <div
                          className="absolute top-[-2px] bottom-[-2px] w-px bg-white/90 transition-all duration-300"
                          style={{ left: `${askWaitingRatio}%`, transform: 'translateX(-50%)' }}
                      />
                    </div>
                    <div className="mt-2 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-stone-400">판매대기</span>
                        <span className="text-brand-blue">{askWaitingTotal.toLocaleString()}</span>
                      </div>
                      <span className="text-stone-400">정규장</span>
                      <div className="flex items-center gap-2">
                        <span className="text-brand-red">{bidWaitingTotal.toLocaleString()}</span>
                        <span className="text-stone-400">구매대기</span>
                      </div>
                    </div>
                  </div>
                </div>
              </>
          ) : (
              /* 차트·호가 외 탭 */
              <div className="flex-1 bg-white rounded-2xl border border-stone-200 p-8 overflow-y-auto shadow-sm">
                {activeTab === 'info'     && <InfoTab tokenInfo={tokenInfo} tokenAssetInfo={tokenAssetInfo} />}
                {activeTab === 'dividend' && <DividendTab allocations={allocations} />}
                {activeTab === 'news'     && <NewsTab disclosures={disclosures} />}
              </div>
          )}

          {/* 주문창: 항상 오른쪽 고정 */}
          <SecureOrderPanel
              currentPrice={currentPrice}
              selectedPrice={selectedOrderPrice}
              onLoginRequired={setLoginModal}
              tokenId={TOKEN_ID}
              token={user?.accessToken}
              wsPendingData={wsPendingData}
          />
        </div>

        {/* ── 차트 크게보기 모달 ─────────────────────────────────── */}
        {chartModalOpen && (
            <div
                className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
                onClick={() => setChartModalOpen(false)}
            >
              <div
                  className="bg-white rounded-2xl border border-stone-200 shadow-2xl w-[90vw] max-w-[1200px] h-[80vh] flex flex-col overflow-hidden"
                  onClick={e => e.stopPropagation()}
              >
                {/* 모달 헤더 */}
                <div className="p-4 border-b border-stone-200 flex items-center justify-between">
                  <div className="flex items-center gap-4">
                <span className="text-sm font-black text-stone-800">
                  {tokenInfo?.assetName ?? '차트'}
                </span>
                    <div className="flex bg-stone-200 p-1 rounded-lg">
                      {CHART_PERIODS.map(p => (
                          <button
                              key={p}
                              onClick={() => setChartPeriod(p)}
                              className={cn(
                                  'px-3 py-1 rounded-md text-[11px] font-bold transition-all',
                                  chartPeriod === p
                                      ? 'bg-white text-stone-800 shadow-sm'
                                      : 'text-stone-400 hover:text-stone-500'
                              )}
                          >
                            {p}
                          </button>
                      ))}
                    </div>
                  </div>
                  <button
                      onClick={() => setChartModalOpen(false)}
                      className="p-1 rounded-lg hover:bg-stone-100 transition-colors"
                  >
                    <X size={18} className="text-stone-500" />
                  </button>
                </div>

                {/* 모달 차트 */}
                <div className="flex-1 flex flex-col overflow-hidden">
                      <CandleVolumeChart
                          resetKey={`modal-${chartPeriod}`}
                          chartData={chartData}
                          loading={loading}
                          displayData={displayData}
                      yDomain={([min, max]) => {
                        const pad = Math.max(Math.ceil((max - min) * 0.05), 1);
                        return [min - pad, max + pad];
                      }}
                      yTicks={undefined}
                      rightMargin={60}
                      yAxisWidth={64}
                      volumeTickGap={30}
                      emptyMessage="캔들 데이터 없음"
                      onHover={setHoveredData}
                      onLeave={() => setHoveredData(null)}
                  />
                </div>
              </div>
            </div>
        )}
      </div>
  );
}

// ── 종목정보 탭 ─────────────────────────────────────────────────
function InfoTab({ tokenInfo, tokenAssetInfo }) {
  if (!tokenInfo) {
    return (
        <div className="flex items-center justify-center h-40 text-stone-400 font-bold text-sm">
          정보를 불러오는 중...
        </div>
    );
  }

  const rows = [
    { label: '토큰 이름',    value: tokenInfo.tokenName   ?? '-' },
    { label: '토큰 심볼',    value: tokenInfo.tokenSymbol ?? '-' },
    { label: '총 발행량',    value: (tokenAssetInfo?.totalSupply ?? tokenInfo.totalSupply) != null ? `${(tokenAssetInfo?.totalSupply ?? tokenInfo.totalSupply).toLocaleString()}주` : '-' },
    { label: '발행가',       value: tokenAssetInfo?.initPrice != null ? `${tokenAssetInfo.initPrice.toLocaleString()}원` : '-' },
    { label: '총 자산 가치', value: tokenAssetInfo?.totalValue != null ? `${tokenAssetInfo.totalValue.toLocaleString()}원` : '-' },
    { label: '자산 주소',    value: tokenAssetInfo?.assetAddress ?? '-' },
    { label: '상장일',       value: (tokenAssetInfo?.createdAt ?? tokenInfo.issuedAt) ? new Date(tokenAssetInfo?.createdAt ?? tokenInfo.issuedAt).toLocaleDateString('ko-KR') : '-' },
  ];

  return (
      <div className="space-y-8 max-w-4xl">
        <section>
          <h3 className="text-lg font-bold mb-4 text-stone-800">종목 상세 정보</h3>
          <div className="grid grid-cols-2 gap-3">
            {rows.map((item, i) => (
                <div key={i} className="flex justify-between p-4 bg-stone-100 rounded-xl border border-stone-200">
                  <span className="text-stone-400 font-bold text-sm">{item.label}</span>
                  <span className="text-stone-800 font-bold text-sm">{item.value}</span>
                </div>
            ))}
          </div>
        </section>

        {tokenInfo.assetName && (
            <section>
              <h3 className="text-lg font-bold mb-4 text-stone-800">자산 개요</h3>
              <p className="text-stone-500 text-sm leading-relaxed">
                {tokenInfo.assetName}
                {tokenAssetInfo?.assetAddress ? ` · ${tokenAssetInfo.assetAddress}` : ''}
              </p>
            </section>
        )}
      </div>
  );
}

// ── 배당금 탭 ───────────────────────────────────────────────────
function DividendTab({ allocations }) {
  if (allocations === null) {
    return (
        <div className="flex items-center justify-center h-40 text-stone-400 font-bold text-sm">
          배당금 내역을 불러오는 중...
        </div>
    );
  }

  const totalDividend = allocations.reduce((sum, a) => sum + (a.monthlyDividendIncome ?? 0), 0);

  return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold text-stone-800">배당금 내역</h3>
          <div className="flex items-center gap-2 text-sm text-stone-400">
            <span>총 누적 배당금:</span>
            <span className="text-stone-800 font-black">
            {totalDividend > 0 ? `${totalDividend.toLocaleString()}원` : '-'}
          </span>
          </div>
        </div>
        {allocations.length === 0 ? (
            <div className="py-16 flex flex-col items-center gap-3 text-center text-stone-400">
              <p className="text-sm font-bold">배당금 내역이 없습니다</p>
            </div>
        ) : (
            <div className="overflow-hidden rounded-xl border border-stone-200">
              <table className="w-full text-sm">
                <thead className="bg-stone-100 text-stone-400 text-[11px] font-bold">
                <tr>
                  <th className="text-left p-4">지급일</th>
                  <th className="text-right p-4">주당 배당금</th>
                  <th className="text-right p-4">총 배당금</th>
                  <th className="text-right p-4">배치 여부</th>
                </tr>
                </thead>
                <tbody className="divide-y divide-stone-200">
                {allocations.map((a, i) => (
                    <tr key={i} className="hover:bg-stone-50">
                      <td className="p-4 text-stone-600 font-bold">
                        {a.settledAt ? new Date(a.settledAt).toLocaleDateString('ko-KR') : '-'}
                      </td>
                      <td className="p-4 text-right font-mono font-bold text-stone-800">
                        {a.allocationPerToken != null ? `${a.allocationPerToken.toLocaleString()}원` : '-'}
                      </td>
                      <td className="p-4 text-right font-mono font-bold text-stone-800">
                        {a.monthlyDividendIncome != null ? `${a.monthlyDividendIncome.toLocaleString()}원` : '-'}
                      </td>
                      <td className="p-4 text-right">
                    <span className={cn(
                        'px-2 py-0.5 rounded text-[10px] font-bold',
                        a.allocationBatchStatus ? 'bg-green-100 text-green-700' : 'bg-stone-200 text-stone-500'
                    )}>
                      {a.allocationBatchStatus ? '완료' : '대기'}
                    </span>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
            </div>
        )}
      </div>
  );
}

// ── 공시 탭 ─────────────────────────────────────────────────────
const DISCLOSURE_CATEGORY_LABEL = {
  BUILDING:  '건물',
  DIVIDEND:  '배당',
  ETC:       '기타',
};

function NewsTab({ disclosures }) {
  const [expanded, setExpanded] = useState(null);

  if (disclosures === null) {
    return (
        <div className="flex items-center justify-center h-40 text-stone-400 font-bold text-sm">
          공시 내역을 불러오는 중...
        </div>
    );
  }

  return (
      <div className="space-y-4">
        <h3 className="text-lg font-bold text-stone-800">공시</h3>
        {disclosures.length === 0 ? (
            <div className="py-16 flex flex-col items-center gap-3 text-center text-stone-400">
              <p className="text-sm font-bold">공시 내역이 없습니다</p>
            </div>
        ) : (
            <div className="space-y-2">
              {disclosures.map((d, i) => (
                  <div key={i} className="border border-stone-200 rounded-xl overflow-hidden">
                    <button
                        className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors text-left"
                        onClick={() => setExpanded(expanded === i ? null : i)}
                    >
                      <div className="flex items-center gap-3">
                  <span className="px-2 py-0.5 bg-stone-200 rounded text-[10px] font-bold text-stone-500">
                    {DISCLOSURE_CATEGORY_LABEL[d.disclosureCategory] ?? d.disclosureCategory}
                  </span>
                        <span className="text-sm font-bold text-stone-800">{d.disclosureTitle}</span>
                      </div>
                      <div className="flex items-center gap-3 shrink-0">
                  <span className="text-[11px] text-stone-400 font-bold">
                    {d.createdAt ? new Date(d.createdAt).toLocaleDateString('ko-KR') : '-'}
                  </span>
                        <span className="text-stone-400 text-xs">{expanded === i ? '▲' : '▼'}</span>
                      </div>
                    </button>
                    {expanded === i && (
                        <div className="px-4 pb-4 pt-2 border-t border-stone-100 bg-stone-50">
                          <p className="text-sm text-stone-600 leading-relaxed whitespace-pre-wrap">
                            {d.disclosureContent}
                          </p>
                          {d.OriginName && (
                              <p className="mt-2 text-[11px] text-stone-400 font-bold">첨부: {d.OriginName}</p>
                          )}
                        </div>
                    )}
                  </div>
              ))}
            </div>
        )}
      </div>
  );
}

// ── 로그인 게이트 주문창 ────────────────────────────────────────
// 비로그인 시 매수/매도/대기 버튼에 로그인 안내 처리

