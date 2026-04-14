import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import {
  History,
  Wallet,
  ArrowUpRight,
  HandCoins,
  TrendingUp,
  Settings as SettingsIcon,
  Coins,
  User,
  Landmark,
  ChevronDown,
} from "lucide-react";
import {
  MOCK_USER,
  ACCOUNT_DIVIDENDS,
  PROFIT_ANALYSIS_DATA,
  OPEN_ORDERS,
  FILLED_ORDERS,
} from "../data/mock.js";
import {
  fetchBalance,
  fetchPortfolio,
  deposit,
  withdraw,
  fetchBankingHistory,
} from "../lib/api.js";
import { cn } from "../lib/utils.js";
import { Modal } from "../components/ui/Modal.jsx";
import { EmptyState } from "../components/ui/EmptyState.jsx";
import { ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { Pagination } from "../components/ui/Pagination.jsx";

const ASSET_PIE = [
  { name: "서울강남빌딩", value: 45, color: "var(--color-brand-blue)" },
  { name: "송도 리조트", value: 25, color: "#64d2ff" },
  { name: "아트프라임", value: 15, color: "var(--color-brand-gold)" },
  { name: "기타", value: 15, color: "#a8a29e" },
];

const SIDEBAR_ITEMS = [
  { id: "assets", label: "자산", icon: Wallet },
  { id: "history", label: "거래내역", icon: History },
  { id: "orders", label: "주문내역", icon: ArrowUpRight },
  { id: "dividends", label: "배당금 내역", icon: HandCoins },
  { id: "analysis", label: "수익분석", icon: TrendingUp },
  { id: "settings", label: "계좌관리", icon: SettingsIcon },
];

export function MyAccountPage() {
  const location = useLocation();
  const [activeSubTab, setActiveSubTab] = useState("assets");
  const [historyFilter, setHistoryFilter] = useState("전체");
  const [orderTab, setOrderTab] = useState("all");
  const [openOrders, setOpenOrders] = useState(OPEN_ORDERS);
  const [isFillModalOpen, setIsFillModalOpen] = useState(false);
  const [isSendModalOpen, setIsSendModalOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [balance, setBalance] = useState(null);
  const [portfolio, setPortfolio] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);

  // 알람 클릭으로 넘어온 경우 해당 탭 자동 선택
  useEffect(() => {
    const tab = location.state?.tab;
    if (tab) setActiveSubTab(tab);
  }, [location.state]);

  useEffect(() => {
    fetchBalance().then((res) => setBalance(res.data));
    fetchPortfolio().then((res) => setPortfolio(res.data));
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const [balanceRes, portfolioRes] = await Promise.all([
          fetchBalance(),
          fetchPortfolio(),
        ]);
        setBalance(balanceRes.data);
        setPortfolio(portfolioRes.data);
      } catch (e) {
        alert(e.response?.data?.message || "계좌 정보를 불러오지 못했습니다.");
      }
    })();
  }, []);

  async function loadHistory(page) {
    try {
      const res = await fetchBankingHistory(page);
      setHistory(res.data.content);
      setHistoryTotalPages(res.data.totalPages);
      setHistoryPage(page);
    } catch (e) {
      alert(e.response?.data?.message || "거래 내역을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    if (activeSubTab === "history") loadHistory(0);
  }, [activeSubTab]);

  async function handleFill() {
    if (!amount || isNaN(Number(amount))) return;

    try {
      await deposit(Number(amount));
      const res = await fetchBalance();
      setBalance(res.data);
      setIsFillModalOpen(false);
      setAmount("");
    } catch (e) {
      alert(e.response?.data?.message || "충전에 실패했습니다.");
    }
  }

  async function handleSend() {
    if (!amount || isNaN(Number(amount))) return;
    try {
      await withdraw(Number(amount));
      const res = await fetchBalance();
      setBalance(res.data);
      setIsSendModalOpen(false);
      setAmount("");
    } catch (e) {
      alert(e.response?.data?.message || "송금에 실패했습니다.");
    }
  }

  function handleCancelOrder(orderId) {
    if (window.confirm("정말 이 주문을 취소하시겠습니까?")) {
      setOpenOrders((prev) => prev.filter((o) => o.id !== orderId));
    }
  }

  return (
    <div className="flex gap-8 pb-20">
      {/* 사이드바 */}
      <aside className="w-48 shrink-0">
        <nav className="space-y-1 sticky top-24">
          {SIDEBAR_ITEMS.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => setActiveSubTab(item.id)}
                className={cn(
                  "w-full flex items-center gap-3 px-4 py-3 rounded-md text-sm font-medium transition-colors text-left",
                  activeSubTab === item.id
                    ? "bg-stone-100 text-stone-800 border border-stone-200"
                    : "text-stone-500 hover:bg-stone-100 hover:text-stone-800",
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
        {activeSubTab === "assets" && (
          <AssetsTab
            onFill={() => setIsFillModalOpen(true)}
            onSend={() => setIsSendModalOpen(true)}
            balance={balance}
            portfolio={portfolio}
          />
        )}
        {activeSubTab === "history" && (
          <HistoryTab
            filter={historyFilter}
            onFilter={setHistoryFilter}
            items={history}
            page={historyPage}
            totalPages={historyTotalPages}
            onPageChange={loadHistory}
          />
        )}
        {activeSubTab === "orders" && (
          <OrdersTab
            orderTab={orderTab}
            onOrderTab={setOrderTab}
            openOrders={openOrders}
            onCancel={handleCancelOrder}
          />
        )}
        {activeSubTab === "dividends" && <DividendsTab />}
        {activeSubTab === "analysis" && <AnalysisTab />}
        {activeSubTab === "settings" && <SettingsTab />}
      </div>

      {/* 충전 모달 */}
      <Modal
        isOpen={isFillModalOpen}
        onClose={() => {
          setIsFillModalOpen(false);
          setAmount("");
        }}
        title="충전"
      >
        <div className="p-8 space-y-4">
          <input
            type="number"
            placeholder="금액 입력"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-stone-800 outline-none focus:border-stone-800 text-sm font-bold"
          />
          <button
            onClick={handleFill}
            className="w-full py-3 bg-stone-800 text-white rounded-xl font-black hover:bg-stone-700 transition-all"
          >
            충전하기
          </button>
        </div>
      </Modal>

      {/* 송금 모달 */}
      <Modal
        isOpen={isSendModalOpen}
        onClose={() => {
          setIsSendModalOpen(false);
          setAmount("");
        }}
        title="송금"
      >
        <div className="p-8 space-y-4">
          <input
            type="number"
            placeholder="금액 입력"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-stone-800 outline-none focus:border-stone-800 text-sm font-bold"
          />
          <button
            onClick={handleSend}
            className="w-full py-3 bg-stone-100 border border-stone-200 text-stone-500 rounded-xl font-black hover:bg-stone-200 transition-all"
          >
            송금하기
          </button>
        </div>
      </Modal>
    </div>
  );
}

// ── 자산 탭 ────────────────────────────────────────────────────
function AssetsTab({ onFill, onSend, balance, portfolio }) {
  return (
    <div className="max-w-4xl space-y-12 pb-20">
      <div className="flex flex-col md:flex-row justify-between gap-8">
        <div className="space-y-4 flex-1">
          <p className="text-stone-400 text-sm font-medium">
            계좌번호 {balance?.accountNumber ?? "-"}
          </p>
          <h2 className="text-4xl font-black text-stone-800 tracking-tight">
            총 자산{" "}
            {balance
              ? (
                  balance.availableBalance + balance.lockedBalance
                ).toLocaleString()
              : "-"}
            원
          </h2>
          <div className="flex gap-3 pt-2">
            <button
              onClick={onFill}
              className="px-8 py-2.5 rounded-full bg-stone-800 text-white text-sm font-bold hover:bg-stone-700 transition-all shadow-lg"
            >
              채우기
            </button>
            <button
              onClick={onSend}
              className="px-8 py-2.5 rounded-full bg-stone-100 text-stone-500 text-sm font-bold hover:bg-stone-200 transition-all border border-stone-200"
            >
              보내기
            </button>
          </div>
        </div>

        <div className="bg-stone-100 rounded-2xl p-6 border border-stone-200 w-full md:w-80 shadow-sm">
          <h3 className="text-xs font-bold text-stone-500 mb-4 uppercase tracking-wider">
            보유 토큰 비중
          </h3>
          <div className="flex items-center gap-6">
            <div className="w-24 h-24">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={ASSET_PIE}
                    innerRadius={30}
                    outerRadius={45}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {ASSET_PIE.map((entry, i) => (
                      <Cell key={i} fill={entry.color} stroke="none" />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex-1 space-y-1.5">
              {ASSET_PIE.slice(0, 3).map((item, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between text-[10px] font-bold"
                >
                  <div className="flex items-center gap-2">
                    <div
                      className="w-2 h-2 rounded-full"
                      style={{ backgroundColor: item.color }}
                    />
                    <span className="text-stone-500">{item.name}</span>
                  </div>
                  <span className="text-stone-800">{item.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="pt-8 border-t border-stone-200">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-stone-800">
            총 주문 가능 금액
          </h3>
          <span className="text-lg font-bold text-stone-800">
            {balance?.availableBalance?.toLocaleString() ?? "-"}원
          </span>
        </div>
        <div className="flex justify-between items-center py-4 border-t border-stone-100">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-stone-200 rounded-lg flex items-center justify-center">
              <Coins size={16} className="text-stone-400" />
            </div>
            <span className="text-sm font-medium text-stone-500">원화</span>
          </div>
          <span className="text-sm font-bold text-stone-800">
            {balance?.availableBalance?.toLocaleString() ?? "-"}원
          </span>
        </div>
      </div>

      <div className="pt-8 border-t border-stone-200">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h3 className="text-lg font-bold text-stone-800 mb-1">내 투자</h3>
            <p className="text-xs text-stone-400">손익 등록 제외</p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-black text-stone-800">
              총 평가금액{" "}
              {portfolio
                .reduce((sum, a) => sum + a.evaluationAmount, 0)
                .toLocaleString()}
              원
            </p>
            <p
              className={cn(
                "text-sm font-bold",
                portfolio.reduce((sum, a) => sum + a.profit, 0) >= 0
                  ? "text-brand-red"
                  : "text-brand-blue",
              )}
            >
              {portfolio.reduce((sum, a) => sum + a.profit, 0) >= 0 ? "+" : ""}
              {portfolio.reduce((sum, a) => sum + a.profit, 0).toLocaleString()}
              원
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-y-8">
          {portfolio.map((a, i) => {
            const isUp = a.profit >= 0;
            return (
              <div
                key={i}
                className="flex items-center justify-between group cursor-pointer"
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-stone-100 border border-stone-200 flex items-center justify-center text-sm font-black text-stone-400">
                    {a.tokenSymbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-stone-800">
                      {a.tokenName}
                    </p>
                    <p className="text-xs text-stone-400 font-medium">
                      {a.quantity}주
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-stone-800">
                    {a.evaluationAmount.toLocaleString()}원
                  </p>
                  <p
                    className={cn(
                      "text-xs font-bold",
                      isUp ? "text-brand-red" : "text-brand-blue",
                    )}
                  >
                    {isUp ? "+" : ""}
                    {a.profit.toLocaleString()}원 ({isUp ? "+" : ""}
                    {Number(a.profitRate).toFixed(2)}%)
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        <div className="pt-8 border-t border-stone-200 space-y-4 mt-12">
          {[
            { label: "3월 수익", value: "0원" },
            { label: "판매수익", value: "0원" },
            { label: "배당금", value: "0원" },
          ].map((item, i) => (
            <div key={i} className="flex justify-between items-center">
              <span className="text-sm font-medium text-stone-500">
                {item.label}
              </span>
              <span className="text-sm font-bold text-stone-800">
                {item.value}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 거래내역 탭 ───────────────────────────────────────────────
function HistoryTab({
  filter,
  onFilter,
  items,
  page,
  totalPages,
  onPageChange,
}) {
  const filtered = items.filter((item) => {
    if (filter === "전체") return true;
    if (filter === "입출금")
      return item.txType === "DEPOSIT" || item.txType === "WITHDRAWAL";
    if (filter === "매수") return item.txType === "ORDER_LOCK";
    if (filter === "매도") return item.txType === "TRADE_SETTLEMENT";
    if (filter === "배당금") return item.txType === "DIVIDEND_DEPOSIT";
    return true;
  });

  function formatTitle(txType) {
    switch (txType) {
      case "DEPOSIT":
        return "계좌 입금";
      case "WITHDRAWAL":
        return "계좌 출금";
      case "ORDER_LOCK":
        return "주문 잠금";
      case "ORDER_UNLOCK":
        return "주문 해제";
      case "TRADE_SETTLEMENT":
        return "체결 정산";
      case "DIVIDEND_DEPOSIT":
        return "배당금 입금";
      default:
        return txType;
    }
  }

  function formatDate(createdAt) {
    return new Date(createdAt)
      .toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })
      .replace(/\. /g, ".")
      .replace(/\.$/, "");
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-black text-stone-800 uppercase tracking-tight">
          거래 내역
        </h2>
        <div className="flex gap-2">
          {["전체", "입출금", "매수", "매도", "배당금"].map((f) => (
            <button
              key={f}
              onClick={() => onFilter(f)}
              className={cn(
                "px-4 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all",
                filter === f
                  ? "bg-stone-800 text-white shadow-lg"
                  : "bg-stone-100 text-stone-500 hover:bg-stone-200 border border-stone-200",
              )}
            >
              {f}
            </button>
          ))}
        </div>
      </div>
      <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-100">
          {filtered.length > 0 ? (
            filtered.map((item) => (
              <div
                key={item.bankingId}
                className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
              >
                <div className="flex items-center gap-6">
                  <span className="text-[10px] font-black text-stone-400 font-mono">
                    {formatDate(item.createdAt)}
                  </span>
                  <div>
                    <p className="text-sm font-bold text-stone-800">
                      {formatTitle(item.txType)}
                    </p>
                    <p className="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-0.5">
                      잔고 {item.balanceSnapshot.toLocaleString()}원
                    </p>
                  </div>
                </div>
                <p
                  className={cn(
                    "text-sm font-black",
                    item.txType === "DEPOSIT" ||
                      item.txType === "TRADE_SETTLEMENT" ||
                      item.txType === "DIVIDEND_DEPOSIT" ||
                      item.txType === "ORDER_UNLOCK"
                      ? "text-brand-red"
                      : "text-brand-blue",
                  )}
                >
                  {item.txType === "DEPOSIT" ||
                  item.txType === "TRADE_SETTLEMENT" ||
                  item.txType === "DIVIDEND_DEPOSIT" ||
                  item.txType === "ORDER_UNLOCK"
                    ? "+"
                    : "-"}
                  {item.bankingAmount.toLocaleString()}원
                </p>
              </div>
            ))
          ) : (
            <EmptyState message="거래 내역이 없습니다." className="m-6" />
          )}
        </div>
      </div>

      {/* 페이지네이션 */}
      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </div>
  );
}

// ── 주문내역 탭 ───────────────────────────────────────────────
function OrdersTab({ orderTab, onOrderTab, openOrders, onCancel }) {
  const allOrders = [
    ...FILLED_ORDERS.map((o) => ({ ...o, isFilled: true })),
    ...openOrders.map((o) => ({ ...o, isFilled: false })),
  ];
  const displayOrders =
    orderTab === "filled"
      ? FILLED_ORDERS.map((o) => ({ ...o, isFilled: true }))
      : orderTab === "open"
        ? openOrders.map((o) => ({ ...o, isFilled: false }))
        : allOrders;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-black text-stone-800 uppercase tracking-tight">
          주문 내역
        </h2>
        <div className="flex bg-stone-100 border border-stone-200 p-1 rounded-xl">
          {[
            { id: "all", label: "전체" },
            { id: "filled", label: "체결" },
            { id: "open", label: "미체결" },
          ].map((t) => (
            <button
              key={t.id}
              onClick={() => onOrderTab(t.id)}
              className={cn(
                "px-6 py-1.5 rounded-lg text-xs font-bold transition-all",
                orderTab === t.id
                  ? "bg-white text-stone-800 shadow-sm border border-stone-200"
                  : "text-stone-400 hover:text-stone-600",
              )}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-100">
          {displayOrders.length > 0 ? (
            displayOrders.map((order) => (
              <div
                key={order.id}
                className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
              >
                <div className="flex items-center gap-6">
                  <div className="flex flex-col w-16">
                    <span className="text-[10px] font-black text-stone-400 font-mono">
                      {order.time}
                    </span>
                    <span className="text-xs font-bold text-stone-800 mt-1">
                      {order.symbol}
                    </span>
                  </div>
                  <div className="w-10 h-10 rounded-lg bg-stone-100 border border-stone-200 flex items-center justify-center text-[10px] font-black text-stone-400 shrink-0">
                    {order.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p
                        className={cn(
                          "text-sm font-black",
                          order.type.includes("매수")
                            ? "text-brand-red"
                            : "text-brand-blue",
                        )}
                      >
                        {order.type}
                      </p>
                      <span
                        className={cn(
                          "text-[9px] font-black px-1.5 py-0.5 rounded uppercase tracking-widest",
                          order.isFilled
                            ? "bg-brand-red-light text-brand-red"
                            : "bg-[#fef6dc] text-[#a07828]",
                        )}
                      >
                        {order.isFilled ? "체결" : "미체결"}
                      </span>
                    </div>
                    <p className="text-[10px] text-stone-400 font-bold mt-0.5">
                      {order.price.toLocaleString()}원 | {order.qty}주
                      {order.isFilled
                        ? ` | 수수료 ${order.fee?.toLocaleString()}원`
                        : ` (잔량 ${order.remaining}주)`}
                    </p>
                  </div>
                </div>
                <div className="text-right flex items-center gap-4">
                  {order.isFilled ? (
                    <div>
                      <p className="text-sm font-black text-stone-800">
                        {order.amount?.toLocaleString()}원
                      </p>
                      <span className="text-[10px] font-black text-brand-red uppercase tracking-widest">
                        체결완료
                      </span>
                    </div>
                  ) : (
                    <button
                      onClick={() => onCancel(order.id)}
                      className="px-4 py-2 rounded-xl bg-brand-red-light text-brand-red text-xs font-black hover:bg-[#fccfcf] transition-all border border-brand-red-light"
                    >
                      취소
                    </button>
                  )}
                </div>
              </div>
            ))
          ) : (
            <EmptyState message="주문 내역이 없습니다." className="m-6" />
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
        <div className="w-4 h-4 rounded-full bg-stone-800 shadow-sm" />
        <h2 className="text-lg font-bold text-stone-800">2026년</h2>
      </div>

      <div className="bg-stone-100 rounded-2xl p-8 flex justify-between items-center border border-stone-200">
        <span className="text-stone-500 font-medium">받은 배당금</span>
        <span className="text-stone-800 font-black text-2xl">
          {totalDividends.toLocaleString()}원
        </span>
      </div>

      {/* 월별 바 차트 */}
      <div className="bg-stone-100 rounded-2xl p-8 h-64 flex flex-col justify-end border border-stone-200">
        <div className="flex justify-between items-end h-full px-4 mb-4">
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => {
            const monthTotal = ACCOUNT_DIVIDENDS.filter(
              (d) => new Date(d.date).getMonth() + 1 === m,
            ).reduce((acc, d) => acc + d.net, 0);
            const height =
              monthTotal > 0 ? Math.min(100, (monthTotal / 50000) * 100) : 5;
            return (
              <div key={m} className="flex flex-col items-center gap-2 flex-1">
                <div
                  className={cn(
                    "w-4 rounded-full transition-all duration-500",
                    monthTotal > 0 ? "bg-stone-800" : "bg-stone-300",
                  )}
                  style={{ height: `${height}%` }}
                />
                <span className="text-[10px] text-stone-400 font-bold">
                  {m}월
                </span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm font-black text-stone-800 uppercase tracking-widest ml-1">
          상세 내역
        </h3>
        <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
          <div className="divide-y divide-stone-100">
            {ACCOUNT_DIVIDENDS.map((item) => (
              <div
                key={item.id}
                className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
              >
                <div className="flex items-center gap-6">
                  <span className="text-[10px] font-black text-stone-400 font-mono w-16">
                    {item.date}
                  </span>
                  <div className="w-10 h-10 rounded-lg bg-stone-100 border border-stone-200 flex items-center justify-center text-[10px] font-black text-stone-400 shrink-0">
                    {item.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-stone-800">
                      {item.name}
                    </p>
                    <p className="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-0.5">
                      {item.qty}주 | 주당 {item.perToken.toLocaleString()}원
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-black text-stone-600">
                    +{item.net.toLocaleString()}원
                  </p>
                  <p className="text-[10px] text-stone-400 font-bold">
                    세전 {item.gross.toLocaleString()}원
                  </p>
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
  const totalProfit = PROFIT_ANALYSIS_DATA.reduce(
    (acc, d) => acc + d.profit,
    0,
  );
  const sellProfit = PROFIT_ANALYSIS_DATA.filter(
    (d) => d.type === "sell",
  ).reduce((acc, d) => acc + d.profit, 0);
  const divProfit = PROFIT_ANALYSIS_DATA.filter(
    (d) => d.type === "dividend",
  ).reduce((acc, d) => acc + d.profit, 0);
  const intProfit = PROFIT_ANALYSIS_DATA.filter(
    (d) => d.type === "interest",
  ).reduce((acc, d) => acc + d.profit, 0);

  return (
    <div className="space-y-8 pb-20">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-4 px-4 py-2 bg-stone-100 border border-stone-200 rounded-xl">
          <button className="text-stone-400 hover:text-stone-800">
            <ChevronDown size={18} className="rotate-90" />
          </button>
          <span className="text-sm font-bold text-stone-800">2026년 3월</span>
          <button className="text-stone-400 hover:text-stone-800">
            <ChevronDown size={18} className="-rotate-90" />
          </button>
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-bold text-stone-400">총 실현수익</p>
        <div className="flex items-baseline gap-6">
          <h2
            className={cn(
              "text-4xl font-black tracking-tight",
              totalProfit >= 0 ? "text-stone-800" : "text-brand-blue",
            )}
          >
            {totalProfit.toLocaleString()}원
          </h2>
          <div className="flex gap-4 text-sm font-bold">
            <span className="text-stone-400">
              판매수익{" "}
              <span
                className={cn(
                  sellProfit >= 0 ? "text-stone-800" : "text-brand-blue",
                )}
              >
                {sellProfit.toLocaleString()}원
              </span>
            </span>
            <span className="text-stone-400">
              배당금{" "}
              <span className="text-stone-800">
                {divProfit.toLocaleString()}원
              </span>
            </span>
            <span className="text-stone-400">
              계좌이자{" "}
              <span className="text-stone-800">
                {intProfit.toLocaleString()}원
              </span>
            </span>
          </div>
        </div>
      </div>

      <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-100">
          {PROFIT_ANALYSIS_DATA.map((item, i) => (
            <div
              key={i}
              className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
            >
              <div className="flex items-center gap-6">
                <span className="text-[10px] font-black text-stone-400 font-mono w-16">
                  {item.date}
                </span>
                <div className="w-10 h-10 rounded-lg bg-stone-100 border border-stone-200 flex items-center justify-center text-[10px] font-black text-stone-400 shrink-0">
                  {item.type === "interest" ? (
                    <Coins size={16} className="text-stone-400" />
                  ) : (
                    item.name.slice(0, 2)
                  )}
                </div>
                <div>
                  <p className="text-sm font-bold text-stone-800">
                    {item.name}
                  </p>
                  <p className="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-0.5">
                    {item.type === "sell"
                      ? "매매 차익"
                      : item.type === "dividend"
                        ? "배당 수익"
                        : "이자 수익"}
                  </p>
                </div>
              </div>
              <p
                className={cn(
                  "text-sm font-black",
                  item.profit >= 0 ? "text-brand-red" : "text-brand-blue",
                )}
              >
                {item.profit >= 0 ? "+" : ""}
                {item.profit.toLocaleString()}원
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
      <h2 className="text-xl font-black text-stone-800 uppercase tracking-tight">
        계좌 관리
      </h2>
      <div className="bg-white border border-stone-200 rounded-[32px] p-8 space-y-8 shadow-sm">
        <div className="flex items-center gap-6">
          <div className="w-20 h-20 bg-stone-800 rounded-2xl flex items-center justify-center shadow-lg">
            <User size={32} className="text-white" />
          </div>
          <div>
            <p className="text-xl font-black text-stone-800 tracking-tight">
              {MOCK_USER.name}
            </p>
            <p className="text-sm text-stone-400 font-bold">
              {MOCK_USER.email}
            </p>
          </div>
        </div>

        <div className="space-y-4">
          {[
            {
              icon: Wallet,
              color: "text-stone-600",
              label: "연결된 지갑",
              value: "0x742d35Cc...1F3A",
            },
            {
              icon: Landmark,
              color: "text-brand-red",
              label: "출금 계좌",
              value: "국민은행 ****4521",
            },
          ].map((item, i) => {
            const Icon = item.icon;
            return (
              <div
                key={i}
                className="flex items-center justify-between p-6 bg-stone-100 rounded-2xl border border-stone-200"
              >
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 bg-white rounded-xl flex items-center justify-center border border-stone-200 shadow-sm">
                    <Icon size={20} className={item.color} />
                  </div>
                  <div>
                    <p className="text-[10px] font-black text-stone-400 uppercase tracking-widest mb-0.5">
                      {item.label}
                    </p>
                    <p className="text-sm font-bold text-stone-800 font-mono">
                      {item.value}
                    </p>
                  </div>
                </div>
                <button className="text-[10px] font-black text-stone-600 uppercase tracking-widest hover:underline">
                  변경하기
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
