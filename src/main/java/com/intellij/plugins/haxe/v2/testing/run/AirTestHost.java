package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.ExecutionException;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeSystemPaths;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Hosts flash-family test runs under `adl -nodebug`: the AIR debug launcher
/// forwards NATIVE trace output (`flash.Lib.trace`) to its stdout and exits
/// with the code the app passes to NativeApplication.exit — the only flash
/// host with both. A plain `haxe -swf` artifact runs as AIR content
/// unmodified; all it needs is the minimal generated application descriptor.
/// adl's DEFAULT (debug-launch) mode swallows trace output, so `-nodebug` is
/// required for a run whose stdout is the test protocol. A DEBUG launch
/// keeps the default mode: the `-debug` swf dials the waiting fdb, which
/// then carries the traces on ITS console instead of adl's stdout.
final class AirTestHost {

  // the SDK's own version tag, e.g. <version>31.0.0</version>
  private static final Pattern SDK_VERSION = Pattern.compile("<version>\\s*(\\d+)\\.(\\d+)");

  /** Descriptor namespace for SDKs without a description file - old enough that every current runtime accepts it. */
  private static final String FALLBACK_NAMESPACE_VERSION = "28.0";

  private AirTestHost() {
  }

  /** {@code adl [-nodebug] <descriptor> <content root>} — the content root is the swf's own directory (see class doc for the modes). */
  @NotNull
  static List<String> command(@NotNull String adlExecutable,
                              @NotNull Path descriptor,
                              @NotNull Path contentRoot,
                              boolean debugLaunch) {
    return debugLaunch
           ? List.of(adlExecutable, descriptor.toString(), contentRoot.toString())
           : List.of(adlExecutable, "-nodebug", descriptor.toString(), contentRoot.toString());
  }

  /**
   * The generated application descriptor for the artifact, under the IDE
   * system directory (the build's output directory stays untouched). The
   * descriptor's {@code <content>} is the bare swf name, resolved against
   * the content root the command passes.
   */
  @NotNull
  static Path descriptorFor(@NotNull Path swfArtifact, @NotNull String namespaceVersion) throws ExecutionException {
    String artifactHash = HaxeSystemPaths.shortHash(swfArtifact.toString().getBytes(StandardCharsets.UTF_8));
    Path directory = HaxeSystemPaths.cacheDirectory("air-tests", artifactHash);
    Path descriptor = directory.resolve("application.xml");
    // AIR apps are single-instance per id: with a reused id a STRAY instance
    // (e.g. a debug player orphaned by a stopped fdb, waiting forever for a
    // debugger) swallows the next launch - "invocation forwarded to primary
    // instance", the fresh adl exits 0 and fdb waits on nothing. A per-LAUNCH
    // id makes every launch its own app.
    String launchId = artifactHash + "t" + System.currentTimeMillis();
    String content = """
      <?xml version="1.0" encoding="utf-8"?>
      <application xmlns="http://ns.adobe.com/air/application/%s">
        <id>com.intellij.haxe.tests.h%s</id>
        <versionNumber>1.0</versionNumber>
        <filename>HaxeTests</filename>
        <initialWindow>
          <content>%s</content>
          <visible>false</visible>
        </initialWindow>
      </application>
      """.formatted(namespaceVersion, launchId, swfArtifact.getFileName());
    try {
      Files.createDirectories(directory);
      Files.writeString(descriptor, content);
      return descriptor;
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeBundle.message("haxe.test.config.air.descriptor.failed", e.getMessage()));
    }
  }

  /**
   * The descriptor namespace version from the SDK's air-sdk-description.xml
   * (major.minor; the namespace must not exceed the runtime's version).
   */
  @NotNull
  static String namespaceVersion(@NotNull Path adlExecutable) {
    Path binDirectory = adlExecutable.getParent();
    Path sdkRoot = binDirectory == null ? null : binDirectory.getParent();
    if (sdkRoot != null) {
      try {
        String description = Files.readString(sdkRoot.resolve("air-sdk-description.xml"));
        Matcher matcher = SDK_VERSION.matcher(description);
        if (matcher.find()) {
          return matcher.group(1) + "." + matcher.group(2);
        }
      }
      catch (IOException ignored) {
      }
    }
    return FALLBACK_NAMESPACE_VERSION;
  }
}
