package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class ClearService {

    private final DataAccess dataAccess;

    public ClearService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void clear() throws ServiceException {
        try {
            dataAccess.clear();
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Internal Server Error");
        }
    }
}

