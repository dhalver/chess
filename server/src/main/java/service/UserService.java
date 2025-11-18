package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;

private final DataAccess dataAccess;

public UserService(DataAccess dataAccess) {
    this.dataAccess = dataAccess;
}
