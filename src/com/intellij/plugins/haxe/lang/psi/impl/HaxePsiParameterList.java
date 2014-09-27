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
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiParameterList extends HaxeParameterListImpl implements HaxeParameterList, PsiParameterList {

  public HaxePsiParameterList(ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public HaxePsiParameter[] getParameters() {
    HaxePsiParameter[] haxePsiParameters = {};
    List<HaxeParameter> paramList = super.getParameterList();
    if (null != paramList) {
      haxePsiParameters = new HaxePsiParameter[paramList.size()];
      int index = 0;
      for (HaxeParameter param : paramList) {
        haxePsiParameters[index++] = new HaxePsiParameter(param);
      }
    }
    return haxePsiParameters;
  }

  @Override
  public int getParameterIndex(PsiParameter parameter) {
    int index = 0;
    List<HaxeParameter> paramList = super.getParameterList();
    if (null != paramList) {
      index = paramList.indexOf(parameter);
    }
    return index;
  }

  @Override
  public int getParametersCount() {
    int count = 0;
    List<HaxeParameter> paramList = super.getParameterList();
    if (null != paramList) {
      count = paramList.size();
    }
    return count;
  }
}
