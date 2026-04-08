import { useState, useEffect } from "react";
import { ArrowRight, FileText } from "lucide-react";
import { imgSrc } from "./assetUtils.jsx";

const EMPTY_FORM = {
  // 자산
  assetName:    "",
  assetAddress: "",
  imgUrl:       "",
  totalValue:   "",
  // 토큰
  tokenName:    "",
  tokenSymbol:  "",
  initPrice:    "",
  // 플랫폼 보유
  holdingType:  "percent", // "percent" | "count"
  holdingValue: "",
  // 파일
  originName:   "",
};

function calcTotalSupply(totalValue, initPrice) {
  const v = Number(String(totalValue).replace(/,/g, ""));
  const p = Number(String(initPrice).replace(/,/g, ""));
  if (!v || !p) return 0;
  return Math.floor(v / p);
}

function calcHoldingSupply(totalSupply, holdingType, holdingValue) {
  const h = Number(holdingValue) || 0;
  if (holdingType === "percent") return Math.floor((totalSupply * h) / 100);
  return Math.min(h, totalSupply);
}

function Field({ label, children }) {
  return (
    <div className="space-y-1.5">
      <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest block">
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
          ? "w-full bg-stone-200 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-400 outline-none font-medium cursor-not-allowed"
          : "w-full bg-stone-100 border border-stone-200 rounded-md px-4 py-3 text-sm text-stone-800 outline-none focus:border-brand-blue transition-colors font-medium"
      }
    />
  );
}

