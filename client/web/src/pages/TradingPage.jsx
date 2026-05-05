import { useState } from 'react';
import { Download, FileText } from 'lucide-react';
import { TOKENS, DIVIDEND_HISTORY, DISCLOSURES } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { useTradingSocket } from '../hooks/useTradingSocket.js';

// mock asset.id(string) → 백엔드 tokenId(number) 매핑
const TOKEN_ID_MAP = {
  SEOULST:  1,
  SONGDORE: 2,
  ARTPRIME: 3,
  JEJU1:    4,
  LOGISHUB: 5,
  SOLAR1:   6,
};
import { AssetHeader } from '../components/trading/AssetHeader.jsx';
import { ChartPanel }  from '../components/trading/ChartPanel.jsx';
import { HogaPanel }   from '../components/trading/HogaPanel.jsx';
import { SecureOrderPanel }  from '../components/trading/SecureOrderPanel.jsx';

// TradingPage — 원본 TradingPage.tsx 구조 복원
//
// 레이아웃 (원본 그대로):
//   - 전체: flex flex-col h-[calc(100vh-64px)] -m-8 bg-stone-100
//   - 상단: AssetHeader (종목 정보 + 탭)
//   - 콘텐츠: flex-1 flex overflow-hidden p-6 gap-6
//       차트 탭: [좌 flex-[2]] [중 w-400] [우 w-360]
//       기타 탭: [콘텐츠 flex-1] [우 w-360]
//
// state: activeTab, currentAssetId, watchlist

export function TradingPage() {
  const [activeTab, setActiveTab]           = useState('chart');
  const [currentAssetId, setCurrentAssetId] = useState('SEOULST');
  const [orderBook, setOrderBook]           = useState({ asks: [], bids: [] });
  const [lastTrade, setLastTrade]           = useState(null);
  const { user, likedTokenIds, toggleLike } = useApp();

  const asset        = TOKENS.find(t => t.id === currentAssetId) || TOKENS[0];
  const currentPrice = asset.price;   // 실서비스: 실시간 가격 상태로 교체
  const tokenId      = TOKEN_ID_MAP[asset.id] ?? null;
  const isLiked      = tokenId != null && likedTokenIds.includes(tokenId);
  const token        = user?.accessToken ?? null;

  useTradingSocket({
    tokenId,
    token,
    onOrderBook: (data) => setOrderBook({ asks: data.asks ?? [], bids: data.bids ?? [] }),
    onTrades: (data) => setLastTrade({ price: data.tradePrice, isBuy: data.isBuy, key: Date.now() }),
  });

  return (
    // 원본: h-[calc(100vh-64px)] -m-8 (MainLayout p-8을 상쇄)
    // 여기서는 AppHeader h-16(64px)을 직접 차감
    <div className="flex flex-col bg-stone-100 text-stone-800 overflow-hidden"
         style={{ height: 'calc(100vh - 64px)' }}>

      {/* 상단: 종목 정보 헤더 */}
      <AssetHeader
        asset={asset}
        currentPrice={currentPrice}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        isLiked={isLiked}
        onToggleLike={() => tokenId != null && toggleLike(tokenId)}
      />

      {/* 메인 콘텐츠 영역 */}
      <div className="flex-1 flex overflow-hidden p-6 gap-6">

        {activeTab === 'chart' ? (
          <>
            {/* 차트·호가 탭: 좌(chart) + 중(hoga) + 우(order) */}
            <ChartPanel currentPrice={currentPrice} />
            <HogaPanel  currentPrice={currentPrice} asks={orderBook.asks} bids={orderBook.bids} lastTrade={lastTrade} />
          </>
        ) : (
          /* 기타 탭: 콘텐츠 패널 */
          <div className="flex-1 bg-[#ffffff] rounded-lg border border-stone-200 p-8 overflow-y-auto">
            {activeTab === 'info'     && <InfoTab     asset={asset} />}
            {activeTab === 'dividend' && <DividendTab />}
            {activeTab === 'news'     && <NewsTab />}
          </div>
        )}

        {/* 주문창: 항상 오른쪽에 고정 */}
        <SecureOrderPanel currentPrice={currentPrice} tokenId={tokenId} token={token} />
      </div>
    </div>
  );
}

