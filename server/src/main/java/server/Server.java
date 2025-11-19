package server;

import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.ClearService;
import service.ServiceException;
import service.UserService;
import model.AuthData;

import java.util.Map;

public class Server {

    private final Javalin javalin;

    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;

    public Server() {
        this.dataAccess = new InMemoryDataAccess();
        this.clearService = new ClearService(dataAccess);
        this.userService = new UserService(dataAccess);

        this.javalin = Javalin.create(config -> config.staticFiles.add("web"));
        configureRoutes();
    }


    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }


    private void configureRoutes() {
        javalin.delete("/db", this::handleClear);

        javalin.post("/user", this::handleRegister);
        javalin.post("/session", this::handleLogin);
        javalin.delete("/session", this::handleLogout);


    }


    private void handleClear(Context ctx) {
        try {
            clearService.clear();
            ctx.status(200);
            ctx.json(Map.of("message", "Clear succeeded"));
        } catch (ServiceException e) {
            ctx.status(500);
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    /** POST /user  (register) */
    private void handleRegister(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String email = (String) body.get("email");

            AuthData auth = userService.register(username, password, email);

            ctx.status(200);
            ctx.json(Map.of(
                    "username", auth.username(),
                    "authToken", auth.authToken()
            ));
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void handleLogin(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            AuthData auth = userService.login(username, password);

            ctx.status(200);
            ctx.json(Map.of(
                    "username", auth.username(),
                    "authToken", auth.authToken()
            ));
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void handleLogout(Context ctx) {
        try {String authToken = ctx.header("Authorization");

            userService.logout(authToken);

            ctx.status(200);
            ctx.json(Map.of("message", "Logout succeeded"));
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }


    private int mapStatus(ServiceException e) {
        return switch (e.getMessage()) {
            case "Bad Request" -> 400;
            case "Already Taken" -> 403;
            case "Unauthorized" -> 401;
            default -> 500;
        };
    }
}