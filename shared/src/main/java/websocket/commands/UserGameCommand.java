package websocket.commands;

import chess.ChessMove;

public class UserGameCommand {

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }

    private final CommandType commandType;
    private final String authToken;
    private final Integer gameID;
    private final ChessMove move;

    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this(commandType, authToken, gameID, null);
    }

    public UserGameCommand(CommandType commandType, String authToken, Integer gameID, ChessMove move) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.move = move;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    public ChessMove getMove() {
        return move;
    }
}
