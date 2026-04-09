import { useEffect, useState } from "react";
import {
  ArrowRight,
  FileText,
  Image as ImageIcon,
  MapPinned,
  Upload,
} from "lucide-react";
import { imgSrc } from "./assetUtils.jsx";

const TOKEN_STATUS_OPTIONS = [
  { value: "ISSUED", label: "발행완료" },
  { value: "TRADING", label: "거래 중" },
  { value: "SUSPENDED", label: "거래 중단" },
  { value: "CLOSED", label: "거래 완료" },
];

const EMPTY_FORM = {
  tokenId: null,
  disclosureId: null,
  assetName: "",
  assetAddress: "",
  imgUrl: "",
  totalValue: "",
  tokenName: "",
  tokenSymbol: "",
  initPrice: "",
  tokenStatus: "",
  isAllocated: true,
  holdingType: "percent",
  holdingValue: "",
  originName: "",
  imageFile: null,
  pdfFile: null,
};

function calcTotalSupply(totalValue, initPrice) {
  const total = Number(String(totalValue).replace(/,/g, ""));
  const price = Number(String(initPrice).replace(/,/g, ""));
  if (!total || !price) return 0;
  return Math.floor(total / price);
}

function calcHoldingSupply(totalSupply, holdingType, holdingValue) {
  const holding = Number(holdingValue) || 0;
  if (holdingType === "percent") return Math.floor((totalSupply * holding) / 100);
  return Math.min(holding, totalSupply);
}

function Field({ label, children }) {
  return (
    <div className="min-w-0 space-y-1.5">
      <label className="block text-[10px] font-semibold uppercase tracking-widest text-stone-400">
        {label}
      </label>
      {children}
    </div>
  );
}

function TextInput({ value, onChange, placeholder, readOnly = false }) {
  return (
    <input
      type="text"
      value={value}
      placeholder={placeholder}
      readOnly={readOnly}
      onChange={onChange}
      className={
        readOnly
          ? "w-full min-w-0 cursor-not-allowed rounded-md border border-stone-200 bg-stone-200 px-4 py-3 text-sm font-medium text-stone-400 outline-none"
          : "w-full min-w-0 rounded-md border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-medium text-stone-800 outline-none transition-colors focus:border-brand-blue"
      }
    />
  );
}

