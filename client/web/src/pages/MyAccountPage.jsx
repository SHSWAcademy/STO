import { useState } from 'react';
import {
  History, Wallet, ArrowUpRight, HandCoins, TrendingUp,
  Settings as SettingsIcon, Coins, AlertCircle, User, Landmark,
  ChevronDown, X
} from 'lucide-react';
import {
  MOCK_USER, PORTFOLIO_ASSETS, ACCOUNT_DIVIDENDS,
  PROFIT_ANALYSIS_DATA, OPEN_ORDERS, FILLED_ORDERS,
} from '../data/mock.js';
import { cn } from '../lib/utils.js';
import { ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const ASSET_PIE = [
  { name: '서울강남빌딩', value: 45, color: '#4a72a0' },
  { name: '송도 리조트',  value: 25, color: '#64d2ff' },
  { name: '아트프라임',   value: 15, color: '#c9a84c' },
  { name: '기타',         value: 15, color: '#636366' },
];

const SIDEBAR_ITEMS = [
  { id: 'assets',    label: '자산',       icon: Wallet },
  { id: 'history',   label: '거래내역',   icon: History },
  { id: 'orders',    label: '주문내역',   icon: ArrowUpRight },
  { id: 'dividends', label: '배당금 내역', icon: HandCoins },
  { id: 'analysis',  label: '수익분석',   icon: TrendingUp },
  { id: 'settings',  label: '계좌관리',   icon: SettingsIcon },
];

const HISTORY_ITEMS = [
  { date: '2026.03.21', title: '서울강남빌딩 매수', amount: '-₩1,240,000', balance: '₩1,240,000', type: 'buy' },
  { date: '2026.03.20', title: '계좌 입금',          amount: '+₩5,000,000', balance: '₩2,480,000', type: 'deposit' },
  { date: '2026.03.19', title: '송도 리조트 매도',    amount: '+₩422,500',  balance: '₩1,240,000', type: 'sell' },
  { date: '2026.03.18', title: '한남더힐 배당금',     amount: '+₩45,600',   balance: '₩1,240,000', type: 'dividend' },
  { date: '2026.03.17', title: '계좌 출금',            amount: '-₩1,000,000', balance: '₩817,500',  type: 'withdraw' },
];

export function MyAccountPage() {
  const [activeSubTab, setActiveSubTab]       = useState('assets');
  const [historyFilter, setHistoryFilter]     = useState('전체');
  const [orderTab, setOrderTab]               = useState('all');
  const [openOrders, setOpenOrders]           = useState(OPEN_ORDERS);
  const [isFillModalOpen, setIsFillModalOpen] = useState(false);
  const [isSendModalOpen, setIsSendModalOpen] = useState(false);
  const [amount, setAmount]                   = useState('');

  function handleFill() {
    if (!amount || isNaN(Number(amount))) return;
    alert(`${Number(amount).toLocaleString()}원이 충전되었습니다.`);
    setIsFillModalOpen(false);
    setAmount('');
  }

  function handleSend() {
    if (!amount || isNaN(Number(amount))) return;
    alert(`${Number(amount).toLocaleString()}원이 송금되었습니다.`);
    setIsSendModalOpen(false);
    setAmount('');
  }

  function handleCancelOrder(orderId) {
    if (window.confirm('정말 이 주문을 취소하시겠습니까?')) {
      setOpenOrders(prev => prev.filter(o => o.id !== orderId));
    }
  }

  return (
    <div className="flex gap-8 pb-20">
      {/* 사이드바 */}
      <aside className="w-48 shrink-0">
        <nav className="space-y-1 sticky top-24">
          {SIDEBAR_ITEMS.map(item => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => setActiveSubTab(item.id)}
                className={cn(
                  'w-full flex items-center gap-3 px-4 py-3 rounded-md text-sm font-medium transition-colors text-left',
                  activeSubTab === item.id
                    ? 'bg-stone-surface text-stone-text-primary border border-stone-border'
                    : 'text-stone-text-secondary hover:bg-stone-surface hover:text-stone-text-primary'
                )}
              >
                <Icon size={16} />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      {/* 콘텐츠 */}
      <div className="flex-1 min-w-0">
        {activeSubTab === 'assets'    && <AssetsTab    onFill={() => setIsFillModalOpen(true)} onSend={() => setIsSendModalOpen(true)} />}
        {activeSubTab === 'history'   && <HistoryTab   filter={historyFilter} onFilter={setHistoryFilter} items={HISTORY_ITEMS} />}
        {activeSubTab === 'orders'    && <OrdersTab    orderTab={orderTab} onOrderTab={setOrderTab} openOrders={openOrders} onCancel={handleCancelOrder} />}
        {activeSubTab === 'dividends' && <DividendsTab />}
        {activeSubTab === 'analysis'  && <AnalysisTab />}
        {activeSubTab === 'settings'  && <SettingsTab />}
      </div>

      {/* 충전 모달 */}
      {isFillModalOpen && (
        <SimpleModal title="충전" onClose={() => { setIsFillModalOpen(false); setAmount(''); }}>
          <input
            type="number"
            placeholder="금액 입력"
            value={amount}
            onChange={e => setAmount(e.target.value)}
            className="w-full bg-stone-bg border border-stone-border rounded-xl px-4 py-3 text-stone-text-primary outline-none focus:border-stone-gold text-sm font-bold"
          />
          <button onClick={handleFill} className="w-full py-3 bg-stone-gold text-[#1c1c1e] rounded-xl font-black hover:bg-stone-gold-light transition-all">
            충전하기
          </button>
        </SimpleModal>
      )}

      {/* 송금 모달 */}
      {isSendModalOpen && (
        <SimpleModal title="송금" onClose={() => { setIsSendModalOpen(false); setAmount(''); }}>
          <input
            type="number"
            placeholder="금액 입력"
            value={amount}
            onChange={e => setAmount(e.target.value)}
            className="w-full bg-stone-bg border border-stone-border rounded-xl px-4 py-3 text-stone-text-primary outline-none focus:border-stone-gold text-sm font-bold"
          />
          <button onClick={handleSend} className="w-full py-3 bg-stone-surface border border-stone-border text-stone-text-secondary rounded-xl font-black hover:bg-stone-elevated transition-all">
            송금하기
          </button>
        </SimpleModal>
      )}
    </div>
  );
}

// ── 모달 공통 래퍼 ────────────────────────────────────────────
function SimpleModal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-stone-surface rounded-2xl border border-stone-border p-8 w-full max-w-sm shadow-2xl space-y-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-black text-stone-text-primary">{title}</h3>
          <button onClick={onClose} className="text-stone-muted hover:text-stone-text-primary transition-colors">
            <X size={20} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

// ── 자산 탭 ────────────────────────────────────────────────────
function AssetsTab({ onFill, onSend }) {
  return (
    <div className="max-w-4xl space-y-12 pb-20">
      <div className="flex flex-col md:flex-row justify-between gap-8">
        <div className="space-y-4 flex-1">
          <p className="text-stone-muted text-sm font-medium">계좌번호 123-456-789012</p>
          <h2 className="text-4xl font-black text-stone-text-primary tracking-tight">총 자산 25,390,000원</h2>
          <div className="flex gap-3 pt-2">
            <button onClick={onFill} className="px-8 py-2.5 rounded-full bg-stone-gold text-[#1c1c1e] text-sm font-bold hover:bg-stone-gold-light transition-all shadow-lg shadow-stone-gold/10">
              채우기
            </button>
            <button onClick={onSend} className="px-8 py-2.5 rounded-full bg-stone-surface text-stone-text-secondary text-sm font-bold hover:bg-stone-elevated transition-all">
              보내기
            </button>
          </div>
        </div>

        <div className="bg-stone-elevated rounded-2xl p-6 border border-stone-border w-full md:w-80 shadow-sm">
          <h3 className="text-xs font-bold text-stone-text-secondary mb-4 uppercase tracking-wider">보유 토큰 비중</h3>
          <div className="flex items-center gap-6">
            <div className="w-24 h-24">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={ASSET_PIE} innerRadius={30} outerRadius={45} paddingAngle={4} dataKey="value">
                    {ASSET_PIE.map((entry, i) => (
                      <Cell key={i} fill={entry.color} stroke="none" />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex-1 space-y-1.5">
              {ASSET_PIE.slice(0, 3).map((item, i) => (
                <div key={i} className="flex items-center justify-between text-[10px] font-bold">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full" style={{ backgroundColor: item.color }} />
                    <span className="text-stone-text-secondary">{item.name}</span>
                  </div>
                  <span className="text-stone-text-primary">{item.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="pt-8 border-t border-stone-surface">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-stone-text-primary">총 주문 가능 금액</h3>
          <span className="text-lg font-bold text-stone-text-primary">1,240,000원</span>
        </div>
        <div className="flex justify-between items-center py-4 border-t border-stone-elevated">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-stone-elevated rounded-lg flex items-center justify-center">
              <Coins size={16} className="text-stone-muted" />
            </div>
            <span className="text-sm font-medium text-stone-text-secondary">원화</span>
          </div>
          <span className="text-sm font-bold text-stone-text-primary">1,240,000원</span>
        </div>
      </div>

      <div className="pt-8 border-t border-stone-surface">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h3 className="text-lg font-bold text-stone-text-primary mb-1">내 투자</h3>
            <p className="text-xs text-stone-muted">손익 등록 제외</p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-black text-stone-text-primary">총 평가금액 24,150,000원</p>
            <p className="text-sm font-bold text-stone-gold">+1,240,000원 (+5.42%)</p>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-y-8">
          {PORTFOLIO_ASSETS.map((a, i) => {
            const evalAmt  = a.qty * a.currentPrice;
            const profit   = (a.currentPrice - a.avgPrice) * a.qty;
            const profitPct = (((a.currentPrice - a.avgPrice) / a.avgPrice) * 100).toFixed(2);
            const isUp = a.currentPrice >= a.avgPrice;
            return (
              <div key={i} className="flex items-center justify-between group cursor-pointer">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-stone-elevated border border-stone-border flex items-center justify-center text-sm font-black text-stone-muted">
                    {a.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-stone-text-primary">{a.name}</p>
                    <p className="text-xs text-stone-muted font-medium">{a.qty}주</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-stone-text-primary">{evalAmt.toLocaleString()}원</p>
                  <p className={cn('text-xs font-bold', isUp ? 'text-stone-buy' : 'text-stone-gold')}>
                    {isUp ? '+' : ''}{profit.toLocaleString()}원 ({isUp ? '+' : ''}{profitPct}%)
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        <div className="pt-8 border-t border-stone-surface space-y-4 mt-12">
          {[
            { label: '3월 수익',  value: '0원' },
            { label: '판매수익', value: '0원' },
            { label: '배당금',   value: '0원' },
          ].map((item, i) => (
            <div key={i} className="flex justify-between items-center">
              <span className="text-sm font-medium text-stone-text-secondary">{item.label}</span>
              <span className="text-sm font-bold text-stone-text-primary">{item.value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 거래내역 탭 ───────────────────────────────────────────────
function HistoryTab({ filter, onFilter, items }) {
  const filtered = items.filter(item => {
    if (filter === '전체')   return true;
    if (filter === '입출금') return item.type === 'deposit' || item.type === 'withdraw';
    if (filter === '매수')   return item.type === 'buy';
    if (filter === '매도')   return item.type === 'sell';
    return true;
  });

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">거래 내역</h2>
        <div className="flex gap-2">
          {['전체', '입출금', '매수', '매도'].map(f => (
            <button
              key={f}
              onClick={() => onFilter(f)}
              className={cn(
                'px-4 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all',
                filter === f
                  ? 'bg-stone-text-primary text-stone-bg shadow-lg'
                  : 'bg-stone-surface text-stone-text-secondary hover:bg-stone-elevated'
              )}
            >
              {f}
            </button>
          ))}
        </div>
      </div>
      <div className="bg-stone-surface border border-stone-border rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-elevated">
          {filtered.length > 0 ? (
            filtered.map((item, i) => (
              <div key={i} className="p-6 flex items-center justify-between hover:bg-stone-bg transition-colors">
                <div className="flex items-center gap-6">
                  <span className="text-[10px] font-black text-stone-muted font-mono">{item.date}</span>
                  <div>
                    <p className="text-sm font-bold text-stone-text-primary">{item.title}</p>
                    <p className="text-[10px] text-stone-muted font-bold uppercase tracking-widest mt-0.5">잔고 {item.balance}</p>
                  </div>
                </div>
                <p className={cn('text-sm font-black', item.amount.startsWith('+') ? 'text-stone-gold' : 'text-stone-buy')}>
                  {item.amount}
                </p>
              </div>
            ))
          ) : (
            <div className="p-20 flex flex-col items-center justify-center text-center space-y-4">
              <AlertCircle size={40} className="text-stone-border" />
              <p className="text-stone-muted font-bold">거래 내역이 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── 주문내역 탭 ───────────────────────────────────────────────
function OrdersTab({ orderTab, onOrderTab, openOrders, onCancel }) {
  const allOrders = [
    ...FILLED_ORDERS.map(o => ({ ...o, isFilled: true })),
    ...openOrders.map(o => ({ ...o, isFilled: false })),
  ];
  const displayOrders =
    orderTab === 'filled' ? FILLED_ORDERS.map(o => ({ ...o, isFilled: true })) :
    orderTab === 'open'   ? openOrders.map(o => ({ ...o, isFilled: false })) :
    allOrders;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">주문 내역</h2>
        <div className="flex bg-stone-surface p-1 rounded-xl">
          {[
            { id: 'all',    label: '전체' },
            { id: 'filled', label: '체결' },
            { id: 'open',   label: '미체결' },
          ].map(t => (
            <button
              key={t.id}
              onClick={() => onOrderTab(t.id)}
              className={cn(
                'px-6 py-1.5 rounded-lg text-xs font-bold transition-all',
                orderTab === t.id
                  ? 'bg-stone-elevated text-stone-text-primary shadow-sm'
                  : 'text-stone-muted hover:text-stone-text-secondary'
              )}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-stone-surface border border-stone-border rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-elevated">
          {displayOrders.length > 0 ? (
            displayOrders.map(order => (
              <div key={order.id} className="p-6 flex items-center justify-between hover:bg-stone-bg transition-colors">
                <div className="flex items-center gap-6">
                  <div className="flex flex-col w-16">
                    <span className="text-[10px] font-black text-stone-muted font-mono">{order.time}</span>
                    <span className="text-xs font-bold text-stone-text-primary mt-1">{order.symbol}</span>
                  </div>
                  <div className="w-10 h-10 rounded-lg bg-stone-elevated border border-stone-border flex items-center justify-center text-[10px] font-black text-stone-muted shrink-0">
                    {order.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className={cn('text-sm font-black', order.type.includes('매수') ? 'text-stone-buy' : 'text-stone-gold')}>
                        {order.type}
                      </p>
                      <span className={cn(
                        'text-[9px] font-black px-1.5 py-0.5 rounded uppercase tracking-widest',
                        order.isFilled ? 'bg-stone-buy-bg text-stone-buy' : 'bg-stone-buy-bg text-stone-gold'
                      )}>
                        {order.isFilled ? '체결' : '미체결'}
                      </span>
                    </div>
                    <p className="text-[10px] text-stone-muted font-bold mt-0.5">
                      {order.price.toLocaleString()}원 | {order.qty}주
                      {order.isFilled ? ` | 수수료 ${order.fee?.toLocaleString()}원` : ` (잔량 ${order.remaining}주)`}
                    </p>
                  </div>
                </div>
                <div className="text-right flex items-center gap-4">
                  {order.isFilled ? (
                    <div>
                      <p className="text-sm font-black text-stone-text-primary">{order.amount?.toLocaleString()}원</p>
                      <span className="text-[10px] font-black text-stone-buy uppercase tracking-widest">체결완료</span>
                    </div>
                  ) : (
                    <button
                      onClick={() => onCancel(order.id)}
                      className="px-4 py-2 rounded-xl bg-stone-surface text-stone-buy text-xs font-black hover:bg-stone-buy-bg transition-all border border-stone-buy-bg"
                    >
                      취소
                    </button>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="p-20 flex flex-col items-center justify-center text-center space-y-4">
              <AlertCircle size={40} className="text-stone-border" />
              <p className="text-stone-muted font-bold">주문 내역이 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── 배당금 탭 ─────────────────────────────────────────────────
function DividendsTab() {
  const totalDividends = ACCOUNT_DIVIDENDS.reduce((acc, d) => acc + d.net, 0);

  return (
    <div className="space-y-8 pb-20">
      <div className="flex items-center gap-3">
        <div className="w-4 h-4 rounded-full bg-stone-text-primary shadow-sm" />
        <h2 className="text-lg font-bold text-stone-text-primary">2026년</h2>
      </div>

      <div className="bg-stone-elevated rounded-2xl p-8 flex justify-between items-center border border-stone-border">
        <span className="text-stone-text-secondary font-medium">받은 배당금</span>
        <span className="text-stone-text-primary font-black text-2xl">{totalDividends.toLocaleString()}원</span>
      </div>

      {/* 월별 바 차트 */}
      <div className="bg-stone-elevated rounded-2xl p-8 h-64 flex flex-col justify-end border border-stone-border">
        <div className="flex justify-between items-end h-full px-4 mb-4">
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(m => {
            const monthTotal = ACCOUNT_DIVIDENDS
              .filter(d => new Date(d.date).getMonth() + 1 === m)
              .reduce((acc, d) => acc + d.net, 0);
            const height = monthTotal > 0 ? Math.min(100, (monthTotal / 50000) * 100) : 5;
            return (
              <div key={m} className="flex flex-col items-center gap-2 flex-1">
                <div
                  className={cn('w-4 rounded-full transition-all duration-500', monthTotal > 0 ? 'bg-stone-text-primary' : 'bg-stone-border')}
                  style={{ height: `${height}%` }}
                />
                <span className="text-[10px] text-stone-muted font-bold">{m}월</span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm font-black text-stone-text-primary uppercase tracking-widest ml-1">상세 내역</h3>
        <div className="bg-stone-surface border border-stone-border rounded-[32px] overflow-hidden shadow-sm">
          <div className="divide-y divide-stone-elevated">
            {ACCOUNT_DIVIDENDS.map(item => (
              <div key={item.id} className="p-6 flex items-center justify-between hover:bg-stone-bg transition-colors">
                <div className="flex items-center gap-6">
                  <span className="text-[10px] font-black text-stone-muted font-mono w-16">{item.date}</span>
                  <div className="w-10 h-10 rounded-lg bg-stone-elevated border border-stone-border flex items-center justify-center text-[10px] font-black text-stone-muted shrink-0">
                    {item.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-stone-text-primary">{item.name}</p>
                    <p className="text-[10px] text-stone-muted font-bold uppercase tracking-widest mt-0.5">
                      {item.qty}주 | 주당 {item.perToken.toLocaleString()}원
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-black text-stone-gold">+{item.net.toLocaleString()}원</p>
                  <p className="text-[10px] text-stone-muted font-bold">세전 {item.gross.toLocaleString()}원</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ── 수익분석 탭 ───────────────────────────────────────────────
function AnalysisTab() {
  const totalProfit = PROFIT_ANALYSIS_DATA.reduce((acc, d) => acc + d.profit, 0);
  const sellProfit  = PROFIT_ANALYSIS_DATA.filter(d => d.type === 'sell').reduce((acc, d) => acc + d.profit, 0);
  const divProfit   = PROFIT_ANALYSIS_DATA.filter(d => d.type === 'dividend').reduce((acc, d) => acc + d.profit, 0);
  const intProfit   = PROFIT_ANALYSIS_DATA.filter(d => d.type === 'interest').reduce((acc, d) => acc + d.profit, 0);

  return (
    <div className="space-y-8 pb-20">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-4 px-4 py-2 bg-stone-elevated rounded-xl">
          <button className="text-stone-muted hover:text-stone-text-primary"><ChevronDown size={18} className="rotate-90" /></button>
          <span className="text-sm font-bold text-stone-text-primary">2026년 3월</span>
          <button className="text-stone-muted hover:text-stone-text-primary"><ChevronDown size={18} className="-rotate-90" /></button>
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-bold text-stone-muted">총 실현수익</p>
        <div className="flex items-baseline gap-6">
          <h2 className={cn('text-4xl font-black tracking-tight', totalProfit >= 0 ? 'text-stone-text-primary' : 'text-stone-buy')}>
            {totalProfit.toLocaleString()}원
          </h2>
          <div className="flex gap-4 text-sm font-bold">
            <span className="text-stone-muted">판매수익 <span className={cn(sellProfit >= 0 ? 'text-stone-text-primary' : 'text-stone-buy')}>{sellProfit.toLocaleString()}원</span></span>
            <span className="text-stone-muted">배당금 <span className="text-stone-text-primary">{divProfit.toLocaleString()}원</span></span>
            <span className="text-stone-muted">계좌이자 <span className="text-stone-text-primary">{intProfit.toLocaleString()}원</span></span>
          </div>
        </div>
      </div>

      <div className="bg-stone-surface border border-stone-border rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-elevated">
          {PROFIT_ANALYSIS_DATA.map((item, i) => (
            <div key={i} className="p-6 flex items-center justify-between hover:bg-stone-bg transition-colors">
              <div className="flex items-center gap-6">
                <span className="text-[10px] font-black text-stone-muted font-mono w-16">{item.date}</span>
                <div className="w-10 h-10 rounded-lg bg-stone-elevated border border-stone-border flex items-center justify-center text-[10px] font-black text-stone-muted shrink-0">
                  {item.type === 'interest' ? <Coins size={16} className="text-stone-muted" /> : item.name.slice(0, 2)}
                </div>
                <div>
                  <p className="text-sm font-bold text-stone-text-primary">{item.name}</p>
                  <p className="text-[10px] text-stone-muted font-bold uppercase tracking-widest mt-0.5">
                    {item.type === 'sell' ? '매매 차익' : item.type === 'dividend' ? '배당 수익' : '이자 수익'}
                  </p>
                </div>
              </div>
              <p className={cn('text-sm font-black', item.profit >= 0 ? 'text-stone-gold' : 'text-stone-buy')}>
                {item.profit >= 0 ? '+' : ''}{item.profit.toLocaleString()}원
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 계좌관리 탭 ───────────────────────────────────────────────
function SettingsTab() {
  return (
    <div className="max-w-2xl space-y-8">
      <h2 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">계좌 관리</h2>
      <div className="bg-stone-surface border border-stone-border rounded-[32px] p-8 space-y-8 shadow-sm">
        <div className="flex items-center gap-6">
          <div className="w-20 h-20 bg-stone-text-primary rounded-2xl flex items-center justify-center shadow-lg">
            <User size={32} className="text-stone-bg" />
          </div>
          <div>
            <p className="text-xl font-black text-stone-text-primary tracking-tight">{MOCK_USER.name}</p>
            <p className="text-sm text-stone-muted font-bold">{MOCK_USER.email}</p>
          </div>
        </div>

        <div className="space-y-4">
          {[
            { icon: Wallet,   color: 'text-stone-gold', label: '연결된 지갑',  value: '0x742d35Cc...1F3A' },
            { icon: Landmark, color: 'text-stone-buy',  label: '출금 계좌',    value: '국민은행 ****4521' },
          ].map((item, i) => {
            const Icon = item.icon;
            return (
              <div key={i} className="flex items-center justify-between p-6 bg-stone-elevated rounded-2xl border border-stone-border">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 bg-stone-surface rounded-xl flex items-center justify-center border border-stone-border shadow-sm">
                    <Icon size={20} className={item.color} />
                  </div>
                  <div>
                    <p className="text-[10px] font-black text-stone-muted uppercase tracking-widest mb-0.5">{item.label}</p>
                    <p className="text-sm font-bold text-stone-text-primary font-mono">{item.value}</p>
                  </div>
                </div>
                <button className="text-[10px] font-black text-stone-gold uppercase tracking-widest hover:underline">변경하기</button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
