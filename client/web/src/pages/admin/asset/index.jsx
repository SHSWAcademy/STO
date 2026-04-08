import { useState, useEffect } from "react";
import { useApp } from "../../../context/AppContext.jsx";
import api from "../../../lib/api.js";
import { AssetList }   from "./AssetList.jsx";
import { AssetDetail } from "./AssetDetail.jsx";
import { AssetForm }   from "./AssetForm.jsx";

const VIEW = { LIST: "list", DETAIL: "detail", FORM: "form" };

export function AssetManagement() {
  const { tokens, setTokens } = useApp();

  const [view, setView]       = useState(VIEW.LIST);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  // 목록에서 선택한 아이템 (AssetListResponseDTO)
  const [selectedItem, setSelectedItem] = useState(null);
  // 상세 조회 결과 (AssetDetailResponseDTO)
  const [assetDetail, setAssetDetail]   = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // 신규 등록 여부
  const [isNew, setIsNew] = useState(false);

  // ── 목록 로드
  useEffect(() => {
    api.get("/admin/asset")
      .then(({ data }) => setTokens(data))
      .catch((err) => {
        console.error("[AssetManagement] 목록 조회 실패:", err.message);
        setTokens([]);
        setError("서버에서 목록을 불러오지 못했습니다.");
      })
      .finally(() => setLoading(false));
  }, []);

  // ── 상세 열기
  function openDetail(item) {
    setSelectedItem(item);
    setAssetDetail(null);
    setDetailLoading(true);
    setView(VIEW.DETAIL);

    api.get(`/admin/asset/${item.assetId}`)
      .then(({ data }) => setAssetDetail(data))
      .catch((err) => console.error("[AssetManagement] 상세 조회 실패:", err.message))
      .finally(() => setDetailLoading(false));
  }

  // ── 수정 폼 열기 (상세에서 진입)
  function openEdit(detail) {
    setAssetDetail(detail);
    setIsNew(false);
    setView(VIEW.FORM);
  }

  // ── 신규 등록 폼 열기
  function openNew() {
    setSelectedItem(null);
    setAssetDetail(null);
    setIsNew(true);
    setView(VIEW.FORM);
  }

  // ── 저장 처리
  function handleSave(payload) {
    if (isNew) {
      // TODO: POST /admin/asset 연동
      console.log("[AssetManagement] 신규 등록:", payload);
    } else {
      // TODO: PUT /admin/asset/{assetId} 연동
      console.log("[AssetManagement] 수정:", selectedItem?.assetId, payload);
    }
    setView(VIEW.LIST);
  }

  // ── 뷰 라우팅
  if (view === VIEW.DETAIL && selectedItem) {
    return (
      <AssetDetail
        item={selectedItem}
        detail={assetDetail}
        loading={detailLoading}
        onBack={() => setView(VIEW.LIST)}
        onEdit={openEdit}
      />
    );
  }

  if (view === VIEW.FORM) {
    return (
      <AssetForm
        detail={assetDetail}
        isNew={isNew}
        onBack={() => setView(isNew ? VIEW.LIST : VIEW.DETAIL)}
        onSave={handleSave}
      />
    );
  }

  return (
    <AssetList
      tokens={tokens}
      loading={loading}
      error={error}
      searchTerm={searchTerm}
      onSearch={setSearchTerm}
      onSelect={openDetail}
      onNew={openNew}
    />
  );
}
