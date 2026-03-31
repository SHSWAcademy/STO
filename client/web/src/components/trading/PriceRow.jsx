// PriceRow — 시세 테이블의 단일 행 (체결가 / 체결량 / 등락률 / 거래량 / 시간)
// 원본 TradingPage의 "시세" 섹션 테이블 tbody row

export function PriceRow({ price, qty, changeRate, vol, time }) {
  const isUp = changeRate >= 0;

  return (
    <tr className="hover:bg-stone-100 transition-colors">
      <td className="p-4 font-mono font-bold text-stone-800">{price.toLocaleString()}</td>
      <td className={`p-4 text-right font-mono font-bold ${isUp ? 'text-brand-red' : 'text-brand-blue'}`}>
        {qty}
      </td>
      <td className={`p-4 text-right font-mono font-bold ${isUp ? 'text-brand-red' : 'text-brand-blue'}`}>
        {changeRate.toFixed(2)}%
      </td>
      <td className="p-4 text-right font-mono font-bold text-stone-500">
        {vol.toLocaleString()}
      </td>
      <td className="p-4 text-right font-mono font-bold text-stone-400">{time}</td>
    </tr>
  );
}
