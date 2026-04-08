import { useState } from 'react';
import { FileText, PlusCircle, Filter, AlertCircle, DollarSign, Edit3, Trash2 } from 'lucide-react';
import { useApp } from '../../context/AppContext.jsx';
import { cn } from '../../lib/utils.js';
import { SearchInput } from '../../components/ui/SearchInput.jsx';
import { Badge } from '../../components/ui/Badge.jsx';
import { Modal } from '../../components/ui/Modal.jsx';

export function ContentManagement() {
  const { disclosures, setDisclosures, tokens } = useApp();
  const [disclosureFilter, setDisclosureFilter] = useState('all');
  const [disclosureTypeTab, setDisclosureTypeTab] = useState('전체');
  const [editingDisclosure, setEditingDisclosure] = useState(null);
  const [isAddingDisclosure, setIsAddingDisclosure] = useState(false);
  const [searchTerm, setSearchTerm]               = useState('');
  const [editForm, setEditForm] = useState({ title: '', file: '', status: '', asset: '', type: '일반' });

  function handleEditDisclosure(d) {
    setEditingDisclosure(d);
    setEditForm({ title: d.title, file: d.file, status: d.status, asset: d.asset, type: d.type });
  }

  function handleSaveDisclosure() {
    if (!editingDisclosure) return;
    setDisclosures(prev => prev.map(d => d.id === editingDisclosure.id ? { ...d, ...editForm } : d));
    setEditingDisclosure(null);
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

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">공시 관리</h1>
          <p className="text-sm text-stone-400">자산별 공시 내역을 관리합니다.</p>
        </div>
        <button
          onClick={() => setIsAddingDisclosure(true)}
          className="flex items-center gap-2 px-6 py-3 bg-brand-blue text-white text-sm font-medium rounded-md hover:bg-brand-blue-dk transition-colors"
        >
          <PlusCircle className="w-5 h-5" />
          신규 공시 등록
        </button>
      </div>
      <div className="space-y-6">
          {/* Disclosure filter cards */}
          <div className="flex items-center gap-4 p-2 bg-stone-100 border border-stone-200 rounded-lg w-fit">
            {[
              { id: 'all',      label: '총 공시 건수',    value: disclosures.length,                             icon: FileText,    color: 'text-stone-500', bg: 'bg-stone-200' },
              { id: 'dividend', label: '이번 달 배당 공시', value: disclosures.filter(d => d.type === '배당').length, icon: DollarSign,  color: 'text-stone-600', bg: 'bg-stone-200' },
              { id: 'pending',  label: '검토 대기',        value: disclosures.filter(d => d.status === '검토대기').length, icon: AlertCircle, color: 'text-brand-red', bg: 'bg-brand-red-light' },
            ].map(stat => (
              <button key={stat.id} onClick={() => setDisclosureFilter(stat.id)}
                className={cn('flex items-center gap-4 px-8 py-4 rounded-lg transition-colors text-left min-w-[200px]',
                  disclosureFilter === stat.id ? 'bg-white border border-stone-200' : 'hover:bg-white/50 border border-transparent'
                )}>
                <div className={cn('p-3 rounded-xl', stat.bg)}>
                  <stat.icon className={cn('w-5 h-5', stat.color)} />
                </div>
                <div>
                  <p className="text-xs font-semibold text-stone-400 uppercase tracking-wide mb-1">{stat.label}</p>
                  <h3 className="text-xl font-semibold text-stone-800">{stat.value}건</h3>
                </div>
              </button>
            ))}
          </div>

          <div className="bg-white rounded-lg border border-stone-200 overflow-hidden">
            <div className="px-8 pt-6 pb-0 border-b border-stone-200 bg-stone-100 space-y-4">
              <div className="flex items-center justify-between">
                <SearchInput variant="light" value={searchTerm} onChange={setSearchTerm} placeholder="공시 내용 검색..." />
                <button className="p-3 bg-white border border-stone-200 rounded-md text-stone-500 hover:bg-stone-100 transition-all">
                  <Filter className="w-5 h-5" />
                </button>
              </div>
              <div className="flex gap-1">
                {['전체', '배당', '일반'].map(tab => (
                  <button key={tab} onClick={() => setDisclosureTypeTab(tab)}
                    className={cn('px-5 py-2.5 text-sm font-semibold transition-colors border-b-2 -mb-px',
                      disclosureTypeTab === tab
                        ? tab === '배당' ? 'border-brand-red text-brand-red' : 'border-brand-blue text-brand-blue'
                        : 'border-transparent text-stone-400 hover:text-stone-500'
                    )}>
                    {tab}
                  </button>
                ))}
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-stone-100 border-b border-stone-200">
                    {['종목','유형','제목','일자','파일','상태','관리'].map(h => (
                      <th key={h} className={`px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide ${['파일','상태','관리'].includes(h) ? 'text-center' : ''}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-200">
                  {filteredDisclosures.map(item => (
                    <tr key={item.id} className="hover:bg-stone-100 transition-all group">
                      <td className="px-6 py-4 text-sm font-semibold text-stone-800">{item.asset}</td>
                      <td className="px-6 py-4">
                        <Badge variant={item.type === '배당' ? 'danger' : 'neutral'}>{item.type}</Badge>
                      </td>
                      <td className="px-6 py-4 text-sm font-bold text-stone-800">{item.title}</td>
                      <td className="px-6 py-4 text-sm font-bold text-stone-400 font-mono">{item.date}</td>
                      <td className="px-6 py-4 text-center"><FileText className="w-4 h-4 text-brand-red mx-auto" /></td>
                      <td className="px-6 py-4 text-center">
                        <Badge variant={item.status === '승인완료' ? 'success' : 'warning'}>{item.status}</Badge>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-all">
                          <button onClick={() => handleEditDisclosure(item)} className="p-2 text-brand-blue hover:bg-stone-100 rounded-lg transition-all"><Edit3 className="w-4 h-4" /></button>
                          <button className="p-2 text-brand-red hover:bg-brand-red-light rounded-lg transition-colors"><Trash2 className="w-4 h-4" /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

      {/* Edit Disclosure Modal */}
      <Modal isOpen={!!editingDisclosure} onClose={() => setEditingDisclosure(null)} title="공시 정보 수정" maxWidth="max-w-lg">
        <div className="p-8 space-y-6">
          {[
            { label: '공시 대상 자산', key: 'asset', readOnly: true },
            { label: '공시 제목', key: 'title', readOnly: false },
            { label: '첨부 파일 (PDF URL)', key: 'file', readOnly: false },
          ].map(({ label, key, readOnly }) => (
            <div key={key} className="space-y-1.5">
              <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">{label}</label>
              <input type="text" value={editForm[key]} readOnly={readOnly}
                onChange={readOnly ? undefined : e => setEditForm({ ...editForm, [key]: e.target.value })}
                className={cn('w-full border border-stone-200 rounded-xl px-4 py-3 text-sm outline-none font-bold transition-all',
                  readOnly ? 'bg-stone-100 text-stone-400' : 'bg-stone-100 text-stone-800 focus:border-brand-blue'
                )}
              />
            </div>
          ))}
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">상태</label>
            <select value={editForm.status} onChange={e => setEditForm({ ...editForm, status: e.target.value })}
              className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-blue font-bold appearance-none">
              <option value="승인완료">승인완료</option>
              <option value="검토대기">검토대기</option>
              <option value="반려">반려</option>
            </select>
          </div>
        </div>
        <div className="p-6 bg-stone-100 border-t border-stone-200 flex gap-3">
          <button onClick={() => setEditingDisclosure(null)} className="flex-1 py-3 rounded-md bg-white border border-stone-200 text-stone-400 text-sm font-medium hover:bg-stone-200 transition-colors">취소</button>
          <button onClick={handleSaveDisclosure} className="flex-[2] py-3 rounded-md bg-brand-blue text-white text-sm font-medium hover:bg-brand-blue-dk transition-colors">저장하기</button>
        </div>
      </Modal>

      {/* Add Disclosure Modal */}
      <Modal isOpen={isAddingDisclosure} onClose={() => setIsAddingDisclosure(false)} title="신규 공시 등록" maxWidth="max-w-lg">
        <div className="p-8 space-y-6">
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">공시 대상 자산</label>
            <select value={editForm.asset} onChange={e => setEditForm({ ...editForm, asset: e.target.value })}
              className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-blue font-bold appearance-none">
              <option value="">자산을 선택하세요</option>
              {tokens.map(t => <option key={t.id} value={t.name}>{t.name}</option>)}
            </select>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">공시 유형</label>
            <div className="flex gap-2 p-1 bg-stone-200 rounded-xl">
              {['배당','일반'].map(type => (
                <button key={type} type="button" onClick={() => setEditForm({ ...editForm, type })}
                  className={cn('flex-1 py-2.5 rounded-lg text-sm font-semibold transition-colors',
                    editForm.type === type ? (type === '배당' ? 'bg-white text-brand-red shadow-sm' : 'bg-white text-brand-blue shadow-sm') : 'text-stone-400 hover:text-stone-500'
                  )}>
                  {type}
                </button>
              ))}
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">공시 제목</label>
            <input type="text" value={editForm.title} onChange={e => setEditForm({ ...editForm, title: e.target.value })}
              placeholder="공시 제목을 입력하세요"
              className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-blue font-bold" />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest">첨부 파일 (PDF URL)</label>
            <div className="relative">
              <input type="text" value={editForm.file} onChange={e => setEditForm({ ...editForm, file: e.target.value })}
                placeholder="https://example.com/report.pdf"
                className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-blue font-bold pr-10" />
              <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
            </div>
          </div>
        </div>
        <div className="p-6 bg-stone-100 border-t border-stone-200 flex gap-3">
          <button onClick={() => setIsAddingDisclosure(false)} className="flex-1 py-3 rounded-md bg-white border border-stone-200 text-stone-400 text-sm font-medium hover:bg-stone-200 transition-colors">취소</button>
          <button onClick={handleAddDisclosure} className="flex-[2] py-3 rounded-md bg-brand-blue text-white text-sm font-medium hover:bg-brand-blue-dk transition-colors">등록하기</button>
        </div>
      </Modal>
    </div>
  );
}
