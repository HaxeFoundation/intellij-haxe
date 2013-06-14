package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeBundle;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGenerateGetterSetterAction extends BaseHaxeGenerateAction {
  @Override
  protected BaseHaxeGenerateHandler getGenerateHandler() {
    return new HaxeGenerateAccessorHandler(CreateGetterSetterFix.Strategy.GETTERSETTER) {
      @Override
      protected String getTitle() {
        return HaxeBundle.message("fields.to.generate.getters.setters");
      }
    };
  }
}
