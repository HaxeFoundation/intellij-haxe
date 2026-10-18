package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.HoverInfo;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.ide.hint.types.HaxeTypeHintAnchors;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.Connected;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The compiler's type at a declaration, from one {@code display/hover} at the
 * declaration's name: parameters, local variables (switch captures included),
 * fields, enum extracted values, loop variables, and methods and local
 * functions, whose answer is their function type. A declaration with a type
 * tag already says what it is and is not asked.
 * <p>
 * Readers hold the read lock and get CACHE-ONLY answers ({@link #wireTypeOf});
 * nothing here waits for the server. A lookup that finds no answer, or one for
 * an older text of the file, queues the file for one batch: on a single
 * background thread the declarations without a current answer are collected
 * under a non-blocking read action ({@link #pendingBatch}), hovered
 * back-to-back on one connection without the lock ({@link #prefetch}), and
 * when answers were stored, highlighting restarts once so the passes read
 * them. The lookup is the trigger rather than a highlighting pass because the
 * daemon's external-tool pass skips library files, whose declarations are
 * evaluated like any other. The restarted pass finds every declaration
 * answered, so no further batch follows; an edit during a batch ends it early
 * and the next lookup queues the rest. The {@link Scope} a reader asks with
 * bounds the batch: a reader that takes class members from a bulk answer asks
 * for the local declarations only.
 * <p>
 * A hover answer is accepted only when its range is the declaration's name,
 * as the compiler reports the smallest expression at the position, which may
 * be a different one than asked for. A stale answer keeps serving until a
 * newer one replaces it, so a declaration never flips back while the file is
 * edited. A compiler error answers its one declaration with nothing and the
 * batch goes on, as the compiler fails some positions on their own; only when
 * the first few requests all fail - as every request on a build context that
 * does not compile does - is the file suspended until its text or the build
 * context changes. A transport failure is retried by the next lookup.
 * <p>
 * TODO: a local hovered at its declaration shows the monomorphs still unbound
 * at that point ({@code Array<?>} for {@code var a = []}); the bound type
 * comes from hovering its last reference in scope.
 * TODO: the answers hold their declarations' PSI strongly, which keeps every
 * hovered file's tree in memory until the caches clear.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerHoverTypes implements Disposable {

  /** Which declarations a batch asks about. */
  public enum Scope {
    /** Those inside a function body: variables, captures, extracted values, loop variables, local functions and their parameters. */
    LOCALS,
    /** Every covered declaration, class members included. */
    ALL
  }

  /**
   * What the compiler said about a declaration for one text of its file: the
   * accepted type, or null for a rejected or empty answer. The text is the
   * document's modification stamp ({@link #textStampOf}); the PSI file's own
   * stamp is not comparable to it, as it reports the last committed text.
   */
  private record Answer(long documentStamp, @Nullable JsonTypeRef wireType) {
  }

  /** Where to hover for a declaration (offset and line of its name) and the range a valid answer must report. */
  record Target(@NotNull PsiElement element, int offset, int line, @NotNull Range expectedRange) {
  }

  /** One file's declarations to ask about, with everything the requests need from the read lock. */
  record Batch(@NotNull VirtualFile file,
               @NotNull Document document,
               @NotNull DisplayContext context,
               @Nullable String contents,
               long documentStamp,
               boolean shiftsByLine,
               @NotNull List<Target> targets) {

    /** Whether the file was edited since the batch was collected; a volatile read, so it needs no lock. */
    boolean isStale() {
      return document.getModificationStamp() != documentStamp;
    }
  }

  /** The document and build-context state under which requests on a file failed. */
  private record FailureMark(long documentStamp, long inputsStamp) {
  }

  /** Compiler errors without a single answer after which a batch suspends its file. */
  private static final int ERRORS_BEFORE_SUSPENDING = 3;

  private final Project project;
  private final Map<PsiElement, Answer> answers = new ConcurrentHashMap<>();
  private final Map<VirtualFile, FailureMark> failedFiles = new ConcurrentHashMap<>();
  /** Files with a batch queued or running; further misses on them wait for it. */
  private final Set<VirtualFile> queuedFiles = ConcurrentHashMap.newKeySet();
  private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Haxe compiler hover types", 1);

  private final AtomicLong lookups = new AtomicLong();
  private final AtomicLong cacheHits = new AtomicLong();
  private final AtomicLong batches = new AtomicLong();
  private final AtomicLong batchTargets = new AtomicLong();
  private final AtomicLong hovers = new AtomicLong();
  private final AtomicLong failures = new AtomicLong();
  private final AtomicLong rejectedRanges = new AtomicLong();
  private final AtomicLong hoverMillis = new AtomicLong();
  private final AtomicLong maxHoverMillis = new AtomicLong();

  public HaxeCompilerHoverTypes(@NotNull Project project) {
    this.project = project;
  }

  public static HaxeCompilerHoverTypes getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerHoverTypes.class);
  }

  @Override
  public void dispose() {
    executor.shutdownNow();
  }

  /** Whether the hover answers for the declaration: a covered kind with a real name and without a type tag; a constructor has no type to show. */
  public static boolean covers(@NotNull PsiElement declaration) {
    HaxeComponentName name = HaxeTypeHintAnchors.componentNameOf(declaration);
    return name != null && !isWildcard(name) && !hasTypeTag(declaration) && !isConstructor(declaration);
  }

  /** Whether the declaration sits in a function body rather than on the class. */
  public static boolean isLocal(@NotNull PsiElement declaration) {
    return switch (declaration) {
      case HaxeFieldDeclaration _ -> false;
      case HaxeMethodDeclaration _ -> false;
      case HaxeParameter parameter -> !(functionOf(parameter) instanceof HaxeMethodDeclaration);
      default -> true;
    };
  }

  /**
   * The compiler's type at the declaration, or null while there is no answer,
   * the answer was about something else, or the compiler reported nothing. A
   * declaration without an answer for its file's current text queues the file
   * for a batch of the scope. Read action; never waits.
   */
  @Nullable
  public JsonTypeRef wireTypeOf(@NotNull PsiElement declaration, @NotNull Scope scope) {
    lookups.incrementAndGet();
    Answer answer = answers.get(declaration);
    PsiFile file = declaration.getContainingFile();
    boolean current = answer != null && file != null && answer.documentStamp() == textStampOf(file);
    if (file != null && !current) {
      queueBatch(file, scope);
    }
    if (answer == null) return null;
    cacheHits.incrementAndGet();
    return answer.wireType();
  }

  // --- batches: queued by a lookup, collected under a read action, run off it ---

  /** Queues one batch for the file unless one is queued already; the batch asks about every declaration of the scope without a current answer. */
  private void queueBatch(@NotNull PsiFile psiFile, @NotNull Scope scope) {
    // fixture tests have no server to ask; never spawn the attempt
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    VirtualFile file = HaxeCompilerDisplayService.physicalFileOf(psiFile);
    if (file == null || !queuedFiles.add(file)) return;
    executor.execute(() -> runBatch(file, scope));
  }

  private void runBatch(@NotNull VirtualFile file, @NotNull Scope scope) {
    int stored;
    try {
      stored = project.isDisposed() ? 0 : collectAndPrefetch(file, scope);
    } finally {
      queuedFiles.remove(file);
    }
    if (stored > 0) {
      HaxeCompilerCaches.dropPsiCachesAndRestartLater(project, "haxe: compiler hover types landed");
    }
  }

  private int collectAndPrefetch(@NotNull VirtualFile file, @NotNull Scope scope) {
    Batch batch = ReadAction.nonBlocking(() -> pendingBatch(file, scope))
      .withDocumentsCommitted(project)
      .executeSynchronously();
    return batch == null ? 0 : prefetch(batch);
  }

  /**
   * The declarations of the scope in the file that have no answer for its
   * current text, or null when nothing is pending or the compiler cannot
   * serve the file. Read action.
   */
  @Nullable
  Batch pendingBatch(@NotNull VirtualFile file, @NotNull Scope scope) {
    if (DumbService.isDumb(project)) return null;
    if (!HaxeCompilerFeatureAvailability.isAvailable(project, file)) return null;
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) return null;
    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return null;
    long stamp = document.getModificationStamp();
    if (isSuspended(file, stamp)) return null;
    DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(file);
    if (context == null) return null;

    List<Target> pending = new ArrayList<>();
    for (Target target : targetsOf(psiFile, document, scope)) {
      Answer answer = answers.get(target.element());
      if (answer == null || answer.documentStamp() != stamp) pending.add(target);
    }
    if (pending.isEmpty()) return null;
    String contents = HaxeCompilerDisplayService.unsavedContents(file);
    boolean shiftsByLine = HaxeServerOffsets.shiftsByLine(file.getDetectedLineSeparator(), contents);
    return new Batch(file, document, context, contents, stamp, shiftsByLine, pending);
  }

  /** The stamp of the file's current text: its document's, or the file's own while no document exists. */
  private long textStampOf(@NotNull PsiFile file) {
    Document document = PsiDocumentManager.getInstance(project).getCachedDocument(file);
    return document != null ? document.getModificationStamp() : file.getModificationStamp();
  }

  /** Every covered declaration of the scope that has a name, in document order. Read action. */
  @NotNull
  static List<Target> targetsOf(@NotNull PsiFile file, @NotNull Document document, @NotNull Scope scope) {
    Collection<PsiElement> declarations = PsiTreeUtil.findChildrenOfAnyType(
      file, HaxeParameter.class, HaxeLocalVarDeclaration.class, HaxeSwitchCaseCapture.class, HaxeFieldDeclaration.class,
      HaxeEnumExtractedValueReference.class, HaxeMethodDeclaration.class, HaxeLocalFunctionDeclaration.class,
      HaxeValueIterator.class, HaxeIteratorkey.class, HaxeIteratorValue.class);
    List<Target> targets = new ArrayList<>();
    for (PsiElement declaration : declarations) {
      if (!covers(declaration)) continue;
      if (scope == Scope.LOCALS && !isLocal(declaration)) continue;
      Target target = targetOf(declaration, document);
      if (target != null) targets.add(target);
    }
    return targets;
  }

  /**
   * Hovers every target of the batch back-to-back on one connection
   * ({@link #hoverEach}); the number of answers stored. Background thread, no
   * read lock.
   */
  int prefetch(@NotNull Batch batch) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    Connected connected = displayService.connectFor(batch.context(), DisplayMethods.HOVER);
    if (connected == null) return 0;
    batches.incrementAndGet();
    batchTargets.addAndGet(batch.targets().size());
    String path = batch.file().getPath();
    try {
      displayService.prepareFile(connected, path, batch.contents());
      return hoverEach(batch, target -> hoverAt(connected, batch, target));
    } catch (DisplayRequestException e) {
      failures.incrementAndGet();
      log.info("display/hover failed for " + path + ": " + e.getMessage());
      if (e.isCompilerAnswered()) markFailed(batch.file(), batch.documentStamp(), e.getMessage());
      return 0;
    } finally {
      if (log.isDebugEnabled()) log.debug(statsSummary());
    }
  }

  /**
   * Hovers the batch's targets in order and stores each answer; the number
   * stored. A compiler error answers its one target with nothing and the
   * batch goes on - the compiler fails some positions on their own. When the
   * first {@value #ERRORS_BEFORE_SUSPENDING} requests all fail, the batch stops
   * and the file is suspended ({@link #isSuspended}). Stops at an edit of the
   * file (the stored answers stay, the next lookup queues the rest) and at a
   * transport failure (the next lookup retries).
   */
  int hoverEach(@NotNull Batch batch, @NotNull TargetHover hover) {
    int stored = 0;
    int answered = 0;
    int failed = 0;
    String lastError = null;
    for (Target target : batch.targets()) {
      if (batch.isStale() || project.isDisposed()) break;
      HoverInfo info = null;
      try {
        info = hover.at(target);
        answered++;
      } catch (DisplayRequestException e) {
        failures.incrementAndGet();
        if (!e.isCompilerAnswered()) {
          log.info("display/hover failed for " + batch.file().getPath() + ": " + e.getMessage());
          return stored;
        }
        failed++;
        lastError = e.getMessage();
        if (log.isDebugEnabled()) log.debug("display/hover failed at " + batch.file().getPath() + ":" + (target.line() + 1) + ": " + lastError);
      }
      answers.put(target.element(), new Answer(batch.documentStamp(), acceptedType(target, info)));
      stored++;
      if (answered == 0 && failed >= ERRORS_BEFORE_SUSPENDING) break;
    }
    if (answered == 0 && lastError != null) markFailed(batch.file(), batch.documentStamp(), lastError);
    return stored;
  }

  @Nullable
  private HoverInfo hoverAt(@NotNull Connected connected, @NotNull Batch batch, @NotNull Target target) throws DisplayRequestException {
    int offset = HaxeServerOffsets.serverOffset(target.offset(), target.line(), batch.shiftsByLine());
    return timed(() -> connected.client().hover(connected.args(), batch.file().getPath(), offset, batch.contents()));
  }

  /** One hover request for a target; null when the compiler reports nothing there. */
  @FunctionalInterface
  interface TargetHover {
    @Nullable
    HoverInfo at(@NotNull Target target) throws DisplayRequestException;
  }

  @FunctionalInterface
  private interface Hover {
    @Nullable HoverInfo send() throws DisplayRequestException;
  }

  @Nullable
  private HoverInfo timed(@NotNull Hover hover) throws DisplayRequestException {
    long started = System.currentTimeMillis();
    try {
      return hover.send();
    } finally {
      long millis = System.currentTimeMillis() - started;
      hovers.incrementAndGet();
      hoverMillis.addAndGet(millis);
      maxHoverMillis.accumulateAndGet(millis, Math::max);
    }
  }

  /** The hovered type when the answer is about the target's name; null (counted) when it is about something else or nothing. */
  @Nullable
  private JsonTypeRef acceptedType(@NotNull Target target, @Nullable HoverInfo hover) {
    if (hover == null) return null;
    if (!target.expectedRange().equals(hover.range())) {
      rejectedRanges.incrementAndGet();
      return null;
    }
    return hover.type();
  }

  /** Whether batches on the file wait for a change: its requests failed on this text and build context. */
  boolean isSuspended(@NotNull VirtualFile file, long documentStamp) {
    FailureMark mark = failedFiles.get(file);
    return mark != null && mark.documentStamp() == documentStamp && mark.inputsStamp() == contextInputsStamp();
  }

  private void markFailed(@NotNull VirtualFile file, long documentStamp, @NotNull String reason) {
    FailureMark previous = failedFiles.put(file, new FailureMark(documentStamp, contextInputsStamp()));
    if (previous == null || previous.documentStamp() != documentStamp) {
      log.info("compiler hover types suspended for " + file.getPath() + " until it or its build context changes: " + reason);
    }
  }

  private long contextInputsStamp() {
    return HaxeCompilerDisplayService.getInstance(project).contextInputsStamp();
  }

  // --- declarations ---

  /** The declaration is hovered at its name, and the answer must be about that name. */
  @Nullable
  private static Target targetOf(@NotNull PsiElement element, @NotNull Document document) {
    HaxeComponentName name = HaxeTypeHintAnchors.componentNameOf(element);
    if (name == null) return null;
    TextRange nameRange = name.getTextRange();
    if (nameRange == null || nameRange.isEmpty() || nameRange.getEndOffset() > document.getTextLength()) return null;
    Range expected = new Range(positionOf(document, nameRange.getStartOffset()), positionOf(document, nameRange.getEndOffset()));
    return new Target(element, nameRange.getStartOffset(), expected.start().line(), expected);
  }

  /** The wire position of an offset: 0-based line and character, as hover answers report ranges. */
  @NotNull
  private static Position positionOf(@NotNull Document document, int offset) {
    int line = document.getLineNumber(offset);
    return new Position(line, offset - document.getLineStartOffset(line));
  }

  private static boolean hasTypeTag(@NotNull PsiElement declaration) {
    return switch (declaration) {
      case HaxeParameter parameter -> parameter.getTypeTag() != null;
      case HaxeLocalVarDeclaration local -> local.getTypeTag() != null;
      case HaxeFieldDeclaration field -> field.getTypeTag() != null;
      case HaxeMethodDeclaration method -> method.getTypeTag() != null;
      case HaxeLocalFunctionDeclaration function -> function.getTypeTag() != null;
      default -> false;
    };
  }

  // a wildcard names nothing to type, and the compiler fails a hover on one in a constructor pattern (EBinop(_))
  private static boolean isWildcard(@NotNull HaxeComponentName name) {
    return name.getIdentifier() instanceof HaxeUnnameIdentifier;
  }

  private static boolean isConstructor(@NotNull PsiElement declaration) {
    return declaration instanceof HaxeMethodDeclaration method && "new".equals(method.getName());
  }

  /** The function declaring the parameter: a method, a local function or a function literal. */
  @Nullable
  private static PsiElement functionOf(@NotNull HaxeParameter parameter) {
    return PsiTreeUtil.getParentOfType(parameter, HaxeMethodDeclaration.class, HaxeLocalFunctionDeclaration.class, HaxeFunctionLiteral.class);
  }

  public void clearCaches() {
    answers.clear();
    failedFiles.clear();
  }

  @NotNull
  public String statsSummary() {
    long count = hovers.get();
    long averageMillis = count == 0 ? 0 : hoverMillis.get() / count;
    return """
      compiler hover types: lookups=%d cacheHits=%d batches=%d batchTargets=%d hovers=%d avgHoverMs=%d maxHoverMs=%d \
      failures=%d rejectedRanges=%d\
      """.formatted(lookups.get(), cacheHits.get(), batches.get(), batchTargets.get(), count, averageMillis, maxHoverMillis.get(),
                     failures.get(), rejectedRanges.get());
  }
}
