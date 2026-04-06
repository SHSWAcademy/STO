// useTradingSocket — SockJS + STOMP 공통 WebSocket 훅
// tokenId, candleType 변경 시 재연결
// 백엔드 미실행 시 자동 재시도 (reconnectDelay: 5s)

import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export function useTradingSocket({
  tokenId,
  candleType = 'MINUTE',
  token,
  memberId,
  onOrderBook,
  onTrades,
  onCandle,
  onPendingOrders,
}) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!tokenId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/trading'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        if (onOrderBook) {
          client.subscribe(`/topic/orderBook/${tokenId}`, (msg) => {
            try { onOrderBook(JSON.parse(msg.body)); } catch (e) {}
          });
        }
        if (onTrades) {
          client.subscribe(`/topic/trades/${tokenId}`, (msg) => {
            try { onTrades(JSON.parse(msg.body)); } catch (e) {}
          });
        }
        if (onCandle) {
          client.subscribe(`/topic/candle/${candleType}/${tokenId}`, (msg) => {
            try { onCandle(JSON.parse(msg.body)); } catch (e) {}
          });
        }
        if (onPendingOrders && memberId) {
          client.subscribe(`/topic/pendingOrders/${tokenId}/${memberId}`, (msg) => {
            try { onPendingOrders(JSON.parse(msg.body)); } catch (e) {}
          });
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
  }, [tokenId, candleType]);

  return clientRef;
}
