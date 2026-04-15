import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Heart } from "lucide-react";
import { ResponsiveContainer, LineChart, Line, Tooltip } from "recharts";
import { useApp } from "../context/AppContext.jsx";
import { cn } from "../lib/utils.js";
import { API_BASE_URL } from "../lib/config.js";
import { TabSwitcher } from "../components/ui/TabSwitcher.jsx";
import { AssetAvatar } from "../components/ui/AssetAvatar.jsx";
import { useDashboardSocket } from "../hooks/useDashboardSocket.js";

const API = API_BASE_URL;
const PAGE_SIZE = 10;

const SORT_ITEMS = ["전체", "거래대금", "거래량"];
const RANGE_ITEMS = ["1일", "1개월", "1년"];

const SELECT_TYPE_MAP = {
  전체: "BASIC",
  거래대금: "TOTAL_TRADE_VALUE",
  거래량: "TOTAL_TRADE_QUANTITY",
};

const PERIOD_TYPE_MAP = {
  "1일": "DAY",
  "1개월": "MONTH",
  "1년": "YEAR",
};

function recalculateFluctuationRate(currentPrice, basePrice) {
  if (!basePrice || basePrice <= 0) return 0;
  return Math.round((((currentPrice - basePrice) / basePrice) * 100) * 100) / 100;
}

function updateSparkLine(prevSparkLine, candle) {
  if (candle?.closePrice == null) return prevSparkLine ?? [];

  const nextLine = Array.isArray(prevSparkLine) ? [...prevSparkLine] : [];
  if (nextLine.length === 0) {
    return [{ value: candle.closePrice, date: '' }];
  }

  nextLine[nextLine.length - 1] = {
    ...nextLine[nextLine.length - 1],
    value: candle.closePrice,
  };
  return nextLine.slice(-7);
}

