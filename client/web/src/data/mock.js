// ─── 현재 종목 ─────────────────────────────────────────────
export const MOCK_USER = { name: '홍길동', email: 'demo@sto.exchange' };

export const TOKENS = [
  { id: 'SEOULST',  name: '서울강남빌딩',   symbol: 'SEOULST',  category: 'real-estate', price: 12450, change: 2.30,  vol: 4821500000, cap: 124500000000, issued: 10000000, desc: '서울 강남구 삼성동 소재 프리미엄 오피스 빌딩. 총 32층, 연면적 4만2천㎡.', yield: 6.8, high: 12600, low: 12100, pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
  { id: 'SONGDORE', name: '송도 리조트',     symbol: 'SONGDORE', category: 'real-estate', price: 8320,  change: -1.15, vol: 2134200000, cap: 83200000000,  issued: 10000000, desc: '인천 송도국제도시 5성급 리조트. 연간 객실점유율 87% 유지.',            yield: 5.2, high: 8500, low: 8200,  pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
  { id: 'ARTPRIME', name: '아트프라임 펀드', symbol: 'ARTPRIME', category: 'art',         price: 45600, change: 0.88,  vol: 987600000,  cap: 456000000000, issued: 10000000, desc: '국내외 블루칩 현대미술 작품 컬렉션. 서울/뉴욕/런던 갤러리 전시.',      yield: 4.1, high: 46000, low: 45200, pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
  { id: 'JEJU1',    name: '제주 호텔 1호',   symbol: 'JEJU1',    category: 'real-estate', price: 6780,  change: 3.45,  vol: 1456000000, cap: 67800000000,  issued: 10000000, desc: '제주도 서귀포시 해안가 4성급 호텔. 세계자연유산 인접.',              yield: 7.2, high: 6900, low: 6500,  pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
  { id: 'LOGISHUB', name: '물류 허브',        symbol: 'LOGISHUB', category: 'infra',       price: 3210,  change: -0.62, vol: 3214000000, cap: 32100000000,  issued: 10000000, desc: '경기도 이천 스마트 물류센터. 국내 최대 이커머스 기업 전용 임차.',      yield: 6.1, high: 3280, low: 3150,  pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
  { id: 'SOLAR1',   name: '태양광 파워',      symbol: 'SOLAR1',   category: 'infra',       price: 2180,  change: 1.22,  vol: 876000000,  cap: 21800000000,  issued: 10000000, desc: '전남 신안군 해상 태양광 발전단지. 연간 발전량 180MW.',               yield: 8.5, high: 2210, low: 2150,  pdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf' },
];

// ─── 호가창 mock 데이터 ──────────────────────────────────────
// 원본: Math.random() 대신 고정값 사용
export const HOGA_ASKS = [
  // 가격 높은 순 → 낮은 순 (화면에 flex-col-reverse로 역순 렌더)
  { price: 12880, amount: 8450 },
  { price: 12780, amount: 22100 },
  { price: 12680, amount: 15300 },
  { price: 12580, amount: 31200 },
  { price: 12480, amount: 44700 },
  { price: 12380, amount: 19800 },
  { price: 12280, amount: 27400 },
  { price: 12180, amount: 38600 },
];

export const HOGA_BIDS = [
  // 가격 높은 순 → 낮은 순
  { price: 12080, amount: 52300 },
  { price: 11980, amount: 29600 },
  { price: 11880, amount: 41800 },
  { price: 11780, amount: 18200 },
  { price: 11680, amount: 63500 },
  { price: 11580, amount: 22900 },
  { price: 11480, amount: 35100 },
  { price: 11380, amount: 48700 },
];

// 체결강도 패널 (왼쪽 미니 리스트)
export const HOGA_EXECUTIONS = [
  { price: 186200, qty: 12, isBuy: true  },
  { price: 186200, qty: 34, isBuy: false },
  { price: 186200, qty: 8,  isBuy: true  },
  { price: 186200, qty: 21, isBuy: false },
  { price: 186200, qty: 45, isBuy: true  },
  { price: 186200, qty: 17, isBuy: false },
  { price: 186200, qty: 29, isBuy: true  },
  { price: 186200, qty: 6,  isBuy: false },
  { price: 186200, qty: 38, isBuy: true  },
  { price: 186200, qty: 14, isBuy: false },
  { price: 186200, qty: 42, isBuy: true  },
  { price: 186200, qty: 23, isBuy: false },
  { price: 186200, qty: 9,  isBuy: true  },
  { price: 186200, qty: 31, isBuy: false },
  { price: 186200, qty: 18, isBuy: true  },
];

// ─── 시세 테이블 ─────────────────────────────────────────────
export const PRICE_HISTORY_ROWS = [
  { price: 12450, qty: 23,  changeRate: -6.51, vol: 47583714, time: '16:52:22' },
  { price: 12480, qty: 45,  changeRate: -6.39, vol: 47583714, time: '16:52:19' },
  { price: 12420, qty: 12,  changeRate: -6.58, vol: 47583714, time: '16:52:15' },
  { price: 12500, qty: 78,  changeRate: -6.28, vol: 47583714, time: '16:52:11' },
  { price: 12450, qty: 31,  changeRate: -6.51, vol: 47583714, time: '16:52:07' },
  { price: 12460, qty: 19,  changeRate: -6.47, vol: 47583714, time: '16:52:03' },
  { price: 12440, qty: 55,  changeRate: -6.54, vol: 47583714, time: '16:51:59' },
  { price: 12420, qty: 8,   changeRate: -6.58, vol: 47583714, time: '16:51:55' },
  { price: 12450, qty: 42,  changeRate: -6.51, vol: 47583714, time: '16:51:51' },
  { price: 12480, qty: 27,  changeRate: -6.39, vol: 47583714, time: '16:51:47' },
];

// ─── 차트 데이터 (정적 OHLCV) ────────────────────────────────
// Math.random() 제거 → 고정 데이터로 교체
function buildChartData() {
  const times = [
    '09:00','09:10','09:20','09:30','09:40','09:50',
    '10:00','10:10','10:20','10:30','10:40','10:50',
    '11:00','11:10','11:20','11:30','11:40','11:50',
    '12:00','12:30','13:00','13:10','13:20','13:30',
    '13:40','13:50','14:00','14:10','14:20','14:30',
    '14:40','14:50','15:00','15:10','15:20','15:29',
  ];
  // 종가 변화값 (누적)
  const closes = [
    11860, 11900, 11870, 11950, 11990, 12050,
    12020, 12080, 12100, 12060, 12120, 12180,
    12150, 12200, 12170, 12250, 12210, 12270,
    12290, 12320, 12300, 12350, 12380, 12360,
    12400, 12420, 12390, 12440, 12460, 12430,
    12470, 12450, 12480, 12460, 12450, 12450,
  ];
  const vols = [
    45321, 38200, 52100, 41800, 29600, 63400,
    38900, 47200, 55100, 31800, 42600, 38100,
    29300, 51400, 44700, 36200, 48900, 40300,
    62100, 44800, 38500, 51200, 47600, 39400,
    55300, 42100, 37800, 49600, 44200, 53100,
    38700, 46300, 51800, 43200, 39600, 28400,
  ];

  return times.map((time, i) => {
    const close = closes[i];
    const prev = i > 0 ? closes[i - 1] : close - 30;
    const open = prev;
    const diff = Math.abs(close - open);
    const high = Math.max(open, close) + diff * 0.4 + 20;
    const low  = Math.min(open, close) - diff * 0.3 - 10;
    return { time, open: Math.round(open), high: Math.round(high), low: Math.round(low), close, vol: vols[i] };
  });
}

export const CHART_DATA = buildChartData();

// ─── 미체결 주문 ─────────────────────────────────────────────
export const PENDING_ORDERS = [
  { id: 'ORD-1001', side: 'buy',  asset: '서울강남빌딩',   assetId: 'SEOULST',  price: 12200, qty: 5,  amount: 61000,  type: '지정가', status: '대기', time: '09:32:14' },
  { id: 'ORD-1002', side: 'sell', asset: '송도 리조트',    assetId: 'SONGDORE', price: 8500,  qty: 10, amount: 85000,  type: '지정가', status: '대기', time: '10:05:47' },
  { id: 'ORD-1003', side: 'buy',  asset: '아트프라임 펀드', assetId: 'ARTPRIME', price: 45000, qty: 2,  amount: 90000,  type: '지정가', status: '대기', time: '11:18:22' },
  { id: 'ORD-1004', side: 'buy',  asset: '물류 허브',       assetId: 'LOGISHUB', price: 3150,  qty: 20, amount: 63000,  type: '지정가', status: '대기', time: '13:44:09' },
];

// ─── 배당금 내역 ─────────────────────────────────────────────
export const DIVIDEND_HISTORY = [
  { base: '2024.03.15', pay: '2024.03.20', per: '125원', total: '125,000원', status: '지급완료' },
  { base: '2023.12.15', pay: '2023.12.20', per: '120원', total: '120,000원', status: '지급완료' },
  { base: '2023.09.15', pay: '2023.09.20', per: '115원', total: '115,000원', status: '지급완료' },
  { base: '2023.06.15', pay: '2023.06.20', per: '110원', total: '110,000원', status: '지급완료' },
];

// ─── 공시 뉴스 (TradingPage 내 NewsTab용) ────────────────────
export const DISCLOSURES = [
  { title: '서울ST, 2026년 1분기 배당금 지급 안내',          date: '2026.03.20' },
  { title: '서울ST, 주요 임차인 계약 갱신 완료 공시',         date: '2026.03.15' },
  { title: '서울ST, 2025년 연간 운용 보고서 공시',            date: '2026.02.28' },
  { title: '서울ST, 배당 기준일 변경 안내',                   date: '2026.02.10' },
  { title: '서울ST, 외부평가기관 감정평가액 업데이트 공시',   date: '2026.01.25' },
];

// ─── 내 계좌 / MyAccountPage ────────────────────────────────
export const PORTFOLIO_ASSETS = [
  { symbol: 'SEOULST',  name: '서울강남빌딩', qty: 500,  avgPrice: 11200, currentPrice: 12450, category: '부동산' },
  { symbol: 'SONGDORE', name: '송도 리조트',  qty: 300,  avgPrice: 9100,  currentPrice: 8320,  category: '부동산' },
  { symbol: 'ARTPRIME', name: '아트프라임',   qty: 50,   avgPrice: 42000, currentPrice: 45600, category: '예술품' },
  { symbol: 'SOLAR1',   name: '태양광 파워',  qty: 2000, avgPrice: 2050,  currentPrice: 2180,  category: '인프라' },
];

export const ACCOUNT_DIVIDENDS = [
  { id: 1, date: '2026-03-15', symbol: 'SEOULST',  name: '서울강남빌딩', qty: 500,  perToken: 70.83, gross: 35415, tax: 5454, net: 29961 },
  { id: 2, date: '2026-03-10', symbol: 'SOLAR1',   name: '태양광 파워',  qty: 2000, perToken: 14.58, gross: 29160, tax: 4491, net: 24669 },
  { id: 3, date: '2026-02-15', symbol: 'SEOULST',  name: '서울강남빌딩', qty: 500,  perToken: 68.50, gross: 34250, tax: 5274, net: 28976 },
  { id: 4, date: '2026-02-10', symbol: 'SONGDORE', name: '송도 리조트',  qty: 300,  perToken: 42.10, gross: 12630, tax: 1945, net: 10685 },
];

export const PROFIT_ANALYSIS_DATA = [
  { date: '2026-03-01', profit:  12000, type: 'dividend', name: '서울강남빌딩' },
  { date: '2026-03-05', profit:  45000, type: 'sell',     name: '송도 리조트' },
  { date: '2026-03-10', profit:   8500, type: 'dividend', name: '태양광 파워' },
  { date: '2026-03-15', profit:   1200, type: 'interest', name: '계좌 이자' },
  { date: '2026-03-20', profit:  -5000, type: 'sell',     name: '아트프라임' },
];

export const OPEN_ORDERS = [
  { id: 'ORD001', time: '14:28:33', type: '지정가 매수', price: 12400, qty: 200, remaining: 200, symbol: 'SEOULST',  name: '서울강남빌딩' },
  { id: 'ORD002', time: '10:15:20', type: '지정가 매도', price: 8500,  qty: 100, remaining: 100, symbol: 'SONGDORE', name: '송도 리조트' },
  { id: 'ORD003', time: '09:45:11', type: '지정가 매수', price: 45000, qty: 50,  remaining: 50,  symbol: 'ARTPRIME', name: '아트프라임 펀드' },
];

export const FILLED_ORDERS = [
  { id: 'FILL001', time: '14:32:11', type: '시장가 매수', price: 12450, qty: 100, amount: 1245000, fee: 623, symbol: 'SEOULST',  name: '서울강남빌딩' },
  { id: 'FILL002', time: '11:20:45', type: '지정가 매도', price: 8320,  qty: 50,  amount: 416000,  fee: 208, symbol: 'SONGDORE', name: '송도 리조트' },
  { id: 'FILL003', time: '09:10:05', type: '지정가 매수', price: 6700,  qty: 200, amount: 1340000, fee: 670, symbol: 'JEJU1',    name: '제주 호텔 1호' },
];

// ─── WatchlistPage 미니 차트 (정적, Math.random 제거) ────────
export const MINI_CHART_DATA = {
  SEOULST:  [{ v: 12200 }, { v: 12150 }, { v: 12300 }, { v: 12250 }, { v: 12400 }, { v: 12350 }, { v: 12450 }, { v: 12380 }, { v: 12500 }, { v: 12450 }],
  SONGDORE: [{ v: 8600  }, { v: 8500  }, { v: 8450  }, { v: 8520  }, { v: 8480  }, { v: 8400  }, { v: 8350  }, { v: 8380  }, { v: 8320  }, { v: 8320  }],
  ARTPRIME: [{ v: 45000 }, { v: 45100 }, { v: 45200 }, { v: 45300 }, { v: 45400 }, { v: 45500 }, { v: 45450 }, { v: 45550 }, { v: 45600 }, { v: 45600 }],
  JEJU1:    [{ v: 6500  }, { v: 6550  }, { v: 6600  }, { v: 6650  }, { v: 6700  }, { v: 6720  }, { v: 6750  }, { v: 6760  }, { v: 6780  }, { v: 6780  }],
  LOGISHUB: [{ v: 3300  }, { v: 3280  }, { v: 3260  }, { v: 3240  }, { v: 3230  }, { v: 3220  }, { v: 3210  }, { v: 3215  }, { v: 3210  }, { v: 3210  }],
  SOLAR1:   [{ v: 2140  }, { v: 2150  }, { v: 2155  }, { v: 2160  }, { v: 2165  }, { v: 2170  }, { v: 2175  }, { v: 2180  }, { v: 2178  }, { v: 2180  }],
};

// ─── 관리자 공시 (ContentManagement용 풍부한 스키마) ──────────
export const ADMIN_DISCLOSURES = [
  { id: 1, asset: '서울강남빌딩',   assetId: 'SEOULST',  type: '배당', title: '서울강남빌딩 2026년 3월 배당금 지급 안내',        date: '2026-03-15', file: '', status: '승인완료' },
  { id: 2, asset: '송도 리조트',    assetId: 'SONGDORE', type: '배당', title: '송도 리조트 2026년 3월 배당금 지급 안내',          date: '2026-03-15', file: '', status: '승인완료' },
  { id: 3, asset: '서울강남빌딩',   assetId: 'SEOULST',  type: '일반', title: '서울강남빌딩 주요 임차인 계약 갱신 완료',           date: '2026-03-10', file: '', status: '승인완료' },
  { id: 4, asset: '아트프라임 펀드', assetId: 'ARTPRIME', type: '일반', title: '아트프라임 펀드 2025년 연간 운용 보고서',           date: '2026-02-28', file: '', status: '승인완료' },
  { id: 5, asset: '제주 호텔 1호',  assetId: 'JEJU1',    type: '일반', title: '제주 호텔 1호 외부평가기관 감정평가 업데이트',       date: '2026-02-20', file: '', status: '검토대기' },
  { id: 6, asset: '태양광 파워',    assetId: 'SOLAR1',   type: '배당', title: '태양광 파워 2026년 2월 배당금 지급 안내',           date: '2026-02-15', file: '', status: '승인완료' },
];

// ─── 관리자 공지사항 (ContentManagement용) ────────────────────
export const ADMIN_NOTICES = [
  { id: 1, title: '2026년 2분기 배당 일정 안내',            date: '2026.03.20', category: '일반',   important: true,  content: '2026년 2분기 배당 지급 일정을 안내드립니다. 배당 기준일은 매월 10일, 지급일은 매월 20일입니다.' },
  { id: 2, title: '시스템 점검 안내 (3월 28일 00:00~06:00)', date: '2026.03.18', category: '시스템', important: true,  content: '서비스 안정화를 위한 정기 시스템 점검을 진행합니다. 해당 시간 동안 거래가 일시 중단됩니다.' },
  { id: 3, title: 'STONE 플랫폼 이용약관 개정 안내',         date: '2026.03.10', category: '일반',   important: false, content: '2026년 4월 1일부로 서비스 이용약관 일부가 개정됩니다. 자세한 내용은 공지를 확인하세요.' },
  { id: 4, title: '신규 자산 상장 안내: 태양광 파워(SOLAR1)', date: '2026.02.28', category: '일반',   important: false, content: '새로운 STO 자산 태양광 파워(SOLAR1)가 상장됩니다. 연간 예상 수익률 8.5%, 매월 배당 지급 예정입니다.' },
];

// ─── DashboardPage 추세 차트 (정적, Math.random 제거) ────────
export const TREND_DATA = {
  '실시간': [{ name: 'P0', val: 208 }, { name: 'P1', val: 212 }, { name: 'P2', val: 210 }, { name: 'P3', val: 215 }, { name: 'P4', val: 213 }, { name: 'P5', val: 217 }, { name: 'P6', val: 218 }],
  '1일':    [{ name: 'P0', val: 234 }, { name: 'P1', val: 241 }, { name: 'P2', val: 238 }, { name: 'P3', val: 252 }, { name: 'P4', val: 250 }, { name: 'P5', val: 257 }, { name: 'P6', val: 262 }],
  '1주일':  [{ name: 'P0', val: 166 }, { name: 'P1', val: 161 }, { name: 'P2', val: 158 }, { name: 'P3', val: 168 }, { name: 'P4', val: 166 }, { name: 'P5', val: 171 }, { name: 'P6', val: 174 }],
  '1개월':  [{ name: 'P0', val: 293 }, { name: 'P1', val: 302 }, { name: 'P2', val: 297 }, { name: 'P3', val: 315 }, { name: 'P4', val: 312 }, { name: 'P5', val: 321 }, { name: 'P6', val: 327 }],
  '3개월':  [{ name: 'P0', val: 195 }, { name: 'P1', val: 201 }, { name: 'P2', val: 198 }, { name: 'P3', val: 210 }, { name: 'P4', val: 208 }, { name: 'P5', val: 214 }, { name: 'P6', val: 218 }],
  '6개월':  [{ name: 'P0', val: 180 }, { name: 'P1', val: 188 }, { name: 'P2', val: 185 }, { name: 'P3', val: 195 }, { name: 'P4', val: 192 }, { name: 'P5', val: 200 }, { name: 'P6', val: 204 }],
  '1년':    [{ name: 'P0', val: 160 }, { name: 'P1', val: 170 }, { name: 'P2', val: 167 }, { name: 'P3', val: 180 }, { name: 'P4', val: 177 }, { name: 'P5', val: 188 }, { name: 'P6', val: 195 }],
};
