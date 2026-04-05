package ui;

import client.ServerFacade;
import model.AuthData;

import java.util.Scanner;

public class Main {

    private static ServerFacade facade = new ServerFacade(8080);
    private static Scanner scanner = new Scanner(System.in);
    private static AuthData authData = null;

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
                    case "create" -> System.out.println("Create game not implemented yet.");
                    case "list" -> System.out.println("List games not implemented yet.");
                    case "play" -> System.out.println("Play game not implemented yet.");
                    case "observe" -> System.out.println("Observe game not implemented yet.");
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
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
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