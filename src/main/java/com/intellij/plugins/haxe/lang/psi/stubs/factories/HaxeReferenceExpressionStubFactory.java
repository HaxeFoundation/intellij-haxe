package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeReferenceExpressionStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;


public class HaxeReferenceExpressionStubFactory implements StubElementFactory<HaxeReferenceExpressionStub, HaxeReferenceExpression> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeReferenceExpressionStub, HaxeElementType, ? extends HaxeReferenceExpression> myPsiCreator;

  public HaxeReferenceExpressionStubFactory(@NotNull HaxeElementType elementType,
                                            @NotNull BiFunction<HaxeReferenceExpressionStub, HaxeElementType, ? extends HaxeReferenceExpression> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }


  @Override
  public HaxeReferenceExpression createPsi(@NotNull HaxeReferenceExpressionStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @Override
  public @NonNull HaxeReferenceExpressionStub createStub(@NonNull HaxeReferenceExpression psi,
                                                         StubElement<? extends PsiElement> parentStub) {
    return new HaxeReferenceExpressionStub(parentStub, myElementType, psi.getText());
  }
}