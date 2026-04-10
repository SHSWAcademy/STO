CREATE TABLE "MEMBERS" (
                           "회원 ID"	BIGINT		NOT NULL,
                           "이메일"	VARCHAR(255)		NOT NULL,
                           "비밀번호"	VARCHAR(255)		NOT NULL,
                           "회원 이름"	VARCHAR		NOT NULL,
                           "활성화 여부"	BOOLEAN	DEFAULT TRUE	NOT NULL,
                           "가입일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                           "변경일자"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "CANDLE_HOURS" (
                                "시계열 데이터 ID"	BIGINT		NOT NULL,
                                "토큰 ID"	BIGINT		NOT NULL,
                                "시가"	DECIMAL		NULL,
                                "고가"	DECIMAL		NULL,
                                "저가"	DECIMAL		NULL,
                                "종가"	DECIMAL		NULL,
                                "해당 구간 체결량 합계"	DECIMAL		NULL,
                                "1시간 단위 타임 스탬프"	DATETIME		NOT NULL,
                                "캔들 시간 구간 동안 실제로 거래가 체결된 횟수"	INT	DEFAULT 0	NOT NULL
);

CREATE TABLE "ORDERS" (
                          "거래 요청 ID"	BIGINT		NOT NULL,
                          "회원 ID"	BIGINT		NOT NULL,
                          "토큰 ID"	BIGINT		NOT NULL,
                          "지정가 주문 가격 (호가)"	BIGINT		NOT NULL,
                          "처음 요청한 매도/매수 수량"	BIGINT	DEFAULT 0	NOT NULL,
                          "체결된 수량"	BIGINT	DEFAULT 0	NULL,
                          "미체결 수량"	BIGINT	DEFAULT 0	NOT NULL,
                          "호가 시간 (주문 접수 시간)"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "호가 변동 시간 (회원의 호가 정정)"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "매도/매수 여부"	ENUM('BUY', 'SELL')		NOT NULL,
                          "호가 상태 (접수,대기,부분체결,전체체결,취소,실패)"	ENUM('OPEN', 'PENDING', 'PARTIAL', 'FILLED', 'CANCELLED', 'FAILED')		NOT NULL,
                          "주문 순서"	BIGINT		NOT NULL
);

CREATE TABLE "TRADES" (
                          "거래 완료 ID"	BIGINT		NOT NULL,
                          "매도자 ID"	BIGINT		NOT NULL,
                          "매수자 ID"	BIGINT		NOT NULL,
                          "매도 요청 ID"	BIGINT		NOT NULL,
                          "매수 요청 ID"	BIGINT		NOT NULL,
                          "토큰 ID"	BIGINT		NOT NULL,
                          "실제 체결 가격 per token"	BIGINT		NOT NULL,
                          "실제 체결 수량"	BIGINT		NOT NULL,
                          "총 체결 금액 (total)"	BIGINT		NOT NULL,
                          "거래 수수료 (계산된 수수료)"	BIGINT	DEFAULT 0	NOT NULL,
                          "정산 상태 (온체인 대기, 성공, 실패)"	ENUM('ON_CHAIN_PENDING', 'SUCCESS', 'FAILED')		NOT NULL,
                          "실제 매칭 엔진이 체결시킨 시간"	DATETIME		NOT NULL,
                          "레코드 생성 시간"	DATETIME		NOT NULL
);

CREATE TABLE "BLOCKCHAIN_OUTBOX_Q" (
                                       "큐 ID"	BIGINT		NOT NULL,
                                       "거래 완료 ID"	BIGINT		NOT NULL,
                                       "지분 ID"	BIGINT		NOT NULL,
                                       "블록체인에 전달할 실제 데이터 묶음"	JSON		NOT NULL,
                                       "큐 처리 상태 (PENDING, PROCESSING, SUBMITTED,CONFIRMED,FAILED,ABANDONED))"	ENUM		NOT NULL,
                                       "재시도 횟수"	INT	DEFAULT 3	NOT NULL,
                                       "마지막 실패 사유"	TEXT		NULL,
                                       "큐 적재 시간"	DATETIME		NOT NULL,
                                       "마지막 상태 변경 시간"	DATETIME		NOT NULL,
                                       "큐가 중복 & 애매한 실패 다시보내는 요청 식별용"	VARCHAR(100)		NOT NULL,
                                       "재시도"	INT	DEFAULT 0	NOT NULL
);

