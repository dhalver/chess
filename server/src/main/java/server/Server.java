package server;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.InMemoryDataAccess;
import io.javalin.Javalin;
import model.AuthData;
import model.GameData;
import service.ClearService;
import service.GameService;
import service.ServiceException;
import service.UserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Server {
    private final Javalin app;
    private final DataAccess dataAccess;

    public Server() {
        dataAccess = new InMemoryDataAccess();

        app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        });

        registerRoutes();
    }

    private void registerRoutes() {
        app.delete("/db", ctx -> {
            try {
                ClearService clearService = new ClearService(dataAccess);
                clearService.clear();
                ctx.status(200);
                ctx.result("{}");
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            }
        });

        app.post("/user", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                CreateUserRequest request = ctx.bodyAsClass(CreateUserRequest.class);

                AuthData auth = userService.register(
                        request.username(),
                        request.password(),
                        request.email()
                );

                ctx.status(200);
                ctx.json(new AuthResponse(auth.username(), auth.authToken()));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: bad request"));
            }
        });

        app.post("/session", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

                AuthData auth = userService.login(
                        request.username(),
                        request.password()
                );

                ctx.status(200);
                ctx.json(new AuthResponse(auth.username(), auth.authToken()));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: bad request"));
            }
        });

        app.delete("/session", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                String authToken = ctx.header("Authorization");

                userService.logout(authToken);

                ctx.status(200);
                ctx.result("{}");
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            }
        });

        app.post("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("Authorization");
                CreateGameRequest request = ctx.bodyAsClass(CreateGameRequest.class);

                GameData game = gameService.createGame(authToken, request.gameName());

                ctx.status(200);
                ctx.json(new CreateGameResponse(game.gameID()));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: bad request"));
            }
        });

        app.get("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("Authorization");

                Collection<GameData> games = gameService.listGames(authToken);
                List<GameSummary> summaries = new ArrayList<>();

                for (GameData game : games) {
                    summaries.add(new GameSummary(
                            game.gameID(),
                            game.whiteUsername(),
                            game.blackUsername(),
                            game.gameName()
                    ));
                }

                ctx.status(200);
                ctx.json(new ListGamesResponse(summaries));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            }
        });

        app.put("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("Authorization");
                JoinGameRequest request = ctx.bodyAsClass(JoinGameRequest.class);

                ChessGame.TeamColor color = parseColor(request.playerColor());
                Integer gameID = request.gameID();

                if (gameID == null) {
                    throw new ServiceException("Bad Request");
                }

                gameService.joinGame(authToken, color, gameID);

                ctx.status(200);
                ctx.result("{}");
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: bad request"));
            }
        });
    }

    private ChessGame.TeamColor parseColor(String color) throws ServiceException {
        if (color == null || color.isBlank()) {
            throw new ServiceException("Bad Request");
        }

        return switch (color.toUpperCase()) {
            case "WHITE" -> ChessGame.TeamColor.WHITE;
            case "BLACK" -> ChessGame.TeamColor.BLACK;
            default -> throw new ServiceException("Bad Request");
        };
    }

    private void handleServiceException(io.javalin.http.Context ctx, ServiceException e) {
        String message = e.getMessage();

        if ("Bad Request".equals(message)) {
            ctx.status(400);
        } else if ("Unauthorized".equals(message)) {
            ctx.status(401);
        } else if ("Already Taken".equals(message)) {
            ctx.status(403);
        } else {
            ctx.status(500);
        }

        ctx.json(new ErrorResponse("Error: " + message));
    }

    public int run(int desiredPort) {
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    public record CreateUserRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String username, String authToken) {}
}
