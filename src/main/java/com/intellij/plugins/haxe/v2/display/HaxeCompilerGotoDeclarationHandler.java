package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.Location;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Go-to-declaration answered by the compilation server. For a reference
 * under the caret, the targets are the PSI elements at the locations
 * {@code display/definition} returns, so the platform navigates, previews
 * and offers a chooser for several targets just as for static results. The
 * platform calls handlers under a cancelable progress, and the request waits
 * a bounded time. Ctrl-hover asks too; the navigation service keeps the
 * answer, so the click that follows a hover costs no second request. A
 * string literal is never asked about: the compiler sees no reference
 * inside one, and the string-link references resolve its names themselves.
 * <p>
 * While the generated-member preview is on, a declaration a macro generated
 * opens the generated-code preview, where it exists: a member the resolver
 * finds only in the compiler's post-macro blueprint, and a name that neither
 * the server nor the static resolver can place, such as the parameter a
 * build macro wraps around an expression. A name the compiler rejects (an
 * unknown type) gets no preview: the code there does not compile, so no
 * macro produced it. For a generated member the server
 * answers the position the macro gave it, typically the source field it was
 * generated from or the class's {@code @:build}; with the preview off,
 * navigation goes there. No targets are returned for a declaration under
 * the caret, where the platform keeps its show-usages behaviour, nor for a
 * file the compiler cannot serve, where the notification is shown and the
 * platform resolves the reference itself.
 */
public final class HaxeCompilerGotoDeclarationHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    HaxeReference reference = referenceToAsk(sourceElement);
    if (reference == null) return null;
    VirtualFile file = HaxeCompilerDisplayService.physicalFileOf(sourceElement);
    if (file == null) return null;
    Project project = sourceElement.getProject();
    DisplayContext previewContext = previewContextFor(project, file);
    if (previewContext != null) {
      PsiElement resolved = reference.resolve();
      PsiElement generatedMember = blueprintMemberPreviewOf(resolved, reference.getReferenceName(), previewContext);
      if (generatedMember != null) return new PsiElement[]{generatedMember};
    }

    HaxeCompilerNavigationService service = HaxeCompilerNavigationService.getInstance(project);
    if (!service.ensureAvailable(file)) return null;
    return targetsOf(project, service.definitionUnderReadLock(file, offset), reference, previewContext);
  }

  /**
   * The targets for the server's answer: the declarations it placed, else,
   * while the preview is on, the generated-code preview around a name it
   * answered for without placing it. Null without an answer: a request the
   * compiler rejected means the code at the caret does not compile, and the
   * preview's dump build would fail on the same error.
   */
  static PsiElement @Nullable [] targetsOf(@NotNull Project project,
                                           @Nullable List<Location> answer,
                                           @NotNull HaxeReference reference,
                                           @Nullable DisplayContext previewContext) {
    if (answer == null) return null;
    List<PsiElement> declarations = declarationsOf(project, answer);
    if (!declarations.isEmpty()) return declarations.toArray(PsiElement.EMPTY_ARRAY);
    PsiElement generated = previewContext == null ? null : generatedDeclarationOf(reference, previewContext);
    return generated == null ? null : new PsiElement[]{generated};
  }

  /**
   * The reference the compiler is asked about for the element under the
   * caret; none inside a string literal, whose links resolve statically.
   */
  @Nullable
  static HaxeReference referenceToAsk(@Nullable PsiElement sourceElement) {
    if (sourceElement == null) return null;
    HaxeReference reference = PsiTreeUtil.getParentOfType(sourceElement, HaxeReference.class, false);
    return reference instanceof HaxeStringLiteralExpression ? null : reference;
  }

  /** The context previews are built in; null while the generated-member preview is off or the file has none. */
  @Nullable
  private static DisplayContext previewContextFor(@NotNull Project project, @NotNull VirtualFile file) {
    if (!HaxeCompilerSettings.getInstance(project).usesGeneratedMemberPreview()) return null;
    return HaxeCompilerDisplayService.getInstance(project).contextFor(file);
  }

  @NotNull
  private static List<PsiElement> declarationsOf(@NotNull Project project, @NotNull List<Location> locations) {
    List<PsiElement> declarations = new ArrayList<>();
    for (Location location : locations) {
      PsiFile psiFile = HaxeCompilerLocations.psiFileOf(project, location);
      PsiElement declaration = psiFile == null ? null : HaxeCompilerLocations.declarationIn(psiFile, location.range());
      if (declaration != null) declarations.add(declaration);
    }
    return declarations;
  }

  /**
   * The generated-code preview of a member the resolver found only in the
   * compiler's post-macro blueprint, with the caret on {@code memberName}.
   * Null for a target with a source declaration.
   */
  @Nullable
  static PsiElement blueprintMemberPreviewOf(@Nullable PsiElement resolved,
                                             @Nullable String memberName,
                                             @NotNull DisplayContext context) {
    if (resolved == null) return null;
    PsiFile resolvedFile = resolved.getContainingFile();
    if (resolvedFile == null) return null;
    String dotPath = resolvedFile.getUserData(HaxeCompilerResolveService.BLUEPRINT_DOT_PATH);
    if (dotPath == null) return null;
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.ofType(dotPath, memberName);
    return HaxeGeneratedPreviewTarget.createElement(resolved, context, spot);
  }

  /**
   * The generated-code preview of the member the reference sits in, as the
   * target for a name the server could not place. Null when the static
   * resolver places the name (the platform then navigates there itself) or
   * the reference sits outside a class member. Read action.
   */
  @Nullable
  static PsiElement generatedDeclarationOf(@NotNull HaxeReference reference, @NotNull DisplayContext context) {
    String name = reference.getReferenceName();
    HaxeNamedComponent member = HaxeGeneratedPreviewSpot.enclosingMember(reference);
    if (name == null || member == null || reference.resolve() != null) return null;
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.around(reference, name);
    return spot == null ? null : HaxeGeneratedPreviewTarget.createElement(member, context, spot);
  }
}
