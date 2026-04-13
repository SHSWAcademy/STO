import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Calendar,
  ChevronLeft,
  ChevronRight,
  DollarSign,
  Mail,
  Power,
  RefreshCw,
  UserCheck,
  UserMinus,
  Users as UsersIcon,
} from "lucide-react";
import api from "../../lib/api.js";
import { cn } from "../../lib/utils.js";
import { SearchInput } from "../../components/ui/SearchInput.jsx";
import { Badge } from "../../components/ui/Badge.jsx";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];

function formatCurrency(value) {
  return `${Number(value ?? 0).toLocaleString()}원`;
}

function formatDate(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

function getInitial(name, email) {
  return (name || email || "?").trim().slice(0, 1).toUpperCase();
}

function toPageNumber(value) {
  return Number.isFinite(value) && value >= 0 ? value : 0;
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

export function UserManagement() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [pageMeta, setPageMeta] = useState({
    number: 0,
    size: DEFAULT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [updatingMemberId, setUpdatingMemberId] = useState(null);

  useEffect(() => {
    let mounted = true;

    async function loadMembers() {
      setLoading(true);
      setError("");
      setStatusMessage("");

      try {
        const { data } = await api.get("/admin/memberlist", {
          params: { page, size },
        });

        if (!mounted) return;

        setUsers(data?.content ?? []);
        setPageMeta({
          number: toPageNumber(data?.number),
          size: data?.size ?? size,
          totalElements: data?.totalElements ?? 0,
          totalPages: data?.totalPages ?? 0,
          first: Boolean(data?.first),
          last: Boolean(data?.last),
        });
      } catch (loadError) {
        console.error("[UserManagement] member list load failed:", loadError);
        if (!mounted) return;
        setUsers([]);
        setError("사용자 목록을 불러오지 못했습니다.");
        setPageMeta((prev) => ({ ...prev, totalElements: 0, totalPages: 0 }));
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadMembers();

    return () => {
      mounted = false;
    };
  }, [page, size, refreshKey]);

  const filteredUsers = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return users;

    return users.filter((user) =>
      [user.memberName, user.email, String(user.memberId ?? "")]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(keyword)),
    );
  }, [searchTerm, users]);

  const currentPageActiveCount = users.filter((user) => user.isActive).length;
  const currentPageInactiveCount = users.length - currentPageActiveCount;
  const currentPageTradeAmount = users.reduce(
    (sum, user) => sum + Number(user.totalTradeAmount ?? 0),
    0,
  );

  const pageNumbers = useMemo(() => {
    const totalPages = pageMeta.totalPages || 1;
    const current = pageMeta.number || 0;
    const start = Math.max(0, Math.min(current - 2, totalPages - 5));
    const end = Math.min(totalPages, start + 5);

    return Array.from({ length: end - start }, (_, index) => start + index);
  }, [pageMeta.number, pageMeta.totalPages]);

  function handleSizeChange(event) {
    setSize(Number(event.target.value));
    setPage(0);
    setSearchTerm("");
  }

  async function handleStatusToggle(memberId, nextActive) {
    setUpdatingMemberId(memberId);
    setStatusMessage("");
    setError("");

    try {
      await api.patch(`/admin/memberlist/${memberId}`, null, {
        params: { isActive: nextActive },
      });

      setUsers((prev) =>
        prev.map((user) =>
          user.memberId === memberId ? { ...user, isActive: nextActive } : user,
        ),
      );
      setStatusMessage(
        nextActive ? "사용자를 활성화했습니다." : "사용자를 비활성화했습니다.",
      );
    } catch (updateError) {
      console.error("[UserManagement] member status update failed:", updateError);
      setError("사용자 상태 변경에 실패했습니다.");
    } finally {
      setUpdatingMemberId(null);
    }
  }

  function goToPage(nextPage) {
    if (nextPage < 0 || nextPage >= pageMeta.totalPages || nextPage === page) return;
    setPage(nextPage);
    setSearchTerm("");
  }

  const from = pageMeta.totalElements === 0 ? 0 : pageMeta.number * pageMeta.size + 1;
  const to = Math.min((pageMeta.number + 1) * pageMeta.size, pageMeta.totalElements);

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">사용자 관리</h1>
          <p className="text-sm text-stone-400">
            가입자 현황과 계정 활성 상태를 관리합니다.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <SearchInput
            variant="light"
            value={searchTerm}
            onChange={setSearchTerm}
            placeholder="현재 페이지에서 이름, 이메일 검색"
            className="min-w-[260px]"
          />
          <button
            type="button"
            onClick={() => {
              setSearchTerm("");
              setRefreshKey((prev) => prev + 1);
            }}
            className="flex items-center justify-center gap-2 rounded-lg border border-stone-200 bg-white px-4 py-2 text-sm font-semibold text-stone-500 transition-colors hover:bg-stone-100"
          >
            <RefreshCw className="h-4 w-4" />
            새로고침
          </button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {[
          {
            label: "전체 사용자",
            value: `${pageMeta.totalElements.toLocaleString()}명`,
            icon: UsersIcon,
            color: "text-stone-600",
            bg: "bg-stone-200",
          },
          {
            label: "현재 페이지 활성",
            value: `${currentPageActiveCount.toLocaleString()}명`,
            icon: UserCheck,
            color: "text-brand-green",
            bg: "bg-brand-green-light",
          },
          {
            label: "현재 페이지 비활성",
            value: `${currentPageInactiveCount.toLocaleString()}명`,
            icon: UserMinus,
            color: "text-brand-red",
            bg: "bg-brand-red-light",
          },
          {
            label: "현재 페이지 투자액",
            value: formatCurrency(currentPageTradeAmount),
            icon: DollarSign,
            color: "text-brand-blue",
            bg: "bg-brand-blue-light",
          },
        ].map((stat) => (
          <div key={stat.label} className="rounded-lg border border-stone-200 bg-white p-6">
            <div className="mb-4 flex items-center justify-between">
              <div className={cn("rounded-lg p-3", stat.bg)}>
                <stat.icon className={cn("h-6 w-6", stat.color)} />
              </div>
            </div>
            <p className="mb-1 text-xs font-bold text-stone-400">{stat.label}</p>
            <h3 className="text-2xl font-semibold text-stone-800">{stat.value}</h3>
          </div>
        ))}
      </div>

      {error && (
        <div className="flex items-center gap-3 rounded-lg border border-red-100 bg-red-50 px-6 py-4 text-sm font-medium text-red-600">
          <AlertCircle className="h-5 w-5" />
          {error}
        </div>
      )}

      {statusMessage && (
        <div className="flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 px-6 py-4 text-sm font-medium text-amber-700">
          <AlertCircle className="h-5 w-5" />
          {statusMessage}
        </div>
      )}

      <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
        <div className="flex flex-col gap-3 border-b border-stone-200 bg-stone-50 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-stone-800">사용자 목록</p>
            <p className="text-xs font-medium text-stone-400">
              전체 {pageMeta.totalElements.toLocaleString()}명 중 {from.toLocaleString()}-
              {to.toLocaleString()} 표시
            </p>
          </div>

          <label className="flex items-center gap-2 text-xs font-bold text-stone-400">
            페이지당 표시
            <select
              value={size}
              onChange={handleSizeChange}
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

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 bg-stone-100">
                {["사용자 정보", "가입일", "총 투자액", "상태", "활성 처리"].map((header) => (
                  <th
                    key={header}
                    className={cn(
                      "px-6 py-4 text-[10px] font-semibold uppercase tracking-wide text-stone-400",
                      (header === "상태" || header === "활성 처리") && "text-center",
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
                  <td colSpan={5} className="px-6 py-16 text-center text-sm text-stone-400">
                    불러오는 중...
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-16 text-center text-sm text-stone-400">
                    표시할 사용자가 없습니다.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.memberId} className="transition-colors hover:bg-stone-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-stone-200 text-sm font-black text-stone-500">
                          {getInitial(user.memberName, user.email)}
                        </div>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-black text-stone-800">
                            {user.memberName || "-"}
                          </p>
                          <p className="flex items-center gap-1 truncate text-xs font-bold text-stone-400">
                            <Mail className="h-3 w-3 shrink-0" />
                            {user.email || "-"}
                          </p>
                          <p className="mt-1 text-[10px] font-bold text-stone-400">
                            ID {user.memberId ?? "-"}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2 whitespace-nowrap text-sm font-bold text-stone-500">
                        <Calendar className="h-4 w-4 text-stone-400" />
                        {formatDate(user.createdAt)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-black text-stone-800">
                      {formatCurrency(user.totalTradeAmount)}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <Badge variant={user.isActive ? "success" : "danger"}>
                        {user.isActive ? "활성" : "비활성"}
                      </Badge>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex justify-center">
                        <button
                          type="button"
                          onClick={() => handleStatusToggle(user.memberId, !user.isActive)}
                          disabled={updatingMemberId === user.memberId}
                          className={cn(
                            "inline-flex min-w-[96px] items-center justify-center gap-2 rounded-lg px-3 py-2 text-xs font-bold transition-colors",
                            user.isActive
                              ? "bg-brand-red-light text-brand-red-dk hover:bg-red-100"
                              : "bg-brand-green-light text-brand-green hover:bg-green-100",
                            updatingMemberId === user.memberId &&
                              "cursor-not-allowed opacity-60",
                          )}
                        >
                          {updatingMemberId === user.memberId ? (
                            <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                          ) : (
                            <Power className="h-3.5 w-3.5" />
                          )}
                          {updatingMemberId === user.memberId
                            ? "처리 중"
                            : user.isActive
                              ? "비활성화"
                              : "활성화"}
                        </button>
                      </div>
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
