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
}
