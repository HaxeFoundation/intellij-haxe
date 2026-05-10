/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeEnumBodyImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.util.HaxeEnumValueUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeEnumModelImpl extends HaxeClassModel implements HaxeEnumModel {
  public HaxeEnumModelImpl(@NotNull HaxeEnumDeclaration haxeClass) {
    super(haxeClass);
  }

  public HaxeEnumDeclaration getEnumDeclaration() {
    return (HaxeEnumDeclaration)haxeClass;
  }

  @Override
  public HaxeClassModel getParentClass() {
    return null;
  }

  @Override
  public List<HaxeClassReferenceModel> getExtendingTypes() {
    return Collections.emptyList();
  }

  @Override
  public List<HaxeClassReferenceModel> getImplementingInterfaces() {
    return Collections.emptyList();
  }

  @Override
  public boolean isClass() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return true;
  }

  @Override
  public boolean isTypedef() {
    return false;
  }

  @Nullable
  @Override
  public HaxeTypeOrAnonymous getUnderlyingTypeOrAnonymous() {
    return null;
  }

  @Override
  public List<HaxeType> getAbstractToList() {
    return Collections.emptyList();
  }

  @Override
  public List<HaxeType> getAbstractFromList() {
    return Collections.emptyList();
  }

  @Override
  public boolean hasMethod(String name, @Nullable HaxeGenericResolver resolver) {
    return false;
  }

  @Override
  public boolean hasMethodSelf(String name) {
    return false;
  }

  @Override
  public HaxeMethodModel getMethodSelf(String name) {
    return null;
  }

  @Override
  public HaxeMethodModel getConstructorSelf() {
    return null;
  }

  @Override
  public HaxeMethodModel getConstructor(@Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public boolean hasConstructor(@Nullable HaxeGenericResolver resolver) {
    return false;
  }

  @Override
  public HaxeMethodModel getParentConstructor(@Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public HaxeBaseMemberModel getMember(@NotNull final String name, @Nullable HaxeGenericResolver resolver) {
    HaxeEnumValueModel valueModel = getValue(name);
    HaxeBaseMemberModel value = valueModel instanceof HaxeBaseMemberModel model ? model : null;
    if (!isAbstractType() && value == null) value = getEnumValueMember(name, resolver);
    return  value;
  }

  @Nullable
  private HaxeBaseMemberModel getEnumValueMember(String name, HaxeGenericResolver resolver) {
    HaxeClassModel model = HaxeEnumValueUtil.getEnumValueClassModel(this.getPsi());
    if(model == null) return null;
    return model.getMember(name, resolver);
  }

  @NotNull
  @Override
  public List<HaxeBaseMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    List<HaxeBaseMemberModel> members = getValuesStream().map(HaxeBaseMemberModel.class::cast).collect(Collectors.toList());
    if (!isAbstractType()) {
      members.addAll(HaxeEnumValueUtil.getEnumValueClassMembers(this.getPsi(), resolver));
    }
    return members;
  }

  @NotNull
  public List<HaxeBaseMemberModel> getMembers(String name, @Nullable HaxeGenericResolver resolver) {
    if (name == null) return List.of();
    return getMembers(resolver).stream()
            .filter(memberModel -> name.equals(memberModel.getName()))
            .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public List<HaxeBaseMemberModel> getMembersSelf() {
    return getMembers(null);
  }


  @Override
  public HaxeEnumValueModel getValue(@NotNull String name) {
    HaxeEnumValueDeclaration value = getValueDeclarationsStream()
      .filter(declaration -> name.equals(declaration.getName()))
      .findFirst()
      .orElse(null);

    return value != null ? (HaxeEnumValueModel)value.getModel() : null;
  }

  @Override
  public List<HaxeEnumValueModel> getValues() {
    return getValuesStream().collect(Collectors.toList());
  }

  public Stream<HaxeEnumValueModel> getValuesStream() {
    return getValueDeclarationsStream()
      .map(HaxeModelTarget::getModel)
      .filter(HaxeEnumValueModel.class::isInstance)
      .map(HaxeEnumValueModel.class::cast);
  }

  private Stream<HaxeEnumValueDeclaration> getValueDeclarationsStream() {
    if (haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeClassStub greenStub = psiClass.getGreenStub();
      if (greenStub != null) {
        return getValueDeclarationsFromStub(greenStub).stream();
      }
    }
    HaxeEnumBodyImpl body = getEnumBodyPsi();
    return body != null ? body.getEnumValueDeclarationList().stream() : Stream.empty();
  }

  private static List<HaxeEnumValueDeclaration> getValueDeclarationsFromStub(HaxeClassStub greenStub) {
    List<HaxeEnumValueDeclaration> list = new ArrayList<>();
    for (StubElement<?> element : greenStub.getChildrenStubs()) {
      PsiElement psi = element.getPsi();
      if (psi instanceof HaxeEnumValueDeclaration declaration) {
        list.add(declaration);
      }
    }
    return list;
  }

  @Nullable
  private HaxeEnumBodyImpl getEnumBodyPsi() {
    return (HaxeEnumBodyImpl)getEnumDeclaration().getEnumBody();
  }

  @Override
  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public List<HaxeFieldModel> getFields() {
    return new ArrayList<>();
  }

  @Override
  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    return new ArrayList<>();
  }

  @Override
  public List<HaxeMethodModel> getMethodsSelf(@Nullable HaxeGenericResolver resolver) {
    return new ArrayList<>();
  }

  @Override
  public List<HaxeMethodModel> getAncestorMethods(@Nullable HaxeGenericResolver resolver) {
    return new ArrayList<>();
  }

  @NotNull
  @Override
  public HaxeClass getPsi() {
    return super.getPsi();
  }

  @Nullable
  @Override
  public HaxePsiCompositeElement getBodyPsi() {
    return getEnumBodyPsi();
  }

  @NotNull
  @Override
  public List<HaxeGenericParamModel> getGenericParams() {
    return super.getGenericParams();
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
    return new SmartList<>(getValues());
  }

  @Override
  public boolean isPublic() {
    return super.isPublic();
  }
}
