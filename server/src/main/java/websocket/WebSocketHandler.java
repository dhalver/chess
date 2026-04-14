package websocket;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class WebSocketHandler {
    private static final Gson GSON = new Gson();
    private static final ConnectionManager CONNECTIONS = new ConnectionManager();

    private final DataAccess dataAccess;

    public WebSocketHandler() {
        try {
            this.dataAccess = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize websocket data access", e);
        }
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            UserGameCommand command = GSON.fromJson(ctx.message(), UserGameCommand.class);

            switch (command.getCommandType()) {
                case CONNECT -> connect(ctx, command);
                case LEAVE -> leave(ctx, command);
                case RESIGN -> resign(ctx, command);
                case MAKE_MOVE -> sendError(ctx, "Error: MAKE_MOVE not implemented yet.");
            }

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void connect(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(command.getAuthToken());
        if (auth == null) {
            sendError(ctx, "Error: invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(command.getGameID());
        if (game == null) {
            sendError(ctx, "Error: invalid game id");
            return;
        }

        CONNECTIONS.add(command.getGameID(), auth.username(), ctx);

        ServerMessage loadGame = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                game.game(),
                null,
                null
        );
        ctx.send(GSON.toJson(loadGame));

        String notificationText;
        if (auth.username().equals(game.whiteUsername())) {
            notificationText = auth.username() + " connected as white";
        } else if (auth.username().equals(game.blackUsername())) {
            notificationText = auth.username() + " connected as black";
        } else {
            notificationText = auth.username() + " connected as an observer";
        }

        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                notificationText
        );

        CONNECTIONS.broadcastExcept(
                command.getGameID(),
                auth.username(),
                GSON.toJson(notification)
        );
    }

    private void leave(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(command.getAuthToken());
        if (auth == null) {
            sendError(ctx, "Error: invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(command.getGameID());
        if (game == null) {
            sendError(ctx, "Error: invalid game id");
            return;
        }

        CONNECTIONS.remove(command.getGameID(), auth.username());

        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                auth.username() + " has left the game"
        );

        CONNECTIONS.broadcast(
                command.getGameID(),
                GSON.toJson(notification)
        );
    }

    private void resign(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(command.getAuthToken());
        if (auth == null) {
            sendError(ctx, "Error: invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(command.getGameID());
        if (game == null) {
            sendError(ctx, "Error: invalid game id");
            return;
        }

        if (game.game().isGameOver()) {
            sendError(ctx, "Error: game is already over");
            return;
        }

        boolean isPlayer = auth.username().equals(game.whiteUsername())
                || auth.username().equals(game.blackUsername());

        if (!isPlayer) {
            sendError(ctx, "Error: only players can resign");
            return;
        }

        game.game().setGameOver(true);
        dataAccess.updateGame(game);

        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                auth.username() + " has resigned the game"
        );

        CONNECTIONS.broadcast(
                command.getGameID(),
                GSON.toJson(notification)
        );
    }

    private void sendError(WsContext ctx, String message) {
        ServerMessage error = new ServerMessage(
                ServerMessage.ServerMessageType.ERROR,
                null,
                message,
                null
        );
        ctx.send(GSON.toJson(error));
    }
}