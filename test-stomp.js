import { Client } from "@stomp/stompjs";
import WebSocket from "websocket";

// Node에서 WebSocket 사용하도록 설정
global.WebSocket = WebSocket.w3cwebsocket;

const client = new Client({
    brokerURL: "ws://localhost:8082/chat",

    connectHeaders: {
        Authorization: "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IkJVWUVSIiwiZXhwIjoxNzc3OTY4NTk1LCJpYXQiOjE3Nzc5NjQ5OTV9.xPbLjGcxNNPY-mlbcX-kmMqIcpZAJ3Mf7Y7RnrvASJ8"
    },

    debug: (str) => {
        console.log("DEBUG:", str);
    },

    reconnectDelay: 5000,

    onConnect: (frame) => {
        console.log("✅ CONNECTED");

        // 1️⃣ 구독
        client.subscribe("/sub/chat/room/1", (message) => {
            console.log("📩 메시지 수신:", message.body);
        });

        // 2️⃣ 메시지 전송
        client.publish({
            destination: "/pub/chat/message",
            body: JSON.stringify({
                roomId: 1,
                message: "hello from node",
                type: "TALK",
                tempId: "123"
            }),
        });
    },

    onStompError: (frame) => {
        console.error("❌ STOMP ERROR:", frame);
    },

    onWebSocketError: (error) => {
        console.error("❌ WS ERROR:", error);
    }
});

client.activate();