import { useEffect, useState } from "react";
import {
  DollarSign,
  PieChart,
  Wallet,
  BarChart3,
  Layers,
  Landmark,
  ArrowUpRight,
  ArrowDownRight,
} from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import api from "../../lib/api.js";
import { cn } from "../../lib/utils.js";
import { imgSrc } from "./asset/assetUtils.jsx";

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function formatCurrency(value) {
  return `₩${toNumber(value).toLocaleString("ko-KR")}`;
}

function formatDateLabel(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function getDirectionSign(direction) {
  const normalized = String(direction ?? "").toUpperCase();
  if (normalized.includes("OUT") || normalized.includes("WITHDRAW") || normalized.includes("DEBIT")) {
    return -1;
  }
  return 1;
}

function buildChartData(items) {
  const dailyMap = new Map();

  items.forEach((item) => {
    const accountType = String(item?.accountType ?? "").toUpperCase();
    const createdAt = item?.createdAt;
    if (!createdAt || accountType !== "FEE") return;

    const dateKey = String(createdAt).slice(0, 10);
    const signedAmount = toNumber(item?.platformBankingAmount) * getDirectionSign(item?.platformBankingDirection);
    dailyMap.set(dateKey, (dailyMap.get(dateKey) ?? 0) + signedAmount);
  });

  return Array.from(dailyMap.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(-30)
    .map(([date, revenue]) => ({
      name: formatDateLabel(date),
      revenue,
    }));
}

function buildDividendAmountByToken(items) {
  const dividendMap = new Map();

  items.forEach((item) => {
    const accountType = String(item?.accountType ?? "").toUpperCase();
    const direction = String(item?.platformBankingDirection ?? "").toUpperCase();
    const tokenId = item?.tokenId;

    if (accountType !== "DIVIDEND" || direction !== "DEPOSIT" || tokenId == null) return;

    dividendMap.set(tokenId, (dividendMap.get(tokenId) ?? 0) + toNumber(item?.platformBankingAmount));
  });

  return dividendMap;
}

function normalizeResponse(data) {
  const holdings = Array.isArray(data?.platformTokenHoldingsDetailList) ? data.platformTokenHoldingsDetailList : [];
  const bankingList = Array.isArray(data?.platformBankingList) ? data.platformBankingList : [];
  const dividendAmountByToken = buildDividendAmountByToken(bankingList);

  const normalizedHoldings = holdings.map((item, index) => {
    const holdingSupply = toNumber(item?.holdingSupply);
    const totalSupply = toNumber(item?.totalSupply);
    const initPrice = toNumber(item?.initPrice);
    const currentPrice = toNumber(item?.currentPrice);
    const tokenId = item?.tokenId;
    const initValue = holdingSupply * initPrice;
    const currentValue = holdingSupply * currentPrice;
    const holdingPercent = totalSupply > 0 ? (holdingSupply / totalSupply) * 100 : 0;
    const mappedDividendAmount = tokenId == null ? 0 : (dividendAmountByToken.get(tokenId) ?? 0);
    const allocationAmount = toNumber(item?.allocationAmount);

    return {
      id: `${item?.tokenSymbol ?? "TOKEN"}-${index}`,
      tokenId,
      tokenName: item?.tokenName ?? "-",
      tokenSymbol: item?.tokenSymbol ?? "-",
      imgUrl: item?.imgUrl ?? "",
      holdingSupply,
      totalSupply,
      circulatingSupply: toNumber(item?.circulatingSupply),
      initPrice,
      currentPrice,
      initValue,
      currentValue,
      holdingPercent,
      dividendAmount: allocationAmount || mappedDividendAmount,
    };
  });

  const totalHoldingSupply = normalizedHoldings.reduce((sum, item) => sum + item.holdingSupply, 0);
  const weightedHoldingPercent = normalizedHoldings.length
    ? normalizedHoldings.reduce((sum, item) => sum + item.holdingPercent * item.holdingSupply, 0) / Math.max(totalHoldingSupply, 1)
    : 0;

  const platformCommRevenue = toNumber(data?.platformCommRevenue);
  const platformAllocationTotalAmount = toNumber(data?.platformAllocationTotalAmount);
  const platformAssetValue = toNumber(data?.platformAssetValue ?? data?.PlatformAssetValue);
  const platformAssetValueCurrent = toNumber(data?.platformAssetValueCurrent ?? data?.PlatformAssetValueCurrent);

  return {
    platformCommRevenue,
    platformAllocationTotalAmount,
    platformAssetValue,
    platformAssetValueCurrent,
    holdings: normalizedHoldings,
    holdingRatioAverage: weightedHoldingPercent,
    chartData: buildChartData(bankingList),
  };
}

export function PlatformRevenue() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [summary, setSummary] = useState({
    platformCommRevenue: 0,
    platformAllocationTotalAmount: 0,
    platformAssetValue: 0,
    platformAssetValueCurrent: 0,
    holdings: [],
    holdingRatioAverage: 0,
    chartData: [],
  });

  useEffect(() => {
    async function loadPlatformProfitAccount() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get("/admin/platformprofitaccount");
        setSummary(normalizeResponse(data));
      } catch (loadError) {
        console.error("[PlatformRevenue] 조회 실패:", loadError);
        setError("플랫폼 수익/보유 현황을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }

    loadPlatformProfitAccount();
  }, []);

  const assetValueDiff = summary.platformAssetValueCurrent - summary.platformAssetValue;
  const assetValueDiffRate = summary.platformAssetValue > 0
    ? (assetValueDiff / summary.platformAssetValue) * 100
    : 0;
  const topDistribution = [...summary.holdings]
    .sort((a, b) => b.holdingPercent - a.holdingPercent)
    .slice(0, 5);

  const stats = [
    {
      label: "누적 수수료 수익",
      value: formatCurrency(summary.platformCommRevenue),
      icon: DollarSign,
      color: "text-stone-600",
      bg: "bg-stone-200",
      trend: null,
    },
    {
      label: "누적 배당 수익",
      value: formatCurrency(summary.platformAllocationTotalAmount),
      icon: Landmark,
      color: "text-brand-blue",
      bg: "bg-brand-blue-light",
      trend: null,
    },
    {
      label: "플랫폼 보유 자산 가치",
      value: formatCurrency(summary.platformAssetValueCurrent),
      icon: Wallet,
      color: "text-brand-red",
      bg: "bg-brand-red-light",
      trend: `${assetValueDiffRate >= 0 ? "+" : ""}${assetValueDiffRate.toFixed(1)}%`,
    },
    {
      label: "평균 보유 지분율",
      value: `${summary.holdingRatioAverage.toFixed(1)}%`,
      icon: PieChart,
      color: "text-brand-red",
      bg: "bg-brand-red-light",
      trend: null,
    },
  ];

  if (loading) {
    return (
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">플랫폼 수익 및 보유 현황</h1>
          <p className="text-sm text-stone-400">플랫폼의 거래 수수료 수익과 직접 보유 중인 토큰 현황을 관리합니다.</p>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white px-6 py-12 text-center text-sm font-medium text-stone-400">
          데이터를 불러오는 중입니다.
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">플랫폼 수익 및 보유 현황</h1>
          <p className="text-sm text-stone-400">플랫폼의 거래 수수료 수익과 직접 보유 중인 토큰 현황을 관리합니다.</p>
        </div>
        <div className="rounded-lg border border-brand-red/20 bg-brand-red-light px-6 py-12 text-center text-sm font-medium text-brand-red-dk">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-stone-800">플랫폼 수익 및 보유 현황</h1>
        <p className="text-sm text-stone-400">플랫폼의 거래 수수료 수익과 직접 보유 중인 토큰 현황을 관리합니다.</p>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-4">
        {stats.map((stat) => {
          const isPositive = stat.trend && stat.trend.startsWith("+");
          const isNegative = stat.trend && stat.trend.startsWith("-");

          return (
            <div key={stat.label} className="rounded-lg border border-stone-200 bg-white p-6">
              <div className="mb-4 flex items-center justify-between">
                <div className={cn("rounded-xl p-3", stat.bg)}>
                  <stat.icon className={cn("h-6 w-6", stat.color)} />
                </div>
                {stat.trend ? (
                  <span
                    className={cn(
                      "flex items-center gap-1 rounded-lg px-2 py-1 text-[10px] font-semibold",
                      isPositive && "bg-brand-green-light text-brand-green",
                      isNegative && "bg-brand-red-light text-brand-red-dk",
                      !isPositive && !isNegative && "bg-stone-100 text-stone-400",
                    )}
                  >
                    {isPositive ? <ArrowUpRight size={10} /> : <ArrowDownRight size={10} />}
                    {stat.trend}
                  </span>
                ) : null}
              </div>
              <p className="mb-1 text-xs font-bold text-stone-400">{stat.label}</p>
              <h3 className="text-xl font-semibold text-stone-800">{stat.value}</h3>
            </div>
          );
        })}
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="rounded-lg border border-stone-200 bg-white p-8 lg:col-span-2">
          <div className="mb-8 flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-sm font-semibold uppercase tracking-widest text-stone-800">
              <BarChart3 size={16} className="text-brand-blue" /> 일자별 수수료 수익 추이
            </h3>
            <span className="rounded-lg bg-stone-100 px-3 py-1 text-[10px] font-black text-stone-500">
              최근 {summary.chartData.length || 0}건
            </span>
          </div>
          <div className="h-[300px] w-full">
            {summary.chartData.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={summary.chartData}>
                  <defs>
                    <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--color-brand-blue)" stopOpacity={0.1} />
                      <stop offset="95%" stopColor="var(--color-brand-blue)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-stone-200)" />
                  <XAxis
                    dataKey="name"
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 10, fontWeight: 700, fill: "var(--color-stone-400)" }}
                    dy={10}
                  />
                  <YAxis
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 10, fontWeight: 700, fill: "var(--color-stone-400)" }}
                    tickFormatter={(value) => `₩${Math.round(value / 10000).toLocaleString("ko-KR")}만`}
                  />
                  <Tooltip
                    formatter={(value) => [formatCurrency(value), "수익"]}
                    contentStyle={{
                      borderRadius: "16px",
                      border: "none",
                      boxShadow: "0 10px 25px -5px rgba(0,0,0,0.1)",
                      padding: "12px",
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="var(--color-brand-blue)"
                    strokeWidth={3}
                    fillOpacity={1}
                    fill="url(#colorRev)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center rounded-lg bg-stone-100 text-sm font-medium text-stone-400">
                표시할 수수료 추이 데이터가 없습니다.
              </div>
            )}
          </div>
        </div>

        <div className="rounded-lg border border-stone-200 bg-white p-8">
          <h3 className="mb-8 flex items-center gap-2 text-sm font-semibold uppercase tracking-widest text-stone-800">
            <Layers size={16} className="text-brand-red" /> 자산별 보유 비중
          </h3>
          <div className="space-y-6">
            {topDistribution.length ? (
              topDistribution.map((item) => (
                <div key={item.id} className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-stone-500">{item.tokenName}</span>
                    <span className="font-bold text-stone-400">{item.holdingPercent.toFixed(2)}%</span>
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-stone-200">
                    <div className="h-full rounded-full bg-brand-red" style={{ width: `${Math.min(item.holdingPercent, 100)}%` }} />
                  </div>
                </div>
              ))
            ) : (
              <div className="rounded-lg bg-stone-100 px-4 py-10 text-center text-sm font-medium text-stone-400">
                보유 비중 데이터가 없습니다.
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="border-b border-stone-200 p-6">
          <div className="flex items-center justify-between gap-4">
            <h3 className="text-lg font-semibold text-stone-800">플랫폼 보유 토큰 상세 내역</h3>
            <div className="text-right">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-stone-400">초기 금액 기준</p>
              <p className="text-sm font-black text-stone-800">{formatCurrency(summary.platformAssetValue)}</p>
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 bg-stone-100">
                {["자산 정보", "보유 수량", "보유 지분율", "초기 평가 금액", "현재 평가 금액", "누적 배당 수익"].map((header) => (
                  <th
                    key={header}
                    className={cn(
                      "px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400",
                      header !== "자산 정보" && "text-right",
                    )}
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {summary.holdings.length ? (
                summary.holdings.map((item) => (
                  <tr key={item.id} className="transition-all hover:bg-stone-100">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        {item.imgUrl ? (
                          <img
                            src={imgSrc(item.imgUrl)}
                            alt={item.tokenName}
                            className="h-10 w-10 rounded-lg border border-stone-200 object-cover"
                          />
                        ) : (
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-stone-200 bg-stone-100 text-xs font-black text-stone-400">
                            {item.tokenSymbol.slice(0, 2)}
                          </div>
                        )}
                        <div>
                          <p className="text-sm font-black text-stone-800">{item.tokenName}</p>
                          <p className="text-[10px] font-mono font-bold text-stone-400">{item.tokenSymbol}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-mono font-bold text-stone-500">
                      {item.holdingSupply.toLocaleString("ko-KR")} ST
                    </td>
                    <td className="px-6 py-4 text-right">
                      <span className="rounded-md bg-stone-200 px-2 py-1 text-[10px] font-semibold text-stone-600">
                        {item.holdingPercent.toFixed(2)}%
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-black text-stone-800">
                      {formatCurrency(item.initValue)}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-bold text-brand-red">
                      {formatCurrency(item.currentValue)}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-bold text-brand-blue">
                      {formatCurrency(item.dividendAmount)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-sm font-medium text-stone-400">
                    플랫폼 보유 토큰 데이터가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
