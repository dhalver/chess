package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class WebSocketHandler {
    private static final Gson GSON = new Gson();

    private final ConnectionManager connections = new ConnectionManager();
    private final DataAccess dataAccess;

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            UserGameCommand command = GSON.fromJson(ctx.message(), UserGameCommand.class);

            if (command == null || command.getCommandType() == null) {
                sendError(ctx, "Error: bad command");
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT -> connect(ctx, command);
                case LEAVE -> leave(ctx, command);
                case RESIGN -> resign(ctx, command);
                case MAKE_MOVE -> makeMove(ctx, command);
                default -> sendError(ctx, "Error: bad command");
            }

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    public void onClose(WsContext ctx) {
        connections.remove(ctx);
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

        connections.add(command.getGameID(), auth.username(), ctx);

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

        connections.broadcastExcept(
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

        String username = auth.username();
        GameData updatedGame = game;

        if (username.equals(game.whiteUsername())) {
            updatedGame = new GameData(
                    game.gameID(),
                    null,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );
            dataAccess.updateGame(updatedGame);
        } else if (username.equals(game.blackUsername())) {
            updatedGame = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    null,
                    game.gameName(),
                    game.game()
            );
            dataAccess.updateGame(updatedGame);
        }

        connections.remove(command.getGameID(), username);

        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                username + " has left the game"
        );

        connections.broadcast(
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

        connections.broadcast(
                command.getGameID(),
                GSON.toJson(notification)
        );
    }

    private void makeMove(WsContext ctx, UserGameCommand command) throws DataAccessException {
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
            sendError(ctx, "Error: observers cannot make moves");
            return;
        }

        boolean isWhiteTurn = game.game().getTeamTurn() == ChessGame.TeamColor.WHITE;
        boolean isCorrectTurn =
                (isWhiteTurn && auth.username().equals(game.whiteUsername()))
                        || (!isWhiteTurn && auth.username().equals(game.blackUsername()));

        if (!isCorrectTurn) {
            sendError(ctx, "Error: not your turn");
            return;
        }

        if (command.getMove() == null) {
            sendError(ctx, "Error: invalid move");
            return;
        }

        try {
            game.game().makeMove(command.getMove());
        } catch (Exception e) {
            sendError(ctx, "Error: invalid move");
            return;
        }

        ChessGame.TeamColor currentTurn = game.game().getTeamTurn();
        String currentPlayerUsername = currentTurn == ChessGame.TeamColor.WHITE
                ? game.whiteUsername()
                : game.blackUsername();

        if (game.game().isInCheckmate(currentTurn) || game.game().isInStalemate(currentTurn)) {
            game.game().setGameOver(true);
        }

        dataAccess.updateGame(game);

        ServerMessage loadGame = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                game.game(),
                null,
                null
        );
        connections.broadcast(command.getGameID(), GSON.toJson(loadGame));

        String moveNotificationText = auth.username() + " moved from "
                + command.getMove().getStartPosition()
                + " to "
                + command.getMove().getEndPosition();

        ServerMessage moveNotification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                moveNotificationText
        );
        connections.broadcastExcept(
                command.getGameID(),
                auth.username(),
                GSON.toJson(moveNotification)
        );

        if (game.game().isInCheckmate(currentTurn)) {
            ServerMessage checkmateNotification = new ServerMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    null,
                    null,
                    currentPlayerUsername + " is in checkmate"
            );
            connections.broadcast(command.getGameID(), GSON.toJson(checkmateNotification));
        } else if (game.game().isInStalemate(currentTurn)) {
            ServerMessage stalemateNotification = new ServerMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    null,
                    null,
                    "Stalemate"
            );
            connections.broadcast(command.getGameID(), GSON.toJson(stalemateNotification));
        } else if (game.game().isInCheck(currentTurn)) {
            ServerMessage checkNotification = new ServerMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    null,
                    null,
                    currentPlayerUsername + " is in check"
            );
            connections.broadcast(command.getGameID(), GSON.toJson(checkNotification));
        }
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