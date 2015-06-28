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

import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HaxeCustomOperatorsModel {
  private final Map<String, HaxeCustomOperatorModel> operators = new LinkedHashMap<String, HaxeCustomOperatorModel>();

  static public HaxeCustomOperatorsModel fromClass(HaxeClassModel clazz) {
    final HaxeCustomOperatorsModel operators = new HaxeCustomOperatorsModel();
    if (clazz.isAbstract()) {
      for (HaxeMethodModel method : clazz.getMethodsSelf()) {
        final HaxeMetasModel metas = method.getMetas();
        final HaxeMetaModel meta = metas.getMeta("@:op");
        if (meta != null) {
          final List<PsiJavaTokenImpl> tokens = UsefulPsiTreeUtil.getDescendantsOfType(meta.getExpressionList(), PsiJavaTokenImpl.class);
          if (tokens.size() == 3) {
            String operator = tokens.get(1).getText();
            operators.get(operator).addMethod(HaxeOperatorType.BINARY, method);
          } else if (tokens.size() == 2) {
            if (tokens.get(0).getText().equals("A")) {
              operators.get(tokens.get(1).getText()).addMethod(HaxeOperatorType.POST, method);
            } else {
              operators.get(tokens.get(0).getText()).addMethod(HaxeOperatorType.PRE, method);
            }
          }
        }
      }
    }
    return operators;
  }

  public HaxeCustomOperatorModel get(String operator) {
    if (!operators.containsKey(operator)) {
      operators.put(operator, new HaxeCustomOperatorModel(operator));
    }
    return operators.get(operator);
  }
}
