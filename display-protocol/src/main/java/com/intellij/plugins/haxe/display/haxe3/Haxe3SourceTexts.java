package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The texts one request's offset and results count in: the request's unsaved
 * buffer for its own file, each other file as it is on disk, read once. A
 * relative path counts from the request's {@code --cwd}.
 */
final class Haxe3SourceTexts {

  private static final String CWD_FLAG = "--cwd";

  private final Path workDirectory;
  private final Path requestFile;
  private final String requestContents;
  private final Map<Path, Optional<Haxe3SourceText>> texts = new HashMap<>();

  private Haxe3SourceTexts(Path workDirectory, Path requestFile, String requestContents) {
    this.workDirectory = workDirectory;
    this.requestFile = requestFile;
    this.requestContents = requestContents;
  }

  static Haxe3SourceTexts forRequest(List<String> baseArgs, String file, String contents) {
    Path workDirectory = workDirectoryOf(baseArgs);
    return new Haxe3SourceTexts(workDirectory, resolve(workDirectory, file), contents);
  }

  /** The texts of a request about no file in particular: every file as it is on disk. */
  static Haxe3SourceTexts forProject(List<String> baseArgs) {
    return new Haxe3SourceTexts(workDirectoryOf(baseArgs), null, null);
  }

  /** The text the server reads for the request's file. */
  Haxe3SourceText requestText() throws DisplayRequestException {
    if (requestContents != null) return Haxe3SourceText.of(requestContents);
    Haxe3SourceText text = requestFile == null ? null : textAt(requestFile);
    if (text == null) throw new DisplayRequestException("Cannot read the display file " + requestFile);
    return text;
  }

  /** The text of a file a result names; null when it cannot be read. */
  Haxe3SourceText textOf(String file) {
    Path path = resolve(workDirectory, file);
    if (path == null) return null;
    if (requestContents != null && path.equals(requestFile)) return Haxe3SourceText.of(requestContents);
    return textAt(path);
  }

  private Haxe3SourceText textAt(Path path) {
    return texts.computeIfAbsent(path, Haxe3SourceTexts::read).orElse(null);
  }

  private static Optional<Haxe3SourceText> read(Path path) {
    try {
      return Optional.of(Haxe3SourceText.read(path));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /** The last {@code --cwd} of the arguments; null without one. */
  private static Path workDirectoryOf(List<String> baseArgs) {
    int flag = baseArgs.lastIndexOf(CWD_FLAG);
    boolean hasValue = flag >= 0 && flag + 1 < baseArgs.size();
    return hasValue ? resolve(null, baseArgs.get(flag + 1)) : null;
  }

  /** The absolute, normalized path; null when the text is no path. */
  private static Path resolve(Path base, String file) {
    try {
      Path path = Path.of(file);
      Path absolute = base != null && !path.isAbsolute() ? base.resolve(path) : path.toAbsolutePath();
      return absolute.normalize();
    } catch (InvalidPathException e) {
      return null;
    }
  }
}
