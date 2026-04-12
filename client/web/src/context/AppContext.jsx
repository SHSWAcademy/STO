import { createContext, useContext, useState } from 'react';
import { TOKENS as INITIAL_TOKENS, ADMIN_DISCLOSURES, ADMIN_NOTICES } from '../data/mock.js';
import { API_BASE_URL } from '../lib/config.js';

const API = API_BASE_URL;

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [watchlist, setWatchlist] = useState([]);
  const [tokens, setTokens] = useState(INITIAL_TOKENS);
  const [disclosures, setDisclosures] = useState(ADMIN_DISCLOSURES);
  const [notices, setNotices] = useState(ADMIN_NOTICES);

  async function login(email, password) {
    const res = await fetch(`${API}/api/auth/member/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const data = await res.json();
    localStorage.setItem('token', data.accessToken);
    setUser({
      name: email,
      email,
      role: 'user',
      accessToken: data.accessToken,
    });
    return false;
  }

  async function loginAdmin(adminLoginId, password) {
    const res = await fetch(`${API}/api/auth/admin/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ adminLoginId, password }),
    });

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    if (data.userType !== 'ADMIN') {
      throw new Error(`Unexpected userType ${data.userType}`);
    }

    localStorage.setItem('token', data.accessToken);
    setUser({
      name: data.adminName ?? data.name ?? adminLoginId,
      email: data.email ?? adminLoginId,
      role: 'admin',
      accessToken: data.accessToken,
    });
  }

  function logout() {
    localStorage.removeItem('token');
    setUser(null);
  }

  function toggleWatchlist(assetId) {
    setWatchlist((prev) =>
      prev.includes(assetId) ? prev.filter((id) => id !== assetId) : [...prev, assetId],
    );
  }

  return (
    <AppContext.Provider
      value={{
        user,
        login,
        loginAdmin,
        logout,
        watchlist,
        toggleWatchlist,
        tokens,
        setTokens,
        disclosures,
        setDisclosures,
        notices,
        setNotices,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}
