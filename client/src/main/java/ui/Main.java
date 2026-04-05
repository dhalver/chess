package ui;

import client.ServerFacade;
import model.AuthData;
import server.GameSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ServerFacade facade = new ServerFacade(8080);
    private static final Scanner scanner = new Scanner(System.in);
    private static AuthData authData = null;
    private static List<GameSummary> lastListedGames = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        System.out.println("Type 'help' to begin.");

        boolean running = true;

        while (running) {
            if (authData == null) {
                System.out.print("[logged out] >>> ");
                String input = scanner.nextLine().trim().toLowerCase();

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
                String input = scanner.nextLine().trim().toLowerCase();

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
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            authData = facade.login(username, password);
            System.out.println("Logged in as " + authData.username() + ".");
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void register() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            authData = facade.register(username, password, email);
            System.out.println("Registered and logged in as " + authData.username() + ".");
        } catch (Exception e) {
            System.out.println("Register failed: " + e.getMessage());
        }
    }

    private static void logout() {
        try {
            facade.logout(authData.authToken());
            authData = null;
            lastListedGames.clear();
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private static void createGame() {
        try {
            System.out.print("Game name: ");
            String gameName = scanner.nextLine().trim();

            facade.createGame(authData.authToken(), gameName);
            System.out.println("Game created.");
        } catch (Exception e) {
            System.out.println("Create failed: " + e.getMessage());
        }
    }

    private static void listGames() {
        try {
            var response = facade.listGames(authData.authToken());
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
            int choice = Integer.parseInt(scanner.nextLine().trim());

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = scanner.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Invalid color.");
                return;
            }

            int gameID = lastListedGames.get(choice - 1).gameID();

            facade.joinGame(authData.authToken(), gameID, color);

            System.out.println("Joined game as " + color + ".");
            System.out.println("Board display not implemented yet.");

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
            int choice = Integer.parseInt(scanner.nextLine().trim());

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            int gameID = lastListedGames.get(choice - 1).gameID();

            // observers still need a valid color for your server
            facade.joinGame(authData.authToken(), gameID, "WHITE");

            System.out.println("Observing game.");
            System.out.println("Board display not implemented yet.");

        } catch (Exception e) {
            System.out.println("Observe failed: " + e.getMessage());
        }
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