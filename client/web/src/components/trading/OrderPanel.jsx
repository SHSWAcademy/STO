import { useState, useEffect, useCallback } from 'react';
import { MoreHorizontal, Edit3, X } from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { API_BASE_URL } from '../../lib/config.js';

const API = API_BASE_URL;

// OrderPanel — 주문창
// Props:
//   asset        : { id, name, ... }
//   currentPrice : number
//   tokenId      : number  — 백엔드 토큰 ID
//   token        : string  — JWT accessToken (없으면 비로그인 상태)
//   wsPendingData: array   — WebSocket 실시간 대기 주문 (없으면 REST 폴링)

export function OrderPanel({ asset, currentPrice, tokenId, token, wsPendingData }) {
  const [orderSide, setOrderSide] = useState('buy');
  const [inputMode, setInputMode] = useState('qty');   // 'qty' | 'amount'
  const [price, setPrice]         = useState(currentPrice > 0 ? String(currentPrice) : '');
  const [qty, setQty]             = useState('');
  const [amount, setAmount]       = useState('');
  const [accountPassword, setAccountPassword] = useState('');
  const [submitting, setSubmitting]   = useState(false);
  const [orderMsg, setOrderMsg]       = useState(null); // { type: 'success'|'error', text }

  // 대기 주문 상태
  const [pendingOrders, setPendingOrders]   = useState([]);
  const [pendingLoading, setPendingLoading] = useState(false);

  // 주문 수정 상태
  const [editingOrderId, setEditingOrderId] = useState(null);
  const [editPrice, setEditPrice]           = useState('');
  const [editQty, setEditQty]               = useState('');
  const [editAccountPassword, setEditAccountPassword] = useState('');
  const [updateMsg, setUpdateMsg]           = useState(null); // { orderId, type, text }
  const [capacity, setCapacity]             = useState({ availableBalance: 0, availableQuantity: 0 });

  // currentPrice 변경 시 가격 필드 자동 세팅 (비어있을 때만)
  useEffect(() => {
    if (currentPrice > 0) setPrice(p => p === '' ? String(currentPrice) : p);
  }, [currentPrice]);

  // WebSocket 실시간 대기 주문 수신
  useEffect(() => {
    if (!wsPendingData) return;

    if (editingOrderId === null) {
      setPendingOrders(wsPendingData);
      return;
    }

    const editingOrderExists = wsPendingData.some(o => o.orderId === editingOrderId);
    if (!editingOrderExists) {
      setPendingOrders(wsPendingData);
      setEditingOrderId(null);
      setEditPrice('');
      setEditQty('');
      setUpdateMsg({
        orderId: editingOrderId,
        type: 'error',
        text: '편집 중인 주문이 체결되었거나 취소되어 편집이 종료되었습니다.',
      });
      return;
    }

    setPendingOrders(prev =>
      wsPendingData.map(incoming => {
        if (incoming.orderId === editingOrderId) {
          return prev.find(o => o.orderId === editingOrderId) ?? incoming;
        }
        return incoming;
      })
    );
  }, [wsPendingData, editingOrderId]);

  const isBuy     = orderSide === 'buy';
  const isPending = orderSide === 'pending';
  const isLoggedIn = !!token;

  const numPrice  = Number(price) || 0;
  const numQty    = inputMode === 'qty'
    ? (Number(qty) || 0)
    : (numPrice > 0 ? Math.floor((Number(amount) || 0) / numPrice) : 0);
  const numAmount = inputMode === 'amount'
    ? (Number(amount) || 0)
    : numPrice * numQty;

  function isValidAccountPassword(value) {
    return /^\d{4}$/.test(value);
  }

  function getUserFriendlyErrorMessage(message, fallback) {
    if (!message || /^HTTP \d+$/.test(message)) {
      return fallback;
    }
    if (message.includes('비밀번호')) {
      return '계좌 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.';
    }
    if (message.includes('잔고')) {
      return '주문 가능 금액이 부족합니다. 잔고를 확인해 주세요.';
    }
    if (message.includes('수량')) {
      return '주문 수량을 다시 확인해 주세요.';
    }
    return message;
  }

  // 대기 주문 REST 조회
  const fetchPendingOrders = useCallback(async () => {
    if (!isLoggedIn || !token || !tokenId) return;
    setPendingLoading(true);
    try {
      const res = await fetch(`${API}/api/token/${tokenId}/order/pending`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setPendingOrders(data);
    } catch (e) {
      console.warn('[OrderPanel] 대기 주문 조회 실패:', e.message);
    } finally {
      setPendingLoading(false);
    }
  }, [isLoggedIn, token, tokenId]);

  useEffect(() => {
    if (orderSide === 'pending') fetchPendingOrders();
  }, [orderSide, fetchPendingOrders]);

  // 가용 잔고/수량 조회
  const fetchCapacity = useCallback(async () => {
    if (!isLoggedIn || !tokenId) return;
    try {
      const res = await fetch(`${API}/api/token/${tokenId}/order/capacity`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return;
      const data = await res.json();
      setCapacity({
        availableBalance:  data.availableBalance  ?? 0,
        availableQuantity: data.availableQuantity ?? 0,
      });
    } catch (e) {
      console.warn('[OrderPanel] capacity 조회 실패:', e.message);
    }
  }, [isLoggedIn, token, tokenId]);

  useEffect(() => {
    if (orderSide === 'buy' || orderSide === 'sell') fetchCapacity();
  }, [orderSide, fetchCapacity]);

  // 주문 제출
  async function handleSubmit() {
    if (!isLoggedIn) {
      setOrderMsg({ type: 'error', text: '로그인 후 주문할 수 있습니다.' });
      return;
    }
    if (!Number.isInteger(numPrice) || !Number.isInteger(numQty) || numPrice <= 0 || numQty <= 0) {
      setOrderMsg({ type: 'error', text: '가격과 수량을 올바르게 입력해 주세요.' });
      return;
    }
    if (!isValidAccountPassword(accountPassword)) {
      setOrderMsg({ type: 'error', text: '계좌 비밀번호 4자리를 입력해 주세요.' });
      return;
    }
    setSubmitting(true);
    setOrderMsg(null);
    try {
      const res = await fetch(`${API}/api/token/${tokenId}/order`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          orderPrice:    numPrice,
          orderQuantity: numQty,
          orderType:     isBuy ? 'BUY' : 'SELL',
          accountPassword,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${res.status}`);
      }
      setOrderMsg({ type: 'success', text: `${isBuy ? '매수' : '매도'} 주문이 정상적으로 접수되었습니다.` });
      setQty('');
      setAmount('');
      setAccountPassword('');
      fetchCapacity();
    } catch (e) {
      setOrderMsg({
        type: 'error',
        text: getUserFriendlyErrorMessage(e.message, '주문 접수 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  // 주문 취소
  async function handleCancelOrder(orderId) {
    if (!token) return;
    try {
      const res = await fetch(`${API}/api/token/order/cancel/${orderId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setPendingOrders(prev => prev.filter(o => o.orderId !== orderId));
    } catch (e) {
      console.warn('[OrderPanel] 주문 취소 실패:', e.message);
    }
  }

  // 주문 수정 시작
  function handleEditStart(o) {
    setEditingOrderId(o.orderId);
    setEditPrice(String(o.orderPrice ?? ''));
    setEditQty(String(o.orderQuantity ?? ''));
    setEditAccountPassword('');
    setUpdateMsg(null);
  }

  function handleEditCancel() {
    setEditingOrderId(null);
    setEditPrice('');
    setEditQty('');
    setEditAccountPassword('');
    setUpdateMsg(null);
  }

  // 주문 수정 제출
  async function handleUpdateOrder(orderId) {
    const p = Number(editPrice);
    const q = Number(editQty);
    if (!isValidAccountPassword(editAccountPassword)) {
      setUpdateMsg({ orderId, type: 'error', text: '계좌 비밀번호 4자리를 입력해 주세요.' });
      return;
    }
    if (!Number.isInteger(p) || !Number.isInteger(q) || p <= 0 || q <= 0) {
      setUpdateMsg({ orderId, type: 'error', text: '가격과 수량을 올바르게 입력해 주세요.' });
      return;
    }
    try {
      const res = await fetch(`${API}/api/token/order/update/${orderId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          updatePrice: p,
          updateQuantity: q,
          accountPassword: editAccountPassword,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${res.status}`);
      }
      setPendingOrders(prev =>
        prev.map(o => {
          if (o.orderId !== orderId) return o;
          const filledQuantity    = Number(o.filledQuantity) || 0;
          const remainingQuantity = Math.max(q - filledQuantity, 0);
          return { ...o, orderPrice: p, remainingQuantity, orderQuantity: q };
        })
      );
      setEditingOrderId(null);
      setEditPrice('');
      setEditQty('');
      setEditAccountPassword('');
      setUpdateMsg(null);
    } catch (e) {
      setUpdateMsg({
        orderId,
        type: 'error',
        text: getUserFriendlyErrorMessage(e.message, '주문 수정 중 오류가 발생했습니다. 다시 시도해 주세요.'),
      });
    }
  }

  const RATIO_MAP = { '10%': 0.1, '25%': 0.25, '50%': 0.5, '최대': 1.0 };

  function handleRatioClick(label) {
    const pct = RATIO_MAP[label];
    const newQty = isBuy
      ? (numPrice > 0 ? Math.floor(capacity.availableBalance * pct / numPrice) : 0)
      : Math.floor(capacity.availableQuantity * pct);
    setQty(String(newQty));
    setInputMode('qty');
  }

  return (
    <div className="w-[360px] bg-[#ffffff] rounded-lg border border-stone-200 flex flex-col overflow-hidden">

      {/* ── 탭: 매수 / 매도 / 대기 ── */}
      <div className="flex border-b border-stone-200">
        {[
          { id: 'buy',     label: '매수', active: 'text-brand-red border-b-2 border-brand-red bg-brand-red-light/40' },
          { id: 'sell',    label: '매도', active: 'text-brand-blue border-b-2 border-brand-blue bg-brand-blue-light/60' },
          { id: 'pending', label: '대기', active: 'text-stone-800 border-b-2 border-stone-800 bg-stone-100/60' },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => { setOrderMsg(null); setOrderSide(t.id); }}
            className={cn(
              'flex-1 py-4 text-sm font-black transition-all',
              orderSide === t.id ? t.active : 'text-stone-400 hover:text-stone-500'
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── 탭 콘텐츠 ── */}
      <div className="flex-1 overflow-y-auto p-5">

        {/* 대기 탭 */}
        {isPending ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-stone-400">미체결 주문</span>
              <button
                onClick={fetchPendingOrders}
                disabled={pendingLoading}
                className="text-[10px] font-bold text-stone-400 hover:text-stone-700 transition-colors"
              >
                {pendingLoading ? '조회 중...' : '새로고침'}
              </button>
            </div>

            {pendingLoading && pendingOrders.length === 0 ? (
              <div className="py-16 flex flex-col items-center gap-3 text-center">
                <div className="w-12 h-12 bg-stone-200 rounded-lg flex items-center justify-center">
                  <MoreHorizontal size={24} className="text-stone-400 animate-pulse" />
                </div>
                <p className="text-sm font-bold text-stone-400">조회 중...</p>
              </div>
            ) : pendingOrders.length === 0 ? (
              <div className="py-16 flex flex-col items-center gap-3 text-center">
                <div className="w-12 h-12 bg-stone-200 rounded-lg flex items-center justify-center">
                  <MoreHorizontal size={24} className="text-stone-400" />
                </div>
                <p className="text-sm font-bold text-stone-400">대기 중인 주문이 없습니다</p>
              </div>
            ) : (
              <div className="space-y-3">
                {pendingOrders.map(o => {
                  const orderIsBuy  = o.orderType === 'BUY';
                  const isProcessing = o.orderStatus === 'PENDING';
                  const isEditing   = editingOrderId === o.orderId;
                  const totalAmount = isEditing
                    ? (Number(editPrice) || 0) * (Number(editQty) || 0)
                    : (o.orderPrice ?? 0) * (o.orderQuantity ?? 0);

                  return (
                    <div key={o.orderId} className="p-4 bg-stone-100 rounded-lg border border-stone-200 space-y-3">
                      {/* 헤더 */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className={cn(
                            'text-[10px] font-black px-2 py-0.5 rounded-md',
                            orderIsBuy ? 'bg-brand-red-light text-brand-red' : 'bg-brand-blue-light text-brand-blue'
                          )}>
                            {orderIsBuy ? '매수' : '매도'}
                          </span>
                          <span className={cn(
                            'text-[10px] font-black px-2 py-0.5 rounded-md',
                            isProcessing
                              ? 'bg-stone-200 text-stone-400'
                              : isEditing
                                ? 'bg-blue-100 text-blue-600'
                                : 'bg-[#fef6dc] text-[#a07828]'
                          )}>
                            {isProcessing ? '처리중' : isEditing ? '수정중' : '대기'}
                          </span>
                        </div>
                        <span className="text-[9px] text-stone-400 font-bold">
                          {o.createdAt
                            ? new Date(o.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
                            : '-'}
                        </span>
                      </div>

                      {/* 수정 모드 */}
                      {isEditing ? (
                        <div className="space-y-2">
                          <div className="space-y-1">
                            <label className="text-[10px] font-bold text-stone-400">수정 가격</label>
                            <div className="flex items-center gap-2 bg-white border border-stone-300 rounded-md px-3 py-2">
                              <input
                                type="number" min="1" step="1"
                                value={editPrice}
                                onChange={e => setEditPrice(e.target.value)}
                                className="flex-1 bg-transparent text-[11px] font-mono font-bold outline-none text-right text-stone-800"
                              />
                              <span className="text-[11px] font-bold text-stone-400">원</span>
                            </div>
                          </div>
                          <div className="space-y-1">
                            <label className="text-[10px] font-bold text-stone-400">수정 수량</label>
                            <div className="flex items-center gap-2 bg-white border border-stone-300 rounded-md px-3 py-2">
                              <input
                                type="number" min="1" step="1"
                                value={editQty}
                                onChange={e => setEditQty(e.target.value)}
                                className="flex-1 bg-transparent text-[11px] font-mono font-bold outline-none text-right text-stone-800"
                              />
                              <span className="text-[11px] font-bold text-stone-400">주</span>
                            </div>
                          </div>
                          <div className="space-y-1">
                            <label className="text-[10px] font-bold text-stone-400">계좌 비밀번호</label>
                            <div className="flex items-center gap-2 bg-white border border-stone-300 rounded-md px-3 py-2">
                              <input
                                type="password"
                                inputMode="numeric"
                                maxLength={4}
                                value={editAccountPassword}
                                onChange={e => setEditAccountPassword(e.target.value.replace(/\D/g, '').slice(0, 4))}
                                placeholder="숫자 4자리"
                                className="flex-1 bg-transparent text-[11px] font-mono font-bold outline-none text-right text-stone-800 placeholder-stone-400"
                              />
                            </div>
                          </div>
                          <div className="flex justify-between text-[11px] font-bold border-t border-stone-200 pt-1.5">
                            <span className="text-stone-400">주문금액</span>
                            <span className="font-mono font-black text-stone-800">{totalAmount.toLocaleString()}원</span>
                          </div>
                          {updateMsg?.orderId === o.orderId && (
                            <p className={cn(
                              'text-[10px] font-bold text-center',
                              updateMsg.type === 'error' ? 'text-brand-red' : 'text-green-600'
                            )}>
                              {updateMsg.text}
                            </p>
                          )}
                          <div className="flex gap-2 pt-1">
                            <button
                              onClick={() => handleUpdateOrder(o.orderId)}
                              className="flex-1 py-2 bg-stone-800 border border-stone-800 rounded-md text-[11px] font-black text-white hover:bg-stone-700 transition-all"
                            >
                              확인
                            </button>
                            <button
                              onClick={handleEditCancel}
                              className="flex-1 py-2 bg-white border border-stone-200 rounded-md text-[11px] font-black text-stone-500 hover:bg-stone-200 transition-all"
                            >
                              취소
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          {/* 일반 표시 모드 */}
                          <div className="space-y-1.5 text-[11px] font-bold">
                            <div className="flex justify-between">
                              <span className="text-stone-400">지정가격</span>
                              <span className="font-mono text-stone-800">{o.orderPrice?.toLocaleString()}원</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-stone-400">미체결/전체</span>
                              <span className="font-mono text-stone-800">{o.remainingQuantity} / {o.orderQuantity}주</span>
                            </div>
                            <div className="flex justify-between border-t border-stone-200 pt-1.5">
                              <span className="text-stone-400">주문금액</span>
                              <span className="font-mono font-black text-stone-800">{totalAmount.toLocaleString()}원</span>
                            </div>
                          </div>
                          <div className="flex gap-2 pt-1">
                            <button
                              disabled={isProcessing}
                              onClick={() => !isProcessing && handleEditStart(o)}
                              className={cn(
                                'flex-1 py-2 border rounded-md text-[11px] font-black transition-all flex items-center justify-center gap-1',
                                isProcessing
                                  ? 'bg-stone-100 border-stone-200 text-stone-300 cursor-not-allowed'
                                  : 'bg-[#ffffff] border-stone-200 text-stone-500 hover:bg-stone-200'
                              )}
                            >
                              <Edit3 size={12} /> 수정
                            </button>
                            <button
                              disabled={isProcessing}
                              onClick={() => !isProcessing && handleCancelOrder(o.orderId)}
                              className={cn(
                                'flex-1 py-2 border rounded-md text-[11px] font-black transition-all flex items-center justify-center gap-1',
                                isProcessing
                                  ? 'bg-stone-100 border-stone-200 text-stone-300 cursor-not-allowed'
                                  : 'bg-brand-red-light border-brand-red-light text-brand-red hover:bg-[#fccfcf]'
                              )}
                            >
                              <X size={12} /> 취소
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

        ) : (
          /* 매수 / 매도 탭 */
          <div className="space-y-5">
            {/* 주문 유형(지정가 고정) + 입력 모드 토글 */}
            <div className="flex items-center justify-between">
              <span className="px-3 py-1 bg-stone-200 rounded-lg text-[10px] font-bold text-stone-500">
                지정가
              </span>
              <div className="flex bg-stone-200 p-1 rounded-lg">
                {[{ id: 'qty', label: '수량' }, { id: 'amount', label: '금액' }].map(m => (
                  <button
                    key={m.id}
                    onClick={() => setInputMode(m.id)}
                    className={cn(
                      'px-3 py-1 rounded-md text-[10px] font-bold transition-all',
                      inputMode === m.id ? 'bg-[#ffffff] text-stone-800 shadow-sm' : 'text-stone-400'
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
                  value={price}
                  onChange={e => setPrice(e.target.value)}
                  placeholder="가격 입력"
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
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-stone-400">계좌 비밀번호</label>
              <div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5">
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="숫자 4자리"
                  value={accountPassword}
                  onChange={e => setAccountPassword(e.target.value.replace(/\D/g, '').slice(0, 4))}
                  className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800 placeholder-stone-400"
                />
              </div>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {['10%', '25%', '50%', '최대'].map(p => (
                <button
                  key={p}
                  onClick={() => handleRatioClick(p)}
                  disabled={!isLoggedIn}
                  className={cn(
                    'py-1.5 rounded-lg text-[10px] font-bold transition-all border',
                    isLoggedIn
                      ? 'bg-stone-200 hover:bg-[#d0ccc0] border-stone-200 text-stone-400'
                      : 'bg-stone-100 border-stone-100 text-stone-300 cursor-not-allowed'
                  )}
                >
                  {p}
                </button>
              ))}
            </div>

            {/* 주문 요약 */}
            <div className="pt-3 border-t border-stone-200 space-y-2">
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">{isBuy ? '매수가능 금액' : '매도가능 수량'}</span>
                <span className="text-stone-800">
                  {isBuy
                    ? `${capacity.availableBalance.toLocaleString()}원`
                    : `${capacity.availableQuantity.toLocaleString()}주`}
                </span>
              </div>
              <div className="flex justify-between text-[10px] font-bold">
                <span className="text-stone-400">총 주문 금액</span>
                <span className="text-stone-800">{numAmount.toLocaleString()}원</span>
              </div>
              <div className="flex items-center gap-1.5 text-[10px] text-[#a07828] font-bold bg-[#fef6dc] px-3 py-2 rounded-lg">
                <MoreHorizontal size={12} />
                지정가 주문은 체결 전까지 대기 상태로 유지됩니다
              </div>
            </div>

            {/* 주문 버튼 */}
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className={cn(
                'w-full py-3.5 text-white rounded-md font-black text-sm transition-colors disabled:opacity-50',
                isBuy
                  ? 'bg-brand-red hover:bg-[#c92a2a]'
                  : 'bg-brand-blue hover:bg-[#1971c2]'
              )}
            >
              {submitting ? '처리 중...' : (isBuy ? '매수하기' : '매도하기')}
            </button>

            {/* 주문 결과 메시지 */}
            {orderMsg && (
              <p className={cn(
                'text-center text-[10px] font-bold',
                orderMsg.type === 'success' ? 'text-green-600' : 'text-red-500'
              )}>
                {orderMsg.text}
              </p>
            )}

            {!isLoggedIn && (
              <p className="text-center text-[10px] text-stone-400 font-bold">
                주문하려면 로그인이 필요합니다
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
