export const NOTICE_TYPE_OPTIONS = [
  { value: "GENERAL", label: "일반" },
  { value: "SYSTEM", label: "시스템" },
];

export function getNoticeTypeLabel(value) {
  return NOTICE_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "-";
}

export function formatNoticeDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString("ko-KR");
}

export function getNoticeDeletedLabel(deletedAt) {
  return deletedAt ? "삭제" : "정상";
}

export function mapNoticeListItem(item) {
  return {
    noticeId: item?.noticeId ?? null,
    noticeType: item?.noticeType ?? "GENERAL",
    noticeTitle: item?.noticeTitle ?? "",
    noticeContent: item?.noticeContent ?? "",
    createdAt: item?.createdAt ?? "",
    deletedAt: item?.deletedAt ?? item?.deleteAt ?? null,
  };
}

export function mapNoticeDetail(item) {
  return {
    noticeType: item?.noticeType ?? "GENERAL",
    noticeTitle: item?.noticeTitle ?? "",
    noticeContent: item?.noticeContent ?? "",
    createdAt: item?.createdAt ?? "",
  };
}

export function buildNoticePayload(form) {
  return {
    noticeType: form.noticeType,
    noticeTitle: form.noticeTitle.trim(),
    noticeContent: form.noticeContent.trim(),
  };
}
