import { useState } from 'react';
import { DollarSign, PlusCircle, ArrowRight, FileText, CheckCircle2, AlertCircle, PieChart } from 'lucide-react';
import { TOKENS } from '../../data/mock.js';
import { cn } from '../../lib/utils.js';

const MOCK_DIVIDEND_HISTORY = [
  { id: 1, assetName: '서울강남빌딩',   date: '2026-03-15', amount: 48000000, status: 'completed', cycle: '2026-02', residual: 1250, tax: 7392000, fee: 24000, netAmount: 40584000 },
  { id: 2, assetName: '송도 리조트',    date: '2026-03-15', amount: 32000000, status: 'completed', cycle: '2026-02', residual: 840,  tax: 4928000, fee: 16000, netAmount: 27056000 },
  { id: 3, assetName: '서울강남빌딩',   date: '2026-02-15', amount: 47500000, status: 'completed', cycle: '2026-01', residual: 1100, tax: 7315000, fee: 23750, netAmount: 40161250 },
  { id: 4, assetName: '송도 리조트',    date: '2026-02-15', amount: 31500000, status: 'completed', cycle: '2026-01', residual: 720,  tax: 4851000, fee: 15750, netAmount: 26633250 },
];

// 잔여금 정적 값 (Math.random 제거)
const RESIDUAL_MAP = { SEOULST: 1250, SONGDORE: 840, ARTPRIME: 320, JEJU1: 560, LOGISHUB: 420, SOLAR1: 180 };

