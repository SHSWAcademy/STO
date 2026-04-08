import { useEffect, useState } from "react";
import { Modal } from "../../../components/ui/Modal.jsx";
import api from "../../../lib/api.js";
import { NoticeDetail } from "./NoticeDetail.jsx";
import { NoticeForm } from "./NoticeForm.jsx";
import { NoticeList } from "./NoticeList.jsx";
import {
  buildNoticePayload,
  mapNoticeDetail,
  mapNoticeListItem,
} from "./noticeUtils.jsx";

const EMPTY_NOTICE_FORM = {
  noticeType: "GENERAL",
  noticeTitle: "",
  noticeContent: "",
};

export function NoticeManagement() {
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingNotice, setEditingNotice] = useState(null);
  const [selectedNotice, setSelectedNotice] = useState(null);
  const [form, setForm] = useState(EMPTY_NOTICE_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  async function loadNotices() {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/admin/notice");
      setNotices((data ?? []).map(mapNoticeListItem));
    } catch (loadError) {
      console.error("[NoticeManagement] 목록 조회 실패:", loadError);
      setError("공지사항 목록을 불러오지 못했습니다.");
      setNotices([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadNotices();
  }, []);

  function handleAdd() {
    setEditingNotice(null);
    setSelectedNotice(null);
    setForm(EMPTY_NOTICE_FORM);
    setFormError("");
    setIsModalOpen(true);
  }

  async function handleEdit(notice) {
    setFormError("");
    try {
      const { data } = await api.get(`/admin/notice/${notice.noticeId}`);
      setEditingNotice(notice);
      setSelectedNotice(null);
      setForm(mapNoticeDetail(data));
      setIsModalOpen(true);
    } catch (detailError) {
      console.error("[NoticeManagement] 상세 조회 실패:", detailError);
      setError("공지사항 상세 정보를 불러오지 못했습니다.");
    }
  }

  async function handleSelect(notice) {
    setDetailLoading(true);
    setDetailError("");
    setSelectedNotice(null);
    try {
      const { data } = await api.get(`/admin/notice/${notice.noticeId}`);
      setSelectedNotice({ ...notice, ...mapNoticeDetail(data) });
    } catch (detailLoadError) {
      console.error("[NoticeManagement] 상세 조회 실패:", detailLoadError);
      setDetailError("공지사항 상세 정보를 불러오지 못했습니다.");
    } finally {
      setDetailLoading(false);
    }
  }

  function closeModal() {
    setIsModalOpen(false);
    setEditingNotice(null);
    setForm(EMPTY_NOTICE_FORM);
    setFormError("");
  }

  async function handleSubmit() {
    if (!form.noticeTitle.trim()) {
      setFormError("공지 제목을 입력해 주세요.");
      return;
    }

    if (!form.noticeContent.trim()) {
      setFormError("공지 내용을 입력해 주세요.");
      return;
    }

    const payload = buildNoticePayload(form);

    try {
      setSaving(true);
      setFormError("");
      if (editingNotice) {
        await api.patch(`/admin/notice/${editingNotice.noticeId}`, payload);
      } else {
        await api.post("/admin/notice", payload);
      }
      await loadNotices();
      closeModal();
    } catch (saveError) {
      console.error("[NoticeManagement] 저장 실패:", saveError);
      setFormError("공지사항 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(notice) {
    if (notice.deletedAt) {
      return;
    }

    try {
      await api.delete(`/admin/notice/${notice.noticeId}`);
      const deletedAt = new Date().toISOString();
      setNotices((prev) =>
        prev.map((item) =>
          item.noticeId === notice.noticeId
            ? { ...item, deletedAt }
            : item,
        ),
      );
      setSelectedNotice((prev) =>
        prev?.noticeId === notice.noticeId ? { ...prev, deletedAt } : prev,
      );
      setEditingNotice((prev) =>
        prev?.noticeId === notice.noticeId ? null : prev,
      );
      await loadNotices();
    } catch (deleteError) {
      console.error("[NoticeManagement] 삭제 실패:", deleteError);
      setError("공지사항 삭제에 실패했습니다.");
    }
  }

  return (
    <>
      <NoticeList
        notices={notices}
        loading={loading}
        error={error}
        searchTerm={searchTerm}
        onSearch={setSearchTerm}
        onAdd={handleAdd}
        onSelect={handleSelect}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />

      <Modal
        isOpen={Boolean(selectedNotice) || detailLoading || Boolean(detailError)}
        onClose={() => {
          setSelectedNotice(null);
          setDetailError("");
        }}
        title="공지사항 상세"
        maxWidth="max-w-3xl"
      >
        <NoticeDetail
          notice={selectedNotice}
          loading={detailLoading}
          error={detailError}
        />
      </Modal>

      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingNotice ? "공지사항 수정" : "신규 공지사항 등록"}
        maxWidth="max-w-3xl"
      >
        <NoticeForm
          form={form}
          setForm={setForm}
          onClose={closeModal}
          onSubmit={handleSubmit}
          editingNotice={editingNotice}
          saving={saving}
          error={formError}
        />
      </Modal>
    </>
  );
}

export default NoticeManagement;
