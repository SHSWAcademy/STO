import { Heart, Pencil, Info, Sparkles } from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { AssetAvatar } from '../ui/AssetAvatar.jsx';

// AssetHeader — TradingPage 상단 섹션
// 원본: px-8 py-6 border-b border-stone-200 bg-stone-surface
// props.asset: 현재 종목 객체
// props.currentPrice: 현재가
// props.activeTab / props.onTabChange: 탭 전환
// props.isLiked / props.onToggleLike: 관심 토글

const TABS = [
  { id: 'chart',    label: '차트·호가' },
  { id: 'info',     label: '종목정보' },
  { id: 'dividend', label: '배당금 내역' },
  { id: 'news',     label: '공시' },
];

export function AssetHeader({
  asset,
  currentPrice,
  basePrice,
  activeTab,
  onTabChange,
  isLiked,
  onToggleLike,
  hideStats = false,
  aiSummary,
  aiSummaryLoading = false,
}) {
  const isUp         = asset.change >= 0;
  const changeAmount = basePrice > 0 ? Math.abs(currentPrice - basePrice) : 0;
  const yesterday    = new Date(Date.now() - 86400000).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
  const summaryText  = aiSummary ?? asset.aiSummary ?? '';

  return (
      <div className="px-8 py-6 border-b border-stone-200 bg-[#ffffff]">
        {/* 행 1: 종목 정보 + 가격 + 통계 */}
        <div className="flex items-center justify-between gap-6 mb-4">
          <div className="flex min-w-0 items-center gap-4">
            {/* 종목 이미지 */}
            <AssetAvatar
                src={asset.imgUrl}
                symbol={asset.symbol}
                alt={asset.name}
                size="md"
                variant="light"
                className="shadow-lg shadow-brand-blue/20"
            />

            {/* 이름 + 가격 */}
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-black tracking-tight text-stone-800 truncate">{asset.name}</h2>
                <span className="text-stone-500 text-sm font-bold flex-shrink-0">{asset.symbol}</span>
                <Pencil size={14} className="text-stone-400 cursor-pointer hover:text-stone-800 transition-colors" />
              </div>
              <div className="flex items-center gap-3 mt-1 flex-wrap">
              <span className="text-2xl font-black font-mono tracking-tighter text-stone-800">
                {currentPrice.toLocaleString()}원
              </span>
                <span className={cn('text-sm font-bold', isUp ? 'text-brand-red' : 'text-brand-blue')}>
                  {yesterday} 어제보다 {isUp ? '+' : '-'}{changeAmount.toLocaleString()}원 ({asset.change}%)
                </span>
                <div className="flex items-center gap-1 text-[10px] text-stone-400 font-bold bg-stone-200 px-2 py-0.5 rounded-md">
                  실시간 주문 가능 <Info size={10} />
                </div>
              </div>
            </div>
          </div>

          {/* 오른쪽: 통계 + 관심 버튼 */}
          <div className="flex flex-shrink-0 items-center gap-4">
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

            {hideStats && (
                <div
                    className="hidden lg:flex mt-3 w-[460px] min-h-[68px] items-start gap-3 rounded-lg border px-4 py-2.5"
                    style={{
                      borderColor: '#d7e3ff',
                      background: 'linear-gradient(135deg, #f8fbff 0%, #fff8fd 100%)',
                    }}
                >
                  <div
                      className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-md text-white shadow-sm"
                      style={{ background: 'linear-gradient(135deg, #4285f4 0%, #a142f4 52%, #ea4335 100%)' }}
                  >
                    <Sparkles size={14} />
                  </div>
                  <div className="min-w-0">
                    <div className="mb-1 flex items-center gap-2">
                      <span
                          className="text-[10px] font-black uppercase tracking-widest"
                          style={{
                            background: 'linear-gradient(90deg, #4285f4, #a142f4, #ea4335)',
                            WebkitBackgroundClip: 'text',
                            backgroundClip: 'text',
                            color: 'transparent',
                          }}
                      >
                        Gemini AI
                      </span>
                      <span className="h-1 w-1 rounded-full" style={{ backgroundColor: '#fbbc04' }} />
                      <span className="text-[10px] font-bold text-stone-400">한줄 요약</span>
                    </div>
                    <p className="line-clamp-3 text-[12px] font-semibold leading-relaxed text-stone-700">
                      {summaryText || (aiSummaryLoading ? 'AI 요약을 불러오는 중입니다.' : 'AI 요약이 아직 없습니다.')}
                    </p>
                  </div>
                </div>
            )}

            <button
                onClick={() => onToggleLike?.(asset.id)}
                className={cn(
                    'p-2 rounded-lg transition-colors',
                    isLiked
                        ? 'bg-brand-red-light text-brand-red'
                        : 'bg-stone-200 hover:bg-stone-200 text-stone-500'
                )}
            >
              <Heart size={18} fill={isLiked ? 'currentColor' : 'none'} />
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
