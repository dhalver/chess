package service;

import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTests {

    private DataAccess dataAccess;
    private UserService userService;
    private GameService gameService;
    private ClearService clearService;

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryDataAccess();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
        clearService = new ClearService(dataAccess);
    }

    @Test
    void clearRemovesUsersGamesAndAuths() throws Exception {
        AuthData auth = userService.register("devin", "lax", "devin@example.com");
        GameData game = gameService.createGame(auth.authToken(), "TestGame");

        assertNotNull(dataAccess.getUser("devin"));
        assertNotNull(dataAccess.getAuth(auth.authToken()));
        assertNotNull(dataAccess.getGame(game.gameID()));

        clearService.clear();

        assertNull(dataAccess.getUser("devin"));
        assertNull(dataAccess.getAuth(auth.authToken()));
        assertNull(dataAccess.getGame(game.gameID()));
        assertTrue(dataAccess.listGames().isEmpty());
    }
}