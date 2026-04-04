package dataaccess;

import model.UserData;
import model.GameData;
import org.junit.jupiter.api.*;

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
    void getGameInvalid() throws Exception {
        GameData game = dao.getGame(9999);
        assertNull(game);
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
