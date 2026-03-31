import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AppProvider, useApp }  from './context/AppContext.jsx';
import { AppHeader }            from './components/AppHeader.jsx';
import { AuthPage }             from './pages/AuthPage.jsx';
import { TradingPage }          from './pages/TradingPage.jsx';
import { DashboardPage }        from './pages/DashboardPage.jsx';
import { MyAccountPage }        from './pages/MyAccountPage.jsx';
import { WatchlistPage }        from './pages/WatchlistPage.jsx';
import { DisclosurePage }       from './pages/DisclosurePage.jsx';
import { NoticePage }           from './pages/NoticePage.jsx';

// Admin
import { AdminLayout }          from './pages/admin/AdminLayout.jsx';
import { AdminDashboard }       from './pages/admin/AdminDashboard.jsx';
import { UserManagement }       from './pages/admin/UserManagement.jsx';
import { AssetManagement }      from './pages/admin/AssetManagement.jsx';
import { PlatformRevenue }      from './pages/admin/PlatformRevenue.jsx';
import { DividendManagement }   from './pages/admin/DividendManagement.jsx';
import { ContentManagement }    from './pages/admin/ContentManagement.jsx';
import { SystemLogs }           from './pages/admin/SystemLogs.jsx';
import { SystemSettings }       from './pages/admin/SystemSettings.jsx';

// 일반 페이지 공통 패딩 래퍼
function PageWrapper({ children }) {
  return (
    <div className="max-w-7xl mx-auto w-full p-8">
      {children}
    </div>
  );
}

// 일반 유저 레이아웃 (AppHeader + Outlet)
function MainLayout() {
  return (
    <div className="min-h-screen bg-stone-100 text-stone-800">
      <AppHeader />
      <main>
        <Outlet />
      </main>
    </div>
  );
}

// 로그인 상태에 따라 분기
function AppContent() {
  const { user } = useApp();

  if (!user) return <AuthPage />;

  return (
    <Routes>
      {/* 관리자 라우트 (AdminLayout 사용, AppHeader 없음) */}
      <Route path="/admin" element={<AdminLayout />}>
        <Route index             element={<AdminDashboard />} />
        <Route path="users"     element={<UserManagement />} />
        <Route path="assets"    element={<AssetManagement />} />
        <Route path="revenue"   element={<PlatformRevenue />} />
        <Route path="dividends" element={<DividendManagement />} />
        <Route path="content"   element={<ContentManagement />} />
        <Route path="logs"      element={<SystemLogs />} />
        <Route path="settings"  element={<SystemSettings />} />
      </Route>

      {/* 일반 유저 라우트 (MainLayout 사용) */}
      <Route element={<MainLayout />}>
        <Route path="/"           element={<PageWrapper><DashboardPage /></PageWrapper>} />
        <Route path="/trading"    element={<TradingPage />} />
        <Route path="/portfolio"  element={<PageWrapper><MyAccountPage /></PageWrapper>} />
        <Route path="/watchlist"  element={<PageWrapper><WatchlistPage /></PageWrapper>} />
        <Route path="/disclosure" element={<PageWrapper><DisclosurePage /></PageWrapper>} />
        <Route path="/notice"     element={<PageWrapper><NoticePage /></PageWrapper>} />
        <Route path="*"           element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  );
}
