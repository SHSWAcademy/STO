import { Target, CheckCircle, X } from 'lucide-react';
import { HOGA_ASKS, HOGA_BIDS, HOGA_EXECUTIONS } from '../../data/mock.js';
import { HogaRow } from './HogaRow.jsx';

// HogaPanel — 호가창 (원본 middle column, w-[400px])
// 원본 구조: 3-column 내부 레이아웃
//   left(w-24): 체결강도 + 미니 체결 목록
//   center(flex-1): 매도호가(8) + 현재가 divider + 매수호가(8)
//   right(w-28): 통계 패널
// bottom: 판매대기 / 구매대기 합계 바

// 가격 대비 등락률 계산 기준가 (전일 종가 가정)
const BASE_PRICE = 12160;
const MAX_ASK_AMOUNT = Math.max(...HOGA_ASKS.map(r => r.amount));
const MAX_BID_AMOUNT = Math.max(...HOGA_BIDS.map(r => r.amount));

export function HogaPanel({ currentPrice }) {
  return (
    <div className="w-[400px] bg-[#ffffff] rounded-2xl border border-[#e0dace] flex flex-col overflow-hidden shadow-sm text-[#2a2820]">

      {/* 헤더 */}
      <div className="p-4 border-b border-[#e0dace] flex items-center justify-between bg-[#f7f5f0]">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-black text-[#2a2820]">호가</h3>
          <div className="flex items-center gap-1 px-2 py-0.5 bg-[#e0dace] rounded text-[9px] font-bold text-[#9a9080]">
            <CheckCircle size={10} className="text-[#4a72a0]" /> 빠른 주문
          </div>
        </div>
        <div className="flex gap-2">
          <button className="p-1 hover:text-[#2a2820] text-[#9a9080]"><Target size={14} /></button>
          <X size={14} className="text-[#9a9080] cursor-pointer hover:text-[#2a2820]" />
        </div>
      </div>

      {/* ── 3-column body ─────────────────────────── */}
      <div className="flex-1 flex overflow-hidden">

        {/* 왼쪽: 체결강도 + 미니 목록 */}
        <div className="w-24 border-r border-[#e0dace] flex flex-col bg-[#f7f5f0]/50">
          <div className="p-2 border-b border-[#e0dace]">
            <p className="text-[9px] font-bold text-[#9a9080] mb-1">체결강도</p>
            <p className="text-[11px] font-black text-[#4a72a0]">73.71%</p>
          </div>
          <div className="flex-1 overflow-y-auto scrollbar-hide py-2">
            {HOGA_EXECUTIONS.map((ex, i) => (
              <div key={i} className="flex justify-between px-2 py-0.5 text-[9px] font-mono font-bold">
                <span className="text-[#9a9080]">{ex.price.toLocaleString()}</span>
                <span className={ex.isBuy ? 'text-[#b85450]' : 'text-[#4a72a0]'}>{ex.qty}</span>
              </div>
            ))}
          </div>
        </div>

        {/* 중앙: 호가 목록 */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <div className="flex-1 overflow-y-auto scrollbar-hide">

            {/* 매도호가 — 낮은 가격이 아래(현재가 인접), flex-col-reverse로 역순 */}
            <div className="flex flex-col-reverse">
              {HOGA_ASKS.map((row, i) => (
                <HogaRow
                  key={`ask-${i}`}
                  price={row.price}
                  amount={row.amount}
                  changePercent={((row.price - BASE_PRICE) / BASE_PRICE) * 100}
                  side="ask"
                  maxAmount={MAX_ASK_AMOUNT}
                />
              ))}
            </div>

            {/* 현재가 divider */}
            <div className="h-9 bg-[#e0dace] flex items-center justify-center relative">
              <div className="absolute left-2 w-4 h-4 bg-[#4a72a0] rounded flex items-center justify-center text-[9px] font-black text-white">
                저
              </div>
              <div className="flex flex-col items-center">
                <span className="text-xs font-black text-[#2a2820] font-mono tracking-tight">
                  {currentPrice.toLocaleString()}
                </span>
                <span className="text-[8px] font-bold text-[#4a72a0]">
                  {(((currentPrice - BASE_PRICE) / BASE_PRICE) * 100).toFixed(2)}%
                </span>
              </div>
            </div>

            {/* 매수호가 */}
            <div className="flex flex-col">
              {HOGA_BIDS.map((row, i) => (
                <HogaRow
                  key={`bid-${i}`}
                  price={row.price}
                  amount={row.amount}
                  changePercent={((row.price - BASE_PRICE) / BASE_PRICE) * 100}
                  side="bid"
                  maxAmount={MAX_BID_AMOUNT}
                />
              ))}
            </div>
          </div>
        </div>

        {/* 오른쪽: 통계 패널 */}
        <div className="w-28 border-l border-[#e0dace] bg-[#f7f5f0] flex flex-col p-2 space-y-4 overflow-y-auto scrollbar-hide">
          <div className="space-y-1">
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>상한가</span>
              <span className="text-[#b85450]">259,000</span>
            </div>
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>하한가</span>
              <span className="text-[#4a72a0]">139,600</span>
            </div>
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>상승VI</span><span>-</span>
            </div>
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>하강VI</span><span>-</span>
            </div>
          </div>
          <div className="h-px bg-[#e0dace]" />
          <div className="space-y-1">
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>시작</span><span>12,160</span>
            </div>
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>최고</span>
              <span className="text-[#b85450]">{Math.max(...HOGA_ASKS.map(r => r.price)).toLocaleString()}</span>
            </div>
            <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
              <span>최저</span>
              <span className="text-[#4a72a0]">{Math.min(...HOGA_BIDS.map(r => r.price)).toLocaleString()}</span>
            </div>
          </div>
          <div className="h-px bg-[#e0dace]" />
          <div className="space-y-1">
            <p className="text-[8px] font-bold text-[#9a9080]">거래량</p>
            <p className="text-[9px] font-black text-[#2a2820]">4,821만</p>
            <p className="text-[8px] font-bold text-[#9a9080]">어제보다 <span className="text-[#2a2820] ml-1">91.81%</span></p>
          </div>
          <div className="h-px bg-[#e0dace]" />
          <div className="flex justify-between text-[8px] font-bold text-[#9a9080]">
            <span>중간호가</span><span>-</span>
          </div>
        </div>
      </div>

      {/* 하단 요약 바 */}
      <div className="h-10 bg-[#f7f5f0] border-t border-[#e0dace] flex items-center justify-between px-4 text-[9px] font-black">
        <div className="flex gap-2">
          <span className="text-[#9a9080]">판매대기</span>
          <span className="text-[#4a72a0]">
            {HOGA_ASKS.reduce((s, r) => s + r.amount, 0).toLocaleString()}
          </span>
        </div>
        <span className="text-[#9a9080]">애프터마켓</span>
        <div className="flex gap-2">
          <span className="text-[#b85450]">
            {HOGA_BIDS.reduce((s, r) => s + r.amount, 0).toLocaleString()}
          </span>
          <span className="text-[#9a9080]">구매대기</span>
        </div>
      </div>
    </div>
  );
}
