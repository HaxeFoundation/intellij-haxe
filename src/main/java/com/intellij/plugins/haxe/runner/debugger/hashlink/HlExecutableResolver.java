package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Locates the HashLink executable for the (experimental) HashLink run/debug
 * support, in order:
 * <ol>
 *   <li>the path configured on the Haxe SDK ("HashLink executable")</li>
 *   <li>environment variables {@code HASHLINK_BIN}, {@code HASHLINK},
 *       {@code HASHLINKPATH} — each tried as the executable itself, then as a
 *       directory containing it</li>
 *   <li>directories on {@code PATH}</li>
 * </ol>
 * The environment is injectable so the precedence is unit-testable.
 */
public final class HlExecutableResolver {
  private static final String[] ENV_VARIABLES = {"HASHLINK_BIN", "HASHLINK", "HASHLINKPATH"};
  private static final String[] EXECUTABLE_NAMES = {"hl.exe", "hl"};

  /** Environment lookup, replaceable in tests. */
  @FunctionalInterface
  public interface Environment {
    @Nullable String get(String name);
  }

  private HlExecutableResolver() {
  }

  /** Resolves for a module, reading the SDK-configured path first. */
  public static Optional<Path> resolve(@Nullable Module module) {
    return resolve(sdkHlBinPath(module), System::getenv);
  }

  /** Testable core: explicit SDK path, then environment, then PATH. */
  public static Optional<Path> resolve(@Nullable String sdkHlBinPath, Environment env) {
    if (sdkHlBinPath != null && !sdkHlBinPath.isBlank()) {
      Optional<Path> fromSdk = asExecutableOrContainingDirectory(sdkHlBinPath);
      if (fromSdk.isPresent()) {
        return fromSdk;
      }
    }

    for (String variable : ENV_VARIABLES) {
      String value = env.get(variable);
      if (value != null && !value.isBlank()) {
        Optional<Path> fromEnv = asExecutableOrContainingDirectory(value);
        if (fromEnv.isPresent()) {
          return fromEnv;
        }
      }
    }

    String pathVariable = env.get("PATH");
    if (pathVariable != null) {
      for (String entry : pathVariable.split(File.pathSeparator)) {
        if (entry.isBlank()) {
          continue;
        }
        Optional<Path> fromPath = findInDirectory(entry);
        if (fromPath.isPresent()) {
          return fromPath;
        }
      }
    }
    return Optional.empty();
  }

  private static @Nullable String sdkHlBinPath(@Nullable Module module) {
    if (module == null) {
      return null;
    }
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    if (sdk != null && sdk.getSdkAdditionalData() instanceof HaxeSdkData data) {
      return data.getHlBinPath();
    }
    return null;
  }

  private static Optional<Path> asExecutableOrContainingDirectory(String value) {
    try {
      Path path = Path.of(value);
      if (Files.isRegularFile(path)) {
        return Optional.of(path);
      }
      if (Files.isDirectory(path)) {
        return findInDirectory(value);
      }
    } catch (InvalidPathException ignored) {
      // malformed entry; keep looking
    }
    return Optional.empty();
  }

  private static Optional<Path> findInDirectory(String directory) {
    try {
      for (String name : EXECUTABLE_NAMES) {
        Path candidate = Path.of(directory, name);
        if (Files.isRegularFile(candidate)) {
          return Optional.of(candidate);
        }
      }
    } catch (InvalidPathException ignored) {
      // malformed entry; keep looking
    }
    return Optional.empty();
  }
}
