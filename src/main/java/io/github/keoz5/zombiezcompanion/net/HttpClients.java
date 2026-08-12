package io.github.keoz5.zombiezcompanion.net;

import java.net.http.HttpClient;
import java.time.Duration;

public final class HttpClients {
    public static final HttpClient SHARED = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    // Follows redirects — GitHub release-asset URLs 302 to a CDN host.
    public static final HttpClient DOWNLOAD = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(HttpClient.Redirect.NORMAL).build();

    private HttpClients() {
    }
}

