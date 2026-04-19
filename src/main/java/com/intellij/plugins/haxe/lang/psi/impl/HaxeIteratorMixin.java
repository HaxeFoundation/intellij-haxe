package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeIteratorStub;
import com.intellij.plugins.haxe.model.HaxeIteratorModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import org.jetbrains.annotations.NotNull;

public abstract class HaxeIteratorMixin extends HaxeStubBasedNamedComponent<HaxeIteratorStub> implements HaxeIterator {

  private HaxeIteratorModel _model;

  public HaxeIteratorMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public HaxeModel getModel() {
    if (_model == null || !_model.isValid()) {
      _model = new HaxeIteratorModel(this);
    }
    return _model;
  }
}
