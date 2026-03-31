// HogaRow — 호가창 단일 행 (매도/매수 공용)
// 원본: grid grid-cols-2 h-8 border-b hover:bg-stone-100 cursor-pointer
// side: 'ask'(매도/파랑) | 'bid'(매수/빨강)
// maxAmount: 전체 행 중 최대 잔량 → depth bar 비율 계산

export function HogaRow({ price, amount, changePercent, side, maxAmount }) {
  const isAsk  = side === 'ask';
  const color  = isAsk ? 'var(--color-brand-blue)' : 'var(--color-brand-red)';
  const bgTint = isAsk ? 'bg-brand-blue/5' : 'bg-brand-red/5';
  const depth  = maxAmount > 0 ? (amount / maxAmount) * 100 : 0;

  return (
    <div className="grid grid-cols-2 h-8 border-b border-stone-200/50 hover:bg-stone-100 cursor-pointer transition-colors group">
      {/* 가격 + 등락률 */}
      <div className={`flex flex-col items-center justify-center border-r border-stone-200/50 ${bgTint}`}>
        <span className="text-[10px] font-mono font-black" style={{ color }}>
          {price.toLocaleString()}
        </span>
        <span className="text-[7px] font-bold" style={{ color, opacity: 0.7 }}>
          {changePercent.toFixed(2)}%
        </span>
      </div>

      {/* 잔량 + depth bar */}
      <div className="relative flex items-center px-2">
        <div
          className="absolute left-0 top-0 bottom-0 transition-all"
          style={{ width: `${depth}%`, backgroundColor: color, opacity: 0.1 }}
        />
        <span
          className="relative z-10 text-[9px] font-mono font-bold ml-auto"
          style={{ color: isAsk ? 'var(--color-brand-blue)' : 'var(--color-stone-500)' }}
        >
          {amount.toLocaleString()}
        </span>
      </div>
    </div>
  );
}
