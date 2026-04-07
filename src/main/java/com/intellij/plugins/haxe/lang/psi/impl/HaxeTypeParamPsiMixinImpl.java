/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeListPart;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeParamPsiMixin;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeContainerStub;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.stubs.IStubElementType;

/**
 * Created by ebishton on 10/18/14.
 */
public class HaxeTypeParamPsiMixinImpl extends HaxeContainerStubPsiElementBase implements HaxeTypeParamPsiMixin {

  public HaxeTypeParamPsiMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeTypeParamPsiMixinImpl(HaxeContainerStub<?> stub, IStubElementType<?, ?> type) {
    super(stub, type);
  }

  @Override
  public PsiTypeParameter[] getTypeParameters() {
    HaxeTypeListPart[] parts = UsefulPsiTreeUtil.getChildrenOfType(this, HaxeTypeListPart.class, null);
    return null != parts ? parts : new PsiTypeParameter[0];
  }

  @Override
  public int getTypeParameterIndex(PsiTypeParameter typeParameter) {
    PsiTypeParameter[] params = getTypeParameters();
    int i = 0;
    for (; i < params.length; ++i) {
      if (typeParameter == params[i]) {
        break;
      }
    }
    return i < params.length ? i : -1;
  }


}
