package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() {
    }

    @Override
    public void clear() throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        throw new DataAccessException("Not implemented");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        throw new DataAccessException("Not implemented");
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