import { cn } from "../../../lib/utils.js";
import { FILE_URLS } from "../../../lib/config.js";

export const IMG_BASE = FILE_URLS.imageBase;
export const PDF_VIEW_BASE = FILE_URLS.pdfViewBase;
export const PDF_DOWNLOAD_BASE = FILE_URLS.pdfDownloadBase;

export function imgSrc(filename) {
  if (!filename) return null;
  return `${IMG_BASE}/${filename}`;
}

export function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleDateString("ko-KR");
}

export const STATUS_LABEL = {
  ACTIVE:   { label: "상장",     className: "bg-green-100 text-green-600" },
  LISTED:   { label: "상장",     className: "bg-green-100 text-green-600" },
  PENDING:  { label: "심사중",   className: "bg-amber-100 text-amber-600" },
  INACTIVE: { label: "비활성",   className: "bg-stone-100 text-stone-400" },
  DELISTED: { label: "상장폐지", className: "bg-red-100 text-red-500" },
  ISSUED:   { label: "발행완료", className: "bg-blue-100 text-blue-600" },
  TRADING:  { label: "거래 중",  className: "bg-green-100 text-green-600" },
  SUSPENDED:{ label: "거래중단", className: "bg-amber-100 text-amber-600" },
  CLOSED:   { label: "거래완료", className: "bg-stone-200 text-stone-600" },
};

export function StatusBadge({ status }) {
  const si = STATUS_LABEL[status] ?? { label: status ?? "-", className: "bg-stone-100 text-stone-400" };
  return (
    <span className={cn("px-2 py-0.5 rounded text-[10px] font-semibold uppercase inline-block", si.className)}>
      {si.label}
    </span>
  );
}
