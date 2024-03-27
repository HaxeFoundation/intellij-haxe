package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeEnumArgumentExtractor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumExtractedValue;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeEnumExtractorModel {

  private final HaxeEnumArgumentExtractor extractor;

  public HaxeEnumExtractorModel(@NotNull HaxeEnumArgumentExtractor extractor) {
    this.extractor = extractor;
  }

  public HaxeEnumValueModel getEnumValueModel() {
    PsiElement resolve = resolveEnumValueCached();
    if (resolve instanceof HaxeEnumValueDeclaration declaration) {
      return (HaxeEnumValueModel)declaration.getModel();
    }
    return null;
  }

  @Nullable
  private PsiElement resolveEnumValueCached() {
    return CachedValuesManager.getProjectPsiDependentCache(extractor, HaxeEnumExtractorModel::computeResolveEnumValue).getValue();
  }


  public int findExtractValueIndex(HaxeEnumExtractedValue value) {
      PsiElement[] extractorArguments = getChildrenCached();
      for (int i = 0; i < extractorArguments.length; i++) {
        if (extractorArguments[i] == value) {
          return i;
        }
      }
    return -1;
  }

  public int findExtractValueIndex(HaxeEnumArgumentExtractor value) {
      PsiElement[] extractorArguments = getChildrenCached();
      for (int i = 0; i < extractorArguments.length; i++) {
        if (extractorArguments[i] == value) {
          return i;
        }
      }
    return -1;
  }


  public HaxeGenericResolver getGenericResolver() {
    HaxeClassModel anEnum = getEnumValueModel().getDeclaringEnum();
    HaxeGenericResolver resolver = anEnum != null ? anEnum.getGenericResolver(null) : new HaxeGenericResolver();
    HaxeEnumArgumentExtractor parentExtractor = PsiTreeUtil.getParentOfType(extractor, HaxeEnumArgumentExtractor.class);
    if (parentExtractor != null) {
      HaxeEnumExtractorModel extractorModel =  new HaxeEnumExtractorModel(parentExtractor);
      HaxeGenericResolver parentResolver = extractorModel.getGenericResolver();

      int index = extractorModel.findExtractValueIndex(extractor);
      HaxeEnumValueModel valueModel = extractorModel.getEnumValueModel();
      ResultHolder parameterType = valueModel.getParameterType(index, parentResolver);
      if (parameterType != null && parameterType.isClassType()) {
        return parameterType.getClassType().getGenericResolver();
      }
    }else {
      SpecificHaxeClassReference reference = HaxeResolveUtil.resolveExtractorEnum(extractor);
      if (reference != null) {
        return reference.getGenericResolver();
      }
    }
    return resolver;
  }

  private @NotNull PsiElement[] getChildrenCached() {
    return CachedValuesManager.getProjectPsiDependentCache(extractor, HaxeEnumExtractorModel::computeChildren).getValue();
  }

  private static CachedValueProvider.Result<PsiElement[]> computeChildren(HaxeEnumArgumentExtractor extractor) {
    PsiElement[] children = extractor.getEnumExtractorArgumentList().getChildren();
    PsiElement[] elements = ArrayUtils.addAll(children, extractor);
    return new CachedValueProvider.Result<>(children, (Object[])elements);
  }

  private static CachedValueProvider.Result<PsiElement> computeResolveEnumValue(HaxeEnumArgumentExtractor extractor) {
    PsiElement resolve = extractor.getEnumValueReference().getReferenceExpression().resolve();
    return new CachedValueProvider.Result<>(resolve, extractor, resolve);
  }
}
