package com.intellij.plugins.haxe.runner.debugger.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Tests our mini http content server used to serve project output for browser targets
@DisplayName("Browser debugger: content http server")
public class ContentHttpServerTest {

  /// Request paths the resolver cannot even parse into a path.
  private static final List<String> UNPARSEABLE_PATHS = List.of("/C:secret.txt", "/app.js::$DATA", "/%00secret");

  private final HttpClient http = HttpClient.newHttpClient();

  private Path root;
  private Path outside;
  private ContentHttpServer server;

  @BeforeEach
  public void serveFixture() throws IOException {
    Path parent = Files.createTempDirectory("content-server-test");

    root = Files.createDirectory(parent.resolve("www"));
    outside = Files.writeString(parent.resolve("secret.txt"), "not served");

    Files.writeString(root.resolve("index.html"), "<html>hello</html>");
    Files.writeString(root.resolve("app.js"), "console.log('x');");
    Files.writeString(root.resolve("app.js.map"), "{\"version\":3}");

    server = new ContentHttpServer(root);
  }

  @AfterEach
  public void tearDown() {
    if (server != null) {
      server.close();
    }
  }

  @Test
  @DisplayName("serves files with types and no store")
  public void servesFilesWithTypesAndNoStore() throws Exception {
    HttpResponse<String> html = get("/index.html");

    assertEquals(200, html.statusCode());
    assertEquals("<html>hello</html>", html.body());
    assertEquals("no-store", html.headers().firstValue("Cache-Control").orElse(""));
    assertTrue(html.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));

    assertTrue(get("/app.js").headers().firstValue("Content-Type").orElse("").startsWith("text/javascript"));
    assertTrue(get("/app.js.map").headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
  }

  @Test
  @DisplayName("directory serves its index")
  public void directoryServesItsIndex() throws Exception {
    HttpResponse<String> response = get("/");

    assertEquals(200, response.statusCode());
    assertEquals("<html>hello</html>", response.body());
  }

  @Test
  @DisplayName("missing file is 404")
  public void missingFileIs404() throws Exception {
    assertEquals(404, get("/nope.js").statusCode());
  }

  @Test
  @DisplayName("traversal outside the root is 404")
  public void traversalOutsideTheRootIs404() throws Exception {
    assertTrue(Files.isRegularFile(outside), "precondition: the secret exists");

    // raw and percent-encoded traversal must be rejected/fail (Java APIs may normalize '..')
    assertEquals(404, get("/../secret.txt").statusCode());
    assertEquals(404, get("/%2e%2e/secret.txt").statusCode());

    // backslash variants
    assertEquals(404, get("/%5c..%5csecret.txt").statusCode());
    assertEquals(404, get("/..%5csecret.txt").statusCode());

    // nested escape (a valid prefix does not soften the guard)
    assertEquals(404, get("/sub/%2e%2e/%2e%2e/secret.txt").statusCode());

    // absolute paths (must never be served)
    assertEquals(404, get(("/" + outside).replace('\\', '/')).statusCode());
    assertEquals(404, get("/etc/passwd").statusCode());
  }

  @Test
  @DisplayName("unparseable names are rejected not errors")
  public void unparseableNamesAreRejectedNotErrors() throws Exception {
    for (String path : UNPARSEABLE_PATHS) {
      int status = get(path).statusCode();
      assertTrue(status == 404 || status == 400, path + " -> " + status);
    }
  }

  @Test
  @DisplayName("symlink inside the root cannot escape it")
  public void symlinkInsideTheRootCannotEscapeIt() throws Exception {
    try {
      Files.createSymbolicLink(root.resolve("escape.txt"), outside);
      Files.createSymbolicLink(root.resolve("escapedir"), outside.getParent());
    } catch (IOException | UnsupportedOperationException e) {
      Assumptions.abort("cannot create symlinks here (Windows non-admin) - skipping: " + e);
    }

    // both symlinks point outside the content root so they should be rejected
    assertEquals(404, get("/escape.txt").statusCode());
    assertEquals(404, get("/escapedir/secret.txt").statusCode());
  }

  @Test
  @DisplayName("symlink staying inside the root still serves")
  public void symlinkStayingInsideTheRootStillServes() throws Exception {
    try {
      Files.createSymbolicLink(root.resolve("alias.js"), root.resolve("app.js"));
    } catch (IOException | UnsupportedOperationException e) {
      Assumptions.abort("cannot create symlinks here (Windows non-admin) - skipping: " + e);
    }

    HttpResponse<String> response = get("/alias.js");

    assertEquals(200, response.statusCode());
    assertEquals("console.log('x');", response.body());
  }

  @Test
  @DisplayName("write methods are rejected")
  public void writeMethodsAreRejected() throws Exception {
    URI uri = URI.create(server.getBaseUrl() + "index.html");
    HttpRequest httpRequest = HttpRequest.newBuilder(uri)
      .POST(HttpRequest.BodyPublishers.ofString("x"))
      .build();

    HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    assertEquals(405, response.statusCode());
  }

  // Firefox needs a refresh (must first load to get source, then reload to be able to break)
  @Test
  @DisplayName("first page refresh injects exactly once")
  public void firstPageRefreshInjectsExactlyOnce() throws Exception {
    server.refreshFirstPage(2);
    String first = get("/index.html").body();
    assertTrue(first.contains("<meta http-equiv=\"refresh\" content=\"2\">"), "meta refresh injected into the first response: " + first);
    assertTrue(first.contains("hello"), "original content preserved");

    String second = get("/index.html").body();
    assertEquals("<html>hello</html>", second, "second response is served clean");

    // scripts are never touched by the injection
    assertEquals("console.log('x');", get("/app.js").body());
  }

  @Test
  @DisplayName("head has no body")
  public void headHasNoBody() throws Exception {
    HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(server.getBaseUrl() + "index.html"))
      .method("HEAD", HttpRequest.BodyPublishers.noBody())
      .build();

    HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertEquals("", response.body());
  }

  private HttpResponse<String> get(String path) throws Exception {
    // strip one trailing slash so baseUrl + path never doubles it
    URI uri = URI.create(server.getBaseUrl().replaceAll("/$", "") + path);
    return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
  }
}
