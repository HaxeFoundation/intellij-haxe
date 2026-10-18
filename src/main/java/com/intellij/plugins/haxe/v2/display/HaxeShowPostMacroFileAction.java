package com.intellij.plugins.haxe.v2.display;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the generated-code preview of the file under the caret or selection:
 * the module as the compiler sees it after its build macros ran. Offered for
 * a file whose classes a build macro reaches ({@code @:build} on the class
 * or {@code @:autoBuild} on a type it extends or implements); expression
 * macros leave no such mark and are not detected. The preview opens on the
 * class and member at the caret when there is one.
 */
public final class HaxeShowPostMacroFileAction extends AnAction {

  /** What one invocation previews: the element the target stands for, and where the preview opens. */
  private record Preview(@NotNull PsiElement previewed, @NotNull DisplayContext context, @NotNull HaxeGeneratedPreviewSpot spot) {
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(previewableFile(e) != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Preview preview = ReadAction.computeBlocking(() -> previewAt(e));
    if (preview == null) return;
    PsiElement target = HaxeGeneratedPreviewTarget.createElement(preview.previewed(), preview.context(), preview.spot());
    Navigatable descriptor = EditSourceUtil.getDescriptor(target);
    if (descriptor != null) OpenSourceUtil.navigate(true, descriptor);
  }

  /** The Haxe source the event names when a build macro reaches it and the compiler can serve it; null otherwise. Read action. */
  @Nullable
  private static HaxeFile previewableFile(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !(e.getData(CommonDataKeys.PSI_FILE) instanceof HaxeFile file)) return null;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null || HaxeGeneratedCodePreview.isPreview(virtualFile)) return null;
    if (!reachesBuildMacro(file)) return null;
    return HaxeCompilerFeatureAvailability.isAvailable(project, virtualFile) ? file : null;
  }

  /** Whether a build macro rewrites a class of the file. */
  static boolean reachesBuildMacro(@NotNull HaxeFile file) {
    for (HaxeClass haxeClass : file.getClassList()) {
      if (AnnotatorUtil.hasMacroForCodeGeneration(haxeClass.getModel())) return true;
    }
    return false;
  }

  /** The preview for the caret's class and member, else for the file's first class; null when there is nothing to preview. */
  @Nullable
  private static Preview previewAt(@NotNull AnActionEvent e) {
    HaxeFile file = previewableFile(e);
    if (file == null) return null;
    VirtualFile virtualFile = file.getVirtualFile();
    DisplayContext context = HaxeCompilerDisplayService.getInstance(file.getProject()).contextFor(virtualFile);
    if (context == null) return null;
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    PsiElement atCaret = editor == null ? null : file.findElementAt(editor.getCaretModel().getOffset());
    if (atCaret != null) {
      HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.around(atCaret, null);
      HaxeNamedComponent member = HaxeGeneratedPreviewSpot.enclosingMember(atCaret);
      HaxeClass haxeClass = PsiTreeUtil.getParentOfType(atCaret, HaxeClass.class, false);
      if (spot != null) return new Preview(member != null ? member : haxeClass, context, spot);
    }
    if (file.getClassList().isEmpty()) return null;
    HaxeClass firstClass = file.getClassList().get(0);
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.of(firstClass, null, null);
    return spot == null ? null : new Preview(firstClass, context, spot);
  }
}
