import { FileText } from "lucide-react";
import { cn } from "../../../lib/utils.js";

const CATEGORY_OPTIONS = [
  { value: "BUILDING", label: "일반" },
  { value: "DIVIDEND", label: "배당" },
  { value: "ETC", label: "일반" },
];

export function DisclosureForm({
  mode,
  form,
  setForm,
  assets = [],
  onClose,
  onSubmit,
  saving = false,
  error = "",
}) {
  return (
    <>
      <div className="space-y-5 p-6">
        {mode === "create" ? (
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              공시 대상 자산
            </label>
            <select
              value={form.assetId ?? ""}
              onChange={(event) => {
                const nextAsset = assets.find(
                  (asset) => String(asset.assetId) === String(event.target.value),
                );
                setForm({
                  ...form,
                  assetId: event.target.value,
                  assetName: nextAsset?.assetName ?? "",
                });
              }}
            className="w-full appearance-none rounded-xl border border-stone-200 bg-stone-100 px-4 py-2.5 text-sm font-bold text-stone-800 outline-none focus:border-brand-blue"
            >
              <option value="">자산을 선택하세요</option>
              {assets.map((asset) => (
                <option key={asset.assetId} value={asset.assetId}>
                  {asset.assetName}
                </option>
              ))}
            </select>
          </div>
        ) : (
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              공시 대상 자산
            </label>
            <input
              type="text"
              readOnly
              value={form.assetName ?? ""}
              className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-2.5 text-sm font-bold text-stone-400 outline-none"
            />
          </div>
        )}

        <div className="space-y-1.5">
          <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            공시 유형
          </label>
          <div className="flex gap-2 rounded-xl bg-stone-200 p-1">
            {CATEGORY_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setForm({ ...form, disclosureCategory: option.value })}
                className={cn(
                  "flex-1 rounded-lg py-2 text-sm font-semibold transition-colors",
                  form.disclosureCategory === option.value
                    ? option.value === "DIVIDEND"
                      ? "bg-white text-brand-red shadow-sm"
                      : "bg-white text-brand-blue shadow-sm"
                    : "text-stone-400 hover:text-stone-500",
                )}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            공시 제목
          </label>
          <input
            type="text"
            value={form.disclosureTitle}
            onChange={(event) => setForm({ ...form, disclosureTitle: event.target.value })}
            placeholder="공시 제목을 입력하세요"
            className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-2.5 text-sm font-bold text-stone-800 outline-none focus:border-brand-blue"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            공시 본문
          </label>
          <textarea
            rows={7}
            value={form.disclosureContent}
            onChange={(event) => setForm({ ...form, disclosureContent: event.target.value })}
            placeholder="공시 본문을 입력하세요"
            className="w-full resize-none rounded-xl border border-stone-200 bg-stone-100 px-4 py-2.5 text-sm font-medium text-stone-800 outline-none focus:border-brand-blue"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
            첨부 파일
          </label>
          <div className="space-y-3 rounded-xl border border-stone-200 bg-stone-50 p-3">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-brand-red-light p-2 text-brand-red-dk">
                <FileText className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-stone-700">
                  {form.file?.name ?? form.originName ?? "첨부 파일 없음"}
                </p>
                {form.storedName && !form.file && (
                  <p className="truncate text-xs text-stone-400">{form.storedName}</p>
                )}
              </div>
            </div>
            <input
              type="file"
              accept="application/pdf"
              onChange={(event) =>
                setForm({
                  ...form,
                  file: event.target.files?.[0] ?? null,
                })
              }
              className="w-full rounded-xl border border-stone-200 bg-white px-4 py-2.5 text-sm font-medium text-stone-800 file:mr-4 file:rounded-md file:border-0 file:bg-stone-800 file:px-3 file:py-2 file:text-xs file:font-semibold file:text-white"
            />
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        )}
      </div>

      <div className="flex gap-3 border-t border-stone-200 bg-stone-100 p-5">
        <button
          onClick={onClose}
          disabled={saving}
          className="flex-1 rounded-md border border-stone-200 bg-white py-3 text-sm font-medium text-stone-400 transition-colors hover:bg-stone-200"
        >
          취소
        </button>
        <button
          onClick={onSubmit}
          disabled={saving}
          className="flex-[2] rounded-md bg-brand-blue py-3 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk disabled:opacity-60"
        >
          {saving ? "저장 중..." : mode === "edit" ? "저장하기" : "등록하기"}
        </button>
      </div>
    </>
  );
}
