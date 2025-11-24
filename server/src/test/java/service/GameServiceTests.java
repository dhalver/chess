package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.InMemoryDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTests {

    private DataAccess dataAccess;
    private GameService gameService;
    private String authToken;   // token for "alice"

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryDataAccess();
        gameService = new GameService(dataAccess);

        try {
            UserData user = new UserData("Devin", "lax", "devin@example.com");
            dataAccess.createUser(user);

            AuthData auth = new AuthData("token1", "devin");
            dataAccess.createAuth(auth);

            authToken = auth.authToken();
        } catch (DataAccessException e) {
            fail(e);
        }
    }

    @Test
    void createGameSuccess() throws Exception {
        GameData game = gameService.createGame(authToken, "My Game");

        assertNotNull(game);
        assertEquals("My Game", game.gameName());
        assertTrue(game.gameID() > 0);

        GameData fromDao = dataAccess.getGame(game.gameID());
        assertNotNull(fromDao);
        assertEquals(game.gameID(), fromDao.gameID());
        assertEquals("My Game", fromDao.gameName());
    }

    @Test
    void createGameBadRequestEmptyName() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> gameService.createGame(authToken, " "));

        assertEquals("Bad Request", ex.getMessage());
    }

    @Test
    void createGameUnauthorizedNoAuth() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> gameService.createGame("bad-token", "Game"));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void listGamesSuccess() throws Exception {
        gameService.createGame(authToken, "G1");
        gameService.createGame(authToken, "G2");

        Collection<GameData> games = gameService.listGames(authToken);

        assertEquals(2, games.size());
    }

    @Test
    void listGamesUnauthorized() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> gameService.listGames("bad-token"));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void joinGameWhiteSuccess() throws Exception {
        GameData game = gameService.createGame(authToken, "Join Game");

        gameService.joinGame(authToken, ChessGame.TeamColor.WHITE, game.gameID());

        GameData fromDao = dataAccess.getGame(game.gameID());
        assertEquals("devin", fromDao.whiteUsername());
        assertNull(fromDao.blackUsername());
    }

    @Test
    void joinGameBlackSuccess() throws Exception {
        GameData game = gameService.createGame(authToken, "Join Game");

        gameService.joinGame(authToken, ChessGame.TeamColor.BLACK, game.gameID());

        GameData fromDao = dataAccess.getGame(game.gameID());
        assertEquals("devin", fromDao.blackUsername());
        assertNull(fromDao.whiteUsername());
    }

    @Test
    void joinGameBadGameId() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> gameService.joinGame(authToken, ChessGame.TeamColor.WHITE, 999_999));

        assertEquals("Bad Request", ex.getMessage());
    }

    @Test
    void joinGameAlreadyTakenSeat() throws Exception {
        GameData game = gameService.createGame(authToken, "Seat Game");
        gameService.joinGame(authToken, ChessGame.TeamColor.WHITE, game.gameID());

        try {
            UserData other = new UserData("jake", "pw", "jake@example.com");
            dataAccess.createUser(other);
            AuthData auth2 = new AuthData("token2", "jake");
            dataAccess.createAuth(auth2);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> gameService.joinGame(auth2.authToken(),
                            ChessGame.TeamColor.WHITE, game.gameID()));

            assertEquals("Already Taken", ex.getMessage());
        } catch (DataAccessException e) {
            fail(e);
        }
    }

    @Test
    void joinGameUnauthorizedBadToken() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> gameService.joinGame("bad-token",
                        ChessGame.TeamColor.WHITE, 1));

        assertEquals("Unauthorized", ex.getMessage());
    }
}
