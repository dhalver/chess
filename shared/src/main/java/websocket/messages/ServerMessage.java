package websocket.messages;

import chess.ChessGame;

public class ServerMessage {

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    private final ServerMessageType serverMessageType;
    private final ChessGame game;
    private final String errorMessage;
    private final String message;

    public ServerMessage(ServerMessageType serverMessageType) {
        this(serverMessageType, null, null, null);
    }

    public ServerMessage(ServerMessageType serverMessageType, ChessGame game, String errorMessage, String message) {
        this.serverMessageType = serverMessageType;
        this.game = game;
        this.errorMessage = errorMessage;
        this.message = message;
    }

    public ServerMessageType getServerMessageType() {
        return serverMessageType;
    }

    public ChessGame getGame() {
        return game;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }
}