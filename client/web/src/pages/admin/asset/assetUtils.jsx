import { cn } from "../../../lib/utils.js";

export const IMG_BASE = "http://localhost:8080/upload";

export function imgSrc(filename) {
  if (!filename) return null;
  return `${IMG_BASE}/${filename}`;
}

export const STATUS_LABEL = {
  ACTIVE:   { label: "상장",     className: "bg-green-100 text-green-600" },
  LISTED:   { label: "상장",     className: "bg-green-100 text-green-600" },
  PENDING:  { label: "심사중",   className: "bg-amber-100 text-amber-600" },
  INACTIVE: { label: "비활성",   className: "bg-stone-100 text-stone-400" },
  DELISTED: { label: "상장폐지", className: "bg-red-100 text-red-500" },
};

export function StatusBadge({ status }) {
  const si = STATUS_LABEL[status] ?? { label: status ?? "-", className: "bg-stone-100 text-stone-400" };
  return (
    <span className={cn("px-2 py-0.5 rounded text-[10px] font-semibold uppercase inline-block", si.className)}>
      {si.label}
    </span>
  );
}
