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
        AuthData auth = getAuthorizedUser(ctx, command);
        if (auth == null) {
            return;
        }

        GameData game = getValidGame(ctx, command);
        if (game == null) {
            return;
        }

        connections.add(command.getGameID(), auth.username(), ctx);

        sendLoadGame(ctx, game.game());

        String notificationText = getConnectMessage(auth.username(), game);
        broadcastNotificationExcept(command.getGameID(), auth.username(), notificationText);
    }

    private void leave(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = getAuthorizedUser(ctx, command);
        if (auth == null) {
            return;
        }

        GameData game = getValidGame(ctx, command);
        if (game == null) {
            return;
        }

        String username = auth.username();

        if (username.equals(game.whiteUsername())) {
            game = new GameData(
                    game.gameID(),
                    null,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );
            dataAccess.updateGame(game);
        } else if (username.equals(game.blackUsername())) {
            game = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    null,
                    game.gameName(),
                    game.game()
            );
            dataAccess.updateGame(game);
        }

        connections.remove(command.getGameID(), username);
        broadcastNotification(command.getGameID(), username + " has left the game");
    }

    private void resign(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = getAuthorizedUser(ctx, command);
        if (auth == null) {
            return;
        }

        GameData game = getValidGame(ctx, command);
        if (game == null) {
            return;
        }

        if (game.game().isGameOver()) {
            sendError(ctx, "Error: game is already over");
            return;
        }

        if (!isPlayer(auth.username(), game)) {
            sendError(ctx, "Error: only players can resign");
            return;
        }

        game.game().setGameOver(true);
        dataAccess.updateGame(game);

        broadcastNotification(command.getGameID(), auth.username() + " has resigned the game");
    }

    private void makeMove(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = getAuthorizedUser(ctx, command);
        if (auth == null) {
            return;
        }

        GameData game = getValidGame(ctx, command);
        if (game == null) {
            return;
        }

        if (!validateMoveRequest(ctx, command, auth.username(), game)) {
            return;
        }

        if (!applyMove(ctx, command, game)) {
            return;
        }

        ChessGame.TeamColor currentTurn = game.game().getTeamTurn();
        String currentPlayerUsername = getCurrentPlayerUsername(game, currentTurn);

        updateGameOverState(game, currentTurn);
        dataAccess.updateGame(game);

        broadcastLoadGame(command.getGameID(), game.game());
        broadcastMoveNotification(command, auth.username());
        broadcastGameStateNotification(command.getGameID(), game.game(), currentTurn, currentPlayerUsername);
    }

    private AuthData getAuthorizedUser(WsContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(command.getAuthToken());
        if (auth == null) {
            sendError(ctx, "Error: invalid auth token");
            return null;
        }
        return auth;
    }

    private GameData getValidGame(WsContext ctx, UserGameCommand command) throws DataAccessException {
        GameData game = dataAccess.getGame(command.getGameID());
        if (game == null) {
            sendError(ctx, "Error: invalid game id");
            return null;
        }
        return game;
    }

    private boolean validateMoveRequest(
            WsContext ctx, UserGameCommand command, String username, GameData game) {
        if (game.game().isGameOver()) {
            sendError(ctx, "Error: game is already over");
            return false;
        }

        if (!isPlayer(username, game)) {
            sendError(ctx, "Error: observers cannot make moves");
            return false;
        }

        if (!isCorrectTurn(username, game)) {
            sendError(ctx, "Error: not your turn");
            return false;
        }

        if (command.getMove() == null) {
            sendError(ctx, "Error: invalid move");
            return false;
        }

        return true;
    }

    private boolean applyMove(WsContext ctx, UserGameCommand command, GameData game) {
        try {
            game.game().makeMove(command.getMove());
            return true;
        } catch (Exception e) {
            sendError(ctx, "Error: invalid move");
            return false;
        }
    }

    private boolean isPlayer(String username, GameData game) {
        return username.equals(game.whiteUsername()) || username.equals(game.blackUsername());
    }

    private boolean isCorrectTurn(String username, GameData game) {
        boolean isWhiteTurn = game.game().getTeamTurn() == ChessGame.TeamColor.WHITE;

        return (isWhiteTurn && username.equals(game.whiteUsername()))
                || (!isWhiteTurn && username.equals(game.blackUsername()));
    }

    private String getCurrentPlayerUsername(GameData game, ChessGame.TeamColor currentTurn) {
        return currentTurn == ChessGame.TeamColor.WHITE
                ? game.whiteUsername()
                : game.blackUsername();
    }

    private void updateGameOverState(GameData game, ChessGame.TeamColor currentTurn) {
        if (game.game().isInCheckmate(currentTurn) || game.game().isInStalemate(currentTurn)) {
            game.game().setGameOver(true);
        }
    }

    private String getConnectMessage(String username, GameData game) {
        if (username.equals(game.whiteUsername())) {
            return username + " connected as white";
        } else if (username.equals(game.blackUsername())) {
            return username + " connected as black";
        } else {
            return username + " connected as an observer";
        }
    }

    private void sendLoadGame(WsContext ctx, ChessGame game) {
        ServerMessage loadGame = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                game,
                null,
                null
        );
        ctx.send(GSON.toJson(loadGame));
    }

    private void broadcastLoadGame(int gameID, ChessGame game) {
        ServerMessage loadGame = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                game,
                null,
                null
        );
        connections.broadcast(gameID, GSON.toJson(loadGame));
    }

    private void broadcastMoveNotification(UserGameCommand command, String username) {
        String moveText = username + " moved from "
                + command.getMove().getStartPosition()
                + " to "
                + command.getMove().getEndPosition();

        broadcastNotificationExcept(command.getGameID(), username, moveText);
    }

    private void broadcastGameStateNotification(
            int gameID,
            ChessGame game,
            ChessGame.TeamColor currentTurn,
            String currentPlayerUsername) {
        if (game.isInCheckmate(currentTurn)) {
            broadcastNotification(gameID, currentPlayerUsername + " is in checkmate");
        } else if (game.isInStalemate(currentTurn)) {
            broadcastNotification(gameID, "Stalemate");
        } else if (game.isInCheck(currentTurn)) {
            broadcastNotification(gameID, currentPlayerUsername + " is in check");
        }
    }

    private void broadcastNotification(int gameID, String message) {
        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                message
        );
        connections.broadcast(gameID, GSON.toJson(notification));
    }

    private void broadcastNotificationExcept(int gameID, String excludedUsername, String message) {
        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                null,
                null,
                message
        );
        connections.broadcastExcept(gameID, excludedUsername, GSON.toJson(notification));
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