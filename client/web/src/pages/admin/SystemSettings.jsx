import { useEffect, useState } from "react";
import {
  Activity,
  AlertCircle,
  CheckCircle2,
  DollarSign,
  Percent,
  RefreshCw,
  Save,
} from "lucide-react";
import api from "../../lib/api.js";

const EMPTY_FORM = {
  taxRate: "",
  chargeRate: "",
  allocateDate: "",
  allocateSetDate: "",
};

function hasValue(value) {
  return value !== "" && value !== null && value !== undefined;
}

function parseRequiredNumber(value) {
  if (!hasValue(value)) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function Field({ label, suffix, children }) {
  return (
    <div className="space-y-1.5">
      <label className="ml-1 text-[10px] font-semibold uppercase tracking-widest text-stone-400">
        {label}
      </label>
      <div className="relative">
        {children}
        <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-black text-stone-400">
          {suffix === "%" ? <Percent className="h-4 w-4" /> : suffix}
        </span>
      </div>
    </div>
  );
}

function NumberInput({ value, onChange, placeholder, min, max, step = "1" }) {
  return (
    <input
      type="number"
      min={min}
      max={max}
      step={step}
      value={value}
      placeholder={placeholder}
      onChange={onChange}
      className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
    />
  );
}

export function SystemSettings() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState("");

  useEffect(() => {
    let mounted = true;

    async function loadCommon() {
      setLoading(true);
      setLoadError("");

      try {
        const { data } = await api.get("/admin/common");
        if (!mounted) return;
        setForm({
          taxRate: data?.taxRate ?? "",
          chargeRate: data?.chargeRate ?? "",
          allocateDate: data?.allocateDate ?? "",
          allocateSetDate: data?.allocateSetDate ?? "",
        });
      } catch (error) {
        console.error("[SystemSettings] 조회 실패:", error);
        if (!mounted) return;
        setLoadError("플랫폼 기본 설정을 불러오지 못했습니다.");
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadCommon();

    return () => {
      mounted = false;
    };
  }, []);

  function set(key, value) {
    setSaveSuccess(false);
    setSaveError("");
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSave() {
    setSaveSuccess(false);
    setSaveError("");

    const payload = {
      taxRate: parseRequiredNumber(form.taxRate),
      chargeRate: parseRequiredNumber(form.chargeRate),
      allocateDate: parseRequiredNumber(form.allocateDate),
      allocateSetDate: parseRequiredNumber(form.allocateSetDate),
    };

    if (
      payload.taxRate === null ||
      payload.chargeRate === null ||
      payload.allocateDate === null ||
      payload.allocateSetDate === null
    ) {
      setSaveError("모든 설정 값을 입력해 주세요.");
      return;
    }

    try {
      setIsSaving(true);
      await api.post("/admin/common", payload);
      setSaveSuccess(true);
    } catch (error) {
      console.error("[SystemSettings] 저장 실패:", error);
      setSaveError("플랫폼 기본 설정 저장에 실패했습니다.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">시스템 설정</h1>
          <p className="text-sm text-stone-400">
            플랫폼 운영 및 정산 기준이 되는 기본 설정을 관리합니다.
          </p>
        </div>
        <button
          onClick={handleSave}
          disabled={isSaving || loading}
          className="flex items-center gap-2 rounded-md bg-stone-800 px-8 py-3 text-sm font-semibold text-white transition-colors hover:bg-black disabled:opacity-60"
        >
          {isSaving ? <RefreshCw className="h-5 w-5 animate-spin" /> : <Save className="h-5 w-5" />}
          설정 저장하기
        </button>
      </div>

      {saveSuccess && (
        <div className="flex items-center gap-3 rounded-md border border-brand-green-light bg-brand-green-light p-4 text-sm font-medium text-brand-green">
          <CheckCircle2 className="h-5 w-5" />
          플랫폼 기본 설정이 성공적으로 저장되었습니다.
        </div>
      )}

      {saveError && (
        <div className="flex items-center gap-3 rounded-md border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-600">
          <AlertCircle className="h-5 w-5" />
          {saveError}
        </div>
      )}

      {loadError && (
        <div className="flex items-center gap-3 rounded-md border border-amber-200 bg-amber-50 p-4 text-sm font-medium text-amber-700">
          <AlertCircle className="h-5 w-5" />
          {loadError}
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-4">
        <div className="space-y-2 lg:col-span-1">
          <button className="w-full rounded-lg border border-stone-200 bg-white px-6 py-4 text-left text-sm font-semibold text-stone-800 transition-colors">
            <span className="flex items-center gap-3">
              <Activity size={18} className="text-brand-blue" />
              거래 및 정산 설정
            </span>
          </button>
        </div>

        <div className="space-y-8 lg:col-span-3">
          <div className="space-y-8 rounded-lg border border-stone-200 bg-white p-8">
            <h3 className="flex items-center gap-2 border-b border-stone-200 pb-4 text-sm font-semibold uppercase tracking-widest text-stone-800">
              <DollarSign size={16} className="text-brand-red" />
              플랫폼 기본 설정
            </h3>

            {loading ? (
              <div className="py-12 text-center text-sm text-stone-400">불러오는 중...</div>
            ) : (
              <div className="grid gap-8 md:grid-cols-2">
                <Field label="배당금 원천징수 세율" suffix="%">
                  <NumberInput
                    value={form.taxRate}
                    step="0.1"
                    placeholder="15.4"
                    onChange={(event) => set("taxRate", event.target.value)}
                  />
                </Field>

                <Field label="기본 거래 수수료" suffix="%">
                  <NumberInput
                    value={form.chargeRate}
                    step="0.01"
                    placeholder="0.05"
                    onChange={(event) => set("chargeRate", event.target.value)}
                  />
                </Field>

                <Field label="배당금 정산일 (매월)" suffix="일">
                  <NumberInput
                    value={form.allocateDate}
                    min="1"
                    max="31"
                    placeholder="20"
                    onChange={(event) => set("allocateDate", event.target.value)}
                  />
                </Field>

                <Field label="관리자 입력 마감일 (매월)" suffix="일">
                  <NumberInput
                    value={form.allocateSetDate}
                    min="1"
                    max="31"
                    placeholder="10"
                    onChange={(event) => set("allocateSetDate", event.target.value)}
                  />
                </Field>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
