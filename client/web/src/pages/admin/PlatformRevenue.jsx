import { DollarSign, PieChart, Wallet, BarChart3, Layers, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { TOKENS } from '../../data/mock.js';
import { cn } from '../../lib/utils.js';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

const REVENUE_DATA = [
  { name: '03-19', revenue: 1250000, volume: 450000000 },
  { name: '03-20', revenue: 1450000, volume: 520000000 },
  { name: '03-21', revenue: 1100000, volume: 380000000 },
  { name: '03-22', revenue: 1850000, volume: 680000000 },
  { name: '03-23', revenue: 2100000, volume: 750000000 },
  { name: '03-24', revenue: 1950000, volume: 710000000 },
  { name: '03-25', revenue: 2400000, volume: 820000000 },
];

// 정적 플랫폼 보유 비율 (Math.random 제거)
const PLATFORM_PERCENTS = { SEOULST: 8, SONGDORE: 12, ARTPRIME: 6, JEJU1: 15, LOGISHUB: 10, SOLAR1: 9 };

export function PlatformRevenue() {
  const platformHoldings = TOKENS.map(t => {
    const platformPercent = PLATFORM_PERCENTS[t.id] ?? 10;
    const platformTokens  = Math.floor(t.issued * (platformPercent / 100));
    const value           = platformTokens * t.price;
    return { ...t, platformPercent, platformTokens, value };
  });

  const totalPlatformValue = platformHoldings.reduce((acc, h) => acc + h.value, 0);
  const totalRevenue       = REVENUE_DATA.reduce((acc, d) => acc + d.revenue, 0);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-black text-[#2a2820]">플랫폼 수익 및 보유 현황</h1>
        <p className="text-sm font-bold text-[#9a9080]">플랫폼의 거래 수수료 수익과 직접 보유 중인 토큰 현황을 관리합니다.</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-6">
        {[
          { label: '누적 수수료 수익',      value: `₩${totalRevenue.toLocaleString()}`,         icon: DollarSign, color: 'text-[#c9a84c]', bg: 'bg-[#ffe65a]/30', trend: '+12.5%' },
          { label: '플랫폼 보유 자산 가치',  value: `₩${totalPlatformValue.toLocaleString()}`,    icon: Wallet,     color: 'text-[#b85450]', bg: 'bg-[#4a3232]',    trend: '+5.2%' },
          { label: '평균 보유 지분율',       value: '10.0%',                                       icon: PieChart,   color: 'text-[#b85450]', bg: 'bg-[#4a3232]',    trend: '0.0%' },
        ].map((stat, i) => (
          <div key={i} className="bg-white p-6 rounded-2xl border border-[#e0dace] shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className={cn('p-3 rounded-xl', stat.bg)}>
                <stat.icon className={cn('w-6 h-6', stat.color)} />
              </div>
              <span className={cn(
                'text-[10px] font-black px-2 py-1 rounded-lg flex items-center gap-1',
                stat.trend.startsWith('+') ? 'bg-[#e0f0e8] text-[#4a7a60]' : 'bg-[#f7f5f0] text-[#9a9080]'
              )}>
                {stat.trend.startsWith('+') ? <ArrowUpRight size={10} /> : <ArrowDownRight size={10} />}
                {stat.trend}
              </span>
            </div>
            <p className="text-xs font-bold text-[#9a9080] mb-1">{stat.label}</p>
            <h3 className="text-xl font-black text-[#2a2820]">{stat.value}</h3>
          </div>
        ))}
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Revenue Chart */}
        <div className="lg:col-span-2 bg-white border border-[#e0dace] rounded-2xl p-8 shadow-sm">
          <div className="flex items-center justify-between mb-8">
            <h3 className="text-sm font-black text-[#2a2820] uppercase tracking-widest flex items-center gap-2">
              <BarChart3 size={16} className="text-[#4a72a0]" /> 일자별 수수료 수익 추이
            </h3>
            <div className="flex gap-2">
              <button className="px-3 py-1 rounded-lg bg-[#e0dace] text-[10px] font-black text-[#7a7060]">7일</button>
              <button className="px-3 py-1 rounded-lg text-[10px] font-black text-[#9a9080]">30일</button>
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={REVENUE_DATA}>
                <defs>
                  <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4a72a0" stopOpacity={0.1} />
                    <stop offset="95%" stopColor="#4a72a0" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e0dace" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#9a9080' }} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#9a9080' }} tickFormatter={v => `₩${(v / 10000).toLocaleString()}만`} />
                <Tooltip contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)', padding: '12px' }} />
                <Area type="monotone" dataKey="revenue" stroke="#4a72a0" strokeWidth={3} fillOpacity={1} fill="url(#colorRev)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Holdings Distribution */}
        <div className="bg-white border border-[#e0dace] rounded-2xl p-8 shadow-sm">
          <h3 className="text-sm font-black text-[#2a2820] uppercase tracking-widest mb-8 flex items-center gap-2">
            <Layers size={16} className="text-[#b85450]" /> 자산별 보유 비중
          </h3>
          <div className="space-y-6">
            {platformHoldings.slice(0, 5).map((h, i) => (
              <div key={i} className="space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-black text-[#7a7060]">{h.name}</span>
                  <span className="font-bold text-[#9a9080]">{h.platformPercent}%</span>
                </div>
                <div className="h-2 w-full bg-[#e0dace] rounded-full overflow-hidden">
                  <div className="h-full bg-[#b85450] rounded-full" style={{ width: `${h.platformPercent}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Holdings Table */}
      <div className="bg-white rounded-2xl border border-[#e0dace] shadow-sm overflow-hidden">
        <div className="p-6 border-b border-[#e0dace]">
          <h3 className="text-lg font-black text-[#2a2820]">플랫폼 보유 토큰 상세 내역</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-[#f7f5f0] border-b border-[#e0dace]">
                {['자산 정보','보유 수량','보유 지분율','평가 금액','배당 수익 (예상)'].map(h => (
                  <th key={h} className={`px-6 py-4 text-[10px] font-black text-[#9a9080] uppercase tracking-wider ${h !== '자산 정보' ? 'text-right' : ''}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e0dace]">
              {platformHoldings.map(h => (
                <tr key={h.id} className="hover:bg-[#f7f5f0] transition-all">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#f7f5f0] border border-[#e0dace] flex items-center justify-center text-xs font-black text-[#9a9080]">
                        {h.symbol.slice(0, 2)}
                      </div>
                      <div>
                        <p className="text-sm font-black text-[#2a2820]">{h.name}</p>
                        <p className="text-[10px] font-mono font-bold text-[#9a9080]">{h.symbol}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-mono font-bold text-[#7a7060]">{h.platformTokens.toLocaleString()} ST</td>
                  <td className="px-6 py-4 text-right">
                    <span className="px-2 py-1 rounded-md bg-[#ffe65a]/30 text-[#c9a84c] text-[10px] font-black">{h.platformPercent}%</span>
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-black text-[#2a2820]">₩{h.value.toLocaleString()}</td>
                  <td className="px-6 py-4 text-right text-sm font-bold text-[#b85450]">
                    ₩{Math.round(h.value * (h.yield / 100) / 12).toLocaleString()}
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