export function AssetForm({ detail, isNew, onBack, onSave }) {
  const [form, setForm] = useState(EMPTY_FORM);

  // 수정 모드: 편집 가능한 필드만 채움
  useEffect(() => {
    if (!isNew && detail) {
      setForm({
        assetName:    detail.assetName    ?? "",
        assetAddress: detail.assetAddress ?? "",
        imgUrl:       detail.imgUrl       ?? "",
        totalValue:   detail.totalValue   ?? "",
        tokenName:    detail.tokenName    ?? "",
        tokenSymbol:  detail.tokenSymbol  ?? "",
        initPrice:    detail.initPrice    ?? "",
        holdingType:  "count",
        holdingValue: detail.holdingSupply ?? "",
        originName:   detail.originName   ?? "",
      });
    } else {
      setForm(EMPTY_FORM);
    }
  }, [isNew, detail]);

  function set(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  const totalSupply   = calcTotalSupply(form.totalValue, form.initPrice);
  const holdingSupply = calcHoldingSupply(totalSupply, form.holdingType, form.holdingValue);
  const available     = Math.max(0, totalSupply - holdingSupply);

  // 수정 모드에서 토큰 심볼·발행가는 읽기전용 (발행 후 변경 불가)
  const tokenFieldReadOnly = !isNew;

  function handleSubmit() {
    const payload = {
      assetName:     form.assetName,
      assetAddress:  form.assetAddress,
      imgUrl:        form.imgUrl,
      totalValue:    Number(String(form.totalValue).replace(/,/g, "")),
      tokenName:     form.tokenName,
      tokenSymbol:   form.tokenSymbol,
      initPrice:     Number(String(form.initPrice).replace(/,/g, "")),
      totalSupply,
      holdingSupply,
      originName:    form.originName,
    };
    onSave(payload);
  }

  return (
    <div className="space-y-8">
      {/* 헤더 */}
      <div className="flex items-center gap-4">
        <button
          onClick={onBack}
          className="p-2 rounded-md bg-white border border-stone-200 text-stone-400 hover:text-stone-800 transition-colors"
        >
          <ArrowRight className="rotate-180 w-5 h-5" />
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

      <div className="grid lg:grid-cols-2 gap-8">
        {/* ── 좌: 자산 기본 정보 */}
        <div className="space-y-8">
          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
              자산 기본 정보
            </h3>

            {/* 이미지 미리보기 */}
            <Field label="자산 이미지 파일명">
              <div className="w-full aspect-video rounded-lg bg-stone-100 border border-stone-200 overflow-hidden flex items-center justify-center mb-2">
                {form.imgUrl ? (
                  <img src={imgSrc(form.imgUrl)} alt="preview" className="w-full h-full object-cover" />
                ) : (
                  <p className="text-xs text-stone-400">파일명을 입력하면 미리보기가 표시됩니다</p>
                )}
              </div>
              <TextInput
                value={form.imgUrl}
                placeholder="building_01.jpg"
                onChange={(e) => set("imgUrl", e.target.value)}
              />
            </Field>

            <Field label="자산명">
              <TextInput
                value={form.assetName}
                placeholder="강남 오피스 빌딩 A"
                onChange={(e) => set("assetName", e.target.value)}
              />
            </Field>

            <Field label="자산 주소">
              <TextInput
                value={form.assetAddress}
                placeholder="서울 강남구 테헤란로 123"
                onChange={(e) => set("assetAddress", e.target.value)}
              />
            </Field>

            <Field label="첨부 파일명 (보고서)">
              <div className="relative">
                <TextInput
                  value={form.originName}
                  placeholder="보고서.pdf"
                  onChange={(e) => set("originName", e.target.value)}
                />
                <FileText className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-stone-400 pointer-events-none" />
              </div>
            </Field>
          </div>

          {/* 플랫폼 보유 설정 */}
          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
              플랫폼 보유 토큰 설정
            </h3>

            <Field label="보유 방식">
              <div className="flex gap-3">
                {[["percent", "비율 (%)"], ["count", "수량 (ST)"]].map(([v, label]) => (
                  <button
                    key={v}
                    onClick={() => set("holdingType", v)}
                    className={`flex-1 py-2.5 rounded-md text-sm font-medium border transition-colors ${
                      form.holdingType === v
                        ? "bg-brand-blue text-white border-brand-blue"
                        : "bg-stone-100 text-stone-500 border-stone-200 hover:bg-stone-200"
                    }`}
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
                onChange={(e) => set("holdingValue", e.target.value)}
              />
            </Field>

            {/* 자동 계산 결과 */}
            <div className="p-4 bg-stone-50 border border-stone-200 rounded-md space-y-3">
              {[
                ["총 공급량",       `${totalSupply.toLocaleString()} ST`],
                ["플랫폼 보유량",   `${holdingSupply.toLocaleString()} ST`],
                ["일반 판매 가능량", `${available.toLocaleString()} ST`],
              ].map(([l, v]) => (
                <div key={l} className="flex justify-between items-center">
                  <span className="text-xs text-stone-400 font-medium">{l}</span>
                  <span className="text-sm font-bold text-stone-700">{v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── 우: 금융 및 토큰 정보 */}
        <div className="space-y-8">
          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
              금융 정보
            </h3>

            <Field label="자산 총 금액 (KRW)">
              <TextInput
                value={form.totalValue}
                placeholder="10,000,000,000"
                readOnly={tokenFieldReadOnly}
                onChange={(e) => set("totalValue", e.target.value)}
              />
            </Field>

            <Field label="토큰 발행가 (KRW)">
              <TextInput
                value={form.initPrice}
                placeholder="5,000"
                readOnly={tokenFieldReadOnly}
                onChange={(e) => set("initPrice", e.target.value)}
              />
            </Field>

            {/* 총 공급량 자동 계산 */}
            <Field label="총 공급량 (자동 계산)">
              <TextInput
                value={`${totalSupply.toLocaleString()} ST`}
                readOnly
              />
              {!tokenFieldReadOnly && (
                <p className="text-[10px] text-stone-400 mt-1">
                  * 자산 총 금액 ÷ 토큰 발행가로 자동 계산됩니다
                </p>
              )}
            </Field>

            {/* 수정 모드: 현재가·유통량 읽기전용 */}
            {!isNew && detail && (
              <>
                <Field label="현재가 (읽기 전용)">
                  <TextInput
                    value={`₩${Number(detail.currentPrice ?? 0).toLocaleString()}`}
                    readOnly
                  />
                </Field>
                <Field label="유통량 (읽기 전용)">
                  <TextInput
                    value={`${(detail.circulatingSupply ?? 0).toLocaleString()} ST`}
                    readOnly
                  />
                </Field>
                <Field label="발행일 (읽기 전용)">
                  <TextInput
                    value={detail.issuedAt ? new Date(detail.issuedAt).toLocaleDateString("ko-KR") : "-"}
                    readOnly
                  />
                </Field>
              </>
            )}
          </div>

          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-6">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4">
              토큰 정보
            </h3>

            <Field label="토큰명">
              <TextInput
                value={form.tokenName}
                placeholder="Gangnam Office Token"
                readOnly={tokenFieldReadOnly}
                onChange={(e) => set("tokenName", e.target.value)}
              />
            </Field>

            <Field label="토큰 심볼">
              <TextInput
                value={form.tokenSymbol}
                placeholder="GOT"
                readOnly={tokenFieldReadOnly}
                onChange={(e) => set("tokenSymbol", e.target.value)}
              />
              {tokenFieldReadOnly && (
                <p className="text-[10px] text-stone-400 mt-1">
                  * 발행 후 심볼과 발행가는 변경할 수 없습니다
                </p>
              )}
            </Field>
          </div>

          {/* 버튼 */}
          <div className="flex gap-4">
            <button
              onClick={onBack}
              className="flex-1 py-4 rounded-md bg-white text-stone-400 text-sm font-medium hover:bg-stone-100 transition-colors border border-stone-200"
            >
              취소
            </button>
            <button
              onClick={handleSubmit}
              className="flex-[2] py-4 rounded-md bg-brand-blue text-white text-sm font-medium hover:bg-brand-blue-dk transition-colors"
            >
              {isNew ? "자산 등록" : "변경사항 저장"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
