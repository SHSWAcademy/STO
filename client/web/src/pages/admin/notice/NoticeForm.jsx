import { cn } from "../../../lib/utils.js";
import { NOTICE_TYPE_OPTIONS } from "./noticeUtils.jsx";

export function NoticeForm({ form, setForm, onClose, onSubmit, editingNotice, saving, error }) {
  return (
    <div className="space-y-6 p-8">
      <div className="space-y-1.5">
        <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
          공지 제목
        </label>
        <input
          type="text"
          value={form.noticeTitle}
          onChange={(event) => setForm({ ...form, noticeTitle: event.target.value })}
          placeholder="공지사항 제목을 입력하세요"
          className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none focus:border-brand-blue"
        />
      </div>

      <div className="space-y-1.5">
        <label className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
          공지 타입
        </label>
        <div className="flex gap-2 rounded-xl bg-stone-200 p-1">
          {NOTICE_TYPE_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => setForm({ ...form, noticeType: option.value })}
              className={cn(
                "flex-1 rounded-lg py-2.5 text-sm font-semibold transition-colors",
                form.noticeType === option.value
                  ? "bg-white text-brand-blue shadow-sm"
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
          공지 내용
        </label>
        <textarea
          rows={12}
          value={form.noticeContent}
          onChange={(event) => setForm({ ...form, noticeContent: event.target.value })}
          placeholder="공지사항 내용을 입력하세요"
          className="w-full resize-none rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none focus:border-brand-blue"
        />
      </div>

      {error && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          {error}
        </div>
      )}

      <div className="flex gap-3 border-t border-stone-200 bg-stone-100 p-6 -mx-8 -mb-8">
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
          className="flex-[2] rounded-md bg-brand-blue py-3 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk"
        >
          {saving ? "저장 중..." : editingNotice ? "저장하기" : "등록하기"}
        </button>
      </div>
    </div>
  );
}
