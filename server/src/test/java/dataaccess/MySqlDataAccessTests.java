package dataaccess;

import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlDataAccessTests {

    private DataAccess dao;

    @BeforeEach
    void setup() throws Exception {
        dao = new MySqlDataAccess();
        dao.clear();
    }

    @Test
    void createUserSuccess() throws Exception {
        UserData user = new UserData("test", "pass", "email@test.com");

        dao.createUser(user);
        UserData result = dao.getUser("test");

        assertNotNull(result);
        assertEquals("test", result.username());
    }

    @Test
    void createUserDuplicate() throws Exception {
        UserData user = new UserData("test", "pass", "email@test.com");

        dao.createUser(user);

        assertThrows(DataAccessException.class, () -> dao.createUser(user));
    }

    @Test
    void getUserSuccess() throws Exception {
        dao.createUser(new UserData("test", "pass", "email@test.com"));

        UserData user = dao.getUser("test");

        assertNotNull(user);
        assertEquals("test", user.username());
    }

    @Test
    void getUserNotFound() throws Exception {
        UserData result = dao.getUser("nope");
        assertNull(result);
    }

    @Test
    void createGameSuccess() throws Exception {
        int id = dao.createGame("game1");

        GameData game = dao.getGame(id);

        assertNotNull(game);
        assertEquals("game1", game.gameName());
    }

    @Test
    void createGameBadRequest() {
        assertThrows(DataAccessException.class, () -> dao.createGame(null));
    }

    @Test
    void getGameSuccess() throws Exception {
        int id = dao.createGame("game");

        GameData game = dao.getGame(id);

        assertNotNull(game);
        assertEquals("game", game.gameName());
    }

    @Test
    void getGameInvalid() throws Exception {
        GameData game = dao.getGame(9999);
        assertNull(game);
    }

    @Test
    void listGamesSuccess() throws Exception {
        dao.createGame("g1");
        dao.createGame("g2");

        var games = dao.listGames();

        assertEquals(2, games.size());
    }

    @Test
    void listGamesEmpty() throws Exception {
        var games = dao.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    void updateGameSuccess() throws Exception {
        int id = dao.createGame("game");

        GameData original = dao.getGame(id);
        GameData updated = new GameData(id, "white", null, "game", original.game());

        dao.updateGame(updated);

        GameData result = dao.getGame(id);
        assertEquals("white", result.whiteUsername());
    }

    @Test
    void updateGameInvalid() {
        assertThrows(DataAccessException.class, () ->
                dao.updateGame(new GameData(9999, "white", null, "game", null)));
    }

    @Test
    void clearRemovesData() throws Exception {
        dao.createUser(new UserData("test", "pass", "email"));
        dao.createGame("game");

        dao.clear();

        assertNull(dao.getUser("test"));
        assertTrue(dao.listGames().isEmpty());
    }
}
