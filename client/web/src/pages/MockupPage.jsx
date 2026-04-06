// MockupPage — 상세 페이지 실데이터 연동 목업
// /mockup/:tokenId 로 접근
// 팀원 작업 중인 TradingPage와 분리된 독립 테스트 공간

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  ResponsiveContainer, ComposedChart, Bar,
  XAxis, YAxis, Tooltip, CartesianGrid,
} from 'recharts';
import {
  ChartLine, Filter, Settings, Maximize2, X, Target, CheckCircle,
} from 'lucide-react';

import { useApp }            from '../context/AppContext.jsx';
import { useTradingSocket }  from '../hooks/useTradingSocket.js';
import { AssetHeader }       from '../components/trading/AssetHeader.jsx';
import { OrderPanel }        from '../components/trading/OrderPanel.jsx';
import { HogaRow }           from '../components/trading/HogaRow.jsx';
import { PriceRow }          from '../components/trading/PriceRow.jsx';
import {
  HOGA_ASKS, HOGA_BIDS, HOGA_EXECUTIONS, PRICE_HISTORY_ROWS,
} from '../data/mock.js';
import { cn } from '../lib/utils.js';

const API = 'http://localhost:8080';

const PERIOD_TO_TYPE = {
  '1분': 'MINUTE',
  '일':  'DAY',
  '주':  'HOUR',
  '월':  'MONTH',
  '년':  'YEAR',
};
const CHART_PERIODS = ['1분', '일', '주', '월', '년'];

