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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodContext;
import com.intellij.plugins.haxe.model.HaxeModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLookupElement extends LookupElement {
  private final HaxeComponentName myComponentName;
  private final HaxeClassResolveResult leftReference;
  private final HaxeMethodContext context;

  public static Collection<HaxeLookupElement> convert(HaxeClassResolveResult leftReference, @NotNull Collection<HaxeComponentName> componentNames, @NotNull Collection<HaxeComponentName> componentNamesExtension) {
    final List<HaxeLookupElement> result = new ArrayList<HaxeLookupElement>(componentNames.size());
    for (HaxeComponentName componentName : componentNames) {
      HaxeMethodContext context = null;
      if (componentNamesExtension.contains(componentName)) {
        context = HaxeMethodContext.EXTENSION;
      } else {
        context = HaxeMethodContext.NO_EXTENSION;
      }
      result.add(new HaxeLookupElement(leftReference, componentName, context));
    }
    return result;
  }

  public HaxeLookupElement(HaxeClassResolveResult leftReference, HaxeComponentName name, HaxeMethodContext context) {
    this.leftReference = leftReference;
    this.myComponentName = name;
    this.context = context;
  }

  @NotNull
  @Override
  public String getLookupString() {
    final String result = myComponentName.getName();
    if (result != null) {
      return result;
    }
    return myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
    if (myComponentNamePresentation == null) {
      presentation.setItemText(getLookupString());
      return;
    }

    String presentableText = myComponentNamePresentation.getPresentableText();

    // Check for members: methods and fields
    HaxeMemberModel member = HaxeMemberModel.fromPsi(myComponentName);

    if (member != null) {
      presentableText = member.getPresentableText(context);

      // Check deprecated modifiers
      if (member.getModifiers().hasModifier(HaxeModifierType.DEPRECATED)) {
        presentation.setStrikeout(true);
      }

      // Check for non-inherited members to highlight them as intellij-java does
      // @TODO: Self members should be displayed first!
      if (leftReference != null) {
        if (member.getDeclaringClass().getPsi() == leftReference.getHaxeClass()) {
          presentation.setItemTextBold(true);
        }
      }
    }

    presentation.setItemText(presentableText);
    presentation.setIcon(myComponentNamePresentation.getIcon(true));
    final String pkg = myComponentNamePresentation.getLocationString();
    if (StringUtil.isNotEmpty(pkg)) {
      presentation.setTailText(" " + pkg, true);
    }
  }

  @NotNull
  @Override
  public Object getObject() {
    return myComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeLookupElement)) return false;

    return myComponentName.equals(((HaxeLookupElement)o).myComponentName);
  }

  @Override
  public int hashCode() {
    return myComponentName.hashCode();
  }
}
