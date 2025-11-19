package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {

    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private AuthData requireAuth(String authToken) throws ServiceException {
        try {
            var auth = dataAccess.getAuth(authToken);
            if (auth == null) {
                throw new ServiceException("Unauthorized");
            }
            return auth;
        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public GameData createGame(String authToken, String gameName) throws ServiceException {
        requireAuth(authToken);

        if (gameName == null || gameName.isBlank()) {
            throw new ServiceException("Bad Request");
        }

        try {
            int id = dataAccess.createGame(gameName);
            return dataAccess.getGame(id);
        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public Collection<GameData> listGames(String authToken) throws ServiceException {
        requireAuth(authToken);
        try {
            return dataAccess.listGames();
        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void joinGame(String authToken, ChessGame.TeamColor playerColor, int gameID)
            throws ServiceException {

        var auth = requireAuth(authToken);
        String username = auth.username();

        try {
            GameData game = dataAccess.getGame(gameID);
            if (game == null) {
                throw new ServiceException("Bad Request");
            }

            String white = game.whiteUsername();
            String black = game.blackUsername();

            if (playerColor == ChessGame.TeamColor.WHITE) {
                if (white != null && !white.equals(username)) {
                    throw new ServiceException("Already Taken");
                }
                game = new GameData(game.gameID(), username, black, game.gameName(), game.game());
            } else if (playerColor == ChessGame.TeamColor.BLACK) {
                if (black != null && !black.equals(username)) {
                    throw new ServiceException("Already Taken");
                }
                game = new GameData(game.gameID(), white, username, game.gameName(), game.game());
            }

            dataAccess.updateGame(game);
        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
