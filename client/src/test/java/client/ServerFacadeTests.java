package client;

import model.AuthData;
import org.junit.jupiter.api.*;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearDatabase() throws Exception {
        facade.clear();
    }

    @Test
    public void registerPositive() throws Exception {
        AuthData auth = facade.register("player1", "password", "p1@email.com");
        Assertions.assertNotNull(auth);
        Assertions.assertNotNull(auth.authToken());
        Assertions.assertEquals("player1", auth.username());
    }

    @Test
    public void registerNegative() throws Exception {
        facade.register("player1", "password", "p1@email.com");
        Assertions.assertThrows(Exception.class, () ->
                facade.register("player1", "password", "p1@email.com"));
    }

    @Test
    public void loginPositive() throws Exception {
        facade.register("player2", "password", "p2@email.com");
        AuthData auth = facade.login("player2", "password");
        Assertions.assertNotNull(auth);
        Assertions.assertNotNull(auth.authToken());
        Assertions.assertEquals("player2", auth.username());
    }

    @Test
    public void loginNegative() {
        Assertions.assertThrows(Exception.class, () ->
                facade.login("badUser", "badPassword"));
    }

    @Test
    public void logoutPositive() throws Exception {
        AuthData auth = facade.register("player3", "password", "p3@email.com");
        facade.logout(auth.authToken());
        Assertions.assertTrue(true);
    }

    @Test
    public void logoutNegative() {
        Assertions.assertThrows(Exception.class, () ->
                facade.logout("badToken"));
    }

    @Test
    public void createGamePositive() throws Exception {
        AuthData auth = facade.register("user1", "pass", "email@test.com");

        int gameID = facade.createGame(auth.authToken(), "testGame");

        Assertions.assertTrue(gameID > 0);
    }

    @Test
    public void createGameNegative() {
        Assertions.assertThrows(Exception.class, () ->
                facade.createGame("badToken", "testGame"));
    }

    @Test
    public void listGamesPositive() throws Exception {
        AuthData auth = facade.register("user2", "pass", "u2@email.com");

        facade.createGame(auth.authToken(), "game1");

        var response = facade.listGames(auth.authToken());

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.games());
        Assertions.assertTrue(response.games().size() >= 1);
    }

    @Test
    public void listGamesNegative() {
        Assertions.assertThrows(Exception.class, () ->
                facade.listGames("badToken"));
    }

    @Test
    public void joinGamePositive() throws Exception {
        AuthData auth = facade.register("user3", "pass", "u3@email.com");

        int gameID = facade.createGame(auth.authToken(), "game2");

        Assertions.assertDoesNotThrow(() ->
                facade.joinGame(auth.authToken(), gameID, "WHITE"));
    }

    @Test
    public void joinGameNegative() throws Exception {
        AuthData auth = facade.register("user4", "pass", "u4@email.com");

        int gameID = facade.createGame(auth.authToken(), "game3");

        Assertions.assertThrows(Exception.class, () ->
                facade.joinGame("badToken", gameID, "WHITE"));
    }
}
