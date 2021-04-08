package org.jroots.queueing.client.database;

import java.util.concurrent.CompletableFuture;

public interface LimitsDatabaseClient {
    CompletableFuture<Integer> getLimit(String identifier);
}
