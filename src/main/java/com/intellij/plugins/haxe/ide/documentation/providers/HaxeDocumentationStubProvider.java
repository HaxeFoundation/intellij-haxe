package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.lang.DocumentationStubProvider;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.HaxeCommenter;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclarationConstructor;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.documentation.HaxeDocTags.TAG_PARAM;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocTags.TAG_RETURN;

/// The declaration a new doc comment documents and the tag lines it starts with, for "Fix doc comment" and Enter
/// after `/**`. Haxe declarations implement Java's PSI interfaces, so this provider runs before the Java plugin's,
/// which would otherwise claim them; for the same reason it answers every Haxe declaration, if only with an empty stub.
public class HaxeDocumentationStubProvider implements DocumentationStubProvider {

  /** Members dox documents; locals, parameters and type parameters have no doc comment of their own. */
  private static final Set<HaxeComponentType> DOCUMENTED_TYPES = EnumSet.of(
    HaxeComponentType.CLASS, HaxeComponentType.ABSTRACT, HaxeComponentType.INTERFACE, HaxeComponentType.TYPEDEF,
    HaxeComponentType.ENUM, HaxeComponentType.METHOD, HaxeComponentType.FIELD);

  /** The declaration around `element` that takes a doc comment; inside a doc comment, the comment's owner. */
  @Override
  public @Nullable PsiElement findDocumentedElement(@NotNull PsiElement element) {
    if (!(element.getContainingFile() instanceof HaxeFile)) return null;
    HaxePsiDocCommentImpl docComment = PsiTreeUtil.getParentOfType(element, HaxePsiDocCommentImpl.class, false);
    if (docComment != null) {
      return docComment.getOwner() instanceof HaxeNamedComponent owner ? owner : null;
    }
    HaxeNamedComponent component = PsiTreeUtil.getParentOfType(element, HaxeNamedComponent.class, false);
    while (component != null && !DOCUMENTED_TYPES.contains(component.getComponentType())) {
      component = PsiTreeUtil.getParentOfType(component, HaxeNamedComponent.class);
    }
    return component;
  }

  /** The comment the declaration reads; an earlier doc comment in front of it is not it. */
  @Override
  public @Nullable PsiComment findDocComment(@NotNull PsiElement element) {
    return element instanceof HaxeNamedComponent component ? HaxeResolveUtil.findDocumentation(component) : null;
  }

  /** The tag lines of the declaration's doc comment, in the comment's style and without its indent. */
  @Override
  public @Nullable String documentationStub(@NotNull PsiElement element) {
    if (!(element instanceof HaxeNamedComponent component)) return null;
    if (!(HaxeResolveUtil.findDocumentation(component) instanceof HaxePsiDocCommentImpl docComment)) return "";

    String linePrefix = HaxeCommenter.usesLeadingAsterisks(docComment) ? HaxeCommenter.DOC_COMMENT_LINE_PREFIX + " " : "";
    StringBuilder stub = new StringBuilder();
    for (String tag : stubTags(docComment)) {
      stub.append(linePrefix).append(tag).append('\n');
    }
    return stub.toString();
  }

  /**
   * The tags a method's new doc comment starts with: one `@param` per parameter, and `@return` when the method
   * returns a value. Empty for other owners, and for a comment that is not the one its owner reads.
   */
  public static @NotNull List<String> stubTags(@NotNull HaxePsiDocCommentImpl docComment) {
    if (!(docComment.getOwner() instanceof HaxeMethod method) || HaxeResolveUtil.findDocumentation(method) != docComment) {
      return List.of();
    }
    List<String> tags = new ArrayList<>();
    for (HaxeParameterModel parameter : method.getModel().getParameters()) {
      tags.add(TAG_PARAM + " " + parameter.getName());
    }
    if (returnsValue(method)) {
      tags.add(TAG_RETURN);
    }
    return tags;
  }

  private static boolean returnsValue(@NotNull HaxeMethod method) {
    if (method instanceof HaxeEnumValueDeclarationConstructor || method.getModel().isConstructor()) return false;
    ResultHolder returnType = method.getModel().getReturnType(null);
    return !returnType.isVoid() && !returnType.isUnknown();
  }
}
