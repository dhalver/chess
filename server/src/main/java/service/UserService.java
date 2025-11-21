package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthData register(String username, String password, String email) throws ServiceException {
        try {
            if (username == null || password == null || email == null ||
                    username.isBlank() || password.isBlank() || email.isBlank()) {
                throw new ServiceException("Bad Request");
            }

            if (dataAccess.getUser(username) != null) {
                throw new ServiceException("Already Taken");
            }

            UserData user = new UserData(username, password, email);
            dataAccess.createUser(user);

            AuthData auth = new AuthData(java.util.UUID.randomUUID().toString(), username);
            dataAccess.createAuth(auth);
            return auth;

        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public AuthData login(String username, String password) throws ServiceException {
        try {
            // *** validate login input: bad/missing fields => 400 Bad Request ***
            if (username == null || password == null ||
                    username.isBlank() || password.isBlank()) {
                throw new ServiceException("Bad Request");
            }

            UserData user = dataAccess.getUser(username);

            if (user == null || !user.password().equals(password)) {
                throw new ServiceException("Unauthorized");
            }

            AuthData auth = new AuthData(java.util.UUID.randomUUID().toString(), username);
            dataAccess.createAuth(auth);
            return auth;

        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void logout(String authToken) throws ServiceException {
        try {
            var auth = dataAccess.getAuth(authToken);
            if (auth == null) {
                throw new ServiceException("Unauthorized");
            }
            dataAccess.deleteAuth(authToken);
        } catch (DataAccessException e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
