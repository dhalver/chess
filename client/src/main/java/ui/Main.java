package ui;

import client.ServerFacade;
import model.AuthData;
import server.GameSummary;
import websocket.UserGameCommand;
import websocket.WebSocketCommunicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ServerFacade FACADE = new ServerFacade(8080);
    private static final Scanner SCANNER = new Scanner(System.in);
    private static AuthData authData;
    private static List<GameSummary> lastListedGames = new ArrayList<>();
    private static WebSocketCommunicator communicator;

    private static final String LIGHT = "\u001B[47m";
    private static final String DARK = "\u001B[46m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        System.out.println("Type 'help' to begin.");

        boolean running = true;

        while (running) {
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
            authData = null;
            lastListedGames.clear();
            communicator = null;
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private static void createGame() {
        try {
            System.out.print("Game name: ");
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
            System.out.println("Connected to game via WebSocket.");
        } catch (Exception e) {
            System.out.println("WebSocket connect failed: " + e.getMessage());
        }
    }

    public static void handleLoadGame(String message) {
        System.out.println("LOAD_GAME received.");
        drawBoard(true);
    }

    public static void handleNotification(String message) {
        System.out.println("NOTIFICATION: " + message);
    }

    public static void handleError(String message) {
        System.out.println("ERROR: " + message);
    }

    private static void drawBoard(boolean isWhitePerspective) {
        String[][] board = {
                {"r", "n", "b", "q", "k", "b", "n", "r"},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };

        for (int row = 0; row < 8; row++) {
            int displayRow = isWhitePerspective ? 8 - row : row + 1;
            System.out.print(displayRow + " ");

            for (int col = 0; col < 8; col++) {
                int r = isWhitePerspective ? row : 7 - row;
                int c = isWhitePerspective ? col : 7 - col;

                boolean isLight = (row + col) % 2 == 0;
                String color = isLight ? LIGHT : DARK;

                String piece = board[r][c];
                if (piece.equals(".")) {
                    piece = " ";
                }

                System.out.print(color + " " + piece + " " + RESET);
            }
            System.out.println();
        }

        System.out.println("   a  b  c  d  e  f  g  h");
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

    public static AuthData getAuthData() {
        return authData;
    }

    public static void setAuthData(AuthData authData) {
        Main.authData = authData;
    }
}