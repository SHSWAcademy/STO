import { useState } from 'react';
import { Users as UsersIcon, Filter, Shield, UserCheck, UserMinus, Mail, Calendar, DollarSign } from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { SearchInput } from '../../components/ui/SearchInput.jsx';
import { Badge } from '../../components/ui/Badge.jsx';

const INITIAL_USERS = [
  { id: 'U001', name: '홍길동', email: 'hong@test.com',  joined: '2025-11-03', invested: 24150000, status: '활성', role: 'user' },
  { id: 'U002', name: '김철수', email: 'kim@test.com',   joined: '2025-12-15', invested: 8720000,  status: '활성', role: 'user' },
  { id: 'A001', name: '이영희', email: 'lee@admin.com',  joined: '2025-10-01', invested: 0,        status: '활성', role: 'admin' },
  { id: 'S001', name: '박민준', email: 'park@super.com', joined: '2025-09-01', invested: 0,        status: '활성', role: 'super-admin' },
  { id: 'U003', name: '정다은', email: 'jung@test.com',  joined: '2026-01-20', invested: 1500000,  status: '정지', role: 'user' },
];

export function UserManagement() {
  const [users, setUsers] = useState(INITIAL_USERS);

  function handleRoleChange(userId, newRole) {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u));
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-[#2a2820]">사용자 관리</h1>
          <p className="text-sm font-bold text-[#9a9080]">플랫폼 가입자 현황 및 권한을 관리합니다.</p>
        </div>
        <div className="flex items-center gap-3">
          <SearchInput variant="light" value="" onChange={() => {}} placeholder="이름, 이메일 검색..." />
          <button className="p-2.5 bg-white border border-[#e0dace] rounded-xl text-[#7a7060] hover:bg-[#f7f5f0] shadow-sm">
            <Filter className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-6">
        {[
          { label: '전체 사용자',     value: '12,482', icon: UsersIcon,  color: 'text-[#c9a84c]', bg: 'bg-[#ffe65a]/30' },
          { label: '신규 가입 (오늘)', value: '124',    icon: UserCheck,  color: 'text-[#b85450]', bg: 'bg-[#4a3232]' },
          { label: '정지된 계정',     value: '42',     icon: UserMinus,  color: 'text-[#b85450]', bg: 'bg-[#4a3232]' },
          { label: '관리자 계정',     value: '8',      icon: Shield,     color: 'text-[#9a9080]', bg: 'bg-[#e0dace]' },
        ].map((stat, i) => (
          <div key={i} className="bg-white p-6 rounded-2xl border border-[#e0dace] shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className={cn('p-3 rounded-xl', stat.bg)}>
                <stat.icon className={cn('w-6 h-6', stat.color)} />
              </div>
            </div>
            <p className="text-xs font-bold text-[#9a9080] mb-1">{stat.label}</p>
            <h3 className="text-2xl font-black text-[#2a2820]">{stat.value}</h3>
          </div>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-[#e0dace] shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                {['사용자 정보','가입일','총 투자액','권한','상태'].map(h => (
                  <th key={h} className="px-6 py-4 text-[10px] font-black text-[#9a9080] uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {users.map(user => (
                <tr key={user.id} className="hover:bg-[#f7f5f0] transition-all group">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-[#e0dace] rounded-full flex items-center justify-center font-black text-[#9a9080]">
                        {user.name[0]}
                      </div>
                      <div>
                        <p className="text-sm font-black text-[#2a2820]">{user.name}</p>
                        <p className="text-xs font-bold text-[#9a9080] flex items-center gap-1">
                          <Mail className="w-3 h-3" /> {user.email}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 text-sm font-bold text-[#7a7060]">
                      <Calendar className="w-4 h-4 text-[#9a9080]" /> {user.joined}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1 text-sm font-black text-[#2a2820]">
                      <DollarSign className="w-4 h-4 text-[#9a9080]" />
                      {user.invested.toLocaleString()}원
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <select
                      value={user.role}
                      onChange={e => handleRoleChange(user.id, e.target.value)}
                      className={cn(
                        'px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wider border-none outline-none cursor-pointer transition-all',
                        user.role === 'super-admin' ? 'bg-[#e0dace] text-[#7a7060]'
                          : user.role === 'admin' ? 'bg-[#ffe65a]/30 text-[#c9a84c]'
                          : user.role === 'ban' ? 'bg-[#4a3232] text-[#b85450]'
                          : 'bg-[#f7f5f0] text-[#7a7060]'
                      )}
                    >
                      <option value="user">USER</option>
                      <option value="admin">ADMIN</option>
                      <option value="ban">BAN (이용정지)</option>
                    </select>
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant={user.role === 'ban' || user.status === '정지' ? 'danger' : 'success'}>
                      {user.role === 'ban' ? '정지됨' : user.status}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
