import { useEffect, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { API_BASE_URL } from "../lib/config.js";

export function useDashboardSocket({
  tokenIds,
  candleType = "DAY",
  token,
  onTrade,
  onCandle,
}) {
  const clientRef = useRef(null);
  const onTradeRef = useRef(onTrade);
  const onCandleRef = useRef(onCandle);
  const tokenIdsKey = Array.isArray(tokenIds) ? tokenIds.join(",") : "";

  useEffect(() => {
    onTradeRef.current = onTrade;
  }, [onTrade]);

  useEffect(() => {
    onCandleRef.current = onCandle;
  }, [onCandle]);

  useEffect(() => {
    if (!tokenIdsKey) return;

    const currentTokenIds = tokenIdsKey
      .split(",")
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value));

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/trading`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        const authHeader = token ? { Authorization: `Bearer ${token}` } : {};

        currentTokenIds.forEach((tokenId) => {
          if (onTradeRef.current) {
            client.subscribe(
              `/topic/trades/${tokenId}`,
              (message) => {
                try {
                  onTradeRef.current?.({ tokenId, trade: JSON.parse(message.body) });
                } catch (error) {
                  console.warn("[DashboardWS] trade parse failed:", error);
                }
              },
              authHeader,
            );
          }

          if (onCandleRef.current) {
            client.subscribe(
              `/topic/candle/live/${tokenId}/${candleType}`,
              (message) => {
                try {
                  onCandleRef.current?.({ tokenId, candle: JSON.parse(message.body) });
                } catch (error) {
                  console.warn("[DashboardWS] candle parse failed:", error);
                }
              },
              authHeader,
            );
          }
        });
      },
      onStompError: (frame) => {
        console.warn("[DashboardWS] STOMP 오류:", frame.headers?.message);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [candleType, token, tokenIdsKey]);

  return clientRef;
}
