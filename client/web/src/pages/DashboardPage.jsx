import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { TOKENS, TREND_DATA } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { cn } from '../lib/utils.js';
import { ResponsiveContainer, LineChart, Line, Tooltip } from 'recharts';
import { TabSwitcher } from '../components/ui/TabSwitcher.jsx';
import { AssetAvatar } from '../components/ui/AssetAvatar.jsx';

export function DashboardPage() {
  const navigate = useNavigate();
  const { watchlist, toggleWatchlist } = useApp();
  const [chartFilter, setChartFilter]   = useState('전체');
  const [timeRange, setTimeRange]       = useState('실시간');
  const [selectedTokenId, setSelectedTokenId] = useState(TOKENS[0].id);

  const selectedToken = TOKENS.find(t => t.id === selectedTokenId) || TOKENS[0];

  const sortedTokens = [...TOKENS].sort((a, b) => {
    if (chartFilter === '거래대금') return b.vol - a.vol;
    if (chartFilter === '거래량')   return (b.vol / b.price) - (a.vol / a.price);
    return 0;
  }).slice(0, 10);

  const currentTrendData = TREND_DATA[timeRange] || TREND_DATA['실시간'];

  return (
    <div className="space-y-6 max-w-[1200px] mx-auto">
      <div className="flex flex-col lg:flex-row gap-8 pt-4">

        {/* 좌: 실시간 차트 테이블 */}
        <div className="flex-1 space-y-6">
          <h2 className="text-xl font-black text-stone-text-primary">실시간 차트</h2>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-stone-surface pb-4">
              <TabSwitcher items={['전체', '거래대금', '거래량']} active={chartFilter} onChange={setChartFilter} />
              <TabSwitcher items={['실시간', '1일', '1주일', '1개월', '3개월', '6개월', '1년']} active={timeRange} onChange={setTimeRange} />
            </div>

            <table className="w-full text-sm">
              <thead>
                <tr className="text-stone-muted text-[11px] font-bold uppercase tracking-wider border-b border-stone-surface">
                  <th className="text-left py-4 font-medium">순위 · {timeRange} 기준</th>
                  <th className="text-right py-4 font-medium">현재가</th>
                  <th className="text-right py-4 font-medium">등락률</th>
                  <th className="text-right py-4 font-medium">{chartFilter === '거래량' ? '거래량' : '거래대금'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-surface">
                {sortedTokens.map((t, i) => (
                  <tr
                    key={t.id}
                    className={cn(
                      'group hover:bg-stone-surface transition-colors cursor-pointer',
                      selectedTokenId === t.id && 'bg-stone-surface'
                    )}
                    onClick={() => setSelectedTokenId(t.id)}
                  >
                    <td className="py-4">
                      <div className="flex items-center gap-4">
                        <button
                          onClick={e => { e.stopPropagation(); toggleWatchlist(t.id); }}
                          className={cn(
                            'transition-colors',
                            watchlist.includes(t.id) ? 'text-stone-buy' : 'text-stone-muted hover:text-stone-buy'
                          )}
                        >
                          <Heart size={16} fill={watchlist.includes(t.id) ? 'currentColor' : 'none'} />
                        </button>
                        <span className="text-stone-muted font-mono w-4">{i + 1}</span>
                        <AssetAvatar symbol={t.symbol} size="sm" />
                        <p className="font-bold text-stone-text-primary group-hover:text-stone-gold transition-colors">{t.name}</p>
                      </div>
                    </td>
                    <td className="py-4 text-right font-mono font-bold text-stone-text-primary">
                      {t.price.toLocaleString()}원
                    </td>
                    <td className={cn('py-4 text-right font-bold', t.change >= 0 ? 'text-stone-buy' : 'text-stone-sell')}>
                      {t.change >= 0 ? '+' : ''}{t.change}%
                    </td>
                    <td className="py-4 text-right text-stone-text-secondary font-mono">
                      {chartFilter === '거래량'
                        ? Math.round(t.vol / t.price).toLocaleString() + '주'
                        : (t.vol / 100000000).toFixed(0) + '억원'
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 우: 종목 프리뷰 */}
        <div className="w-full lg:w-[380px] space-y-6">
          <div className="bg-stone-surface rounded-2xl border border-stone-border p-8 shadow-sm sticky top-24">
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
                <AssetAvatar symbol={selectedToken.symbol} size="md" />
                <div>
                  <h3 className="text-xl font-black text-stone-text-primary">{selectedToken.name}</h3>
                  <p className="text-sm font-bold mt-1">
                    <span className="text-stone-text-primary font-mono">{selectedToken.price.toLocaleString()}원</span>
                    <span className={cn('ml-2', selectedToken.change >= 0 ? 'text-stone-buy' : 'text-stone-sell')}>
                      {selectedToken.change >= 0 ? '+' : ''}{selectedToken.change}%
                    </span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => toggleWatchlist(selectedToken.id)}
                className={cn(
                  'p-3 rounded-2xl transition-all duration-300 border',
                  watchlist.includes(selectedToken.id)
                    ? 'bg-stone-buy-bg text-stone-buy border-stone-buy-bg'
                    : 'bg-stone-bg text-stone-muted border-stone-surface hover:text-stone-buy'
                )}
              >
                <Heart size={20} fill={watchlist.includes(selectedToken.id) ? 'currentColor' : 'none'} />
              </button>
            </div>

            <div className="h-64 mb-8">
              <p className="text-[10px] font-black text-stone-muted uppercase tracking-widest mb-4">{timeRange} 차트</p>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={currentTrendData}>
                  <Tooltip
                    contentStyle={{ backgroundColor: '#3a3a3c', border: '1px solid #636366', borderRadius: '8px', fontSize: '10px' }}
                    itemStyle={{ color: '#ebebf5' }}
                  />
                  <Line
                    type="monotone"
                    dataKey="val"
                    stroke={selectedToken.change >= 0 ? '#b85450' : '#4a72a0'}
                    strokeWidth={3}
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <button
              onClick={() => navigate('/trading')}
              className="w-full py-4 rounded-2xl bg-stone-gold text-[#1c1c1e] font-black uppercase tracking-widest hover:bg-stone-gold-light transition-all shadow-xl shadow-stone-gold/20"
            >
              거래하기
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
