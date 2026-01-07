package com.securegate.audit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * WebSocket endpoint for real-time audit log streaming
 */
@ServerEndpoint("/audit")
public class AuditLogWebSocket {

    private static final Logger LOGGER = Logger.getLogger(AuditLogWebSocket.class.getName());

    // Connected sessions
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    // Audit event storage (in production, use database)
    private static final Map<String, JsonObject> auditLog = new ConcurrentHashMap<>();

    // Scheduled executor for demo heartbeat
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // Send heartbeat every 30 seconds to keep connections alive
        scheduler.scheduleAtFixedRate(() -> {
            JsonObject heartbeat = Json.createObjectBuilder()
                    .add("type", "heartbeat")
                    .add("timestamp", Instant.now().toString())
                    .add("connectedClients", sessions.size())
                    .build();
            broadcast(heartbeat);
        }, 30, 30, TimeUnit.SECONDS);
    }

    @OnOpen
    public void onOpen(Session session) {
        // Extract token from query string for authentication
        String query = session.getQueryString();
        String token = null;

        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        // For demo purposes, accept all connections
        // In production, validate JWT token here

        sessions.add(session);
        LOGGER.info("WebSocket client connected: " + session.getId() + " (Total: " + sessions.size() + ")");

        // Send welcome message
        try {
            JsonObject welcome = Json.createObjectBuilder()
                    .add("type", "connected")
                    .add("message", "Connected to SecureGate Audit Stream")
                    .add("sessionId", session.getId())
                    .add("timestamp", Instant.now().toString())
                    .build();
            session.getBasicRemote().sendText(welcome.toString());

            // Send recent audit events
            sendRecentEvents(session);

        } catch (IOException e) {
            LOGGER.warning("Error sending welcome message: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.fine("Received message from " + session.getId() + ": " + message);

        // Handle client commands
        try {
            JsonObject cmd = Json.createReader(new java.io.StringReader(message)).readObject();
            String type = cmd.getString("type", "");

            switch (type) {
                case "ping" -> {
                    JsonObject pong = Json.createObjectBuilder()
                            .add("type", "pong")
                            .add("timestamp", Instant.now().toString())
                            .build();
                    session.getBasicRemote().sendText(pong.toString());
                }
                case "subscribe" -> {
                    // Handle subscription preferences
                    LOGGER.fine("Client subscribed to events");
                }
                default -> LOGGER.fine("Unknown command type: " + type);
            }
        } catch (Exception e) {
            LOGGER.fine("Could not parse client message as JSON: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);
        LOGGER.info("WebSocket client disconnected: " + session.getId() +
                " (Reason: " + closeReason.getReasonPhrase() + ", Remaining: " + sessions.size() + ")");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.warning("WebSocket error for session " + session.getId() + ": " + throwable.getMessage());
        sessions.remove(session);
    }

    /**
     * Send recent audit events to newly connected client
     */
    private void sendRecentEvents(Session session) {
        // Send demo events
        try {
            JsonObject historyStart = Json.createObjectBuilder()
                    .add("type", "history_start")
                    .add("count", 3)
                    .build();
            session.getBasicRemote().sendText(historyStart.toString());

            // Demo events
            String[] eventTypes = { "login", "policy_update", "access_granted" };
            String[] actions = { "User authentication successful", "Policy p001 was modified",
                    "Access to audit logs granted" };

            for (int i = 0; i < eventTypes.length; i++) {
                JsonObject event = Json.createObjectBuilder()
                        .add("type", "audit_event")
                        .add("eventId", UUID.randomUUID().toString())
                        .add("eventType", eventTypes[i])
                        .add("action", actions[i])
                        .add("user", "admin@mortadha.me")
                        .add("resource", "api-gateway")
                        .add("result", "success")
                        .add("timestamp", Instant.now().minusSeconds((3 - i) * 300).toString())
                        .build();
                session.getBasicRemote().sendText(event.toString());
            }

            JsonObject historyEnd = Json.createObjectBuilder()
                    .add("type", "history_end")
                    .build();
            session.getBasicRemote().sendText(historyEnd.toString());

        } catch (IOException e) {
            LOGGER.warning("Error sending history: " + e.getMessage());
        }
    }

    /**
     * Broadcast audit event to all connected clients
     */
    public static void broadcast(JsonObject event) {
        String message = event.toString();
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    LOGGER.fine("Error broadcasting to session: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Log and broadcast an audit event
     */
    public static void logEvent(String eventType, String action, String user,
            String resource, String result, Map<String, Object> metadata) {
        String eventId = UUID.randomUUID().toString();

        var eventBuilder = Json.createObjectBuilder()
                .add("type", "audit_event")
                .add("eventId", eventId)
                .add("eventType", eventType)
                .add("action", action)
                .add("user", user != null ? user : "system")
                .add("resource", resource != null ? resource : "unknown")
                .add("result", result != null ? result : "unknown")
                .add("timestamp", Instant.now().toString());

        if (metadata != null) {
            var metaBuilder = Json.createObjectBuilder();
            metadata.forEach((k, v) -> metaBuilder.add(k, v != null ? v.toString() : ""));
            eventBuilder.add("metadata", metaBuilder);
        }

        JsonObject event = eventBuilder.build();

        // Store event
        auditLog.put(eventId, event);

        // Broadcast to connected clients
        broadcast(event);

        LOGGER.info("Audit event: " + eventType + " - " + action);
    }

    /**
     * Get count of connected clients
     */
    public static int getConnectedClientCount() {
        return sessions.size();
    }
}
