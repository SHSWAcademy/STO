import { useEffect, useState } from "react";
import api from "../../../lib/api.js";
import { AllocationDetail } from "./AllocationDetail.jsx";
import { AllocationList } from "./AllocationList.jsx";

const VIEW = { LIST: "list", DETAIL: "detail" };

export function AllocationManagement() {
  const [view, setView] = useState(VIEW.LIST);
  const [items, setItems] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedItem, setSelectedItem] = useState(null);
  const [detailItems, setDetailItems] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [monthMeta, setMonthMeta] = useState({ targetMonth: "", allocateSetMonth: "" });
  const [saveError, setSaveError] = useState("");
  const [saving, setSaving] = useState(false);

  async function loadItems() {
    setLoading(true);
    setError(null);
    try {
      const [{ data: allocationData }, { data: assetData }] = await Promise.all([
        api.get("/admin/allocationEvent"),
        api.get("/admin/asset"),
      ]);
      setItems(allocationData);
      setAssets(assetData);
      if (allocationData?.length) {
        setMonthMeta({
          targetMonth: allocationData[0].targetMonth ?? "",
          allocateSetMonth: allocationData[0].allocateSetMonth ?? "",
        });
      }
    } catch (loadError) {
      console.error("[AllocationManagement] 조회 실패:", loadError);
      setItems([]);
      setAssets([]);
      setError("서버에서 배당 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadItems();
  }, []);

  async function openDetail(item) {
    setSelectedItem(item);
    setDetailItems([]);
    setDetailLoading(true);
    setSaveError("");
    setView(VIEW.DETAIL);

    try {
      const { data } = await api.get(`/admin/allocationEvent/${item.assetId}`);
      setDetailItems(data);
    } catch (detailError) {
      console.error("[AllocationManagement] 상세 조회 실패:", detailError);
      setDetailItems([]);
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleSave({ assetId, monthlyDividendIncome, file, currentAllocation }) {
    setSaveError("");

    if (currentAllocation) {
      return;
    }

    if (!assetId) {
      setSaveError("자산 정보가 올바르지 않습니다.");
      return;
    }

    if (!monthlyDividendIncome) {
      setSaveError("월 수익을 입력해 주세요.");
      return;
    }

    if (!file) {
      setSaveError("증빙 PDF 파일을 선택해 주세요.");
      return;
    }

    const payload = new FormData();
    payload.append(
      "dto",
      new Blob(
        [
          JSON.stringify({
            assetId: Number(assetId),
            monthlyDividendIncome: Number(String(monthlyDividendIncome).replace(/,/g, "")),
          }),
        ],
        { type: "application/json" },
      ),
    );
    payload.append("file", file);

    try {
      setSaving(true);
      await api.post("/admin/allocationEvent", payload, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      await loadItems();
      if (selectedItem) {
        const refreshedItem = { ...selectedItem, monthlyDividendIncome: Number(monthlyDividendIncome) };
        await openDetail(refreshedItem);
      } else {
        setView(VIEW.LIST);
      }
    } catch (error) {
      console.error("[AllocationManagement] 등록 실패:", error);
      setSaveError("배당 이벤트 등록에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  if (view === VIEW.DETAIL && selectedItem) {
    return (
      <AllocationDetail
        item={selectedItem}
        details={detailItems}
        loading={detailLoading}
        monthMeta={monthMeta}
        saving={saving}
        submitError={saveError}
        onBack={() => setView(VIEW.LIST)}
        onSubmit={handleSave}
      />
    );
  }

  return (
    <AllocationList
      items={items}
      loading={loading}
      error={error}
      onSelect={openDetail}
      onNew={() => {
        if (items[0]) {
          openDetail(items[0]);
        }
      }}
    />
  );
}
