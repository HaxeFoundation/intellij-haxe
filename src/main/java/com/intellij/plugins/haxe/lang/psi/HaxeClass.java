/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeExternClassDeclarationImpl;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeClass extends HaxeComponent, PsiClass, HaxeModelTarget {
  HaxeClass[] EMPTY_ARRAY = new HaxeClass[0];

  /**
   * Create a Non-existent (source) class that is used to mark untyped monomorphs and unconstrained type parameters.
   * @param node - The AST Node for which we are creating the class.
   */
  static HaxeClass createUnknownClass(ASTNode node) {
    return new HaxeExternClassDeclarationImpl(node) {
      @SuppressWarnings({"ConstantConditions"})
      @Nullable
      @Override
      public String getName() {
        return SpecificTypeReference.UNKNOWN;
      }
    };
  }


  @NotNull
  @NonNls
  String getQualifiedName();

  HaxeClassModel getModel();

  @NotNull
  List<HaxeType> getHaxeExtendsList();

  @NotNull
  List<HaxeType> getHaxeImplementsList();

  boolean isAbstractType();

  boolean isExtern();

  boolean isInterface();

  boolean isTypeDef();

  /**
   * Retrieves list of Methods defined in current class
   * excluding methods defined or implemented in inherited classes
   *
   * @param resolver
   * @return list of Methods defined in current class
   */
  @NotNull
  List<HaxeMethod> getHaxeMethodsSelf(@Nullable HaxeGenericResolver resolver);


  @NotNull
  List<HaxeFieldDeclaration> getFieldSelf(@Nullable HaxeGenericResolver resolver);

  /**
   * Retrieves list of Methods defined both in current class and inherited classes that matches filter arguments
   *
   * @param fromTypesFilter list of super types to traverse
   * @return list of Methods
   */
  @NotNull
  List<HaxeMethod> getHaxeMethodsAll(HaxeComponentType... fromTypesFilter);

  /**
   * Retrieves list of Methods defined in superclasses of current class
   * none of the current class members are included
   *
   * @param  unique flag indicating that we want to deduplicate when a method is defined in multiple classes
   * @return list of Methods
   */
  List<HaxeMethod> getHaxeMethodsAncestor(boolean unique);


  /**
   * Retrieves list of Fields defined in current class
   * excluding Fields defined or implemented in inherited classes
   *
   * @param resolver
   * @return list of Fields defined in current class
   */
  @NotNull
  List<HaxeNamedComponent> getHaxeFieldsSelf(@Nullable HaxeGenericResolver resolver);


  /**
   * Retrieves list of FieldDeclarations in both current class and inherited classes that matches filter arguments
   *
   * @param fromTypesFilter list of super types to traverse
   * @return list of FieldDeclaration
   */
  @NotNull
  List<HaxeFieldDeclaration> getHaxeFieldAll(HaxeComponentType... fromTypesFilter);

  @Nullable
  HaxeNamedComponent findHaxeFieldByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver);

  @Nullable
  HaxeNamedComponent findHaxeMethodByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver);

  @Nullable
  HaxeNamedComponent findHaxeMemberByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver);

  /**
   * Given the class resolver, return the resolver used with members.  In most cases, this is
   * the resolver itself.  For abstracts and typedefs, this will be the resolver to use with underlying
   * class members.
   *
   * @param resolver - Class resolver to use.
   * @return the resolver to use when dealing with generic types/parameters on class members.
   */
  @Nullable
  HaxeGenericResolver getMemberResolver(HaxeGenericResolver resolver);

  boolean isGeneric();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeNamedComponent findArrayAccessGetter(@Nullable HaxeGenericResolver resolver);

  default boolean hasMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.hasMeta(this, meta);
  }

  default boolean hasCompileTimeMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.hasMeta(this, HaxeMetadataCompileTimeMeta.class, meta);
  }

  default HaxeMetadataList getCompileTimeMeta(HaxeMetadataTypeName meta) {
    return HaxeMetadataUtils.getMetadataList(this, HaxeMetadataCompileTimeMeta.class, meta);
  }
}