// ── 종목정보 탭 ─────────────────────────────────────────────────
function InfoTab({ asset }) {
  return (
    <div className="space-y-8 max-w-4xl">
      <section>
        <h3 className="text-lg font-bold mb-4 text-stone-800">종목 상세 정보</h3>
        <div className="grid grid-cols-2 gap-4">
          {[
            { label: '총 토큰',  value: asset.issued.toLocaleString() },
            { label: '가격',     value: `${asset.price.toLocaleString()} 원` },
            { label: '상장일',   value: '2024-01-15' },
            { label: '자산 이름', value: asset.name },
          ].map((item, i) => (
            <div key={i} className="flex justify-between p-4 bg-stone-100 rounded-xl border border-stone-200">
              <span className="text-stone-400 font-bold">{item.label}</span>
              <span className="text-stone-800 font-bold">{item.value}</span>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h3 className="text-lg font-bold mb-4 text-stone-800">사업 개요</h3>
        <p className="text-stone-500 text-sm leading-relaxed">{asset.desc}</p>
      </section>

      {asset.pdfUrl && (
        <section>
          <h3 className="text-lg font-bold mb-4 text-stone-800">투자 설명서</h3>
          <div className="p-6 bg-stone-100 rounded-lg border border-stone-200 flex items-center justify-between group hover:border-brand-blue transition-all">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-brand-red-light flex items-center justify-center text-brand-red">
                <FileText size={24} />
              </div>
              <div>
                <p className="font-bold text-stone-800">{asset.name} 투자설명서.pdf</p>
                <p className="text-xs text-stone-400 font-bold uppercase tracking-widest">PDF Document · 2.4 MB</p>
              </div>
            </div>
            <a
              href={asset.pdfUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 px-6 py-3 bg-[#ffffff] border border-stone-200 rounded-xl text-sm font-black text-stone-500 hover:bg-brand-blue hover:text-white hover:border-brand-blue transition-all shadow-sm"
            >
              <Download size={18} /> 다운로드
            </a>
          </div>
        </section>
      )}
    </div>
  );
}

// ── 배당금 내역 탭 ──────────────────────────────────────────────
function DividendTab() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-stone-800">배당금 내역</h3>
        <div className="flex items-center gap-2 text-sm text-stone-400">
          <span>총 누적 배당금:</span>
          <span className="text-stone-800 font-black">1,250,000원</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { label: '최근 배당금',    value: '125,000원', date: '2024.03.15' },
          { label: '배당 수익률',    value: '4.2%',      date: '연환산 기준' },
          { label: '다음 배당 예정일', value: '2024.06.15', date: '분기 배당' },
        ].map((item, i) => (
          <div key={i} className="p-6 bg-stone-100 rounded-lg border border-stone-200">
            <p className="text-xs font-bold text-stone-400 mb-1">{item.label}</p>
            <p className="text-xl font-black text-stone-800">{item.value}</p>
            <p className="text-[10px] text-stone-400 mt-2 font-bold">{item.date}</p>
          </div>
        ))}
      </div>

      <table className="w-full text-sm mt-8">
        <thead className="text-stone-400 border-b border-stone-200">
          <tr>
            <th className="text-left py-4 font-bold">배당기준일</th>
            <th className="text-left py-4 font-bold">지급일</th>
            <th className="text-right py-4 font-bold">주당 배당금</th>
            <th className="text-right py-4 font-bold">총 배당금</th>
            <th className="text-right py-4 font-bold">상태</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-stone-200">
          {DIVIDEND_HISTORY.map((item, i) => (
            <tr key={i} className="hover:bg-stone-100 transition-colors">
              <td className="py-4 text-stone-500 font-mono">{item.base}</td>
              <td className="py-4 text-stone-500 font-mono">{item.pay}</td>
              <td className="py-4 text-right font-mono font-bold text-stone-800">{item.per}</td>
              <td className="py-4 text-right font-mono font-bold text-stone-800">{item.total}</td>
              <td className="py-4 text-right">
                <span className="px-2 py-1 bg-brand-red/10 text-brand-red rounded text-[10px] font-bold">
                  {item.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ── 공시 탭 ─────────────────────────────────────────────────────
function NewsTab() {
  return (
    <div className="space-y-4">
      {DISCLOSURES.map((item, i) => (
        <div key={i} className="p-6 bg-[#ffffff] rounded-lg hover:bg-stone-100 transition-all cursor-pointer border border-stone-200 group">
          <div className="flex justify-between items-start mb-2">
            <h4 className="font-bold text-stone-800 group-hover:text-brand-blue transition-colors">
              {item.title}
            </h4>
            <span className="text-[11px] text-stone-400 font-bold shrink-0 ml-4">{item.date}</span>
          </div>
          <p className="text-sm text-stone-500">
            당사는 관련 공시 내용을 상기와 같이 안내드립니다. 자세한 내용은 첨부 파일을 확인하시기 바랍니다.
          </p>
        </div>
      ))}
    </div>
  );
}
