package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeRestParameter;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeParameterStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public class HaxeParameterStubFactory implements StubElementFactory<HaxeParameterStub, HaxeParameter> {

  private final HaxeElementType myElementType;
  private final BiFunction<HaxeParameterStub, HaxeElementType, ? extends HaxeParameter> myPsiCreator;

  public HaxeParameterStubFactory(@NotNull HaxeElementType elementType,
                                  @NotNull BiFunction<HaxeParameterStub, HaxeElementType, ? extends HaxeParameter> psiCreator) {
    myElementType = elementType;
    myPsiCreator = psiCreator;
  }



  @Override
  public HaxeParameter createPsi(@NotNull HaxeParameterStub stub) {
    return myPsiCreator.apply(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeParameterStub createStub(@NotNull HaxeParameter psi, StubElement<?> parentStub) {
    boolean isOptional = psi.getOptionalMark() != null;
    boolean hasInit = psi.getVarInit() != null;
    boolean isRest = psi instanceof HaxeRestParameter;
    return new HaxeParameterStub(parentStub, myElementType, psi.getName(), isOptional, isRest, hasInit);
  }
}