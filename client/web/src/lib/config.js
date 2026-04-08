export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const FILE_URLS = {
  imageBase: `${API_BASE_URL}/file/images`,
  pdfViewBase: `${API_BASE_URL}/file/pdf/view`,
  pdfDownloadBase: `${API_BASE_URL}/file/pdf/download`,
};
