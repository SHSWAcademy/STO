import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Heart } from "lucide-react";
import { useApp } from "../context/AppContext.jsx";
import { cn } from "../lib/utils.js";
import { ResponsiveContainer, LineChart, Line, Tooltip } from "recharts";
import { TabSwitcher } from "../components/ui/TabSwitcher.jsx";

const API = 'http://localhost:8080';

const SELECT_TYPE_MAP = {
  "전체": "BASIC",
  "거래대금": "TOTAL_TRADE_VALUE",
  "거래량": "TOTAL_TRADE_QUANTITY",
};

const PERIOD_TYPE_MAP = {
  "1일": "DAY",
  "1개월": "MONTH",
  "1년": "YEAR",
};

export function DashboardPage() {
  const navigate = useNavigate();
  const { watchlist, toggleWatchlist, user } = useApp();

  const [chartFilter, setChartFilter] = useState("전체");
  const [timeRange, setTimeRange]     = useState("1일");
  const [page, setPage]               = useState(0);

  const [tokens, setTokens]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);

  const [previewToken, setPreviewToken] = useState(null);

  // 탭/필터 변경 시 페이지 초기화
  useEffect(() => { setPage(0); }, [chartFilter, timeRange]);

  // 백엔드 조회
  useEffect(() => {
    const selectType = SELECT_TYPE_MAP[chartFilter];
    const periodType = PERIOD_TYPE_MAP[timeRange];

    setLoading(true);

    const headers = {};
    if (user?.accessToken) headers['Authorization'] = `Bearer ${user.accessToken}`;

    fetch(`${API}/api/token?page=${page}&selectType=${selectType}&periodType=${periodType}`, { headers })
      .then(r => r.ok ? r.json() : Promise.reject(r.status))
      .then(data => {
        setTokens(data);
        setHasNext(data.length === 10);
        setPreviewToken(prev => {
          if (!data.length) return null;
          const found = prev ? data.find(t => t.tokenId === prev.tokenId) : null;
          return found ?? data[0];
        });
      })
      .catch(e => console.warn('[Dashboard] 토큰 목록 조회 실패:', e))
      .finally(() => setLoading(false));
  }, [page, chartFilter, timeRange, user]);

  const sparklineData = previewToken?.sparkLine?.map(v => ({ v })) ?? [];

  return (
    <div className="space-y-6 max-w-[1200px] mx-auto">
      <div className="flex flex-col lg:flex-row gap-8 pt-4">

        {/* 좌: 차트 테이블 */}
        <div className="flex-1 space-y-6">
          <h2 className="text-xl font-black text-stone-800">차트</h2>

          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-stone-200 pb-4">
              <TabSwitcher
                items={["전체", "거래대금", "거래량"]}
                active={chartFilter}
                onChange={setChartFilter}
              />
              <TabSwitcher
                items={["1일", "1개월", "1년"]}
                active={timeRange}
                onChange={setTimeRange}
              />
            </div>

            <table className="w-full text-sm">
              <thead>
                <tr className="text-stone-400 text-[11px] font-medium uppercase tracking-wide border-b border-stone-200">
                  <th className="text-left py-4 font-medium">종목</th>
                  <th className="text-right py-4 font-medium">현재가</th>
                  <th className="text-right py-4 font-medium">등락률</th>
                  <th className="text-right py-4 font-medium">거래대금</th>
                  <th className="text-right py-4 font-medium">거래량</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-200">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-stone-400 text-sm">
                      불러오는 중...
                    </td>
                  </tr>
                ) : tokens.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-10 text-center text-stone-400 text-sm">
                      데이터가 없습니다.
                    </td>
                  </tr>
                ) : (
                  tokens.map((t, i) => (
                    <tr
                      key={t.tokenId}
                      className={cn(
                        "group hover:bg-stone-100 transition-colors cursor-pointer",
                        previewToken?.tokenId === t.tokenId && "bg-stone-100",
                      )}
                      onMouseEnter={() => setPreviewToken(t)}
                      onClick={() => navigate(`/token/${t.tokenId}`)}
                    >
                      <td className="py-4">
                        <div className="flex items-center gap-4">
                          <button
                            onClick={e => {
                              e.stopPropagation();
                              toggleWatchlist(t.tokenId);
                            }}
                            className={cn(
                              "transition-colors",
                              watchlist.includes(t.tokenId)
                                ? "text-brand-red"
                                : "text-stone-400 hover:text-brand-red",
                            )}
                          >
                            <Heart
                              size={16}
                              fill={watchlist.includes(t.tokenId) ? "currentColor" : "none"}
                            />
                          </button>
                          <span className="text-stone-400 font-mono w-4">
                            {page * 10 + i + 1}
                          </span>
                          <p className="font-bold text-stone-800 group-hover:text-stone-600 transition-colors">
                            {t.assetName}
                          </p>
                        </div>
                      </td>
                      <td className="py-4 text-right font-mono font-bold text-stone-800">
                        {t.currentPrice.toLocaleString()}원
                      </td>
                      <td className={cn(
                        "py-4 text-right font-bold",
                        t.fluctuationRate >= 0 ? "text-brand-red" : "text-brand-blue",
                      )}>
                        {t.fluctuationRate >= 0 ? "+" : ""}{t.fluctuationRate}%
                      </td>
                      <td className="py-4 text-right text-stone-500 font-mono">
                        {t.totalTradeValue.toLocaleString()}원
                      </td>
                      <td className="py-4 text-right text-stone-500 font-mono">
                        {t.totalTradeQuantity.toLocaleString()}주
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            {/* 페이징 */}
            <div className="flex items-center justify-center gap-4 pt-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
                className="px-4 py-2 text-sm font-bold rounded-lg border border-stone-200 disabled:opacity-30 hover:bg-stone-100 transition-colors"
              >
                이전
              </button>
              <span className="text-sm text-stone-500 font-mono">{page + 1}</span>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={!hasNext || loading}
                className="px-4 py-2 text-sm font-bold rounded-lg border border-stone-200 disabled:opacity-30 hover:bg-stone-100 transition-colors"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        {/* 우: 종목 미리보기 */}
        <div className="w-full lg:w-[380px]">
          <div className="bg-white rounded-xl border border-stone-200 p-8 sticky top-24 shadow-sm">
            {previewToken ? (
              <>
                <div className="mb-8">
                  <h3 className="text-xl font-black text-stone-800">
                    {previewToken.assetName}
                  </h3>
                  <p className="text-sm font-bold mt-1">
                    <span className="text-stone-800 font-mono">
                      {previewToken.currentPrice.toLocaleString()}원
                    </span>
                    <span className={cn(
                      "ml-2",
                      previewToken.fluctuationRate >= 0 ? "text-brand-red" : "text-brand-blue",
                    )}>
                      {previewToken.fluctuationRate >= 0 ? "+" : ""}{previewToken.fluctuationRate}%
                    </span>
                  </p>
                </div>

                <div className="h-64 mb-8">
                  <p className="text-[10px] font-black text-stone-400 uppercase tracking-widest mb-4">
                    {timeRange} 차트
                  </p>
                  {sparklineData.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={sparklineData}>
                        <Tooltip
                          contentStyle={{
                            backgroundColor: "#ffffff",
                            border: "1px solid #e7e5e4",
                            borderRadius: "8px",
                            fontSize: "10px",
                          }}
                          itemStyle={{ color: "#292524" }}
                          formatter={v => [`${v.toLocaleString()}원`, '종가']}
                        />
                        <Line
                          type="monotone"
                          dataKey="v"
                          stroke={previewToken.fluctuationRate >= 0
                            ? "var(--color-brand-red)"
                            : "var(--color-brand-blue)"}
                          strokeWidth={3}
                          dot={false}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-stone-300 text-sm">
                      차트 데이터 없음
                    </div>
                  )}
                </div>

                <button
                  onClick={() => navigate(`/token/${previewToken.tokenId}`)}
                  className="w-full py-4 rounded-lg bg-stone-800 text-white font-black uppercase tracking-widest hover:bg-stone-700 transition-colors"
                >
                  거래하기
                </button>
              </>
            ) : (
              <div className="h-64 flex items-center justify-center text-stone-300 text-sm">
                종목을 선택해주세요
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
