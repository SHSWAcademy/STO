import { useEffect, useState } from 'react';
import { FileText, Download, Filter, Calendar } from 'lucide-react';
import { cn } from '../lib/utils.js';
import { TabSwitcher } from '../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { EmptyState } from '../components/ui/EmptyState.jsx';
import api from '../lib/api.js';
import { FILE_URLS } from '../lib/config.js';

const DISCLOSURE_TABS = ['전체', '배당', '일반'];
const DISCLOSURE_LABEL = {
  BUILDING: '일반',
  DIVIDEND: '배당',
  ETC: '일반',
};

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('ko-KR');
}

function resolveImageSrc(src) {
  if (!src) return null;
  if (/^(https?:)?\/\//.test(src) || src.startsWith('data:') || src.startsWith('blob:')) {
    return src;
  }
  if (src.startsWith('/')) return src;
  return `${FILE_URLS.imageBase}/${src}`;
}

export function DisclosurePage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('전체');
  const [page, setPage] = useState(0);
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [openingId, setOpeningId] = useState(null);

  useEffect(() => {
    let mounted = true;

    async function loadDisclosures() {
      setLoading(true);
      try {
        const { data } = await api.get('/api/disclosure', {
          params: { page, size: 10 },
        });

        if (!mounted) return;
        setItems(Array.isArray(data?.content) ? data.content : []);
        setTotalPages(Math.max(data?.totalPages ?? 1, 1));
      } catch (error) {
        console.error('[DisclosurePage] 목록 조회 실패:', error);
        if (!mounted) return;
        setItems([]);
        setTotalPages(1);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    loadDisclosures();
    return () => {
      mounted = false;
    };
  }, [page]);

  const filteredItems = items.filter((item) => {
    const categoryLabel = DISCLOSURE_LABEL[item.disclosureCategory] ?? '일반';
    const keyword = searchQuery.trim().toLowerCase();
    const matchesTab = activeTab === '전체' || categoryLabel === activeTab;
    const matchesSearch =
      keyword.length === 0 ||
      String(item.disclosureTitle ?? '').toLowerCase().includes(keyword) ||
      String(item.assetName ?? '').toLowerCase().includes(keyword);

    return matchesTab && matchesSearch;
  });

  async function handleOpenDisclosure(item) {
    if (!item?.storedName) return;

    setOpeningId(item.disclosureId);
    try {
      const response = await api.get(`${FILE_URLS.pdfViewBase}/${encodeURIComponent(item.storedName)}`, {
        responseType: 'blob',
      });
      const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      window.open(blobUrl, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60_000);
    } catch (error) {
      console.error('[DisclosurePage] PDF 열기 실패:', error);
    } finally {
      setOpeningId(null);
    }
  }

  return (
    <div className="mx-auto max-w-[1000px] space-y-8">
      <div className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
        <div>
          <h2 className="text-3xl font-black tracking-tight text-stone-800 uppercase">
            공시
          </h2>
          <p className="mt-2 text-sm font-bold text-stone-500">
            자산 운용과 배당 관련 공시를 확인하세요.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <SearchInput
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="종목명 또는 공시 제목 검색"
          />
          <button className="rounded-2xl border border-stone-200 bg-stone-100 p-2.5 text-stone-400 transition-all hover:text-stone-800">
            <Filter size={20} />
          </button>
        </div>
      </div>

      <TabSwitcher variant="pill" items={DISCLOSURE_TABS} active={activeTab} onChange={setActiveTab} />

      <div className="grid gap-4">
        {loading ? (
          <EmptyState message="공시 내역을 불러오는 중입니다." />
        ) : filteredItems.length > 0 ? (
          filteredItems.map((item) => {
            const categoryLabel = DISCLOSURE_LABEL[item.disclosureCategory] ?? '일반';
            const imageSrc = resolveImageSrc(item.imgUrl);
            return (
              <div
                key={item.disclosureId}
                className="group cursor-pointer rounded-2xl border border-stone-200 bg-white p-6 transition-all hover:border-stone-300 hover:shadow-xl"
              >
                <div className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
                  <div className="flex items-start gap-6">
                    <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-stone-200 bg-stone-100">
                      {imageSrc ? (
                        <img
                          src={imageSrc}
                          alt={item.assetName || item.disclosureTitle || 'asset'}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <FileText size={28} className="text-stone-400" />
                      )}
                    </div>
                    <div>
                      <div className="mb-2 flex flex-wrap items-center gap-3">
                        <Badge variant={categoryLabel === '배당' ? 'buy' : 'muted'}>
                          {categoryLabel} 공시
                        </Badge>
                        <span className="flex items-center gap-1 text-[10px] font-bold text-stone-400">
                          <Calendar size={12} /> {formatDate(item.createdAt)}
                        </span>
                        <span className="rounded-md bg-stone-100 px-2 py-0.5 text-[10px] font-black text-stone-600">
                          {item.assetName ?? '-'}
                        </span>
                      </div>
                      <h3 className="mb-2 text-lg font-bold text-stone-800 transition-colors group-hover:text-stone-600">
                        {item.disclosureTitle}
                      </h3>
                      <p className="line-clamp-2 text-sm leading-relaxed text-stone-500">
                        {item.disclosureContent}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 self-end md:self-center">
                    <div className="mx-2 hidden h-12 w-px bg-stone-200 md:block" />
                    <button
                      type="button"
                      onClick={() => handleOpenDisclosure(item)}
                      disabled={!item.storedName || openingId === item.disclosureId}
                      className={cn(
                        'flex items-center gap-2 rounded-2xl border px-5 py-3 text-xs font-black transition-all',
                        item.storedName
                          ? 'border-stone-200 bg-stone-100 text-stone-500 hover:border-stone-800 hover:bg-stone-800 hover:text-white'
                          : 'cursor-not-allowed border-stone-200 bg-stone-50 text-stone-300',
                      )}
                    >
                      <FileText size={16} className={cn(item.storedName ? 'text-brand-red' : 'text-stone-300')} />
                      {openingId === item.disclosureId ? '열는 중...' : item.originName ? '첨부 공시 보기' : '첨부 파일 없음'}
                      <Download size={14} className="ml-1" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <EmptyState message="공시 내역이 없습니다." />
        )}
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
