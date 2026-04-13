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

const DEFAULT_PAGE_META = {
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function unwrapNoticePage(data) {
  return data?.content ? data : data?.data?.content ? data.data : data?.result?.content ? data.result : data;
}

function buildArrayPage(data, page, size) {
  if (!Array.isArray(data)) return null;

  return {
    content: data,
    number: page,
    size,
    totalElements: data.length,
    totalPages: data.length ? Math.ceil(data.length / size) : 0,
    first: page === 0,
    last: true,
  };
}

export function NoticeManagement() {
  const [notices, setNotices] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [pageMeta, setPageMeta] = useState(DEFAULT_PAGE_META);
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
      const { data } = await api.get("/admin/notice", {
        params: { page, size },
      });
      const noticePage = buildArrayPage(data, page, size) ?? unwrapNoticePage(data);
      const content = noticePage?.content;

      if (!Array.isArray(content)) {
        console.error("[NoticeManagement] unexpected list response:", data);
        setNotices([]);
        setPageMeta({ ...DEFAULT_PAGE_META, size });
        setError("공지사항 응답 형식이 올바르지 않습니다.");
        return;
      }

      setNotices(content.map(mapNoticeListItem));
      setPageMeta({
        number: noticePage?.number ?? page,
        size: noticePage?.size ?? size,
        totalElements: noticePage?.totalElements ?? 0,
        totalPages: noticePage?.totalPages ?? 0,
        first: noticePage?.first ?? page === 0,
        last: noticePage?.last ?? (noticePage?.totalPages ?? 0) <= page + 1,
      });
    } catch (loadError) {
      console.error("[NoticeManagement] list load failed:", loadError);
      setError("공지사항 목록을 불러오지 못했습니다.");
      setNotices([]);
      setPageMeta({ ...DEFAULT_PAGE_META, size });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadNotices();
  }, [page, size]);

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
      console.error("[NoticeManagement] detail load failed:", detailError);
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
      console.error("[NoticeManagement] detail load failed:", detailLoadError);
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
        setPage(0);
      }

      await loadNotices();
      closeModal();
    } catch (saveError) {
      console.error("[NoticeManagement] save failed:", saveError);
      setFormError("공지사항 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(notice) {
    if (notice.deletedAt) return;

    try {
      await api.delete(`/admin/notice/${notice.noticeId}`);
      setSelectedNotice((prev) =>
        prev?.noticeId === notice.noticeId
          ? { ...prev, deletedAt: new Date().toISOString() }
          : prev,
      );
      setEditingNotice((prev) =>
        prev?.noticeId === notice.noticeId ? null : prev,
      );
      await loadNotices();
    } catch (deleteError) {
      console.error("[NoticeManagement] delete failed:", deleteError);
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
        pageMeta={pageMeta}
        pageSize={size}
        onSearch={setSearchTerm}
        onPageChange={setPage}
        onPageSizeChange={(nextSize) => {
          setSize(nextSize);
          setPage(0);
          setSearchTerm("");
        }}
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
