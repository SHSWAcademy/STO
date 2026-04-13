import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, ArrowUp, ArrowDown, LayoutGrid, List, ChevronRight } from 'lucide-react';
import { MINI_CHART_DATA } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { cn } from '../lib/utils.js';
import { SearchInput } from '../components/ui/SearchInput.jsx';
import { AssetAvatar } from '../components/ui/AssetAvatar.jsx';
import { MiniChart } from '../components/ui/MiniChart.jsx';
import { API_BASE_URL } from '../lib/config.js';

function formatPrice(value) {
  return `${Number(value ?? 0).toLocaleString()} KRW`;
}

function getMiniChart(symbol) {
  return MINI_CHART_DATA[symbol] || [];
}

export function WatchlistPage() {
  const navigate = useNavigate();
  const { user, likedTokenIds, toggleLike } = useApp();
  const [viewMode, setViewMode] = useState('list');
  const [searchQuery, setSearchQuery] = useState('');
  const [likedItems, setLikedItems] = useState([]);
  const [loading, setLoading] = useState(false);

  async function fetchLikedItems() {
    if (!user?.accessToken) {
      setLikedItems([]);
      return;
    }

    setLoading(true);

    try {
      const res = await fetch(`${API_BASE_URL}/api/likes`, {
        headers: {
          Authorization: `Bearer ${user.accessToken}`,
        },
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      setLikedItems(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('[WatchlistPage] likes load failed:', err);
      setLikedItems([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchLikedItems();
  }, [user?.accessToken, likedTokenIds]);

  async function handleToggle(tokenId, e) {
    e.stopPropagation();

    try {
      await toggleLike(tokenId);
    } catch (err) {
      console.error('[WatchlistPage] like toggle failed:', err);
    }
  }

  const filteredItems = likedItems.filter((item) => {
    const assetName = String(item.assetName ?? '').toLowerCase();
    const tokenSymbol = String(item.tokenSymbol ?? '').toLowerCase();
    const keyword = searchQuery.toLowerCase();

    return assetName.includes(keyword) || tokenSymbol.includes(keyword);
  });

  return (
    <div className="space-y-8 pb-20">
      <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="mb-2 text-3xl font-black tracking-tight text-stone-800 uppercase">
            관심 종목
          </h2>
          <p className="text-sm font-bold text-stone-500">
            내가 찜한 STO의 실시간 시세를 한눈에 확인하세요.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <SearchInput
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="종목명 검색..."
          />
          <div className="flex rounded-xl border border-stone-200 bg-stone-100 p-1">
            <button
              onClick={() => setViewMode('list')}
              className={cn(
                'rounded-lg p-2 transition-all',
                viewMode === 'list' ? 'bg-white text-stone-800 shadow-sm' : 'text-stone-400',
              )}
            >
              <List size={18} />
            </button>
            <button
              onClick={() => setViewMode('grid')}
              className={cn(
                'rounded-lg p-2 transition-all',
                viewMode === 'grid' ? 'bg-white text-stone-800 shadow-sm' : 'text-stone-400',
              )}
            >
              <LayoutGrid size={18} />
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-32 text-center">
          <p className="text-sm font-bold text-stone-400">Loading liked assets...</p>
        </div>
      ) : viewMode === 'list' ? (
        <div className="overflow-hidden rounded-[32px] border border-stone-200 bg-white shadow-sm">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-stone-200 text-[11px] font-black uppercase tracking-widest text-stone-400">
                <th className="w-12 px-8 py-4"></th>
                <th className="px-4 py-4">Asset</th>
                <th className="px-4 py-4 text-right">Price</th>
                <th className="w-32 px-4 py-4">Trend</th>
                <th className="w-12 px-8 py-4"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {filteredItems.map((item) => {
                const tokenId = item.tokenId;
                const isLiked = likedTokenIds.includes(tokenId);

                return (
                  <tr
                    key={tokenId}
                    className="group cursor-pointer transition-colors hover:bg-stone-50"
                    onClick={() => navigate(`/token/${tokenId}`)}
                  >
                    <td className="px-8 py-6">
                      <div
                        className="flex h-12 w-12 items-center justify-center"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          onClick={(e) => handleToggle(tokenId, e)}
                          className={cn(
                            'flex h-11 w-11 items-center justify-center rounded-full transition-colors',
                            isLiked
                              ? 'bg-brand-red-light/70 text-brand-red'
                              : 'text-stone-300 hover:bg-stone-100 hover:text-brand-red',
                          )}
                        >
                          <Heart size={20} fill={isLiked ? 'currentColor' : 'none'} />
                        </button>
                      </div>
                    </td>
                    <td className="px-4 py-6">
                      <div className="flex items-center gap-4">
                        <AssetAvatar symbol={item.tokenSymbol ?? '?'} size="md" />
                        <div>
                          <p className="text-sm font-black text-stone-800 transition-colors group-hover:text-stone-600">
                            {item.assetName ?? '-'}
                          </p>
                          <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">
                            {item.tokenSymbol ?? '-'}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-6 text-right">
                      <p className="font-mono text-sm font-black text-stone-800">
                        {formatPrice(item.currentPrice)}
                      </p>
                    </td>
                    <td className="px-4 py-6">
                      <MiniChart
                        data={getMiniChart(item.tokenSymbol)}
                        isUp
                        className="h-8 w-24"
                      />
                    </td>
                    <td className="px-8 py-6 text-right">
                      <ChevronRight
                        size={18}
                        className="text-stone-300 transition-colors group-hover:text-stone-600"
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredItems.map((item) => {
            const tokenId = item.tokenId;
            const isLiked = likedTokenIds.includes(tokenId);

            return (
              <div
                key={tokenId}
                onClick={() => navigate(`/token/${tokenId}`)}
                className="group relative cursor-pointer overflow-hidden rounded-[32px] border border-stone-200 bg-white p-6 transition-all hover:shadow-xl"
              >
                <div
                  className="absolute right-4 top-4 z-10 flex h-12 w-12 items-center justify-center"
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    onClick={(e) => handleToggle(tokenId, e)}
                    className={cn(
                      'flex h-11 w-11 items-center justify-center rounded-full transition-colors',
                      isLiked
                        ? 'bg-brand-red-light/70 text-brand-red'
                        : 'text-stone-300 hover:bg-stone-100 hover:text-brand-red',
                    )}
                  >
                    <Heart size={20} fill={isLiked ? 'currentColor' : 'none'} />
                  </button>
                </div>

                <div className="mb-6 flex items-center gap-4">
                  <AssetAvatar symbol={item.tokenSymbol ?? '?'} size="lg" />
                  <div>
                    <h3 className="text-sm font-black text-stone-800 transition-colors group-hover:text-stone-600">
                      {item.assetName ?? '-'}
                    </h3>
                    <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">
                      {item.tokenSymbol ?? '-'}
                    </p>
                  </div>
                </div>

                <div className="mb-6 space-y-1">
                  <p className="font-mono text-2xl font-black tracking-tighter text-stone-800">
                    {formatPrice(item.currentPrice)}
                  </p>
                </div>

                <MiniChart
                  data={getMiniChart(item.tokenSymbol)}
                  isUp
                  className="mb-4 h-16 w-full"
                />

                <div className="flex items-center justify-between border-t border-stone-100 pt-4">
                  <span className="text-[10px] font-black uppercase tracking-widest text-stone-400">
                    {item.tokenSymbol ?? '-'}
                  </span>
                  <ChevronRight size={16} className="text-stone-300" />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {!loading && filteredItems.length === 0 && (
        <div className="flex flex-col items-center justify-center py-32 space-y-6 text-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-full border border-stone-200 bg-stone-100">
            <Heart size={40} className="text-stone-300" />
          </div>
          <div>
            <h3 className="text-xl font-black text-stone-800">No liked assets yet</h3>
            <p className="mt-2 font-bold text-stone-400">
              Tap the heart button on an asset to save it here.
            </p>
          </div>
          <button
            onClick={() => navigate('/')}
            className="rounded-2xl bg-stone-800 px-8 py-3 text-sm font-black text-white shadow-lg transition-all hover:bg-stone-700"
          >
            Browse Assets
          </button>
        </div>
      )}
    </div>
  );
}
