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
import { MOCK_USER, PROFIT_ANALYSIS_DATA } from "../data/mock.js";

import {
  fetchBalance,
  fetchPortfolio,
  deposit,
  withdraw,
  fetchBankingHistory,
  fetchOrderHistory,
  cancelOrder,
  fetchDividendHistory,
  fetchDividendTotal,
  fetchAccountSummary,
  fetchSellHistory,
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

const FILTER_TX_TYPES = {
  전체: [],
  입출금: ["DEPOSIT", "WITHDRAWAL"],
  매수: ["TRADE_SETTLEMENT_BUY"],
  매도: ["TRADE_SETTLEMENT_SELL"],
  배당금: ["DIVIDEND_DEPOSIT"],
};

export function MyAccountPage() {
  const location = useLocation();
  const [activeSubTab, setActiveSubTab] = useState("assets");
  const [historyFilter, setHistoryFilter] = useState("전체");
  const [orderTab, setOrderTab] = useState("all");
  const [orders, setOrders] = useState([]);
  const [ordersPage, setOrdersPage] = useState(0);
  const [ordersTotalPages, setOrdersTotalPages] = useState(0);
  const [isFillModalOpen, setIsFillModalOpen] = useState(false);
  const [isSendModalOpen, setIsSendModalOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [balance, setBalance] = useState(null);
  const [portfolio, setPortfolio] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [cancelPassword, setCancelPassword] = useState("");
  const [cancelOrderId, setCancelOrderId] = useState(null);

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

  async function loadOrders(page) {
    try {
      const res = await fetchOrderHistory(page, orderTab);
      setOrders(res.data.content);
      setOrdersTotalPages(res.data.totalPages);
      setOrdersPage(page);
    } catch (e) {
      alert(e.response?.data?.message || "주문 내역을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    if (activeSubTab === "orders") loadOrders(0);
  }, [activeSubTab, orderTab]);

  async function loadHistory(page) {
    try {
      const txTypes = FILTER_TX_TYPES[historyFilter] ?? [];
      const res = await fetchBankingHistory(page, txTypes);
      setHistory(res.data.content);
      setHistoryTotalPages(res.data.totalPages);
      setHistoryPage(page);
    } catch (e) {
      alert(e.response?.data?.message || "거래 내역을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    if (activeSubTab === "history") loadHistory(0);
  }, [activeSubTab, historyFilter]);

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
    setCancelOrderId(orderId);
    setCancelPassword("");
  }

  async function handleCancelConfirm() {
    try {
      await cancelOrder(cancelOrderId, cancelPassword);
      setCancelOrderId(null);
      setCancelPassword("");
      loadOrders(ordersPage);
    } catch (e) {
      alert(e.response?.data?.message || "주문 취소에 실패했습니다.");
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
            orders={orders}
            page={ordersPage}
            totalPages={ordersTotalPages}
            onPageChange={(p) => loadOrders(p)}
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

      {cancelOrderId && (
        <OrderPinPadModal
          title="주문 취소 확인"
          description="취소할 주문 내역을 확인한 뒤 계좌 비밀번호를 입력해 주세요."
          password={cancelPassword}
          onChange={setCancelPassword}
          onClose={() => setCancelOrderId(null)}
          onConfirm={handleCancelConfirm}
        />
      )}
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
      case "TRADE_SETTLEMENT_BUY":
        return "매수 체결";
      case "TRADE_SETTLEMENT_SELL":
        return "매도 체결";
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
          {items.length > 0 ? (
            items.map((item) => (
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
                      item.txType === "TRADE_SETTLEMENT_SELL" ||
                      item.txType === "DIVIDEND_DEPOSIT" ||
                      item.txType === "ORDER_UNLOCK"
                      ? "text-brand-red"
                      : "text-brand-blue",
                  )}
                >
                  {item.txType === "DEPOSIT" ||
                  item.txType === "TRADE_SETTLEMENT_SELL" ||
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
function OrdersTab({
  orderTab,
  onOrderTab,
  orders,
  page,
  totalPages,
  onPageChange,
  onCancel,
}) {
  function formatDateTime(createdAt) {
    const d = new Date(createdAt);
    const date = d
      .toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })
      .replace(/\. /g, ".")
      .replace(/\.$/, "");
    const time = d.toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
    return `${date} ${time}`;
  }

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
          {orders.length > 0 ? (
            orders.map((order) => (
              <div
                key={order.orderId}
                className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
              >
                <div className="flex items-center gap-6">
                  <div className="flex flex-col w-16">
                    <span className="text-[10px] font-black text-stone-400 font-mono">
                      {formatDateTime(order.createdAt)}
                    </span>
                    <span className="text-xs font-bold text-stone-800 mt-1">
                      {order.tokenSymbol}
                    </span>
                  </div>
                  <div className="w-10 h-10 rounded-lg bg-stone-100 border border-stone-200 flex items-center justify-center text-[10px] font-black text-stone-400 shrink-0">
                    {order.tokenSymbol.slice(0, 2)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p
                        className={cn(
                          "text-sm font-black",
                          order.orderType === "BUY"
                            ? "text-brand-red"
                            : "text-brand-blue",
                        )}
                      >
                        {order.orderType === "BUY"
                          ? "지정가 매수"
                          : "지정가 매도"}
                      </p>
                      <span
                        className={cn(
                          "text-[9px] font-black px-1.5 py-0.5 rounded uppercase tracking-widest",
                          order.orderStatus === "FILLED"
                            ? "bg-brand-red-light text-brand-red"
                            : order.orderStatus === "PARTIAL"
                              ? "bg-blue-100 text-blue-600"
                              : order.orderStatus === "CANCELLED" ||
                                  order.orderStatus === "FAILED"
                                ? "bg-stone-100 text-stone-400"
                                : "bg-[#fef6dc] text-[#a07828]",
                        )}
                      >
                        {order.orderStatus === "FILLED"
                          ? "체결"
                          : order.orderStatus === "PARTIAL"
                            ? "부분체결"
                            : order.orderStatus === "CANCELLED"
                              ? "취소"
                              : order.orderStatus === "FAILED"
                                ? "실패"
                                : "미체결"}
                      </span>
                    </div>
                    <p className="text-[10px] text-stone-400 font-bold mt-0.5">
                      {order.orderPrice.toLocaleString()}원 |{" "}
                      {order.orderQuantity}주
                      {order.orderStatus !== "FILLED" &&
                        ` (잔량 ${order.remainingQuantity}주)`}
                    </p>
                  </div>
                </div>
                <div className="text-right flex items-center gap-4">
                  {order.orderStatus === "FILLED" ? (
                    <div>
                      <p className="text-sm font-black text-stone-800">
                        {(
                          order.orderPrice * order.filledQuantity
                        )?.toLocaleString()}
                        원
                      </p>
                      <span className="text-[10px] font-black text-brand-red uppercase tracking-widest">
                        체결완료
                      </span>
                    </div>
                  ) : order.orderStatus === "CANCELLED" ||
                    order.orderStatus === "FAILED" ? (
                    <span className="text-[10px] font-black text-stone-400 uppercase tracking-widest">
                      {order.orderStatus === "CANCELLED" ? "취소됨" : "실패"}
                    </span>
                  ) : (
                    <button
                      onClick={() => onCancel(order.orderId)}
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
      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </div>
  );
}

// ── 배당금 탭 ─────────────────────────────────────────────────
function DividendsTab() {
  const [dividends, setDividends] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [year, setYear] = useState(new Date().getFullYear());
  const [totalDividends, setTotalDividends] = useState(0);

  async function loadDividends(p, y) {
    try {
      const res = await fetchDividendHistory(p, y);
      setDividends(res.data.content);
      setTotalPages(res.data.totalPages);
      setPage(p);
    } catch (e) {
      alert(e.response?.data?.message || "배당금 내역을 불러오지 못했습니다.");
    }
  }

  async function loadTotal(y) {
    try {
      const res = await fetchDividendTotal(y);
      setTotalDividends(res.data);
    } catch (e) {
      console.error(e);
    }
  }

  useEffect(() => {
    loadDividends(0, year);
    loadTotal(year); // ← 추가
  }, [year]);

  return (
    <div className="space-y-8 pb-20">
      {/* 연도 선택 */}
      <div className="flex items-center gap-3">
        <div className="w-4 h-4 rounded-full bg-stone-800 shadow-sm" />
        <button
          onClick={() => {
            setYear((y) => y - 1);
          }}
          className="text-stone-400 hover:text-stone-800 font-black px-1"
        >
          ‹
        </button>
        <h2 className="text-lg font-bold text-stone-800">{year}년</h2>
        <button
          onClick={() => {
            setYear((y) => y + 1);
          }}
          className="text-stone-400 hover:text-stone-800 font-black px-1"
        >
          ›
        </button>
      </div>

      {/* 받은 배당금 합계 */}
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
            // settlementMonth 기준으로 집계 (mock의 new Date(d.date).getMonth()+1 대신)
            const monthTotal = dividends
              .filter((d) => d.settlementMonth === m)
              .reduce((acc, d) => acc + d.memberIncome, 0);
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

      {/* 상세 내역 */}
      <div className="space-y-4">
        <h3 className="text-sm font-black text-stone-800 uppercase tracking-widest ml-1">
          상세 내역
        </h3>
        <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
          <div className="divide-y divide-stone-100">
            {dividends.length === 0 ? (
              <EmptyState message="배당금 내역이 없습니다." className="m-6" />
            ) : (
              dividends.map((item) => (
                <div
                  key={item.allocationPayoutId}
                  className="p-6 flex items-center justify-between hover:bg-stone-50 transition-colors"
                >
                  <div className="flex items-center gap-6">
                    {/* mock: item.date → 백엔드: item.settlementYear, item.settlementMonth */}
                    <span className="text-[10px] font-black text-stone-400 font-mono w-16">
                      {item.settlementYear}-
                      {String(item.settlementMonth).padStart(2, "0")}
                    </span>
                    {/* mock: item.symbol → 백엔드: item.tokenSymbol */}
                    <div
                      className="w-10 h-10 rounded-lg bg-stone-100 border border-stone-200 flex items-center
  justify-center text-[10px] font-black text-stone-400 shrink-0"
                    >
                      {item.tokenSymbol.slice(0, 2)}
                    </div>
                    <div>
                      {/* mock: item.name → 백엔드: item.tokenName */}
                      <p className="text-sm font-bold text-stone-800">
                        {item.tokenName}
                      </p>
                      {/* mock: item.qty, item.perToken → 백엔드: item.holdingQuantity, item.perTokenAmount */}
                      <p className="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-0.5">
                        {item.holdingQuantity}주 | 주당{" "}
                        {item.perTokenAmount.toLocaleString()}원
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    {/* mock: item.net → 백엔드: item.memberIncome */}
                    <p className="text-sm font-black text-stone-600">
                      +{item.memberIncome.toLocaleString()}원
                    </p>
                    {/* mock: item.gross (세전) → 동일하게 memberIncome 사용 (세금 처리 없음) */}
                    <p className="text-[10px] text-stone-400 font-bold">
                      세전 {item.memberIncome.toLocaleString()}원
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={(p) => loadDividends(p, year)}
      />
    </div>
  );
}

// ── 수익분석 탭 ───────────────────────────────────────────────
function AnalysisTab() {
  const [page, setPage] = useState(0);
  const [year, setYear] = useState(new Date().getFullYear());
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [sellHistory, setSellHistory] = useState([]);
  const [dividendHistory, setDividendHistory] = useState([]);
  const [summary, setSummary] = useState({
    thisMonthTotal: 0,
    thisMonthSellProfit: 0,
    thisMonthDividend: 0,
  });

  async function loadDetail(y, m) {
    try {
      const [sellRes, divRes] = await Promise.all([
        fetchSellHistory(y, m),
        fetchDividendHistory(0, y, m, 1000),
      ]);
      setSellHistory(sellRes.data.content);
      setDividendHistory(divRes.data.content);
    } catch (e) {
      alert(e.response?.data?.message || "수익 내역을 불러오지 못했습니다.");
    }
  }

  const combinedList = [
    ...sellHistory.map((item) => ({ ...item, listType: "sell" })),
    ...dividendHistory.map((item) => ({ ...item, listType: "dividend" })),
  ].sort(
    (a, b) =>
      new Date(b.executedAt ?? b.createdAt) -
      new Date(a.executedAt ?? a.createdAt),
  );

  const PAGE_SIZE = 10;
  const totalPageCount = Math.ceil(combinedList.length / PAGE_SIZE);
  const paginatedList = combinedList.slice(
    page * PAGE_SIZE,
    (page + 1) * PAGE_SIZE,
  );
  useEffect(() => {
    setPage(0);
    loadDetail(year, month);
    fetchAccountSummary(year, month)
      .then((res) => setSummary(res.data))
      .catch((e) =>
        alert(e.response?.data?.message || "요약 정보를 불러오지 못했습니다."),
      );
  }, [year, month]);

  return (
    <div className="space-y-8 pb-20">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-4 px-4 py-2 bg-stone-100 border border-stone-200 rounded-xl">
          <button
            onClick={() => {
              if (month === 1) {
                setMonth(12);
                setYear((y) => y - 1);
              } else setMonth((m) => m - 1);
            }}
          >
            ‹
          </button>
          <span>
            {year}년 {month}월
          </span>
          <button
            onClick={() => {
              if (month === 12) {
                setMonth(1);
                setYear((y) => y + 1);
              } else setMonth((m) => m + 1);
            }}
          >
            ›
          </button>
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-bold text-stone-400">총 실현수익</p>
        <div className="flex items-baseline gap-6">
          <h2
            className={cn(
              "text-4xl font-black tracking-tight",
              summary.thisMonthTotal >= 0
                ? "text-stone-800"
                : "text-brand-blue",
            )}
          >
            {summary.thisMonthTotal.toLocaleString()}원
          </h2>
          <div className="flex gap-4 text-sm font-bold">
            <span className="text-stone-400">
              판매수익{" "}
              <span
                className={cn(
                  summary.thisMonthSellProfit >= 0
                    ? "text-stone-800"
                    : "text-brand-blue",
                )}
              >
                {summary.thisMonthSellProfit.toLocaleString()}원
              </span>
            </span>
            <span className="text-stone-400">
              배당금{" "}
              <span className="text-stone-800">
                {summary.thisMonthDividend.toLocaleString()}원
              </span>
            </span>
          </div>
        </div>
      </div>

      <div className="bg-white border border-stone-200 rounded-[32px] overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-100">
          {paginatedList.map((item, i) => (
            <div
              key={
                item.listType === "sell"
                  ? `sell-${item.tradeId}`
                  : `dividend-${item.allocationPayoutId}`
              }
              className="p-6 flex items-center justify-between"
            >
              <div className="flex items-center gap-6">
                <span className="text-[10px] font-black text-stone-400 font-mono w-16">
                  {new Date(
                    item.executedAt ?? item.createdAt,
                  ).toLocaleDateString("ko-KR")}
                </span>
                <div>
                  <p className="text-sm font-bold text-stone-800">
                    {item.tokenName}
                  </p>
                  <p className="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-0.5">
                    {item.listType === "sell" ? "매매 차익" : "배당 수익"}
                  </p>
                </div>
              </div>
              <p className={cn("text-sm font-black", "text-brand-red")}>
                +{(item.bankingAmount ?? item.memberIncome).toLocaleString()}원
              </p>
            </div>
          ))}
        </div>
      </div>
      <Pagination
        page={page}
        totalPages={totalPageCount}
        onPageChange={(p) => setPage(p)}
      />
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

function OrderPinPadModal({
  title,
  description,
  password,
  errorMessage,
  submitting,
  onChange,
  onClose,
  onConfirm,
}) {
  const masked = password.length > 0 ? "•".repeat(password.length) : "○ ○ ○ ○";
  const keys = [
    "1",
    "2",
    "3",
    "4",
    "5",
    "6",
    "7",
    "8",
    "9",
    "reset",
    "0",
    "delete",
  ];

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50"
      onClick={onClose}
    >
      <div
        className="w-[360px] rounded-2xl border border-stone-200 bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="space-y-2">
          <h3 className="text-base font-black text-stone-800">{title}</h3>
          <p className="text-sm font-medium text-stone-500">{description}</p>
        </div>

        <div className="mt-5 rounded-2xl border border-stone-200 bg-stone-100 px-4 py-5">
          <div className="text-center font-mono text-2xl font-black tracking-[0.35em] text-stone-800">
            {masked}
          </div>
        </div>

        {errorMessage && (
          <p className="mt-3 text-center text-[11px] font-bold text-brand-red">
            {errorMessage}
          </p>
        )}

        <div className="mt-5 grid grid-cols-3 gap-2">
          {keys.map((key) => (
            <button
              key={key}
              type="button"
              onClick={() => {
                if (key === "reset") {
                  onChange("");
                  return;
                }
                if (key === "delete") {
                  onChange(password.slice(0, -1));
                  return;
                }
                if (password.length >= 4) return;
                onChange(`${password}${key}`);
              }}
              className={cn(
                "rounded-xl border py-3 text-sm font-black transition-colors",
                key === "reset" || key === "delete"
                  ? "border-stone-200 bg-stone-100 text-stone-500 hover:bg-stone-200"
                  : "border-stone-200 bg-white text-stone-800 hover:bg-stone-100",
              )}
            >
              {key === "reset" ? "초기화" : key === "delete" ? "지우기" : key}
            </button>
          ))}
        </div>

        <div className="mt-6 rounded-2xl border border-stone-200 bg-stone-50 p-4 text-sm text-stone-600">
          {description}
        </div>

        <div className="mt-4 flex gap-2">
          <button
            onClick={onClose}
            disabled={submitting}
            className="flex-1 rounded-xl border border-stone-200 bg-white py-3 text-sm font-black text-stone-500 hover:bg-stone-100 disabled:opacity-50"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            disabled={submitting || password.length !== 4}
            className="flex-1 rounded-xl bg-stone-800 py-3 text-sm font-black text-white hover:bg-stone-700 disabled:opacity-50"
          >
            {submitting ? "처리 중..." : "확인"}
          </button>
        </div>
      </div>
    </div>
  );
}
