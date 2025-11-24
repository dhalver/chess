package service;

import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {

    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryDataAccess();
        userService = new UserService(dataAccess);
    }

    @Test
    void registerSuccess() throws Exception {
        AuthData auth = userService.register("devin", "password", "devin@example.com");

        assertNotNull(auth);
        assertEquals("devin", auth.username());
        assertNotNull(dataAccess.getUser("devin"));
        assertNotNull(dataAccess.getAuth(auth.authToken()));
    }

    @Test
    void registerDuplicateUsername() throws Exception {
        userService.register("jake", "lax", "jake@example.com");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.register("jake", "laxbro", "jakelax@example.com"));

        assertEquals("Already Taken", ex.getMessage());
    }

    @Test
    void registerBadRequestMissingField() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.register("christina", "", "christina@example.com"));

        assertEquals("Bad Request", ex.getMessage());
    }

    @Test
    void loginSuccess() throws Exception {
        userService.register("dave", "secret", "dave@example.com");

        AuthData auth = userService.login("dave", "secret");

        assertNotNull(auth);
        assertEquals("dave", auth.username());
        assertNotNull(dataAccess.getAuth(auth.authToken()));
    }

    @Test
    void loginUnauthorizedBadPassword() throws Exception {
        userService.register("erin", "pw", "erin@example.com");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.login("erin", "wrong"));

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void logoutSuccess() throws Exception {
        userService.register("frank", "pw", "frank@example.com");
        AuthData auth = userService.login("frank", "pw");

        assertNotNull(dataAccess.getAuth(auth.authToken()));

        userService.logout(auth.authToken());

        assertNull(dataAccess.getAuth(auth.authToken()));
    }

    @Test
    void logoutUnauthorizedBadToken() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> userService.logout("not-a-real-token"));

        assertEquals("Unauthorized", ex.getMessage());
    }
}

