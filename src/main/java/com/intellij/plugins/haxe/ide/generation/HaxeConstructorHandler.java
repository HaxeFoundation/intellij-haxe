/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

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
        if (clazz.isAbstractType()) {
          SpecificTypeReference underlyingType = clazz.getUnderlyingType();
          String parameter  = underlyingType == null || underlyingType.isUnknown() ? "value" :  "value:" +underlyingType.toPresentationString();
          String out = "public function new(" + parameter + ") {\nthis = value;\n}";
           anchor = clazz.getBodyPsi().getFirstChild();
          doAddMethodsForOne(project, out, anchor);
          return;
        }

        HaxeMethodModel parentConstructor = clazz.getParentConstructor(null);

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


        boolean first = true;

        PsiElement anchor = clazz.getBodyPsi() != null ? PsiTreeUtil.getDeepestVisibleFirst(clazz.getBodyPsi()) : null;

        List<HaxeFieldModel> fields = clazz.getFields();
        for (HaxeFieldModel field : fields) {
          anchor = field.getBasePsi();
        }

        String out = createClassConstrcutorString( params, first, parentConstructor, paramsSuper, paramsWrite);
        doAddMethodsForOne(project, out, anchor);
        //super.processElements(project, elementsToProcess);
      }

      private static @NotNull String createClassConstrcutorString(ArrayList<ParamElement> params, boolean first, HaxeMethodModel parentConstructor, ArrayList<ParamElement> paramsSuper, ArrayList<ParamElement> paramsWrite) {
        String out = "public function new(";
        for (ParamElement param : params) {
          if (!first) {
            out += ",";
          } else {
            first = false;
          }
          out += param.name;
          if(!param.type.equals("unknown")) {
            out += ":";
            out += param.type;
          }
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
        return out;
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
    final List<HaxeNamedComponent> subComponents = HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(haxeClass);
    final Map<String, HaxeNamedComponent> componentMap = HaxeNamedSubComponentUtil.namedComponentToMap(subComponents);

    for (HaxeNamedComponent haxeNamedComponent : subComponents) {
      if (!(haxeNamedComponent instanceof HaxeFieldDeclaration)) continue;
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