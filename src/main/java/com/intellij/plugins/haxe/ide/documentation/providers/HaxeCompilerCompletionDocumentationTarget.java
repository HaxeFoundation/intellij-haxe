package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.model.Pointer;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.ide.documentation.HaxeDocumentationRenderer;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCompletionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quick documentation for a completion item from the compilation server. It
 * shows a signature line (the name, a type's qualified path, the type the
 * compiler reports) and the doc comment, rendered like the plugin's own
 * documentation. The doc arrives as the comment's raw body with its source
 * indentation, so it is stripped like a PSI comment before the markdown
 * renderer sees it. For an item that arrived without a doc, the doc is
 * requested asynchronously ({@code display/completionItem/resolve}) without
 * holding the read lock. The item is a plain value, so the target serves as
 * its own pointer.
 */
final class HaxeCompilerCompletionDocumentationTarget implements DocumentationTarget {

  private final Project project;
  private final VirtualFile file;
  private final CompletionItem item;

  HaxeCompilerCompletionDocumentationTarget(@NotNull Project project, @NotNull VirtualFile file, @NotNull CompletionItem item) {
    this.project = project;
    this.file = file;
    this.item = item;
  }

  @Override
  public @NotNull TargetPresentation computePresentation() {
    return TargetPresentation.builder(item.name()).presentation();
  }

  @Override
  public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
    return Pointer.hardPointer(this);
  }

  @Override
  public @NotNull DocumentationResult computeDocumentation() {
    if (item.doc() != null) return DocumentationResult.documentation(render(item.doc()));
    boolean resolvable = item.index() >= 0 && (item.isField() || item.isEnumField() || item.isType());
    if (!resolvable) return DocumentationResult.documentation(render(null));
    return DocumentationResult.asyncDocumentation(this::resolveAndRender);
  }

  /**
   * Fetches the doc without the read lock, then renders under a read action,
   * because the renderer highlights code fences through the editor's lexers.
   */
  private DocumentationResult.Documentation resolveAndRender() {
    String doc = HaxeCompilerCompletionService.getInstance(project).resolveDoc(file, item.index());
    String html = ReadAction.nonBlocking(() -> render(doc)).executeSynchronously();
    return DocumentationResult.documentation(html);
  }

  private String render(@Nullable String doc) {
    HtmlBuilder html = new HtmlBuilder().append(signature());
    if (doc != null) {
      HaxeDocumentationRenderer renderer = project.getService(HaxeDocumentationRenderer.class);
      String markdown = HaxeDocumentationUtil.stripForRendering(doc);
      html.hr().br().append(HtmlChunk.raw(renderer.parseAndRender(markdown)));
    }
    return html.toString();
  }

  /** {@code name:Type} for a value, the qualified path for a type or package, the bare name otherwise. */
  private HtmlChunk signature() {
    HtmlBuilder line = new HtmlBuilder().append(HtmlChunk.text(item.name()).bold());
    if (item.detail() != null && !item.detail().isEmpty() && !item.detail().equals(item.name())) {
      line.nbsp(1).append(HtmlChunk.text(item.detail()).italic());
    }
    if (item.type() != null) {
      line.append(":").append(HtmlChunk.text(item.type().presentable()));
    }
    return line.toFragment();
  }
}
