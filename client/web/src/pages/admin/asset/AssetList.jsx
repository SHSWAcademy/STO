import { Search, Filter, PlusCircle } from "lucide-react";
import { formatDate, imgSrc, StatusBadge } from "./assetUtils.jsx";

export function AssetList({
  tokens,
  loading,
  error,
  searchTerm,
  onSearch,
  onSelect,
  onNew,
}) {
  const filtered = tokens.filter(
    (t) =>
      (t.assetName ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (t.tokenSymbol ?? "").toLowerCase().includes(searchTerm.toLowerCase()),
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">자산 관리</h1>
          <p className="text-sm text-stone-400">
            플랫폼에 등록된 STO 자산을 관리하고 신규 자산을 등록합니다.
          </p>
        </div>
        <button
          onClick={onNew}
          className="flex items-center gap-2 px-6 py-3 bg-brand-blue text-white text-sm font-medium rounded-md hover:bg-brand-blue-dk transition-colors"
        >
          <PlusCircle className="w-5 h-5" /> 신규 자산 등록
        </button>
      </div>

      <div className="bg-white rounded-lg border border-stone-200 overflow-hidden">
        <div className="p-6 border-b border-stone-200 flex items-center justify-between bg-stone-50">
          <div className="flex items-center gap-3 bg-white px-4 py-3 rounded-md border border-stone-200 focus-within:border-brand-blue transition-colors">
            <Search className="w-5 h-5 text-stone-400" />
            <input
              type="text"
              placeholder="자산명 또는 심볼 검색..."
              value={searchTerm}
              onChange={(e) => onSearch(e.target.value)}
              className="bg-transparent border-none outline-none text-sm w-64 font-medium text-stone-800"
            />
          </div>
          <button className="p-3 bg-white border border-stone-200 rounded-md text-stone-400 hover:bg-stone-100 transition-colors">
            <Filter className="w-5 h-5" />
          </button>
        </div>

        {error && (
          <div className="px-6 py-3 bg-amber-50 border-b border-amber-200 text-xs font-medium text-amber-700">
            {error}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-stone-50 border-b border-stone-200">
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide">
                  자산 정보
                </th>
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide text-right">
                  자산 총 금액
                </th>
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide text-right">
                  총 발행량
                </th>
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide text-center">
                  배당 지급
                </th>
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide text-center">
                  등록일
                </th>
                <th className="px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide text-center">
                  상태
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {loading ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-6 py-16 text-center text-sm text-stone-400"
                  >
                    등록된 자산이 없습니다.
                  </td>
                </tr>
              ) : (
                filtered.map((t) => (
                  <tr
                    key={t.assetId}
                    className="hover:bg-stone-50 transition-colors cursor-pointer"
                    onClick={() => onSelect(t)}
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        {t.imgUrl ? (
                          <img
                            src={imgSrc(t.imgUrl)}
                            alt={t.assetName}
                            className="w-12 h-12 rounded-lg object-cover border border-stone-200"
                          />
                        ) : (
                          <div className="w-12 h-12 rounded-lg bg-stone-100 border border-stone-200 flex items-center justify-center text-xs font-bold text-stone-400">
                            {(t.tokenSymbol ?? "?").slice(0, 2)}
                          </div>
                        )}
                        <div>
                          <p className="text-sm font-semibold text-stone-800">
                            {t.assetName}
                          </p>
                          <p className="text-[10px] font-mono font-bold text-stone-400">
                            {t.tokenSymbol}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-bold text-stone-800">
                      ₩{(t.totalValue ?? 0).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-bold text-stone-800">
                      {(t.totalSupply ?? 0).toLocaleString()} ST
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${
                          t.isAllocated
                            ? "bg-brand-green-light text-brand-green"
                            : "bg-stone-100 text-stone-500"
                        }`}
                      >
                        {t.isAllocated ? "지급" : "미지급"}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center text-sm font-medium text-stone-500 whitespace-nowrap">
                      {formatDate(t.issuedAt)}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <StatusBadge status={t.status} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
