package ru.mindils.jb2.app.integration.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Component
public class JsonHttpClient {

  private final HttpClient client;
  private final ObjectMapper mapper;

  public JsonHttpClient(HttpClient client, ObjectMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  public <T> HttpResult<T> retrieve(URI uri, Class<T> type) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().build();
    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

    if (resp.statusCode() / 100 != 2) {
      throw new ExternalServiceException("GET " + uri + " failed with " + resp.statusCode(), resp.body());
    }

    T data = mapper.readValue(resp.body(), type);
    return new HttpResult<>(
        data,
        resp.statusCode(),
        resp.headers().map(),
        resp.body(),
        uri,
        Instant.now()
    );
  }

  public <T> HttpResult<T> retrieve(URI uri, TypeReference<T> typeRef) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().build();
    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

    if (resp.statusCode() / 100 != 2) {
      throw new ExternalServiceException(
          "GET " + uri + " failed with " + resp.statusCode(),
          resp.body(),
          resp.statusCode()
      );
    }

    T data = mapper.readValue(resp.body(), typeRef);
    return new HttpResult<>(
        data,
        resp.statusCode(),
        resp.headers().map(),
        resp.body(),
        uri,
        Instant.now()
    );
  }
}
