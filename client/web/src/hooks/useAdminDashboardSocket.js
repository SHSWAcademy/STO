import { useEffect, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { API_BASE_URL } from "../lib/config.js";

export function useAdminDashboardSocket({ token, onDashboard, onTrade }) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!token) return;

    const authHeader = { Authorization: `Bearer ${token}` };
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/admin`),
      connectHeaders: authHeader,
      reconnectDelay: 5000,
      onConnect: () => {
        if (onDashboard) {
          client.subscribe(
            "/topic/admin/dashboard",
            (message) => {
              try {
                const data = JSON.parse(message.body);
                if (import.meta.env.DEV) {
                  console.debug("[AdminDashboardWS] dashboard received:", data);
                }
                onDashboard(data);
              } catch (error) {
                console.warn("[AdminDashboardWS] dashboard parse failed:", error);
              }
            },
            authHeader,
          );
        }

        if (onTrade) {
          client.subscribe(
            "/topic/admin/trades",
            (message) => {
              try {
                onTrade(JSON.parse(message.body));
              } catch (error) {
                console.warn("[AdminDashboardWS] trade parse failed:", error);
              }
            },
            authHeader,
          );
        }
      },
      onStompError: (frame) => {
        console.warn("[AdminDashboardWS] STOMP error:", frame.headers?.message);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [token, onDashboard, onTrade]);

  return clientRef;
}
