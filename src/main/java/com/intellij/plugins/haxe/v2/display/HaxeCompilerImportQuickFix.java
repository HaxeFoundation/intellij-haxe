package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Imports the type the compiler suggested for an unresolved identifier,
 * through {@link HaxeAddImportHelper}, the plugin's single place for adding
 * imports. Implements both fix interfaces, because Inspect Code keeps an
 * annotation's fix only when it is a {@link LocalQuickFix}.
 */
final class HaxeCompilerImportQuickFix implements IntentionAction, LocalQuickFix {
  private final String qualifiedName;

  HaxeCompilerImportQuickFix(@NotNull String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  @Override
  public @NotNull String getText() {
    return HaxeBundle.message("haxe.diagnostics.fix.import", qualifiedName);
  }

  @Override
  public @NotNull String getName() {
    return getText();
  }

  @Override
  public @NotNull String getFamilyName() {
    return HaxeBundle.message("haxe.diagnostics.fix.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return file instanceof HaxeFile && !alreadyImported(file);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    HaxeAddImportHelper.addImport(qualifiedName, file);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element != null) {
      HaxeAddImportHelper.addImport(qualifiedName, element.getContainingFile());
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private boolean alreadyImported(@NotNull PsiFile file) {
    // the whole statement `import <qualified name>;` with any whitespace around the path
    Pattern statement = Pattern.compile("import\\s+" + Pattern.quote(qualifiedName) + "\\s*;");
    return PsiTreeUtil.findChildrenOfType(file, HaxeImportStatement.class).stream()
      .anyMatch(existing -> statement.matcher(existing.getText()).matches());
  }
}
