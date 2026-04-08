import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Download, Eye } from "lucide-react";
import { AllocationForm } from "./AllocationForm.jsx";
import {
  AllocationStatusBadge,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatSettlementLabel,
  formatYearMonth,
  imgSrc,
  pdfDownloadUrl,
  pdfViewUrl,
} from "./allocationUtils.jsx";

export function AllocationDetail({
  item,
  details,
  loading,
  monthMeta,
  saving,
  submitError,
  onBack,
  onSubmit,
}) {
  const [selectedHistoryId, setSelectedHistoryId] = useState(null);

  const currentMonthAllocation = useMemo(() => {
    const target = monthMeta?.targetMonth;
    if (!target || details.length === 0) return null;
    return (
      details.find((detail) => {
        const ym = `${detail.settlementYear}-${String(detail.settlementMonth).padStart(2, "0")}`;
        return ym === target;
      }) ?? null
    );
  }, [details, monthMeta]);

  useEffect(() => {
    if (currentMonthAllocation?.allocationEventId) {
      setSelectedHistoryId(currentMonthAllocation.allocationEventId);
      return;
    }

    if (details[0]?.allocationEventId) {
      setSelectedHistoryId(details[0].allocationEventId);
      return;
    }

    setSelectedHistoryId(null);
  }, [currentMonthAllocation, details]);

  const selectedHistory =
    details.find((detail) => detail.allocationEventId === selectedHistoryId) ??
    currentMonthAllocation ??
    details[0] ??
    null;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={onBack}
            className="rounded-md border border-stone-200 bg-white p-2 text-stone-400 transition-colors hover:text-stone-800"
          >
            <ArrowRight className="h-5 w-5 rotate-180" />
          </button>
          <div>
            <h2 className="text-xl font-semibold text-stone-800">
              배당 상세 내역
            </h2>
            <p className="text-sm text-stone-400">
              {item.assetName} ({item.tokenSymbol})
            </p>
          </div>
        </div>
        <div className="rounded-md border border-stone-200 bg-white px-5 py-3 text-right">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            현재 정산월
          </p>
          <p className="mt-1 text-sm font-semibold text-stone-800">
            {formatYearMonth(monthMeta?.targetMonth)}
          </p>
          <p className="mt-1 text-xs text-stone-400">
            관리자 입력 마감일 {formatDate(monthMeta?.allocateSetMonth)}
          </p>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-[450px_minmax(0,1fr)]">
        <div className="space-y-8">
          <AllocationForm
            item={item}
            details={details}
            monthMeta={monthMeta}
            selectedHistory={selectedHistory}
            loading={loading}
            saving={saving}
            submitError={submitError}
            onSubmit={onSubmit}
          />
        </div>

        <div className="space-y-8 min-w-0">
          <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
            <div className="border-b border-stone-200 p-6">
              <h3 className="text-lg font-semibold text-stone-800">
                정산 이력
              </h3>
            </div>

            {loading ? (
              <div className="px-6 py-16 text-center text-sm text-stone-400">
                불러오는 중...
              </div>
            ) : details.length === 0 ? (
              <div className="px-6 py-16 text-center text-sm text-stone-400">
                등록된 정산 이력이 없습니다.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-stone-200 bg-stone-50">
                      <th className="px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                        정산월
                      </th>
                      <th className="px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                        정산일
                      </th>
                      <th className="px-6 py-4 text-right text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                        월 수익
                      </th>
                      <th className="px-6 py-4 text-center text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                        상태
                      </th>
                      <th className="px-6 py-4 text-center text-[10px] font-semibold uppercase tracking-wide text-stone-400">
                        증빙자료
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-200">
                    {details.map((detail) => (
                      <tr
                        key={detail.allocationEventId}
                        onClick={() =>
                          setSelectedHistoryId(detail.allocationEventId)
                        }
                        className={`cursor-pointer transition-colors hover:bg-stone-50 ${
                          selectedHistory?.allocationEventId ===
                          detail.allocationEventId
                            ? "bg-brand-blue-light/40"
                            : ""
                        }`}
                      >
                        <td className="px-6 py-4 text-sm font-semibold text-stone-800">
                          {formatSettlementLabel(
                            detail.settlementYear,
                            detail.settlementMonth,
                          )}
                        </td>
                        <td className="px-6 py-4 text-sm text-stone-500">
                          {formatDateTime(detail.settledAt)}
                        </td>
                        <td className="px-6 py-4 text-right text-sm font-bold text-stone-800">
                          {formatCurrency(detail.monthlyDividendIncome)}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <AllocationStatusBadge
                            value={detail.allocationBatchStatus}
                          />
                        </td>
                        <td className="px-6 py-4 text-center">
                          {detail.storedName ? (
                            <div className="flex items-center justify-center gap-2">
                              <span className="max-w-[180px] truncate text-xs font-medium text-stone-600">
                                {detail.originName ?? detail.storedName}
                              </span>
                              <a
                                href={pdfViewUrl(detail.storedName)}
                                target="_blank"
                                rel="noopener noreferrer"
                                onClick={(event) => event.stopPropagation()}
                                className="rounded-md border border-stone-200 bg-white p-2 text-stone-500 transition-colors hover:border-brand-blue hover:text-brand-blue"
                                title={detail.originName ?? "파일 보기"}
                              >
                                <Eye className="h-4 w-4" />
                              </a>
                              <a
                                href={pdfDownloadUrl(detail.storedName)}
                                download={
                                  detail.originName ?? detail.storedName
                                }
                                onClick={(event) => event.stopPropagation()}
                                className="rounded-md border border-stone-200 bg-white p-2 text-stone-500 transition-colors hover:border-brand-blue hover:text-brand-blue"
                                title={detail.originName ?? "파일 다운로드"}
                              >
                                <Download className="h-4 w-4" />
                              </a>
                            </div>
                          ) : (
                            <span className="text-xs text-stone-400">
                              파일 없음
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
