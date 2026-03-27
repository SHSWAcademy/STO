import { useState } from 'react';
import { FileText, PlusCircle, Filter, CheckCircle2, XCircle, AlertCircle, DollarSign, Edit3, Trash2, X } from 'lucide-react';
import { useApp } from '../../context/AppContext.jsx';
import { cn } from '../../lib/utils.js';
import { TabSwitcher } from '../../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../../components/ui/SearchInput.jsx';
import { Badge } from '../../components/ui/Badge.jsx';

export function ContentManagement() {
  const { disclosures, setDisclosures, tokens, notices, setNotices } = useApp();
  const [activeTab, setActiveTab]         = useState('notices');
  const [disclosureFilter, setDisclosureFilter] = useState('all');
  const [disclosureTypeTab, setDisclosureTypeTab] = useState('전체');
  const [editingDisclosure, setEditingDisclosure] = useState(null);
  const [isAddingNotice, setIsAddingNotice]       = useState(false);
  const [isAddingDisclosure, setIsAddingDisclosure] = useState(false);
  const [noticeSearchTerm, setNoticeSearchTerm]   = useState('');
  const [searchTerm, setSearchTerm]               = useState('');
  const [editForm, setEditForm] = useState({ title: '', file: '', status: '', asset: '', type: '일반' });
  const [noticeForm, setNoticeForm] = useState({ title: '', category: '일반', important: false, content: '' });

  function handleEditDisclosure(d) {
    setEditingDisclosure(d);
    setEditForm({ title: d.title, file: d.file, status: d.status, asset: d.asset, type: d.type });
  }

  function handleSaveDisclosure() {
    if (!editingDisclosure) return;
    setDisclosures(prev => prev.map(d => d.id === editingDisclosure.id ? { ...d, ...editForm } : d));
    setEditingDisclosure(null);
  }

  function handleAddNotice() {
    const newNotice = {
      id: Date.now(),
      title: noticeForm.title,
      date: new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\s/g, ''),
      category: noticeForm.category,
      important: noticeForm.important,
      content: noticeForm.content,
    };
    setNotices(prev => [newNotice, ...prev]);
    alert('신규 공지사항이 등록되었습니다.');
    setIsAddingNotice(false);
    setNoticeForm({ title: '', category: '일반', important: false, content: '' });
  }

  function handleAddDisclosure() {
    const newDisclosure = {
      id: Date.now(),
      asset:  editForm.asset,
      assetId: tokens.find(t => t.name === editForm.asset)?.id || 'all',
      type:   editForm.type,
      title:  editForm.title,
      date:   new Date().toISOString().split('T')[0],
      file:   editForm.file,
      status: '승인완료',
    };
    setDisclosures(prev => [newDisclosure, ...prev]);
    alert('신규 공시가 등록되었습니다.');
    setIsAddingDisclosure(false);
    setEditForm({ title: '', file: '', status: '', asset: '', type: '일반' });
  }

  const filteredDisclosures = disclosures.filter(item => {
    const matchesFilter =
      disclosureFilter === 'all' ||
      (disclosureFilter === 'dividend' && item.type === '배당') ||
      (disclosureFilter === 'pending' && item.status === '검토대기');
    const matchesTypeTab = disclosureTypeTab === '전체' || item.type === disclosureTypeTab;
    const matchesSearch  = item.asset.toLowerCase().includes(searchTerm.toLowerCase()) || item.title.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesFilter && matchesTypeTab && matchesSearch;
  });

  const filteredNotices = notices.filter(n =>
    n.title.toLowerCase().includes(noticeSearchTerm.toLowerCase()) ||
    n.category.toLowerCase().includes(noticeSearchTerm.toLowerCase())
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#2a2820]">공시/공지 관리</h1>
          <p className="text-sm text-[#9a9080]">플랫폼 공지사항 및 자산별 공시 내역을 관리합니다.</p>
        </div>
        <button
          onClick={() => activeTab === 'notices' ? setIsAddingNotice(true) : setIsAddingDisclosure(true)}
          className="flex items-center gap-2 px-6 py-3 bg-[#4a72a0] text-white text-sm font-medium rounded-md hover:bg-[#3a62a0] transition-colors"
        >
          <PlusCircle className="w-5 h-5" />
          {activeTab === 'notices' ? '신규 공지 등록' : '신규 공시 등록'}
        </button>
      </div>

      {/* Tabs */}
      <TabSwitcher variant="light"
        items={[{ id: 'notices', label: '공지사항' }, { id: 'disclosures', label: '공시 내역' }]}
        active={activeTab}
        onChange={setActiveTab}
      />

      {activeTab === 'notices' ? (
        <div className="bg-white rounded-lg border border-[#e0dace] overflow-hidden">
          <div className="p-6 border-b border-[#e0dace] flex items-center justify-between">
            <SearchInput variant="light" value={noticeSearchTerm} onChange={setNoticeSearchTerm} placeholder="공지 제목 검색..." />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                  {['제목','카테고리','작성일','중요','관리'].map(h => (
                    <th key={h} className={`px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide ${h === '중요' || h === '관리' ? 'text-center' : ''}`}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e0dace]">
                {filteredNotices.map(n => (
                  <tr key={n.id} className="hover:bg-[#f7f5f0] transition-all group">
                    <td className="px-6 py-4"><p className="text-sm font-semibold text-[#2a2820]">{n.title}</p></td>
                    <td className="px-6 py-4">
                      <Badge variant="warning">{n.category}</Badge>
                    </td>
                    <td className="px-6 py-4 text-sm font-bold text-[#9a9080]">{n.date}</td>
                    <td className="px-6 py-4 text-center">
                      {n.important
                        ? <CheckCircle2 className="w-5 h-5 text-[#b85450] mx-auto" />
                        : <XCircle className="w-5 h-5 text-[#e0dace] mx-auto" />}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-all">
                        <button className="p-2 text-[#4a72a0] hover:bg-[#ffe65a]/20 rounded-lg transition-all"><Edit3 className="w-4 h-4" /></button>
                        <button className="p-2 text-[#b85450] hover:bg-[#fde8e8] rounded-lg transition-colors"><Trash2 className="w-4 h-4" /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Disclosure filter cards */}
          <div className="flex items-center gap-4 p-2 bg-[#f7f5f0] border border-[#e0dace] rounded-lg w-fit">
            {[
              { id: 'all',      label: '총 공시 건수',    value: disclosures.length,                             icon: FileText,    color: 'text-[#7a7060]', bg: 'bg-[#e0dace]' },
              { id: 'dividend', label: '이번 달 배당 공시', value: disclosures.filter(d => d.type === '배당').length, icon: DollarSign,  color: 'text-[#c9a84c]', bg: 'bg-[#ffe65a]/30' },
              { id: 'pending',  label: '검토 대기',        value: disclosures.filter(d => d.status === '검토대기').length, icon: AlertCircle, color: 'text-[#b85450]', bg: 'bg-[#fde8e8]' },
            ].map(stat => (
              <button key={stat.id} onClick={() => setDisclosureFilter(stat.id)}
                className={cn('flex items-center gap-4 px-8 py-4 rounded-lg transition-colors text-left min-w-[200px]',
                  disclosureFilter === stat.id ? 'bg-white border border-[#e0dace]' : 'hover:bg-white/50 border border-transparent'
                )}>
                <div className={cn('p-3 rounded-xl', stat.bg)}>
                  <stat.icon className={cn('w-5 h-5', stat.color)} />
                </div>
                <div>
                  <p className="text-xs font-semibold text-[#9a9080] uppercase tracking-wide mb-1">{stat.label}</p>
                  <h3 className="text-xl font-semibold text-[#2a2820]">{stat.value}건</h3>
                </div>
              </button>
            ))}
          </div>

          <div className="bg-white rounded-lg border border-[#e0dace] overflow-hidden">
            <div className="px-8 pt-6 pb-0 border-b border-[#e0dace] bg-[#f0ede4] space-y-4">
              <div className="flex items-center justify-between">
                <SearchInput variant="light" value={searchTerm} onChange={setSearchTerm} placeholder="공시 내용 검색..." />
                <button className="p-3 bg-white border border-[#e0dace] rounded-md text-[#7a7060] hover:bg-[#f7f5f0] transition-all">
                  <Filter className="w-5 h-5" />
                </button>
              </div>
              <div className="flex gap-1">
                {['전체', '배당', '일반'].map(tab => (
                  <button key={tab} onClick={() => setDisclosureTypeTab(tab)}
                    className={cn('px-5 py-2.5 text-sm font-semibold transition-colors border-b-2 -mb-px',
                      disclosureTypeTab === tab
                        ? tab === '배당' ? 'border-[#b85450] text-[#b85450]' : 'border-[#4a72a0] text-[#4a72a0]'
                        : 'border-transparent text-[#9a9080] hover:text-[#7a7060]'
                    )}>
                    {tab}
                  </button>
                ))}
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                    {['종목','유형','제목','일자','파일','상태','관리'].map(h => (
                      <th key={h} className={`px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide ${['파일','상태','관리'].includes(h) ? 'text-center' : ''}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e0dace]">
                  {filteredDisclosures.map(item => (
                    <tr key={item.id} className="hover:bg-[#f7f5f0] transition-all group">
                      <td className="px-6 py-4 text-sm font-semibold text-[#2a2820]">{item.asset}</td>
                      <td className="px-6 py-4">
                        <Badge variant={item.type === '배당' ? 'danger' : 'neutral'}>{item.type}</Badge>
                      </td>
                      <td className="px-6 py-4 text-sm font-bold text-[#2a2820]">{item.title}</td>
                      <td className="px-6 py-4 text-sm font-bold text-[#9a9080] font-mono">{item.date}</td>
                      <td className="px-6 py-4 text-center"><FileText className="w-4 h-4 text-[#b85450] mx-auto" /></td>
                      <td className="px-6 py-4 text-center">
                        <Badge variant={item.status === '승인완료' ? 'success' : 'warning'}>{item.status}</Badge>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-all">
                          <button onClick={() => handleEditDisclosure(item)} className="p-2 text-[#4a72a0] hover:bg-[#ffe65a]/20 rounded-lg transition-all"><Edit3 className="w-4 h-4" /></button>
                          <button className="p-2 text-[#b85450] hover:bg-[#fde8e8] rounded-lg transition-colors"><Trash2 className="w-4 h-4" /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Edit Disclosure Modal */}
      {editingDisclosure && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-[#e0dace] flex items-center justify-between bg-[#f7f5f0]">
              <h3 className="text-lg font-semibold text-[#2a2820]">공시 정보 수정</h3>
              <button onClick={() => setEditingDisclosure(null)} className="p-2 hover:bg-[#e0dace] rounded-xl transition-all">
                <X className="w-5 h-5 text-[#9a9080]" />
              </button>
            </div>
            <div className="p-8 space-y-6">
              {[
                { label: '공시 대상 자산', key: 'asset', readOnly: true },
                { label: '공시 제목', key: 'title', readOnly: false },
                { label: '첨부 파일 (PDF URL)', key: 'file', readOnly: false },
              ].map(({ label, key, readOnly }) => (
                <div key={key} className="space-y-1.5">
                  <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">{label}</label>
                  <input type="text" value={editForm[key]} readOnly={readOnly}
                    onChange={readOnly ? undefined : e => setEditForm({ ...editForm, [key]: e.target.value })}
                    className={cn('w-full border border-[#e0dace] rounded-xl px-4 py-3 text-sm outline-none font-bold transition-all',
                      readOnly ? 'bg-[#f7f5f0] text-[#9a9080]' : 'bg-[#f7f5f0] text-[#2a2820] focus:border-[#4a72a0]'
                    )}
                  />
                </div>
              ))}
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">상태</label>
                <select value={editForm.status} onChange={e => setEditForm({ ...editForm, status: e.target.value })}
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold appearance-none">
                  <option value="승인완료">승인완료</option>
                  <option value="검토대기">검토대기</option>
                  <option value="반려">반려</option>
                </select>
              </div>
            </div>
            <div className="p-6 bg-[#f7f5f0] border-t border-[#e0dace] flex gap-3">
              <button onClick={() => setEditingDisclosure(null)} className="flex-1 py-3 rounded-md bg-white border border-[#e0dace] text-[#9a9080] text-sm font-medium hover:bg-[#e0dace] transition-colors">취소</button>
              <button onClick={handleSaveDisclosure} className="flex-[2] py-3 rounded-md bg-[#4a72a0] text-white text-sm font-medium hover:bg-[#3a62a0] transition-colors">저장하기</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Notice Modal */}
      {isAddingNotice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-[#e0dace] flex items-center justify-between bg-[#f7f5f0]">
              <h3 className="text-lg font-semibold text-[#2a2820]">신규 공지사항 등록</h3>
              <button onClick={() => setIsAddingNotice(false)} className="p-2 hover:bg-[#e0dace] rounded-xl transition-all"><X className="w-5 h-5 text-[#9a9080]" /></button>
            </div>
            <div className="p-8 space-y-6">
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">공지 제목</label>
                <input type="text" value={noticeForm.title} onChange={e => setNoticeForm({ ...noticeForm, title: e.target.value })}
                  placeholder="공지사항 제목을 입력하세요"
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold" />
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">카테고리</label>
                <div className="flex gap-2 p-1 bg-[#e0dace] rounded-xl">
                  {['일반','시스템'].map(cat => (
                    <button key={cat} type="button" onClick={() => setNoticeForm({ ...noticeForm, category: cat })}
                      className={cn('flex-1 py-2.5 rounded-lg text-sm font-semibold transition-colors',
                        noticeForm.category === cat ? 'bg-white text-[#4a72a0] shadow-sm' : 'text-[#9a9080] hover:text-[#7a7060]'
                      )}>
                      {cat}
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">중요 공지 여부</label>
                <div className="flex items-center gap-3 h-12 px-4 bg-[#f7f5f0] border border-[#e0dace] rounded-xl">
                  <input type="checkbox" checked={noticeForm.important} onChange={e => setNoticeForm({ ...noticeForm, important: e.target.checked })} className="w-4 h-4 rounded" />
                  <span className="text-xs font-bold text-[#7a7060]">상단 고정</span>
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">공지 내용</label>
                <textarea rows={6} value={noticeForm.content} onChange={e => setNoticeForm({ ...noticeForm, content: e.target.value })}
                  placeholder="공지사항 내용을 입력하세요"
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold resize-none" />
              </div>
            </div>
            <div className="p-6 bg-[#f7f5f0] border-t border-[#e0dace] flex gap-3">
              <button onClick={() => setIsAddingNotice(false)} className="flex-1 py-3 rounded-md bg-white border border-[#e0dace] text-[#9a9080] text-sm font-medium hover:bg-[#e0dace] transition-colors">취소</button>
              <button onClick={handleAddNotice} className="flex-[2] py-3 rounded-md bg-[#4a72a0] text-white text-sm font-medium hover:bg-[#3a62a0] transition-colors">등록하기</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Disclosure Modal */}
      {isAddingDisclosure && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-[#e0dace] flex items-center justify-between bg-[#f7f5f0]">
              <h3 className="text-lg font-semibold text-[#2a2820]">신규 공시 등록</h3>
              <button onClick={() => setIsAddingDisclosure(false)} className="p-2 hover:bg-[#e0dace] rounded-xl transition-all"><X className="w-5 h-5 text-[#9a9080]" /></button>
            </div>
            <div className="p-8 space-y-6">
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">공시 대상 자산</label>
                <select value={editForm.asset} onChange={e => setEditForm({ ...editForm, asset: e.target.value })}
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold appearance-none">
                  <option value="">자산을 선택하세요</option>
                  {tokens.map(t => <option key={t.id} value={t.name}>{t.name}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">공시 유형</label>
                <div className="flex gap-2 p-1 bg-[#e0dace] rounded-xl">
                  {['배당','일반'].map(type => (
                    <button key={type} type="button" onClick={() => setEditForm({ ...editForm, type })}
                      className={cn('flex-1 py-2.5 rounded-lg text-sm font-semibold transition-colors',
                        editForm.type === type ? (type === '배당' ? 'bg-white text-[#b85450] shadow-sm' : 'bg-white text-[#4a72a0] shadow-sm') : 'text-[#9a9080] hover:text-[#7a7060]'
                      )}>
                      {type}
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">공시 제목</label>
                <input type="text" value={editForm.title} onChange={e => setEditForm({ ...editForm, title: e.target.value })}
                  placeholder="공시 제목을 입력하세요"
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold" />
              </div>
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">첨부 파일 (PDF URL)</label>
                <div className="relative">
                  <input type="text" value={editForm.file} onChange={e => setEditForm({ ...editForm, file: e.target.value })}
                    placeholder="https://example.com/report.pdf"
                    className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] font-bold pr-10" />
                  <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-[#9a9080]" />
                </div>
              </div>
            </div>
            <div className="p-6 bg-[#f7f5f0] border-t border-[#e0dace] flex gap-3">
              <button onClick={() => setIsAddingDisclosure(false)} className="flex-1 py-3 rounded-md bg-white border border-[#e0dace] text-[#9a9080] text-sm font-medium hover:bg-[#e0dace] transition-colors">취소</button>
              <button onClick={handleAddDisclosure} className="flex-[2] py-3 rounded-md bg-[#4a72a0] text-white text-sm font-medium hover:bg-[#3a62a0] transition-colors">등록하기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
