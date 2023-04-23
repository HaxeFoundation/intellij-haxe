package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.StripSpaces;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeRemoveElementFixer;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.PsiFileUtils;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.PACKAGE_NAME_CHECK;

public class HaxePackageAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxePackageStatement packageStatement) {
      check(packageStatement, holder);
    }
  }

  static public void check(final HaxePackageStatement element, final AnnotationHolder holder) {

    HaxeFile file = (HaxeFile)element.getContainingFile();
    if (element != file.getPackageStatement()) {  // If it's not the first one...
      holder.newAnnotation(HighlightSeverity.ERROR, "Multiple package names are not allowed.")
        .range(element)
        .withFix(new HaxeRemoveElementFixer("Remove extra package declaration", element, StripSpaces.BOTH))
        .create();
    }

    if (!PACKAGE_NAME_CHECK.isEnabled(element)) return;

    final HaxeReferenceExpression expression = element.getReferenceExpression();
    String packageName = (expression != null) ? expression.getText() : "";
    PsiDirectory fileDirectory = file.getParent();
    if (fileDirectory == null) return;
    List<PsiFileSystemItem> fileRange = PsiFileUtils.getRange(PsiFileUtils.findRoot(fileDirectory), fileDirectory);
    fileRange.remove(0);
    String actualPath = PsiFileUtils.getListPath(fileRange);
    final String actualPackage = actualPath.replace('/', '.');
    final String actualPackage2 = HaxeResolveUtil.getPackageName(file);
    // @TODO: Should use HaxeResolveUtil

    for (String s : StringUtils.split(packageName, '.')) {
      if (!s.substring(0, 1).toLowerCase().equals(s.substring(0, 1))) {
        //HaxeSemanticError.addError(element, new HaxeSemanticError("Package name '" + s + "' must start with a lower case character"));
        // @TODO: Move to bundle
        holder.newAnnotation(HighlightSeverity.ERROR, "Package name '" + s + "' must start with a lower case character").create();
      }
    }

    if (!packageName.equals(actualPackage)) {
      holder.newAnnotation(HighlightSeverity.ERROR, "Invalid package name! '" + packageName + "' should be '" + actualPackage + "'")
        .range(element)
        .withFix(
          new HaxeFixer("Fix package") {
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
