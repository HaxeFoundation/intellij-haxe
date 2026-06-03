package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class lookupItemImportUtil {

  public static void addImportIfNecessary(InsertionContext context, PsiElement element, String qname) {
    context.commitDocument();
    HaxeReference reference = PsiTreeUtil.getParentOfType(element, HaxeReference.class);
    if (reference != null) {
      List<? extends PsiElement> resolve = HaxeResolver.INSTANCE.resolve(reference, true);
      boolean needImport = resolve.isEmpty();
      if(needImport) {
        HaxeAddImportHelper.addImport(qname, context.getFile());
      }else {
        PsiElement first = resolve.getFirst();
        if(isStdTypes(first)) return;
        if(!isCorrectClass(first, qname)) {
          // replace class with fully qualified path to avoid conflicts
          HaxeReference fullyQualifiedReference = HaxeElementGenerator.createReferenceFromText(element.getProject(), qname);
          if (fullyQualifiedReference!= null) {

            element.replace(fullyQualifiedReference);
            context.commitDocument();
          }
        }
      }
    }
  }

  // we do not want to import or use Fqn ref on standard types
  private static boolean isStdTypes(PsiElement first) {
    if (first != null) {
      PsiFile containingFile = first.getContainingFile();
      return HaxeIndexUtil.isStdTypeFile(containingFile);
    }
    return false;
  }


  public static boolean isCorrectClass(PsiElement element, String expectedQname) {
    HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeClass.class);
    if (haxeClass != null) {
      String qualifiedName = haxeClass.getQualifiedName();
      return qualifiedName!= null && qualifiedName.equals(expectedQname);
    }
    return false;
  }

  public static @NotNull String createQname(String name, String path) {
    if (path.isEmpty()) return name;
    return path + "." + name;
  }

}
