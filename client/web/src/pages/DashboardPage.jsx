import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Heart } from "lucide-react";
import { TREND_DATA } from "../data/mock.js";
import { useApp } from "../context/AppContext.jsx";
import { cn } from "../lib/utils.js";
import { ResponsiveContainer, LineChart, Line, Tooltip } from "recharts";
import { TabSwitcher } from "../components/ui/TabSwitcher.jsx";
import { AssetAvatar } from "../components/ui/AssetAvatar.jsx";

const API = 'http://localhost:8080';

export function DashboardPage() {
  const navigate = useNavigate();
  const { watchlist, toggleWatchlist, tokens } = useApp();
  const [chartFilter, setChartFilter] = useState("전체");
  const [timeRange, setTimeRange] = useState("실시간");

  // 백엔드 자산 목록: { assetId, assetName, tokenSymbol, ... }
  // mock 데이터에 backendId를 병합해서 사용
  const [backendAssets, setBackendAssets] = useState([]);

  useEffect(() => {
    fetch(`${API}/admin/asset`)
      .then(r => r.ok ? r.json() : Promise.reject(r.status))
      .then(data => setBackendAssets(data))
      .catch(e => console.warn('[Dashboard] 자산 목록 조회 실패:', e));
  }, []);

  // mock 토큰에 backendId 병합 (tokenSymbol 기준 매칭)
  const mergedTokens = tokens.map(t => {
    const matched = backendAssets.find(a => a.tokenSymbol === t.symbol);
    return { ...t, assetId: matched?.assetId ?? null };
  });

  const [selectedTokenId, setSelectedTokenId] = useState(tokens[0]?.id ?? null);

  useEffect(() => {
    if (!mergedTokens.length) return;

    const hasSelectedToken = mergedTokens.some((token) => token.id === selectedTokenId);
    if (!hasSelectedToken) {
      setSelectedTokenId(mergedTokens[0].id);
    }
  }, [mergedTokens, selectedTokenId]);

  const selectedToken =
    mergedTokens.find((t) => t.id === selectedTokenId) || mergedTokens[0];
  // 토큰이 하나도 없을 때 렌더링 오류 막기
  if (!selectedToken) {
    return null;
  }

  const sortedTokens = [...mergedTokens]
    .sort((a, b) => {
      if (chartFilter === "거래대금") return b.vol - a.vol;
      if (chartFilter === "거래량") return b.vol / b.price - a.vol / a.price;
      return 0;
    })
    .slice(0, 10);

  const currentTrendData = TREND_DATA[timeRange] || TREND_DATA["실시간"];

  return (
    <div className="space-y-6 max-w-[1200px] mx-auto">
      <div className="flex flex-col lg:flex-row gap-8 pt-4">
        {/* 좌: 실시간 차트 테이블 */}
        <div className="flex-1 space-y-6">
          <h2 className="text-xl font-black text-stone-800">실시간 차트</h2>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-stone-200 pb-4">
              <TabSwitcher
                items={["전체", "거래대금", "거래량"]}
                active={chartFilter}
                onChange={setChartFilter}
              />
              <TabSwitcher
                items={[
                  "실시간",
                  "1일",
                  "1주일",
                  "1개월",
                  "3개월",
                  "6개월",
                  "1년",
                ]}
                active={timeRange}
                onChange={setTimeRange}
              />
            </div>

            <table className="w-full text-sm">
              <thead>
                <tr className="text-stone-400 text-[11px] font-medium uppercase tracking-wide border-b border-stone-200">
                  <th className="text-left py-4 font-medium">
                    순위 · {timeRange} 기준
                  </th>
                  <th className="text-right py-4 font-medium">현재가</th>
                  <th className="text-right py-4 font-medium">등락률</th>
                  <th className="text-right py-4 font-medium">
                    {chartFilter === "거래량" ? "거래량" : "거래대금"}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-200">
                {sortedTokens.map((t, i) => (
                  <tr
                    key={t.id}
                    className={cn(
                      "group hover:bg-stone-100 transition-colors cursor-pointer",
                      selectedTokenId === t.id && "bg-stone-100",
                    )}
                    onClick={() => {
                      setSelectedTokenId(t.id);
                    }}
                  >
                    <td className="py-4">
                      <div className="flex items-center gap-4">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleWatchlist(t.assetId);
                          }}
                          className={cn(
                            "transition-colors",
                            watchlist.includes(t.assetId)
                              ? "text-brand-red"
                              : "text-stone-400 hover:text-brand-red",
                          )}
                        >
                          <Heart
                            size={16}
                            fill={
                              watchlist.includes(t.assetId) ? "currentColor" : "none"
                            }
                          />
                        </button>
                        <span className="text-stone-400 font-mono w-4">
                          {i + 1}
                        </span>
                        <AssetAvatar symbol={t.symbol} size="sm" />
                        <p className="font-bold text-stone-800 group-hover:text-stone-600 transition-colors">
                          {t.name}
                        </p>
                      </div>
                    </td>
                    <td className="py-4 text-right font-mono font-bold text-stone-800">
                      {t.price.toLocaleString()}원
                    </td>
                    <td
                      className={cn(
                        "py-4 text-right font-bold",
                        t.change >= 0 ? "text-brand-red" : "text-brand-blue",
                      )}
                    >
                      {t.change >= 0 ? "+" : ""}
                      {t.change}%
                    </td>
                    <td className="py-4 text-right text-stone-500 font-mono">
                      {chartFilter === "거래량"
                        ? Math.round(t.vol / t.price).toLocaleString() + "주"
                        : (t.vol / 100000000).toFixed(0) + "억원"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 우: 종목 프리뷰 */}
        <div className="w-full lg:w-[380px] space-y-6">
          <div className="bg-white rounded-xl border border-stone-200 p-8 sticky top-24 shadow-sm">
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
                <AssetAvatar symbol={selectedToken.symbol} size="md" />
                <div>
                  <h3 className="text-xl font-black text-stone-800">
                    {selectedToken.name}
                  </h3>
                  <p className="text-sm font-bold mt-1">
                    <span className="text-stone-800 font-mono">
                      {selectedToken.price.toLocaleString()}원
                    </span>
                    <span
                      className={cn(
                        "ml-2",
                        selectedToken.change >= 0
                          ? "text-brand-red"
                          : "text-brand-blue",
                      )}
                    >
                      {selectedToken.change >= 0 ? "+" : ""}
                      {selectedToken.change}%
                    </span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => toggleWatchlist(selectedToken.assetId)}
                className={cn(
                  "p-3 rounded-lg transition-colors border",
                  watchlist.includes(selectedToken.assetId)
                    ? "bg-brand-red-light text-brand-red border-brand-red-light"
                    : "bg-stone-100 text-stone-400 border-stone-200 hover:text-brand-red",
                )}
              >
                <Heart
                  size={20}
                  fill={
                    watchlist.includes(selectedToken.assetId)
                      ? "currentColor"
                      : "none"
                  }
                />
              </button>
            </div>

            <div className="h-64 mb-8">
              <p className="text-[10px] font-black text-stone-400 uppercase tracking-widest mb-4">
                {timeRange} 차트
              </p>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={currentTrendData}>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#ffffff",
                      border: "1px solid #e7e5e4",
                      borderRadius: "8px",
                      fontSize: "10px",
                    }}
                    itemStyle={{ color: "#292524" }}
                  />
                  <Line
                    type="monotone"
                    dataKey="val"
                    stroke={
                      selectedToken.change >= 0
                        ? "var(--color-brand-red)"
                        : "var(--color-brand-blue)"
                    }
                    strokeWidth={3}
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <button
              onClick={() => {
                if (selectedToken?.assetId) {
                  navigate(`/mockup/${selectedToken.assetId}`);
                }
              }}
              className="w-full py-4 rounded-lg bg-stone-800 text-white font-black uppercase tracking-widest hover:bg-stone-700 transition-colors"
            >
              거래하기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
