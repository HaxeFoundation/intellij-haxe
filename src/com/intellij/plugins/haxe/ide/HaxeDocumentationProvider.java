package com.intellij.plugins.haxe.ide;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDocumentationProvider implements DocumentationProvider {

  /*
    provides ctrl+hover info
   */
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, PsiElement originalElement) {
    if (!(element instanceof HaxeComponentName) && !(element instanceof HaxeNamedComponent)) {
      return null;
    }
    PsiElement namedComponent = element instanceof HaxeNamedComponent ? element : element.getParent();
    final StringBuilder builder = new StringBuilder();
    final HaxeComponentType type = HaxeComponentType.typeOf(namedComponent);
    if (namedComponent instanceof HaxeClass) {
      builder.append(((HaxeClass)namedComponent).getQualifiedName());
    }
    else if (type == HaxeComponentType.FIELD || type == HaxeComponentType.METHOD) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class);
      assert haxeClass != null;
      builder.append(haxeClass.getQualifiedName());
      builder.append(" ");
      builder.append(type.toString().toLowerCase());
      builder.append(" ");
      builder.append(element.getText());
    }
    final PsiComment comment = HaxeResolveUtil.findDocumentation((HaxeNamedComponent)namedComponent);
    if (comment != null) {
      builder.append("<br/>");
      builder.append(HaxeDocumentationUtil.unwrapCommentDelimiters(comment.getText()));
    }
    return builder.toString();
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    return null;
  }

  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    return null;
  }
}
