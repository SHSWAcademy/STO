import { useState } from 'react';
import { Bell, ChevronRight, Filter, FileText, Download, TrendingUp } from 'lucide-react';
import { cn } from '../lib/utils.js';
import { TabSwitcher } from '../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { EmptyState } from '../components/ui/EmptyState.jsx';

const NOTICES = [
  { id: 1, title: '3월 21일 서비스 점검 및 시스템 고도화 안내',   date: '2026.03.20', category: '시스템', important: true,  desc: '안정적인 서비스 제공을 위해 시스템 점검이 진행될 예정입니다.' },
  { id: 3, title: '신규 STO 자산 상장 안내 (강남 오피스 빌딩 A)', date: '2026.03.18', category: '일반',   important: false, desc: '새로운 부동산 STO 자산이 상장되었습니다.' },
  { id: 5, title: '개인정보 처리방침 개정 안내',                   date: '2026.03.15', category: '일반',   important: false, desc: '개인정보 처리방침이 일부 개정되었습니다.' },
  { id: 6, title: 'STONE 플랫폼 베타 서비스 오픈 이벤트',          date: '2026.03.10', category: '일반',   important: true,  desc: '베타 서비스 오픈 기념 다양한 이벤트를 확인하세요.' },
  { id: 7, title: '보안 강화를 위한 2단계 인증(2FA) 설정 권고',   date: '2026.03.05', category: '시스템', important: false, desc: '안전한 거래를 위해 2단계 인증을 설정해주세요.' },
];

export function NoticePage() {
  const [activeTab, setActiveTab]           = useState('전체');
  const [searchQuery, setSearchQuery]       = useState('');
  const [selectedNotice, setSelectedNotice] = useState(null);

  const tabs = ['전체', '일반', '시스템'];

  const filtered = NOTICES.filter(n => {
    const matchesTab    = activeTab === '전체' || n.category === activeTab;
    const matchesSearch = n.title.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  // 상세 보기
  if (selectedNotice) {
    return (
      <div className="max-w-[800px] mx-auto space-y-8">
        <button
          onClick={() => setSelectedNotice(null)}
          className="flex items-center gap-2 text-stone-400 hover:text-stone-800 transition-colors font-bold text-sm"
        >
          <ChevronRight className="rotate-180" size={18} /> 목록으로 돌아가기
        </button>

        <div className="bg-white rounded-2xl border border-stone-200 overflow-hidden shadow-sm">
          <div className="p-8 border-b border-stone-200">
            <div className="flex items-center gap-3 mb-4">
              <Badge variant={selectedNotice.category !== '시스템' && selectedNotice.important ? 'gold' : 'muted'}>
                {selectedNotice.category}
              </Badge>
              <span className="text-[10px] font-bold text-stone-400 font-mono">
                {selectedNotice.date}
              </span>
            </div>
            <h2 className="text-2xl font-black text-stone-800 leading-tight">
              {selectedNotice.title}
            </h2>
          </div>

          <div className="p-8 space-y-6">
            <div className="text-stone-500 leading-relaxed font-medium">
              {selectedNotice.desc}
              <br /><br />
              안녕하세요, STONE입니다.<br /><br />
              항상 저희 서비스를 이용해 주시는 고객님께 깊은 감사의 말씀을 드립니다.
              본 공지사항을 통해 안내드리는 내용을 확인하시어 서비스 이용에 참고하시기 바랍니다.
              <br /><br />
              [주요 내용]<br />
              - 일시: {selectedNotice.date}<br />
              - 대상: 전체 서비스 이용자<br />
              - 상세: {selectedNotice.desc}
              <br /><br />
              더욱 안정적이고 편리한 서비스를 제공하기 위해 최선을 다하겠습니다.
              감사합니다.
            </div>

            <div className="pt-8 border-t border-stone-200 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-stone-100 text-stone-500 text-xs font-bold hover:bg-stone-200 transition-all">
                  <FileText size={14} /> 첨부파일.pdf
                </button>
                <button className="p-2 rounded-xl bg-stone-100 text-stone-400 hover:text-stone-600 transition-all">
                  <Download size={18} />
                </button>
              </div>
              <button
                onClick={() => setSelectedNotice(null)}
                className="px-6 py-2 rounded-xl bg-stone-800 text-white text-xs font-black uppercase tracking-widest shadow-lg hover:bg-black transition-all"
              >
                확인 완료
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // 목록
  return (
    <div className="max-w-[1000px] mx-auto space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h2 className="text-3xl font-black text-stone-800 tracking-tight uppercase">
            공지사항
          </h2>
          <p className="text-sm text-stone-500 font-bold mt-2">
            STONE 플랫폼의 새로운 소식을 전해드립니다.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <SearchInput value={searchQuery} onChange={setSearchQuery} placeholder="제목 검색..." />
          <button className="p-2.5 rounded-2xl bg-stone-100 border border-stone-200 text-stone-400 hover:text-stone-800 transition-all">
            <Filter size={20} />
          </button>
        </div>
      </div>

      <TabSwitcher variant="pill" items={tabs} active={activeTab} onChange={setActiveTab} />

      <div className="bg-white rounded-2xl border border-stone-200 overflow-hidden shadow-sm">
        <div className="divide-y divide-stone-100">
          {filtered.length > 0 ? (
            filtered.map(notice => (
              <div
                key={notice.id}
                onClick={() => setSelectedNotice(notice)}
                className="group p-6 hover:bg-stone-50 transition-all cursor-pointer"
              >
                <div className="flex items-start justify-between gap-6">
                  <div className="flex items-start gap-6">
                    <div className={cn(
                      'w-12 h-12 rounded-2xl flex items-center justify-center transition-all shrink-0',
                      notice.important
                        ? 'bg-stone-800 text-white shadow-lg'
                        : 'bg-stone-100 text-stone-400 group-hover:bg-stone-200'
                    )}>
                      {notice.category === '배당' ? <TrendingUp size={20} /> : <Bell size={20} />}
                    </div>
                    <div>
                      <div className="flex items-center gap-3 mb-1">
                        <Badge variant={notice.category !== '시스템' && notice.important ? 'gold' : 'muted'}>
                          {notice.category}
                        </Badge>
                        <span className="text-[10px] font-bold text-stone-400 font-mono">{notice.date}</span>
                      </div>
                      <h3 className="text-base font-bold text-stone-800 group-hover:text-stone-600 transition-colors mb-2">
                        {notice.title}
                      </h3>
                      <p className="text-sm text-stone-500 line-clamp-1">{notice.desc}</p>
                    </div>
                  </div>
                  <ChevronRight className="text-stone-400 group-hover:text-stone-600 transition-colors mt-1" size={20} />
                </div>
              </div>
            ))
          ) : (
            <EmptyState message="검색 결과가 없습니다." className="m-4" />
          )}
        </div>
      </div>

      <div className="flex justify-center gap-2">
        {[1, 2, 3].map(p => (
          <button
            key={p}
            className={cn(
              'w-10 h-10 rounded-xl font-bold text-xs transition-all',
              p === 1
                ? 'bg-stone-800 text-white shadow-lg'
                : 'bg-white border border-stone-200 text-stone-400 hover:text-stone-800 hover:bg-stone-100'
            )}
          >
            {p}
          </button>
        ))}
      </div>
    </div>
  );
}
