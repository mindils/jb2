package ru.mindils.jb2.app.integration.http;

public class ExternalServiceException extends RuntimeException {
  private final String responseBody;
  public ExternalServiceException(String message, String responseBody) {
    super(message);
    this.responseBody = responseBody;
  }
  public String responseBody() { return responseBody; }
}
