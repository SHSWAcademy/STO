import { useState, useEffect, useCallback } from 'react';
import { MoreHorizontal, Edit3, X } from 'lucide-react';
import { cn } from '../../lib/utils.js';
import api from '../../lib/api.js';

function OrderPinPadModal({ title, description, password, errorMessage, submitting, onChange, onClose, onConfirm }) {
  const masked = `${'●'.repeat(password.length)}${'○'.repeat(Math.max(4 - password.length, 0))}`;
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'reset', '0', 'delete'];

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50" onClick={onClose}>
      <div className="w-[360px] rounded-2xl border border-stone-200 bg-white p-6 shadow-xl" onClick={e => e.stopPropagation()}>
        <div className="space-y-2">
          <h3 className="text-base font-black text-stone-800">{title}</h3>
          <p className="text-sm font-medium text-stone-500">{description}</p>
        </div>
        <div className="mt-5 rounded-2xl border border-stone-200 bg-stone-100 px-4 py-5">
          <div className="text-center font-mono text-2xl font-black tracking-[0.35em] text-stone-800">{masked}</div>
        </div>
        {errorMessage && <p className="mt-3 text-center text-[11px] font-bold text-brand-red">{errorMessage}</p>}
        <div className="mt-5 grid grid-cols-3 gap-2">
          {keys.map(key => (
            <button
              key={key}
              type="button"
              onClick={() => {
                if (key === 'reset') return onChange('');
                if (key === 'delete') return onChange(password.slice(0, -1));
                if (password.length >= 4) return;
                onChange(`${password}${key}`);
              }}
              className={cn(
                'rounded-xl border py-3 text-sm font-black transition-colors',
                key === 'reset' || key === 'delete'
                  ? 'border-stone-200 bg-stone-100 text-stone-500 hover:bg-stone-200'
                  : 'border-stone-200 bg-white text-stone-800 hover:bg-stone-100'
              )}
            >
              {key === 'reset' ? '초기화' : key === 'delete' ? '지우기' : key}
            </button>
          ))}
        </div>
        <div className="mt-4 flex gap-2">
          <button onClick={onClose} disabled={submitting} className="flex-1 rounded-xl border border-stone-200 bg-white py-3 text-sm font-black text-stone-500 hover:bg-stone-100 disabled:opacity-50">취소</button>
          <button onClick={onConfirm} disabled={submitting || password.length !== 4} className="flex-1 rounded-xl bg-stone-800 py-3 text-sm font-black text-white hover:bg-stone-700 disabled:opacity-50">
            {submitting ? '처리 중..' : '확인'}
          </button>
        </div>
      </div>
    </div>
  );
}

function OrderExecutionConfirmModal({ title, items, submitting, onClose, onConfirm }) {
  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/55" onClick={onClose}>
      <div className="w-[360px] rounded-2xl border border-stone-200 bg-white p-6 shadow-xl" onClick={e => e.stopPropagation()}>
        <div className="space-y-1">
          <h3 className="text-base font-black text-stone-800">{title}</h3>
          <p className="text-sm text-stone-500">주문 내역을 확인한 뒤 최종 진행해 주세요.</p>
        </div>
        <div className="mt-5 space-y-2 rounded-2xl border border-stone-200 bg-stone-50 p-4">
          {items.map(item => (
            <div key={item.label} className="flex items-center justify-between text-sm">
              <span className="font-bold text-stone-400">{item.label}</span>
              <span className="font-mono font-black text-stone-800">{item.value}</span>
            </div>
          ))}
        </div>
        <div className="mt-6 flex gap-2">
          <button onClick={onClose} disabled={submitting} className="flex-1 rounded-xl border border-stone-200 bg-white py-3 text-sm font-black text-stone-500 hover:bg-stone-100 disabled:opacity-50">취소</button>
          <button onClick={onConfirm} disabled={submitting} className="flex-1 rounded-xl bg-stone-800 py-3 text-sm font-black text-white hover:bg-stone-700 disabled:opacity-50">
            {submitting ? '처리 중..' : '최종 확인'}
          </button>
        </div>
      </div>
    </div>
  );
}

