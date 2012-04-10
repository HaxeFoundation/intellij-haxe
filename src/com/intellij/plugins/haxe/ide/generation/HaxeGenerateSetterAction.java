package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeBundle;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGenerateSetterAction extends BaseHaxeGenerateAction {
  @Override
  protected BaseHaxeGenerateHandler getGenerateHandler() {
    return new HaxeGenerateAccessorHandler(CreateGetterSetterFix.Strategy.SETTER){
      @Override
      protected String getTitle() {
        return HaxeBundle.message("fields.to.generate.setters");
      }
    };
  }
}
