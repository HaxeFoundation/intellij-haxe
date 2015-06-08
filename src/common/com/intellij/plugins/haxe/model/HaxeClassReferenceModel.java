/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;

public class HaxeClassReferenceModel {
  public HaxeType type;

  public HaxeClassReferenceModel(HaxeType type) {
    this.type = type;
  }

  public HaxeType getPsi() {
    return type;
  }

  private HaxeClass _clazz;
  public HaxeClassModel getHaxeClass() {
    if (_clazz == null) {
      _clazz = HaxeResolveUtil.getHaxeClassResolveResult(type).getHaxeClass();
    }
    return (_clazz != null) ? _clazz.getModel() : null;
  }
}
