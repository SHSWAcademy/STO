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

// 응답 인터셉터 - 401 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const token = localStorage.getItem("token");
    const errorCode = error.response?.data?.errorCode;
    const shouldLogout =
      error.response?.status === 401 &&
      token &&
      ["UNAUTHORIZED", "INVALID_TOKEN", "TOKEN_EXPIRED"].includes(errorCode);

    if (shouldLogout) {
      localStorage.removeItem("token");
      window.location.href = "/";
    }
    return Promise.reject(error);
  },
);
export const fetchBalance = () => api.get("/api/myAccount/balance");
export const fetchPortfolio = () => api.get("/api/myAccount/portfolio");
export const deposit = (amount) =>
  api.post("/api/myAccount/deposit", { amount });
export const withdraw = (amount) =>
  api.post("/api/myAccount/withdraw", { amount });
export const fetchBankingHistory = (page = 0, txTypes = []) =>
  api.get("/api/myAccount/history", {
    params: {
      page,
      size: 10,
      ...(txTypes.length > 0 && { txTypes }),
    },
    paramsSerializer: (params) => {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (Array.isArray(value)) {
          value.forEach((v) => searchParams.append(key, v));
        } else {
          searchParams.append(key, value);
        }
      });
      return searchParams.toString();
    },
  });

export const fetchOrderHistory = (page = 0, orderTab = "all") =>
  api.get("/api/myAccount/orders", {
    params: { page, size: 10, orderTab },
  });

export const cancelOrder = (orderId, accountPassword) =>
  api.delete(`/api/token/order/cancel/${orderId}`, {
    data: { accountPassword },
  });

export const fetchDividendHistory = (
  page = 0,
  year = new Date().getFullYear(),
  month = null,
  size = 10,
) =>
  api.get("/api/myAccount/dividends", {
    params: { page, size, year, ...(month && { month }) },
  });

export const fetchDividendTotal = (year = new Date().getFullYear()) =>
  api.get("/api/myAccount/dividends/total", {
    params: { year },
  });

export const fetchAccountSummary = (year, month) =>
  api.get("/api/myAccount/summary", { params: { year, month } });

export const fetchSellHistory = (year, month, page = 0, size = 1000) =>
  api.get("/api/myAccount/sell-history", {
    params: { year, month, page, size },
  });
export default api;
