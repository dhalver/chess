package ui;

import client.ServerFacade;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Welcome to Chess!");
        System.out.println("Type 'help' to begin.");

        Scanner scanner = new Scanner(System.in);
        ServerFacade facade = new ServerFacade(8080);

        boolean running = true;

        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            switch (input.toLowerCase()) {
                case "help" -> printHelp();
                case "quit" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                case "login" -> System.out.println("Login not implemented yet.");
                case "register" -> System.out.println("Register not implemented yet.");
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
                Commands:
                help      - show this message
                login     - log into your account
                register  - create a new account
                quit      - exit the program
                """);
    }
}