CREATE TABLE "PLATFORM_TOKEN_HOLDINGS" (
                                           "지분 ID"	BIGINT		NOT NULL,
                                           "관리자 ID"	BIGINT		NOT NULL,
                                           "토큰 ID"	BIGINT		NOT NULL,
                                           "현재 보유한 해당 자산의 20% 토큰 개수"	BIGINT	DEFAULT 0	NOT NULL,
                                           "토큰 초기 가격"	BIGINT	DEFAULT 0	NOT NULL,
                                           "20% STO 지분이 플랫폼에 들어온 날짜"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "ALARMS" (
                          "알람 ID"	BIGINT		NOT NULL,
                          "회원 ID"	BIGINT		NOT NULL,
                          "알림 제목"	VARCHAR(255)		NOT NULL,
                          "알림 내용"	VARCHAR(500)		NOT NULL,
                          "알림 발송 시간"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "읽음 여부"	BOOLEAN	DEFAULT FALSE	NOT NULL
);

CREATE TABLE "CANDLE_YEARS" (
                                "시계열 데이터 ID"	BIGINT		NOT NULL,
                                "토큰 ID"	BIGINT		NOT NULL,
                                "시가"	DECIMAL		NULL,
                                "고가"	DECIMAL		NULL,
                                "저가"	DECIMAL		NULL,
                                "종가"	DECIMAL		NULL,
                                "해당 구간 체결량 합계"	DECIMAL		NULL,
                                "1년 단위 타임 스탬프"	DATETIME		NOT NULL,
                                "캔들 시간 구간 동안 실제로 거래가 체결된 횟수"	INT	DEFAULT 0	NOT NULL
);

CREATE TABLE "COMMONS" (
                           "기본 ID"	BIGINT		NOT NULL,
                           "세율"	DECIMAL	DEFAULT 15.4	NOT NULL,
                           "수수료율"	DECIMAL	DEFAULT 0.1	NOT NULL,
                           "배당 전체 지급일 (매달 20일)"	TINYINT		NOT NULL,
                           "배당 기준일 (관리자 입력)"	TINYINT		NOT NULL
);

CREATE TABLE "ASSET_BANKINGS" (
                                  "부동산 계좌 입출금 내역 id"	BIGINT		NOT NULL,
                                  "부동산 계좌 id"	BIGINT		NOT NULL,
                                  "계좌 입출금 금액"	BIGINT		NOT NULL,
                                  "입급 / 출금"	ENUM('DEPOSIT', 'WITHDRAWAL')		NOT NULL,
                                  "입출금 유형"	ENUM('ALLOCATION', 'EXTRA')		NOT NULL,
                                  "계좌 입출금 시각"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "API_LOG" (
                           "API 로그 ID"	BIGINT		NOT NULL,
                           "요청 추적용 ID"	VARCHAR		NOT NULL,
                           "호출 엔드포인트"	VARCHAR		NOT NULL,
                           "HTTP 메서드"	VARCHAR		NOT NULL,
                           "응답 상태 코드"	INT		NOT NULL,
                           "응답 시간"	INT		NOT NULL,
                           "요청 발생 시각"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "PLATFORM_BANKING" (
                                    "플랫폼 입출금 내역 ID"	BIGINT		NOT NULL,
                                    "토큰 ID"	BIGINT		NULL,
                                    "거래 완료 ID"	BIGINT		NULL,
                                    "수익유형 (수수료, 배당)"	ENUM ('FEE', 'DIVIDEND')		NOT NULL,
                                    "입출금 금액"	BIGINT		NOT NULL,
                                    "입급 / 출금"	ENUM('DEPOSIT', 'WITHDRAWAL')		NOT NULL,
                                    "입금일자"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "ALLOCATION_EVENTS" (
                                     "배당 관리 ID"	BIGINT		NOT NULL,
                                     "부동산 ID"	BIGINT		NOT NULL,
                                     "공시 ID"	BIGINT		NOT NULL,
                                     "관리자의 배당 등록 일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                                     "배당 등록 수정 일자"	DATETIME		NULL,
                                     "배치 여부(성공,실패)"	BOOLEAN	DEFAULT FALSE	NOT NULL,
                                     "배당 월수익 (관리자 입력)"	BIGINT		NULL,
                                     "배당 지급일"	DATETIME		NULL,
                                     "배당 지급년도"	INT		NULL,
                                     "배당 지급월"	int		NULL
);

CREATE TABLE "ASSETS" (
                          "부동산 ID"	BIGINT		NOT NULL,
                          "건물 총 가격(가치)"	BIGINT		NOT NULL,
                          "초기 토큰 가격"	BIGINT		NOT NULL,
                          "주소"	VARCHAR(255)		NOT NULL,
                          "건물 사진 url"	VARCHAR(255)		NULL,
                          "업로드 일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "수정 일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "토큰 발행 총 개수"	BIGINT		NOT NULL,
                          "건물 이름"	VARCHAR(255)		NOT NULL,
                          "배당지급 여부"	BOOLEAN	DEFAULT FALSE	NOT NULL
);

CREATE TABLE "TOKEN_HOLDINGS" (
                                  "종목별 토큰 보유량 PK"	BIGINT		NOT NULL,
                                  "회원 ID"	BIGINT		NOT NULL,
                                  "토큰 ID"	BIGINT		NOT NULL,
                                  "지갑 ID"	BIGINT		NOT NULL,
                                  "변경일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                                  "현재 회원이 가지고 있는 토큰 보유량"	BIGINT	DEFAULT 0	NOT NULL,
                                  "매도 주문으로 묶인 수량"	BIGINT	DEFAULT 0	NOT NULL,
                                  "평균 매수가(수익률, 평가손익 계산)"	DECIMAL	DEFAULT 0	NOT NULL
);

CREATE TABLE "FILES" (
                         "파일 ID"	BIGINT		NOT NULL,
                         "공시 ID"	BIGINT		NOT NULL,
                         "업로드 일자"	DATETIME		NOT NULL,
                         "수정 일자"	DATETIME		NOT NULL,
                         "원본 파일명"	VARCHAR(255)		NOT NULL,
                         "저장본 파일명"	VARCHAR(255)		NOT NULL,
                         "사이즈"	BIGINT		NOT NULL,
                         "저장 경로"	VARCHAR(500)		NOT NULL
);

CREATE TABLE "CANDLE_DAYS" (
                               "시계열 데이터 ID"	BIGINT		NOT NULL,
                               "토큰 ID"	BIGINT		NOT NULL,
                               "시가"	DECIMAL		NULL,
                               "고가"	DECIMAL		NULL,
                               "저가"	DECIMAL		NULL,
                               "종가"	DECIMAL		NULL,
                               "해당 구간 체결량 합계"	DECIMAL		NULL,
                               "1일 단위 타임 스탬프"	DATETIME		NOT NULL,
                               "캔들 시간 구간 동안 실제로 거래가 체결된 횟수"	INT	DEFAULT 0	NOT NULL
);

CREATE TABLE "TRADE_LOGS" (
                              "거래 완료 로그 ID"	BIGINT		NOT NULL,
                              "거래 완료 ID"	BIGINT		NOT NULL,
                              "임시 이벤트 데이터"	JSON		NOT NULL,
                              "로그 생성 시간"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "ASSET_ACCOUNTS" (
                                  "부동산 계좌 id"	BIGINT		NOT NULL,
                                  "부동산 ID"	BIGINT		NOT NULL,
                                  "지금 출금 가능한 잔고"	BIGINT		NOT NULL,
                                  "각 부동산 별 계좌 생성 시간"	DATETIME	DEFAULT NOW()	NOT NULL,
                                  "잔고 변경 시간"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "ACCOUNTS" (
                            "계좌 ID"	BIGINT		NOT NULL,
                            "회원 ID"	BIGINT		NOT NULL,
                            "결제 비밀번호(bcrypt)"	VARCHAR(255)		NOT NULL,
                            "계좌 번호"	VARCHAR(255)		NOT NULL,
                            "사용 가능한 현금 잔고"	BIGINT	DEFAULT 0	NULL,
                            "주문으로 묶인 현금"	BIGINT	DEFAULT 0	NULL,
                            "계좌 생성 일자"	DATETIME	DEFAULT NOW()	NOT NULL,
                            "계좌 수정 일자"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "PLATFORM_ACCOUNTS" (
                                     "플랫폼 계좌 ID"	BIGINT		NOT NULL,
                                     "지금 출금 가능한 잔고"	BIGINT		NOT NULL,
                                     "수익 누계"	BIGINT		NOT NULL,
                                     "출금 누계"	BIGINT		NOT NULL,
                                     "마지막 업데이트 일"	DATETIME		NOT NULL
);

CREATE TABLE "TOKENS" (
                          "토큰 ID"	BIGINT		NOT NULL,
                          "부동산  ID"	BIGINT		NOT NULL,
                          "토큰 발행 총 개수 (100%)"	BIGINT		NOT NULL,
                          "토큰 발행 실제 개수 (80%)"	BIGINT		NOT NULL,
                          "토큰 이름"	VARCHAR(255)		NOT NULL,
                          "자산이름을 계속 json으로 보내주면 더러워질 수 있어서 짧게 줄여서 쓰는것"	VARCHAR(255)		NOT NULL,
                          "온체인 토큰 ID or 컨트랙트 내 토큰 ID(블록체인을 위한 pk)"	VARCHAR(255)		NULL,
                          "ERC20 토큰 메타 데이터"	VARCHAR(255)		NULL,
                          "토큰 초기 가격"	BIGINT		NOT NULL,
                          "토큰 현재 가격"	DECIMAL		NOT NULL,
                          "거래 가능 상태 (발행완료, 거래 중, 거래 중단, 거래 완료)"	ENUM('ISSUED', 'TRADING', 'SUSPENDED', 'CLOSED')		NOT NULL,
                          "실제 거래 가능한 상태로 게시된 시간"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "토큰이 처음 분리된 시간"	DATETIME	DEFAULT NOW()	NOT NULL,
                          "토큰 상태, 가격 등이 변경된 시간"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "DISCLOSURE" (
                              "공시 ID"	BIGINT		NOT NULL,
                              "부동산 ID"	BIGINT		NOT NULL,
                              "업로드 일자"	DATETIME		NOT NULL,
                              "수정 일자"	DATETIME		NOT NULL,
                              "제목"	VARCHAR(255)		NOT NULL,
                              "내용"	VARCHAR(255)		NOT NULL,
                              "분류(1=건물, 2=배당, 3=기타)"	ENUM('BUILDING', 'DIVIDEND', 'ETC')		NOT NULL,
                              "시스템 자동 업로드 여부"	BOOLEAN	DEFAULT FALSE	NOT NULL
);

CREATE TABLE "WALLETS" (
                           "지갑 ID"	BIGINT		NOT NULL,
                           "회원 ID"	BIGINT		NOT NULL,
                           "지갑 주소"	VARCHAR(255)		NOT NULL,
                           "서비스 관리, 사용자 관리 여부"	ENUM('CUSTODIAL', 'EXTERNAL')	DEFAULT CUSTODIAL	NOT NULL,
                           "지갑 활성화 여부"	ENUM('ACTIVE','SUSPENDED','TREASURY'	DEFAULT TRUE	NOT NULL,
	"생성시간"	DATETIME		NOT NULL,
	"변경일자"	DATETIME		NOT NULL
);

CREATE TABLE "ADMINS" (
                          "관리자 ID"	BIGINT		NOT NULL,
                          "관리자 로그인 ID"	VARCHAR		NOT NULL,
                          "관리자 로그인 PW"	VARCHAR		NOT NULL
);

CREATE TABLE "LIKES" (
                         "관심 ID"	BIGINT		NOT NULL,
                         "회원 ID"	BIGINT		NOT NULL,
                         "부동산 ID"	BIGINT		NOT NULL
);

CREATE TABLE "ALLOCATION_PAYOUTS" (
                                      "배당 지급 내역 ID"	BIGINT		NOT NULL,
                                      "회원 ID"	BIGINT		NOT NULL,
                                      "배당 관리 ID"	BIGINT		NOT NULL,
                                      "토큰 ID"	BIGINT		NOT NULL,
                                      "지급 날짜"	DATETIME		NOT NULL,
                                      "회원이 그 달에 받은 자산 별 총 배당금"	BIGINT	DEFAULT 0	NOT NULL,
                                      "배당 기준일 당시 토큰 보유 수량"	BIGINT		NOT NULL,
                                      "지급 상태(대기, 성공, 실패)"	ENUM('PENDING', 'SUCCESS', 'FAILED')		NOT NULL
);

CREATE TABLE "BLOCKCHAIN_TX" (
                                 "블록체인 TX ID"	BIGINT		NOT NULL,
                                 "큐 ID"	BIGINT		NULL,
                                 "거래 완료 ID"	BIGINT		NOT NULL,
                                 "지분 ID"	BIGINT		NOT NULL,
                                 "블록체인 트랜잭션 해시"	VARCHAR(255)		NULL,
                                 "송신 주소"	VARCHAR(255)		NULL,
                                 "수신 주소"	VARCHAR(255)		NULL,
                                 "호출한 컨트랙트 주소"	VARCHAR(255)		NOT NULL,
                                 "사용된 가스량(체인 비용 추적용)"	BIGINT		NULL,
                                 "포함된 블록 번호"	BIGINT		NULL,
                                 "트랜잭션 상태(SUBMITTED,CONFIRMED,REVERTED,UNKNOWN)"	ENUM		NOT NULL,
                                 "체인에 제출한 시간"	DATETIME	DEFAULT NOW()	NOT NULL,
                                 "실제 확정된 시간"	DATETIME		NULL,
                                 "Field"	ENUM('TRADE,' 'DEPLOY')		NOT NULL
);

CREATE TABLE "NOTICES" (
                           "공지 ID"	BIGINT		NOT NULL,
                           "공지 타입 (1=시스템, 2=일반 공지)"	ENUM ('SYSTEM', 'GENERAL')		NOT NULL,
                           "제목"	VARCHAR(255)		NOT NULL,
                           "내용"	TEXT		NOT NULL,
                           "생성 일자"	DATETIME		NOT NULL,
                           "수정 일자"	DATETIME		NOT NULL
);

CREATE TABLE "LOGIN_LOG" (
                             "로그인 로그 ID"	BIGINT		NOT NULL,
                             "회원 ID"	BIGINT		NULL,
                             "IP"	VARCHAR(45)		NOT NULL,
                             "로그인 성공, 실패 상태"	ENUM ('SUCCESS', 'FAILED')		NOT NULL,
                             "로그인 시도 시각"	DATETIME		NOT NULL
);

CREATE TABLE "BANKINGS" (
                            "입출금 id"	BIGINT		NOT NULL,
                            "계좌 ID"	BIGINT		NOT NULL,
                            "입금 출금 ('입금', '출금', '주문잠금', '주문해제', '체결정산', '배당입금')"	ENUM('DEPOSIT', 'WITHDRAWAL', 'ORDER_LOCK', 'ORDER_UNLOCK', 'TRADE_SETTLEMENT', 'DIVIDEND_DEPOSIT')		NOT NULL,
                            "거래 대기, 성공, 실패"	ENUM('PENDING', 'SUCCESS', 'FAILED')		NOT NULL,
                            "거래 금액"	BIGINT		NOT NULL,
                            "거래 후 잔고 스냅샷"	BIGINT		NOT NULL,
                            "입출금 일자"	DATETIME	DEFAULT NOW()	NOT NULL
);

CREATE TABLE "CANDLE_MINUTES" (
                                  "시계열 데이터 ID"	BIGINT		NOT NULL,
                                  "토큰 ID"	BIGINT		NOT NULL,
                                  "시가"	DECIMAL		NULL,
                                  "고가"	DECIMAL		NULL,
                                  "저가"	DECIMAL		NULL,
                                  "종가"	DECIMAL		NULL,
                                  "해당 구간 체결량 합계"	DECIMAL		NULL,
                                  "1분 단위 타임 스탬프"	DATETIME		NOT NULL,
                                  "캔들 시간 구간 동안 실제로 거래가 체결된 횟수"	INT	DEFAULT 0	NOT NULL
);

CREATE TABLE "CANDLE_MONTHS" (
                                 "시계열 데이터 ID"	BIGINT		NOT NULL,
                                 "토큰 ID"	BIGINT		NOT NULL,
                                 "시가"	DECIMAL		NULL,
                                 "고가"	DECIMAL		NULL,
                                 "저가"	DECIMAL		NULL,
                                 "종가"	DECIMAL		NULL,
                                 "해당 구간 체결량 합계"	DECIMAL		NULL,
                                 "1달 단위 타임 스탬프"	DATETIME		NOT NULL,
                                 "캔들 시간 구간 동안 실제로 거래가 체결된 횟수"	INT	DEFAULT 0	NOT NULL
);

ALTER TABLE "MEMBERS" ADD CONSTRAINT "PK_MEMBERS" PRIMARY KEY (
                                                               "회원 ID"
    );

ALTER TABLE "CANDLE_HOURS" ADD CONSTRAINT "PK_CANDLE_HOURS" PRIMARY KEY (
                                                                         "시계열 데이터 ID"
    );

ALTER TABLE "ORDERS" ADD CONSTRAINT "PK_ORDERS" PRIMARY KEY (
                                                             "거래 요청 ID"
    );

ALTER TABLE "TRADES" ADD CONSTRAINT "PK_TRADES" PRIMARY KEY (
                                                             "거래 완료 ID"
    );

ALTER TABLE "BLOCKCHAIN_OUTBOX_Q" ADD CONSTRAINT "PK_BLOCKCHAIN_OUTBOX_Q" PRIMARY KEY (
                                                                                       "큐 ID"
    );

ALTER TABLE "PLATFORM_TOKEN_HOLDINGS" ADD CONSTRAINT "PK_PLATFORM_TOKEN_HOLDINGS" PRIMARY KEY (
                                                                                               "지분 ID"
    );

ALTER TABLE "ALARMS" ADD CONSTRAINT "PK_ALARMS" PRIMARY KEY (
                                                             "알람 ID"
    );

ALTER TABLE "CANDLE_YEARS" ADD CONSTRAINT "PK_CANDLE_YEARS" PRIMARY KEY (
                                                                         "시계열 데이터 ID"
    );

ALTER TABLE "COMMONS" ADD CONSTRAINT "PK_COMMONS" PRIMARY KEY (
                                                               "기본 ID"
    );

ALTER TABLE "ASSET_BANKINGS" ADD CONSTRAINT "PK_ASSET_BANKINGS" PRIMARY KEY (
                                                                             "부동산 계좌 입출금 내역 id"
    );

ALTER TABLE "API_LOG" ADD CONSTRAINT "PK_API_LOG" PRIMARY KEY (
                                                               "API 로그 ID"
    );

ALTER TABLE "PLATFORM_BANKING" ADD CONSTRAINT "PK_PLATFORM_BANKING" PRIMARY KEY (
                                                                                 "플랫폼 입출금 내역 ID"
    );

ALTER TABLE "ALLOCATION_EVENTS" ADD CONSTRAINT "PK_ALLOCATION_EVENTS" PRIMARY KEY (
                                                                                   "배당 관리 ID"
    );

ALTER TABLE "ASSETS" ADD CONSTRAINT "PK_ASSETS" PRIMARY KEY (
                                                             "부동산 ID"
    );

ALTER TABLE "TOKEN_HOLDINGS" ADD CONSTRAINT "PK_TOKEN_HOLDINGS" PRIMARY KEY (
                                                                             "종목별 토큰 보유량 PK"
    );

ALTER TABLE "FILES" ADD CONSTRAINT "PK_FILES" PRIMARY KEY (
                                                           "파일 ID"
    );

ALTER TABLE "CANDLE_DAYS" ADD CONSTRAINT "PK_CANDLE_DAYS" PRIMARY KEY (
                                                                       "시계열 데이터 ID"
    );

ALTER TABLE "TRADE_LOGS" ADD CONSTRAINT "PK_TRADE_LOGS" PRIMARY KEY (
                                                                     "거래 완료 로그 ID"
    );

ALTER TABLE "ASSET_ACCOUNTS" ADD CONSTRAINT "PK_ASSET_ACCOUNTS" PRIMARY KEY (
                                                                             "부동산 계좌 id"
    );

ALTER TABLE "ACCOUNTS" ADD CONSTRAINT "PK_ACCOUNTS" PRIMARY KEY (
                                                                 "계좌 ID"
    );

ALTER TABLE "PLATFORM_ACCOUNTS" ADD CONSTRAINT "PK_PLATFORM_ACCOUNTS" PRIMARY KEY (
                                                                                   "플랫폼 계좌 ID"
    );

ALTER TABLE "TOKENS" ADD CONSTRAINT "PK_TOKENS" PRIMARY KEY (
                                                             "토큰 ID"
    );

ALTER TABLE "DISCLOSURE" ADD CONSTRAINT "PK_DISCLOSURE" PRIMARY KEY (
                                                                     "공시 ID"
    );

ALTER TABLE "WALLETS" ADD CONSTRAINT "PK_WALLETS" PRIMARY KEY (
                                                               "지갑 ID"
    );

ALTER TABLE "ADMINS" ADD CONSTRAINT "PK_ADMINS" PRIMARY KEY (
                                                             "관리자 ID"
    );

ALTER TABLE "LIKES" ADD CONSTRAINT "PK_LIKES" PRIMARY KEY (
                                                           "관심 ID"
    );

ALTER TABLE "ALLOCATION_PAYOUTS" ADD CONSTRAINT "PK_ALLOCATION_PAYOUTS" PRIMARY KEY (
                                                                                     "배당 지급 내역 ID"
    );

ALTER TABLE "BLOCKCHAIN_TX" ADD CONSTRAINT "PK_BLOCKCHAIN_TX" PRIMARY KEY (
                                                                           "블록체인 TX ID"
    );

ALTER TABLE "NOTICES" ADD CONSTRAINT "PK_NOTICES" PRIMARY KEY (
                                                               "공지 ID"
    );

ALTER TABLE "LOGIN_LOG" ADD CONSTRAINT "PK_LOGIN_LOG" PRIMARY KEY (
                                                                   "로그인 로그 ID"
    );

ALTER TABLE "BANKINGS" ADD CONSTRAINT "PK_BANKINGS" PRIMARY KEY (
                                                                 "입출금 id"
    );

ALTER TABLE "CANDLE_MINUTES" ADD CONSTRAINT "PK_CANDLE_MINUTES" PRIMARY KEY (
                                                                             "시계열 데이터 ID"
    );

ALTER TABLE "CANDLE_MONTHS" ADD CONSTRAINT "PK_CANDLE_MONTHS" PRIMARY KEY (
                                                                           "시계열 데이터 ID"
    );

