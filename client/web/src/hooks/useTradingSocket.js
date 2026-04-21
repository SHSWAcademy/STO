// useTradingSocket — SockJS + STOMP 공통 WebSocket 훅
// tokenId, candleType 변경 시 재연결
// 백엔드 미실행 시 자동 재시도 (reconnectDelay: 5s)

import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '../lib/config.js';

export function useTradingSocket({
                                   tokenId,
                                   candleType = 'MINUTE',
                                   token,
                                   memberId,
                                   onOrderBook,
                                   onTrades,
                                   onCandle,
                                   onDayCandle,
                                   onPendingOrders,
                                 }) {
  const clientRef          = useRef(null);
  const onOrderBookRef     = useRef(onOrderBook);
  const onTradesRef        = useRef(onTrades);
  const onCandleRef        = useRef(onCandle);
  const onDayCandleRef     = useRef(onDayCandle);
  const onPendingOrdersRef = useRef(onPendingOrders);

  // 매 렌더마다 ref를 최신 콜백으로 갱신 — 재연결 없이 항상 최신 함수 호출
  onOrderBookRef.current     = onOrderBook;
  onTradesRef.current        = onTrades;
  onCandleRef.current        = onCandle;
  onDayCandleRef.current     = onDayCandle;
  onPendingOrdersRef.current = onPendingOrders;

  useEffect(() => {
    if (!tokenId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/trading`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        const authHeader = token ? { Authorization: `Bearer ${token}` } : {};
        if (onOrderBookRef.current) {
          client.subscribe(`/topic/orderBook/${tokenId}`, (msg) => {
            try { onOrderBookRef.current(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onTradesRef.current) {
          client.subscribe(`/topic/trades/${tokenId}`, (msg) => {
            try { onTradesRef.current(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onCandleRef.current) {
          client.subscribe(`/topic/candle/live/${tokenId}/${candleType}`, (msg) => {
            try {
              const data = JSON.parse(msg.body);
              onCandleRef.current(data);
              // 버그B: DAY 탭이면 이미 DAY 토픽을 구독 중이므로 같은 구독에서 onDayCandle도 처리
              if (candleType === 'DAY' && onDayCandleRef.current) onDayCandleRef.current(data);
            } catch (e) {}
          }, authHeader);
        }
        // DAY 캔들은 차트 주기와 무관하게 항상 구독 — 오늘 최고/최저가 실시간 갱신용
        // 버그B: candleType이 DAY면 위에서 이미 구독했으므로 중복 구독 방지
        if (onDayCandleRef.current && candleType !== 'DAY') {
          client.subscribe(`/topic/candle/live/${tokenId}/DAY`, (msg) => {
            try { onDayCandleRef.current(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onPendingOrdersRef.current && memberId) {
          client.subscribe(`/topic/pendingOrders/${tokenId}/${memberId}`, (msg) => {
            try { onPendingOrdersRef.current(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
      },
      onStompError: (frame) => {
        console.warn('[WS] STOMP 오류:', frame.headers?.message);
      },
      onDisconnect: () => {},
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [tokenId, candleType, token, memberId]);

  return clientRef;
}
