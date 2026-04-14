import { cn } from "../../lib/utils.js";

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const pages = Array.from({ length: totalPages }, (_, i) => i)
    .filter((i) => i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 2)
    .reduce((acc, i, idx, arr) => {
      if (idx > 0 && i - arr[idx - 1] > 1) {
        acc.push("ellipsis-" + i);
      }
      acc.push(i);
      return acc;
    }, []);

  return (
    <div className="flex justify-center gap-2">
      <button
        onClick={() => onPageChange(Math.max(0, page - 1))}
        disabled={page === 0}
        className="px-3 py-1.5 rounded-lg text-xs font-bold bg-stone-100 text-stone-500 hover:bg-stone-200"
      >
        이전
      </button>

      {pages.map((item) =>
        typeof item === "string" ? (
          <span
            key={item}
            className="px-2 py-1.5 text-xs text-stone-400 font-bold"
          >
            ...
          </span>
        ) : (
          <button
            key={item}
            onClick={() => onPageChange(item)}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-bold transition-all",
              page === item
                ? "bg-stone-800 text-white"
                : "bg-stone-100 text-stone-500 hover:bg-stone-200",
            )}
          >
            {item + 1}
          </button>
        ),
      )}

      <button
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
        className="px-3 py-1.5 rounded-lg text-xs font-bold bg-stone-100 text-stone-500 hover:bg-stone-200"
      >
        다음
      </button>
    </div>
  );
}
