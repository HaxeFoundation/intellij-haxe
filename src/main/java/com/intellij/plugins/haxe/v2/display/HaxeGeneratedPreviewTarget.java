package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeUnsavedDocuments;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedCodePreview.PreparedPreview;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedCodePreview.PreviewOrigin;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedDumpService.Dumped;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedDumpService.Failed;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTarget;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.Future;

/**
 * A navigation target whose navigate() builds the module dump and opens the
 * generated-code preview, wrapped by {@link #createElement} as the PSI
 * element navigation surfaces expect. The navigation element is the
 * previewed declaration: it passes the navigability checks and supplies the
 * icon and location shown in popups, and the implementation popup takes the
 * target back out of the wrapper as its own navigatable. Go-to-declaration
 * would navigate to that element itself without calling navigate(), so the
 * Haxe target-element evaluator keeps the wrapper as the target there. A
 * dump that cannot be built ends in a notification saying why.
 */
@CustomLog
public final class HaxeGeneratedPreviewTarget implements PsiTarget, PomNamedTarget {

  /** The preview to open, or the reason there is none, as shown to the user. */
  private record Preparation(@Nullable PreparedPreview preview, @Nullable String failure) {
  }

  /** What to open: a module's preview, at the spot or, unless {@link #NO_LINE}, at a line. */
  private record PreviewRequest(@NotNull Project project,
                                @NotNull HaxeCompilerDisplayService.DisplayContext context,
                                @NotNull HaxeGeneratedPreviewSpot spot,
                                int caretLine) {
  }

  private static final int NO_LINE = -1;

  private final PsiElement previewedElement;
  private final HaxeCompilerDisplayService.DisplayContext context;
  private final HaxeGeneratedPreviewSpot spot;

  private HaxeGeneratedPreviewTarget(@NotNull PsiElement previewedElement,
                                     @NotNull HaxeCompilerDisplayService.DisplayContext context,
                                     @NotNull HaxeGeneratedPreviewSpot spot) {
    this.previewedElement = previewedElement;
    this.context = context;
    this.spot = spot;
  }

  /** A preview opening on the spot, wrapped as the PSI element navigation surfaces expect. */
  @NotNull
  public static PsiElement createElement(@NotNull PsiElement previewedElement,
                                         @NotNull HaxeCompilerDisplayService.DisplayContext context,
                                         @NotNull HaxeGeneratedPreviewSpot spot) {
    HaxeGeneratedPreviewTarget target = new HaxeGeneratedPreviewTarget(previewedElement, context, spot);
    return PomService.convertToPsi(previewedElement.getProject(), target);
  }

  @TestOnly
  @NotNull
  HaxeGeneratedPreviewSpot spot() {
    return spot;
  }

  @Override
  public @NotNull String getName() {
    return HaxeBundle.message("haxe.generated.preview.target.name", spot.title());
  }

  @Override
  public @NotNull PsiElement getNavigationElement() {
    return previewedElement;
  }

  @Override
  public boolean isValid() {
    return previewedElement.isValid();
  }

  @Override
  public boolean canNavigate() {
    return true;
  }

  @Override
  public boolean canNavigateToSource() {
    return true;
  }

  @Override
  public void navigate(boolean requestFocus) {
    open(new PreviewRequest(previewedElement.getProject(), context, spot, NO_LINE));
  }

  /**
   * Builds a fresh dump of a stale preview's module and build and opens it in
   * the preview's place, at the caret line the stale one showed. EDT.
   */
  static void regenerate(@NotNull Project project, @NotNull VirtualFile staleFile, @NotNull FileEditor fileEditor) {
    PreviewOrigin origin = HaxeGeneratedCodePreview.originOf(staleFile);
    String typeDotPath = staleFile.getUserData(HaxeGeneratedCodePreview.PREVIEW_KEY);
    if (origin == null || typeDotPath == null) return;
    HaxeGeneratedPreviewSpot spot = new HaxeGeneratedPreviewSpot(origin.moduleDotPath(), typeDotPath, null, null);
    open(new PreviewRequest(project, origin.context(), spot, caretLineOf(fileEditor)));
  }

