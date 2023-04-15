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
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.util.HaxeEnumValueUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public List<HaxeClassReferenceModel> getInterfaceExtendingInterfaces() {
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
  public HaxeTypeOrAnonymous getUnderlyingType() {
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
  public HaxeMemberModel getMember(@NotNull final String name, @Nullable HaxeGenericResolver resolver) {
    HaxeMemberModel value = getValue(name);
    if (!isAbstract() && value == null) value = getEnumValueMember(name, resolver);
    return  value;
  }

  @Nullable
  private HaxeMemberModel getEnumValueMember(String name, HaxeGenericResolver resolver) {
    HaxeClassModel model = HaxeEnumValueUtil.getEnumValueClassModel(this.getPsi());
    if(model == null) return null;
    return model.getMember(name, resolver);
  }

  @Override
  public List<HaxeMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMemberModel> members = getValuesStream().collect(Collectors.toList());
    if (!isAbstract()) {
      members.addAll(HaxeEnumValueUtil.getEnumValueClassMembers(this.getPsi(), resolver));
    }
    return members;
  }

  @NotNull
  @Override
  public List<HaxeMemberModel> getMembersSelf() {
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
      .map(declaration -> (HaxeEnumValueModel)declaration.getModel());
  }

  private Stream<HaxeEnumValueDeclaration> getValueDeclarationsStream() {
    HaxeEnumBody body = getEnumBodyPsi();

    return body != null ? body.getEnumValueDeclarationList().stream() : Stream.empty();
  }

  @Nullable
  private HaxeEnumBody getEnumBodyPsi() {
    return getEnumDeclaration().getEnumBody();
  }

  @Override
  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public List<HaxeFieldModel> getFields() {
    return Collections.emptyList();
  }

  @Override
  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    return null;
  }

  @Override
  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    return Collections.emptyList();
  }

  @Override
  public List<HaxeMethodModel> getMethodsSelf(@Nullable HaxeGenericResolver resolver) {
    return Collections.emptyList();
  }

  @Override
  public List<HaxeMethodModel> getAncestorMethods(@Nullable HaxeGenericResolver resolver) {
    return Collections.emptyList();
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
