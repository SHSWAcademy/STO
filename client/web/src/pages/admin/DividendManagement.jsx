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
            <button onClick={() => setIsViewingDetail(false)} className="p-2 rounded-md bg-white border border-stone-200 text-stone-400 hover:text-stone-800 transition-colors">
              <ArrowRight className="rotate-180 w-5 h-5" />
            </button>
            <div>
              <h2 className="text-xl font-semibold text-stone-800">배당 스케줄 및 내역 관리</h2>
              <p className="text-sm text-stone-400">{selectedAsset.name} ({selectedAsset.symbol})</p>
            </div>
          </div>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-1 bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4 flex items-center gap-2">
              <PlusCircle size={16} className="text-brand-red" /> 신규 배당 스케줄 등록
            </h3>
            <div className="space-y-4">
              {[['배당 기준일 (고정)', fixedBaseDate, 'date'], ['지급 예정일 (고정)', fixedPaymentDate, 'date']].map(([label, value]) => (
                <div key={label} className="space-y-1.5">
                  <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest ml-1">{label}</label>
                  <input type="date" value={value} readOnly
                    className="w-full bg-stone-200 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-400 outline-none cursor-not-allowed font-medium"
                  />
                </div>
              ))}

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest ml-1">월 수익 (KRW)</label>
                  <input type="number" placeholder="0" value={form.monthlyProfit}
                    onChange={e => {
                      const val = e.target.value;
                      const amount = Math.round((parseFloat(val) || 0) / selectedAsset.issued);
                      setForm({ ...form, monthlyProfit: val, amountPerToken: amount.toString() });
                    }}
                    className="w-full bg-stone-100 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-red transition-colors font-medium"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest ml-1">1주당 배당금</label>
                  <input type="number" readOnly value={form.amountPerToken}
                    className="w-full bg-stone-100/50 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-400 outline-none font-mono font-medium"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest ml-1">배당 세부 내역 (PDF URL)</label>
                <div className="relative">
                  <input type="text" placeholder="https://example.com/dividend.pdf" value={form.pdfUrl}
                    onChange={e => setForm({ ...form, pdfUrl: e.target.value })}
                    className="w-full bg-stone-100 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-red transition-colors font-medium pr-10"
                  />
                  <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400" />
                </div>
              </div>

              <button onClick={handleRegister}
                className="w-full py-4 bg-brand-red text-white text-xs font-semibold uppercase tracking-widest rounded-md hover:bg-[#a04040] transition-colors mt-4">
                스케줄 및 내역 등록하기
              </button>
            </div>
          </div>

          <div className="lg:col-span-2 bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <div className="flex items-center justify-between border-b border-stone-200 pb-4">
              <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest">배당 지급 및 스케줄 이력</h3>
              <div className="flex gap-2">
                <span className="px-2 py-0.5 rounded bg-brand-green-light text-brand-green text-[9px] font-semibold uppercase">지급완료</span>
                <span className="px-2 py-0.5 rounded bg-stone-200 text-stone-500 text-[9px] font-semibold uppercase">예정</span>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead>
                  <tr className="text-stone-400 border-b border-stone-200">
                    {['정산월','지급(예정)일','지급 총액','증빙자료','상태'].map(h => (
                      <th key={h} className={`py-4 font-bold uppercase tracking-widest ${h === '지급 총액' ? 'text-right' : h === '증빙자료' || h === '상태' ? 'text-center' : 'text-left'}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-200">
                  {MOCK_DIVIDEND_HISTORY.filter(h => h.assetName === selectedAsset.name).map(h => (
                    <tr key={h.id} className="hover:bg-stone-100 transition-colors">
                      <td className="py-4 font-bold text-stone-800">{h.cycle}</td>
                      <td className="py-4 font-mono text-stone-400">{h.date}</td>
                      <td className="py-4 text-right font-black text-stone-800">₩{h.amount.toLocaleString()}</td>
                      <td className="py-4 text-center">
                        <button className="p-2 text-brand-red hover:bg-brand-red-light rounded-lg transition-colors"><FileText size={16} /></button>
                      </td>
                      <td className="py-4 text-center">
                        <span className="px-2 py-0.5 rounded bg-brand-green-light text-brand-green text-[9px] font-semibold uppercase">완료</span>
                      </td>
                    </tr>
                  ))}
                  {MOCK_DIVIDEND_HISTORY.filter(h => h.assetName === selectedAsset.name).length === 0 && (
                    <tr><td colSpan={5} className="py-8 text-center text-sm text-stone-400 font-bold">배당 내역이 없습니다.</td></tr>
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
          <h1 className="text-2xl font-semibold text-stone-800">배당금 관리</h1>
          <p className="text-sm text-stone-400">STO 자산별 배당금 정산 및 지급 내역을 관리합니다.</p>
        </div>
        <div className="flex items-center gap-5 bg-white border border-stone-200 rounded-lg px-6 py-3">
          <div className="flex flex-col items-center justify-center bg-brand-red text-white rounded-md w-14 h-14">
            <span className="text-[10px] font-black uppercase leading-none mb-1">{year}</span>
            <span className="text-2xl font-black leading-none">{month}</span>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-stone-800 leading-tight">정산 대상 월</h3>
            <p className="text-xs font-bold text-stone-400">이번 달 배당 정산 현황입니다.</p>
          </div>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex items-center gap-4 p-2 bg-stone-100 border border-stone-200 rounded-lg w-fit">
        {[
          { id: 'all',          label: '전체 자산',     value: TOKENS.length,                      icon: PieChart,    color: 'text-stone-600', bg: 'bg-stone-200' },
          { id: 'registered',   label: '당월 등록 완료', value: registeredAssets.size,               icon: CheckCircle2, color: 'text-brand-green', bg: 'bg-brand-green-light' },
          { id: 'unregistered', label: '당월 미등록',    value: TOKENS.length - registeredAssets.size, icon: AlertCircle,  color: 'text-brand-red', bg: 'bg-brand-red-light' },
        ].map(stat => (
          <button key={stat.id} onClick={() => setFilterStatus(stat.id)}
            className={cn(
              'flex items-center gap-4 px-8 py-4 rounded-lg transition-colors text-left min-w-[200px]',
              filterStatus === stat.id
                ? 'bg-white border border-stone-200'
                : 'hover:bg-white/50 border border-transparent'
            )}
          >
            <div className={cn('p-3 rounded-xl', stat.bg)}>
              <stat.icon className={cn('w-5 h-5', stat.color)} />
            </div>
            <div>
              <p className="text-xs font-semibold text-stone-400 uppercase tracking-wide mb-1">{stat.label}</p>
              <h3 className="text-xl font-semibold text-stone-800">{stat.value}</h3>
            </div>
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg border border-stone-200 overflow-hidden">
        <div className="p-6 border-b border-stone-200 flex items-center justify-between bg-stone-100">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-stone-800">배당 및 정산 현황</h3>
            <div className="px-2 py-0.5 rounded bg-brand-red-light text-brand-red-dk text-[10px] font-semibold">
              기준일 D-{Math.max(0, 10 - now.getDate())}
            </div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-stone-100 border-b border-stone-200">
                {['자산 정보','등록 상태','월 수익 (KRW)','누적 잔여금','관리'].map(h => (
                  <th key={h} className={`px-6 py-4 text-[10px] font-semibold text-stone-400 uppercase tracking-wide ${h === '월 수익 (KRW)' || h === '누적 잔여금' ? 'text-right' : h === '등록 상태' || h === '관리' ? 'text-center' : ''}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {filtered.map(t => {
                const buildingValue  = t.price * t.issued;
                const monthlyProfit  = Math.round(buildingValue * (t.yield / 100) / 12);
                const isRegistered   = registeredAssets.has(t.id);
                const daysLeft       = 10 - now.getDate();
                const isUrgent       = !isRegistered && daysLeft <= 3;
                const residual       = RESIDUAL_MAP[t.id] ?? 500;

                return (
                  <tr key={t.id} className="hover:bg-stone-100 transition-all cursor-pointer group" onClick={() => openDetail(t)}>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-xl bg-stone-100 border border-stone-200 flex items-center justify-center text-xs font-black text-stone-400">
                          {t.symbol.slice(0, 2)}
                        </div>
                        <div>
                          <p className="text-sm font-black text-stone-800 group-hover:text-brand-red transition-colors">{t.name}</p>
                          <p className="text-[10px] font-mono font-bold text-stone-400">{t.symbol}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      {isRegistered ? (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-brand-green-light text-brand-green text-[10px] font-black uppercase tracking-wider">
                          <CheckCircle2 size={12} /> 등록 완료
                        </div>
                      ) : (
                        <div className={cn(
                          'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider',
                          isUrgent ? 'bg-brand-red-light text-brand-red-dk' : 'bg-stone-100 text-stone-400'
                        )}>
                          <AlertCircle size={12} /> 미등록
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-mono font-bold text-stone-500">₩{monthlyProfit.toLocaleString()}</td>
                    <td className="px-6 py-4 text-right text-sm font-mono font-bold text-brand-red">₩{residual.toLocaleString()}</td>
                    <td className="px-6 py-4 text-center" onClick={e => e.stopPropagation()}>
                      <button onClick={() => openDetail(t)}
                        className="px-4 py-1.5 rounded-lg bg-white border border-stone-200 text-[10px] font-black text-stone-400 hover:text-brand-red hover:border-brand-red transition-all">
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
