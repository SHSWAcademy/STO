import { PlusCircle } from "lucide-react";
import {
  AllocationStatusBadge,
  formatDate,
  formatYearMonth,
  formatCurrency,
  getAllocationDisplayStatus,
  imgSrc,
} from "./allocationUtils.jsx";

export function AllocationList({ items, loading, error, onSelect, onNew }) {
  const monthMeta = items[0] ?? {};

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">배당 관리</h1>
          <p className="text-sm text-stone-400">
            자산별 배당 이벤트를 등록하고 정산 내역을 조회합니다.
          </p>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {[
          ["정산월", formatYearMonth(monthMeta.targetMonth)],
          ["관리자 입력 마감일", formatDate(monthMeta.allocateSetMonth)],
          ["상태 기준", "정산월 대비 입력/지급 현황"],
        ].map(([label, value]) => (
          <div
            key={label}
            className="rounded-lg border border-stone-200 bg-white p-6"
          >
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              {label}
            </p>
            <p className="mt-2 text-lg font-semibold text-stone-800">{value}</p>
          </div>
        ))}
      </div>

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="border-b border-stone-200 bg-stone-50 p-6">
          <h3 className="text-lg font-semibold text-stone-800">
            배당 이벤트 현황
          </h3>
        </div>

        {error && (
          <div className="border-b border-amber-200 bg-amber-50 px-6 py-3 text-xs font-medium text-amber-700">
            {error}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 bg-stone-50">
                <th className="px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                  자산 정보
                </th>
                <th className="px-6 py-4 text-right text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                  월 수익
                </th>
                <th className="px-6 py-4 text-center text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                  정산월
                </th>
                <th className="px-6 py-4 text-center text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                  상태
                </th>
                <th className="px-6 py-4 text-center text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                  관리
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {loading ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    등록된 배당 이벤트가 없습니다.
                  </td>
                </tr>
              ) : (
                items.map((item) => {
                  const displayStatus = getAllocationDisplayStatus(item);

                  return (
                    <tr
                      key={`${item.assetId}-${item.tokenSymbol}`}
                      className="cursor-pointer transition-colors hover:bg-stone-50"
                      onClick={() => onSelect(item)}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          {item.imgUrl ? (
                            <img
                              src={imgSrc(item.imgUrl)}
                              alt={item.assetName}
                              className="h-12 w-12 rounded-lg border border-stone-200 object-cover"
                            />
                          ) : (
                            <div className="flex h-12 w-12 items-center justify-center rounded-lg border border-stone-200 bg-stone-100 text-xs font-bold text-stone-400">
                              {(item.tokenSymbol ?? "?").slice(0, 2)}
                            </div>
                          )}
                          <div>
                            <p className="text-sm font-semibold text-stone-800">
                              {item.assetName}
                            </p>
                            <p className="text-[10px] font-mono font-bold text-stone-400">
                              {item.tokenSymbol}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right text-sm font-bold text-stone-800">
                        {formatCurrency(item.monthlyDividendIncome)}
                      </td>
                      <td className="px-6 py-4 text-center text-sm font-medium text-stone-500">
                        {formatYearMonth(item.targetMonth)}
                      </td>
                      <td className="px-6 py-4 text-center">
                        {item.allocationBatchStatus ? (
                          <AllocationStatusBadge
                            value={item.allocationBatchStatus}
                          />
                        ) : (
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${displayStatus.className}`}
                          >
                            {displayStatus.label}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            onSelect(item);
                          }}
                          className="rounded-md border border-stone-200 bg-white px-4 py-2 text-xs font-semibold text-stone-600 transition-colors hover:border-brand-blue hover:text-brand-blue"
                        >
                          상세보기
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
