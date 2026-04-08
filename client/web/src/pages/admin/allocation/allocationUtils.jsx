import { PDF_DOWNLOAD_BASE, PDF_VIEW_BASE, imgSrc } from "../asset/assetUtils.jsx";

export { PDF_DOWNLOAD_BASE, PDF_VIEW_BASE, imgSrc };

export function formatCurrency(value) {
  return `₩${Number(value ?? 0).toLocaleString()}`;
}

export function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("ko-KR");
}

export function formatSettlementLabel(year, month) {
  if (!year || !month) return "-";
  return `${year}.${String(month).padStart(2, "0")}`;
}

export function formatYearMonth(value) {
  if (!value) return "-";
  const normalized = String(value);
  const [year, month] = normalized.split("-");
  if (!year || !month) return normalized;
  return `${year}.${String(month).padStart(2, "0")}`;
}

export function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString("ko-KR");
}

export function getAllocationDisplayStatus(item) {
  const adminDate = item?.allocateSetMonth;
  const targetMonth = item?.targetMonth;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const deadline = adminDate ? new Date(adminDate) : null;
  const deadlineDate = deadline && !Number.isNaN(deadline.getTime())
    ? new Date(deadline.getFullYear(), deadline.getMonth(), deadline.getDate())
    : null;

  if (item?.allocationBatchStatus) {
    return {
      label: "지급 완료",
      className: "bg-brand-green-light text-brand-green",
    };
  }

  if (item?.monthlyDividendIncome) {
    return {
      label: targetMonth ? "등록 완료" : "등록 완료",
      className: "bg-brand-blue-light text-brand-blue",
    };
  }

  if (deadlineDate && today > deadlineDate) {
    return {
      label: "입력 마감",
      className: "bg-brand-red-light text-brand-red-dk",
    };
  }

  return {
    label: "미등록",
    className: "bg-stone-100 text-stone-500",
  };
}

export function AllocationStatusBadge({ value }) {
  const active = Boolean(value);
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${
        active
          ? "bg-brand-green-light text-brand-green"
          : "bg-stone-100 text-stone-500"
      }`}
    >
      {active ? "지급 완료" : "미지급"}
    </span>
  );
}
