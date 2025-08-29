package ru.mindils.jb2.app.integration.http;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record HttpResult<T>(
    T data,
    int status,
    Map<String, List<String>> headers,
    String rawBody,
    URI uri,
    Instant receivedAt
) {}