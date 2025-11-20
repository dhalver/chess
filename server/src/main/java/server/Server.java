package server;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import model.AuthData;
import service.ClearService;
import service.GameService;
import service.ServiceException;
import service.UserService;

import java.util.Map;

public class Server {

    private final Javalin javalin;

    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;

    public Server() {
        this.dataAccess = new InMemoryDataAccess();
        this.clearService = new ClearService(dataAccess);
        this.userService = new UserService(dataAccess);
        this.gameService = new GameService(dataAccess);

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

        javalin.post("/game", this::handleCreateGame);
        javalin.get("/game", this::handleListGames);
        javalin.put("/game", this::handleJoinGame);
    }


    private void handleClear(Context ctx) {
        try {
            clearService.clear();
            ctx.status(200);
            ctx.json(Map.of());
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
        try {
            String authToken = ctx.header("authorization");
            userService.logout(authToken);
            ctx.status(200);
            ctx.json(Map.of()); // {}
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }


    private void handleCreateGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            CreateGameRequest req = ctx.bodyAsClass(CreateGameRequest.class);

            var game = gameService.createGame(authToken, req.gameName());
            ctx.status(200).json(new CreateGameResponse(game.gameID()));
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void handleListGames(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            var games = gameService.listGames(authToken);
            ctx.status(200).json(new ListGamesResponse(games));
        } catch (ServiceException e) {
            ctx.status(mapStatus(e));
            ctx.json(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private void handleJoinGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            JoinGameRequest req = ctx.bodyAsClass(JoinGameRequest.class);

            ChessGame.TeamColor color = null;
            if (req.playerColor() != null) {
                // tests send "WHITE" or "BLACK"
                color = ChessGame.TeamColor.valueOf(req.playerColor());
            }

            gameService.joinGame(authToken, color, req.gameID());
            ctx.status(200).json(Map.of()); // {}
        } catch (IllegalArgumentException e) {
            // bad playerColor string
            ctx.status(400).json(Map.of("message", "Error: Bad Request"));
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