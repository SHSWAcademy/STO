// useAlarmSocket — 알람 전용 WebSocket 훅
// 로그인한 사용자 전용: /topic/alarm/{memberId} 구독
// - 구독 직후: 백엔드가 미읽음 알람 스냅샷(배열)을 즉시 전송
// - 이후 체결/배당 이벤트: 단건 알람 객체 수신

import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '../lib/config.js';

export function useAlarmSocket({ memberId, token, onSnapshot, onNewAlarm }) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!memberId || !token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/trading`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('[AlarmWS] 연결 성공 — memberId:', memberId);
        client.subscribe(
          `/topic/alarm/${memberId}`,
          (msg) => {
            console.log('[AlarmWS] 메시지 수신 raw:', msg.body);
            try {
              const data = JSON.parse(msg.body);
              // 구독 직후 스냅샷: 배열 / 이후 실시간: 단건 객체
              if (Array.isArray(data)) {
                console.log('[AlarmWS] 스냅샷 수신, 건수:', data.length);
                onSnapshot?.(data);
              } else {
                console.log('[AlarmWS] 실시간 알람 수신:', data);
                onNewAlarm?.(data);
              }
            } catch (e) {
              console.warn('[AlarmWS] 메시지 파싱 실패', e);
            }
          },
          { Authorization: `Bearer ${token}` },
        );
      },
      onStompError: (frame) => {
        console.warn('[AlarmWS] STOMP 오류:', frame.headers?.message);
      },
      onDisconnect: () => {
        console.log('[AlarmWS] 연결 해제');
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [memberId, token]);

  return clientRef;
}