export function DashboardPage() {
  const navigate = useNavigate();
  const { likedTokenIds, toggleLike, user, showGuestBanner } = useApp();

  const [chartFilter, setChartFilter] = useState("전체");
  const [timeRange, setTimeRange] = useState("1일");
  const [page, setPage] = useState(0);
  const [tokens, setTokens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [previewTokenId, setPreviewTokenId] = useState(null);

  const periodType = PERIOD_TYPE_MAP[timeRange];
  const tokenIds = useMemo(() => tokens.map((token) => token.tokenId), [tokens]);

  useEffect(() => {
    setPage(0);
  }, [chartFilter, timeRange]);

  useEffect(() => {
    const selectType = SELECT_TYPE_MAP[chartFilter];

    setLoading(true);

    const headers = {};
    if (user?.accessToken) {
      headers.Authorization = `Bearer ${user.accessToken}`;
    }

    fetch(`${API}/api/token?page=${page}&selectType=${selectType}&periodType=${periodType}`, { headers })
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((data) => {
        const nextTokens = Array.isArray(data) ? data : [];
        setTokens(nextTokens);
        setHasNext(nextTokens.length === PAGE_SIZE);
        setPreviewTokenId((prev) => {
          if (!nextTokens.length) return null;
          const hasCurrent = prev != null && nextTokens.some((token) => token.tokenId === prev);
          return hasCurrent ? prev : nextTokens[0].tokenId;
        });
      })
      .catch((error) => console.warn("[Dashboard] token list fetch failed:", error))
      .finally(() => setLoading(false));
  }, [page, chartFilter, periodType, user]);

  useDashboardSocket({
    tokenIds,
    candleType: periodType,
    token: user?.accessToken,
    onTrade: ({ tokenId, trade }) => {
      setTokens((prev) =>
        prev.map((token) => {
          if (token.tokenId !== tokenId) return token;

          const currentPrice = trade.tradePrice ?? token.currentPrice ?? 0;
          const tradeQuantity = trade.tradeQuantity ?? 0;
          const tradeAmount = currentPrice * tradeQuantity;

          return {
            ...token,
            currentPrice,
            totalTradeValue: (token.totalTradeValue ?? 0) + tradeAmount,
            totalTradeQuantity: (token.totalTradeQuantity ?? 0) + tradeQuantity,
            fluctuationRate: recalculateFluctuationRate(currentPrice, token.basePrice),
          };
        }),
      );
    },
    onCandle: ({ tokenId, candle }) => {
      setTokens((prev) =>
        prev.map((token) => {
          if (token.tokenId !== tokenId) return token;

          return {
            ...token,
            sparkLine: updateSparkLine(token.sparkLine, candle),
          };
        }),
      );
    },
  });

  const previewToken = useMemo(
    () => tokens.find((token) => token.tokenId === previewTokenId) ?? null,
    [tokens, previewTokenId],
  );

  const sparklineData = previewToken?.sparkLine?.map((p) => ({ value: p.value, date: p.date })) ?? [];

  return (
    <div className="mx-auto max-w-[1200px] space-y-6">
      <div className="flex flex-col gap-8 pt-4 lg:flex-row">
        <div className="min-w-0 flex-1 space-y-6">
          <h2 className="text-xl font-black text-stone-800">차트</h2>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-stone-200 pb-4">
              <TabSwitcher items={SORT_ITEMS} active={chartFilter} onChange={setChartFilter} />
              <TabSwitcher items={RANGE_ITEMS} active={timeRange} onChange={setTimeRange} />
            </div>

            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-stone-200 text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  <th className="py-4 text-left font-medium">종목</th>
                  <th className="py-4 text-right font-medium">현재가</th>
                  <th className="py-4 text-right font-medium">등락률</th>
                  <th className="py-4 text-right font-medium">거래대금</th>
                  <th className="py-4 text-right font-medium">거래량</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-200">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-stone-400">
                      데이터를 불러오는 중입니다.
                    </td>
                  </tr>
                ) : tokens.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-sm text-stone-400">
                      표시할 종목이 없습니다.
                    </td>
                  </tr>
                ) : (
                  tokens.map((token, index) => (
                    <tr
                      key={token.tokenId}
                      className={cn(
                        "group cursor-pointer border-y border-transparent transition-all duration-200 hover:border-stone-200 hover:bg-white hover:shadow-[0_10px_30px_rgba(28,25,23,0.06)]",
                        previewTokenId === token.tokenId && "border-stone-200 bg-white shadow-[0_12px_32px_rgba(28,25,23,0.08)]",
                      )}
                      onMouseEnter={() => setPreviewTokenId(token.tokenId)}
                      onClick={() => navigate(`/token/${token.tokenId}`)}
                    >
                      <td className="py-0">
                        <div className="flex min-h-[72px] items-stretch">
                          <div
                            className="flex w-20 shrink-0 items-center justify-center"
                            onMouseDown={(event) => event.stopPropagation()}
                            onClick={(event) => event.stopPropagation()}
                          >
                            <button
                              onClick={async (event) => {
                                event.stopPropagation();
                                if (!user) {
                                  showGuestBanner("관심 종목, 내 계좌 등 개인 기능은 로그인 후 이용할 수 있어요.");
                                  return;
                                }

                                try {
                                  await toggleLike(token.tokenId);
                                } catch (error) {
                                  console.error("[Dashboard] like toggle failed:", error);
                                }
                              }}
                              className={cn(
                                "flex h-10 w-10 items-center justify-center rounded-full transition-colors",
                                likedTokenIds.includes(token.tokenId)
                                  ? "bg-brand-red-light/70 text-brand-red"
                                  : "text-stone-400 hover:bg-stone-200 hover:text-brand-red",
                              )}
                            >
                              <Heart
                                size={16}
                                fill={likedTokenIds.includes(token.tokenId) ? "currentColor" : "none"}
                              />
                            </button>
                          </div>

                          <div className="flex min-w-0 items-center gap-4 py-4">
                            <span className="w-4 font-mono text-stone-400">{page * PAGE_SIZE + index + 1}</span>
                            <AssetAvatar
                              symbol={token.tokenSymbol}
                              src={token.imgUrl}
                              alt={token.assetName}
                              size="md"
                              variant="light"
                              className="shrink-0"
                            />
                            <div className="min-w-0">
                              <p className="truncate font-bold text-stone-800 transition-colors group-hover:text-stone-900">
                                {token.assetName}
                              </p>
                              <p className="mt-0.5 truncate font-mono text-[11px] font-bold text-stone-400">
                                {token.tokenSymbol || "-"}
                              </p>
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 text-right font-mono font-bold text-stone-800">
                        {(token.currentPrice ?? 0).toLocaleString()}원
                      </td>
                      <td
                        className={cn(
                          "py-4 text-right font-bold",
                          (token.fluctuationRate ?? 0) >= 0 ? "text-brand-red" : "text-brand-blue",
                        )}
                      >
                        {(token.fluctuationRate ?? 0) >= 0 ? "+" : ""}
                        {token.fluctuationRate ?? 0}%
                      </td>
                      <td className="py-4 text-right font-mono text-stone-500">
                        {(token.totalTradeValue ?? 0).toLocaleString()}원
                      </td>
                      <td className="py-4 text-right font-mono text-stone-500">
                        {(token.totalTradeQuantity ?? 0).toLocaleString()}주
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            <div className="flex items-center justify-center gap-4 pt-2">
              <button
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                disabled={page === 0 || loading}
                className="rounded-lg border border-stone-200 px-4 py-2 text-sm font-bold transition-colors hover:bg-stone-100 disabled:opacity-30"
              >
                이전
              </button>
              <span className="font-mono text-sm text-stone-500">{page + 1}</span>
              <button
                onClick={() => setPage((prev) => prev + 1)}
                disabled={!hasNext || loading}
                className="rounded-lg border border-stone-200 px-4 py-2 text-sm font-bold transition-colors hover:bg-stone-100 disabled:opacity-30"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        <div className="w-full min-w-0 lg:w-[380px]">
          <div className="sticky top-24 rounded-xl border border-stone-200 bg-white p-8 shadow-sm">
            {previewToken ? (
              <>
                <div className="mb-8 flex items-start gap-4">
                  <AssetAvatar
                    symbol={previewToken.tokenSymbol}
                    src={previewToken.imgUrl}
                    alt={previewToken.assetName}
                    size="lg"
                    variant="light"
                    className="shrink-0"
                  />
                  <div className="min-w-0">
                    <h3 className="truncate text-xl font-black text-stone-800">{previewToken.assetName}</h3>
                    <p className="mt-1 font-mono text-xs font-bold text-stone-400">
                      {previewToken.tokenSymbol || "-"}
                    </p>
                    <p className="mt-2 text-sm font-bold">
                      <span className="font-mono text-stone-800">
                        {(previewToken.currentPrice ?? 0).toLocaleString()}원
                      </span>
                      <span
                        className={cn(
                          "ml-2",
                          (previewToken.fluctuationRate ?? 0) >= 0 ? "text-brand-red" : "text-brand-blue",
                        )}
                      >
                        {(previewToken.fluctuationRate ?? 0) >= 0 ? "+" : ""}
                        {previewToken.fluctuationRate ?? 0}%
                      </span>
                    </p>
                  </div>
                </div>

                <div className="mb-8 h-64">
                  <p className="mb-4 text-[10px] font-black uppercase tracking-widest text-stone-400">
                    {timeRange} 차트
                  </p>
                  {sparklineData.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                      <LineChart data={sparklineData}>
                        <Tooltip
                          contentStyle={{
                            backgroundColor: "#ffffff",
                            border: "1px solid #e7e5e4",
                            borderRadius: "8px",
                            fontSize: "10px",
                          }}
                          itemStyle={{ color: "#292524" }}
                          formatter={(value) => [`${value.toLocaleString()}원`, "종가"]}
                          labelFormatter={(_, payload) => payload?.[0]?.payload?.date ?? ''}
                        />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke={(previewToken.fluctuationRate ?? 0) >= 0 ? "var(--color-brand-red)" : "var(--color-brand-blue)"}
                          strokeWidth={3}
                          dot={false}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="flex h-full items-center justify-center text-sm text-stone-300">
                      차트 데이터가 없습니다.
                    </div>
                  )}
                </div>

                <button
                  onClick={() => navigate(`/token/${previewToken.tokenId}`)}
                  className="w-full rounded-lg bg-stone-800 py-4 font-black uppercase tracking-widest text-white transition-colors hover:bg-stone-700"
                >
                  거래하기
                </button>
              </>
            ) : (
              <div className="flex h-64 items-center justify-center text-sm text-stone-300">
                종목을 선택해 주세요.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
