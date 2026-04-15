package websocket;

import io.javalin.websocket.WsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final Map<Integer, Map<String, WsContext>> connections =
            new ConcurrentHashMap<>();

    public void add(int gameID, String username, WsContext ctx) {
        Map<String, WsContext> gameConnections =
                connections.computeIfAbsent(gameID, id -> new ConcurrentHashMap<>());

        WsContext oldCtx = gameConnections.put(username, ctx);

        if (oldCtx != null && oldCtx != ctx) {
            try {
                if (oldCtx.session.isOpen()) {
                    oldCtx.session.close();
                }
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

    public void remove(WsContext ctx) {
        for (var gameEntry : connections.entrySet()) {
            Integer gameID = gameEntry.getKey();
            Map<String, WsContext> gameConnections = gameEntry.getValue();

            String usernameToRemove = null;
            for (var userEntry : gameConnections.entrySet()) {
                if (userEntry.getValue() == ctx) {
                    usernameToRemove = userEntry.getKey();
                    break;
                }
            }

            if (usernameToRemove != null) {
                gameConnections.remove(usernameToRemove);
                if (gameConnections.isEmpty()) {
                    connections.remove(gameID);
                }
                return;
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

        Map<String, WsContext> snapshot = new HashMap<>(gameConnections);

        for (var entry : snapshot.entrySet()) {
            String username = entry.getKey();
            WsContext ctx = entry.getValue();

            try {
                if (ctx.session.isOpen()) {
                    ctx.send(message);
                } else {
                    remove(gameID, username);
                }
            } catch (Exception ignored) {
                remove(gameID, username);
            }
        }
    }

    public void broadcastExcept(int gameID, String excludedUsername, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return;
        }

        Map<String, WsContext> snapshot = new HashMap<>(gameConnections);

        for (var entry : snapshot.entrySet()) {
            String username = entry.getKey();
            WsContext ctx = entry.getValue();

            if (!username.equals(excludedUsername)) {
                try {
                    if (ctx.session.isOpen()) {
                        ctx.send(message);
                    } else {
                        remove(gameID, username);
                    }
                } catch (Exception ignored) {
                    remove(gameID, username);
                }
            }
        }
    }
}