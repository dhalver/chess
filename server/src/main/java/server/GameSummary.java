package server;

public record GameSummary(
        Integer gameID,
        String whiteUsername,
        String blackUsername,
        String gameName
) { }
