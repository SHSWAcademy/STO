import {
  ChevronLeft,
  ChevronRight,
  Edit3,
  FileText,
  Filter,
  PlusCircle,
  Trash2,
} from "lucide-react";
import { SearchInput } from "../../../components/ui/SearchInput.jsx";
import { Badge } from "../../../components/ui/Badge.jsx";
import { cn } from "../../../lib/utils.js";
import {
  formatDisclosureDate,
  getDisclosureCategoryLabel,
  getDisclosureDeletedLabel,
} from "./disclosureUtils.jsx";

const PAGE_SIZE_OPTIONS = [10, 20, 50];

function PageButton({ children, active, disabled, onClick, ariaLabel }) {
  return (
    <button
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "flex h-9 min-w-9 items-center justify-center rounded-lg border px-3 text-xs font-bold transition-colors",
        active
          ? "border-stone-800 bg-stone-800 text-white"
          : "border-stone-200 bg-white text-stone-500 hover:bg-stone-100",
        disabled && "cursor-not-allowed opacity-40 hover:bg-white",
      )}
    >
      {children}
    </button>
  );
}

export function DisclosureList({
  disclosures,
  disclosureTypeTab,
  loading,
  error,
  searchTerm,
  pageMeta,
  pageSize,
  onTypeTabChange,
  onSearchChange,
  onPageChange,
  onPageSizeChange,
  onAdd,
  onSelect,
  onEdit,
  onDelete,
}) {
  const totalPages = pageMeta?.totalPages ?? 0;
  const currentPage = pageMeta?.number ?? 0;
  const pageNumbers = Array.from(
    { length: Math.min(5, totalPages) },
    (_, index) => Math.max(0, Math.min(currentPage - 2, totalPages - 5)) + index,
  ).filter((pageNumber) => pageNumber < totalPages);

  const totalElements = pageMeta?.totalElements ?? 0;
  const currentSize = pageMeta?.size ?? pageSize;
  const from = totalElements === 0 ? 0 : currentPage * currentSize + 1;
  const to = Math.min((currentPage + 1) * currentSize, totalElements);

  function goToPage(nextPage) {
    if (nextPage < 0 || nextPage >= totalPages || nextPage === currentPage) return;
    onSearchChange("");
    onTypeTabChange("전체");
    onPageChange(nextPage);
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">공시 관리</h1>
          <p className="text-sm text-stone-400">자산별 공시 내역을 관리합니다.</p>
        </div>
        <button
          onClick={onAdd}
          className="flex items-center gap-2 rounded-md bg-brand-blue px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk"
        >
          <PlusCircle className="h-5 w-5" />
          신규 공시 등록
        </button>
      </div>

      <div className="space-y-6">
        <div className="flex w-fit items-center gap-4 rounded-lg border border-stone-200 bg-stone-100 p-2">
          {[
            {
              label: "총 공시 건수",
              value: disclosures.length,
              color: "text-stone-500",
              bg: "bg-stone-200",
            },
            {
              label: "배당 공시",
              value: disclosures.filter(
                (item) => getDisclosureCategoryLabel(item.disclosureCategory) === "배당",
              ).length,
              color: "text-brand-red",
              bg: "bg-stone-200",
            },
            {
              label: "삭제 공시",
              value: disclosures.filter((item) => item.deletedAt).length,
              color: "text-brand-red",
              bg: "bg-brand-red-light",
            },
          ].map((stat) => (
            <div
              key={stat.label}
              className="flex min-w-[200px] items-center gap-4 rounded-lg border border-transparent px-8 py-4 text-left"
            >
              <div className={cn("rounded-xl p-3", stat.bg)}>
                <FileText className={cn("h-5 w-5", stat.color)} />
              </div>
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-stone-400">
                  {stat.label}
                </p>
                <h3 className="text-xl font-semibold text-stone-800">{stat.value}건</h3>
              </div>
            </div>
          ))}
        </div>

        <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
          <div className="space-y-4 border-b border-stone-200 bg-stone-100 px-8 pb-0 pt-6">
            <div className="flex items-center justify-between">
              <SearchInput
                variant="light"
                value={searchTerm}
                onChange={onSearchChange}
                placeholder="현재 페이지에서 공시 제목 검색"
              />
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-xs font-bold text-stone-400">
                  페이지당 표시
                  <select
                    value={pageSize}
                    onChange={(event) => onPageSizeChange(Number(event.target.value))}
                    className="rounded-lg border border-stone-200 bg-white px-3 py-2 text-sm font-semibold text-stone-700 outline-none focus:border-brand-blue"
                  >
                    {PAGE_SIZE_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {option}개
                      </option>
                    ))}
                  </select>
                </label>
                <button className="rounded-md border border-stone-200 bg-white p-3 text-stone-500 transition-all hover:bg-stone-100">
                  <Filter className="h-5 w-5" />
                </button>
              </div>
            </div>
            <div className="flex gap-1">
              {["전체", "배당", "일반"].map((tab) => (
                <button
                  key={tab}
                  onClick={() => onTypeTabChange(tab)}
                  className={cn(
                    "border-b-2 px-5 py-2.5 text-sm font-semibold transition-colors -mb-px",
                    disclosureTypeTab === tab
                      ? tab === "배당"
                        ? "border-brand-red text-brand-red"
                        : "border-brand-blue text-brand-blue"
                      : "border-transparent text-stone-400 hover:text-stone-500",
                  )}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>

          {error && (
            <div className="border-b border-red-100 bg-red-50 px-6 py-4 text-sm text-red-600">
              {error}
            </div>
          )}

          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-stone-200 bg-stone-100">
                  {["공시 ID", "종목", "유형", "제목", "일자", "파일", "삭제 여부", "관리"].map((header) => (
                    <th
                      key={header}
                      className={`px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400 ${
                        ["파일", "삭제 여부", "관리"].includes(header) ? "text-center" : ""
                      }`}
                    >
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-200">
                {loading && (
                  <tr>
                    <td colSpan="8" className="px-6 py-16 text-center text-sm text-stone-400">
                      불러오는 중...
                    </td>
                  </tr>
                )}

                {!loading &&
                  disclosures.map((item) => (
                    <tr
                      key={item.disclosureId}
                      onClick={() => onSelect(item)}
                      className="cursor-pointer transition-all hover:bg-stone-100"
                    >
                      <td className="px-6 py-4 text-sm font-semibold text-stone-500">
                        {item.disclosureId ?? "-"}
                      </td>
                      <td className="px-6 py-4 text-sm font-semibold text-stone-800">
                        {item.assetName || "-"}
                      </td>
                      <td className="px-6 py-4">
                        <Badge
                          variant={
                            getDisclosureCategoryLabel(item.disclosureCategory) === "배당"
                              ? "danger"
                              : "neutral"
                          }
                        >
                          {getDisclosureCategoryLabel(item.disclosureCategory)}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-sm font-bold text-stone-800">
                        {item.disclosureTitle || "-"}
                      </td>
                      <td className="px-6 py-4 font-mono text-sm font-bold text-stone-400">
                        {formatDisclosureDate(item.createdAt)}
                      </td>
                      <td className="px-6 py-4 text-center">
                        {item.originName ? (
                          <span
                            className="inline-flex items-center justify-center rounded-lg p-2 text-brand-red"
                            title={item.originName}
                            aria-label={`첨부 파일: ${item.originName}`}
                          >
                            <FileText className="h-4 w-4 shrink-0 text-brand-red" />
                          </span>
                        ) : (
                          <span className="text-xs text-stone-400">파일 없음</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${
                            item.deletedAt
                              ? "bg-brand-red-light text-brand-red-dk"
                              : "bg-stone-100 text-stone-500"
                          }`}
                        >
                          {getDisclosureDeletedLabel(item.deletedAt)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <div className="flex justify-center gap-2">
                          <button
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              onEdit(item);
                            }}
                            disabled={Boolean(item.deletedAt)}
                            className="rounded-lg p-2 text-brand-blue transition-all hover:bg-stone-100 disabled:opacity-40"
                          >
                            <Edit3 className="h-4 w-4" />
                          </button>
                          <button
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              onDelete(item);
                            }}
                            disabled={Boolean(item.deletedAt)}
                            className="rounded-lg p-2 text-brand-red transition-all hover:bg-brand-red-light disabled:opacity-40"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}

                {!loading && disclosures.length === 0 && (
                  <tr>
                    <td colSpan="8" className="px-6 py-16 text-center text-sm text-stone-400">
                      등록된 공시가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-4 border-t border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-xs font-bold text-stone-400">
              전체 {totalElements.toLocaleString()}개 중 {from.toLocaleString()}-
              {to.toLocaleString()} 표시
            </p>

            <div className="flex flex-wrap items-center gap-2">
              <PageButton
                disabled={pageMeta?.first || loading || totalPages === 0}
                onClick={() => goToPage(currentPage - 1)}
                ariaLabel="이전 페이지"
              >
                <ChevronLeft className="h-4 w-4" />
              </PageButton>

              {pageNumbers.map((pageNumber) => (
                <PageButton
                  key={pageNumber}
                  active={pageNumber === currentPage}
                  disabled={loading}
                  onClick={() => goToPage(pageNumber)}
                  ariaLabel={`${pageNumber + 1} 페이지`}
                >
                  {pageNumber + 1}
                </PageButton>
              ))}

              <PageButton
                disabled={pageMeta?.last || loading || totalPages === 0}
                onClick={() => goToPage(currentPage + 1)}
                ariaLabel="다음 페이지"
              >
                <ChevronRight className="h-4 w-4" />
              </PageButton>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
