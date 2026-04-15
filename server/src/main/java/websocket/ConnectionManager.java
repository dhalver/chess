package websocket;

import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final Map<Integer, Map<String, WsContext>> connections = new ConcurrentHashMap<>();

    public void add(int gameID, String username, WsContext ctx) {
        Map<String, WsContext> gameConnections =
                connections.computeIfAbsent(gameID, id -> new ConcurrentHashMap<>());

        WsContext oldCtx = gameConnections.put(username, ctx);

        if (oldCtx != null && oldCtx != ctx) {
            try {
                oldCtx.session.close();
            } catch (Exception ignored) {
            }
        }
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

    public void clear() {
        connections.clear();
    }

    public void broadcast(int gameID, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return;
        }

        for (var entry : gameConnections.entrySet()) {
            try {
                if (entry.getValue().session.isOpen()) {
                    entry.getValue().send(message);
                } else {
                    remove(gameID, entry.getKey());
                }
            } catch (Exception ignored) {
                remove(gameID, entry.getKey());
            }
        }
    }

    public void broadcastExcept(int gameID, String excludedUsername, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return;
        }

        for (var entry : gameConnections.entrySet()) {
            if (!entry.getKey().equals(excludedUsername)) {
                try {
                    if (entry.getValue().session.isOpen()) {
                        entry.getValue().send(message);
                    } else {
                        remove(gameID, entry.getKey());
                    }
                } catch (Exception ignored) {
                    remove(gameID, entry.getKey());
                }
            }
        }
    }
}