
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumArgumentExtractor;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.model.HaxeEnumExtractorModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import lombok.CustomLog;


@CustomLog
public abstract class HaxeEnumArgumentExtractorMixinImpl extends HaxeExpressionImpl implements HaxeModelTarget, HaxeExpression {

  HaxeEnumExtractorModel model;

  public HaxeEnumArgumentExtractorMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeModel getModel() {
    if (model == null || !model.isValid()) {
      if (this instanceof HaxeEnumArgumentExtractor argumentExtractor)
      model = new HaxeEnumExtractorModel(argumentExtractor);
    }
    return model;
  }
}
