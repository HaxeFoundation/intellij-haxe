/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.HaxeBundle;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGenerateGetterAction extends BaseHaxeGenerateAction {
  @Override
  protected BaseHaxeGenerateHandler getGenerateHandler() {
    return new HaxeGenerateAccessorHandler(CreateGetterSetterFix.Strategy.GETTER) {
      @Override
      protected String getTitle() {
        return HaxeBundle.message("fields.to.generate.getters");
      }
    };
  }
}
