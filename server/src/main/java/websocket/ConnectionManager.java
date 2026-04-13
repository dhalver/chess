package websocket;

import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final Map<Integer, Map<String, WsContext>> connections = new ConcurrentHashMap<>();

    public void add(int gameID, String username, WsContext ctx) {
        connections
                .computeIfAbsent(gameID, id -> new ConcurrentHashMap<>())
                .put(username, ctx);
    }

    public void remove(int gameID, String username) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections != null) {
            gameConnections.remove(username);
            if (gameConnections.isEmpty()) {
                connections.remove(gameID);
            }
        }
    }

    public void broadcast(int gameID, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return;
        }

        for (WsContext ctx : gameConnections.values()) {
            ctx.send(message);
        }
    }

    public void broadcastExcept(int gameID, String excludedUsername, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return;
        }

        for (Map.Entry<String, WsContext> entry : gameConnections.entrySet()) {
            if (!entry.getKey().equals(excludedUsername)) {
                entry.getValue().send(message);
            }
        }
    }
}