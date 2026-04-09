import { Badge } from "../../../components/ui/Badge.jsx";
import {
  formatNoticeDate,
  getNoticeDeletedLabel,
  getNoticeTypeLabel,
} from "./noticeUtils.jsx";

export function NoticeDetail({ notice, loading, error }) {
  if (loading) {
    return <div className="p-8 text-sm text-stone-400">불러오는 중...</div>;
  }

  if (error) {
    return <div className="p-8 text-sm text-red-600">{error}</div>;
  }

  if (!notice) {
    return <div className="p-8 text-sm text-stone-400">상세 정보를 불러오지 못했습니다.</div>;
  }

  return (
    <div className="space-y-6 p-8">
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-1">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">공지 타입</p>
          <Badge variant={notice.noticeType === "SYSTEM" ? "danger" : "warning"}>
            {getNoticeTypeLabel(notice.noticeType)}
          </Badge>
        </div>
        <div className="space-y-1">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">작성일</p>
          <p className="text-sm font-semibold text-stone-800">{formatNoticeDate(notice.createdAt)}</p>
        </div>
        <div className="space-y-1">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">삭제 여부</p>
          <p className="text-sm font-semibold text-stone-800">{getNoticeDeletedLabel(notice.deletedAt)}</p>
        </div>
      </div>

      <div className="space-y-1.5">
        <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">공지 제목</p>
        <div className="rounded-xl border border-stone-200 bg-stone-50 px-4 py-3 text-sm font-semibold text-stone-800">
          {notice.noticeTitle || "-"}
        </div>
      </div>

      <div className="space-y-1.5">
        <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">공지 내용</p>
        <div className="min-h-48 whitespace-pre-wrap rounded-xl border border-stone-200 bg-stone-50 px-4 py-3 text-sm leading-relaxed text-stone-700">
          {notice.noticeContent || "-"}
        </div>
      </div>
    </div>
  );
}
