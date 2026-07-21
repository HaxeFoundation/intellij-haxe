package com.intellij.plugins.haxe.matrix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * Downloads and extracts the manifest toolchains into
 * {@code <repo>/debuggerResources/{haxe,hashlink}/<name>/}, and DISCOVERS any
 * extra directories dropped there by hand (nightlies, linux HashLink builds
 * from source, ...). A version is provisioned once — the marker file makes
 * re-runs free; delete the version directory to force a re-download.
 */
final class Provisioner {
  private final Path resources;
  private final Log log;
  private final HttpClient http = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .connectTimeout(Duration.ofSeconds(30))
    .build();

  Provisioner(Path resources, Log log) {
    this.resources = resources;
    this.log = log;
  }

  /** Provisioned haxe version directories (each contains haxe + std). */
  List<Path> haxeDirs() throws IOException {
    return ensureAll("haxe", VersionManifest.haxeVersions(), "haxe");
  }

  /** Provisioned HashLink runtime directories (each contains hl). */
  List<Path> hashlinkDirs() throws IOException {
    return ensureAll("hashlink", VersionManifest.hashlinkVersions(), "hl");
  }

  private List<Path> ensureAll(String kind, List<VersionManifest.Tool> tools, String binary) throws IOException {
    Path base = resources.resolve(kind);
    Files.createDirectories(base);
    List<Path> dirs = new ArrayList<>();
    for (VersionManifest.Tool tool : tools) {
      Path dir = base.resolve(tool.name());
      if (Files.isRegularFile(dir.resolve(".provisioned")) && Platform.findBinary(dir, binary) != null) {
        dirs.add(dir);
        continue;
      }
      if (!tool.downloadable()) {
        if (Platform.findBinary(dir, binary) != null) {
          dirs.add(dir); // provided by hand (e.g. a linux HashLink built from source)
        } else {
          log.line(kind + " " + tool.name() + " : no release binary for this OS - provide it manually under "
                   + dir + " (skipped)");
        }
        continue;
      }
      log.line(kind + " " + tool.name() + " : downloading " + tool.url());
      try {
        downloadAndExtract(tool.url(), dir);
        if (Platform.findBinary(dir, binary) == null) {
          log.line(kind + " " + tool.name() + " : extracted but no " + binary + " binary found - skipped");
          continue;
        }
        Files.writeString(dir.resolve(".provisioned"), tool.url());
        dirs.add(dir);
      } catch (Exception e) {
        log.line(kind + " " + tool.name() + " : provisioning FAILED (" + e.getMessage() + ") - skipped");
      }
    }
    // hand-dropped extras (not in the manifest) join the matrix by discovery
    try (var children = Files.list(base)) {
      for (Path dir : children.filter(Files::isDirectory).sorted().toList()) {
        if (dirs.contains(dir)) {
          continue;
        }
        if (Platform.findBinary(dir, binary) != null) {
          log.line(kind + " " + dir.getFileName() + " : discovered (manually provided)");
          dirs.add(dir);
        }
      }
    }
    dirs.sort(null);
    return dirs;
  }

  private void downloadAndExtract(String url, Path dir) throws IOException, InterruptedException {
    if (Files.exists(dir)) {
      deleteRecursively(dir);
    }
    Files.createDirectories(dir);
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
      .timeout(Duration.ofMinutes(10)).GET().build();
    HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() != 200) {
      throw new IOException("HTTP " + response.statusCode() + " for " + url);
    }
    try (InputStream body = response.body()) {
      if (url.endsWith(".zip")) {
        extractZip(body, dir);
      } else if (url.endsWith(".tar.gz") || url.endsWith(".tgz")) {
        extractTarGz(body, dir);
      } else {
        throw new IOException("unsupported archive type: " + url);
      }
    }
    unwrapNestedArchives(dir, 2);
  }

  /**
   * CI artifacts are zips OF the uploaded files — and hashlink's CI uploads
   * a zip, so the download extracts to a single nested archive. Unwrap any
   * archives found at the top level (bounded depth; each nested archive is
   * extracted beside itself and deleted).
   */
  private void unwrapNestedArchives(Path dir, int depth) throws IOException {
    if (depth <= 0) {
      return;
    }
    List<Path> archives;
    try (var files = Files.list(dir)) {
      archives = files.filter(f -> {
        String name = f.getFileName().toString();
        return Files.isRegularFile(f)
               && (name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tgz"));
      }).toList();
    }
    for (Path archive : archives) {
      String name = archive.getFileName().toString();
      try (InputStream in = Files.newInputStream(archive)) {
        if (name.endsWith(".zip")) {
          extractZip(in, dir);
        } else {
          extractTarGz(in, dir);
        }
      }
      Files.delete(archive);
      unwrapNestedArchives(dir, depth - 1);
    }
  }

  private void extractZip(InputStream in, Path dir) throws IOException {
    try (ZipInputStream zip = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        Path target = safeResolve(dir, entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private void extractTarGz(InputStream in, Path dir) throws IOException {
    boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
      TarArchiveEntry entry;
      while ((entry = tar.getNextTarEntry()) != null) {
        Path target = safeResolve(dir, entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(target);
          continue;
        }
        if (entry.isSymbolicLink()) {
          Files.createDirectories(target.getParent());
          Files.deleteIfExists(target);
          Files.createSymbolicLink(target, Path.of(entry.getLinkName()));
          continue;
        }
        Files.createDirectories(target.getParent());
        Files.copy(tar, target, StandardCopyOption.REPLACE_EXISTING);
        if (posix && (entry.getMode() & 0100) != 0) {
          // preserve the executable bit the JDK zip/tar copy loses
          Set<PosixFilePermission> perms = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);
          Files.setPosixFilePermissions(target, perms);
        }
      }
    }
  }

  /** Zip-slip guard: an archive entry must stay inside the target dir. */
  private static Path safeResolve(Path dir, String entryName) throws IOException {
    Path target = dir.resolve(entryName).normalize();
    if (!target.startsWith(dir)) {
      throw new IOException("archive entry escapes target dir: " + entryName);
    }
    return target;
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (var walk = Files.walk(path)) {
      for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(p);
      }
    }
  }
}
