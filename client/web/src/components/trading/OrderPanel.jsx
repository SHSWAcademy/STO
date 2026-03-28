import { useState } from 'react';
import { MoreHorizontal, Edit3, X } from 'lucide-react';
import { PENDING_ORDERS } from '../../data/mock.js';
import { cn } from '../../lib/utils.js';

// OrderPanel — 주문창 (원본 right column, w-[360px])
// 원본 구조:
//   tabs: 매수 / 매도 / 대기
//   매수·매도 탭: 주문유형 + 입력모드 + 가격 + 수량/금액 + 비율 버튼 + 요약 + 제출
//   대기 탭: 미체결 주문 카드 목록

export function OrderPanel({ asset, currentPrice }) {
  const [orderSide, setOrderSide]   = useState('buy');
  const [orderType, setOrderType]   = useState('limit');
  const [inputMode, setInputMode]   = useState('qty');   // 'qty' | 'amount'
  const [price, setPrice]           = useState(String(currentPrice));
  const [qty, setQty]               = useState('');
  const [amount, setAmount]         = useState('');
  const [pendingOrders, setPendingOrders] = useState(PENDING_ORDERS);

  const isBuy     = orderSide === 'buy';
  const isPending = orderSide === 'pending';
  const isMarket  = orderType === 'market';

  const numPrice  = Number(price) || 0;
  const numQty    = inputMode === 'qty' ? (Number(qty) || 0)    : (numPrice > 0 ? Math.floor((Number(amount) || 0) / numPrice) : 0);
  const numAmount = inputMode === 'amt' ? (Number(amount) || 0) : numPrice * numQty;
  const pendingCount = pendingOrders.filter(o => o.assetId === asset.id).length;

  function handleSubmit() {
    // API 연결 시: onSubmit?.({ side: orderSide, orderType, price: numPrice, qty: numQty })
    alert(`${isBuy ? '매수' : '매도'} 주문 (mock)\n가격: ${numPrice.toLocaleString()}원\n수량: ${numQty}주`);
  }

  function handleCancelOrder(id) {
    setPendingOrders(prev => prev.filter(o => o.id !== id));
  }

  return (
    <div className="w-[360px] bg-[#ffffff] rounded-lg border border-stone-200 flex flex-col overflow-hidden">

      {/* ── 탭: 매수 / 매도 / 대기 ────────────────── */}
      <div className="flex border-b border-stone-200">
        <button
          onClick={() => setOrderSide('buy')}
          className={cn(
            'flex-1 py-4 text-sm font-black transition-all',
            orderSide === 'buy'
              ? 'text-brand-red border-b-2 border-brand-red bg-brand-red-light/40'
              : 'text-stone-400 hover:text-stone-500'
          )}
        >
          매수
        </button>
        <button
          onClick={() => setOrderSide('sell')}
          className={cn(
            'flex-1 py-4 text-sm font-black transition-all',
            orderSide === 'sell'
              ? 'text-brand-blue border-b-2 border-brand-blue bg-brand-blue-light/60'
              : 'text-stone-400 hover:text-stone-500'
          )}
        >
          매도
        </button>
        <button
          onClick={() => setOrderSide('pending')}
          className={cn(
            'flex-1 py-4 text-sm font-black transition-all relative',
            orderSide === 'pending'
              ? 'text-stone-800 border-b-2 border-stone-800 bg-stone-100/60'
              : 'text-stone-400 hover:text-stone-500'
          )}
        >
          대기
          {pendingCount > 0 && (
            <span className="absolute top-3 right-4 w-4 h-4 bg-stone-800 text-white text-[8px] font-black rounded-full flex items-center justify-center">
              {pendingCount}
            </span>
          )}
        </button>
      </div>

      {/* ── 탭 콘텐츠 ─────────────────────────────── */}
      <div className="flex-1 overflow-y-auto p-5">

        {/* 대기 탭 */}
        {isPending ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-sm font-black text-stone-800">미체결 주문</h3>
              <span className="text-[10px] font-bold text-stone-400">{pendingOrders.length}건</span>
            </div>

            {pendingOrders.length === 0 ? (
              <div className="py-16 flex flex-col items-center gap-3 text-center">
                <div className="w-12 h-12 bg-stone-200 rounded-lg flex items-center justify-center">
                  <MoreHorizontal size={24} className="text-stone-400" />
                </div>
                <p className="text-sm font-bold text-stone-400">대기 중인 주문이 없습니다</p>
              </div>
            ) : (
              pendingOrders.map(order => (
                <PendingOrderCard
                  key={order.id}
                  order={order}
                  onCancel={handleCancelOrder}
                />
              ))
            )}
          </div>

        ) : (
          /* 매수 / 매도 탭 */
          <div className="space-y-5">
            {/* 주문 유형 + 입력 모드 */}
            <div className="flex items-center justify-between">
              <div className="flex bg-stone-200 p-1 rounded-lg">
                {[{ id: 'limit', label: '지정가' }, { id: 'market', label: '시장가' }].map(t => (
                  <button
                    key={t.id}
                    onClick={() => setOrderType(t.id)}
                    className={cn(
                      'px-3 py-1 rounded-md text-[10px] font-bold transition-all',
                      orderType === t.id
                        ? 'bg-[#ffffff] text-stone-800 shadow-sm'
                        : 'text-stone-400'
                    )}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
              <div className="flex bg-stone-200 p-1 rounded-lg">
                {[{ id: 'qty', label: '수량' }, { id: 'amount', label: '금액' }].map(m => (
                  <button
                    key={m.id}
                    onClick={() => setInputMode(m.id)}
                    className={cn(
                      'px-3 py-1 rounded-md text-[10px] font-bold transition-all',
                      inputMode === m.id
                        ? 'bg-[#ffffff] text-stone-800 shadow-sm'
                        : 'text-stone-400'
                    )}
                  >
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

            {/* 수량 또는 금액 입력 */}
            {inputMode === 'qty' ? (
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-stone-400">
                  {isBuy ? '매수' : '매도'} 수량
                </label>
                <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                  <input
                    type="text"
                    placeholder="수량 입력"
                    value={qty}
                    onChange={e => setQty(e.target.value)}
                    className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800"
                  />
                  <span className="text-sm font-bold text-stone-400">주</span>
                </div>
              </div>
            ) : (
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-stone-400">
                  {isBuy ? '매수' : '매도'} 금액
                </label>
                <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                  <input
                    type="text"
                    placeholder="금액 입력"
                    value={amount}
                    onChange={e => setAmount(e.target.value)}
                    className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800"
                  />
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
                <button
                  key={p}
                  className="py-1.5 bg-stone-200 hover:bg-[#d0ccc0] rounded-lg text-[10px] font-bold text-stone-400 transition-all border border-stone-200"
                >
                  {p}
                </button>
              ))}
            </div>

            {/* 주문 요약 */}
            <div className="pt-3 border-t border-stone-200 space-y-2">
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">
                  {isBuy ? '매수가능 금액' : '매도가능 수량'}
                </span>
                <span className="text-stone-800">
                  {isBuy ? '0원' : '0주'}
                </span>
              </div>
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">총 주문 금액</span>
                <span className="text-stone-800">{numAmount.toLocaleString()}원</span>
              </div>
              {orderType === 'limit' && (
                <div className="flex items-center gap-1.5 text-[10px] text-[#a07828] font-bold bg-[#fef6dc] px-3 py-2 rounded-lg">
                  <MoreHorizontal size={12} />
                  지정가 주문은 체결 전까지 대기 상태로 유지됩니다
                </div>
              )}
            </div>

            {/* 주문 버튼 */}
            <button
              onClick={handleSubmit}
              className={cn(
                'w-full py-3.5 text-white rounded-md font-black text-sm transition-colors',
                isBuy
                  ? 'bg-brand-red hover:bg-[#c92a2a]'
                  : 'bg-brand-blue hover:bg-[#1971c2]'
              )}
            >
              {isBuy ? '매수하기' : '매도하기'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

// ── 미체결 주문 카드 (대기 탭 내부) ────────────────────────────
function PendingOrderCard({ order, onCancel }) {
  const isBuy = order.side === 'buy';

  return (
    <div className="p-4 bg-stone-100 rounded-lg border border-stone-200 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className={cn(
            'text-[10px] font-black px-2 py-0.5 rounded-md',
            isBuy ? 'bg-brand-red-light text-brand-red' : 'bg-brand-blue-light text-brand-blue'
          )}>
            {isBuy ? '매수' : '매도'}
          </span>
          <span className="text-[10px] font-black bg-[#fef6dc] text-[#a07828] px-2 py-0.5 rounded-md">
            대기
          </span>
        </div>
        <span className="text-[9px] text-stone-400 font-bold">{order.time}</span>
      </div>

      <div className="space-y-1.5 text-[11px] font-bold">
        <div className="flex justify-between">
          <span className="text-stone-400">종목</span>
          <span className="text-stone-800">{order.asset}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-stone-400">지정가격</span>
          <span className="font-mono text-stone-800">{order.price.toLocaleString()}원</span>
        </div>
        <div className="flex justify-between">
          <span className="text-stone-400">수량</span>
          <span className="font-mono text-stone-800">{order.qty}주</span>
        </div>
        <div className="flex justify-between border-t border-stone-200 pt-1.5">
          <span className="text-stone-400">주문금액</span>
          <span className="font-mono font-black text-stone-800">{order.amount.toLocaleString()}원</span>
        </div>
      </div>

      <div className="flex gap-2 pt-1">
        <button className="flex-1 py-2 bg-[#ffffff] border border-stone-200 rounded-md text-[11px] font-black text-stone-500 hover:bg-stone-200 transition-all flex items-center justify-center gap-1">
          <Edit3 size={12} /> 수정
        </button>
        <button
          onClick={() => onCancel(order.id)}
          className="flex-1 py-2 bg-brand-red-light border border-brand-red-light rounded-md text-[11px] font-black text-brand-red hover:bg-[#fccfcf] transition-all flex items-center justify-center gap-1"
        >
          <X size={12} /> 취소
        </button>
      </div>
    </div>
  );
}
