package com.scofu.network.message.internal;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class PendingRequestStore {

  private final Map<String, CompletableFuture> requests;
  private final SecureRandom secureRandom;

  @Inject
  PendingRequestStore() {
    this.requests = Maps.newConcurrentMap();
    this.secureRandom = new SecureRandom();
  }

  public <R> String prepare(CompletableFuture<R> future) {
    final var requestId = generateRequestId(16);
    requests.put(requestId, future);
    return requestId;
  }

  @SuppressWarnings("unchecked")
  public <R> Optional<CompletableFuture<R>> poll(String requestId) {
    return Optional.ofNullable(requests.remove(requestId));
  }

  private String generateRequestId(int size) {
    final var bytes = new byte[size];
    secureRandom.nextBytes(bytes);
    return Base64.getEncoder().withoutPadding().encodeToString(bytes).replaceAll("[/+]", "");
  }
}
