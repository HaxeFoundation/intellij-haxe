package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public class HaxeContainerStubFactory<P extends PsiElement> implements StubElementFactory<HaxeEmptyContainerStub<P>, P> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeEmptyContainerStub<P>, HaxeElementType, P> myPsiCreator;

  public HaxeContainerStubFactory(@NotNull HaxeElementType elementType,
                                  @NotNull BiFunction<HaxeEmptyContainerStub<P>, HaxeElementType, P> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }

  @Override
  public P createPsi(@NotNull HaxeEmptyContainerStub<P> stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeEmptyContainerStub<P> createStub(@NotNull P psi, StubElement<?> parentStub) {
    return new HaxeEmptyContainerStub<>(parentStub, myElementType);
  }
}