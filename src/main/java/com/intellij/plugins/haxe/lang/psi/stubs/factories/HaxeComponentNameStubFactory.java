package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubFilterUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeComponentNameStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public class HaxeComponentNameStubFactory implements StubElementFactory<HaxeComponentNameStub, HaxeComponentName> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeComponentNameStub, HaxeElementType, ? extends HaxeComponentName> myPsiCreator;

  public HaxeComponentNameStubFactory(@NotNull HaxeElementType elementType,
                                      @NotNull BiFunction<HaxeComponentNameStub, HaxeElementType, ? extends HaxeComponentName> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }


  @Override
  public HaxeComponentName createPsi(@NotNull HaxeComponentNameStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @Override
  public @NonNull HaxeComponentNameStub createStub(@NonNull HaxeComponentName psi,
                                                   StubElement<? extends PsiElement> parentStub) {
    return new HaxeComponentNameStub(parentStub, myElementType, psi.getName());
  }
}