import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, ArrowUp, ArrowDown, LayoutGrid, List, ChevronRight } from 'lucide-react';
import { TOKENS, MINI_CHART_DATA } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { cn } from '../lib/utils.js';
import { ResponsiveContainer, LineChart, Line, YAxis } from 'recharts';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { AssetAvatar } from '../components/ui/AssetAvatar.jsx';

export function WatchlistPage() {
  const navigate = useNavigate();
  const { watchlist, toggleWatchlist } = useApp();
  const [viewMode, setViewMode]     = useState('list');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredTokens = TOKENS.filter(token => {
    const matchesSearch = token.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         token.symbol.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSearch && watchlist.includes(token.id);
  });

  function handleToggle(id, e) {
    e.stopPropagation();
    toggleWatchlist(id);
  }

  function getMiniChart(id) {
    return MINI_CHART_DATA[id] || [];
  }

  return (
    <div className="space-y-8 pb-20">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <h2 className="text-3xl font-black text-stone-text-primary tracking-tight mb-2 uppercase">
            관심 <span className="text-stone-gold">종목</span>
          </h2>
          <p className="text-stone-text-secondary text-sm font-bold">내가 찜한 STO의 실시간 시세를 한눈에 확인하세요.</p>
        </div>

        <div className="flex items-center gap-3">
          <SearchInput value={searchQuery} onChange={setSearchQuery} placeholder="종목명 또는 심볼 검색" />
          <div className="flex bg-stone-surface p-1 rounded-xl">
            <button
              onClick={() => setViewMode('list')}
              className={cn('p-2 rounded-lg transition-all', viewMode === 'list' ? 'bg-stone-elevated text-stone-text-primary shadow-sm' : 'text-stone-muted')}
            >
              <List size={18} />
            </button>
            <button
              onClick={() => setViewMode('grid')}
              className={cn('p-2 rounded-lg transition-all', viewMode === 'grid' ? 'bg-stone-elevated text-stone-text-primary shadow-sm' : 'text-stone-muted')}
            >
              <LayoutGrid size={18} />
            </button>
          </div>
        </div>
      </div>

      {viewMode === 'list' ? (
        <div className="bg-stone-surface rounded-[32px] border border-stone-border overflow-hidden shadow-sm">
          <table className="w-full text-left">
            <thead>
              <tr className="text-stone-muted text-[11px] font-black uppercase tracking-widest border-b border-stone-surface">
                <th className="px-8 py-4 w-12"></th>
                <th className="px-4 py-4">종목명</th>
                <th className="px-4 py-4 text-right">현재가</th>
                <th className="px-4 py-4 text-right">전일대비</th>
                <th className="px-4 py-4 text-right">거래대금</th>
                <th className="px-4 py-4 w-32">추이</th>
                <th className="px-8 py-4 w-12"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-bg">
              {filteredTokens.map(token => {
                const isUp = token.change >= 0;
                return (
                  <tr
                    key={token.id}
                    className="hover:bg-stone-bg transition-colors cursor-pointer group"
                    onClick={() => navigate('/trading')}
                  >
                    <td className="px-8 py-6">
                      <button
                        onClick={e => handleToggle(token.id, e)}
                        className={cn('transition-colors', watchlist.includes(token.id) ? 'text-stone-buy' : 'text-stone-border hover:text-stone-buy')}
                      >
                        <Heart size={20} fill={watchlist.includes(token.id) ? 'currentColor' : 'none'} />
                      </button>
                    </td>
                    <td className="px-4 py-6">
                      <div className="flex items-center gap-4">
                        <AssetAvatar symbol={token.symbol} size="md" />
                        <div>
                          <p className="text-sm font-black text-stone-text-primary group-hover:text-stone-gold transition-colors">{token.name}</p>
                          <p className="text-[10px] text-stone-muted font-black uppercase tracking-widest">{token.symbol}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-6 text-right">
                      <p className="text-sm font-black text-stone-text-primary font-mono">₩{token.price.toLocaleString()}</p>
                    </td>
                    <td className="px-4 py-6 text-right">
                      <div className={cn('inline-flex items-center gap-1 text-xs font-black', isUp ? 'text-stone-buy' : 'text-stone-sell')}>
                        {isUp ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
                        {Math.abs(token.change)}%
                      </div>
                    </td>
                    <td className="px-4 py-6 text-right">
                      <p className="text-xs font-bold text-stone-text-secondary">{(token.vol / 100000000).toFixed(1)}억</p>
                    </td>
                    <td className="px-4 py-6">
                      <div className="h-8 w-24">
                        <ResponsiveContainer width="100%" height="100%">
                          <LineChart data={getMiniChart(token.id)}>
                            <YAxis hide domain={['auto', 'auto']} />
                            <Line type="monotone" dataKey="v" stroke={isUp ? '#b85450' : '#4a72a0'} strokeWidth={2} dot={false} />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-right">
                      <ChevronRight size={18} className="text-stone-border group-hover:text-stone-gold transition-colors" />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredTokens.map(token => {
            const isUp = token.change >= 0;
            return (
              <div
                key={token.id}
                onClick={() => navigate('/trading')}
                className="bg-stone-surface rounded-[32px] border border-stone-border p-6 hover:shadow-xl transition-all cursor-pointer group relative overflow-hidden"
              >
                <button
                  onClick={e => handleToggle(token.id, e)}
                  className={cn('absolute top-6 right-6 z-10 transition-colors', watchlist.includes(token.id) ? 'text-stone-buy' : 'text-stone-border hover:text-stone-buy')}
                >
                  <Heart size={20} fill={watchlist.includes(token.id) ? 'currentColor' : 'none'} />
                </button>

                <div className="flex items-center gap-4 mb-6">
                  <AssetAvatar symbol={token.symbol} size="lg" />
                  <div>
                    <h3 className="text-sm font-black text-stone-text-primary group-hover:text-stone-gold transition-colors">{token.name}</h3>
                    <p className="text-[10px] text-stone-muted font-black uppercase tracking-widest">{token.symbol}</p>
                  </div>
                </div>

                <div className="space-y-1 mb-6">
                  <p className="text-2xl font-black text-stone-text-primary font-mono tracking-tighter">₩{token.price.toLocaleString()}</p>
                  <div className={cn('flex items-center gap-1 text-xs font-black', isUp ? 'text-stone-buy' : 'text-stone-sell')}>
                    {isUp ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
                    {Math.abs(token.change)}%
                  </div>
                </div>

                <div className="h-16 w-full mb-4">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={getMiniChart(token.id)}>
                      <YAxis hide domain={['auto', 'auto']} />
                      <Line type="monotone" dataKey="v" stroke={isUp ? '#b85450' : '#4a72a0'} strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                <div className="flex items-center justify-between pt-4 border-t border-stone-bg">
                  <span className="text-[10px] font-black text-stone-muted uppercase tracking-widest">{token.category}</span>
                  <span className="text-[10px] font-black text-stone-text-secondary">
                    거래대금 {(token.vol / 100000000).toFixed(1)}억
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {filteredTokens.length === 0 && (
        <div className="flex flex-col items-center justify-center py-32 space-y-6 text-center">
          <div className="w-20 h-20 bg-stone-surface rounded-full flex items-center justify-center">
            <Heart size={40} className="text-stone-border" />
          </div>
          <div>
            <h3 className="text-xl font-black text-stone-text-primary">관심 종목이 없습니다</h3>
            <p className="text-stone-muted font-bold mt-2">마음에 드는 종목의 하트 버튼을 눌러보세요.</p>
          </div>
          <button
            onClick={() => navigate('/')}
            className="px-8 py-3 bg-stone-gold text-[#1c1c1e] rounded-2xl text-sm font-black hover:bg-stone-gold-light transition-all shadow-lg shadow-stone-gold/20"
          >
            종목 보러가기
          </button>
        </div>
      )}
    </div>
  );
}
