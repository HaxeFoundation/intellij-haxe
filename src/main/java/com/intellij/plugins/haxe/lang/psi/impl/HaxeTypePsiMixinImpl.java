/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypePsiMixin;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.PsiType;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 10/9/14.
 */
@CustomLog
public class HaxeTypePsiMixinImpl extends HaxeContainerStubPsiElementBase implements HaxeTypePsiMixin {

  static {
    log.setLevel(LogLevel.DEBUG);
  }

  public HaxeTypePsiMixinImpl(ASTNode node) {
    super(node);
  }

  public HaxeTypePsiMixinImpl(HaxeEmptyContainerStub<?> stub, IElementType type) {
    super(stub, type);
  }


  @Nullable
  @Override
  public PsiType getPsiType() {
    return (this instanceof HaxeType) ? new HaxePsiTypeAdapter((HaxeType)this) : null;
  }

}
