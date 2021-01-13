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

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.build.FieldWrapper;
import com.intellij.plugins.haxe.build.IdeaTarget;
import com.intellij.plugins.haxe.lang.psi.HaxeClassResolveResult;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodContext;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil.getResolverSkipAbstractNullScope;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLookupElement extends LookupElement {
  private final HaxeComponentName myComponentName;
  private final HaxeClassResolveResult leftReference;
  private final HaxeMethodContext context;

  public static Collection<HaxeLookupElement> convert(HaxeClassResolveResult leftReferenceResolveResult,
                                                      @NotNull Collection<HaxeComponentName> componentNames,
                                                      @NotNull Collection<HaxeComponentName> componentNamesExtension) {
    final List<HaxeLookupElement> result = new ArrayList<>(componentNames.size());
    for (HaxeComponentName componentName : componentNames) {
      HaxeMethodContext context = null;
      if (componentNamesExtension.contains(componentName)) {
        context = HaxeMethodContext.EXTENSION;
      } else {
        context = HaxeMethodContext.NO_EXTENSION;
      }
      result.add(new HaxeLookupElement(leftReferenceResolveResult, componentName, context));
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
    return myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
    if (myComponentNamePresentation == null) {
      presentation.setItemText(getLookupString());
      return;
    }
    HaxeGenericResolver resolver =  null;
    if(leftReference != null && leftReference.getHaxeClass() != null) {
      resolver = getResolverSkipAbstractNullScope(leftReference.getHaxeClass().getModel(), leftReference.getGenericResolver());
    }

    String presentableText = myComponentNamePresentation.getPresentableText();

    // Check for members: methods and fields
    HaxeBaseMemberModel model = HaxeBaseMemberModel.fromPsi(myComponentName);

    if (model != null) {
      presentableText = model.getPresentableText(context, resolver);

      // Check deprecated modifiers
      if (model instanceof HaxeMemberModel && ((HaxeMemberModel)model).getModifiers().hasModifier(HaxePsiModifier.DEPRECATED)) {
        presentation.setStrikeout(true);
      }

      // Check for non-inherited members to highlight them as intellij-java does
      // @TODO: Self members should be displayed first!
      if (leftReference != null) {
        if (model.getDeclaringClass().getPsi() == leftReference.getHaxeClass()) {
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

  @Override
  public void handleInsert(InsertionContext context) {
    HaxeBaseMemberModel memberModel = HaxeBaseMemberModel.fromPsi(myComponentName);
    boolean hasParams = false;
    boolean isMethod = false;
    if (memberModel != null) {
      if (memberModel instanceof HaxeMethodModel) {
        isMethod = true;
        HaxeMethodModel methodModel = (HaxeMethodModel)memberModel;
        hasParams = !methodModel.getParametersWithContext(this.context).isEmpty();
      }
    }

    if (isMethod) {
      final LookupElement[] allItems = context.getElements();
      final boolean overloadsMatter = allItems.length == 1 && getUserData(FORCE_SHOW_SIGNATURE_ATTR) == null;
      JavaCompletionUtil.insertParentheses(context, this, overloadsMatter, hasParams);
    }
  }

  private static final Key<Boolean> FORCE_SHOW_SIGNATURE_ATTR =
    IdeaTarget.IS_VERSION_15_COMPATIBLE ? new FieldWrapper<Key<Boolean>>(JavaCompletionUtil.class,
                                                                         "FORCE_SHOW_SIGNATURE_ATTR").get(null)
                                        : Key.<Boolean>create("forceShowSignature");

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
