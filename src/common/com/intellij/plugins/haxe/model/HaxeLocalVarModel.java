/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;

import static com.intellij.plugins.haxe.util.HaxeCollectionUtil.firstOrNull;

public class HaxeLocalVarModel extends HaxeMemberModel {

  private HaxeLocalVarDeclaration element;

  public HaxeLocalVarModel(HaxeLocalVarDeclaration element) {
    super(element, element, UsefulPsiTreeUtil.getChild(element, HaxeLocalVarDeclarationPart.class));
    this.element = element;
  }

  @Override
  public HaxeClassModel getDeclaringClass() {
    final HaxeClass hClass = (HaxeClass)((HaxeLocalVarDeclaration)element).getContainingClass();
    return hClass != null ? hClass.getModel() : null;
  }
  @Override
  public ResultHolder getResultType() {
    final HaxeLocalVarDeclarationPart part = UsefulPsiTreeUtil.getChild(element, HaxeLocalVarDeclarationPart.class);
    final HaxeTypeTag typeTag = part != null ? part.getTypeTag() : null;
    final HaxeTypeOrAnonymous type = typeTag != null ? firstOrNull(typeTag.getTypeOrAnonymousList()) : null;
    return type != null ? HaxeTypeResolver.getTypeFromTypeOrAnonymous(type) : null;

  }
  @Override
  public String getPresentableText(HaxeMethodContext context) {
    final ResultHolder type = getResultType();
    return type == null ? this.getName() : this.getName() + ":" + type;
  }

}
