// ru.mindils.jb2.app.integration.http.ExternalServiceException
package ru.mindils.jb2.app.integration.http;

public class ExternalServiceException extends RuntimeException {
  private final String responseBody;
  private final int statusCode;

  public ExternalServiceException(String message, String responseBody, int statusCode) {
    super(message);
    this.responseBody = responseBody;
    this.statusCode = statusCode;
  }

  // оставляем конструктор для обратной совместимости, если где-то ещё вызывается
  public ExternalServiceException(String message, String responseBody) {
    this(message, responseBody, -1);
  }

  public String responseBody() { return responseBody; }
  public int statusCode() { return statusCode; }
}