  private static int caretLineOf(@NotNull FileEditor fileEditor) {
    return fileEditor instanceof TextEditor textEditor
           ? textEditor.getEditor().getCaretModel().getLogicalPosition().line
           : NO_LINE;
  }

  /**
   * Builds the dump unless the cached one is current and opens the preview.
   * Saves the documents first: the dump build compiles the files on disk. EDT.
   */
  private static void open(@NotNull PreviewRequest request) {
    Project project = request.project();
    // the dump is a full compile - macros run
    if (!HaxeProjectTrust.confirmForAction(project, HaxeBundle.message("haxe.trust.action.generated.preview"))) {
      return;
    }
    HaxeUnsavedDocuments.saveAll();
    // modal so an uncached dump (a full compile) is visibly in progress and
    // cancelable, instead of an easily missed status-bar task
    String title = HaxeBundle.message("haxe.generated.preview.progress.title");
    Task.Modal dumpTask = new Task.Modal(project, title, true) {
      private Preparation prepared;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        // the dump compile blocks in socket IO and cannot observe the
        // indicator, so it runs on its own thread. Cancel closes the dialog
        // immediately, while an abandoned build still finishes and lands in
        // the cache for the next attempt.
        Future<Preparation> preparing = ApplicationManager.getApplication().executeOnPooledThread(() -> preparePreview(request));
        prepared = HaxeCancelableFutures.awaitUnderProgress(preparing, indicator, "generated-code preview preparation failed");
      }

      @Override
      public void onSuccess() {
        if (prepared == null) return;
        if (prepared.preview() != null) {
          HaxeGeneratedCodePreview.openPrepared(project, prepared.preview());
        }
        else if (prepared.failure() != null) {
          notifyFailure(project, prepared.failure());
        }
      }
    };
    dumpTask.queue();
  }

  @NotNull
  private static Preparation preparePreview(@NotNull PreviewRequest request) {
    HaxeGeneratedDumpService dumpService = HaxeGeneratedDumpService.getInstance(request.project());
    return switch (dumpService.ensureDumps(request.context())) {
      case Failed failed -> failed(request, failed.message());
      case Dumped dumped -> prepareFrom(request, dumpService, dumped);
    };
  }

  @NotNull
  private static Preparation prepareFrom(@NotNull PreviewRequest request,
                                         @NotNull HaxeGeneratedDumpService dumpService,
                                         @NotNull Dumped dumped) {
    String moduleDotPath = request.spot().moduleDotPath();
    Path moduleDump = dumpService.findModuleDump(dumped.targetDumpDir(), moduleDotPath);
    if (moduleDump == null) return failed(request, HaxeBundle.message("haxe.generated.preview.failed.no.module", moduleDotPath));
    // rendering and the offset lookup parse the dump, which can be
    // library-sized, so they stay off the EDT
    PreviewOrigin origin = new PreviewOrigin(request.context(), moduleDotPath, dumped.sourcesStamp());
    PreparedPreview preview = HaxeGeneratedCodePreview.prepare(request.project(), moduleDump, request.spot(), origin);
    if (preview == null) return failed(request, HaxeBundle.message("haxe.generated.preview.failed.unreadable", moduleDotPath));
    PreparedPreview placed = request.caretLine() == NO_LINE ? preview : preview.atLine(request.caretLine());
    return new Preparation(placed, null);
  }

  @NotNull
  private static Preparation failed(@NotNull PreviewRequest request, @NotNull String message) {
    log.info("generated-code preview of " + request.spot().moduleDotPath() + ": " + message);
    return new Preparation(null, message);
  }

  private static void notifyFailure(@NotNull Project project, @NotNull String message) {
    HaxeCompilerNotifications.getInstance(project).warn(HaxeBundle.message("haxe.generated.preview.failed.title"), message);
  }
}
