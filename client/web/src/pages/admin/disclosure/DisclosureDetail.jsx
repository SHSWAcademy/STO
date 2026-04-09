import { Download, Edit3, FileText, Trash2 } from "lucide-react";
import { Badge } from "../../../components/ui/Badge.jsx";
import {
  formatDisclosureDate,
  getDisclosureCategoryLabel,
} from "./disclosureUtils.jsx";
import { imgSrc, pdfDownloadUrl, pdfViewUrl } from "../allocation/allocationUtils.jsx";

export function DisclosureDetail({ item, onEdit, onDelete }) {
  if (!item) {
    return <div className="p-8 text-sm text-stone-400">상세 정보를 불러오지 못했습니다.</div>;
  }

  return (
    <div className="space-y-6 p-8">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Badge
              variant={
                getDisclosureCategoryLabel(item.disclosureCategory) === "배당"
                  ? "danger"
                  : "neutral"
              }
            >
              {getDisclosureCategoryLabel(item.disclosureCategory)}
            </Badge>
            <span className="text-xs font-medium text-stone-400">
              공시 ID {item.disclosureId ?? "-"}
            </span>
          </div>
          <h3 className="text-xl font-semibold text-stone-800">{item.disclosureTitle || "-"}</h3>
          <p className="text-sm text-stone-400">
            {item.assetName || "-"} · {formatDisclosureDate(item.createdAt)}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onEdit}
            disabled={Boolean(item.deletedAt)}
            className="inline-flex items-center gap-2 rounded-md bg-brand-blue px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk disabled:opacity-40"
          >
            <Edit3 className="h-4 w-4" />
            수정하기
          </button>
          <button
            type="button"
            onClick={onDelete}
            disabled={Boolean(item.deletedAt)}
            className="inline-flex items-center gap-2 rounded-md bg-brand-red px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-red-dk disabled:opacity-40"
          >
            <Trash2 className="h-4 w-4" />
            삭제하기
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[260px_minmax(0,1fr)]">
        <div className="space-y-4">
          <div className="overflow-hidden rounded-xl border border-stone-200 bg-stone-50">
            {item.imgUrl ? (
              <img
                src={imgSrc(item.imgUrl)}
                alt={item.assetName}
                className="aspect-video w-full object-cover"
              />
            ) : (
              <div className="flex aspect-video items-center justify-center text-sm text-stone-400">
                이미지 없음
              </div>
            )}
          </div>

          <div className="rounded-xl border border-stone-200 bg-stone-50 p-4">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              삭제 여부
            </p>
            <p className="mt-1 text-sm font-semibold text-stone-800">
              {item.deletedAt ? "삭제" : "정상"}
            </p>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-stone-200 bg-stone-50 p-4">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              공시 본문
            </p>
            <div className="mt-3 min-h-56 whitespace-pre-wrap text-sm leading-relaxed text-stone-700">
              {item.disclosureContent || "-"}
            </div>
          </div>

          <div className="rounded-xl border border-stone-200 bg-stone-50 p-4">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-stone-400">
              첨부 파일
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <div className="flex min-w-0 items-center gap-3">
                <div className="rounded-lg bg-brand-red-light p-2 text-brand-red-dk">
                  <FileText className="h-4 w-4" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-stone-700">
                    {item.originName || "첨부 파일 없음"}
                  </p>
                  {item.storedName && (
                    <p className="truncate text-xs text-stone-400">{item.storedName}</p>
                  )}
                </div>
              </div>
              {item.storedName && (
                <div className="flex gap-2">
                  <a
                    href={pdfViewUrl(item.storedName)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="rounded-md border border-stone-200 bg-white px-3 py-2 text-xs font-medium text-stone-700 transition-colors hover:border-brand-blue hover:text-brand-blue"
                  >
                    보기
                  </a>
                  <a
                    href={pdfDownloadUrl(item.storedName)}
                    download={item.originName || item.storedName}
                    className="inline-flex items-center gap-2 rounded-md border border-stone-200 bg-white px-3 py-2 text-xs font-medium text-stone-700 transition-colors hover:border-brand-blue hover:text-brand-blue"
                  >
                    <Download className="h-4 w-4" />
                    다운로드
                  </a>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
