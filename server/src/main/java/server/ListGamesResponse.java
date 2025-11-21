package server;

import java.util.Collection;

public record ListGamesResponse(Collection<GameSummary> games) {}