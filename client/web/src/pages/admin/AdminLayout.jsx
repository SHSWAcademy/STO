import { Link, useLocation, useNavigate, Outlet } from 'react-router-dom';
import {
  LayoutDashboard, Users, FileText, Settings, LogOut,
  Search, TrendingUp, DollarSign, BarChart3, Database,
} from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { useApp } from '../../context/AppContext.jsx';
import { StoneLogo } from '../../components/ui/StoneLogo.jsx';

const MENU_ITEMS = [
  { icon: LayoutDashboard, label: '대시보드',        path: '/admin-console' },
  { icon: Users,           label: '사용자 관리',      path: '/admin-console/users' },
  { icon: TrendingUp,      label: '자산 관리',        path: '/admin-console/assets' },
  { icon: BarChart3,       label: '플랫폼 수익/보유', path: '/admin-console/revenue' },
  { icon: DollarSign,      label: '배당 관리',        path: '/admin-console/dividends' },
  { icon: FileText,        label: '공지사항 관리',    path: '/admin-console/notices' },
  { icon: FileText,        label: '공시 관리',        path: '/admin-console/content' },
  { icon: Database,        label: '로그 관리',        path: '/admin-console/logs' },
  { icon: Settings,        label: '시스템 설정',      path: '/admin-console/settings' },
];

export function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useApp();

  function handleLogout() {
    logout();
    navigate('/admin-console');
  }

  // exact match for /admin-console, prefix match for sub-pages
  function isActive(path) {
    if (path === '/admin-console') return location.pathname === '/admin-console';
    return location.pathname.startsWith(path);
  }

  return (
    <div className="flex h-screen bg-stone-100 font-sans overflow-hidden overscroll-none">
      {/* Sidebar */}
      <aside className="w-64 bg-stone-800 text-white flex flex-col shrink-0">
        <div className="p-6 flex items-center gap-3 border-b border-white/10">
          <StoneLogo size={28} />
          <span className="font-black text-lg tracking-tight">STO ADMIN</span>
        </div>

        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          {MENU_ITEMS.map(item => (
            <Link
              key={item.path}
              to={item.path}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-md text-sm font-medium transition-colors',
                isActive(item.path)
                  ? 'bg-brand-blue text-white'
                  : 'text-white/60 hover:text-white hover:bg-white/5',
              )}
            >
              <item.icon className="w-5 h-5" />
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-3 w-full rounded-xl text-sm font-bold text-brand-red hover:bg-brand-red/10 transition-all"
          >
            <LogOut className="w-5 h-5" />
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-h-0 overflow-hidden overscroll-none">
        {/* Header */}
        <header className="h-16 bg-white border-b border-stone-200 flex items-center justify-between px-8 shrink-0">
          <div className="flex items-center gap-4 bg-stone-200 px-4 py-2 rounded-xl w-96">
            <Search className="w-4 h-4 text-stone-400" />
            <input
              type="text"
              placeholder="사용자, 거래번호 검색..."
              className="bg-transparent border-none outline-none text-sm w-full"
            />
          </div>
          <div className="flex items-center gap-3 pl-6 border-l border-stone-200">
            <div className="text-right">
              <p className="text-sm font-black text-stone-800">{user?.name || '관리자'}</p>
              <p className="text-[10px] font-bold text-stone-400">Super Admin</p>
            </div>
            <div className="w-10 h-10 bg-stone-200 rounded-full flex items-center justify-center font-black text-stone-400">
              {user?.name?.[0] ?? 'A'}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden overscroll-contain p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
