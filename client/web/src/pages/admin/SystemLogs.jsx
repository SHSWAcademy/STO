import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowRightLeft,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  LogIn,
  RefreshCw,
} from "lucide-react";
import api from "../../lib/api.js";
import { cn } from "../../lib/utils.js";
import { Badge } from "../../components/ui/Badge.jsx";
import { SearchInput } from "../../components/ui/SearchInput.jsx";
import { TabSwitcher } from "../../components/ui/TabSwitcher.jsx";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];

const LOG_TABS = [
  {
    id: "loginLog",
    label: "로그인 로그",
    icon: LogIn,
    idKey: "loginLogId",
    idPrefix: "LOGIN",
  },
  {
    // Backend currently checks "oderLog"; keep this value until the API typo is fixed.
    id: "oderLog",
    label: "주문 로그",
    icon: ClipboardList,
    idKey: "orderLogId",
    idPrefix: "ORDER",
  },
  {
    id: "tradeLog",
    label: "거래 로그",
    icon: ArrowRightLeft,
    idKey: "tradeLogId",
    idPrefix: "TRADE",
  },
];

function normalizePage(data, fallbackSize) {
  return {
    number: Number.isFinite(data?.number) ? data.number : 0,
    size: data?.size ?? fallbackSize,
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    first: Boolean(data?.first),
    last: Boolean(data?.last),
  };
}

function formatDateTime(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ").slice(0, 19);

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

function getLogId(log, tab) {
  const value = log?.[tab.idKey];
  return value ? `${tab.idPrefix}-${value}` : "-";
}

function getResultVariant(result) {
  if (result === true) return "success";
  if (result === false) return "danger";
  return "neutral";
}

function getResultLabel(result) {
  if (result === true) return "성공";
  if (result === false) return "실패";
  return "미확인";
}

function PageButton({ children, active, disabled, onClick, ariaLabel }) {
  return (
    <button
      type="button"
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "flex h-9 min-w-9 items-center justify-center rounded-lg border px-3 text-xs font-bold transition-colors",
        active
          ? "border-stone-800 bg-stone-800 text-white"
          : "border-stone-200 bg-white text-stone-500 hover:bg-stone-100",
        disabled && "cursor-not-allowed opacity-40 hover:bg-white",
      )}
    >
      {children}
    </button>
  );
}

