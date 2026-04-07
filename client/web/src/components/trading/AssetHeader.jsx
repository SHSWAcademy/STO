import { Heart, Pencil, Info } from 'lucide-react';
import { cn } from '../../lib/utils.js';

// AssetHeader — TradingPage 상단 섹션
// 원본: px-8 py-6 border-b border-stone-200 bg-stone-surface
// props.asset: 현재 종목 객체
// props.currentPrice: 현재가
// props.activeTab / props.onTabChange: 탭 전환
// props.inWatchlist / props.onToggleWatchlist: 관심 토글

const TABS = [
  { id: 'chart',    label: '차트·호가' },
  { id: 'info',     label: '종목정보' },
  { id: 'dividend', label: '배당금 내역' },
  { id: 'news',     label: '공시' },
];

export function AssetHeader({ asset, currentPrice, activeTab, onTabChange, inWatchlist, onToggleWatchlist, hideStats = false }) {
  const isUp = asset.change >= 0;
  const changeAmount = Math.abs(Math.round((currentPrice * asset.change) / 100));

  return (
      <div className="px-8 py-6 border-b border-stone-200 bg-[#ffffff]">
        {/* 행 1: 종목 정보 + 가격 + 통계 */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-4">
            {/* 종목 이미지 */}
            <div className="w-10 h-10 rounded-lg overflow-hidden shadow-lg shadow-brand-blue/20">
              <img
                  src={`https://picsum.photos/seed/${asset.name}/100/100`}
                  alt={asset.name}
                  className="w-full h-full object-cover"
                  referrerPolicy="no-referrer"
              />
            </div>

            {/* 이름 + 가격 */}
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-black tracking-tight text-stone-800">{asset.name}</h2>
                <span className="text-stone-500 text-sm font-bold">{asset.symbol}</span>
                <Pencil size={14} className="text-stone-400 cursor-pointer hover:text-stone-800 transition-colors" />
              </div>
              <div className="flex items-center gap-3 mt-1">
              <span className="text-2xl font-black font-mono tracking-tighter text-stone-800">
                {currentPrice.toLocaleString()}원
              </span>
                <span className={cn('text-sm font-bold', isUp ? 'text-brand-red' : 'text-brand-blue')}>
                3월 20일보다 {isUp ? '+' : '-'}{changeAmount.toLocaleString()}원 ({asset.change}%)
              </span>
                <div className="flex items-center gap-1 text-[10px] text-stone-400 font-bold bg-stone-200 px-2 py-0.5 rounded-md">
                  실시간 주문 가능 <Info size={10} />
                </div>
              </div>
            </div>
          </div>

          {/* 오른쪽: 통계 + 관심 버튼 */}
          <div className="flex items-center gap-8">
            {!hideStats && (
                <div className="flex gap-6 text-[11px] font-bold text-stone-400 uppercase tracking-widest">
                  <div>
                    <p className="mb-1">1일 최고</p>
                    <p className="text-stone-800 font-mono">{asset.high.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="mb-1">1일 최저</p>
                    <p className="text-stone-800 font-mono">{asset.low.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="mb-1">52주 최고</p>
                    <p className="text-stone-800 font-mono">{Math.round(asset.high * 1.2).toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="mb-1">52주 최저</p>
                    <p className="text-stone-800 font-mono">{Math.round(asset.low * 0.8).toLocaleString()}</p>
                  </div>
                </div>
            )}

            <button
                onClick={() => onToggleWatchlist?.(asset.id)}
                className={cn(
                    'p-2 rounded-lg transition-colors',
                    inWatchlist
                        ? 'bg-brand-red-light text-brand-red'
                        : 'bg-stone-200 hover:bg-stone-200 text-stone-500'
                )}
            >
              <Heart size={18} fill={inWatchlist ? 'currentColor' : 'none'} />
            </button>
          </div>
        </div>

        {/* 행 2: 탭 */}
        <div className="flex gap-6">
          {TABS.map(tab => (
              <button
                  key={tab.id}
                  onClick={() => onTabChange?.(tab.id)}
                  className={cn(
                      'text-sm font-bold pb-2 transition-all border-b-2',
                      activeTab === tab.id
                          ? 'text-stone-800 border-stone-800'
                          : 'text-stone-400 border-transparent hover:text-stone-500'
                  )}
              >
                {tab.label}
              </button>
          ))}
        </div>
      </div>
  );
}
