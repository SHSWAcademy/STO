import { useState } from 'react';
import {
  Users, TrendingUp, ArrowUpRight, ArrowDownRight,
  Activity, DollarSign, PieChart, Clock, Search, Filter,
  ChevronDown, Info,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell,
  PieChart as RePieChart, Pie,
} from 'recharts';
import { cn } from '../../lib/utils.js';

const VOLUME_DATA = [
  { name: '03/19', volume: 2400 },
  { name: '03/20', volume: 1398 },
  { name: '03/21', volume: 9800 },
  { name: '03/22', volume: 3908 },
  { name: '03/23', volume: 4800 },
  { name: '03/24', volume: 3800 },
  { name: '03/25', volume: 4300 },
];

const ASSET_DISTRIBUTION = [
  { name: '강남 오피스', value: 45, color: '#4a72a0' },
  { name: '제주 리조트', value: 25, color: '#b85450' },
  { name: '미술품 A',   value: 15, color: '#c9a84c' },
  { name: '기타',       value: 15, color: '#9a9080' },
];

const TOKEN_DETAILS = {
  '강남 오피스': {
    total: 10000,
    traded: 7500,
    holders: [
      { name: '김철수', amount: 1200, percent: 12 },
      { name: '이영희', amount: 850,  percent: 8.5 },
      { name: '박지민', amount: 600,  percent: 6 },
      { name: '최민호', amount: 450,  percent: 4.5 },
      { name: '기타',   amount: 4400, percent: 44 },
    ],
  },
  '제주 리조트': {
    total: 5000,
    traded: 2100,
    holders: [
      { name: '정우성', amount: 500, percent: 10 },
      { name: '한지민', amount: 300, percent: 6 },
      { name: '박서준', amount: 250, percent: 5 },
      { name: '기타',   amount: 1050, percent: 21 },
    ],
  },
  '미술품 A': {
    total: 1000,
    traded: 950,
    holders: [
      { name: '이정재', amount: 200, percent: 20 },
      { name: '공유',   amount: 150, percent: 15 },
      { name: '기타',   amount: 600, percent: 60 },
    ],
  },
};