export function SystemLogs() {
  const [activeTab, setActiveTab] = useState(LOG_TABS[0].id);
  const [pageByTab, setPageByTab] = useState(() =>
    Object.fromEntries(LOG_TABS.map((tab) => [tab.id, 0])),
  );
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [logs, setLogs] = useState([]);
  const [pageMeta, setPageMeta] = useState(normalizePage(null, DEFAULT_PAGE_SIZE));
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  const activeTabConfig = useMemo(
    () => LOG_TABS.find((tab) => tab.id === activeTab) ?? LOG_TABS[0],
    [activeTab],
  );
  const page = pageByTab[activeTab] ?? 0;

  useEffect(() => {
    let mounted = true;

    async function loadLogs() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get("/admin/systemlog", {
          params: { category: activeTab, page, size },
        });

        if (!mounted) return;

        setLogs(data?.content ?? []);
        setPageMeta(normalizePage(data, size));
      } catch (loadError) {
        console.error("[SystemLogs] log load failed:", loadError);
        if (!mounted) return;

        setLogs([]);
        setPageMeta((prev) => ({ ...prev, totalElements: 0, totalPages: 0 }));
        setError("로그 목록을 불러오지 못했습니다.");
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadLogs();

    return () => {
      mounted = false;
    };
  }, [activeTab, page, size, refreshKey]);

  const filteredLogs = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return logs;

    return logs.filter((log) =>
      [
        getLogId(log, activeTabConfig),
        log.identifier,
        log.task,
        log.detail,
        log.ip,
        log.orderType,
        getResultLabel(log.result),
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword)),
    );
  }, [activeTabConfig, logs, searchTerm]);

  const pageNumbers = useMemo(() => {
    const totalPages = pageMeta.totalPages || 1;
    const current = pageMeta.number || 0;
    const start = Math.max(0, Math.min(current - 2, totalPages - 5));
    const end = Math.min(totalPages, start + 5);

    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [pageMeta.number, pageMeta.totalPages]);

  const from = pageMeta.totalElements === 0 ? 0 : pageMeta.number * pageMeta.size + 1;
  const to = Math.min((pageMeta.number + 1) * pageMeta.size, pageMeta.totalElements);
  const showIp = activeTab === "loginLog";
  const showOrderType = activeTab === "oderLog";
  const colSpan = 6 + Number(showIp) + Number(showOrderType);

  function changeTab(nextTab) {
    setActiveTab(nextTab);
    setSearchTerm("");
  }

  function changeSize(event) {
    const nextSize = Number(event.target.value);
    setSize(nextSize);
    setSearchTerm("");
    setPageByTab(Object.fromEntries(LOG_TABS.map((tab) => [tab.id, 0])));
  }

  function goToPage(nextPage) {
    if (nextPage < 0 || nextPage >= pageMeta.totalPages || nextPage === page) return;
    setSearchTerm("");
    setPageByTab((prev) => ({ ...prev, [activeTab]: nextPage }));
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">로그 관리</h1>
          <p className="text-sm text-stone-400">
            로그인, 주문, 거래 로그를 탭별로 조회합니다.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setSearchTerm("");
            setRefreshKey((prev) => prev + 1);
          }}
          className="flex items-center justify-center gap-2 rounded-lg border border-stone-200 bg-white px-4 py-2 text-sm font-semibold text-stone-500 transition-colors hover:bg-stone-100"
        >
          <RefreshCw className={cn("h-4 w-4", loading && "animate-spin")} />
          새로고침
        </button>
      </div>

      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <TabSwitcher
          variant="light"
          items={LOG_TABS}
          active={activeTab}
          onChange={changeTab}
          className="max-w-full overflow-x-auto"
        />

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <SearchInput
            variant="light"
            value={searchTerm}
            onChange={setSearchTerm}
            placeholder="현재 페이지에서 검색"
            className="min-w-[260px]"
          />

          <label className="flex items-center gap-2 text-xs font-bold text-stone-400">
            페이지당
            <select
              value={size}
              onChange={changeSize}
              className="rounded-lg border border-stone-200 bg-white px-3 py-2 text-sm font-semibold text-stone-700 outline-none focus:border-brand-blue"
            >
              {PAGE_SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}개
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 rounded-lg border border-red-100 bg-red-50 px-6 py-4 text-sm font-medium text-red-600">
          <AlertCircle className="h-5 w-5" />
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="flex flex-col gap-2 border-b border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-stone-800">{activeTabConfig.label}</p>
            <p className="text-xs font-medium text-stone-400">
              전체 {pageMeta.totalElements.toLocaleString()}건 중 {from.toLocaleString()}-
              {to.toLocaleString()} 표시
            </p>
          </div>
          <p className="text-xs font-bold text-stone-400">
            {searchTerm ? `현재 페이지 검색 결과 ${filteredLogs.length.toLocaleString()}건` : ""}
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 bg-stone-100">
                {[
                  "로그 ID",
                  "발생 시각",
                  "식별자",
                  "작업",
                  ...(showOrderType ? ["주문 유형"] : []),
                  "상세 내용",
                  ...(showIp ? ["IP"] : []),
                  "결과",
                ].map((header) => (
                  <th
                    key={header}
                    className={cn(
                      "whitespace-nowrap px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400",
                      header === "결과" && "text-center",
                    )}
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {loading ? (
                <tr>
                  <td colSpan={colSpan} className="px-6 py-16 text-center text-sm text-stone-400">
                    로그를 불러오는 중입니다.
                  </td>
                </tr>
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={colSpan} className="px-6 py-16 text-center text-sm text-stone-400">
                    표시할 로그가 없습니다.
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log, index) => (
                  <tr
                    key={`${activeTab}-${getLogId(log, activeTabConfig)}-${index}`}
                    className="transition-colors hover:bg-stone-50"
                  >
                    <td className="whitespace-nowrap px-6 py-4 text-xs font-mono font-bold text-stone-400">
                      {getLogId(log, activeTabConfig)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-xs font-bold text-stone-500">
                      {formatDateTime(log.createdAt ?? log.timeStamp)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-xs font-black text-stone-800">
                      {log.identifier || "-"}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-xs font-bold text-stone-500">
                      {log.task || "-"}
                    </td>
                    {showOrderType && (
                      <td className="whitespace-nowrap px-6 py-4 text-xs font-bold text-stone-500">
                        {log.orderType || "-"}
                      </td>
                    )}
                    <td className="px-6 py-4">
                      <p className="max-w-md truncate text-xs font-bold text-stone-400">
                        {log.detail || "-"}
                      </p>
                    </td>
                    {showIp && (
                      <td className="whitespace-nowrap px-6 py-4 text-xs font-mono font-bold text-stone-400">
                        {log.ip || "-"}
                      </td>
                    )}
                    <td className="px-6 py-4 text-center">
                      <Badge variant={getResultVariant(log.result)}>
                        {getResultLabel(log.result)}
                      </Badge>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-4 border-t border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs font-bold text-stone-400">
            {pageMeta.totalPages > 0
              ? `${(pageMeta.number + 1).toLocaleString()} / ${pageMeta.totalPages.toLocaleString()} 페이지`
              : "0 / 0 페이지"}
          </p>

          <div className="flex flex-wrap items-center gap-2">
            <PageButton
              disabled={pageMeta.first || loading || pageMeta.totalPages === 0}
              onClick={() => goToPage(pageMeta.number - 1)}
              ariaLabel="이전 페이지"
            >
              <ChevronLeft className="h-4 w-4" />
            </PageButton>

            {pageNumbers.map((pageNumber) => (
              <PageButton
                key={pageNumber}
                active={pageNumber === pageMeta.number}
                disabled={loading}
                onClick={() => goToPage(pageNumber)}
                ariaLabel={`${pageNumber + 1} 페이지`}
              >
                {pageNumber + 1}
              </PageButton>
            ))}

            <PageButton
              disabled={pageMeta.last || loading || pageMeta.totalPages === 0}
              onClick={() => goToPage(pageMeta.number + 1)}
              ariaLabel="다음 페이지"
            >
              <ChevronRight className="h-4 w-4" />
            </PageButton>
          </div>
        </div>
      </div>
    </div>
  );
}
