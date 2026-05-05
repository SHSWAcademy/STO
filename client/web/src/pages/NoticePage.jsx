import { useEffect, useState } from 'react';
import { Bell, ChevronRight, Filter, LoaderCircle } from 'lucide-react';
import { cn } from '../lib/utils.js';
import { TabSwitcher } from '../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { EmptyState } from '../components/ui/EmptyState.jsx';
import api from '../lib/api.js';

const NOTICE_TABS = ['전체', '일반', '시스템'];
const NOTICE_LABEL = {
  GENERAL: '일반',
  SYSTEM: '시스템',
};

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('ko-KR');
}

export function NoticePage() {
  const [activeTab, setActiveTab] = useState('전체');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [selectedNotice, setSelectedNotice] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function loadNotices() {
      setLoading(true);
      try {
        const { data } = await api.get('/api/notice', {
          params: { page, size: 10 },
        });

        if (!mounted) return;
        setItems(Array.isArray(data?.content) ? data.content : []);
        setTotalPages(Math.max(data?.totalPages ?? 1, 1));
      } catch (error) {
        console.error('[NoticePage] 목록 조회 실패:', error);
        if (!mounted) return;
        setItems([]);
        setTotalPages(1);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    loadNotices();
    return () => {
      mounted = false;
    };
  }, [page]);

  const filteredItems = items.filter((item) => {
    const typeLabel = NOTICE_LABEL[item.noticeType] ?? '일반';
    const keyword = searchQuery.trim().toLowerCase();
    const matchesTab = activeTab === '전체' || typeLabel === activeTab;
    const matchesSearch =
      keyword.length === 0 ||
      String(item.noticeTitle ?? '').toLowerCase().includes(keyword) ||
      String(item.noticeContent ?? '').toLowerCase().includes(keyword);

    return matchesTab && matchesSearch;
  });

  async function handleSelectNotice(notice) {
    setDetailLoading(true);
    try {
      const { data } = await api.get(`/api/notice/${notice.noticeId}`);
      setSelectedNotice({
        noticeId: notice.noticeId,
        noticeType: data.noticeType,
        noticeTitle: data.noticeTitle,
        noticeContent: data.noticeContent,
        createdAt: data.createdAt,
      });
    } catch (error) {
      console.error('[NoticePage] 상세 조회 실패:', error);
    } finally {
      setDetailLoading(false);
    }
  }

  if (selectedNotice) {
    const typeLabel = NOTICE_LABEL[selectedNotice.noticeType] ?? '일반';
    return (
      <div className="mx-auto max-w-[800px] space-y-8">
        <button
          type="button"
          onClick={() => setSelectedNotice(null)}
          className="flex items-center gap-2 text-sm font-bold text-stone-400 transition-colors hover:text-stone-800"
        >
          <ChevronRight className="rotate-180" size={18} /> 목록으로 돌아가기
        </button>

        <div className="overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-sm">
          <div className="border-b border-stone-200 p-8">
            <div className="mb-4 flex items-center gap-3">
              <Badge variant={typeLabel === '시스템' ? 'muted' : 'gold'}>{typeLabel}</Badge>
              <span className="font-mono text-[10px] font-bold text-stone-400">
                {formatDate(selectedNotice.createdAt)}
              </span>
            </div>
            <h2 className="text-2xl font-black leading-tight text-stone-800">
              {selectedNotice.noticeTitle}
            </h2>
          </div>

          <div className="space-y-6 p-8">
            <div className="whitespace-pre-wrap leading-relaxed text-stone-600">
              {selectedNotice.noticeContent}
            </div>

            <div className="flex items-center justify-end border-t border-stone-200 pt-8">
              <button
                type="button"
                onClick={() => setSelectedNotice(null)}
                className="rounded-xl bg-stone-800 px-6 py-2 text-xs font-black uppercase tracking-widest text-white shadow-lg transition-all hover:bg-black"
              >
                확인 완료
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-[1000px] space-y-8">
      <div className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
        <div>
          <h2 className="text-3xl font-black tracking-tight text-stone-800 uppercase">
            공지사항
          </h2>
          <p className="mt-2 text-sm font-bold text-stone-500">
            서비스 운영과 점검 관련 안내를 확인하세요.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <SearchInput value={searchQuery} onChange={setSearchQuery} placeholder="공지 제목 검색" />
          <button className="rounded-2xl border border-stone-200 bg-stone-100 p-2.5 text-stone-400 transition-all hover:text-stone-800">
            <Filter size={20} />
          </button>
        </div>
      </div>

      <TabSwitcher variant="pill" items={NOTICE_TABS} active={activeTab} onChange={setActiveTab} />

      <div className="overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-sm">
        <div className="divide-y divide-stone-100">
          {loading ? (
            <EmptyState message="공지사항을 불러오는 중입니다." className="m-4" />
          ) : filteredItems.length > 0 ? (
            filteredItems.map((notice) => {
              const typeLabel = NOTICE_LABEL[notice.noticeType] ?? '일반';
              return (
                <div
                  key={notice.noticeId}
                  onClick={() => handleSelectNotice(notice)}
                  className="group cursor-pointer p-6 transition-all hover:bg-stone-50"
                >
                  <div className="flex items-start justify-between gap-6">
                    <div className="flex items-start gap-6">
                      <div
                        className={cn(
                          'flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl transition-all',
                          typeLabel === '시스템'
                            ? 'bg-stone-800 text-white shadow-lg'
                            : 'bg-stone-100 text-stone-400 group-hover:bg-stone-200',
                        )}
                      >
                        <Bell size={20} />
                      </div>
                      <div>
                        <div className="mb-1 flex items-center gap-3">
                          <Badge variant={typeLabel === '시스템' ? 'muted' : 'gold'}>{typeLabel}</Badge>
                          <span className="font-mono text-[10px] font-bold text-stone-400">
                            {formatDate(notice.createdAt)}
                          </span>
                        </div>
                        <h3 className="mb-2 text-base font-bold text-stone-800 transition-colors group-hover:text-stone-600">
                          {notice.noticeTitle}
                        </h3>
                        <p className="line-clamp-1 text-sm text-stone-500">{notice.noticeContent}</p>
                      </div>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-stone-400 transition-colors group-hover:text-stone-600">
                      {detailLoading && selectedNotice === null ? <LoaderCircle size={16} className="animate-spin" /> : null}
                      <ChevronRight size={20} />
                    </div>
                  </div>
                </div>
              );
            })
          ) : (
            <EmptyState message="검색 결과가 없습니다." className="m-4" />
          )}
        </div>
      </div>

      <div className="flex justify-center gap-2">
        {Array.from({ length: totalPages }, (_, index) => index).map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setPage(value)}
            className={cn(
              'h-10 w-10 rounded-xl text-xs font-bold transition-all',
              value === page
                ? 'bg-stone-800 text-white shadow-lg'
                : 'border border-stone-200 bg-white text-stone-400 hover:bg-stone-100 hover:text-stone-800',
            )}
          >
            {value + 1}
          </button>
        ))}
      </div>
    </div>
  );
}
