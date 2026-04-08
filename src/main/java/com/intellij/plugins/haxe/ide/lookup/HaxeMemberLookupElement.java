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
package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.NO_COMPLETION;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeMemberLookupElement extends LookupElement  implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  private final HaxeComponentName myComponentName;
  private final HaxeResolveResult leftReference;
  private final HaxeMethodContext context;

  private final HaxeGenericResolver resolver;
  @Getter private final boolean isFunctionType;
  @Getter private HaxeBaseMemberModel model;

  private String presentableText;
  private String tailText;
  private String typeText;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon = null;
  private boolean presentationCalculated = false;

  @NotNull
  public static Collection<HaxeMemberLookupElement> convert(HaxeResolveResult leftReferenceResolveResult,
                                                            @NotNull Collection<HaxeComponentName> componentNames,
                                                            @NotNull Collection<HaxeComponentName> componentNamesExtension,
                                                            @Nullable  HaxeGenericResolver resolver) {
   return convert(leftReferenceResolveResult,componentNames, componentNamesExtension, resolver, false, false);
  }
  @NotNull
  public static Collection<HaxeMemberLookupElement> convert(HaxeResolveResult leftReferenceResolveResult,
                                                            @NotNull Collection<HaxeComponentName> componentNames,
                                                            @NotNull Collection<HaxeComponentName> componentNamesExtension,
                                                            @Nullable  HaxeGenericResolver resolver,
                                                            boolean excludeCallSuggestions, boolean excludeMethodReferenceSuggestions) {
    final List<HaxeMemberLookupElement> result = new ArrayList<>(componentNames.size());
    if (resolver == null) resolver = new HaxeGenericResolver();
    for (HaxeComponentName componentName : componentNames) {
      HaxeMethodContext context = null;
      boolean shouldBeIgnored = false;
      if (componentNamesExtension.contains(componentName)) {
        context = HaxeMethodContext.EXTENSION;
      } else {
        context = HaxeMethodContext.NO_EXTENSION;
      }

      // TODO figure out if  @:noUsing / NO_USING should be filtered

      if(componentName.getParent() instanceof HaxeFieldDeclaration fieldDeclaration) {
        shouldBeIgnored = fieldDeclaration.hasCompileTimeMetadata(NO_COMPLETION) ;
      }
      if(componentName.getParent() instanceof HaxeMethodDeclaration methodDeclaration) {
        shouldBeIgnored = methodDeclaration.hasCompileTimeMetadata(NO_COMPLETION) ;
      }
      // ignore constructors for now, (completion creates `new()`)
      if (componentName.textMatches("new")) {
        shouldBeIgnored = true;
      }
      if (!shouldBeIgnored) {
        HaxeBaseMemberModel model = HaxeBaseMemberModel.fromPsi(componentName);
        if (model != null) {
          HaxeClassModel classModel = model.getDeclaringClass();
          if (classModel != null && leftReferenceResolveResult != null) {
            HaxeClass currentClass = leftReferenceResolveResult.getHaxeClass();
            HaxeClass membersClass = classModel.haxeClass;
            if(currentClass != null && !resolver.isEmpty())  resolver = resolver.translateFromTo(currentClass, membersClass);
          }
          if (model instanceof HaxeMethodModel) {
            // adding functionType in addition to method call
            if(!excludeMethodReferenceSuggestions) {
              result.add(new HaxeMemberLookupElement(leftReferenceResolveResult, componentName, context, resolver, model, true));
            }
            if(!excludeCallSuggestions) {
              result.add(new HaxeMemberLookupElement(leftReferenceResolveResult, componentName, context, resolver, model));
            }
            continue;
          }
        }
        result.add(new HaxeMemberLookupElement(leftReferenceResolveResult, componentName, context, resolver, model));
      }
    }
    return result;
  }


  public HaxeMemberLookupElement(HaxeResolveResult leftReference, HaxeComponentName name, HaxeMethodContext context, HaxeGenericResolver resolver, HaxeBaseMemberModel model) {
    this(leftReference, name, context, resolver, model, false);
  }
  public HaxeMemberLookupElement(HaxeResolveResult leftReference, HaxeComponentName name, HaxeMethodContext context, HaxeGenericResolver resolver, HaxeBaseMemberModel model, boolean functionType) {
    this.leftReference = leftReference;
    this.myComponentName = name;
    this.context = context;
    this.resolver = resolver;
    this.model = model;
    this.isFunctionType = functionType;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    if (!presentationCalculated) {
      calculatePresentation();
      presentationCalculated = true;
    }
    presentation.setItemText(presentableText);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTypeText(typeText);

    if (tailText != null) presentation.setTailText(tailText, true);

  }

  public void calculatePresentation() {
    presentableText = getLookupString();

    if (!isFunctionType) {
      final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
      if (myComponentNamePresentation == null) return;
      icon = myComponentNamePresentation.getIcon(true);
    } else {
      // TODO functionType references should perhaps have its own icon?
      icon = HaxeIcons.Field;
    }
    if (model != null) {
      determineStriketrough();
      determineBold();

      evaluateTailText();
      evaluateTypeText();
    }
  }

  private void evaluateTypeText() {
    if (isFunctionType && model instanceof HaxeMethodModel methodModel) {
      SpecificFunctionReference functionType = methodModel.getFunctionType(resolver);
      typeText =  functionType.toPresentationString();
      return;
    }
    ResultHolder type = model.getResultType(resolver);
    if (type != null && !type.isUnknown()) {
      typeText = type.toPresentationString();
    }
  }

  private void evaluateTailText() {
    if (model instanceof  HaxeMethodModel && !isFunctionType) {
      if (leftReference != null) {
        tailText = "(" + getParameterListAsText() + ")";
      }
    }
  }

  private @NotNull String getParameterListAsText() {
    if (leftReference != null){
      return HaxePresentableUtil.getPresentableParameterList(model.getNamedComponentPsi(), leftReference.getSpecialization(), true, false);
    }else {
      return HaxePresentableUtil.getPresentableParameterList(model.getNamedComponentPsi());
    }
  }

  private void determineBold() {
    // Check for non-inherited members to highlight them as intellij-java does
    if (leftReference != null) {
      HaxeClassModel declaringClass = model.getDeclaringClass();
      if (declaringClass!= null && declaringClass.getPsi() == leftReference.getHaxeClass()) {
        bold = true;
      }
    }
  }

  private void determineStriketrough() {
    if (model instanceof HaxeMemberModel && ((HaxeMemberModel)model).getModifiers().hasModifier(HaxePsiModifier.DEPRECATED)) {
          strikeout = true;
        }
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    HaxeBaseMemberModel memberModel = HaxeBaseMemberModel.fromPsi(myComponentName);
    boolean hasParams = false;
    boolean isMethod = false;
    if (memberModel != null) {
      if (memberModel instanceof HaxeMethodModel methodModel)  {
        hasParams = !methodModel.getParametersWithContext(this.context).isEmpty();
        isMethod = true;
      }
    }

    if (isMethod && !isFunctionType) {
      final LookupElement[] allItems = context.getElements();
      final boolean overloadsMatter = allItems.length == 1 && getUserData(JavaCompletionUtil.FORCE_SHOW_SIGNATURE_ATTR) == null;
      JavaCompletionUtil.insertParentheses(context, this, overloadsMatter, hasParams);
    }
  }

  @Override
  public PrioritizedLookupElement<LookupElement> toPrioritized() {
    return (PrioritizedLookupElement<LookupElement>)PrioritizedLookupElement.withPriority(this, priority.calculate());
  }



  @NotNull
  @Override
  public Object getObject() {
    return myComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof HaxeMemberLookupElement lookupElement) {
      return myComponentName.equals(lookupElement.myComponentName) && lookupElement.isFunctionType == isFunctionType;
    }else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return myComponentName.hashCode();
  }
}