function SelectInput({ value, onChange, options }) {
  return (
    <select
      value={value}
      onChange={onChange}
      className="w-full min-w-0 rounded-md border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-medium text-stone-800 outline-none transition-colors focus:border-brand-blue"
    >
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

function loadDaumPostcodeScript() {
  const existingScript = document.querySelector(
    'script[src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"]',
  );

  if (existingScript) {
    if (window.daum?.Postcode) return Promise.resolve();
    return new Promise((resolve, reject) => {
      existingScript.addEventListener("load", resolve, { once: true });
      existingScript.addEventListener("error", reject, { once: true });
    });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error("다음 주소 검색 스크립트를 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

export function AssetForm({ detail, isNew, onBack, onSave }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitError, setSubmitError] = useState("");
  const [saving, setSaving] = useState(false);
  const [imagePreviewSrc, setImagePreviewSrc] = useState(null);
  const [addressLoading, setAddressLoading] = useState(false);

  useEffect(() => {
    if (!isNew && detail) {
      setForm({
        tokenId: detail.tokenId ?? null,
        disclosureId: detail.disclosureId ?? null,
        assetName: detail.assetName ?? "",
        assetAddress: detail.assetAddress ?? "",
        imgUrl: detail.imgUrl ?? "",
        totalValue: detail.totalValue ?? "",
        tokenName: detail.tokenName ?? detail.assetName ?? "",
        tokenSymbol: detail.tokenSymbol ?? "",
        initPrice: detail.initPrice ?? "",
        tokenStatus: detail.tokenStatus ?? "",
        isAllocated: Boolean(detail.isAllocated),
        holdingType: "count",
        holdingValue: detail.holdingSupply ?? "",
        originName: detail.originName ?? "",
        imageFile: null,
        pdfFile: null,
      });
      return;
    }

    setForm(EMPTY_FORM);
  }, [detail, isNew]);

  useEffect(() => {
    if (!form.imageFile) {
      setImagePreviewSrc(form.imgUrl ? imgSrc(form.imgUrl) : null);
      return undefined;
    }

    const objectUrl = URL.createObjectURL(form.imageFile);
    setImagePreviewSrc(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [form.imageFile, form.imgUrl]);

  function set(key, value) {
    setForm((prev) => {
      if (key === "assetName") {
        return {
          ...prev,
          assetName: value,
          tokenName: value,
        };
      }

      return { ...prev, [key]: value };
    });
  }

  function handleFileChange(key, file) {
    setSubmitError("");
    setForm((prev) => ({
      ...prev,
      [key]: file ?? null,
      ...(key === "pdfFile" ? { originName: file?.name ?? prev.originName } : {}),
    }));
  }

  async function handleAddressSearch() {
    setSubmitError("");
    try {
      setAddressLoading(true);
      await loadDaumPostcodeScript();

      new window.daum.Postcode({
        oncomplete: (data) => {
          const address = data.roadAddress || data.address;
          const extraAddress = [data.buildingName, data.bname].filter(Boolean).join(" ");
          set("assetAddress", extraAddress ? `${address} (${extraAddress})` : address);
        },
      }).open();
    } catch (error) {
      console.error("[AssetForm] 주소 검색 로드 실패:", error);
      setSubmitError("주소 검색 서비스를 불러오지 못했습니다.");
    } finally {
      setAddressLoading(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError("");

    if (isNew && !form.imageFile) {
      setSubmitError("자산 이미지를 선택해 주세요.");
      return;
    }

    if (isNew && !form.pdfFile) {
      setSubmitError("보고서 PDF를 선택해 주세요.");
      return;
    }

    const totalSupply = calcTotalSupply(form.totalValue, form.initPrice);
    const holdingSupply = calcHoldingSupply(totalSupply, form.holdingType, form.holdingValue);
    const circulatingSupply = Math.max(0, totalSupply - holdingSupply);

    const dto = {
      tokenId: form.tokenId,
      disclosureId: form.disclosureId,
      assetName: form.assetName.trim(),
      tokenSymbol: form.tokenSymbol.trim(),
      assetAddress: form.assetAddress.trim(),
      imgUrl: form.imgUrl,
      isAllocated: form.isAllocated,
      ...(isNew ? {} : { tokenStatus: form.tokenStatus }),
      totalValue: Number(String(form.totalValue).replace(/,/g, "")),
      tokenName: form.tokenName.trim(),
      initPrice: Number(String(form.initPrice).replace(/,/g, "")),
      totalSupply,
      holdingSupply,
      circulatingSupply,
    };

    const payload = new FormData();
    payload.append("dto", new Blob([JSON.stringify(dto)], { type: "application/json" }));

    if (form.imageFile) payload.append("imageFile", form.imageFile);
    if (form.pdfFile) payload.append("pdfFile", form.pdfFile);

    try {
      setSaving(true);
      await onSave(payload);
    } catch (error) {
      console.error("[AssetForm] 저장 실패:", error);
      setSubmitError("자산 저장에 실패했습니다. 입력값과 파일을 확인해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  const totalSupply = calcTotalSupply(form.totalValue, form.initPrice);
  const holdingSupply = calcHoldingSupply(totalSupply, form.holdingType, form.holdingValue);
  const available = Math.max(0, totalSupply - holdingSupply);
  const financeReadOnly = !isNew;
  const holdingReadOnly = !isNew;

  return (
    <form onSubmit={handleSubmit} className="space-y-8 overflow-x-hidden">
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={onBack}
          className="rounded-md border border-stone-200 bg-white p-2 text-stone-400 transition-colors hover:text-stone-800"
        >
          <ArrowRight className="h-5 w-5 rotate-180" />
        </button>
        <div>
          <h2 className="text-xl font-semibold text-stone-800">
            {isNew ? "신규 자산 등록" : "자산 정보 수정"}
          </h2>
          <p className="text-sm text-stone-400">
            {isNew ? "새 자산 정보를 입력합니다" : `${detail?.assetName} (${detail?.tokenSymbol})`}
          </p>
        </div>
      </div>

      <div className="grid items-start gap-8 lg:grid-cols-2">
        <div className="min-w-0 space-y-8">
          <div className="space-y-6 overflow-hidden rounded-lg border border-stone-200 bg-white p-8">
            <h3 className="border-b border-stone-200 pb-4 text-sm font-semibold uppercase tracking-widest text-stone-800">
              자산 기본 정보
            </h3>

            <Field label="자산 이미지">
              <div className="mb-2 flex aspect-video w-full items-center justify-center overflow-hidden rounded-lg border border-stone-200 bg-stone-100">
                {imagePreviewSrc ? (
                  <img src={imagePreviewSrc} alt="preview" className="h-full w-full object-cover" />
                ) : (
                  <div className="flex flex-col items-center gap-2 text-stone-400">
                    <ImageIcon className="h-6 w-6" />
                    <p className="text-xs">이미지 파일을 선택하면 미리보기가 표시됩니다</p>
                  </div>
                )}
              </div>
              <label className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed border-stone-300 bg-stone-100 px-4 py-3 text-sm text-stone-600 transition-colors hover:border-brand-blue hover:text-brand-blue">
                <Upload className="h-4 w-4 shrink-0" />
                <span className="truncate">{form.imageFile?.name ?? form.imgUrl ?? "이미지 파일 선택"}</span>
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/jpg"
                  onChange={(e) => handleFileChange("imageFile", e.target.files?.[0])}
                  className="sr-only"
                />
              </label>
            </Field>

            <Field label="자산명">
              <TextInput
                value={form.assetName}
                placeholder="강남 오피스 빌딩 A"
                onChange={(e) => set("assetName", e.target.value)}
              />
            </Field>

            <Field label="자산 주소">
              <div className="flex flex-col gap-3 sm:flex-row">
                <div className="min-w-0 flex-[2]">
                  <TextInput
                    value={form.assetAddress}
                    placeholder="서울 강남구 테헤란로 123"
                    onChange={(e) => set("assetAddress", e.target.value)}
                  />
                </div>
                <button
                  type="button"
                  onClick={handleAddressSearch}
                  disabled={addressLoading}
                  className="inline-flex shrink-0 items-center justify-center gap-2 rounded-md border border-stone-200 bg-white px-4 py-3 text-sm font-medium text-stone-700 transition-colors hover:border-brand-blue hover:text-brand-blue disabled:opacity-60"
                >
                  <MapPinned className="h-4 w-4" />
                  {addressLoading ? "불러오는 중..." : "주소 검색"}
                </button>
              </div>
              <p className="mt-1 text-[10px] text-stone-400">
                다음 주소 검색으로 도로명 주소를 자동 입력할 수 있습니다.
              </p>
            </Field>

            <Field label="첨부 파일 (보고서 PDF)">
              <label className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed border-stone-300 bg-stone-100 px-4 py-3 text-sm text-stone-600 transition-colors hover:border-brand-blue hover:text-brand-blue">
                <FileText className="h-4 w-4 shrink-0" />
                <span className="truncate">{form.pdfFile?.name ?? form.originName ?? "PDF 파일 선택"}</span>
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={(e) => handleFileChange("pdfFile", e.target.files?.[0])}
                  className="sr-only"
                />
              </label>
            </Field>
          </div>

          <div className="space-y-6 overflow-hidden rounded-lg border border-stone-200 bg-white p-8">
            <h3 className="border-b border-stone-200 pb-4 text-sm font-semibold uppercase tracking-widest text-stone-800">
              토큰 정보
            </h3>

            <Field label="토큰명">
              <TextInput value={form.tokenName} placeholder="자산명을 입력하면 자동 설정됩니다" readOnly />
            </Field>

            <Field label="토큰 심볼">
              <TextInput
                value={form.tokenSymbol}
                placeholder="GOT"
                onChange={(e) => set("tokenSymbol", e.target.value)}
              />
            </Field>

            {!isNew && (
              <Field label="거래 상태">
                <SelectInput
                  value={form.tokenStatus}
                  onChange={(e) => set("tokenStatus", e.target.value)}
                  options={TOKEN_STATUS_OPTIONS}
                />
              </Field>
            )}
          </div>
        </div>

        <div className="min-w-0 space-y-8">
          <div className="space-y-6 overflow-hidden rounded-lg border border-stone-200 bg-white p-8">
            <h3 className="border-b border-stone-200 pb-4 text-sm font-semibold uppercase tracking-widest text-stone-800">
              금융 정보
            </h3>

            <Field label="자산 총 금액 (KRW)">
              <TextInput
                value={form.totalValue}
                placeholder="10,000,000,000"
                readOnly={financeReadOnly}
                onChange={(e) => set("totalValue", e.target.value)}
              />
            </Field>

            <Field label="토큰 발행가 (KRW)">
              <TextInput
                value={form.initPrice}
                placeholder="5,000"
                readOnly={financeReadOnly}
                onChange={(e) => set("initPrice", e.target.value)}
              />
            </Field>

            <Field label="총 공급량 (자동 계산)">
              <TextInput value={`${totalSupply.toLocaleString()} ST`} readOnly />
              <p className="mt-1 text-[10px] text-stone-400">
                * 자산 총 금액 ÷ 토큰 발행가로 자동 계산됩니다
              </p>
            </Field>
          </div>

          <div className="space-y-6 overflow-hidden rounded-lg border border-stone-200 bg-white p-8">
            <h3 className="border-b border-stone-200 pb-4 text-sm font-semibold uppercase tracking-widest text-stone-800">
              플랫폼 보유 토큰 설정
            </h3>

            <Field label="보유 방식">
              <div className="flex flex-col gap-3 sm:flex-row">
                {[
                  ["percent", "비율 (%)"],
                  ["count", "수량 (ST)"],
                ].map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    disabled={holdingReadOnly}
                    onClick={() => !holdingReadOnly && set("holdingType", value)}
                    className={`flex-1 rounded-md border py-2.5 text-sm font-medium transition-colors ${
                      form.holdingType === value
                        ? "border-brand-blue bg-brand-blue text-white"
                        : "border-stone-200 bg-stone-100 text-stone-500 hover:bg-stone-200"
                    } ${holdingReadOnly ? "cursor-not-allowed opacity-70" : ""}`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </Field>

            <Field label={form.holdingType === "percent" ? "보유 비율 (%)" : "보유 수량 (ST)"}>
              <TextInput
                value={form.holdingValue}
                placeholder={form.holdingType === "percent" ? "10" : "1000"}
                readOnly={holdingReadOnly}
                onChange={(e) => set("holdingValue", e.target.value)}
              />
            </Field>

            <Field label="배당 지급 여부">
              <div className="flex flex-col gap-3 sm:flex-row">
                {[
                  [true, "지급"],
                  [false, "미지급"],
                ].map(([value, label]) => (
                  <button
                    key={label}
                    type="button"
                    onClick={() => set("isAllocated", value)}
                    className={`flex-1 rounded-md border py-2.5 text-sm font-medium transition-colors ${
                      form.isAllocated === value
                        ? "border-brand-blue bg-brand-blue text-white"
                        : "border-stone-200 bg-stone-100 text-stone-500 hover:bg-stone-200"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </Field>

            {holdingReadOnly && (
              <p className="text-[10px] text-stone-400">
                * 수정 시 플랫폼 보유 방식과 보유 수량은 읽기 전용입니다.
              </p>
            )}

            <div className="space-y-3 rounded-md border border-stone-200 bg-stone-50 p-4">
              {[
                ["총 공급량", `${totalSupply.toLocaleString()} ST`],
                ["플랫폼 보유량", `${holdingSupply.toLocaleString()} ST`],
                ["일반 판매 가능량", `${available.toLocaleString()} ST`],
              ].map(([label, value]) => (
                <div key={label} className="flex items-center justify-between gap-4">
                  <span className="text-xs font-medium text-stone-400">{label}</span>
                  <span className="text-right text-sm font-bold text-stone-700">{value}</span>
                </div>
              ))}
            </div>
          </div>

          {submitError && (
            <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {submitError}
            </div>
          )}

          <div className="flex flex-col gap-4 sm:flex-row">
            <button
              type="button"
              onClick={onBack}
              className="flex-1 rounded-md border border-stone-200 bg-white py-4 text-sm font-medium text-stone-400 transition-colors hover:bg-stone-100"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex-[2] rounded-md bg-brand-blue py-4 text-sm font-medium text-white transition-colors hover:bg-brand-blue-dk"
            >
              {saving ? "저장 중..." : isNew ? "자산 등록" : "변경사항 저장"}
            </button>
          </div>
        </div>
      </div>
    </form>
  );
}
