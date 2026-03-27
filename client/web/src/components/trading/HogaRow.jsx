// HogaRow — 호가창 단일 행 (매도/매수 공용)
// 원본: grid grid-cols-2 h-8 border-b hover:bg-[#f7f5f0] cursor-pointer
// side: 'ask'(매도/파랑) | 'bid'(매수/빨강)
// maxAmount: 전체 행 중 최대 잔량 → depth bar 비율 계산

export function HogaRow({ price, amount, changePercent, side, maxAmount }) {
  const isAsk  = side === 'ask';
  const color  = isAsk ? '#4a72a0' : '#b85450';
  const bgTint = isAsk ? 'bg-[#4a72a0]/5' : 'bg-[#b85450]/5';
  const depth  = maxAmount > 0 ? (amount / maxAmount) * 100 : 0;

  return (
    <div className="grid grid-cols-2 h-8 border-b border-[#e0dace]/50 hover:bg-[#f7f5f0] cursor-pointer transition-colors group">
      {/* 가격 + 등락률 */}
      <div className={`flex flex-col items-center justify-center border-r border-[#e0dace]/50 ${bgTint}`}>
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
          style={{ color: isAsk ? '#4a72a0' : '#7a7060' }}
        >
          {amount.toLocaleString()}
        </span>
      </div>
    </div>
  );
}
