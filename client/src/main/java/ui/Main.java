package ui;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import client.ServerFacade;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.AuthData;
import server.GameSummary;
import websocket.WebSocketCommunicator;
import websocket.commands.UserGameCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ServerFacade FACADE = new ServerFacade(8080);
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Gson GSON = new Gson();

    private static AuthData authData;
    private static List<GameSummary> lastListedGames = new ArrayList<>();
    private static WebSocketCommunicator communicator;
    private static ChessGame currentGame;
    private static boolean whitePerspective = true;
    private static boolean inGameplay = false;
    private static Integer activeGameId = null;

    private static final String LIGHT = "\u001B[47m";
    private static final String DARK = "\u001B[46m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        System.out.println("Type 'help' to begin.");

        boolean running = true;

        while (running) {
            if (inGameplay) {
                runGameplayLoop();
                continue;
            }

            if (authData == null) {
                System.out.print("[logged out] >>> ");
                String input = SCANNER.nextLine().trim().toLowerCase();

                if (input.isEmpty()) {
                    System.out.println("Please enter a command.");
                    continue;
                }

                switch (input) {
                    case "help" -> printPreloginHelp();
                    case "quit" -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    case "login" -> login();
                    case "register" -> register();
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            } else {
                System.out.print("[logged in] >>> ");
                String input = SCANNER.nextLine().trim().toLowerCase();

                if (input.isEmpty()) {
                    System.out.println("Please enter a command.");
                    continue;
                }

                switch (input) {
                    case "help" -> printPostloginHelp();
                    case "logout" -> logout();
                    case "create" -> createGame();
                    case "list" -> listGames();
                    case "play" -> playGame();
                    case "observe" -> observeGame();
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            }
        }
    }

    private static void runGameplayLoop() {
        while (inGameplay) {
            System.out.print("[gameplay] >>> ");
            String input = SCANNER.nextLine().trim().toLowerCase();

            if (input.isEmpty()) {
                System.out.println("Please enter a command.");
                continue;
            }

            switch (input) {
                case "help" -> printGameplayHelp();
                case "redraw" -> drawBoard(whitePerspective);
                case "leave" -> leaveGame();
                case "resign" -> resignGame();
                case "move" -> makeMove();
                case "highlight" -> System.out.println("Highlight not implemented yet.");
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static void login() {
        try {
            System.out.print("Enter username: ");
            String username = SCANNER.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be blank.");
                return;
            }

            System.out.print("Enter password: ");
            String password = SCANNER.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("Password cannot be blank.");
                return;
            }

            authData = FACADE.login(username, password);
            System.out.println("Logged in as " + authData.username() + ".");
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void register() {
        try {
            System.out.print("Enter username: ");
            String username = SCANNER.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be blank.");
                return;
            }

            System.out.print("Enter password: ");
            String password = SCANNER.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("Password cannot be blank.");
                return;
            }

            System.out.print("Enter email: ");
            String email = SCANNER.nextLine().trim();
            if (email.isEmpty()) {
                System.out.println("Email cannot be blank.");
                return;
            }

            authData = FACADE.register(username, password, email);
            System.out.println("Registered and logged in as " + authData.username() + ".");
        } catch (Exception e) {
            System.out.println("Register failed: " + e.getMessage());
        }
    }

    private static void logout() {
        try {
            FACADE.logout(authData.authToken());
            resetGameplayState();
            authData = null;
            lastListedGames.clear();
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private static void createGame() {
        try {
            System.out.print("Enter game name: ");
            String gameName = SCANNER.nextLine().trim();

            if (gameName.isEmpty()) {
                System.out.println("Game name cannot be blank.");
                return;
            }

            FACADE.createGame(authData.authToken(), gameName);
            System.out.println("Game created.");
        } catch (Exception e) {
            System.out.println("Create failed: " + e.getMessage());
        }
    }

    private static void listGames() {
        try {
            var response = FACADE.listGames(authData.authToken());
            lastListedGames = new ArrayList<>(response.games());

            if (lastListedGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            for (int i = 0; i < lastListedGames.size(); i++) {
                GameSummary game = lastListedGames.get(i);

                String white = game.whiteUsername() == null ? "-" : game.whiteUsername();
                String black = game.blackUsername() == null ? "-" : game.blackUsername();

                System.out.printf("%d. %s | White: %s | Black: %s%n",
                        i + 1,
                        game.gameName(),
                        white,
                        black);
            }
        } catch (Exception e) {
            System.out.println("List failed: " + e.getMessage());
        }
    }

    private static void playGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list' first.");
                return;
            }

            System.out.print("Enter game number: ");
            String choiceInput = SCANNER.nextLine().trim();
            int choice = Integer.parseInt(choiceInput);

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = SCANNER.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Invalid color.");
                return;
            }

            int gameID = lastListedGames.get(choice - 1).gameID();
            FACADE.joinGame(authData.authToken(), gameID, color);

            whitePerspective = color.equals("WHITE");
            activeGameId = gameID;

            System.out.println("Joined game as " + color + ".");
            connectToGameplay(gameID);

        } catch (NumberFormatException e) {
            System.out.println("Invalid game number.");
        } catch (Exception e) {
            System.out.println("Play failed: " + e.getMessage());
        }
    }

    private static void observeGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list' first.");
                return;
            }

            System.out.print("Enter game number: ");
            String choiceInput = SCANNER.nextLine().trim();
            int choice = Integer.parseInt(choiceInput);

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            int gameID = lastListedGames.get(choice - 1).gameID();

            whitePerspective = true;
            activeGameId = gameID;

            System.out.println("Observing game.");
            connectToGameplay(gameID);

        } catch (NumberFormatException e) {
            System.out.println("Invalid game number.");
        } catch (Exception e) {
            System.out.println("Observe failed: " + e.getMessage());
        }
    }

    private static void connectToGameplay(Integer gameID) {
        try {
            communicator = new WebSocketCommunicator("ws://localhost:8080/ws");

            UserGameCommand command = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authData.authToken(),
                    gameID
            );

            communicator.sendCommand(command);
            inGameplay = true;
            System.out.println("Connected to game via WebSocket.");
        } catch (Exception e) {
            System.out.println("WebSocket connect failed: " + e.getMessage());
        }
    }

    private static void leaveGame() {
        try {
            if (communicator != null && activeGameId != null) {
                UserGameCommand command = new UserGameCommand(
                        UserGameCommand.CommandType.LEAVE,
                        authData.authToken(),
                        activeGameId
                );
                communicator.sendCommand(command);
            }
            System.out.println("Left game.");
        } catch (Exception e) {
            System.out.println("Leave failed: " + e.getMessage());
        } finally {
            resetGameplayState();
        }
    }

    private static void resignGame() {
        try {
            System.out.print("Are you sure you want to resign? (yes/no): ");
            String answer = SCANNER.nextLine().trim().toLowerCase();

            if (!answer.equals("yes")) {
                System.out.println("Resign canceled.");
                return;
            }

            if (communicator != null && activeGameId != null) {
                UserGameCommand command = new UserGameCommand(
                        UserGameCommand.CommandType.RESIGN,
                        authData.authToken(),
                        activeGameId
                );
                communicator.sendCommand(command);
            }

            System.out.println("Resign command sent.");
        } catch (Exception e) {
            System.out.println("Resign failed: " + e.getMessage());
        }
    }

    private static void makeMove() {
        try {
            if (communicator == null || activeGameId == null) {
                System.out.println("Not connected to a game.");
                return;
            }

            System.out.print("Enter start position (e.g. e2): ");
            String startInput = SCANNER.nextLine().trim().toLowerCase();

            System.out.print("Enter end position (e.g. e4): ");
            String endInput = SCANNER.nextLine().trim().toLowerCase();

            ChessPosition startPosition = parsePosition(startInput);
            ChessPosition endPosition = parsePosition(endInput);
            ChessPiece.PieceType promotionPiece = readPromotionPiece();

            ChessMove move = new ChessMove(startPosition, endPosition, promotionPiece);

            UserGameCommand command = new UserGameCommand(
                    authData.authToken(),
                    activeGameId,
                    move
            );

            communicator.sendCommand(command);
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Invalid move input. Use positions like e2 and e4, and press Enter for no promotion.");
        } catch (Exception e) {
            System.out.println("Move failed: " + e.getMessage());
        }
    }

    private static ChessPosition parsePosition(String input) {
        if (input.length() != 2) {
            throw new IllegalArgumentException("Invalid position");
        }

        char file = input.charAt(0);
        char rankChar = input.charAt(1);

        if (file < 'a' || file > 'h' || rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Invalid position");
        }

        int column = file - 'a' + 1;
        int row = rankChar - '0';

        return new ChessPosition(row, column);
    }

    private static ChessPiece.PieceType readPromotionPiece() {
        System.out.print("Promotion piece (QUEEN, ROOK, BISHOP, KNIGHT, or press Enter for none): ");
        String input = SCANNER.nextLine().trim().toUpperCase();

        if (input.isEmpty()) {
            return null;
        }

        return switch (input) {
            case "QUEEN" -> ChessPiece.PieceType.QUEEN;
            case "ROOK" -> ChessPiece.PieceType.ROOK;
            case "BISHOP" -> ChessPiece.PieceType.BISHOP;
            case "KNIGHT" -> ChessPiece.PieceType.KNIGHT;
            default -> throw new IllegalArgumentException("Invalid promotion piece");
        };
    }

    private static void resetGameplayState() {
        communicator = null;
        currentGame = null;
        whitePerspective = true;
        inGameplay = false;
        activeGameId = null;
    }

    public static void handleLoadGame(String message) {
        try {
            JsonObject json = GSON.fromJson(message, JsonObject.class);
            currentGame = GSON.fromJson(json.get("game"), ChessGame.class);
            System.out.println("Game loaded.");
            drawBoard(whitePerspective);
        } catch (Exception e) {
            System.out.println("Failed to load game: " + e.getMessage());
        }
    }

    public static void handleNotification(String message) {
        try {
            JsonObject json = GSON.fromJson(message, JsonObject.class);
            String notification = json.has("message") && !json.get("message").isJsonNull()
                    ? json.get("message").getAsString()
                    : message;
            System.out.println(notification);
        } catch (Exception e) {
            System.out.println("Notification: " + message);
        }
    }

    public static void handleError(String message) {
        try {
            JsonObject json = GSON.fromJson(message, JsonObject.class);
            String errorMessage = json.has("errorMessage") && !json.get("errorMessage").isJsonNull()
                    ? json.get("errorMessage").getAsString()
                    : message;
            System.out.println(errorMessage);
        } catch (Exception e) {
            System.out.println("Error: " + message);
        }
    }

    public static void handleSocketClosed() {
        System.out.println("Connection closed. Returning to logged-in menu.");
        resetGameplayState();
    }

    private static void drawBoard(boolean isWhitePerspective) {
        if (currentGame == null) {
            System.out.println("No game loaded.");
            return;
        }

        var board = currentGame.getBoard();

        for (int row = 0; row < 8; row++) {
            int displayRow = isWhitePerspective ? 8 - row : row + 1;
            System.out.print(displayRow + " ");

            for (int col = 0; col < 8; col++) {
                int boardRow = isWhitePerspective ? 7 - row : row;
                int boardCol = isWhitePerspective ? col : 7 - col;

                ChessPiece piece = board.getPiece(new ChessPosition(boardRow + 1, boardCol + 1));

                String symbol = " ";
                if (piece != null) {
                    symbol = getPieceSymbol(piece);
                }

                boolean isLight = (row + col) % 2 == 0;
                String color = isLight ? LIGHT : DARK;

                System.out.print(color + " " + symbol + " " + RESET);
            }
            System.out.println();
        }

        if (isWhitePerspective) {
            System.out.println("   a  b  c  d  e  f  g  h");
        } else {
            System.out.println("   h  g  f  e  d  c  b  a");
        }
    }

    private static String getPieceSymbol(ChessPiece piece) {
        return switch (piece.getPieceType()) {
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "K" : "k";
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "Q" : "q";
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "R" : "r";
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "B" : "b";
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "N" : "n";
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "P" : "p";
        };
    }

    private static void printPreloginHelp() {
        System.out.println("""
                Commands:
                help      - show this message
                login     - log into your account
                register  - create a new account
                quit      - exit the program
                """);
    }

    private static void printPostloginHelp() {
        System.out.println("""
                Commands:
                help      - show this message
                logout    - log out of your account
                create    - create a game
                list      - list games
                play      - play a game
                observe   - observe a game
                """);
    }

    private static void printGameplayHelp() {
        System.out.println("""
                Commands:
                help       - show this message
                redraw     - redraw the chess board
                leave      - leave the game
                resign     - resign the game
                move       - make a move
                highlight  - highlight legal moves
                """);
    }
}