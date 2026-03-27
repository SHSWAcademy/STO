import { useState } from 'react';
import { Filter, Download, LogIn, ArrowRightLeft, ShieldAlert, Code, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { TabSwitcher } from '../../components/ui/TabSwitcher.jsx';
import { SearchInput } from '../../components/ui/SearchInput.jsx';
import { Badge } from '../../components/ui/Badge.jsx';

const MOCK_LOGS = {
  user_login: [
    { id: 'L-001', timestamp: '2026-03-25 18:05:12', user: '김철수 (user_ks)',   action: '로그인',      details: 'Chrome / Windows 11',                                  status: 'success', ip: '192.168.0.1' },
    { id: 'L-002', timestamp: '2026-03-25 17:58:45', user: '이영희 (yh_lee)',   action: '로그인 실패',  details: '비밀번호 5회 오류',                                     status: 'failure', ip: '211.234.12.55' },
    { id: 'L-003', timestamp: '2026-03-25 17:45:22', user: '박지민 (jimin_p)',  action: '로그인',      details: 'Safari / iPhone 15',                                   status: 'success', ip: '172.16.254.1' },
  ],
  transaction: [
    { id: 'T-001', timestamp: '2026-03-25 18:10:05', user: '김철수',  action: '매수 주문', details: '강남 빌딩 STO / 10주 / 1,000,000원', status: 'success' },
    { id: 'T-002', timestamp: '2026-03-25 18:08:33', user: '최민호',  action: '매도 주문', details: '제주 리조트 STO / 5주 / 500,000원',   status: 'success' },
    { id: 'T-003', timestamp: '2026-03-25 17:55:12', user: '박지민',  action: '출금 신청', details: '5,000,000원 / 신한은행',              status: 'warning' },
  ],
  admin_login: [
    { id: 'A-001', timestamp: '2026-03-25 09:00:01', user: '관리자 (admin_01)', action: '로그인',   details: 'Edge / Windows Server', status: 'success', ip: '10.0.0.5' },
    { id: 'A-002', timestamp: '2026-03-24 18:30:15', user: '운영자 (op_02)',    action: '로그아웃', details: '세션 종료',               status: 'success', ip: '10.0.0.12' },
  ],
  api_order: [
    { id: 'API-001', timestamp: '2026-03-25 18:12:45', user: 'API_KEY_...928', action: 'POST /v1/orders', details: '{"symbol": "GN-01", "side": "buy", "qty": 10}', status: 'success' },
    { id: 'API-002', timestamp: '2026-03-25 18:11:20', user: 'API_KEY_...112', action: 'POST /v1/orders', details: '{"error": "insufficient_balance"}',              status: 'failure' },
  ],
};

const TABS = [
  { id: 'user_login',  label: '회원 로그인 로그',      icon: LogIn },
  { id: 'transaction', label: '거래 로그',              icon: ArrowRightLeft },
  { id: 'admin_login', label: '관리자 로그인 로그',     icon: ShieldAlert },
  { id: 'api_order',   label: 'API 로그 (주문)',         icon: Code },
];

export function SystemLogs() {
  const [activeTab, setActiveTab] = useState('user_login');
  const [searchTerm, setSearchTerm] = useState('');

  const showIp = activeTab === 'user_login' || activeTab === 'admin_login';

  const STATUS_VARIANT = { success: 'success', failure: 'danger', warning: 'warning' };
  const STATUS_LABEL   = { success: '성공',    failure: '실패',   warning: '경고' };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-[#2a2820]">시스템 로그 관리</h1>
          <p className="text-sm font-bold text-[#9a9080]">플랫폼 내의 모든 활동 로그를 모니터링합니다.</p>
        </div>
        <button className="flex items-center gap-2 px-6 py-2.5 bg-white border border-[#e0dace] rounded-xl text-sm font-bold text-[#7a7060] hover:bg-[#f7f5f0] transition-all">
          <Download size={16} /> 로그 내보내기 (CSV)
        </button>
      </div>

      {/* Tab Nav */}
      <TabSwitcher variant="light" items={TABS} active={activeTab} onChange={setActiveTab} />

      {/* Filter Bar */}
      <div className="bg-white p-6 rounded-2xl border border-[#e0dace] shadow-sm flex items-center gap-4">
        <SearchInput variant="light" value={searchTerm} onChange={setSearchTerm} placeholder="사용자명, 아이디, 상세 내용 검색..." />
        <div className="flex items-center gap-2 bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-2.5">
          <Calendar className="w-4 h-4 text-[#9a9080]" />
          <span className="text-xs font-bold text-[#7a7060]">2026.03.25 ~ 2026.03.25</span>
        </div>
        <button className="p-2.5 bg-white border border-[#e0dace] rounded-xl text-[#9a9080] hover:text-[#2a2820] hover:bg-[#f7f5f0] transition-all">
          <Filter size={18} />
        </button>
      </div>

      {/* Log Table */}
      <div className="bg-white rounded-2xl border border-[#e0dace] shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                {['로그 ID','타임스탬프','사용자/식별자','수행 작업','상세 정보', ...(showIp ? ['IP 주소'] : []), '결과'].map(h => (
                  <th key={h} className={`px-6 py-4 text-[10px] font-black text-[#9a9080] uppercase tracking-wider ${h === '결과' ? 'text-center' : ''}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {MOCK_LOGS[activeTab].map(log => (
                <tr key={log.id} className="hover:bg-[#f7f5f0] transition-all">
                  <td className="px-6 py-4 text-xs font-mono font-bold text-[#9a9080]">{log.id}</td>
                  <td className="px-6 py-4 text-xs font-bold text-[#7a7060]">{log.timestamp}</td>
                  <td className="px-6 py-4 text-xs font-black text-[#2a2820]">{log.user}</td>
                  <td className="px-6 py-4 text-xs font-bold text-[#7a7060]">{log.action}</td>
                  <td className="px-6 py-4"><p className="text-xs font-bold text-[#9a9080] max-w-xs truncate">{log.details}</p></td>
                  {showIp && <td className="px-6 py-4 text-xs font-mono font-bold text-[#9a9080]">{log.ip}</td>}
                  <td className="px-6 py-4 text-center">
                    <Badge variant={STATUS_VARIANT[log.status]}>{STATUS_LABEL[log.status]}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-6 border-t border-[#e0dace] flex items-center justify-between bg-[#f0ede4]">
          <p className="text-xs font-bold text-[#9a9080]">전체 1,284개 중 1-10 표시</p>
          <div className="flex items-center gap-2">
            <button className="p-2 rounded-lg border border-[#e0dace] text-[#9a9080] hover:bg-[#e0dace] transition-all"><ChevronLeft size={16} /></button>
            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map(n => (
                <button key={n} className={cn('w-8 h-8 rounded-lg text-xs font-bold transition-all',
                  n === 1 ? 'bg-[#4a72a0] text-white shadow-md shadow-[#4a72a0]/20' : 'text-[#9a9080] hover:bg-[#e0dace]'
                )}>{n}</button>
              ))}
            </div>
            <button className="p-2 rounded-lg border border-[#e0dace] text-[#9a9080] hover:bg-[#e0dace] transition-all"><ChevronRight size={16} /></button>
          </div>
        </div>
      </div>
    </div>
  );
}
