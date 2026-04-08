import { useEffect, useMemo, useState } from "react";
import { FileText, Upload } from "lucide-react";
import { formatDate, formatYearMonth, formatSettlementLabel } from "./allocationUtils.jsx";

function Field({ label, children }) {
  return (
    <div className="space-y-1.5">
      <label className="block text-[10px] font-semibold uppercase tracking-widest text-stone-400">
        {label}
      </label>
      {children}
    </div>
  );
}

function TextInput({ value, onChange, placeholder, readOnly = false }) {
  return (
    <input
      type="text"
      value={value}
      placeholder={placeholder}
      readOnly={readOnly}
      onChange={onChange}
      className={
        readOnly
          ? "w-full cursor-not-allowed rounded-md border border-stone-200 bg-stone-200 px-4 py-3 text-sm font-medium text-stone-500 outline-none"
          : "w-full rounded-md border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-medium text-stone-800 outline-none transition-colors focus:border-brand-blue"
      }
    />
  );
}

export function AllocationForm({
  item,
  details,
  monthMeta,
  loading,
  saving,
  submitError,
  onSubmit,
}) {
  const [monthlyDividendIncome, setMonthlyDividendIncome] = useState("");
  const [file, setFile] = useState(null);

  const currentAllocation = useMemo(() => {
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
    if (currentAllocation) {
      setMonthlyDividendIncome(String(currentAllocation.monthlyDividendIncome ?? ""));
      setFile(null);
      return;
    }

    setMonthlyDividendIncome(item?.monthlyDividendIncome ? String(item.monthlyDividendIncome) : "");
    setFile(null);
  }, [currentAllocation, item]);

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({
      assetId: item.assetId,
      monthlyDividendIncome,
      file,
      currentAllocation,
    });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6 rounded-lg border border-stone-200 bg-white p-8">
      <div className="border-b border-stone-200 pb-4">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-stone-800">
          {currentAllocation ? "이번 정산월 등록 정보" : "이번 정산월 등록"}
        </h3>
        <p className="mt-2 text-sm text-stone-400">
          {formatYearMonth(monthMeta?.targetMonth)} 정산월 기준 화면입니다.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-md border border-stone-200 bg-stone-50 px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            정산월
          </p>
          <p className="mt-1 text-sm font-semibold text-stone-800">
            {formatYearMonth(monthMeta?.targetMonth)}
          </p>
        </div>
        <div className="rounded-md border border-stone-200 bg-stone-50 px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            관리자 입력 마감일
          </p>
          <p className="mt-1 text-sm font-semibold text-stone-800">
            {formatDate(monthMeta?.allocateSetMonth)}
          </p>
        </div>
      </div>

      <Field label="대상 자산">
        <TextInput
          value={`${item.assetName} (${item.tokenSymbol})`}
          readOnly
        />
      </Field>

      <Field label="월 수익 (KRW)">
        <TextInput
          value={monthlyDividendIncome}
          placeholder="48000000"
          readOnly={Boolean(currentAllocation)}
          onChange={(event) => setMonthlyDividendIncome(event.target.value)}
        />
      </Field>

      <Field label="증빙 자료 PDF">
        {currentAllocation ? (
          <div className="rounded-md border border-stone-200 bg-stone-50 px-4 py-3 text-sm font-medium text-stone-700">
            {currentAllocation.originName ?? "등록된 파일 없음"}
          </div>
        ) : (
          <>
            <label className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed border-stone-300 bg-stone-100 px-4 py-3 text-sm text-stone-600 transition-colors hover:border-brand-blue hover:text-brand-blue">
              <Upload className="h-4 w-4 shrink-0" />
              <span className="truncate">{file?.name ?? "PDF 파일 선택"}</span>
              <input
                type="file"
                accept="application/pdf"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                className="sr-only"
              />
            </label>
            <div className="mt-2 flex items-center gap-2 text-[10px] text-stone-400">
              <FileText className="h-3.5 w-3.5" />
              등록 시 공시 파일로 함께 업로드됩니다.
            </div>
          </>
        )}
      </Field>

      {currentAllocation && (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="rounded-md border border-stone-200 bg-stone-50 px-4 py-3">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              정산월 라벨
            </p>
            <p className="mt-1 text-sm font-semibold text-stone-800">
              {formatSettlementLabel(
                currentAllocation.settlementYear,
                currentAllocation.settlementMonth,
              )}
            </p>
          </div>
          <div className="rounded-md border border-stone-200 bg-stone-50 px-4 py-3">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              등록 상태
            </p>
            <p className="mt-1 text-sm font-semibold text-stone-800">
              이번 정산월 입력이 이미 완료되었습니다.
            </p>
          </div>
        </div>
      )}

      {submitError && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          {submitError}
        </div>
      )}

      <div className="flex gap-4">
        <button
          type="submit"
          disabled={saving || loading || Boolean(currentAllocation)}
          className="w-full rounded-md bg-brand-blue py-4 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk disabled:cursor-not-allowed disabled:opacity-60"
        >
          {currentAllocation ? "이번 정산월 등록 완료" : saving ? "저장 중..." : "이번 정산월 등록"}
        </button>
      </div>
    </form>
  );
}
