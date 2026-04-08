import { Edit3, Trash2 } from "lucide-react";
import { SearchInput } from "../../../components/ui/SearchInput.jsx";
import { Badge } from "../../../components/ui/Badge.jsx";
import {
  formatNoticeDate,
  getNoticeDeletedLabel,
  getNoticeTypeLabel,
} from "./noticeUtils.jsx";

export function NoticeList({
  notices,
  loading,
  error,
  searchTerm,
  onSearch,
  onAdd,
  onSelect,
  onEdit,
  onDelete,
}) {
  const filteredNotices = notices.filter(
    (notice) =>
      notice.noticeTitle.toLowerCase().includes(searchTerm.toLowerCase()) ||
      getNoticeTypeLabel(notice.noticeType).toLowerCase().includes(searchTerm.toLowerCase()),
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">공지사항 관리</h1>
          <p className="text-sm text-stone-400">플랫폼 공지사항을 등록하고 관리합니다.</p>
        </div>
        <button
          onClick={onAdd}
          className="flex items-center gap-2 rounded-md bg-brand-blue px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk"
        >
          신규 공지 등록
        </button>
      </div>

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="flex items-center justify-between border-b border-stone-200 p-6">
          <SearchInput
            variant="light"
            value={searchTerm}
            onChange={onSearch}
            placeholder="공지 제목 검색..."
          />
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
                {["공지 ID", "타입", "제목", "작성일", "삭제 여부", "관리"].map((header) => (
                  <th
                    key={header}
                    className={`px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400 ${
                      header === "관리" ? "text-center" : ""
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
                  <td colSpan="6" className="px-6 py-16 text-center text-sm text-stone-400">
                    불러오는 중...
                  </td>
                </tr>
              )}
              {!loading && filteredNotices.map((notice) => (
                <tr
                  key={notice.noticeId}
                  onClick={() => onSelect(notice)}
                  className={`group cursor-pointer transition-all hover:bg-stone-100 ${
                    notice.deletedAt ? "bg-stone-50 text-stone-400" : ""
                  }`}
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
                    <p className="text-sm font-semibold text-stone-800">{notice.noticeTitle}</p>
                  </td>
                  <td className="px-6 py-4 text-sm font-bold text-stone-400">{formatNoticeDate(notice.createdAt)}</td>
                  <td className="px-6 py-4 text-sm font-semibold text-stone-500">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${
                        notice.deletedAt
                          ? "bg-brand-red-light text-brand-red-dk"
                          : "bg-stone-100 text-stone-500"
                      }`}
                    >
                      {getNoticeDeletedLabel(notice.deletedAt)}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <div className="flex justify-center gap-2 opacity-0 transition-all group-hover:opacity-100">
                      <button
                        onClick={(event) => {
                          event.stopPropagation();
                          onEdit(notice);
                        }}
                        disabled={Boolean(notice.deletedAt)}
                        className="rounded-lg p-2 text-brand-blue transition-all hover:bg-stone-100"
                      >
                        <Edit3 className="h-4 w-4" />
                      </button>
                      <button
                        onClick={(event) => {
                          event.stopPropagation();
                          onDelete(notice);
                        }}
                        disabled={Boolean(notice.deletedAt)}
                        className="rounded-lg p-2 text-brand-red transition-colors hover:bg-brand-red-light"
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
                    등록된 공지사항이 없습니다.
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
