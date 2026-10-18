package com.intellij.plugins.haxe.runner.debugger.javascript;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.net.JdkProxyProvider;
import com.intellij.util.net.ssl.CertificateManager;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

/**
 * The IDE's {@link AdapterStore}: the pinned-adapter cache under the system
 * directory, downloading through the IDE's network settings — its proxy
 * selector and proxy credentials (Settings | Appearance &amp; Behavior |
 * System Settings | HTTP Proxy) and the server certificates the user
 * accepted. A bare JDK client would bypass all three.
 */
public final class AdapterStores {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

  private AdapterStores() {
  }

  /** The pinned-adapter cache: {@code <ide-system>/haxe/debug-adapters}. */
  public static Path root() {
    return Path.of(PathManager.getSystemPath(), "haxe", "debug-adapters");
  }

  public static AdapterStore open() {
    return new AdapterStore(root(), AdapterStores::ideHttpClient);
  }

  private static HttpClient ideHttpClient() {
    JdkProxyProvider proxy = JdkProxyProvider.getInstance();
    return HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(CONNECT_TIMEOUT)
      .proxy(proxy.getProxySelector())
      .authenticator(proxy.getAuthenticator())
      .sslContext(CertificateManager.getInstance().getSslContext())
      .build();
  }
}
