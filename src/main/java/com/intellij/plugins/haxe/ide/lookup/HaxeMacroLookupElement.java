package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeMacroLookupElement extends LookupElement implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  private final HaxeComponentName myComponentName;

  private final HaxeGenericResolver resolver;
  @Getter private final boolean isFunctionType;
  @Getter private HaxeBaseMemberModel model;

  private String presentableText;
  private String tailText;
  private String typeText;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon = null;

  @NotNull
  public static HaxeMacroLookupElement create(@NotNull HaxeComponentName componentName, HaxeGenericResolver resolver) {
    HaxeBaseMemberModel model = HaxeBaseMemberModel.fromPsi(componentName);
    if (model instanceof HaxeMethodModel) {
      return new HaxeMacroLookupElement(componentName, resolver, model, true);
    }
    return new HaxeMacroLookupElement(componentName, resolver, model);
  }


  public HaxeMacroLookupElement(HaxeComponentName name,  HaxeGenericResolver resolver, HaxeBaseMemberModel model) {
    this(name,  resolver, model, false);
  }
  public HaxeMacroLookupElement(HaxeComponentName name,  HaxeGenericResolver resolver, HaxeBaseMemberModel model, boolean functionType) {
    this.myComponentName = name;
    this.resolver = resolver;
    this.model = model;
    this.isFunctionType = functionType;
    calculatePresentation();
  }

  @NotNull
  @Override
  public String getLookupString() {
    return "$" +myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(presentableText);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTypeText(typeText);

    if (tailText != null) presentation.setTailText(tailText, true);

  }

  public void calculatePresentation() {
    presentableText =  getLookupString();

    if (!isFunctionType) {
      final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
      if (myComponentNamePresentation != null) {
        icon = myComponentNamePresentation.getIcon(true);
      }
      else {
        // TODO functionType references should perhaps have its own icon?
        icon = HaxeIcons.Field;
      }
      if (model != null) {
        determineStriketrough();
        evaluateTypeTextAndPriorityBoost();
      }
    }
    // currently defaulting to Variable icon for unspecified types (enum extracted values etc.)
    if (icon == null) {
      //TODO should probably make icons for extracted values etc.
      icon = HaxeIcons.Variable;
    }
  }

  private void evaluateTypeTextAndPriorityBoost() {
    ResultHolder typeHolder = model.getResultType(resolver);

    if (isFunctionType && model instanceof HaxeMethodModel methodModel) {
      SpecificFunctionReference functionType = methodModel.getFunctionType(resolver);
      typeText =  functionType.toPresentationString();
      return;
    }
    if (typeHolder != null && !typeHolder.isUnknown()) {
      typeText = typeHolder.toPresentationString();

      SpecificTypeReference type = typeHolder.tryUnwrapNullType().getType();
      if(type.isExpr() || type.isExprOf()) {
        priority.assignable = 2;
      }
      else {
        String qualifiedName = tryFindQualifiedName(type);
        if(qualifiedName != null && qualifiedName.startsWith("haxe.macro")) {
          priority.assignable = 2;
        }
      }

    }
  }

  private static @Nullable String tryFindQualifiedName(SpecificTypeReference type) {
    if (type instanceof SpecificEnumValueReference valueReference) {
      HaxeClass enumParentClass = valueReference.getEnumClass().getHaxeClass();
      if (enumParentClass != null) {
        return enumParentClass.getQualifiedName();
      }
    }
    if (type instanceof  SpecificHaxeClassReference classReference) {
      HaxeClass haxeClass = classReference.getHaxeClass();
      if(haxeClass != null) {
        return haxeClass.getQualifiedName();
      }
    }
    return "";
  }


  private void determineStriketrough() {
    if (model instanceof HaxeMemberModel && ((HaxeMemberModel)model).getModifiers().hasModifier(HaxeCompilerMetadata.DEPRECATED)) {
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
        hasParams = !methodModel.getParametersWithContext(null).isEmpty();
        isMethod = true;
      }
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
    return myComponentName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof HaxeMacroLookupElement lookupElement) {
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