export function DividendManagement() {
  const now   = new Date();
  const year  = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const fixedBaseDate    = `${year}-${month}-10`;
  const fixedPaymentDate = `${year}-${month}-20`;

  const [selectedAsset,   setSelectedAsset]   = useState(null);
  const [isViewingDetail, setIsViewingDetail] = useState(false);
  const [filterStatus,    setFilterStatus]    = useState('all');
  const [registeredAssets, setRegisteredAssets] = useState(new Set(['SEOULST', 'SONGDORE']));
  const [form, setForm] = useState({ monthlyProfit: '', amountPerToken: '', pdfUrl: '' });

  function openDetail(token) {
    setSelectedAsset(token);
    setForm({ monthlyProfit: '', amountPerToken: '', pdfUrl: '' });
    setIsViewingDetail(true);
  }

  function handleRegister() {
    if (selectedAsset) {
      setRegisteredAssets(prev => new Set([...prev, selectedAsset.id]));
    }
    alert('배당 스케줄 및 세부 내역이 성공적으로 등록되었습니다.');
    setForm({ monthlyProfit: '', amountPerToken: '', pdfUrl: '' });
  }

  // ── 상세 보기 ──
  if (isViewingDetail && selectedAsset) {
    return (
      <div className="space-y-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => setIsViewingDetail(false)} className="p-2 rounded-xl bg-white border border-[#e0dace] text-[#9a9080] hover:text-[#2a2820] transition-all">
              <ArrowRight className="rotate-180 w-5 h-5" />
            </button>
            <div>
              <h2 className="text-xl font-black text-[#2a2820]">배당 스케줄 및 내역 관리</h2>
              <p className="text-sm font-bold text-[#9a9080]">{selectedAsset.name} ({selectedAsset.symbol})</p>
            </div>
          </div>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-1 bg-white border border-[#e0dace] rounded-2xl p-8 space-y-6 shadow-sm">
            <h3 className="text-sm font-black text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4 flex items-center gap-2">
              <PlusCircle size={16} className="text-[#b85450]" /> 신규 배당 스케줄 등록
            </h3>
            <div className="space-y-4">
              {[['배당 기준일 (고정)', fixedBaseDate, 'date'], ['지급 예정일 (고정)', fixedPaymentDate, 'date']].map(([label, value]) => (
                <div key={label} className="space-y-1.5">
                  <label className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest ml-1">{label}</label>
                  <input type="date" value={value} readOnly
                    className="w-full bg-[#e0dace] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#9a9080] outline-none cursor-not-allowed font-bold"
                  />
                </div>
              ))}

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest ml-1">월 수익 (KRW)</label>
                  <input type="number" placeholder="0" value={form.monthlyProfit}
                    onChange={e => {
                      const val = e.target.value;
                      const amount = Math.round((parseFloat(val) || 0) / selectedAsset.issued);
                      setForm({ ...form, monthlyProfit: val, amountPerToken: amount.toString() });
                    }}
                    className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#b85450] transition-all font-bold"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest ml-1">1주당 배당금</label>
                  <input type="number" readOnly value={form.amountPerToken}
                    className="w-full bg-[#f7f5f0]/50 border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#9a9080] outline-none font-mono font-bold"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest ml-1">배당 세부 내역 (PDF URL)</label>
                <div className="relative">
                  <input type="text" placeholder="https://example.com/dividend.pdf" value={form.pdfUrl}
                    onChange={e => setForm({ ...form, pdfUrl: e.target.value })}
                    className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] outline-none focus:border-[#b85450] transition-all font-bold pr-10"
                  />
                  <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-[#9a9080]" />
                </div>
              </div>

              <button onClick={handleRegister}
                className="w-full py-4 bg-[#b85450] text-white text-xs font-black uppercase tracking-widest rounded-2xl shadow-lg shadow-[#b85450]/20 hover:opacity-90 transition-all mt-4">
                스케줄 및 내역 등록하기
              </button>
            </div>
          </div>

          <div className="lg:col-span-2 bg-white border border-[#e0dace] rounded-2xl p-8 space-y-6 shadow-sm">
            <div className="flex items-center justify-between border-b border-[#e0dace] pb-4">
              <h3 className="text-sm font-black text-[#2a2820] uppercase tracking-widest">배당 지급 및 스케줄 이력</h3>
              <div className="flex gap-2">
                <span className="px-2 py-0.5 rounded bg-[#e0f0e8] text-[#4a7a60] text-[9px] font-black uppercase">지급완료</span>
                <span className="px-2 py-0.5 rounded bg-[#ffe65a]/30 text-[#c9a84c] text-[9px] font-black uppercase">예정</span>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead>
                  <tr className="text-[#9a9080] border-b border-[#e0dace]">
                    {['정산월','지급(예정)일','지급 총액','증빙자료','상태'].map(h => (
                      <th key={h} className={`py-4 font-bold uppercase tracking-widest ${h === '지급 총액' ? 'text-right' : h === '증빙자료' || h === '상태' ? 'text-center' : 'text-left'}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e0dace]">
                  {MOCK_DIVIDEND_HISTORY.filter(h => h.assetName === selectedAsset.name).map(h => (
                    <tr key={h.id} className="hover:bg-[#f7f5f0] transition-colors">
                      <td className="py-4 font-bold text-[#2a2820]">{h.cycle}</td>
                      <td className="py-4 font-mono text-[#9a9080]">{h.date}</td>
                      <td className="py-4 text-right font-black text-[#2a2820]">₩{h.amount.toLocaleString()}</td>
                      <td className="py-4 text-center">
                        <button className="p-2 text-[#b85450] hover:bg-[#4a3232] rounded-lg transition-all"><FileText size={16} /></button>
                      </td>
                      <td className="py-4 text-center">
                        <span className="px-2 py-0.5 rounded bg-[#e0f0e8] text-[#4a7a60] text-[9px] font-black uppercase">완료</span>
                      </td>
                    </tr>
                  ))}
                  {MOCK_DIVIDEND_HISTORY.filter(h => h.assetName === selectedAsset.name).length === 0 && (
                    <tr><td colSpan={5} className="py-8 text-center text-sm text-[#9a9080] font-bold">배당 내역이 없습니다.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── 목록 뷰 ──
  const filtered = TOKENS.filter(t => {
    if (filterStatus === 'registered')   return registeredAssets.has(t.id);
    if (filterStatus === 'unregistered') return !registeredAssets.has(t.id);
    return true;
  });

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-[#2a2820]">배당금 관리</h1>
          <p className="text-sm font-bold text-[#9a9080]">STO 자산별 배당금 정산 및 지급 내역을 관리합니다.</p>
        </div>
        <div className="flex items-center gap-5 bg-white border border-[#e0dace] rounded-2xl px-6 py-3 shadow-sm">
          <div className="flex flex-col items-center justify-center bg-[#b85450] text-white rounded-xl w-14 h-14 shadow-lg shadow-[#b85450]/20">
            <span className="text-[10px] font-black uppercase leading-none mb-1">{year}</span>
            <span className="text-2xl font-black leading-none">{month}</span>
          </div>
          <div>
            <h3 className="text-lg font-black text-[#2a2820] leading-tight">정산 대상 월</h3>
            <p className="text-xs font-bold text-[#9a9080]">이번 달 배당 정산 현황입니다.</p>
          </div>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex items-center gap-4 p-2 bg-[#f7f5f0] border border-[#e0dace] rounded-2xl w-fit">
        {[
          { id: 'all',          label: '전체 자산',     value: TOKENS.length,                      icon: PieChart,    color: 'text-[#c9a84c]', bg: 'bg-[#ffe65a]/30' },
          { id: 'registered',   label: '당월 등록 완료', value: registeredAssets.size,               icon: CheckCircle2, color: 'text-[#4a7a60]', bg: 'bg-[#e0f0e8]' },
          { id: 'unregistered', label: '당월 미등록',    value: TOKENS.length - registeredAssets.size, icon: AlertCircle,  color: 'text-[#b85450]', bg: 'bg-[#4a3232]' },
        ].map(stat => (
          <button key={stat.id} onClick={() => setFilterStatus(stat.id)}
            className={cn(
              'flex items-center gap-4 px-8 py-4 rounded-2xl transition-all text-left min-w-[200px]',
              filterStatus === stat.id
                ? 'bg-white shadow-md border border-[#e0dace]'
                : 'hover:bg-white/50 border border-transparent'
            )}
          >
            <div className={cn('p-3 rounded-xl', stat.bg)}>
              <stat.icon className={cn('w-5 h-5', stat.color)} />
            </div>
            <div>
              <p className="text-xs font-black text-[#9a9080] uppercase tracking-wider mb-1">{stat.label}</p>
              <h3 className="text-xl font-black text-[#2a2820]">{stat.value}</h3>
            </div>
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-[#e0dace] shadow-sm overflow-hidden">
        <div className="p-6 border-b border-[#e0dace] flex items-center justify-between bg-[#f0ede4]">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-black text-[#2a2820]">배당 및 정산 현황</h3>
            <div className="px-2 py-0.5 rounded bg-[#4a3232] text-[#b85450] text-[10px] font-black">
              기준일 D-{Math.max(0, 10 - now.getDate())}
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                {['자산 정보','등록 상태','월 수익 (KRW)','누적 잔여금','관리'].map(h => (
                  <th key={h} className={`px-6 py-4 text-[10px] font-black text-[#9a9080] uppercase tracking-wider ${h === '월 수익 (KRW)' || h === '누적 잔여금' ? 'text-right' : h === '등록 상태' || h === '관리' ? 'text-center' : ''}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {filtered.map(t => {
                const buildingValue  = t.price * t.issued;
                const monthlyProfit  = Math.round(buildingValue * (t.yield / 100) / 12);
                const isRegistered   = registeredAssets.has(t.id);
                const daysLeft       = 10 - now.getDate();
                const isUrgent       = !isRegistered && daysLeft <= 3;
                const residual       = RESIDUAL_MAP[t.id] ?? 500;

                return (
                  <tr key={t.id} className="hover:bg-[#f7f5f0] transition-all cursor-pointer group" onClick={() => openDetail(t)}>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-xl bg-[#f7f5f0] border border-[#e0dace] flex items-center justify-center text-xs font-black text-[#9a9080]">
                          {t.symbol.slice(0, 2)}
                        </div>
                        <div>
                          <p className="text-sm font-black text-[#2a2820] group-hover:text-[#b85450] transition-colors">{t.name}</p>
                          <p className="text-[10px] font-mono font-bold text-[#9a9080]">{t.symbol}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      {isRegistered ? (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#e0f0e8] text-[#4a7a60] text-[10px] font-black uppercase tracking-wider">
                          <CheckCircle2 size={12} /> 등록 완료
                        </div>
                      ) : (
                        <div className={cn(
                          'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider',
                          isUrgent ? 'bg-[#4a3232] text-[#b85450]' : 'bg-[#f7f5f0] text-[#9a9080]'
                        )}>
                          <AlertCircle size={12} /> 미등록
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-mono font-bold text-[#7a7060]">₩{monthlyProfit.toLocaleString()}</td>
                    <td className="px-6 py-4 text-right text-sm font-mono font-bold text-[#b85450]">₩{residual.toLocaleString()}</td>
                    <td className="px-6 py-4 text-center" onClick={e => e.stopPropagation()}>
                      <button onClick={() => openDetail(t)}
                        className="px-4 py-1.5 rounded-lg bg-white border border-[#e0dace] text-[10px] font-black text-[#9a9080] hover:text-[#b85450] hover:border-[#b85450] transition-all">
                        상세보기
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
