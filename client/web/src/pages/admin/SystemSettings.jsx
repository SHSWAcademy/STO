import { useState } from 'react';
import { Settings, Activity, Save, RefreshCw, CheckCircle2, DollarSign, Percent } from 'lucide-react';
import { cn } from '../../lib/utils.js';

export function SystemSettings() {
  const [isSaving, setIsSaving]       = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  function handleSave() {
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    }, 1000);
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-[#2a2820]">시스템 설정</h1>
          <p className="text-sm font-bold text-[#9a9080]">플랫폼 운영 및 거래 시스템의 핵심 설정을 관리합니다.</p>
        </div>
        <button onClick={handleSave} disabled={isSaving}
          className="flex items-center gap-2 px-8 py-3 bg-[#2a2820] text-white text-sm font-black rounded-xl hover:bg-black transition-all shadow-lg shadow-black/10 disabled:opacity-60">
          {isSaving ? <RefreshCw className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
          설정 저장하기
        </button>
      </div>

      {saveSuccess && (
        <div className="bg-[#e0f0e8] border border-[#e0f0e8] rounded-2xl p-4 flex items-center gap-3 text-[#4a7a60] text-sm font-bold">
          <CheckCircle2 className="w-5 h-5" />
          모든 설정이 성공적으로 저장되었습니다.
        </div>
      )}

      <div className="grid lg:grid-cols-4 gap-8">
        {/* Sidebar */}
        <div className="lg:col-span-1 space-y-2">
          <button className="w-full flex items-center gap-3 px-6 py-4 rounded-2xl text-sm font-black transition-all border bg-white border-[#e0dace] text-[#2a2820] shadow-sm">
            <Activity size={18} className="text-[#4a72a0]" />
            거래 및 정산 설정
          </button>
        </div>

        {/* Content */}
        <div className="lg:col-span-3 space-y-8">
          <div className="bg-white border border-[#e0dace] rounded-2xl p-8 space-y-8 shadow-sm">
            <h3 className="text-sm font-black text-[#2a2820] uppercase tracking-widest border-b border-[#e0dace] pb-4 flex items-center gap-2">
              <DollarSign size={16} className="text-[#b85450]" />
              거래 수수료 및 배당 정산 설정
            </h3>

            <div className="grid grid-cols-2 gap-8">
              {[
                { label: '기본 거래 수수료 (%)',    defaultValue: '0.05', step: '0.01', suffix: '%' },
                { label: '배당금 원천징수 세율 (%)', defaultValue: '15.4', step: '0.1',  suffix: '%' },
                { label: '배당금 정산 일자 (매월)',  defaultValue: '10',   step: '1',    suffix: '일', min: '1', max: '31' },
                { label: '배당금 지급일 (매월)',     defaultValue: '20',   step: '1',    suffix: '일', min: '1', max: '31' },
              ].map(({ label, defaultValue, step, suffix, min, max }) => (
                <div key={label} className="space-y-1.5">
                  <label className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest ml-1">{label}</label>
                  <div className="relative">
                    <input
                      type="number"
                      step={step}
                      defaultValue={defaultValue}
                      min={min}
                      max={max}
                      className="w-full bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-3 text-sm text-[#2a2820] font-bold outline-none focus:border-[#4a72a0] transition-all"
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-black text-[#9a9080]">
                      {suffix === '%' ? <Percent className="w-4 h-4" /> : suffix}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
