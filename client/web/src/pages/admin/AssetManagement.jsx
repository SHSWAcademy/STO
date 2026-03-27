import { useState } from 'react';
import { PlusCircle, Search, Filter, ArrowRight, FileText, Download, X } from 'lucide-react';
import { useApp } from '../../context/AppContext.jsx';
import { cn } from '../../lib/utils.js';

export function AssetManagement() {
  const { tokens, setTokens, setDisclosures } = useApp();
  const [selectedAsset, setSelectedAsset] = useState(null);
  const [isEditingAsset, setIsEditingAsset] = useState(false);
  const [isViewingAsset, setIsViewingAsset] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [editForm, setEditForm] = useState({
    name: '', symbol: '', category: '', desc: '',
    buildingValue: 0, tokenPrice: 5000, monthlyProfit: 0,
    imageUrl: '', pdfUrl: '', issued: 0, yield: 0, dividendDay: 15,
    platformAllocationType: 'percent', platformAllocationValue: 0, availableForSale: 0,
  });

  const filteredTokens = tokens.filter(t =>
    t.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    t.symbol.toLowerCase().includes(searchTerm.toLowerCase())
  );

  function openEdit(asset) {
    const buildingValue = asset.buildingValue || asset.price * asset.issued;
    const tokenPrice    = asset.tokenPrice || asset.price;
    const monthlyProfit = asset.monthlyProfit || Math.round((buildingValue * (asset.yield / 100)) / 12);
    setSelectedAsset(asset);
    setEditForm({
      ...asset, buildingValue, tokenPrice, monthlyProfit,
      imageUrl: asset.imageUrl || '',
      pdfUrl:   asset.pdfUrl   || '',
      issued:   asset.issued   || Math.floor(buildingValue / tokenPrice),
      yield:    asset.yield    || ((monthlyProfit * 12) / buildingValue) * 100,
      dividendDay: asset.dividendDay || 15,
      platformAllocationType:  asset.platformAllocationType  || 'percent',
      platformAllocationValue: asset.platformAllocationValue || 0,
      availableForSale:        asset.availableForSale        || asset.issued,
    });
    setIsEditingAsset(true);
    setIsViewingAsset(false);
  }

  function openNew() {
    setEditForm({ name: '', symbol: '', category: '', desc: '', buildingValue: 0, tokenPrice: 5000, monthlyProfit: 0, imageUrl: '', pdfUrl: '', issued: 0, yield: 0, dividendDay: 15, platformAllocationType: 'percent', platformAllocationValue: 0, availableForSale: 0 });
    setSelectedAsset({ name: '신규 자산', symbol: 'NEW' });
    setIsEditingAsset(true);
    setIsViewingAsset(false);
  }

  function openView(asset) {
    setSelectedAsset(asset);
    setIsViewingAsset(true);
    setIsEditingAsset(false);
  }

  function handleSave() {
    if (!selectedAsset) return;
    const isNew       = !selectedAsset.id;
    const newAssetId  = isNew ? `ASSET_${Date.now()}` : selectedAsset.id;
    const assetData   = { ...editForm, id: newAssetId, price: editForm.tokenPrice };

    if (isNew) {
      setTokens(prev => [...prev, assetData]);
      setDisclosures(prev => [{
        id: Date.now(), asset: assetData.name, assetId: newAssetId,
        type: '일반', title: `${assetData.name} 자산 세부사항 보고서`,
        date: new Date().toISOString().split('T')[0], file: editForm.pdfUrl, status: '승인완료',
      }, ...prev]);
    } else {
      setTokens(prev => prev.map(t => t.id === selectedAsset.id ? assetData : t));
      setDisclosures(prev => prev.map(d =>
        d.assetId === selectedAsset.id && d.title.includes('자산 세부사항 보고서')
          ? { ...d, file: editForm.pdfUrl, asset: assetData.name }
          : d
      ));
    }
    setIsEditingAsset(false);
    setSelectedAsset(null);
  }

  // ── 상세 보기 ──
  if (isViewingAsset && selectedAsset) {
    return (
      <div className="space-y-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => setIsViewingAsset(false)} className="p-2 rounded-md bg-white border border-[#e0dace] text-[#9a9080] hover:text-[#2a2820] transition-colors">
              <ArrowRight className="rotate-180 w-5 h-5" />
            </button>
            <div>
              <h2 className="text-xl font-semibold text-[#2a2820]">자산 상세 정보</h2>
              <p className="text-sm text-[#9a9080]">{selectedAsset.name} ({selectedAsset.symbol})</p>
            </div>
          </div>
          <button onClick={() => openEdit(selectedAsset)} className="px-6 py-2.5 bg-[#4a72a0] text-white text-sm font-medium rounded-md hover:bg-[#3a62a0] transition-colors">
            정보 수정하기
          </button>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          <div className="bg-white border border-[#e0dace] rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4">기본 정보</h3>
            <div className="grid grid-cols-2 gap-8">
              {[['자산명', selectedAsset.name], ['심볼', selectedAsset.symbol]].map(([l, v]) => (
                <div key={l} className="space-y-1">
                  <p className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">{l}</p>
                  <p className="text-sm font-bold text-[#2a2820]">{v}</p>
                </div>
              ))}
              <div className="space-y-1">
                <p className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">상태</p>
                <span className="px-2 py-0.5 rounded bg-[#e0f0e8] text-[#4a7a60] text-[10px] font-semibold uppercase inline-block">상장 활성</span>
              </div>
            </div>
            <div className="space-y-1">
              <p className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">자산 설명</p>
              <p className="text-sm text-[#7a7060] leading-relaxed">{selectedAsset.desc}</p>
            </div>
            {selectedAsset.pdfUrl && (
              <div className="pt-4 border-t border-[#e0dace]">
                <p className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest mb-2">세부 정보 보고서</p>
                <a href={selectedAsset.pdfUrl} target="_blank" rel="noopener noreferrer"
                  className="flex items-center gap-3 p-3 rounded-xl bg-[#f7f5f0] border border-[#e0dace] hover:bg-[#e0dace] transition-all group">
                  <div className="p-2 bg-[#fde8e8] rounded-lg text-[#b04040]"><FileText size={16} /></div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-bold text-[#2a2820] truncate">{selectedAsset.name} 세부사항 보고서.pdf</p>
                    <p className="text-[10px] text-[#9a9080]">PDF Document</p>
                  </div>
                  <Download size={14} className="text-[#9a9080] group-hover:text-[#4a72a0]" />
                </a>
              </div>
            )}
          </div>

          <div className="bg-white border border-[#e0dace] rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4">금융 및 발행 정보</h3>
            <div className="grid grid-cols-2 gap-8">
              {[
                ['건물 가치', `₩${(selectedAsset.price * selectedAsset.issued).toLocaleString()}`],
                ['토큰 가격', `₩${selectedAsset.price.toLocaleString()}`],
                ['총 발행량', `${selectedAsset.issued.toLocaleString()} ST`],
                ['연간 수익률', `${selectedAsset.yield}%`],
              ].map(([l, v]) => (
                <div key={l} className="space-y-1">
                  <p className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">{l}</p>
                  <p className="text-lg font-semibold text-[#2a2820]">{v}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── 편집 뷰 ──
  if (isEditingAsset && selectedAsset) {
    return (
      <div className="space-y-8">
        <div className="flex items-center gap-4">
          <button onClick={() => setIsEditingAsset(false)} className="p-2 rounded-md bg-white border border-[#e0dace] text-[#9a9080] hover:text-[#2a2820] transition-colors">
            <ArrowRight className="rotate-180 w-5 h-5" />
          </button>
          <div>
            <h2 className="text-xl font-semibold text-[#2a2820]">자산 정보 수정</h2>
            <p className="text-sm text-[#9a9080]">{selectedAsset.name}</p>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          <div className="bg-white border border-[#e0dace] rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4">기본 정보</h3>
            {[
              { label: '자산명', key: 'name', placeholder: '강남 오피스 빌딩 A' },
              { label: '자산코드', key: 'symbol', placeholder: 'GN_A' },
            ].map(({ label, key, placeholder }) => (
              <div key={key} className="space-y-1.5">
                <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">{label}</label>
                <input
                  type="text" value={editForm[key]} placeholder={placeholder}
                  onChange={e => setEditForm(f => ({ ...f, [key]: e.target.value }))}
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-md px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] transition-colors font-medium"
                />
              </div>
            ))}
            <div className="space-y-1.5">
              <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">세부 정보 PDF URL</label>
              <div className="relative">
                <input type="text" value={editForm.pdfUrl} placeholder="https://example.com/report.pdf"
                  onChange={e => setEditForm(f => ({ ...f, pdfUrl: e.target.value }))}
                  className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#4a72a0] transition-all font-bold pr-10"
                />
                <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-[#9a9080]" />
              </div>
            </div>
          </div>

          <div className="space-y-8">
            <div className="bg-white border border-[#e0dace] rounded-lg p-8 space-y-6">
              <h3 className="text-sm font-semibold text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4">금융 정보 (읽기 전용)</h3>
              <div className="grid grid-cols-2 gap-4">
                {[
                  ['토큰 가격', `₩${editForm.tokenPrice.toLocaleString()}`],
                  ['총 발행량', `${editForm.issued.toLocaleString()} ST`],
                  ['연간 수익률', `${(editForm.yield || 0).toFixed(1)}%`],
                  ['배당지급일', `매월 ${editForm.dividendDay}일`],
                ].map(([l, v]) => (
                  <div key={l} className="space-y-1.5">
                    <label className="text-[10px] font-semibold text-[#9a9080] uppercase tracking-widest">{l}</label>
                    <input type="text" value={v} readOnly className="w-full bg-[#e0dace] border border-[#e0dace] rounded-md px-4 py-3 text-sm text-[#9a9080] outline-none font-medium cursor-not-allowed" />
                  </div>
                ))}
              </div>
            </div>
            <div className="flex gap-4">
              <button onClick={() => setIsEditingAsset(false)} className="flex-1 py-4 rounded-md bg-white text-[#9a9080] text-sm font-medium hover:bg-[#f7f5f0] transition-colors border border-[#e0dace]">
                취소
              </button>
              <button onClick={handleSave} className="flex-[2] py-4 rounded-md bg-[#4a72a0] text-white text-sm font-medium hover:bg-[#3a62a0] transition-colors">
                변경사항 저장
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── 목록 뷰 ──
  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#2a2820]">자산 관리</h1>
          <p className="text-sm text-[#9a9080]">플랫폼에 등록된 STO 자산을 관리하고 신규 자산을 등록합니다.</p>
        </div>
        <button onClick={openNew} className="flex items-center gap-2 px-6 py-3 bg-[#4a72a0] text-white text-sm font-medium rounded-md hover:bg-[#3a62a0] transition-colors">
          <PlusCircle className="w-5 h-5" /> 신규 자산 등록
        </button>
      </div>

      <div className="bg-white rounded-lg border border-[#e0dace] overflow-hidden">
        <div className="p-6 border-b border-[#e0dace] flex items-center justify-between bg-[#f7f5f0]">
          <div className="flex items-center gap-3 bg-white px-4 py-3 rounded-md border border-[#e0dace] focus-within:border-[#4a72a0] transition-colors">
            <Search className="w-5 h-5 text-[#9a9080]" />
            <input type="text" placeholder="자산명 검색..." value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="bg-transparent border-none outline-none text-sm w-64 font-bold text-[#2a2820]"
            />
          </div>
          <button className="p-3 bg-white border border-[#e0dace] rounded-md text-[#7a7060] hover:bg-[#f7f5f0] transition-colors">
            <Filter className="w-5 h-5" />
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                <th className="px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide">자산 정보</th>
                <th className="px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide text-right">총 발행량</th>
                <th className="px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide text-right">현재가</th>
                <th className="px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide text-center">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {filteredTokens.map(t => (
                <tr key={t.id} className="hover:bg-[#f7f5f0] transition-all cursor-pointer group" onClick={() => openView(t)}>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-[#f7f5f0] border border-[#e0dace] flex items-center justify-center text-xs font-black text-[#9a9080]">
                        {t.symbol.slice(0, 2)}
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-[#2a2820]">{t.name}</p>
                        <p className="text-[10px] font-mono font-bold text-[#9a9080]">{t.symbol}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-mono font-bold text-[#7a7060]">
                    {t.issued.toLocaleString()} ST
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-black text-[#2a2820]">
                    ₩{t.price.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <span className="px-2 py-1 rounded-md bg-[#e0f0e8] text-[#4a7a60] text-[10px] font-semibold">상장</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
