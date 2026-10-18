package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The offset the server must be asked at for an offset in the editor's text.
 * The editor holds one LF per line break, while a file with CRLF breaks on
 * disk is one character longer per line before the offset; the server reads
 * that file unless the request carries the text itself.
 */
final class HaxeServerOffsets {

  private static final String CRLF = "\r\n";

  private HaxeServerOffsets() {
  }

  /** Whether requests on a file with that detected line separator shift their offsets: no text is sent and the disk file is CRLF. */
  static boolean shiftsByLine(@Nullable String detectedLineSeparator, @Nullable String contents) {
    return contents == null && CRLF.equals(detectedLineSeparator);
  }

  /** The server's offset for an editor offset on a 0-based line. */
  static int serverOffset(int offset, int line, boolean shiftsByLine) {
    return shiftsByLine ? offset + line : offset;
  }

  /** The server's offset for an editor offset in the file, {@code contents} being the text the request sends, if any. Read action. */
  static int serverOffset(@NotNull VirtualFile file, int offset, @Nullable String contents) {
    if (!shiftsByLine(file.getDetectedLineSeparator(), contents)) return offset;
    Document document = FileDocumentManager.getInstance().getDocument(file);
    return document == null ? offset : serverOffset(offset, document.getLineNumber(offset), true);
  }
}
