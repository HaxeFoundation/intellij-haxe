package com.intellij.plugins.haxe.runner.debugger.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Exercises the store against a LOCAL http server (the module's own
 * ContentHttpServer, dogfooded) — no network, no real pins. The archive shape
 * mirrors the firefox vsix: a zip with extension/dist/adapter.bundle.js.
 */
public class AdapterStoreTest {
  private static final String ENTRY = "extension/dist/adapter.bundle.js";
  private static final String BUNDLE_CONTENT = "// fake adapter bundle";

  private Path www;
  private Path storeRoot;
  private ContentHttpServer server;
  private byte[] archive;
  private String archiveSha;

  @Before
  public void setUp() throws Exception {
    www = Files.createTempDirectory("adapter-store-www");
    storeRoot = Files.createTempDirectory("adapter-store");
    archive = zipWith(ENTRY, BUNDLE_CONTENT);
    Files.write(www.resolve("adapter.vsix"), archive);
    archiveSha = sha256(archive);
    server = new ContentHttpServer(www);
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.close();
    }
  }

  private AdapterPin pin(String sha) {
    return new AdapterPin("test-adapter", "1.0.0", server.getBaseUrl() + "adapter.vsix", sha, ENTRY);
  }

  @Test
  public void downloadsVerifiesUnpacksAndCaches() throws Exception {
    AdapterStore store = new AdapterStore(storeRoot);
    Path entry = store.resolveEntry(pin(archiveSha), null);
    assertTrue(Files.isRegularFile(entry));
    assertEquals(BUNDLE_CONTENT, Files.readString(entry));
    assertTrue("completion marker written",
               Files.isRegularFile(storeRoot.resolve("test-adapter").resolve("1.0.0.ok")));

    // second resolve is a pure cache hit: kill the server to prove no fetch
    server.close();
    Path again = new AdapterStore(storeRoot).resolveEntry(pin(archiveSha), null);
    assertEquals(entry, again);
  }

  @Test
  public void wrongHashRefusesTheArtifactAndCachesNothing() throws Exception {
    AdapterStore store = new AdapterStore(storeRoot);
    String wrongSha = "0".repeat(64);
    try {
      store.resolveEntry(pin(wrongSha), null);
      fail("expected the SHA-256 mismatch to refuse the artifact");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("SHA-256 mismatch"));
    }
    assertTrue("nothing unpacked",
               !Files.exists(storeRoot.resolve("test-adapter").resolve("1.0.0").resolve(ENTRY)));
    assertTrue("no marker", !Files.exists(storeRoot.resolve("test-adapter").resolve("1.0.0.ok")));
  }

  @Test
  public void tornPreviousAttemptIsRedone() throws Exception {
    // simulate a crash mid-unpack: version dir exists WITHOUT the marker
    Path versionDir = storeRoot.resolve("test-adapter").resolve("1.0.0");
    Files.createDirectories(versionDir.resolve("extension"));
    Files.writeString(versionDir.resolve("extension/leftover.txt"), "torn");

    Path entry = new AdapterStore(storeRoot).resolveEntry(pin(archiveSha), null);
    assertEquals(BUNDLE_CONTENT, Files.readString(entry));
    assertTrue("torn leftovers discarded", !Files.exists(versionDir.resolve("extension/leftover.txt")));
  }

  @Test
  public void overrideDirectoryWinsAndSkipsTheStore() throws Exception {
    Path override = Files.createTempDirectory("adapter-override");
    Path overrideEntry = override.resolve(ENTRY);
    Files.createDirectories(overrideEntry.getParent());
    Files.writeString(overrideEntry, "// user-provided bundle");
    server.close(); // no fetch may happen

    Path entry = new AdapterStore(storeRoot).resolveEntry(pin(archiveSha), override);
    assertEquals(overrideEntry, entry);
  }

  @Test
  public void emptyOverrideDirectoryFailsWithAClearMessage() throws Exception {
    Path override = Files.createTempDirectory("adapter-override-empty");
    try {
      new AdapterStore(storeRoot).resolveEntry(pin(archiveSha), override);
      fail("expected the empty override to be rejected");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("does not contain"));
    }
  }

  @Test
  public void zipSlipEntriesAreRejected() throws Exception {
    byte[] evil = zipWith("../escaped.txt", "evil");
    Files.write(www.resolve("adapter.vsix"), evil);
    try {
      new AdapterStore(storeRoot).resolveEntry(pin(sha256(evil)), null);
      fail("expected the zip-slip entry to be rejected");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("zip-slip"));
    }
    assertTrue("nothing escaped the store",
               !Files.exists(storeRoot.resolve("test-adapter").resolve("escaped.txt")));
  }

  @Test
  public void tarGzArtifactsUnpack() throws Exception {
    byte[] tarGz = tarGzWith("js-debug/src/dapDebugServer.js", "// fake dap server");
    Files.write(www.resolve("adapter.tar.gz"), tarGz);
    AdapterPin pin = new AdapterPin("test-tgz", "1.0.0", server.getBaseUrl() + "adapter.tar.gz",
                                    sha256(tarGz), "js-debug/src/dapDebugServer.js");
    Path entry = new AdapterStore(storeRoot).resolveEntry(pin, null);
    assertEquals("// fake dap server", Files.readString(entry));
  }

  @Test
  public void tarSlipEntriesAreRejected() throws Exception {
    byte[] evil = tarGzWith("../escaped.txt", "evil");
    Files.write(www.resolve("adapter.tar.gz"), evil);
    AdapterPin pin = new AdapterPin("test-tgz", "1.0.0", server.getBaseUrl() + "adapter.tar.gz",
                                    sha256(evil), "whatever.js");
    try {
      new AdapterStore(storeRoot).resolveEntry(pin, null);
      fail("expected the tar-slip entry to be rejected");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("tar-slip"));
    }
    assertTrue("nothing escaped the store",
               !Files.exists(storeRoot.resolve("test-tgz").resolve("escaped.txt")));
  }

  private static byte[] tarGzWith(String entryName, String content) throws IOException {
    byte[] data = content.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
      TarArchiveEntry entry = new TarArchiveEntry(entryName);
      entry.setSize(data.length);
      tar.putArchiveEntry(entry);
      tar.write(data);
      tar.closeArchiveEntry();
    }
    return bytes.toByteArray();
  }

  private static byte[] zipWith(String entryName, String content) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  private static String sha256(byte[] data) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
  }
}
