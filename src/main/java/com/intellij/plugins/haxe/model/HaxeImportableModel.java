/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class HaxeImportableModel implements HaxeExposableModel {
  protected final PsiElement basePsi;

  protected HaxeImportableModel(@NotNull PsiElement element) {
    this.basePsi = element;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    HaxeReferenceExpression referenceExpression = getReferenceExpression();
    return referenceExpression == null ? null : new FullyQualifiedInfo(referenceExpression);
  }

  @Nullable
  public abstract HaxeReferenceExpression getReferenceExpression();

  @NotNull
  public List<HaxeModel> getExposedMembersInternal() {
    FullyQualifiedInfo qualifiedInfo = getQualifiedInfo();
    List<HaxeModel> result;
    result = HaxeProjectModel.fromElement(basePsi).resolve(qualifiedInfo, basePsi.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel firstItem = result.get(0);
      if (firstItem instanceof HaxeFileModel || firstItem instanceof HaxePackageModel) {
        result = ((HaxeExposableModel)firstItem).getExposedMembers();
      }
    }
    return result == null ? Collections.emptyList() : result;
  }

  public static CachedValueProvider<List<HaxeModel>> exposedMembersCollector(final HaxeImportableModel importableModel) {
    return () -> {
      List<HaxeModel> exposedMembers = importableModel.getExposedMembersInternal();
      PsiElement[] dependencies = new PsiElement[exposedMembers.size() + 1];
      int i = 0;
      dependencies[i++] = importableModel.getBasePsi();
      for (HaxeModel xMember : exposedMembers) {
        dependencies[i++] = xMember.getBasePsi();
      }
      return new CachedValueProvider.Result<List<HaxeModel>>(exposedMembers, (Object[])dependencies);
    };
  }

  @NotNull
  @Override
  public List<HaxeModel> getExposedMembers() {
    return CachedValuesManager.getCachedValue(getBasePsi(), exposedMembersCollector(this));
  }

  @Nullable
  protected HaxeModel getExposedMember(String name) {
    List<? extends HaxeModel> members = getExposedMembers();
    if (members.isEmpty()) return null;
    for (HaxeModel model : members) {
      if (name.equals(model.getName())) {
        return model;
      }
    }
    // if not found with normal  name-search try to search typedefs and enums
    for (HaxeModel model : members) {
      if (model instanceof HaxeEnumModel enumModel) {
        HaxeModel memberFromEnum = getExposedMemberFromEnum(name, model, enumModel);
        if (memberFromEnum != null) return memberFromEnum;
      }

      else if (model instanceof HaxeClassModel classModel) {
        if (classModel.haxeClass instanceof HaxeTypedefDeclaration typedefDeclaration) {
          HaxeModel memberFromEnum = getExposedMemberFromTypeDefReference(name, model, typedefDeclaration);
          if (memberFromEnum != null) return memberFromEnum;
        }
      }
    }

    return null;
  }

  //TODO mlo consider making typdefs some sort of HaxeImportableModel when its a type reference
  @Nullable
  private static HaxeModel getExposedMemberFromTypeDefReference(String name, HaxeModel model, HaxeTypedefDeclaration typedefDeclaration) {
    HaxeTypeOrAnonymous typeOrAnonymous = typedefDeclaration.getTypeOrAnonymous();
    if (typeOrAnonymous != null) {
      HaxeType type = typeOrAnonymous.getType();
      if (type != null) {
        PsiElement resolve = type.getReferenceExpression().resolve();
        if (resolve instanceof  HaxeEnumDeclaration enumDeclaration) {
          if (enumDeclaration.getModel() instanceof  HaxeEnumModel enumModel) {
            HaxeModel memberFromEnum = getExposedMemberFromEnum(name, model, enumModel);
            if (memberFromEnum != null) return memberFromEnum;
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static HaxeModel getExposedMemberFromEnum(String name, HaxeModel model, HaxeEnumModel enumModel) {
    for (HaxeEnumValueModel enumModelValue : enumModel.getValues()) {
      if (name.equals(enumModelValue.getName())) {
        return enumModelValue;
      }
    }
    return null;
  }

  @Nullable
  public abstract PsiElement exposeByName(String name);

  @Override
  public String getName() {
    return null;
  }

  @NotNull
  @Override
  public PsiElement getBasePsi() {
    return this.basePsi;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeImportableModel)) return false;
    HaxeImportableModel model = (HaxeImportableModel)o;
    return getBasePsi().equals(model.getBasePsi());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBasePsi());
  }
}