export function SecureOrderPanel({ currentPrice, selectedPrice, tokenId, token, wsPendingData, onLoginRequired }) {
  const [orderSide, setOrderSide] = useState('buy');
  const [inputMode, setInputMode] = useState('qty');
  const [price, setPrice] = useState(currentPrice > 0 ? String(currentPrice) : '');
  const [qty, setQty] = useState('');
  const [amount, setAmount] = useState('');
  const [accountPassword, setAccountPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [orderMsg, setOrderMsg] = useState(null);
  const [pendingOrders, setPendingOrders] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [editingOrderId, setEditingOrderId] = useState(null);
  const [editPrice, setEditPrice] = useState('');
  const [editQty, setEditQty] = useState('');
  const [editAccountPassword, setEditAccountPassword] = useState('');
  const [updateMsg, setUpdateMsg] = useState(null);
  const [capacity, setCapacity] = useState({ availableBalance: 0, availableQuantity: 0 });
  const [passwordModal, setPasswordModal] = useState(null);
  const [confirmModal, setConfirmModal] = useState(null);
  const isBuy = orderSide === 'buy';
  const isPendingTab = orderSide === 'pending';
  const isLoggedIn = !!token;

  useEffect(() => {
    if (currentPrice > 0) setPrice(prev => (prev === '' ? String(currentPrice) : prev));
  }, [currentPrice]);

  useEffect(() => {
    if (!selectedPrice || isPendingTab) return;
    setPrice(String(selectedPrice));
  }, [selectedPrice, isPendingTab]);

  useEffect(() => {
    if (!wsPendingData) return;
    if (editingOrderId === null) {
      setPendingOrders(wsPendingData);
      return;
    }
    const editingOrderExists = wsPendingData.some(order => order.orderId === editingOrderId);
    if (!editingOrderExists) {
      setPendingOrders(wsPendingData);
      setEditingOrderId(null);
      setEditPrice('');
      setEditQty('');
      setUpdateMsg({ orderId: editingOrderId, type: 'error', text: '편집 중이던 주문이 체결되었거나 취소되어 편집이 종료되었습니다.' });
      return;
    }
    setPendingOrders(prev => wsPendingData.map(incoming => (
      incoming.orderId === editingOrderId ? (prev.find(order => order.orderId === editingOrderId) ?? incoming) : incoming
    )));
  }, [wsPendingData, editingOrderId]);

  const numPrice = Number(price) || 0;
  const numQty = inputMode === 'qty' ? (Number(qty) || 0) : (numPrice > 0 ? Math.floor((Number(amount) || 0) / numPrice) : 0);
  const numAmount = inputMode === 'amount' ? (Number(amount) || 0) : numPrice * numQty;

  const fetchPendingOrders = useCallback(async () => {
    if (!isLoggedIn || !tokenId) return;
    setPendingLoading(true);
    try {
      const res = await api.get(`/api/token/${tokenId}/order/pending`);
      setPendingOrders(res.data);
    } catch (error) {
      console.warn('[SecureOrderPanel] pending order fetch failed:', error.message);
    } finally {
      setPendingLoading(false);
    }
  }, [isLoggedIn, tokenId]);

  useEffect(() => {
    if (orderSide === 'pending') fetchPendingOrders();
  }, [orderSide, fetchPendingOrders]);

  const fetchCapacity = useCallback(async () => {
    if (!isLoggedIn || !tokenId) return;
    try {
      const res = await api.get(`/api/token/${tokenId}/order/capacity`);
      setCapacity({
        availableBalance: res.data.availableBalance ?? 0,
        availableQuantity: res.data.availableQuantity ?? 0,
      });
    } catch (error) {
      console.warn('[SecureOrderPanel] capacity fetch failed:', error.message);
    }
  }, [isLoggedIn, tokenId]);

  useEffect(() => {
    if (orderSide === 'buy' || orderSide === 'sell') fetchCapacity();
  }, [orderSide, fetchCapacity]);

  function isValidAccountPassword(value) {
    return /^\d{4}$/.test(value);
  }

  function getApiErrorMessage(error, fallback) {
    const errorCode = error?.response?.data?.errorCode;
    if (errorCode === 'MATCH_SERVICE_UNAVAILABLE') {
      return '주문 서버와의 연결이 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요.';
    }
    return error?.response?.data?.errorMessage || error?.response?.data?.message || error?.message || fallback;
  }

  function getTickSize(orderPrice) {
    if (orderPrice < 100) return 10;
    if (orderPrice < 1000) return 50;
    if (orderPrice < 10000) return 100;
    return 500;
  }

  function showLoginRequired(message) {
    if (onLoginRequired) onLoginRequired(message);
    else setOrderMsg({ type: 'error', text: message });
  }

  function openPasswordModal(action, orderId = null) {
    setPasswordModal({ action, orderId });
    setConfirmModal(null);
    if (action === 'create') {
      setAccountPassword('');
      setOrderMsg(null);
    } else {
      setEditAccountPassword('');
      setUpdateMsg(null);
    }
  }

  function closePasswordModal() {
    setPasswordModal(null);
    setAccountPassword('');
    setEditAccountPassword('');
  }

  function resetOrderAuthFlow() {
    setPasswordModal(null);
    setConfirmModal(null);
    setAccountPassword('');
    setEditAccountPassword('');
  }

  async function verifyPasswordBeforeConfirm() {
    const password = passwordModal?.action === 'create' ? accountPassword : editAccountPassword;
    try {
      await api.post('/api/myaccount/verify-password', { accountPassword: password });
      return { ok: true };
    } catch (error) {
      return { ok: false, message: getApiErrorMessage(error, '계좌 비밀번호를 다시 확인해 주세요.') };
    }
  }

  function handleTabClick(side) {
    if (!isLoggedIn && side === 'pending') {
      showLoginRequired('로그인 후 미체결 주문을 확인할 수 있습니다.');
      return;
    }
    setOrderMsg(null);
    setOrderSide(side);
  }

  async function handleSubmit() {
    if (!isLoggedIn) {
      showLoginRequired('매수/매도 주문을 하려면 먼저 로그인해 주세요.');
      return;
    }
    if (!Number.isInteger(numPrice) || !Number.isInteger(numQty) || numPrice <= 0 || numQty <= 0) {
      setOrderMsg({ type: 'error', text: '가격과 수량을 올바르게 입력해 주세요.' });
      return;
    }
    const tick = getTickSize(numPrice);
    if (numPrice % tick !== 0) {
      setOrderMsg({ type: 'error', text: `호가 단위를 확인해 주세요. 현재 가격의 호가 단위는 ${tick.toLocaleString()}원입니다.` });
      return;
    }
    openPasswordModal('create');
  }

  async function submitCreateOrder() {
    if (!isValidAccountPassword(accountPassword)) {
      setOrderMsg({ type: 'error', text: '계좌 비밀번호 4자리를 입력해 주세요.' });
      return;
    }
    setSubmitting(true);
    setOrderMsg(null);
    try {
      await api.post(`/api/token/${tokenId}/order`, {
        orderPrice: numPrice,
        orderQuantity: numQty,
        orderType: isBuy ? 'BUY' : 'SELL',
        accountPassword,
      });
      setOrderMsg({ type: 'success', text: `${isBuy ? '매수' : '매도'} 주문이 정상적으로 접수되었습니다.` });
      setQty('');
      setAmount('');
      resetOrderAuthFlow();
      fetchCapacity();
    } catch (error) {
      setOrderMsg({ type: 'error', text: getApiErrorMessage(error, '주문 접수 중 오류가 발생했습니다.') });
    } finally {
      setSubmitting(false);
    }
  }

  function handleEditStart(order) {
    setEditingOrderId(order.orderId);
    setEditPrice(String(order.orderPrice ?? ''));
    setEditQty(String(order.orderQuantity ?? ''));
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

  function handleCancelOrder(orderId) {
    openPasswordModal('cancel', orderId);
  }

  async function submitCancelOrder(orderId) {
    setSubmitting(true);
    try {
      await api.delete(`/api/token/order/cancel/${orderId}`, { data: { accountPassword: editAccountPassword } });
      setPendingOrders(prev => prev.filter(order => order.orderId !== orderId));
      resetOrderAuthFlow();
    } catch (error) {
      setUpdateMsg({ orderId, type: 'error', text: getApiErrorMessage(error, '주문 취소에 실패했습니다.') });
    } finally {
      setSubmitting(false);
    }
  }

  async function submitUpdateOrder(orderId) {
    const updatePrice = Number(editPrice);
    const updateQuantity = Number(editQty);
    if (!Number.isInteger(updatePrice) || !Number.isInteger(updateQuantity) || updatePrice <= 0 || updateQuantity <= 0) {
      setUpdateMsg({ orderId, type: 'error', text: '가격과 수량을 올바르게 입력해 주세요.' });
      return;
    }
    const tick = getTickSize(updatePrice);
    if (updatePrice % tick !== 0) {
      setUpdateMsg({ orderId, type: 'error', text: `호가 단위를 확인해 주세요. 현재 가격의 호가 단위는 ${tick.toLocaleString()}원입니다.` });
      return;
    }
    if (!isValidAccountPassword(editAccountPassword)) {
      setUpdateMsg({ orderId, type: 'error', text: '계좌 비밀번호 4자리를 입력해 주세요.' });
      return;
    }
    setSubmitting(true);
    try {
      await api.put(`/api/token/order/update/${orderId}`, {
        updatePrice,
        updateQuantity,
        accountPassword: editAccountPassword,
      });
      setPendingOrders(prev => prev.map(order => {
        if (order.orderId !== orderId) return order;
        const filledQuantity = Number(order.filledQuantity) || 0;
        const remainingQuantity = Math.max(updateQuantity - filledQuantity, 0);
        return { ...order, orderPrice: updatePrice, orderQuantity: updateQuantity, remainingQuantity };
      }));
      setEditingOrderId(null);
      setEditPrice('');
      setEditQty('');
      resetOrderAuthFlow();
      setUpdateMsg(null);
    } catch (error) {
      setUpdateMsg({ orderId, type: 'error', text: getApiErrorMessage(error, '주문 수정에 실패했습니다.') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePasswordConfirm() {
    if (!passwordModal) return;
    const result = await verifyPasswordBeforeConfirm();
    if (!result.ok) {
      if (passwordModal.action === 'create') {
        setAccountPassword('');
        setOrderMsg({ type: 'error', text: result.message });
      } else {
        setEditAccountPassword('');
        setUpdateMsg({ orderId: passwordModal.orderId, type: 'error', text: result.message });
      }
      return;
    }
    setPasswordModal(null);
    setConfirmModal(passwordModal);
  }

  function getConfirmModalSpec() {
    if (!confirmModal) return null;
    if (confirmModal.action === 'create') {
      return {
        title: `${isBuy ? '매수' : '매도'} 주문 최종 확인`,
        items: [
          { label: '구분', value: isBuy ? '매수' : '매도' },
          { label: '주문가', value: `${numPrice.toLocaleString()}원` },
          { label: '주문수량', value: `${numQty.toLocaleString()}주` },
          { label: '총 주문금액', value: `${numAmount.toLocaleString()}원` },
        ],
      };
    }
    const order = pendingOrders.find(item => item.orderId === confirmModal.orderId);
    const updateTotalAmount = (Number(editPrice) || 0) * (Number(editQty) || 0);
    if (confirmModal.action === 'update') {
      return {
        title: '주문 수정 최종 확인',
        items: [
          { label: '주문번호', value: String(confirmModal.orderId ?? '-') },
          { label: '구분', value: order?.orderType === 'BUY' ? '매수' : '매도' },
          { label: '수정가', value: `${(Number(editPrice) || 0).toLocaleString()}원` },
          { label: '수정수량', value: `${(Number(editQty) || 0).toLocaleString()}주` },
          { label: '주문금액', value: `${updateTotalAmount.toLocaleString()}원` },
        ],
      };
    }
    return {
      title: '주문 취소 최종 확인',
      items: [
        { label: '주문번호', value: String(confirmModal.orderId ?? '-') },
        { label: '구분', value: order?.orderType === 'BUY' ? '매수' : '매도' },
        { label: '주문가', value: `${Number(order?.orderPrice || 0).toLocaleString()}원` },
        { label: '잔여수량', value: `${Number(order?.remainingQuantity || 0).toLocaleString()}주` },
      ],
    };
  }

  async function handleConfirmExecution() {
    if (submitting || !confirmModal) return;
    if (confirmModal.action === 'create') return submitCreateOrder();
    if (confirmModal.action === 'update') return submitUpdateOrder(confirmModal.orderId);
    return submitCancelOrder(confirmModal.orderId);
  }

  const ratioMap = { '10%': 0.1, '25%': 0.25, '50%': 0.5, '최대': 1.0 };

  function handleRatioClick(label) {
    const ratio = ratioMap[label];
    const nextQty = isBuy ? (numPrice > 0 ? Math.floor(capacity.availableBalance * ratio / numPrice) : 0) : Math.floor(capacity.availableQuantity * ratio);
    setQty(String(nextQty));
    setInputMode('qty');
  }

  const confirmSpec = getConfirmModalSpec();

  return (
    <div className="w-[360px] bg-white rounded-lg border border-stone-200 flex flex-col overflow-hidden">
      <div className="flex border-b border-stone-200">
        {[
          { id: 'buy', label: '매수', active: 'text-brand-red border-b-2 border-brand-red bg-brand-red-light/40' },
          { id: 'sell', label: '매도', active: 'text-brand-blue border-b-2 border-brand-blue bg-brand-blue-light/60' },
          { id: 'pending', label: '대기', active: 'text-stone-800 border-b-2 border-stone-800 bg-stone-100/60' },
        ].map(tab => (
          <button key={tab.id} onClick={() => handleTabClick(tab.id)} className={cn('flex-1 py-4 text-sm font-black transition-all', orderSide === tab.id ? tab.active : 'text-stone-400 hover:text-stone-500')}>
            {tab.label}
          </button>
        ))}
      </div>
      <div className="flex-1 overflow-y-auto p-5">
        {isPendingTab ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-stone-400">미체결 주문</span>
              <button onClick={fetchPendingOrders} disabled={pendingLoading} className="text-[10px] font-bold text-stone-400 hover:text-stone-700 transition-colors">{pendingLoading ? '조회 중..' : '새로고침'}</button>
            </div>
            {pendingLoading && pendingOrders.length === 0 ? (
              <div className="py-16 flex flex-col items-center gap-3 text-center"><div className="w-12 h-12 bg-stone-200 rounded-lg flex items-center justify-center"><MoreHorizontal size={24} className="text-stone-400 animate-pulse" /></div><p className="text-sm font-bold text-stone-400">조회 중..</p></div>
            ) : pendingOrders.length === 0 ? (
              <div className="py-16 flex flex-col items-center gap-3 text-center"><div className="w-12 h-12 bg-stone-200 rounded-lg flex items-center justify-center"><MoreHorizontal size={24} className="text-stone-400" /></div><p className="text-sm font-bold text-stone-400">대기 중인 주문이 없습니다</p></div>
            ) : (
              <div className="space-y-3">
                {pendingOrders.map(order => {
                  const orderIsBuy = order.orderType === 'BUY';
                  const isProcessing = order.orderStatus === 'PENDING';
                  const isEditing = editingOrderId === order.orderId;
                  const totalAmount = isEditing ? (Number(editPrice) || 0) * (Number(editQty) || 0) : (order.orderPrice ?? 0) * (order.orderQuantity ?? 0);
                  return (
                    <div key={order.orderId} className="p-4 bg-stone-100 rounded-lg border border-stone-200 space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className={cn('text-[10px] font-black px-2 py-0.5 rounded-md', orderIsBuy ? 'bg-brand-red-light text-brand-red' : 'bg-brand-blue-light text-brand-blue')}>{orderIsBuy ? '매수' : '매도'}</span>
                          <span className={cn('text-[10px] font-black px-2 py-0.5 rounded-md', isProcessing ? 'bg-stone-200 text-stone-400' : isEditing ? 'bg-blue-100 text-blue-600' : 'bg-[#fef6dc] text-[#a07828]')}>{isProcessing ? '처리중' : isEditing ? '수정중' : '대기'}</span>
                        </div>
                        <span className="text-[9px] text-stone-400 font-bold">{order.createdAt ? new Date(order.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '-'}</span>
                      </div>
                      {isEditing ? (
                        <div className="space-y-2">
                          <div className="space-y-1"><label className="text-[10px] font-bold text-stone-400">수정 가격</label><div className="flex items-center gap-2 bg-white border border-stone-300 rounded-md px-3 py-2"><input type="number" min="1" step="1" value={editPrice} onChange={e => setEditPrice(e.target.value)} className="flex-1 bg-transparent text-[11px] font-mono font-bold outline-none text-right text-stone-800" /><span className="text-[11px] font-bold text-stone-400">원</span></div>{Number(editPrice) > 0 && Number(editPrice) % getTickSize(Number(editPrice)) !== 0 && <p className="text-[10px] font-bold text-amber-600">호가 단위({getTickSize(Number(editPrice)).toLocaleString()}원)에 맞지 않습니다.</p>}</div>
                          <div className="space-y-1"><label className="text-[10px] font-bold text-stone-400">수정 수량</label><div className="flex items-center gap-2 bg-white border border-stone-300 rounded-md px-3 py-2"><input type="number" min="1" step="1" value={editQty} onChange={e => setEditQty(e.target.value)} className="flex-1 bg-transparent text-[11px] font-mono font-bold outline-none text-right text-stone-800" /><span className="text-[11px] font-bold text-stone-400">주</span></div></div>
                          <div className="flex justify-between text-[11px] font-bold border-t border-stone-200 pt-1.5"><span className="text-stone-400">주문금액</span><span className="font-mono font-black text-stone-800">{totalAmount.toLocaleString()}원</span></div>
                          {updateMsg?.orderId === order.orderId && <p className={cn('text-[10px] font-bold text-center', updateMsg.type === 'error' ? 'text-brand-red' : 'text-green-600')}>{updateMsg.text}</p>}
                          <div className="flex gap-2 pt-1"><button onClick={() => openPasswordModal('update', order.orderId)} className="flex-1 py-2 bg-stone-800 border border-stone-800 rounded-md text-[11px] font-black text-white hover:bg-stone-700 transition-all">확인</button><button onClick={handleEditCancel} className="flex-1 py-2 bg-white border border-stone-200 rounded-md text-[11px] font-black text-stone-500 hover:bg-stone-200 transition-all">취소</button></div>
                        </div>
                      ) : (
                        <><div className="space-y-1.5 text-[11px] font-bold"><div className="flex justify-between"><span className="text-stone-400">지정가격</span><span className="font-mono text-stone-800">{order.orderPrice?.toLocaleString()}원</span></div><div className="flex justify-between"><span className="text-stone-400">미체결/전체</span><span className="font-mono text-stone-800">{order.remainingQuantity} / {order.orderQuantity}주</span></div><div className="flex justify-between border-t border-stone-200 pt-1.5"><span className="text-stone-400">주문금액</span><span className="font-mono font-black text-stone-800">{totalAmount.toLocaleString()}원</span></div></div><div className="flex gap-2 pt-1"><button disabled={isProcessing} onClick={() => !isProcessing && handleEditStart(order)} className={cn('flex-1 py-2 border rounded-md text-[11px] font-black transition-all flex items-center justify-center gap-1', isProcessing ? 'bg-stone-100 border-stone-200 text-stone-300 cursor-not-allowed' : 'bg-white border-stone-200 text-stone-500 hover:bg-stone-200')}><Edit3 size={12} /> 수정</button><button disabled={isProcessing} onClick={() => !isProcessing && handleCancelOrder(order.orderId)} className={cn('flex-1 py-2 border rounded-md text-[11px] font-black transition-all flex items-center justify-center gap-1', isProcessing ? 'bg-stone-100 border-stone-200 text-stone-300 cursor-not-allowed' : 'bg-brand-red-light border-brand-red-light text-brand-red hover:bg-[#fccfcf]')}><X size={12} /> 취소</button></div></>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-5">
            <div className="flex items-center justify-between"><span className="px-3 py-1 bg-stone-200 rounded-lg text-[10px] font-bold text-stone-500">지정가</span><div className="flex bg-stone-200 p-1 rounded-lg">{[{ id: 'qty', label: '수량' }, { id: 'amount', label: '금액' }].map(mode => <button key={mode.id} onClick={() => setInputMode(mode.id)} className={cn('px-3 py-1 rounded-md text-[10px] font-bold transition-all', inputMode === mode.id ? 'bg-white text-stone-800 shadow-sm' : 'text-stone-400')}>{mode.label}</button>)}</div></div>
            <div className="space-y-1.5"><div className="flex items-center justify-between"><label className="text-[10px] font-bold text-stone-400">{isBuy ? '매수' : '매도'} 가격</label>{numPrice > 0 && <span className="text-[10px] font-bold text-stone-400">호가 단위: {getTickSize(numPrice).toLocaleString()}원</span>}</div><div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5"><input type="text" value={price} onChange={e => setPrice(e.target.value)} placeholder="가격 입력" className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800 placeholder-stone-400" /><span className="text-sm font-bold text-stone-400">원</span></div>{numPrice > 0 && numPrice % getTickSize(numPrice) !== 0 && <p className="text-[10px] font-bold text-amber-600">호가 단위({getTickSize(numPrice).toLocaleString()}원)에 맞지 않습니다.</p>}</div>
            {inputMode === 'qty' ? <div className="space-y-1.5"><label className="text-[10px] font-bold text-stone-400">{isBuy ? '매수' : '매도'} 수량</label><div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5"><input type="text" placeholder="수량 입력" value={qty} onChange={e => setQty(e.target.value)} className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800" /><span className="text-sm font-bold text-stone-400">주</span></div></div> : <div className="space-y-1.5"><label className="text-[10px] font-bold text-stone-400">{isBuy ? '매수' : '매도'} 금액</label><div className="flex items-center gap-2 bg-stone-200 border border-stone-200 rounded-md px-4 py-2.5"><input type="text" placeholder="금액 입력" value={amount} onChange={e => setAmount(e.target.value)} className="flex-1 bg-transparent text-sm font-mono font-bold outline-none text-right pr-2 text-stone-800" /><span className="text-sm font-bold text-stone-400">원</span></div>{amount && numPrice > 0 && <p className="text-[10px] text-stone-400 font-bold text-right">약 {Math.floor(Number(amount) / numPrice)}주</p>}</div>}
            <div className="grid grid-cols-4 gap-2">{['10%', '25%', '50%', '최대'].map(label => <button key={label} onClick={() => handleRatioClick(label)} disabled={!isLoggedIn} className={cn('py-1.5 rounded-lg text-[10px] font-bold transition-all border', isLoggedIn ? 'bg-stone-200 hover:bg-stone-300 border-stone-200 text-stone-400' : 'bg-stone-100 border-stone-100 text-stone-300 cursor-not-allowed')}>{label}</button>)}</div>
            <div className="pt-3 border-t border-stone-200 space-y-2"><div className="flex justify-between text-[10px] font-bold"><span className="text-stone-400">{isBuy ? '매수 가능 금액' : '매도 가능 수량'}</span><span className="text-stone-800">{isBuy ? `${capacity.availableBalance.toLocaleString()}원` : `${capacity.availableQuantity.toLocaleString()}주`}</span></div><div className="flex justify-between text-[10px] font-bold"><span className="text-stone-400">총 주문 금액</span><span className="text-stone-800">{numAmount.toLocaleString()}원</span></div><div className="flex items-center gap-1.5 text-[10px] text-[#a07828] font-bold bg-[#fef6dc] px-3 py-2 rounded-lg"><MoreHorizontal size={12} />지정가 주문은 체결 전까지 대기 상태로 유지됩니다.</div></div>
            <button onClick={handleSubmit} disabled={submitting} className={cn('w-full py-3.5 text-white rounded-md font-black text-sm transition-colors disabled:opacity-50', isBuy ? 'bg-brand-red hover:bg-red-700' : 'bg-brand-blue hover:bg-blue-700')}>{submitting ? '처리 중..' : isBuy ? '매수하기' : '매도하기'}</button>
            {orderMsg && <p className={cn('text-center text-[10px] font-bold', orderMsg.type === 'success' ? 'text-green-600' : 'text-red-500')}>{orderMsg.text}</p>}
            {!isLoggedIn && <p className="text-center text-[10px] text-stone-400 font-bold">주문하려면 로그인이 필요합니다.</p>}
          </div>
        )}
      </div>
      {passwordModal && <OrderPinPadModal title={passwordModal.action === 'create' ? `${isBuy ? '매수' : '매도'} 주문 확인` : passwordModal.action === 'update' ? '주문 수정 확인' : '주문 취소 확인'} description={passwordModal.action === 'create' ? `${isBuy ? '매수' : '매도'} 주문 내역을 확인한 뒤 계좌 비밀번호를 입력해 주세요.` : passwordModal.action === 'update' ? '수정할 주문 내역을 확인한 뒤 계좌 비밀번호를 입력해 주세요.' : '취소할 주문 내역을 확인한 뒤 계좌 비밀번호를 입력해 주세요.'} password={passwordModal.action === 'create' ? accountPassword : editAccountPassword} errorMessage={passwordModal.action === 'create' ? (orderMsg?.type === 'error' ? orderMsg.text : null) : (updateMsg?.orderId === passwordModal.orderId && updateMsg.type === 'error' ? updateMsg.text : null)} submitting={submitting} onChange={value => { if (passwordModal.action === 'create') setAccountPassword(value); else setEditAccountPassword(value); }} onClose={closePasswordModal} onConfirm={handlePasswordConfirm} />}
      {confirmSpec && <OrderExecutionConfirmModal title={confirmSpec.title} items={confirmSpec.items} submitting={submitting} onClose={() => setConfirmModal(null)} onConfirm={handleConfirmExecution} />}
    </div>
  );
}


