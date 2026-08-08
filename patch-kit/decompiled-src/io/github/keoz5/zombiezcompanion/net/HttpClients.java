/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.net;

import java.net.http.HttpClient;
import java.time.Duration;

public final class HttpClients {
    public static final HttpClient SHARED = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();

    private HttpClients() {
    }
}

