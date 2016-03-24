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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclarationPart;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HaxeConstructorHandler extends BaseHaxeGenerateHandler {
  @Override
  protected BaseCreateMethodsFix createFix(final HaxeClass haxeClass) {
    return new BaseCreateMethodsFix<HaxeNamedComponent>(haxeClass) {
      @Override
      protected void processElements(Project project, Set<HaxeNamedComponent> elementsToProcess) {
        HaxeClassModel clazz = haxeClass.getModel();

        if (clazz.getConstructorSelf() != null) {
          // Constructor already exists
          return;
        }

        HaxeMethodModel parentConstructor = clazz.getParentConstructor();

        ArrayList<ParamElement> params = new ArrayList<ParamElement>();
        ArrayList<ParamElement> paramsSuper = new ArrayList<ParamElement>();
        ArrayList<ParamElement> paramsWrite = new ArrayList<ParamElement>();
        if (parentConstructor != null) {
          for (HaxeParameterModel param : parentConstructor.getParameters()) {
            ParamElement element = new ParamElement(param.getName(), param.getType().toStringWithoutConstant());
            params.add(element);
            paramsSuper.add(element);
          }
        }
        for (HaxeNamedComponent node : elementsToProcess) {
          ParamElement element = new ParamElement(node.getName(), HaxeTypeResolver
            .getFieldOrMethodReturnType((AbstractHaxeNamedComponent)node).toStringWithoutConstant());
          params.add(element);
          paramsWrite.add(element);
        }

        System.out.println(parentConstructor);

        String out = "";
        out += "public function new(";
        boolean first = true;

        PsiElement anchor = null;

        List<HaxeFieldModel> fields = clazz.getFields();
        for (HaxeFieldModel field : fields) {
          anchor = field.getPsi();
        }

        for (ParamElement param : params) {
          if (!first) {
            out += ",";
          } else {
            first = false;
          }
          out += param.name;
          out += ":";
          out += param.type;
        }

        out += ") {\n";
        if (parentConstructor != null) {
          out += "super(";
          int count = 0;
          for (ParamElement param : paramsSuper) {
            if (count > 0) {
              out += ",";
            }
            out += param.name;
            count++;
          }
          out += ");\n";
        }
        for (ParamElement param : paramsWrite) {
          out += "this." + param.name + " = " + param.name + ";\n";
        }
        out += "}\n\n";

        doAddMethodsForOne(project, out, anchor);
        //super.processElements(project, elementsToProcess);
      }

      @Override
      protected String buildFunctionsText(HaxeNamedComponent e) {
        return null;
      }
    };
  }

  @Override
  protected String getTitle() {
    return HaxeBundle.message("fields.to.generate.constructor");
  }

  @Override
  void collectCandidates(HaxeClass haxeClass, List<HaxeNamedComponent> candidates) {
    final List<HaxeNamedComponent> subComponents = HaxeResolveUtil.getNamedSubComponents(haxeClass);
    final Map<String, HaxeNamedComponent> componentMap = HaxeResolveUtil.namedComponentToMap(subComponents);

    for (HaxeNamedComponent haxeNamedComponent : subComponents) {
      if (!(haxeNamedComponent instanceof HaxeVarDeclarationPart)) continue;
      if (haxeNamedComponent.isStatic()) continue;

      //if (!myStrategy.accept(haxeNamedComponent.getName(), componentMap.keySet())) continue;

      candidates.add(haxeNamedComponent);
    }
  }
}

class ParamElement {
  public String name;
  public String type;

  public ParamElement(String name, String type) {
    this.name = name;
    this.type = type;
  }
}