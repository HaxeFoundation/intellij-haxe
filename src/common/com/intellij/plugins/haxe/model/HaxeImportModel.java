/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HaxeImportModel implements HaxeExposableModel {
  private final HaxeImportStatement basePsi;
  private final CachedValuesManager cacheManager;

  public HaxeImportModel(@NotNull HaxeImportStatement importStatement) {
    this.basePsi = importStatement;
    this.cacheManager = CachedValuesManager.getManager(importStatement.getProject());
  }

  @NotNull
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    HaxeReferenceExpression referenceExpression = getReferenceExpression();
    return new FullyQualifiedInfo(referenceExpression);
  }

  public boolean hasAlias() {
    return basePsi.getAlias() != null;
  }

  public boolean hasWildcard() {
    return basePsi.getWildcard() != null;
  }

  @Nullable
  public String getAliasName() {
    HaxeIdentifier alias = basePsi.getAlias();
    if (alias == null) return null;
    return alias.getText();
  }

  public HaxeReferenceExpression getReferenceExpression() {
    return basePsi.getReferenceExpression();
  }

  @NotNull
  public List<HaxeModel> getExposedMembersInternal() {
    FullyQualifiedInfo qualifiedInfo = getQualifiedInfo();
    List<HaxeModel> result = null;
    if (hasWildcard()) {
      if (qualifiedInfo.memberName != null) return Collections.emptyList();

      if (qualifiedInfo.fileName != null && qualifiedInfo.className == null) {
        qualifiedInfo = new FullyQualifiedInfo(qualifiedInfo.packagePath, qualifiedInfo.fileName, qualifiedInfo.fileName, null);
      }
      List<HaxeModel> items = HaxeProjectModel.fromElement(basePsi).resolve(qualifiedInfo, basePsi.getResolveScope());
      if (items != null && items.size() > 0) {
        result = items.stream()
          .filter(model -> model instanceof HaxeExposableModel)
          .flatMap(model -> ((HaxeExposableModel)model).getExposedMembers().stream())
          .collect(Collectors.toList());
      }
    } else {
      if (hasAlias() && qualifiedInfo.fileName != null && qualifiedInfo.className == null) {
        qualifiedInfo = new FullyQualifiedInfo(qualifiedInfo.packagePath, qualifiedInfo.fileName, qualifiedInfo.fileName, null);
      }
      result = HaxeProjectModel.fromElement(basePsi).resolve(qualifiedInfo, basePsi.getResolveScope());
      if (result != null && !result.isEmpty()) {
        HaxeModel firstItem = result.get(0);
        if (firstItem instanceof HaxeFileModel || firstItem instanceof HaxePackageModel) {
          result = ((HaxeExposableModel)firstItem).getExposedMembers();
        }
      }
    }

    return result != null ? exposeEnumValues(result) : Collections.emptyList();
  }

  @NotNull
  @Override
  public List<HaxeModel> getExposedMembers() {
    List<HaxeModel> exposed = cacheManager.getCachedValue(getBasePsi(), () -> {
      List<HaxeModel> exposedMembers = getExposedMembersInternal();
      PsiElement [] dependencies = new PsiElement[exposedMembers.size() + 1];
      int i = 0;
      dependencies[i++] = getBasePsi();
      for (HaxeModel xMember : exposedMembers) {
        dependencies[i++] = xMember.getBasePsi();
      }
      return new CachedValueProvider.Result<>(exposedMembers, (Object[])dependencies);
    });

    return exposed;
  }


  @NotNull
  private List<HaxeModel> exposeEnumValues(@NotNull List<HaxeModel> result) {
    result.addAll(
      result.stream()
        .filter(model -> model instanceof HaxeEnumModel)
        .flatMap(model -> ((HaxeEnumModel)model).getValues().stream())
        .collect(Collectors.toList())
    );

    return result;
  }

  @Nullable
  public PsiElement exposeByName(String name) {
    if (name == null) return null;

    if (hasWildcard()) {
      for (HaxeModel exposedMember : getExposedMembers()) {
        if (Objects.equals(exposedMember.getName(), name)) return exposedMember.getBasePsi();
      }
    } else {
      if (getReferenceExpression() != null) {
        FullyQualifiedInfo qualifiedInfo = getQualifiedInfo();

        HaxeModel member = getExposedMember(name);
        if (((equalsToAlias(name) || qualifiedInfo.equalsToNamedPart(name)) && member != null) || member != null) {
          return member.getBasePsi();
        }
      }
    }

    return null;
  }

  private boolean equalsToAlias(String name) {
    return hasAlias() && Objects.equals(getAliasName(), name);
  }

  private HaxeModel getExposedMember(String name) {
    List<? extends HaxeModel> members = getExposedMembers();
    if (members.isEmpty()) return null;
    if (hasAlias()) {
      return (Objects.equals(getAliasName(), name)) ? members.get(0) : null;
    }
    return members.stream()
      .filter(model -> model.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public String getName() {
    return null;
  }

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
  protected void finalize() throws Throwable {
    super.finalize();
  }
}
