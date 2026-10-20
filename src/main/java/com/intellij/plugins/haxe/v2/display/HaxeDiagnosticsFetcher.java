package com.intellij.plugins.haxe.v2.display;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The part the compiler-diagnostics annotators share: deciding whether a
 * request can be sent, fetching {@code display/diagnostics}, and converting
 * the compiler's ranges to document ranges. Every annotator (errors, unused
 * imports, removable code) collects through {@link #collect} and fetches
 * through {@link #fetch}.
 *
 * The cache keeps the LAST KNOWN diagnostics per file, with the document
 * state they describe. Within its time to live, one highlighting pass sends
 * one request however many annotators use the result. It also survives
 * transient fetch failures, such as a restarting server or a refused socket:
 * a re-highlight of the same text still shows the last known diagnostics.
 * After an edit they are not reused, since their line and column positions
 * would land on the wrong text.
 */
@CustomLog
final class HaxeDiagnosticsFetcher {

  /** The inputs of one fetch, collected under the read lock; the fetch itself runs without the lock. */
  record Request(@NotNull HaxeCompilerDisplayService.DisplayContext context,
                 @NotNull HaxeCompilerDisplayService service,
                 @NotNull String filePath,
                 @Nullable String contents,
                 long documentStamp) {
  }

  private record CacheEntry(long documentStamp, long timestampMillis, List<Diagnostic> diagnostics) {
  }

  /**
   * Last known diagnostics per file path, one map per PROJECT: a file open in
   * two projects must not render the other project's diagnostics, and one
   * project's cache clear must not wipe the others. Entries outlive the TTL
   * as the fallback for transient failures; the service is disposed with its
   * project.
   */
  @Service(Service.Level.PROJECT)
  static final class Cache {
    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @NotNull
    static Cache getInstance(@NotNull Project project) {
      return project.getService(Cache.class);
    }
  }

  /** Long enough to span one daemon pass over all annotators, short enough to never serve a stale edit. */
  private static final long CACHE_TTL_MILLIS = 5_000;

  private static final int CACHE_MAX_FILES = 200;

  private HaxeDiagnosticsFetcher() {
  }

  /** The request for this file, or null when compiler diagnostics cannot run for it. The caller checks the feature toggles. */
  @Nullable
  static Request collect(@NotNull PsiFile file, @NotNull Editor editor) {
    return collect(file, editor.getDocument());
  }

  /** Batch (Inspect Code) entry without an editor. The file's document still decides whether unsaved contents are sent. */
  @Nullable
  static Request collect(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    return document == null ? null : collect(file, document);
  }

  @Nullable
  private static Request collect(@NotNull PsiFile file, @NotNull Document document) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || !virtualFile.isInLocalFileSystem()) return null;

    // no request while the text does not parse: the compiler would fail on
    // the same syntax, and the parser's own error highlighting covers it
    if (!HaxeCompilerDisplayService.isSyntaxClean(file.getProject(), virtualFile)) {
      if (log.isDebugEnabled()) {
        log.debug("no display/diagnostics for " + virtualFile.getPath() + ": the file has parse errors");
      }
      return null;
    }

    HaxeCompilerDisplayService service = HaxeCompilerDisplayService.getInstance(file.getProject());
    HaxeCompilerDisplayService.DisplayContext context = service.contextFor(virtualFile);
    if (context == null) {
      if (log.isDebugEnabled()) log.debug("no display/diagnostics for " + virtualFile.getPath() + ": no build context");
      return null;
    }

    String contents = HaxeCompilerDisplayService.unsavedContents(virtualFile);
    return new Request(context, service, virtualFile.getPath(), contents, document.getModificationStamp());
  }

  /**
   * The file's diagnostics, fetched once per file and buffer state and shared
   * by the annotators of one pass. Only a call that fills the cache also
   * requests the whole-project diagnostics and updates the problem marks in
   * the Project view.
   */
  @Nullable
  static List<Diagnostic> fetch(@NotNull Request request) {
    Map<String, CacheEntry> cache = Cache.getInstance(request.service().getProject()).entries;
    String key = request.filePath();
    CacheEntry cached = cache.get(key);
    boolean sameText = cached != null && cached.documentStamp() == request.documentStamp();
    boolean fresh = sameText && System.currentTimeMillis() - cached.timestampMillis() < CACHE_TTL_MILLIS;
    if (fresh) {
      return cached.diagnostics();
    }

    List<FileDiagnostics> results =
      request.service().diagnostics(request.context(), request.filePath(), request.contents());
    if (results == null) {
      // a transient failure (server restarting, a refused socket): the same
      // text keeps its last known diagnostics instead of losing the
      // highlights; edited text gets none until a request succeeds
      return sameText ? cached.diagnostics() : null;
    }

    // only the whole-project request reports errors in OTHER files (the
    // per-file request stays silent about broken dependencies). Both feed
    // the problem marks in the Project view, which are not editor annotations.
    List<FileDiagnostics> projectResults = request.service().projectDiagnostics(request.context());
    HaxeCompilerProblemMarker.getInstance(request.service().getProject())
      .updateFromDiagnostics(request.filePath(), results, projectResults);

    List<Diagnostic> diagnostics = results.stream()
      .filter(entry -> FileUtil.pathsEqual(entry.file(), request.filePath()))
      .flatMap(entry -> entry.diagnostics().stream())
      .toList();
    if (log.isDebugEnabled()) {
      log.debug("display/diagnostics for " + key + ": " + diagnostics.size() + " of " + countOf(results)
                + " diagnostics belong to the file");
    }
    if (cache.size() >= CACHE_MAX_FILES) {
      evictOldest(cache);
    }
    cache.put(key, new CacheEntry(request.documentStamp(), System.currentTimeMillis(), diagnostics));
    return diagnostics;
  }

  private static int countOf(@NotNull List<FileDiagnostics> results) {
    return results.stream().mapToInt(entry -> entry.diagnostics().size()).sum();
  }

  static void clearCache(@NotNull Project project) {
    Cache.getInstance(project).entries.clear();
  }

  private static void evictOldest(@NotNull Map<String, CacheEntry> cache) {
    cache.entrySet().stream()
      .min(Map.Entry.comparingByValue(Comparator.comparingLong(CacheEntry::timestampMillis)))
      .ifPresent(oldest -> cache.remove(oldest.getKey()));
  }

  /** The text a diagnostic is shown with: the compiler's message where it sends one, else a message for its kind. */
  @NotNull
  static String messageOf(@NotNull Diagnostic diagnostic) {
    return switch (diagnostic.kind()) {
      case UNRESOLVED_IDENTIFIER -> HaxeBundle.message("haxe.diagnostics.unresolved.identifier");
      case COMPILER_ERROR, PARSER_ERROR, DEPRECATION_WARNING -> orGeneric(diagnostic.messageArg());
      case MISSING_FIELDS -> HaxeBundle.message("haxe.diagnostics.missing.fields");
      default -> HaxeBundle.message("haxe.diagnostics.generic");
    };
  }

  @NotNull
  private static String orGeneric(@NotNull String message) {
    return message.isBlank() ? HaxeBundle.message("haxe.diagnostics.generic") : message;
  }

  @NotNull
  static HighlightSeverity severityOf(@NotNull Diagnostic diagnostic) {
    return switch (diagnostic.severity()) {
      case ERROR -> HighlightSeverity.ERROR;
      case WARNING -> HighlightSeverity.WARNING;
      case INFORMATION, HINT, UNKNOWN -> HighlightSeverity.WEAK_WARNING;
    };
  }

  /**
   * Converts a compiler range (0-based line and character) to a document
   * range. A position that no longer fits the document gives null rather
   * than a wrongly clamped range.
   */
  @Nullable
  static TextRange toTextRange(@NotNull Document document, @NotNull Range range) {
    Integer start = toOffset(document, range.start());
    Integer end = toOffset(document, range.end());
    if (start == null || end == null || start > end) return null;
    return new TextRange(start, end);
  }

  @Nullable
  private static Integer toOffset(@NotNull Document document, @NotNull Position position) {
    if (position.line() < 0 || position.line() >= document.getLineCount()) return null;
    int lineStart = document.getLineStartOffset(position.line());
    int lineEnd = document.getLineEndOffset(position.line());
    int offset = lineStart + position.character();
    return offset <= lineEnd ? offset : null;
  }
}
