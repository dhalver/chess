package client;

import com.google.gson.Gson;
import model.AuthData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import server.ListGamesResponse;
import server.CreateGameResponse;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        var requestBody = Map.of(
                "username", username,
                "password", password,
                "email", email
        );
        return this.makeRequest("POST", "/user", requestBody, null, AuthData.class);
    }

    public AuthData login(String username, String password) throws Exception {
        var requestBody = Map.of(
                "username", username,
                "password", password
        );
        return this.makeRequest("POST", "/session", requestBody, null, AuthData.class);
    }

    public void logout(String authToken) throws Exception {
        this.makeRequest("DELETE", "/session", null, authToken, null);
    }

    public ListGamesResponse listGames(String authToken) throws Exception {
        return this.makeRequest("GET", "/game", null, authToken, ListGamesResponse.class);
    }

    public int createGame(String authToken, String gameName) throws Exception {
        var requestBody = Map.of("gameName", gameName);

        CreateGameResponse response = this.makeRequest(
                "POST",
                "/game",
                requestBody,
                authToken,
                CreateGameResponse.class
        );

        return response.gameID();
    }

    public void joinGame(String authToken, int gameID, String playerColor) throws Exception {
        var requestBody = Map.of(
                "playerColor", playerColor,
                "gameID", gameID
        );

        this.makeRequest("PUT", "/game", requestBody, authToken, null);
    }

    private <T> T makeRequest(String method, String path, Object requestBody,
                              String authToken, Class<T> responseClass) throws Exception {
        URI uri = new URI(serverUrl + path);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod(method);
        connection.setDoInput(true);

        if (authToken != null) {
            connection.setRequestProperty("authorization", authToken);
        }

        if (requestBody != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", "application/json");
            String json = gson.toJson(requestBody);

            try (OutputStream reqBody = connection.getOutputStream()) {
                reqBody.write(json.getBytes());
            }
        }

        connection.connect();
        this.throwIfNotSuccessful(connection);

        if (responseClass == null) {
            return null;
        }

        try (InputStream responseBody = connection.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(responseBody);
            return gson.fromJson(reader, responseClass);
        }
    }

    public void clear() throws Exception {
        this.makeRequest("DELETE", "/db", null, null, null);
    }

    private void throwIfNotSuccessful(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        if (status / 100 != 2) {
            String message = "Request failed";
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    Map<?, ?> errorResponse = gson.fromJson(new InputStreamReader(errorStream), Map.class);
                    Object errorMessage = errorResponse.get("message");
                    if (errorMessage != null) {
                        message = errorMessage.toString();
                    }
                }
            }
            throw new Exception(message);
        }
    }
}
