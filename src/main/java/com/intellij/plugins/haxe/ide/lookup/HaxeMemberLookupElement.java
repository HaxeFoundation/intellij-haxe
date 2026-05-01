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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.NO_COMPLETION;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeMemberLookupElement extends LookupElement implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  @Getter private final SpecificTypeReference leftReference;
  @Getter private final HaxeGenericResolver resolver;
  @Getter private final HaxeBaseMemberModel model;
  @Getter private final HaxeMethodContext context;
  @Getter private final boolean isFunctionType;

  private String presentableText;
  private String tailText;
  private String typeText;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon = null;
  private boolean presentationCalculated = false;


  // TODO create visibility "evaluator" to filter members
  //  we should be shown private members from the same class/module
  //  and it should consider @:Allow / @:access / @PrivateAccess  annotations (see HaxeAccessAnnotator)
  public static Set<HaxeComponentName> filterCompletable(Set<HaxeComponentName> componentNames) {

    return componentNames.stream().filter(componentName -> {
      if (componentName.getParent() instanceof HaxeFieldDeclaration fieldDeclaration) {
        //if(hidePrivate && !fieldDeclaration.isPublic()) return false;
        if(fieldDeclaration.hasCompileTimeMetadata(NO_COMPLETION)) return false;
      }
      if (componentName.getParent() instanceof HaxeMethodDeclaration methodDeclaration) {
        //if(hidePrivate && !methodDeclaration.isPublic()) return false;
        if(methodDeclaration.hasCompileTimeMetadata(NO_COMPLETION)) return false;
      }
      // ignore constructors for now, (completion creates `new()`)
      if (componentName.textMatches(HaxeTokenTypes.ONEW.toString())) {
        return false;
      }
      return true;
    }).collect(Collectors.toSet());

  }

  public static List<HaxeMemberLookupElement> createLocalMembers(SpecificHaxeClassReference leftReference,
                                                                 HaxeGenericResolver resolver,
                                                                 Set<HaxeComponentName> componentNames) {

    return create(leftReference, resolver, componentNames, HaxeMethodContext.NO_EXTENSION, true, true);
  }
  public static List<HaxeMemberLookupElement> createClassMembers(SpecificHaxeClassReference leftReference,
                                                                 HaxeGenericResolver resolver,
                                                                 Set<HaxeComponentName> componentNames) {

    return create(leftReference, resolver, componentNames, HaxeMethodContext.NO_EXTENSION, true,true);
  }

  public static List<HaxeMemberLookupElement> createClassMembers(SpecificHaxeClassReference leftReference,
                                                                 HaxeGenericResolver resolver,
                                                                 Set<HaxeComponentName> componentNames,
                                                                 boolean addFunctionReference,
                                                                 boolean addFunctionCallExpression
  ) {

    return create(leftReference, resolver, componentNames, HaxeMethodContext.NO_EXTENSION, addFunctionReference,addFunctionCallExpression);
  }

  public static List<HaxeMemberLookupElement> createModuleMembers(Set<HaxeComponentName> componentNames) {

    return create(null, new HaxeGenericResolver(), componentNames, HaxeMethodContext.NO_EXTENSION, true,true);
  }

  public static List<HaxeMemberLookupElement> createExtensionMembers(SpecificTypeReference leftReference,
                                                                     HaxeGenericResolver resolver,
                                                                     Set<HaxeComponentName> componentNames) {

    return create(leftReference, resolver, componentNames, HaxeMethodContext.EXTENSION, false, true);
  }

  public static List<HaxeMemberLookupElement> create(SpecificTypeReference leftReference,
                                                     HaxeGenericResolver resolver,
                                                     Set<HaxeComponentName> componentNames,
                                                     HaxeMethodContext context,
                                                     boolean addFunctionReference,
                                                     boolean addFunctionCallExpression
  ) {

    Set<HaxeComponentName> filtered = filterCompletable(componentNames);
    List<HaxeMemberLookupElement> lookupElements = new ArrayList<>();
    for (HaxeComponentName name : filtered) {
      // TODO check if stub-friendly code?
      if(name.getParent() instanceof HaxeModelTarget modelTarget) {
        HaxeModel model = modelTarget.getModel();
        if(model instanceof HaxeBaseMemberModel memberModel) {
          if( memberModel instanceof HaxeMethodModel) {
            if(addFunctionReference) {
              lookupElements.add(new HaxeMemberLookupElement(memberModel, resolver, context, leftReference, true));
            }
            if(addFunctionCallExpression) {
              lookupElements.add(new HaxeMemberLookupElement(memberModel, resolver, context, leftReference, false));
            }
          }
          else { //HaxeLocalVarModel ++
            lookupElements.add(new HaxeMemberLookupElement(memberModel, resolver, context, leftReference, false));
          }
        }
      }
    }
    return lookupElements;
  }




  public HaxeMemberLookupElement(@NotNull HaxeBaseMemberModel memberModel,
                                 @NotNull HaxeGenericResolver memberResolver,
                                 @NotNull HaxeMethodContext context,
                                 @Nullable SpecificTypeReference leftReference,
                                 boolean functionType) {
    this.model = memberModel;
    this.resolver = memberResolver;
    this.context = context;
    this.leftReference = leftReference;
    this.isFunctionType = functionType;
  }



  @NotNull
  @Override
  public String getLookupString() {
    return model.getName();
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
      HaxeComponentType type = HaxeComponentType.typeOf(model.getNamedComponentPsi());
      if(type != null) {
        icon = type.getCompletionIcon();
      }
    } else {
      // TODO functionType references should perhaps have its own icon?
      icon = HaxeIcons.Field;
    }
    if (model != null) {
      determineStrikethrough();
      determineBold();

      evaluateTailText();
      evaluateTypeText();
    }
  }

  private void evaluateTypeText() {
    if(model instanceof HaxeLocalValueElementModel localValueModel){
      ResultHolder type = localValueModel.getVariableType();
      if(type != null && !type.isUnknown()) {
        typeText = type.toPresentationString();
      }
    }
    if (isFunctionType && model instanceof HaxeMethodModel methodModel) {
      if(leftReference instanceof SpecificHaxeClassReference classReference) {
        HaxeClass haxeClass = classReference.getHaxeClass();
        HaxeClassModel methodsParentClass = model.getDeclaringClass();
        if (methodsParentClass != null) {
          HaxeGenericResolver translatedResolver = resolver.translateFromTo(haxeClass, methodsParentClass.haxeClass);
          SpecificFunctionReference functionType = methodModel.getFunctionType(resolver);
          SpecificFunctionReference resolve = translatedResolver.resolve(functionType);
          if (resolve != null && !resolve.isUnknown()) {
            typeText = resolve.toPresentationString();
            return;
          }
        }
      }
      SpecificFunctionReference functionType = methodModel.getFunctionType(resolver);
      typeText =  functionType.toPresentationString();
      return;
    }
    if (leftReference instanceof SpecificHaxeClassReference classReference) {
      HaxeClass haxeClass = classReference.getHaxeClass();
      HaxeClassModel declaringClass = model.getDeclaringClass();
      if (declaringClass != null) {
        HaxeGenericResolver translatedResolver = resolver.translateFromTo(haxeClass, declaringClass.haxeClass);
        ResultHolder type = model.getResultType(translatedResolver);
        // TODO mlo: figure out why this is necessary (would expect getResultType to handle this)
        type = translatedResolver.resolve(type);

        if (type != null && !type.isUnknown()) {
          typeText = type.toPresentationString();
          return;
        }
      }
    }

    ResultHolder type = model.getResultType(resolver);
    if (type != null && !type.isUnknown()) {
      typeText = type.toPresentationString();
    }
  }

  private void evaluateTailText() {
    if (model instanceof HaxeMethodModel && !isFunctionType) {
      tailText = "(" + getParameterListAsText() + ")";
    }
  }

  private @NotNull String getParameterListAsText() {
    if (leftReference != null){
      return HaxePresentableUtil.getPresentableParameterList(model.getNamedComponentPsi(), resolver, true, false);
    }else {
      return HaxePresentableUtil.getPresentableParameterList(model.getNamedComponentPsi());
    }
  }

  private void determineBold() {
    // Check for non-inherited members to highlight them as intellij-java does
    if (leftReference  instanceof SpecificHaxeClassReference classReference) {
      HaxeClassModel declaringClass = model.getDeclaringClass();
      if (declaringClass!= null && declaringClass.getPsi() == classReference.getHaxeClass()) {
        bold = true;
      }
    }
  }

  private void determineStrikethrough() {
    if (model instanceof HaxeMemberModel && ((HaxeMemberModel)model).getModifiers().hasModifier(HaxeCompilerMetadata.DEPRECATED)) {
          strikeout = true;
        }
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    boolean hasParams = false;
    boolean isMethod = false;
      if (model instanceof HaxeMethodModel methodModel)  {
        hasParams = !methodModel.getParametersWithContext(this.context).isEmpty();
        isMethod = true;
    }

    if (isMethod && !isFunctionType) {
      final LookupElement[] allItems = context.getElements();
      final boolean overloadsMatter = allItems.length == 1 && getUserData(JavaCompletionUtil.FORCE_SHOW_SIGNATURE_ATTR) == null;
      JavaCompletionUtil.insertParentheses(context, this, overloadsMatter, hasParams);
    }
  }


  @NotNull
  @Override
  public Object getObject() {
    return model.getNamePsi();
  }

}
