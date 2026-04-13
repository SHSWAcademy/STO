import { ChevronLeft, ChevronRight, Edit3, Trash2 } from "lucide-react";
import { SearchInput } from "../../../components/ui/SearchInput.jsx";
import { Badge } from "../../../components/ui/Badge.jsx";
import { cn } from "../../../lib/utils.js";
import {
  formatNoticeDate,
  getNoticeDeletedLabel,
  getNoticeTypeLabel,
} from "./noticeUtils.jsx";

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

export function NoticeList({
  notices,
  loading,
  error,
  searchTerm,
  pageMeta,
  pageSize,
  onSearch,
  onPageChange,
  onPageSizeChange,
  onAdd,
  onSelect,
  onEdit,
  onDelete,
}) {
  const filteredNotices = notices.filter((notice) => {
    const keyword = searchTerm.toLowerCase();

    return (
      notice.noticeTitle.toLowerCase().includes(keyword) ||
      getNoticeTypeLabel(notice.noticeType).toLowerCase().includes(keyword)
    );
  });

  const totalPages = pageMeta?.totalPages ?? 0;
  const currentPage = pageMeta?.number ?? 0;
  const pageNumbers = Array.from(
    {
      length: Math.min(5, totalPages),
    },
    (_, index) => Math.max(0, Math.min(currentPage - 2, totalPages - 5)) + index,
  ).filter((pageNumber) => pageNumber < totalPages);

  const totalElements = pageMeta?.totalElements ?? 0;
  const from = totalElements === 0 ? 0 : currentPage * (pageMeta?.size ?? pageSize) + 1;
  const to = Math.min((currentPage + 1) * (pageMeta?.size ?? pageSize), totalElements);

  function goToPage(nextPage) {
    if (nextPage < 0 || nextPage >= totalPages || nextPage === currentPage) return;
    onSearch("");
    onPageChange(nextPage);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">공지사항 관리</h1>
          <p className="text-sm text-stone-400">
            플랫폼 공지사항을 등록하고 관리합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={onAdd}
          className="flex items-center justify-center gap-2 rounded-md bg-brand-blue px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk"
        >
          신규 공지 등록
        </button>
      </div>

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="flex flex-col gap-3 border-b border-stone-200 p-6 sm:flex-row sm:items-center sm:justify-between">
          <SearchInput
            variant="light"
            value={searchTerm}
            onChange={onSearch}
            placeholder="현재 페이지에서 제목, 유형 검색"
            className="min-w-[260px]"
          />

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
                {["공지 ID", "유형", "제목", "작성일", "삭제 여부", "관리"].map((header) => (
                  <th
                    key={header}
                    className={cn(
                      "px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400",
                      header === "관리" && "text-center",
                    )}
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {loading && (
                <tr>
                  <td colSpan="6" className="px-6 py-16 text-center text-sm text-stone-400">
                    불러오는 중...
                  </td>
                </tr>
              )}

              {!loading &&
                filteredNotices.map((notice) => (
                  <tr
                    key={notice.noticeId}
                    onClick={() => onSelect(notice)}
                    className={cn(
                      "group cursor-pointer transition-colors hover:bg-stone-100",
                      notice.deletedAt && "bg-stone-50 text-stone-400",
                    )}
                  >
                    <td className="px-6 py-4 text-sm font-semibold text-stone-500">
                      {notice.noticeId ?? "-"}
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={notice.noticeType === "SYSTEM" ? "danger" : "warning"}>
                        {getNoticeTypeLabel(notice.noticeType)}
                      </Badge>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm font-semibold text-stone-800">
                        {notice.noticeTitle || "-"}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-sm font-bold text-stone-400">
                      {formatNoticeDate(notice.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-stone-500">
                      <span
                        className={cn(
                          "inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider",
                          notice.deletedAt
                            ? "bg-brand-red-light text-brand-red-dk"
                            : "bg-stone-100 text-stone-500",
                        )}
                      >
                        {getNoticeDeletedLabel(notice.deletedAt)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="flex justify-center gap-2 opacity-0 transition-all group-hover:opacity-100">
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            onEdit(notice);
                          }}
                          disabled={Boolean(notice.deletedAt)}
                          className="rounded-lg p-2 text-brand-blue transition-all hover:bg-stone-100 disabled:opacity-40"
                        >
                          <Edit3 className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            onDelete(notice);
                          }}
                          disabled={Boolean(notice.deletedAt)}
                          className="rounded-lg p-2 text-brand-red transition-colors hover:bg-brand-red-light disabled:opacity-40"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}

              {!loading && filteredNotices.length === 0 && (
                <tr>
                  <td colSpan="6" className="px-6 py-16 text-center text-sm text-stone-400">
                    표시할 공지사항이 없습니다.
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
  );
}
