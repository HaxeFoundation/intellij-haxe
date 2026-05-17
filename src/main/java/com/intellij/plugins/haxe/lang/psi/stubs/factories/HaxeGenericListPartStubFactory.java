package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeGenericListPartStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public class HaxeGenericListPartStubFactory implements StubElementFactory<HaxeGenericListPartStub, HaxeClass> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeGenericListPartStub, HaxeElementType, ? extends HaxeClass> myPsiCreator;

  public HaxeGenericListPartStubFactory(
    @NotNull HaxeElementType elementType,
    @NotNull BiFunction<HaxeGenericListPartStub, HaxeElementType, ? extends HaxeClass> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }



  @Override
  public HaxeClass createPsi(@NotNull HaxeGenericListPartStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeGenericListPartStub createStub(@NotNull HaxeClass psi, StubElement<?> parentStub) {
    return new HaxeGenericListPartStub(parentStub, myElementType, psi.getName());
  }
}