import axios from "axios";
import { API_BASE_URL } from "./config.js";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// 요청 인터셉터 — 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터 — 401 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const token = localStorage.getItem("token");

    if (error.response?.status === 401 && token) {
      localStorage.removeItem("token");
      window.location.href = "/";
    }
    return Promise.reject(error);
  },
);
export const fetchBalance = () => api.get("/api/myaccount/balance");
export const fetchPortfolio = () => api.get("/api/myaccount/portfolio");
export const deposit = (amount) =>
  api.post("/api/myaccount/deposit", { amount });
export const withdraw = (amount) =>
  api.post("/api/myaccount/withdraw", { amount });
export default api;
