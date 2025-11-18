package service;

public class ServiceException extends Exception {
    private final int statusCode;   // HTTP status code

    public ServiceException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}