import { ArrowRight, FileText, Download } from "lucide-react";
import { imgSrc, IMG_BASE, StatusBadge } from "./assetUtils.jsx";

export function AssetDetail({ item, detail, loading, onBack, onEdit }) {
  return (
    <div className="space-y-8">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={onBack}
            className="p-2 rounded-md bg-white border border-stone-200 text-stone-400 hover:text-stone-800 transition-colors"
          >
            <ArrowRight className="rotate-180 w-5 h-5" />
          </button>
          <div>
            <h2 className="text-xl font-semibold text-stone-800">자산 상세 정보</h2>
            <p className="text-sm text-stone-400">
              {item.assetName} ({item.tokenSymbol})
            </p>
          </div>
        </div>
        {detail && (
          <button
            onClick={() => onEdit(detail)}
            className="px-6 py-2.5 bg-brand-blue text-white text-sm font-medium rounded-md hover:bg-brand-blue-dk transition-colors"
          >
            정보 수정하기
          </button>
        )}
      </div>

      {loading && (
        <div className="py-20 text-center text-sm text-stone-400">불러오는 중...</div>
      )}
      {!loading && !detail && (
        <div className="py-20 text-center text-sm text-red-400">상세 정보를 불러오지 못했습니다.</div>
      )}

      {!loading && detail && (
        <div className="grid lg:grid-cols-2 gap-8">
          {/* ── 좌: 자산 기본 정보 */}
          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
              자산 기본 정보
            </h3>

            <div className="w-full aspect-video rounded-lg bg-stone-100 border border-stone-200 overflow-hidden">
              {detail.imgUrl ? (
                <img src={imgSrc(detail.imgUrl)} alt={detail.assetName} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-stone-400 text-sm">이미지 없음</div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-6">
              {[
                ["자산명",      detail.assetName],
                ["자산 주소",   detail.assetAddress ?? "-"],
                ["자산 총 금액", `₩${(detail.totalValue ?? 0).toLocaleString()}`],
                ["총 공급량",   `${(detail.totalSupply ?? 0).toLocaleString()} ST`],
              ].map(([l, v]) => (
                <div key={l} className="space-y-1">
                  <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">{l}</p>
                  <p className="text-sm font-semibold text-stone-800">{v}</p>
                </div>
              ))}
              <div className="space-y-1">
                <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">상태</p>
                <StatusBadge status={detail.tokenStatus} />
              </div>
            </div>

            {detail.originName && (
              <div className="pt-4 border-t border-stone-200">
                <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest mb-2">
                  세부 정보 보고서
                </p>
                <a
                  href={`${IMG_BASE}/${detail.storedName}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-3 p-3 rounded-lg bg-stone-50 border border-stone-200 hover:bg-stone-100 transition-colors group"
                >
                  <div className="p-2 bg-red-50 rounded-md text-red-500">
                    <FileText size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-semibold text-stone-800 truncate">{detail.originName}</p>
                    <p className="text-[10px] text-stone-400">PDF Document</p>
                  </div>
                  <Download size={14} className="text-stone-400 group-hover:text-brand-blue" />
                </a>
              </div>
            )}
          </div>

          {/* ── 우: 토큰 발행 정보 */}
          <div className="space-y-8">
            <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
              <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
                토큰 발행 정보
              </h3>
              <div className="grid grid-cols-2 gap-6">
                {[
                  ["토큰명",       detail.tokenName],
                  ["심볼",        detail.tokenSymbol],
                  ["발행가",       `₩${(detail.initPrice ?? 0).toLocaleString()}`],
                  ["현재가",       `₩${Number(detail.currentPrice ?? 0).toLocaleString()}`],
                  ["총 공급량",    `${(detail.totalSupply ?? 0).toLocaleString()} ST`],
                  ["유통량",       `${(detail.circulatingSupply ?? 0).toLocaleString()} ST`],
                  ["플랫폼 보유량", `${(detail.holdingSupply ?? 0).toLocaleString()} ST`],
                  ["발행일",       detail.issuedAt
                    ? new Date(detail.issuedAt).toLocaleDateString("ko-KR")
                    : "-"],
                ].map(([l, v]) => (
                  <div key={l} className="space-y-1">
                    <p className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">{l}</p>
                    <p className="text-base font-semibold text-stone-800">{v}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
