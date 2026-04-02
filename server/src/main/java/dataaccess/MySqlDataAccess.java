package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

import chess.ChessGame;
import com.google.gson.Gson;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private void configureDatabase() throws DataAccessException {
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) NOT NULL PRIMARY KEY,
                    password_hash VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS auth (
                    auth_token VARCHAR(255) NOT NULL PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS games (
                    game_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    white_username VARCHAR(255),
                    black_username VARCHAR(255),
                    game_name VARCHAR(255) NOT NULL,
                    game_json TEXT NOT NULL
                )
                """
        };

        try (var conn = DatabaseManager.getConnection()) {
            for (String statement : statements) {
                try (var ps = conn.prepareStatement(statement)) {
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("Unable to configure database: " + e.getMessage());
        }
    }

    @Override
    public void clear() throws DataAccessException {
        String[] statements = {
                "DELETE FROM auth",
                "DELETE FROM games",
                "DELETE FROM users"
        };

        try (var conn = DatabaseManager.getConnection()) {
            for (String statement : statements) {
                try (var ps = conn.prepareStatement(statement)) {
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("Unable to clear database: " + e.getMessage());
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        String statement = """
            INSERT INTO users (username, password_hash, email)
            VALUES (?, ?, ?)
            """;

        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setString(1, user.username());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.email());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Unable to create user: " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String statement = """
            SELECT username, password_hash, email
            FROM users
            WHERE username = ?
            """;

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setString(1, username);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password_hash"), // NOTE: this is hashed password
                            rs.getString("email")
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("Unable to get user: " + e.getMessage());
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String statement = """
            INSERT INTO auth (auth_token, username)
            VALUES (?, ?)
            """;

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Unable to create auth: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String statement = """
            SELECT auth_token, username
            FROM auth
            WHERE auth_token = ?
            """;

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setString(1, authToken);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("auth_token"),
                            rs.getString("username")
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("Unable to get auth: " + e.getMessage());
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String statement = """
            DELETE FROM auth
            WHERE auth_token = ?
            """;

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setString(1, authToken);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Unable to delete auth: " + e.getMessage());
        }
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        String statement = """
            INSERT INTO games (white_username, black_username, game_name, game_json)
            VALUES (?, ?, ?, ?)
            """;

        ChessGame game = new ChessGame();
        String gameJson = new Gson().toJson(game);

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, null);
            ps.setString(2, null);
            ps.setString(3, gameName);
            ps.setString(4, gameJson);

            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new DataAccessException("Unable to get game ID");

        } catch (Exception e) {
            throw new DataAccessException("Unable to create game: " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String statement = """
            SELECT game_id, white_username, black_username, game_name, game_json
            FROM games
            WHERE game_id = ?
            """;

        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(statement)) {

            ps.setInt(1, gameID);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String white = rs.getString("white_username");
                    String black = rs.getString("black_username");
                    String name = rs.getString("game_name");
                    String json = rs.getString("game_json");

                    ChessGame game = new Gson().fromJson(json, ChessGame.class);

                    return new GameData(
                            rs.getInt("game_id"),
                            white,
                            black,
                            name,
                            game
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("Unable to get game: " + e.getMessage());
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }
}