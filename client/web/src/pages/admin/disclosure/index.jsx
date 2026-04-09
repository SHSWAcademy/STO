import { useEffect, useState } from "react";
import { Modal } from "../../../components/ui/Modal.jsx";
import api from "../../../lib/api.js";
import { DisclosureDetail } from "./DisclosureDetail.jsx";
import { DisclosureForm } from "./DisclosureForm.jsx";
import { DisclosureList } from "./DisclosureList.jsx";
import {
  filterDisclosures,
  mapDisclosureListItem,
} from "./disclosureUtils.jsx";

const EMPTY_CREATE_FORM = {
  assetId: "",
  assetName: "",
  disclosureTitle: "",
  disclosureContent: "",
  disclosureCategory: "BUILDING",
  originName: "",
  storedName: "",
  file: null,
};

export function DisclosureManagement() {
  const [items, setItems] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [disclosureTypeTab, setDisclosureTypeTab] = useState("전체");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedDisclosure, setSelectedDisclosure] = useState(null);
  const [editingDisclosure, setEditingDisclosure] = useState(null);
  const [editingForm, setEditingForm] = useState(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  const filteredDisclosures = filterDisclosures(
    items,
    disclosureTypeTab,
    searchTerm,
  );

  async function loadDisclosures() {
    setLoading(true);
    setError("");
    try {
      const [{ data }, { data: assetData }] = await Promise.all([
        api.get("/admin/disclosure"),
        api.get("/admin/asset"),
      ]);
      setItems((data ?? []).map(mapDisclosureListItem));
      setAssets(assetData ?? []);
    } catch (loadError) {
      console.error("[DisclosureManagement] 목록 조회 실패:", loadError);
      setError("공시 목록을 불러오지 못했습니다.");
      setItems([]);
      setAssets([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDisclosures();
  }, []);

  function handleSelect(item) {
    setSelectedDisclosure(item);
  }

  function handleEdit(item) {
    setSelectedDisclosure(null);
    setFormError("");
    setEditingDisclosure(item);
    setEditingForm({ ...item, file: null });
  }

  async function handleDelete(item) {
    if (item.deletedAt) return;
    try {
      await api.delete(`/admin/disclosure/${item.disclosureId}`, {
        params: { storedName: item.storedName ?? "" },
      });
      const deletedAt = new Date().toISOString();
      setItems((prev) =>
        prev.map((current) =>
          current.disclosureId === item.disclosureId
            ? { ...current, deletedAt }
            : current,
        ),
      );
      setSelectedDisclosure((prev) =>
        prev?.disclosureId === item.disclosureId ? { ...prev, deletedAt } : prev,
      );
      setEditingDisclosure((prev) =>
        prev?.disclosureId === item.disclosureId ? null : prev,
      );
      await loadDisclosures();
    } catch (deleteError) {
      console.error("[DisclosureManagement] 삭제 실패:", deleteError);
      setError("공시 삭제에 실패했습니다.");
    }
  }

  function closeEditModal() {
    setEditingDisclosure(null);
    setEditingForm(null);
    setFormError("");
  }

  function closeCreateModal() {
    setIsCreateOpen(false);
    setCreateForm(EMPTY_CREATE_FORM);
    setFormError("");
  }

  async function handleSaveEdit() {
    if (!editingDisclosure || !editingForm) return;
    if (!editingForm.disclosureTitle?.trim()) {
      setFormError("공시 제목을 입력해 주세요.");
      return;
    }
    if (!editingForm.disclosureContent?.trim()) {
      setFormError("공시 본문을 입력해 주세요.");
      return;
    }

    const payload = new FormData();
    payload.append(
      "dto",
      new Blob(
        [
          JSON.stringify({
            disclosureTitle: editingForm.disclosureTitle.trim(),
            disclosureContent: editingForm.disclosureContent.trim(),
            disclosureCategory: editingForm.disclosureCategory,
          }),
        ],
        { type: "application/json" },
      ),
    );
    if (editingForm.file) {
      payload.append("file", editingForm.file);
    }

    try {
      setSaving(true);
      setFormError("");
      await api.patch(`/admin/disclosure/${editingDisclosure.disclosureId}`, payload, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      await loadDisclosures();
      closeEditModal();
    } catch (saveError) {
      console.error("[DisclosureManagement] 수정 실패:", saveError);
      setFormError("공시 수정에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleCreate() {
    if (!createForm.assetId) {
      setFormError("공시 대상 자산을 선택해 주세요.");
      return;
    }
    if (!createForm.disclosureTitle?.trim()) {
      setFormError("공시 제목을 입력해 주세요.");
      return;
    }
    if (!createForm.disclosureContent?.trim()) {
      setFormError("공시 본문을 입력해 주세요.");
      return;
    }
    if (!createForm.file) {
      setFormError("PDF 파일을 선택해 주세요.");
      return;
    }

    const payload = new FormData();
    payload.append(
      "dto",
      new Blob(
        [
          JSON.stringify({
            assetId: Number(createForm.assetId),
            disclosureTitle: createForm.disclosureTitle.trim(),
            disclosureContent: createForm.disclosureContent.trim(),
            disclosureCategory: createForm.disclosureCategory,
          }),
        ],
        { type: "application/json" },
      ),
    );
    payload.append("file", createForm.file);

    try {
      setSaving(true);
      setFormError("");
      await api.post("/admin/disclosure", payload, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      await loadDisclosures();
      closeCreateModal();
    } catch (createError) {
      console.error("[DisclosureManagement] 등록 실패:", createError);
      setFormError("공시 등록에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <DisclosureList
        disclosures={filteredDisclosures}
        disclosureTypeTab={disclosureTypeTab}
        loading={loading}
        error={error}
        searchTerm={searchTerm}
        onTypeTabChange={setDisclosureTypeTab}
        onSearchChange={setSearchTerm}
        onAdd={() => {
          setFormError("");
          setIsCreateOpen(true);
        }}
        onSelect={handleSelect}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />

      <Modal
        isOpen={Boolean(selectedDisclosure)}
        onClose={() => setSelectedDisclosure(null)}
        title="공시 상세"
        maxWidth="max-w-5xl"
      >
        <DisclosureDetail
          item={selectedDisclosure}
          onEdit={() => handleEdit(selectedDisclosure)}
          onDelete={() => handleDelete(selectedDisclosure)}
        />
      </Modal>

      <Modal
        isOpen={Boolean(editingDisclosure && editingForm)}
        onClose={closeEditModal}
        title="공시 수정"
        maxWidth="max-w-2xl"
      >
        {editingForm && (
          <DisclosureForm
            mode="edit"
            form={editingForm}
            setForm={setEditingForm}
            assets={assets}
            onClose={closeEditModal}
            onSubmit={handleSaveEdit}
            saving={saving}
            error={formError}
          />
        )}
      </Modal>

      <Modal
        isOpen={isCreateOpen}
        onClose={closeCreateModal}
        title="신규 공시 등록"
        maxWidth="max-w-2xl"
      >
        <DisclosureForm
          mode="create"
          form={createForm}
          setForm={setCreateForm}
          assets={assets}
          onClose={closeCreateModal}
          onSubmit={handleCreate}
          saving={saving}
          error={formError}
        />
      </Modal>
    </>
  );
}

export default DisclosureManagement;
