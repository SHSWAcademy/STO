import { useState } from 'react';
import {
  ResponsiveContainer,
  ComposedChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { ChartLine, Filter, Settings, Maximize2, X } from 'lucide-react';
import { CHART_DATA, PRICE_HISTORY_ROWS } from '../../data/mock.js';
import { PriceRow } from './PriceRow.jsx';
import { cn } from '../../lib/utils.js';

// ChartPanel — TradingPage 좌측 컬럼
// - 차트 섹션 (캔들스틱 + 거래량)
// - 시세 섹션 (실시간/일별 탭 + 체결 테이블)
// state: chartPeriod, priceHistoryTab (로컬 UI 제어만)

// 캔들스틱 커스텀 shape — 원본 CandlestickShape와 동일
function CandlestickShape(props) {
  const { x, y, width, height, payload } = props;
  if (!payload) return null;
  const { open, close, high, low } = payload;
  const isUp = close >= open;
  const color = isUp ? 'var(--color-brand-red)' : 'var(--color-brand-blue)';
  const priceHeight = Math.abs(open - close);
  const ratio = priceHeight === 0 ? 1 : height / priceHeight;
  const wickTop    = y - (high - Math.max(open, close)) * ratio;
  const wickBottom = y + height + (Math.min(open, close) - low) * ratio;

  return (
    <g>
      <line x1={x + width / 2} y1={wickTop} x2={x + width / 2} y2={wickBottom} stroke={color} strokeWidth={1} />
      <rect x={x} y={y} width={width} height={Math.max(height, 1)} fill={color} />
    </g>
  );
}

const CHART_PERIODS = ['1분', '일', '주', '월', '년'];

export function ChartPanel({ currentPrice }) {
  const [chartPeriod, setChartPeriod]         = useState('일');
  const [priceHistoryTab, setPriceHistoryTab] = useState('realtime');

  // 호버된 캔들 표시용
  const [hoveredData, setHoveredData] = useState(null);
  const displayData = hoveredData || CHART_DATA[CHART_DATA.length - 1];

  return (
    <div className="flex-[2] flex flex-col gap-6 overflow-hidden">

      {/* ── 차트 섹션 ───────────────────────────────── */}
      <div className="bg-[#ffffff] rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">

        {/* 차트 헤더 */}
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
                      ? 'bg-[#ffffff] text-stone-800 shadow-sm'
                      : 'text-stone-400 hover:text-stone-500'
                  )}
                >
                  {p}
                </button>
              ))}
            </div>
            <div className="flex gap-3 text-stone-400">
              <ChartLine  size={16} className="cursor-pointer hover:text-stone-800" />
              <Filter     size={16} className="cursor-pointer hover:text-stone-800" />
              <Settings   size={16} className="cursor-pointer hover:text-stone-800" />
            </div>
          </div>
          <div className="flex items-center gap-4 text-[11px] font-bold text-stone-400">
            <button className="flex items-center gap-1 hover:text-stone-800 transition-colors">
              차트 크게보기 <Maximize2 size={14} />
            </button>
          </div>
        </div>

        {/* 차트 영역 */}
        <div className="h-[520px] p-6 relative">
          {/* 좌상단 OHLCV 인디케이터 */}
          <div className="absolute top-6 left-6 z-10 flex flex-col gap-1 pointer-events-none">
            <div className="flex items-center gap-3">
              {[
                { label: '시', val: displayData?.open },
                { label: '고', val: displayData?.high, color: 'var(--color-brand-red)' },
                { label: '저', val: displayData?.low,  color: 'var(--color-brand-blue)' },
                { label: '종', val: displayData?.close },
              ].map(({ label, val, color }) => (
                <div key={label} className="flex items-center gap-1">
                  <span className="text-[10px] font-bold text-stone-400">{label}</span>
                  <span className="text-[11px] font-mono font-bold text-stone-800" style={color ? { color } : {}}>
                    {val?.toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
            <p className="text-[10px] font-bold text-stone-400">
              이동평균선 <span className="text-brand-red">5</span>{' '}
              <span className="text-brand-red">20</span>{' '}
              <span className="text-brand-gold">60</span>{' '}
              <span className="text-stone-800">120</span>
            </p>
          </div>

          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart
              data={CHART_DATA}
              margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
              onMouseMove={e => {
                if (e?.activePayload?.length > 0) setHoveredData(e.activePayload[0].payload);
              }}
              onMouseLeave={() => setHoveredData(null)}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-stone-200)" vertical={false} />
              <XAxis
                dataKey="time"
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 10, fill: 'var(--color-stone-400)' }}
                minTickGap={30}
              />
              <YAxis
                yAxisId="price"
                domain={['auto', 'auto']}
                orientation="right"
                tick={{ fontSize: 10, fill: 'var(--color-stone-400)', fontWeight: 'bold' }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                yAxisId="vol"
                orientation="left"
                domain={[0, dataMax => dataMax * 4]}
                tick={{ fontSize: 9, fill: 'var(--color-stone-400)' }}
                axisLine={false}
                tickLine={false}
                tickFormatter={val => `${(val / 10000).toFixed(0)}만`}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'var(--color-stone-100)',
                  border: '1px solid var(--color-stone-200)',
                  borderRadius: '12px',
                  fontSize: '11px',
                  color: 'var(--color-stone-800)',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
                }}
                itemStyle={{ fontWeight: 'bold' }}
                formatter={(value, name) => {
                  if (name === 'vol') return [`${value.toLocaleString()}주`, '거래량'];
                  return [`${value.toLocaleString()}원`, '가격'];
                }}
              />
              {/* 거래량 바 */}
              <Bar
                yAxisId="vol"
                dataKey="vol"
                name="vol"
                fill="var(--color-stone-400)"
                opacity={0.1}
                radius={[2, 2, 0, 0]}
              />
              {/* 캔들스틱 */}
              <Bar
                yAxisId="price"
                dataKey={d => [d.open, d.close]}
                shape={<CandlestickShape />}
                animationDuration={0}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── 시세 섹션 ────────────────────────────────── */}
      <div className="bg-[#ffffff] rounded-2xl border border-stone-200 flex flex-col overflow-hidden shadow-sm">
        {/* 시세 헤더 */}
        <div className="p-4 border-b border-stone-200 flex items-center justify-between">
          <h3 className="text-sm font-bold text-stone-800">시세</h3>
          <div className="flex bg-stone-200 p-1 rounded-lg">
            {[
              { id: 'realtime', label: '실시간' },
              { id: 'daily',    label: '일별' },
            ].map(t => (
              <button
                key={t.id}
                onClick={() => setPriceHistoryTab(t.id)}
                className={cn(
                  'px-8 py-1 rounded-md text-[11px] font-bold transition-all',
                  priceHistoryTab === t.id
                    ? 'bg-[#ffffff] text-stone-800 shadow-sm'
                    : 'text-stone-400'
                )}
              >
                {t.label}
              </button>
            ))}
          </div>
          <X size={14} className="text-stone-400 cursor-pointer" />
        </div>

        {/* 시세 테이블 */}
        <div className="flex-1 overflow-y-auto">
          <table className="w-full text-[11px]">
            <thead className="text-stone-400 border-b border-stone-200 sticky top-0 bg-[#ffffff]">
              <tr>
                <th className="text-left p-4 font-bold">체결가</th>
                <th className="text-right p-4 font-bold">체결량(주)</th>
                <th className="text-right p-4 font-bold">등락률</th>
                <th className="text-right p-4 font-bold">거래량(주)</th>
                <th className="text-right p-4 font-bold">시간</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {PRICE_HISTORY_ROWS.map((row, i) => (
                <PriceRow key={i} {...row} />
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
