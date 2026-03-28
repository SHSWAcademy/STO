import { useState } from 'react';
import { FileText, Download, Filter, Calendar } from 'lucide-react';
import { cn } from '../lib/utils.js';
import { TabSwitcher } from '../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { Badge } from '../components/ui/Badge.jsx';
import { EmptyState } from '../components/ui/EmptyState.jsx';

const DISCLOSURES = [
  { id: 1, title: '2026년 1분기 서울강남빌딩 배당 보고서',        date: '2026.03.18', category: '배당', asset: '서울강남빌딩', desc: '서울강남빌딩의 2026년 1분기 운용 실적 및 배당금 산정 내역 보고서입니다.' },
  { id: 2, title: '2026년 1분기 송도 리조트 배당 보고서',          date: '2026.03.17', category: '배당', asset: '송도 리조트',  desc: '송도 리조트의 2026년 1분기 운용 실적 및 배당금 산정 내역 보고서입니다.' },
  { id: 3, title: '2026년 2월 전 종목 배당금 지급 결과 보고서',    date: '2026.03.01', category: '배당', asset: '전체',       desc: '2026년 2월 전 종목 배당금 지급 결과 요약 보고서입니다.' },
  { id: 4, title: '서울강남빌딩 주요 임대차 계약 갱신 공시',       date: '2026.02.25', category: '일반', asset: '서울강남빌딩', desc: '주요 임차인과의 계약 갱신에 따른 임대 수익 변동 안내입니다.' },
  { id: 5, title: '아트프라임 펀드 작품 매각 및 수익 분배 안내',   date: '2026.02.10', category: '배당', asset: '아트프라임 펀드', desc: '보유 작품 매각에 따른 특별 배당금 지급 안내 보고서입니다.' },
];

export function DisclosurePage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab]     = useState('전체');

  const tabs = ['전체', '배당', '일반'];

  const filtered = DISCLOSURES.filter(item => {
    const matchesTab    = activeTab === '전체' || item.category === activeTab;
    const matchesSearch = item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          item.asset.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  return (
    <div className="max-w-[1000px] mx-auto space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h2 className="text-3xl font-black text-stone-800 tracking-tight uppercase">
            정기<span className="text-stone-gold">공시</span>
          </h2>
          <p className="text-sm text-stone-500 font-bold mt-2">
            자산 운용 및 배당에 관한 공식 보고서를 확인하세요.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <SearchInput value={searchQuery} onChange={setSearchQuery} placeholder="종목명 또는 제목 검색..." />
          <button className="p-2.5 rounded-2xl bg-stone-100 border border-stone-200 text-stone-400 hover:text-stone-800 transition-all">
            <Filter size={20} />
          </button>
        </div>
      </div>

      <TabSwitcher variant="pill" items={tabs} active={activeTab} onChange={setActiveTab} />

      <div className="grid gap-4">
        {filtered.length > 0 ? (
          filtered.map(item => (
            <div key={item.id} className="group bg-white rounded-2xl border border-stone-200 p-6 hover:border-brand-gold/30 hover:shadow-xl transition-all cursor-pointer">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div className="flex items-start gap-6">
                  <div className="w-20 h-20 rounded-2xl bg-stone-100 border border-stone-200 flex items-center justify-center shrink-0">
                    <FileText size={28} className="text-stone-400" />
                  </div>
                  <div>
                    <div className="flex items-center gap-3 mb-2">
                      <Badge variant={item.category === '배당' ? 'buy' : 'muted'}>
                        {item.category} 공시
                      </Badge>
                      <span className="text-[10px] font-bold text-stone-400 flex items-center gap-1">
                        <Calendar size={12} /> {item.date}
                      </span>
                      <span className="text-[10px] font-black text-brand-gold bg-[#fef6dc] px-2 py-0.5 rounded-md">
                        {item.asset}
                      </span>
                    </div>
                    <h3 className="text-lg font-bold text-stone-800 group-hover:text-brand-gold transition-colors mb-2">{item.title}</h3>
                    <p className="text-sm text-stone-500 line-clamp-2 leading-relaxed">{item.desc}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3 self-end md:self-center">
                  <div className="hidden md:block h-12 w-px bg-stone-200 mx-2" />
                  <button className="flex items-center gap-2 px-5 py-3 bg-stone-100 border border-stone-200 rounded-2xl text-xs font-black text-stone-500 hover:bg-stone-800 hover:text-white hover:border-stone-800 transition-all">
                    <FileText size={16} className="text-brand-red" />
                    보고서 보기
                    <Download size={14} className="ml-1" />
                  </button>
                </div>
              </div>
            </div>
          ))
        ) : (
          <EmptyState message="공시 내역이 없습니다." />
        )}
      </div>

      <div className="flex justify-center gap-2">
        {[1, 2].map(p => (
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
