package ui;

import client.ServerFacade;
import model.AuthData;
import server.GameSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ServerFacade FACADE = new ServerFacade(8080);
    private static final Scanner SCANNER = new Scanner(System.in);
    private static AuthData AuthData = null;
    private static List<GameSummary> LastListedGames = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        System.out.println("Type 'help' to begin.");

        boolean running = true;

        while (running) {
            if (AuthData == null) {
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

            AuthData = FACADE.login(username, password);
            System.out.println("Logged in as " + AuthData.username() + ".");
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

            AuthData = FACADE.register(username, password, email);
            System.out.println("Registered and logged in as " + AuthData.username() + ".");
        } catch (Exception e) {
            System.out.println("Register failed: " + e.getMessage());
        }
    }

    private static void logout() {
        try {
            FACADE.logout(AuthData.authToken());
            AuthData = null;
            LastListedGames.clear();
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

            FACADE.createGame(AuthData.authToken(), gameName);
            System.out.println("Game created.");
        } catch (Exception e) {
            System.out.println("Create failed: " + e.getMessage());
        }
    }

    private static void listGames() {
        try {
            var response = FACADE.listGames(AuthData.authToken());
            LastListedGames = new ArrayList<>(response.games());

            if (LastListedGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            for (int i = 0; i < LastListedGames.size(); i++) {
                GameSummary game = LastListedGames.get(i);

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
            if (LastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list' first.");
                return;
            }

            System.out.print("Enter game number: ");
            String choiceInput = SCANNER.nextLine().trim();
            int choice = Integer.parseInt(choiceInput);

            if (choice < 1 || choice > LastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = SCANNER.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Invalid color.");
                return;
            }

            int gameID = LastListedGames.get(choice - 1).gameID();
            FACADE.joinGame(AuthData.authToken(), gameID, color);

            System.out.println("Joined game as " + color + ".");
            drawBoard(color.equals("WHITE"));

        } catch (NumberFormatException e) {
            System.out.println("Invalid game number.");
        } catch (Exception e) {
            System.out.println("Play failed: " + e.getMessage());
        }
    }

    private static void observeGame() {
        try {
            if (LastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list' first.");
                return;
            }

            System.out.print("Enter game number: ");
            String choiceInput = SCANNER.nextLine().trim();
            int choice = Integer.parseInt(choiceInput);

            if (choice < 1 || choice > LastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            System.out.println("Observing game.");
            drawBoard(true);

        } catch (NumberFormatException e) {
            System.out.println("Invalid game number.");
        } catch (Exception e) {
            System.out.println("Observe failed: " + e.getMessage());
        }
    }

    private static final String LIGHT = "\u001B[47m";
    private static final String DARK = "\u001B[46m";
    private static final String RESET = "\u001B[0m";

    private static void drawBoard(boolean isWhitePerspective) {
        String[][] board = {
                {"r","n","b","q","k","b","n","r"},
                {"p","p","p","p","p","p","p","p"},
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {"P","P","P","P","P","P","P","P"},
                {"R","N","B","Q","K","B","N","R"}
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
}