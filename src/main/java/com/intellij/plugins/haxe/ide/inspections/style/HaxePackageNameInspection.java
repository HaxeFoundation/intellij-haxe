package com.intellij.plugins.haxe.ide.inspections.style;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.util.PsiFileUtils;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFileSystemItem;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Package naming conventions: lower-case parts, name matching the directory. */
public class HaxePackageNameInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxePackageStatement.class, HaxePackageNameInspection::check);
  }


  public static void check(@NotNull HaxePackageStatement element, @NotNull HaxeProblemReporter reporter) {
    HaxeFile file = (HaxeFile)element.getContainingFile();
    if (element != file.getPackageStatement()) return; // the annotator flags the extras

    final HaxeReferenceExpression expression = element.getReferenceExpression();
    String packageName = (expression != null) ? expression.getText() : "";
    PsiDirectory fileDirectory = file.getParent();
    if (fileDirectory == null) return;
    List<PsiFileSystemItem> fileRange = PsiFileUtils.getRange(PsiFileUtils.findRoot(fileDirectory), fileDirectory);
    fileRange.remove(0);
    String actualPath = PsiFileUtils.getListPath(fileRange);
    final String actualPackage = actualPath.replace('/', '.');

    for (String packagePart : StringUtils.split(packageName, '.')) {
      if (!packagePart.substring(0, 1).toLowerCase().equals(packagePart.substring(0, 1))) {
        String errorMessage = HaxeBundle.message("haxe.semantic.package.name.must.start.with.lower.case", packagePart);
        reporter.problem(HighlightSeverity.ERROR, errorMessage).range(element).create();
      }
    }

    if (!packageName.equals(actualPackage)) {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.package.name", packageName, actualPackage))
        .range(element)
        .withFix(
          new HaxeFixer(HaxeBundle.message("haxe.quickfix.fix.package")) {
            @Override
            public void run() {
              Document document =
                PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());

              if (expression != null) {
                TextRange range = expression.getTextRange();
                document.replaceString(range.getStartOffset(), range.getEndOffset(), actualPackage);
              }
              else {
                int offset =
                  element.getNode().findChildByType(HaxeTokenTypes.OSEMI).getTextRange().getStartOffset();
                document.replaceString(offset, offset, actualPackage);
              }
            }
          }
        )
        .create();
    }
  }

}
