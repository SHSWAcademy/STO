import { createContext, useContext, useState } from 'react';
import { TOKENS as INITIAL_TOKENS, ADMIN_DISCLOSURES, ADMIN_NOTICES } from '../data/mock.js';
import { API_BASE_URL } from '../lib/config.js';

const API = API_BASE_URL;

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [user, setUser]               = useState(null);
  const [watchlist, setWatchlist]     = useState([]);
  const [tokens, setTokens]           = useState(INITIAL_TOKENS);
  const [disclosures, setDisclosures] = useState(ADMIN_DISCLOSURES);
  const [notices, setNotices]         = useState(ADMIN_NOTICES);

  // 실제 로그인 API 호출
  // ADMIN/ADMIN → 관리자 로그인, 그 외 → 회원 로그인
  async function login(email, password) {
    const isAdminInput =
      email.trim().toUpperCase() === 'ADMIN' &&
      password.trim().toUpperCase() === 'ADMIN';

    try {
      let res, data;

      if (isAdminInput) {
        res = await fetch(`${API}/api/auth/admin/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ adminLoginId: email, password }),
        });
      } else {
        res = await fetch(`${API}/api/auth/member/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password }),
        });
      }

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      data = await res.json();

      const isAdmin = data.userType === 'ADMIN' || isAdminInput;
      localStorage.setItem('token', data.accessToken);
      setUser({
        name:        isAdmin ? '관리자' : email,
        email:       email,
        role:        isAdmin ? 'admin' : 'user',
        accessToken: data.accessToken,
      });
      return isAdmin;

    } catch (e) {
      console.warn('[Login] API 실패, mock 로그인으로 대체:', e.message);
      // 백엔드 미실행 시 mock fallback
      const isAdmin = isAdminInput;
      setUser({
        name:        isAdmin ? '관리자' : '홍길동',
        email:       isAdmin ? 'admin@sto.exchange' : 'demo@sto.exchange',
        role:        isAdmin ? 'admin' : 'user',
        accessToken: null,
      });
      return isAdmin;
    }
  }

  function logout() {
    localStorage.removeItem('token');
    setUser(null);
  }

  function toggleWatchlist(assetId) {
    setWatchlist(prev =>
      prev.includes(assetId) ? prev.filter(id => id !== assetId) : [...prev, assetId]
    );
  }

  return (
    <AppContext.Provider value={{
      user, login, logout,
      watchlist, toggleWatchlist,
      tokens, setTokens,
      disclosures, setDisclosures,
      notices, setNotices,
    }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}
