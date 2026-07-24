package com.intellij.plugins.haxe.runner.debugger.browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * Acquires the pinned debug-adapter artifacts on demand and caches them under
 * a local store directory ({@code <ide-system>/haxe/debug-adapters} in
 * production; any directory in tests). Layout per adapter:
 *
 * <pre>
 *   {@code <store>/<id>/<version>/}          the unpacked artifact
 *   {@code <store>/<id>/<version>.ok}       completion marker (written LAST)
 * </pre>
 *
 * Supply-chain rules enforced here (see {@link AdapterPin}): the download is
 * verified against the pin's SHA-256 BEFORE anything is unpacked, unpacking
 * rejects entries escaping the target directory (zip-slip), and the marker is
 * only written after a fully successful unpack — a torn earlier attempt is
 * re-done, never trusted. No code from the archive runs during acquisition.
 */
public final class AdapterStore {
  private final Path storeRoot;
  private final HttpClient httpClient;

  public AdapterStore(Path storeRoot) {
    this(storeRoot, HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(30))
      .build());
  }

  AdapterStore(Path storeRoot, HttpClient httpClient) {
    this.storeRoot = storeRoot;
    this.httpClient = httpClient;
  }

  /**
   * Whether the pinned artifact is already fully acquired in this store —
   * a read-only check (the run configuration UI shows the download state);
   * {@link #resolveEntry} remains the only acquisition path.
   */
  public boolean isInstalled(AdapterPin pin) {
    Path versionDir = storeRoot.resolve(pin.id()).resolve(pin.version());
    Path marker = storeRoot.resolve(pin.id()).resolve(pin.version() + ".ok");
    return Files.isRegularFile(marker) && Files.isRegularFile(versionDir.resolve(pin.entryRelativePath()));
  }

  /**
   * The adapter's entry-point file, downloading and unpacking the pinned
   * artifact on first use. {@code overrideDir} (a settings field: an already
   * unpacked artifact, the offline story) wins when it contains the entry.
   */
  public Path resolveEntry(AdapterPin pin, Path overrideDir) throws IOException {
    if (overrideDir != null) {
      Path overridden = overrideDir.resolve(pin.entryRelativePath());
      if (Files.isRegularFile(overridden)) {
        return overridden;
      }
      throw new IOException("The configured " + pin.id() + " adapter directory does not contain "
                            + pin.entryRelativePath() + ": " + overrideDir);
    }
    Path versionDir = storeRoot.resolve(pin.id()).resolve(pin.version());
    Path marker = storeRoot.resolve(pin.id()).resolve(pin.version() + ".ok");
    Path entry = versionDir.resolve(pin.entryRelativePath());
    if (Files.isRegularFile(marker) && Files.isRegularFile(entry)) {
      return entry;
    }
    acquire(pin, versionDir, marker);
    if (!Files.isRegularFile(entry)) {
      throw new IOException("The " + pin.id() + " " + pin.version() + " artifact unpacked without its entry point "
                            + pin.entryRelativePath() + " - the pin is wrong or the artifact changed shape");
    }
    return entry;
  }

  private void acquire(AdapterPin pin, Path versionDir, Path marker) throws IOException {
    Files.createDirectories(storeRoot.resolve(pin.id()));
    // download to a temp file first; nothing is unpacked before the hash check
    Path download = Files.createTempFile(storeRoot.resolve(pin.id()), "download-", ".tmp");
    try {
      fetch(pin.url(), download);
      String actual = sha256(download);
      if (!actual.equalsIgnoreCase(pin.sha256())) {
        throw new IOException("SHA-256 mismatch for " + pin.url() + "\n  expected " + pin.sha256()
                              + "\n  actual   " + actual
                              + "\nRefusing the artifact (supply-chain protection).");
      }
      // a torn previous attempt (no marker) is discarded wholesale
      deleteRecursively(versionDir);
      if (pin.isTarGz()) {
        untarGz(download, versionDir);
      } else {
        unzip(download, versionDir);
      }
      Files.writeString(marker, pin.sha256()); // written LAST: presence == complete
    } finally {
      Files.deleteIfExists(download);
    }
  }

  private void fetch(String url, Path target) throws IOException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
      .timeout(Duration.ofMinutes(5))
      .GET()
      .build();
    try {
      HttpResponse<Path> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
      if (response.statusCode() != 200) {
        throw new IOException("Download of " + url + " failed with HTTP " + response.statusCode());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while downloading " + url, e);
    }
  }

  private static String sha256(Path file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JRE without SHA-256", e);
    }
    try (InputStream in = Files.newInputStream(file)) {
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = in.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  // The .vsix artifacts are plain zips. Entries are constrained to the target
  // directory (zip-slip) and only regular files/directories are materialized.
  private static void unzip(Path archive, Path targetDir) throws IOException {
    Files.createDirectories(targetDir);
    Path target = targetDir.toRealPath();
    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        Path resolved = target.resolve(entry.getName()).normalize();
        if (!resolved.startsWith(target)) {
          throw new IOException("Archive entry escapes the target directory (zip-slip): " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else {
          Files.createDirectories(resolved.getParent());
          Files.copy(zip, resolved, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  /**
   * Gzipped-tar extraction (the js-debug-dap artifact) via commons-compress,
   * which the IDE platform bundles. Only regular files and directories are
   * materialized; anything else (links, devices) is rejected — a debug-adapter
   * artifact has no business containing them. Same escape guard as the zip path.
   */
  private static void untarGz(Path archive, Path targetDir) throws IOException {
    Files.createDirectories(targetDir);
    Path target = targetDir.toRealPath();
    try (TarArchiveInputStream tar = new TarArchiveInputStream(
      new GzipCompressorInputStream(Files.newInputStream(archive)))) {
      TarArchiveEntry entry;
      while ((entry = tar.getNextEntry()) != null) {
        Path resolved = target.resolve(entry.getName()).normalize();
        if (!resolved.startsWith(target)) {
          throw new IOException("Archive entry escapes the target directory (tar-slip): " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else if (entry.isFile()) {
          Files.createDirectories(resolved.getParent());
          Files.copy(tar, resolved, StandardCopyOption.REPLACE_EXISTING);
        } else {
          throw new IOException("Unsupported tar entry type for " + entry.getName() + " - refusing the artifact");
        }
      }
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (var walk = Files.walk(dir)) {
      var toDelete = walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
      for (Path path : toDelete) {
        Files.deleteIfExists(path);
      }
    }
  }
}