// ── 유틸 ────────────────────────────────────────────────────────
function formatCandleTime(candleTime, period) {
  if (!candleTime) return '';
  const d = new Date(candleTime);
  if (period === '1분' || period === '주') return d.toTimeString().slice(0, 5);
  if (period === '일') return `${d.getMonth() + 1}/${d.getDate()}`;
  if (period === '월') return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}`;
  if (period === '년') return `${d.getFullYear()}`;
  return d.toTimeString().slice(0, 5);
}

function mapCandle(dto, period) {
  return {
    time:  formatCandleTime(dto.candleTime, period),
    open:  Math.round(dto.openPrice  || 0),
    high:  Math.round(dto.highPrice  || 0),
    low:   Math.round(dto.lowPrice   || 0),
    close: Math.round(dto.closePrice || 0),
    vol:   Math.round(dto.volume     || 0),
  };
}

// ── 캔들스틱 shape ───────────────────────────────────────────────
function CandlestickShape({ x, y, width, height, payload }) {
  if (!payload) return null;
  const { open, close, high, low } = payload;
  const isUp    = close >= open;
  const color   = isUp ? 'var(--color-brand-red)' : 'var(--color-brand-blue)';
  const ph      = Math.abs(open - close);
  const ratio   = ph === 0 ? 1 : height / ph;
  const wickTop = y - (high - Math.max(open, close)) * ratio;
  const wickBot = y + height + (Math.min(open, close) - low) * ratio;
  return (
    <g>
      <line x1={x + width / 2} y1={wickTop} x2={x + width / 2} y2={wickBot}
        stroke={color} strokeWidth={1} />
      <rect x={x} y={y} width={width} height={Math.max(height, 1)} fill={color} />
    </g>
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

// ── MockupPage ───────────────────────────────────────────────────
export function MockupPage() {
  const { tokenId }    = useParams();
  const { user, watchlist, toggleWatchlist } = useApp();
  const TOKEN_ID = Number(tokenId) || 1;

  // ── 로그인 필요 팝업 ─────────────────────────────────────────
  const [loginModal, setLoginModal] = useState(null); // null | string(message)

  // ── 탭 상태 ─────────────────────────────────────────────────
  const [activeTab, setActiveTab] = useState('chart');

  // ── 토큰 상세 정보 (백엔드) ──────────────────────────────────
  const [tokenInfo, setTokenInfo] = useState(null);

  useEffect(() => {
    const headers = user?.accessToken
      ? { Authorization: `Bearer ${user.accessToken}` }
      : {};
    fetch(`${API}/api/token/${TOKEN_ID}/chart`, { headers })
      .then(r => r.ok ? r.json() : Promise.reject(r.status))
      .then(data => setTokenInfo(data))
      .catch(e => console.warn('[MockupPage] 토큰 상세 조회 실패:', e));
  }, [TOKEN_ID, user?.accessToken]);

  // ── 차트 상태 ────────────────────────────────────────────────
  const [chartPeriod, setChartPeriod]         = useState('1분');
  const [chartData, setChartData]             = useState([]);
  const [hoveredData, setHoveredData]         = useState(null);
  const [loading, setLoading]                 = useState(false);
  const [priceHistoryTab, setPriceHistoryTab] = useState('realtime');

  // ── 호가 / 체결 상태 (match 서버 연동 전: mock 기본값) ────────
  const [asks, setAsks]             = useState(HOGA_ASKS);
  const [bids, setBids]             = useState(HOGA_BIDS);
  const [executions, setExecutions] = useState(HOGA_EXECUTIONS);
  const [trades, setTrades]         = useState(PRICE_HISTORY_ROWS);

  // ── 현재가 계산 ──────────────────────────────────────────────
  const currentPrice = chartData.length > 0
    ? chartData[chartData.length - 1].close
    : (tokenInfo?.currentPrice ?? 0);

  const basePrice   = chartData.length > 0 ? chartData[0].open : currentPrice || 1;
  const displayData = hoveredData || (chartData.length > 0 ? chartData[chartData.length - 1] : null);

  const dailyHigh = chartData.length > 0 ? Math.max(...chartData.map(c => c.high)) : 0;
  const dailyLow  = chartData.length > 0 ? Math.min(...chartData.map(c => c.low))  : 0;

  const maxAskAmount = Math.max(...asks.map(r => r.amount), 1);
  const maxBidAmount = Math.max(...bids.map(r => r.amount), 1);

  // AssetHeader / OrderPanel 이 기대하는 asset 객체 형태로 변환
  // TokenDetailDto: tokenId, totalSupply, tokenName, tokenSymbol, issuedAt, assetName, imgUrl
  const asset = tokenInfo ? {
    id:     TOKEN_ID,
    name:   tokenInfo.tokenName  || tokenInfo.assetName || '-',
    symbol: tokenInfo.tokenSymbol || '-',
    change: 0,
    high:   dailyHigh || currentPrice || 0,
    low:    dailyLow  || currentPrice || 0,
    price:  currentPrice,
    issued: tokenInfo.totalSupply || 0,
    desc:   tokenInfo.assetName   || '',
    pdfUrl: null,
  } : null;

  const inWatchlist = watchlist.includes(TOKEN_ID);

  // ── 캔들 API 조회 ────────────────────────────────────────────
  const fetchCandles = useCallback(async () => {
    setLoading(true);
    try {
      const type    = PERIOD_TO_TYPE[chartPeriod];
      const headers = user?.accessToken
        ? { Authorization: `Bearer ${user.accessToken}` }
        : {};
      const res = await fetch(
        `${API}/api/token/${TOKEN_ID}/candles?type=${type}`,
        { headers }
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setChartData(data.map(d => mapCandle(d, chartPeriod)));
    } catch (e) {
      console.warn('[MockupPage] 캔들 조회 실패:', e.message);
    } finally {
      setLoading(false);
    }
  }, [chartPeriod, TOKEN_ID, user?.accessToken]);

  useEffect(() => { fetchCandles(); }, [fetchCandles]);

  // ── WebSocket 연동 ───────────────────────────────────────────
  useTradingSocket({
    tokenId:    TOKEN_ID,
    candleType: PERIOD_TO_TYPE[chartPeriod],
    token:      user?.accessToken,
    onOrderBook: (data) => {
      if (data.asks) setAsks(data.asks);
      if (data.bids) setBids(data.bids);
    },
    onTrades: (data) => {
      setExecutions(prev =>
        [{ price: data.price, qty: data.quantity, isBuy: data.isBuy }, ...prev].slice(0, 15)
      );
      setTrades(prev =>
        [{
          price:      data.price,
          qty:        data.quantity,
          changeRate: data.percentageChange ?? 0,
          vol:        data.totalVolume ?? 0,
          time:       data.tradeTime ?? '',
        }, ...prev].slice(0, 50)
      );
    },
    onCandle: (data) => {
      const newCandle = mapCandle(data, chartPeriod);
      setChartData(prev => {
        if (prev.length === 0) return [newCandle];
        const last = prev[prev.length - 1];
        return last.time === newCandle.time
          ? [...prev.slice(0, -1), newCandle]
          : [...prev, newCandle];
      });
    },
  });

  // ── 렌더 ────────────────────────────────────────────────────
  return (
    <div className="flex flex-col bg-stone-100 text-stone-800 overflow-hidden"
         style={{ height: 'calc(100vh - 64px)' }}>

      {loginModal && (
        <LoginRequiredModal message={loginModal} onClose={() => setLoginModal(null)} />
      )}

      {/* 상단: 종목 헤더 */}
      {asset ? (
        <AssetHeader
          asset={asset}
          currentPrice={currentPrice}
          activeTab={activeTab}
          onTabChange={setActiveTab}
          inWatchlist={inWatchlist}
          onToggleWatchlist={() => toggleWatchlist(TOKEN_ID)}
        />
      ) : (
        /* 로딩 중 스켈레톤 */
        <div className="px-8 py-6 border-b border-stone-200 bg-white flex items-center gap-4">
          <div className="w-10 h-10 bg-stone-200 rounded-lg animate-pulse" />
          <div className="space-y-2">
            <div className="w-40 h-5 bg-stone-200 rounded animate-pulse" />
            <div className="w-24 h-4 bg-stone-200 rounded animate-pulse" />
          </div>
          {loading && (
            <span className="ml-auto text-xs text-stone-400 font-bold animate-pulse">
              데이터 로딩 중...
            </span>
          )}
        </div>
      )}

      {/* 메인 콘텐츠 */}
      <div className="flex-1 flex overflow-hidden p-6 gap-6">

        {activeTab === 'chart' ? (
          <>
            {/* ────────── 차트 패널 ────────── */}
            <div className="flex-[2] flex flex-col gap-6 overflow-hidden">

              {/* 캔들 차트 */}
              <div className="bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">
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
                    <div className="flex gap-3 text-stone-400">
                      <ChartLine size={16} className="cursor-pointer hover:text-stone-800" />
                      <Filter    size={16} className="cursor-pointer hover:text-stone-800" />
                      <Settings  size={16} className="cursor-pointer hover:text-stone-800" />
                    </div>
                  </div>
                  <button className="flex items-center gap-1 text-[11px] font-bold text-stone-400 hover:text-stone-800 transition-colors">
                    차트 크게보기 <Maximize2 size={14} />
                  </button>
                </div>

                <div className="h-[360px] p-6 relative">
                  {/* OHLCV 인디케이터 */}
                  <div className="absolute top-6 left-6 z-10 flex items-center gap-3 pointer-events-none">
                    {[
                      { label: '시', val: displayData?.open },
                      { label: '고', val: displayData?.high, color: 'var(--color-brand-red)' },
                      { label: '저', val: displayData?.low,  color: 'var(--color-brand-blue)' },
                      { label: '종', val: displayData?.close },
                    ].map(({ label, val, color }) => (
                      <div key={label} className="flex items-center gap-1">
                        <span className="text-[10px] font-bold text-stone-400">{label}</span>
                        <span className="text-[11px] font-mono font-bold text-stone-800"
                          style={color ? { color } : {}}>
                          {val?.toLocaleString() ?? '-'}
                        </span>
                      </div>
                    ))}
                  </div>

                  {chartData.length === 0 ? (
                    <div className="w-full h-full flex items-center justify-center text-stone-400 text-sm font-bold">
                      {loading ? '데이터 로딩 중...' : '캔들 데이터 없음 (백엔드 확인 필요)'}
                    </div>
                  ) : (
                    <ResponsiveContainer width="100%" height="100%">
                      <ComposedChart
                        data={chartData}
                        margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
                        onMouseMove={e => {
                          if (e?.activePayload?.length > 0) setHoveredData(e.activePayload[0].payload);
                        }}
                        onMouseLeave={() => setHoveredData(null)}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="var(--color-stone-200)" vertical={false} />
                        <XAxis dataKey="time" axisLine={false} tickLine={false}
                          tick={{ fontSize: 10, fill: 'var(--color-stone-400)' }} minTickGap={30} />
                        <YAxis yAxisId="price" domain={['auto', 'auto']} orientation="right"
                          tick={{ fontSize: 10, fill: 'var(--color-stone-400)', fontWeight: 'bold' }}
                          axisLine={false} tickLine={false} />
                        <YAxis yAxisId="vol" orientation="left"
                          domain={[0, dataMax => dataMax * 4]}
                          tick={{ fontSize: 9, fill: 'var(--color-stone-400)' }}
                          axisLine={false} tickLine={false}
                          tickFormatter={val => `${(val / 10000).toFixed(0)}만`} />
                        <Tooltip
                          contentStyle={{
                            backgroundColor: 'var(--color-stone-100)',
                            border: '1px solid var(--color-stone-200)',
                            borderRadius: '12px', fontSize: '11px',
                            color: 'var(--color-stone-800)',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
                          }}
                          formatter={(value, name) => {
                            if (name === 'vol') return [`${value.toLocaleString()}주`, '거래량'];
                            return [`${value.toLocaleString()}원`, '가격'];
                          }}
                        />
                        <Bar yAxisId="vol" dataKey="vol" name="vol"
                          fill="var(--color-stone-400)" opacity={0.1} radius={[2, 2, 0, 0]} />
                        <Bar yAxisId="price" dataKey={d => [d.open, d.close]}
                          shape={<CandlestickShape />} animationDuration={0} />
                      </ComposedChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </div>

              {/* 시세 섹션 */}
              <div className="flex-1 bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">
                <div className="p-4 border-b border-stone-200 flex items-center justify-between">
                  <h3 className="text-sm font-bold text-stone-800">시세</h3>
                  <div className="flex bg-stone-200 p-1 rounded-lg">
                    {[{ id: 'realtime', label: '실시간' }, { id: 'daily', label: '일별' }].map(t => (
                      <button
                        key={t.id}
                        onClick={() => setPriceHistoryTab(t.id)}
                        className={cn(
                          'px-8 py-1 rounded-md text-[11px] font-bold transition-all',
                          priceHistoryTab === t.id
                            ? 'bg-white text-stone-800 shadow-sm'
                            : 'text-stone-400'
                        )}
                      >
                        {t.label}
                      </button>
                    ))}
                  </div>
                  <X size={14} className="text-stone-400 cursor-pointer" />
                </div>
                <div className="flex-1 overflow-y-auto">
                  <table className="w-full text-[11px]">
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
            <div className="w-[400px] bg-white rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm text-stone-800">
              <div className="p-4 border-b border-stone-200 flex items-center justify-between bg-stone-100">
                <div className="flex items-center gap-2">
                  <h3 className="text-sm font-black text-stone-800">호가</h3>
                  <div className="flex items-center gap-1 px-2 py-0.5 bg-stone-200 rounded text-[9px] font-bold text-stone-400">
                    <CheckCircle size={10} className="text-brand-blue" /> 빠른 주문
                  </div>
                </div>
                <div className="flex gap-2">
                  <button className="p-1 hover:text-stone-800 text-stone-400">
                    <Target size={14} />
                  </button>
                  <X size={14} className="text-stone-400 cursor-pointer hover:text-stone-800" />
                </div>
              </div>

              <div className="flex-1 flex overflow-hidden">
                {/* 왼쪽: 체결강도 + 미니 체결 목록 */}
                <div className="w-24 border-r border-stone-200 flex flex-col bg-stone-100/50">
                  <div className="p-2 border-b border-stone-200">
                    <p className="text-[9px] font-bold text-stone-400 mb-1">체결강도</p>
                    <p className="text-[11px] font-black text-brand-blue">
                      {(() => {
                        const totalAsk = asks.reduce((s, r) => s + r.amount, 0);
                        const totalBid = bids.reduce((s, r) => s + r.amount, 0);
                        const total = totalAsk + totalBid;
                        return total > 0 ? `${Math.round((totalBid / total) * 100)}%` : '-';
                      })()}
                    </p>
                  </div>
                  <div className="flex-1 overflow-y-auto scrollbar-hide py-2">
                    {executions.map((ex, i) => (
                      <div key={i} className="flex justify-between px-2 py-0.5 text-[9px] font-mono font-bold">
                        <span className="text-stone-400">{ex.price.toLocaleString()}</span>
                        <span className={ex.isBuy ? 'text-brand-red' : 'text-brand-blue'}>{ex.qty}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 중앙: 호가 목록 */}
                <div className="flex-1 flex flex-col overflow-hidden">
                  <div className="flex-1 overflow-y-auto scrollbar-hide">
                    <div className="flex flex-col-reverse">
                      {asks.map((row, i) => (
                        <HogaRow
                          key={`ask-${i}`}
                          price={row.price}
                          amount={row.amount}
                          changePercent={basePrice > 0 ? ((row.price - basePrice) / basePrice) * 100 : 0}
                          side="ask"
                          maxAmount={maxAskAmount}
                        />
                      ))}
                    </div>

                    <div className="h-9 bg-stone-200 flex items-center justify-center relative">
                      <div className="absolute left-2 w-4 h-4 bg-brand-blue rounded flex items-center justify-center text-[9px] font-black text-white">
                        현
                      </div>
                      <div className="flex flex-col items-center">
                        <span className="text-xs font-black text-stone-800 font-mono tracking-tight">
                          {currentPrice > 0 ? currentPrice.toLocaleString() : '-'}
                        </span>
                        {currentPrice > 0 && basePrice > 0 && (
                          <span className="text-[8px] font-bold text-brand-blue">
                            {(((currentPrice - basePrice) / basePrice) * 100).toFixed(2)}%
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex flex-col">
                      {bids.map((row, i) => (
                        <HogaRow
                          key={`bid-${i}`}
                          price={row.price}
                          amount={row.amount}
                          changePercent={basePrice > 0 ? ((row.price - basePrice) / basePrice) * 100 : 0}
                          side="bid"
                          maxAmount={maxBidAmount}
                        />
                      ))}
                    </div>
                  </div>
                </div>

                {/* 오른쪽: 통계 패널 */}
                <div className="w-28 border-l border-stone-200 bg-stone-100 flex flex-col p-2 space-y-4 overflow-y-auto scrollbar-hide">
                  <div className="space-y-1">
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>상한가</span>
                      <span className="text-brand-red">
                        {currentPrice > 0 ? Math.round(currentPrice * 1.3).toLocaleString() : '-'}
                      </span>
                    </div>
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>하한가</span>
                      <span className="text-brand-blue">
                        {currentPrice > 0 ? Math.round(currentPrice * 0.7).toLocaleString() : '-'}
                      </span>
                    </div>
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>상승VI</span><span>-</span>
                    </div>
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>하강VI</span><span>-</span>
                    </div>
                  </div>
                  <div className="h-px bg-stone-200" />
                  <div className="space-y-1">
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>시작</span>
                      <span>{chartData.length > 0 ? chartData[0].open.toLocaleString() : '-'}</span>
                    </div>
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>1일 최고</span>
                      <span className="text-brand-red">
                        {dailyHigh > 0 ? dailyHigh.toLocaleString() : '-'}
                      </span>
                    </div>
                    <div className="flex justify-between text-[8px] font-bold text-stone-400">
                      <span>1일 최저</span>
                      <span className="text-brand-blue">
                        {dailyLow > 0 ? dailyLow.toLocaleString() : '-'}
                      </span>
                    </div>
                  </div>
                  <div className="h-px bg-stone-200" />
                  <div className="space-y-1">
                    <p className="text-[8px] font-bold text-stone-400">거래량</p>
                    <p className="text-[9px] font-black text-stone-800">
                      {trades.length > 0 && trades[0].vol > 0
                        ? `${(trades[0].vol / 10000).toFixed(0)}만`
                        : '-'}
                    </p>
                  </div>
                </div>
              </div>

              {/* 하단 요약 바 */}
              <div className="h-10 bg-stone-100 border-t border-stone-200 flex items-center justify-between px-4 text-[9px] font-black">
                <div className="flex gap-2">
                  <span className="text-stone-400">판매대기</span>
                  <span className="text-brand-blue">
                    {asks.reduce((s, r) => s + r.amount, 0).toLocaleString()}
                  </span>
                </div>
                <span className="text-stone-400">애프터마켓</span>
                <div className="flex gap-2">
                  <span className="text-brand-red">
                    {bids.reduce((s, r) => s + r.amount, 0).toLocaleString()}
                  </span>
                  <span className="text-stone-400">구매대기</span>
                </div>
              </div>
            </div>
          </>
        ) : (
          /* 차트·호가 외 탭 */
          <div className="flex-1 bg-white rounded-2xl border border-stone-200 p-8 overflow-y-auto shadow-sm">
            {activeTab === 'info'     && <InfoTab tokenInfo={tokenInfo} />}
            {activeTab === 'dividend' && <DividendTab />}
            {activeTab === 'news'     && <NewsTab />}
          </div>
        )}

        {/* 주문창: 항상 오른쪽 고정 */}
        {asset && (
          <LoginGateOrderPanel
            asset={asset}
            currentPrice={currentPrice}
            isLoggedIn={!!user}
            onLoginRequired={setLoginModal}
          />
        )}
      </div>
    </div>
  );
}

// ── 종목정보 탭 ─────────────────────────────────────────────────
function InfoTab({ tokenInfo }) {
  if (!tokenInfo) {
    return (
      <div className="flex items-center justify-center h-40 text-stone-400 font-bold text-sm">
        정보를 불러오는 중...
      </div>
    );
  }

  const rows = [
    { label: '토큰 이름',   value: tokenInfo.tokenName   ?? '-' },
    { label: '토큰 심볼',   value: tokenInfo.tokenSymbol ?? '-' },
    { label: '총 발행량',   value: tokenInfo.totalSupply != null ? `${tokenInfo.totalSupply.toLocaleString()}주` : '-' },
    { label: '유통량',      value: tokenInfo.circulatingSupply != null ? `${tokenInfo.circulatingSupply.toLocaleString()}주` : '-' },
    { label: '발행가',      value: tokenInfo.initPrice != null ? `${tokenInfo.initPrice.toLocaleString()}원` : '-' },
    { label: '현재가',      value: tokenInfo.currentPrice != null ? `${Number(tokenInfo.currentPrice).toLocaleString()}원` : '-' },
    { label: '자산 주소',   value: tokenInfo.assetAddress ?? '-' },
    { label: '총 자산 가치', value: tokenInfo.totalValue != null ? `${tokenInfo.totalValue.toLocaleString()}원` : '-' },
    { label: '상장일',      value: tokenInfo.issuedAt ? new Date(tokenInfo.issuedAt).toLocaleDateString('ko-KR') : '-' },
    { label: '상태',        value: tokenInfo.tokenStatus ?? '-' },
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
            {tokenInfo.assetName} — {tokenInfo.assetAddress}
          </p>
        </section>
      )}
    </div>
  );
}

// ── 배당금 탭 (mock) ────────────────────────────────────────────
function DividendTab() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-stone-800">배당금 내역</h3>
        <div className="flex items-center gap-2 text-sm text-stone-400">
          <span>총 누적 배당금:</span>
          <span className="text-stone-800 font-black">-</span>
        </div>
      </div>
      <div className="py-16 flex flex-col items-center gap-3 text-center text-stone-400">
        <p className="text-sm font-bold">배당금 내역이 없습니다</p>
      </div>
    </div>
  );
}

// ── 공시 탭 (mock) ──────────────────────────────────────────────
function NewsTab() {
  return (
    <div className="space-y-4">
      <h3 className="text-lg font-bold text-stone-800">공시</h3>
      <div className="py-16 flex flex-col items-center gap-3 text-center text-stone-400">
        <p className="text-sm font-bold">공시 내역이 없습니다</p>
      </div>
    </div>
  );
}

// ── 로그인 게이트 주문창 ────────────────────────────────────────
// 비로그인 시 매수/매도/대기 버튼에 로그인 안내 처리
function LoginGateOrderPanel({ asset, currentPrice, isLoggedIn, onLoginRequired }) {
  const [orderSide, setOrderSide] = useState('buy');
  const [orderType, setOrderType] = useState('limit');
  const [inputMode, setInputMode] = useState('qty');
  const [price, setPrice]         = useState(String(currentPrice));
  const [qty, setQty]             = useState('');
  const [amount, setAmount]       = useState('');

  const isBuy     = orderSide === 'buy';
  const isPending = orderSide === 'pending';
  const isMarket  = orderType === 'market';

  const numPrice  = Number(price) || 0;
  const numQty    = inputMode === 'qty'
    ? (Number(qty) || 0)
    : (numPrice > 0 ? Math.floor((Number(amount) || 0) / numPrice) : 0);
  const numAmount = inputMode === 'amt'
    ? (Number(amount) || 0)
    : numPrice * numQty;

  function handleTabClick(side) {
    if (!isLoggedIn && side === 'pending') {
      onLoginRequired('로그인해야 볼 수 있습니다');
      return;
    }
    setOrderSide(side);
  }

  function handleSubmit() {
    if (!isLoggedIn) {
      onLoginRequired('매수/매도 주문을 하려면\n먼저 로그인해야 합니다');
      return;
    }
    alert(`${isBuy ? '매수' : '매도'} 주문 (mock)\n가격: ${numPrice.toLocaleString()}원\n수량: ${numQty}주`);
  }

  return (
    <div className="w-[360px] bg-white rounded-lg border border-stone-200 flex flex-col overflow-hidden">

      {/* 탭: 매수 / 매도 / 대기 */}
      <div className="flex border-b border-stone-200">
        {[
          { id: 'buy',     label: '매수',  active: 'text-brand-red border-b-2 border-brand-red bg-brand-red-light/40' },
          { id: 'sell',    label: '매도',  active: 'text-brand-blue border-b-2 border-brand-blue bg-brand-blue-light/60' },
          { id: 'pending', label: '대기',  active: 'text-stone-800 border-b-2 border-stone-800 bg-stone-100/60' },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => handleTabClick(t.id)}
            className={cn(
              'flex-1 py-4 text-sm font-black transition-all',
              orderSide === t.id ? t.active : 'text-stone-400 hover:text-stone-500'
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-5">

        {/* 대기 탭: 로그인 필요 안내 (여기까지 오면 이미 로그인 상태) */}
        {isPending ? (
          <div className="py-16 flex flex-col items-center gap-3 text-center text-stone-400">
            <p className="text-sm font-bold">대기 중인 주문이 없습니다</p>
          </div>
        ) : (
          <div className="space-y-5">
            {/* 주문 유형 + 입력 모드 */}
            <div className="flex items-center justify-between">
              <div className="flex bg-stone-200 p-1 rounded-lg">
                {[{ id: 'limit', label: '지정가' }, { id: 'market', label: '시장가' }].map(t => (
                  <button key={t.id} onClick={() => setOrderType(t.id)}
                    className={cn(
                      'px-3 py-1 rounded-md text-[10px] font-bold transition-all',
                      orderType === t.id ? 'bg-white text-stone-800 shadow-sm' : 'text-stone-400'
                    )}>
                    {t.label}
                  </button>
                ))}
              </div>
              <div className="flex bg-stone-200 p-1 rounded-lg">
                {[{ id: 'qty', label: '수량' }, { id: 'amount', label: '금액' }].map(m => (
                  <button key={m.id} onClick={() => setInputMode(m.id)}
                    className={cn(
                      'px-3 py-1 rounded-md text-[10px] font-bold transition-all',
                      inputMode === m.id ? 'bg-white text-stone-800 shadow-sm' : 'text-stone-400'
                    )}>
                    {m.label}
                  </button>
                ))}
              </div>
            </div>

            {/* 가격 입력 */}
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-stone-400">
                {isBuy ? '매수' : '매도'} 가격
              </label>
              <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                <input
                  type="text"
                  value={isMarket ? '' : price}
                  onChange={e => setPrice(e.target.value)}
                  readOnly={isMarket}
                  placeholder={isMarket ? '시장가 자동' : ''}
                  className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800 placeholder-stone-400"
                />
                <span className="text-sm font-bold text-stone-400">원</span>
              </div>
            </div>

            {/* 수량 / 금액 입력 */}
            {inputMode === 'qty' ? (
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-stone-400">
                  {isBuy ? '매수' : '매도'} 수량
                </label>
                <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                  <input type="text" placeholder="수량 입력" value={qty}
                    onChange={e => setQty(e.target.value)}
                    className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800" />
                  <span className="text-sm font-bold text-stone-400">주</span>
                </div>
              </div>
            ) : (
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-stone-400">
                  {isBuy ? '매수' : '매도'} 금액
                </label>
                <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                  <input type="text" placeholder="금액 입력" value={amount}
                    onChange={e => setAmount(e.target.value)}
                    className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800" />
                  <span className="text-sm font-bold text-stone-400">원</span>
                </div>
                {amount && numPrice > 0 && (
                  <p className="text-[10px] text-stone-400 font-bold text-right">
                    ≈ {Math.floor(Number(amount) / numPrice)}주
                  </p>
                )}
              </div>
            )}

            {/* 비율 버튼 */}
            <div className="grid grid-cols-4 gap-2">
              {['10%', '25%', '50%', '최대'].map(p => (
                <button key={p}
                  className="py-1.5 bg-stone-200 hover:bg-stone-300 rounded-lg text-[10px] font-bold text-stone-400 transition-all border border-stone-200">
                  {p}
                </button>
              ))}
            </div>

            {/* 주문 요약 */}
            <div className="pt-3 border-t border-stone-200 space-y-2">
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">{isBuy ? '매수가능 금액' : '매도가능 수량'}</span>
                <span className="text-stone-800">{isBuy ? '0원' : '0주'}</span>
              </div>
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">총 주문 금액</span>
                <span className="text-stone-800">{numAmount.toLocaleString()}원</span>
              </div>
            </div>

            {/* 주문 버튼 */}
            <button
              onClick={handleSubmit}
              className={cn(
                'w-full py-3.5 text-white rounded-md font-black text-sm transition-colors',
                isBuy
                  ? 'bg-brand-red hover:bg-red-700'
                  : 'bg-brand-blue hover:bg-blue-700'
              )}
            >
              {isBuy ? '매수하기' : '매도하기'}
            </button>

            {/* 비로그인 안내 */}
            {!isLoggedIn && (
              <p className="text-center text-[10px] text-stone-400 font-bold">
                주문하려면 로그인이 필요합니다
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
