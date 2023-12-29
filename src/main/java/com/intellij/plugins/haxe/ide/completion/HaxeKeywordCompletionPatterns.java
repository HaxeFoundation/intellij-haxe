package com.intellij.plugins.haxe.ide.completion;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.string;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeKeywordCompletionPatterns {

  public static final PsiElementPattern.Capture<PsiElement> packageExpected =
    psiElement().inside(HaxeFile.class).atStartOf(psiElement(HaxeFile.class));
  public static final PsiElementPattern.Capture<PsiElement> toplevelScope =
    psiElement().andNot(psiElement().inside(psiElement(HaxeModule.class)));

  public static final PsiElementPattern.Capture<PsiElement> moduleScope =
    psiElement()
      .andNot(psiElement().inside(psiElement(HaxeClass.class)))
      .andNot(psiElement().inside(psiElement(HaxeMethod.class)))
      .andNot(psiElement().afterLeafSkipping(psiElement().whitespaceCommentEmptyOrError(), psiElement()
        .andOr(psiElement().withElementType(KFUNCTION), psiElement().withElementType(KVAR))
      ));

  public static final PsiElementPattern.Capture<PsiElement> classDeclarationScope =
    psiElement()
      .and(psiElement().inside(psiElement(HaxeClassDeclaration.class)))
      .andNot(psiElement().inside(psiElement(HaxeClassBody.class)));
  public static final PsiElementPattern.Capture<PsiElement> interfaceDeclarationScope =
    psiElement()
      .and(psiElement().inside(psiElement(HaxeInterfaceDeclaration.class)))
      .andNot(psiElement().inside(psiElement(HaxeInterfaceBody.class)));
  public static final PsiElementPattern.Capture<PsiElement> abstractTypeDeclarationScope =
    psiElement()
      .and(psiElement().inside(psiElement(HaxeAbstractTypeDeclaration.class)))
      .andNot(psiElement().inside(psiElement(HaxeClassBody.class)));
  public static final PsiElementPattern.Capture<PsiElement> interfaceBodyScope =
    psiElement()
      .and(psiElement().inside(psiElement(HaxeInterfaceBody.class)))
      .andNot(psiElement().inside(psiElement(HaxeMethod.class)));
  public static final PsiElementPattern.Capture<PsiElement> classBodyScope =
    psiElement()
      .and(psiElement().inside(psiElement(HaxeClassBody.class)))
      .andNot(psiElement().inside(psiElement(HaxeMethod.class)));
  public static final PsiElementPattern.Capture<PsiElement> functionBodyScope =
    psiElement().inside(psiElement(HaxeMethod.class));

  public static final PsiElementPattern.Capture<PsiElement> insideSwitchCase = psiElement()
    .inside(HaxeSwitchBlock.class)
    .andNot(psiElement().inside(HaxeSwitchCase.class));
  public static final PsiElementPattern.Capture<PsiElement> isInsideLoopBlock = psiElement()
    .andOr(
      psiElement().inside(HaxeForStatement.class),
      psiElement().inside(HaxeWhileStatement.class),
      psiElement().inside(HaxeDoWhileBody.class)
    );
  public static final PsiElementPattern.Capture<PsiElement> isInsideForIterator = psiElement()
    .inside(HaxeForStatement.class)
    .and(psiElement().afterSiblingSkipping(psiElement().whitespaceCommentEmptyOrError(), psiElement().withElementType(COMPONENT_NAME)))
    .andNot(psiElement().afterLeafSkipping(psiElement().whitespaceCommentEmptyOrError(), psiElement().withText(")")));

  public static final PsiElementPattern.Capture<PsiElement> isPropertyGetterValue = psiElement()
      .inside(HaxePropertyDeclaration.class)
        .andNot(psiElement().afterSiblingSkipping(psiElement().whitespace().and(string().oneOf(",")), psiElement(HaxePropertyAccessor.class)));
  public static final PsiElementPattern.Capture<PsiElement> isPropertySetterValue = psiElement()
      .inside(HaxePropertyDeclaration.class)
        .and(psiElement().afterSiblingSkipping(psiElement().whitespace().and(string().oneOf(",")), psiElement(HaxePropertyAccessor.class)));


  public static final PsiElementPattern.Capture<PsiElement> isAfterIfStatement = psiElement()
    .afterSiblingSkipping(psiElement().whitespaceCommentEmptyOrError(), psiElement(HaxeIfStatement.class));
}
