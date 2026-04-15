package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import model.AuthData;
import model.GameData;
import service.ClearService;
import service.GameService;
import service.ServiceException;
import service.UserService;
import websocket.WebSocketHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Server {
    private final Javalin app;
    private final DataAccess dataAccess;
    private final Gson gson;
    private final WebSocketHandler webSocketHandler;

    public Server() {
        try {
            this.dataAccess = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }

        this.gson = new Gson();
        this.webSocketHandler = new WebSocketHandler(dataAccess);

        this.app = Javalin.create(config -> config.staticFiles.add("web"));

        registerClearEndpoint();
        registerUserEndpoints();
        registerGameEndpoints();
        registerWebSocketEndpoint();
    }

    private void registerWebSocketEndpoint() {
        app.ws("/ws", ws -> {
            ws.onMessage(webSocketHandler::onMessage);
            ws.onClose(webSocketHandler::onClose);
        });
    }

    private void registerClearEndpoint() {
        app.delete("/db", ctx -> {
            try {
                ClearService clearService = new ClearService(dataAccess);
                clearService.clear();
                writeJson(ctx, 200, new EmptyResponse());
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            }
        });
    }

    private void registerUserEndpoints() {
        app.post("/user", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                CreateUserRequest request = gson.fromJson(ctx.body(), CreateUserRequest.class);

                AuthData auth = userService.register(
                        request.username(),
                        request.password(),
                        request.email()
                );

                writeJson(ctx, 200, new AuthResponse(auth.username(), auth.authToken()));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 400, new ErrorResponse("Error: bad request"));
            }
        });

        app.post("/session", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);

                AuthData auth = userService.login(
                        request.username(),
                        request.password()
                );

                writeJson(ctx, 200, new AuthResponse(auth.username(), auth.authToken()));
            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 400, new ErrorResponse("Error: bad request"));
            }
        });

        app.delete("/session", ctx -> {
            try {
                UserService userService = new UserService(dataAccess);
                String authToken = ctx.header("authorization");

                userService.logout(authToken);
                writeJson(ctx, 200, new EmptyResponse());

            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 500, new ErrorResponse("Error: " + e.getMessage()));
            }
        });
    }

    private void registerGameEndpoints() {
        app.post("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("authorization");
                CreateGameRequest request = gson.fromJson(ctx.body(), CreateGameRequest.class);

                GameData game = gameService.createGame(authToken, request.gameName());
                writeJson(ctx, 200, new CreateGameResponse(game.gameID()));

            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 400, new ErrorResponse("Error: bad request"));
            }
        });

        app.get("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("authorization");

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

                writeJson(ctx, 200, new ListGamesResponse(summaries));

            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 500, new ErrorResponse("Error: " + e.getMessage()));
            }
        });

        app.put("/game", ctx -> {
            try {
                GameService gameService = new GameService(dataAccess);
                String authToken = ctx.header("authorization");
                JoinGameRequest request = gson.fromJson(ctx.body(), JoinGameRequest.class);

                Integer gameID = request.gameID();
                if (gameID == null) {
                    throw new ServiceException("Bad Request");
                }

                ChessGame.TeamColor color = parseColor(request.playerColor());
                gameService.joinGame(authToken, color, gameID);

                writeJson(ctx, 200, new EmptyResponse());

            } catch (ServiceException e) {
                handleServiceException(ctx, e);
            } catch (Exception e) {
                writeJson(ctx, 400, new ErrorResponse("Error: bad request"));
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

    private void handleServiceException(Context ctx, ServiceException e) {
        String message = e.getMessage();

        if ("Bad Request".equals(message)) {
            writeJson(ctx, 400, new ErrorResponse("Error: bad request"));
        } else if ("Unauthorized".equals(message)) {
            writeJson(ctx, 401, new ErrorResponse("Error: unauthorized"));
        } else if ("Already Taken".equals(message)) {
            writeJson(ctx, 403, new ErrorResponse("Error: already taken"));
        } else {
            writeJson(ctx, 500, new ErrorResponse("Error: " + message));
        }
    }

    private void writeJson(Context ctx, int statusCode, Object responseBody) {
        ctx.status(statusCode);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(responseBody));
    }

    public int run(int desiredPort) {
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    public record CreateUserRequest(String username, String password, String email) { }
    public record LoginRequest(String username, String password) { }
    public record CreateGameRequest(String gameName) { }
    public record JoinGameRequest(String playerColor, Integer gameID) { }
    public record AuthResponse(String username, String authToken) { }
    public record CreateGameResponse(Integer gameID) { }
    public record GameSummary(
            Integer gameID,
            String whiteUsername,
            String blackUsername,
            String gameName
    ) { }
    public record ListGamesResponse(List<GameSummary> games) { }
    public record ErrorResponse(String message) { }
    public record EmptyResponse() { }
}