function StatCard({ title, value, change, icon: Icon, color }) {
  return (
    <div className="bg-white p-6 rounded-lg border border-[#e0dace] transition-colors">
      <div className="flex items-center justify-between mb-4">
        <div className={cn('p-3 rounded-xl', color)}>
          <Icon className="w-6 h-6 text-white" />
        </div>
        <div className={cn(
          'flex items-center gap-1 text-xs font-black',
          change > 0 ? 'text-[#4a72a0]' : 'text-[#b85450]',
        )}>
          {change > 0 ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
          {Math.abs(change)}%
        </div>
      </div>
      <p className="text-xs text-[#9a9080] mb-1">{title}</p>
      <h3 className="text-2xl font-semibold text-[#2a2820]">{value}</h3>
    </div>
  );
}

export function AdminDashboard() {
  const [selectedAsset, setSelectedAsset] = useState('강남 오피스');
  const currentAsset = TOKEN_DETAILS[selectedAsset] || TOKEN_DETAILS['강남 오피스'];
  const untraded = currentAsset.total - currentAsset.traded;
  const tradedPercent = (currentAsset.traded / currentAsset.total) * 100;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#2a2820]">어드민 대시보드</h1>
          <p className="text-sm text-[#9a9080]">플랫폼의 실시간 현황을 한눈에 확인하세요.</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="px-4 py-2 bg-white border border-[#e0dace] rounded-md text-sm font-medium text-[#7a7060] hover:bg-[#f7f5f0] flex items-center gap-2">
            <Clock className="w-4 h-4" /> 최근 7일
          </button>
          <button className="px-4 py-2 bg-[#4a72a0] text-white rounded-md text-sm font-medium hover:bg-[#3a62a0] transition-colors">
            보고서 다운로드
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-6">
        <StatCard title="총 가입자 수"  value="12,482명"  change={12.5}  icon={Users}    color="bg-[#4a72a0]" />
        <StatCard title="일일 거래액"   value="4.28억원"  change={-2.4}  icon={DollarSign} color="bg-[#b85450]" />
        <StatCard title="활성 사용자"   value="1,248명"   change={8.2}   icon={Activity}  color="bg-[#9a9080]" />
        <StatCard title="총 예치금"     value="128.5억원" change={5.1}   icon={PieChart}  color="bg-[#b85450]" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-2 gap-8">
        <div className="bg-white p-8 rounded-lg border border-[#e0dace]">
          <h3 className="text-lg font-semibold text-[#2a2820] mb-6">자산별 거래 비중</h3>
          <div className="h-[300px] flex items-center">
            <div className="flex-1 h-full">
              <ResponsiveContainer width="100%" height="100%">
                <RePieChart>
                  <Pie data={ASSET_DISTRIBUTION} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={5} dataKey="value">
                    {ASSET_DISTRIBUTION.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 20px rgba(0,0,0,0.05)' }} />
                </RePieChart>
              </ResponsiveContainer>
            </div>
            <div className="w-40 space-y-3">
              {ASSET_DISTRIBUTION.map(item => (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                    <span className="text-xs font-bold text-[#7a7060]">{item.name}</span>
                  </div>
                  <span className="text-xs font-black text-[#2a2820]">{item.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-white p-8 rounded-lg border border-[#e0dace]">
          <h3 className="text-lg font-semibold text-[#2a2820] mb-6">일별 거래량 추이</h3>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={VOLUME_DATA}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e0dace" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#9a9080' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#9a9080' }} />
                <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 20px rgba(0,0,0,0.05)' }} labelStyle={{ fontWeight: 800, color: '#2a2820', marginBottom: '4px' }} />
                <Bar dataKey="volume" fill="#b85450" radius={[4, 4, 0, 0]} barSize={24} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Token Issuance & Ownership Analysis */}
      <div className="bg-white p-8 rounded-lg border border-[#e0dace] space-y-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-[#2a2820]">토큰 발행 및 소유권 분석</h3>
            <div className="group relative">
              <Info size={14} className="text-[#9a9080] cursor-help" />
              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 p-2 bg-[#2a2820] text-white text-[10px] rounded-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                발행된 총 토큰 중 유통 중인 물량과 주요 보유자 현황을 나타냅니다.
              </div>
            </div>
          </div>
          <div className="relative">
            <select
              value={selectedAsset}
              onChange={e => setSelectedAsset(e.target.value)}
              className="appearance-none bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-4 py-2 pr-10 text-sm font-bold text-[#7a7060] outline-none focus:border-[#4a72a0] cursor-pointer"
            >
              {Object.keys(TOKEN_DETAILS).map(asset => (
                <option key={asset} value={asset}>{asset}</option>
              ))}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#9a9080] pointer-events-none" />
          </div>
        </div>

        <div className="grid lg:grid-cols-3 gap-12">
          <div className="lg:col-span-2 space-y-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest block">발행 및 유통 현황</span>
                <p className="text-sm font-black text-[#2a2820]">총 {currentAsset.total.toLocaleString()} 토큰</p>
              </div>
              <div className="text-right">
                <span className="text-[10px] font-black text-[#4a72a0] uppercase tracking-widest block">유통 비율</span>
                <p className="text-xl font-black text-[#4a72a0]">{tradedPercent.toFixed(1)}%</p>
              </div>
            </div>

            <div className="flex gap-12 items-center">
              <div className="grid grid-cols-10 gap-1 w-full max-w-[240px] aspect-square p-2 bg-[#f7f5f0] rounded-2xl border border-[#e0dace]">
                {Array.from({ length: 100 }).map((_, i) => {
                  const isTraded = i < Math.round(tradedPercent);
                  return (
                    <div
                      key={i}
                      className={cn('w-full h-full rounded-[2px]', isTraded ? 'bg-[#4a72a0]' : 'bg-[#e0dace]')}
                    />
                  );
                })}
              </div>

              <div className="flex-1 space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 rounded-[2px] bg-[#4a72a0]" />
                    <div>
                      <p className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest leading-none mb-1">유통 중</p>
                      <p className="text-sm font-black text-[#2a2820]">{currentAsset.traded.toLocaleString()} 토큰</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 rounded-[2px] bg-[#e0dace]" />
                    <div>
                      <p className="text-[10px] font-black text-[#9a9080] uppercase tracking-widest leading-none mb-1">미유통 (회사 보유)</p>
                      <p className="text-sm font-black text-[#2a2820]">{untraded.toLocaleString()} 토큰</p>
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-[#f7f5f0] rounded-xl border border-[#e0dace] space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold text-[#9a9080]">실시간 유통 밀도</span>
                    <span className="text-[10px] font-black text-[#4a72a0]">High</span>
                  </div>
                  <div className="h-1.5 w-full bg-[#e0dace] rounded-full overflow-hidden">
                    <div className="h-full bg-[#4a72a0] rounded-full" style={{ width: `${tradedPercent}%` }} />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <span className="text-xs font-black text-[#9a9080] uppercase tracking-widest">주요 보유자 (Top Holders)</span>
            <div className="space-y-3">
              {currentAsset.holders.map((holder, idx) => (
                <div key={idx} className="flex items-center justify-between p-3 bg-[#f7f5f0] rounded-xl border border-transparent hover:border-[#e0dace] transition-all">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-white rounded-lg border border-[#e0dace] flex items-center justify-center text-[10px] font-black text-[#9a9080]">
                      {idx + 1}
                    </div>
                    <div>
                      <p className="text-xs font-black text-[#2a2820]">{holder.name}</p>
                      <p className="text-[10px] font-bold text-[#9a9080]">{holder.amount.toLocaleString()} 토큰</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-black text-[#4a72a0]">{holder.percent}%</p>
                    <div className="w-16 h-1 bg-[#e0dace] rounded-full mt-1 overflow-hidden">
                      <div className="h-full bg-[#4a72a0]" style={{ width: `${holder.percent}%` }} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="bg-white rounded-lg border border-[#e0dace] overflow-hidden">
        <div className="p-6 border-b border-[#e0dace] flex items-center justify-between">
          <div className="flex items-center gap-6">
            <h3 className="text-lg font-semibold text-[#2a2820]">최근 거래 내역</h3>
            <div className="flex items-center gap-2 bg-[#f7f5f0] border border-[#e0dace] rounded-xl px-3 py-1.5">
              <Search className="w-3.5 h-3.5 text-[#9a9080]" />
              <input type="text" placeholder="거래번호, 사용자 검색..." className="bg-transparent border-none outline-none text-xs font-bold w-48" />
            </div>
          </div>
          <button className="text-sm font-bold text-[#4a72a0] hover:underline">전체 보기</button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                {['거래번호','사용자','종목','유형','수량','금액','상태'].map(h => (
                  <th key={h} className="px-6 py-4 text-[10px] font-semibold text-[#9a9080] uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {[
                { id: 'TX-10294', user: '김철수', stock: '강남 빌딩 STO', type: '매수', qty: '10주', amount: '1,000,000원', status: '완료' },
                { id: 'TX-10293', user: '이영희', stock: '제주 리조트 STO', type: '매도', qty: '5주', amount: '500,000원', status: '완료' },
                { id: 'TX-10292', user: '박지민', stock: '강남 빌딩 STO', type: '매수', qty: '20주', amount: '2,000,000원', status: '대기' },
                { id: 'TX-10291', user: '최민호', stock: '미술품 STO', type: '매수', qty: '1주', amount: '10,000,000원', status: '완료' },
              ].map((tx, i) => (
                <tr key={i} className="hover:bg-[#f7f5f0] transition-all">
                  <td className="px-6 py-4 text-sm font-mono font-bold text-[#7a7060]">{tx.id}</td>
                  <td className="px-6 py-4 text-sm font-bold text-[#2a2820]">{tx.user}</td>
                  <td className="px-6 py-4 text-sm font-bold text-[#7a7060]">{tx.stock}</td>
                  <td className="px-6 py-4">
                    <span className={cn('px-2 py-1 rounded-md text-[10px] font-semibold',
                      tx.type === '매수' ? 'bg-[#fde8e8] text-[#b04040]' : 'bg-[#e8f0fa] text-[#3a62a0]'
                    )}>{tx.type}</span>
                  </td>
                  <td className="px-6 py-4 text-sm text-[#7a7060]">{tx.qty}</td>
                  <td className="px-6 py-4 text-sm font-semibold text-[#2a2820]">{tx.amount}</td>
                  <td className="px-6 py-4">
                    <span className={cn('px-2 py-1 rounded-md text-[10px] font-semibold',
                      tx.status === '대기' ? 'bg-[#fef6dc] text-[#a07828]' : 'bg-[#e8f4ee] text-[#3d7a58]'
                    )}>{tx.status}</span>
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
