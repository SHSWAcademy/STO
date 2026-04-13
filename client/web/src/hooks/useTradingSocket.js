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
                                   onPendingOrders,
                                 }) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!tokenId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/trading`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        const authHeader = token ? { Authorization: `Bearer ${token}` } : {};
        if (onOrderBook) {
          client.subscribe(`/topic/orderBook/${tokenId}`, (msg) => {
            try { onOrderBook(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onTrades) {
          client.subscribe(`/topic/trades/${tokenId}`, (msg) => {
            try { onTrades(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onCandle) {
          client.subscribe(`/topic/candle/live/${tokenId}/${candleType}`, (msg) => {
            try { onCandle(JSON.parse(msg.body)); } catch (e) {}
          }, authHeader);
        }
        if (onPendingOrders && memberId) {
          client.subscribe(`/topic/pendingOrders/${tokenId}/${memberId}`, (msg) => {
            try { onPendingOrders(JSON.parse(msg.body)); } catch (e) {}
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
