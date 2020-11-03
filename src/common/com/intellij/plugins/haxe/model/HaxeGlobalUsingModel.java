/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeGlobalUsingModel extends HaxeUsingModel {

  // package access only.
  HaxeGlobalUsingModel(@NotNull PsiElement baseElement) {
    super(baseElement);
  }

  @Override
  @Nullable
  public FullyQualifiedInfo getQualifiedInfo() {
    FullyQualifiedInfo info = super.getQualifiedInfo();
    if (null != info) {
      return info;
    }

    HaxeModel model = null;
    if(getBasePsi() instanceof AbstractHaxePsiClass) {
      model = ((AbstractHaxePsiClass)getBasePsi()).getModel();
    }

    if (getBasePsi() instanceof HaxeFile) {
      model = ((HaxeFile)getBasePsi()).getModel();
    }

    return null == model ? null : model.getQualifiedInfo();
  }

}
