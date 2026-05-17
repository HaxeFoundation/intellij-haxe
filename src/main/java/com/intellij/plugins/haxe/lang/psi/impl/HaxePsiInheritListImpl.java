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
package com.intellij.plugins.haxe.lang.psi.impl;


import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.*;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayFactory;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 *  Since extends and implements statments can be "mixed" (class A implements X extends Y  implements Z) in haxe
 *  We can not imitate the PsiReferenceList used in java 100% correctly, this class therefor contains both implements and extends
 *  and thus does not really have a Role
 */
@CustomLog
public abstract class HaxePsiInheritListImpl extends HaxeContainerStubPsiElementBase implements HaxePsiInheritList {

  //static {
  //  // Turn on all local messages.
  //  log.setLevel(LogLevel.DEBUG);
  //}

  /**
   * The empty array of PSI Java code references which can be reused to avoid unnecessary allocations.
   */
  HaxeInheritList[] EMPTY_ARRAY = new HaxeInheritList[0];

  ArrayFactory<HaxeInheritList> ARRAY_FACTORY = new ArrayFactory<HaxeInheritList>() {
    @NotNull
    @Override
    public HaxeInheritList[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new HaxeInheritList[count];
    }
  };


  public HaxePsiInheritListImpl(ASTNode node) {
    super(node);
  }

  public HaxePsiInheritListImpl(HaxeEmptyContainerStub<?> stub, IElementType type) {
    super(stub, type);
  }


  public PsiClassType @NotNull [] getReferencedImplements() {
    List<HaxeType> typeList = getImplementTypes();
    List<HaxeClass> list = new ArrayList<>();
    for (HaxeType haxeType : typeList) {
      if(haxeType.getReferenceExpression().resolve() instanceof HaxeClass aClass) {
        list.add(aClass);
      }
    }

    return createPsiClassTypes(list.toArray(HaxeClass[]::new));
  }

  public PsiClassType @NotNull [] getReferencedExtends() {
    List<HaxeType> typeList = getExtendsTypes();

    List<HaxeClass> list = new ArrayList<>();
    for (HaxeType haxeType : typeList) {
      if(haxeType.getReferenceExpression().resolve() instanceof HaxeClass aClass) {
        list.add(aClass);
      }
    }

    return createPsiClassTypes(list.toArray(HaxeClass[]::new));
  }



  private PsiClassType @NonNull [] createPsiClassTypesForCodeReference(PsiJavaCodeReferenceElement[] ref) {
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    PsiClassType[] types = new PsiClassType[ref.length];
    for (int i = 0; i < ref.length; i++) {
      types[i] = factory.createType(ref[i]);
    }
    return types;
  }
  private PsiClassType @NonNull [] createPsiClassTypes(HaxeClass[] ref) {
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    PsiClassType[] types = new PsiClassType[ref.length];
    for (int i = 0; i < ref.length; i++) {
      types[i] = factory.createType(ref[i]);
    }
    return types;
  }



  @NotNull
  @Override
  public PsiClassType[] getReferencedTypes() {
    log.debug("getReferencedTypes");
    PsiJavaCodeReferenceElement[] refs = getReferenceElements();
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    PsiClassType[] types = new PsiClassType[refs.length];
    for (int i = 0; i < types.length; i++) {
      types[i] = factory.createType(refs[i]);
    }

    return types;
  }

  @NotNull
  @Override
  public PsiJavaCodeReferenceElement[] getReferenceElements() {
    log.debug("getReferenceElements");
    List<HaxeType> typeList = getTypeList();
    return getReferenceElements(typeList);
  }
  public PsiJavaCodeReferenceElement[] getReferenceElements(List<HaxeType> typeList) {
    PsiJavaCodeReferenceElement[] refList = new PsiJavaCodeReferenceElement[typeList.size()];
    for (int i = 0; i < typeList.size(); ++i) {
      refList[i] = typeList.get(i).getReferenceExpression();
    }
    return refList;
  }

  @Override
  public Role getRole() {
    log.warn("HaxePsiInheritList can not reliably return a Role, returning null");
    return null;
  }

  @NotNull
  public List<HaxeType> getTypeList() {
    List<HaxeType> typeList = getExtendsTypes();
    typeList.addAll(getImplementTypes());

    return typeList;

  }
  private @NonNull List<HaxeType> getExtendsTypes() {
    List<HaxeType> typeList = new ArrayList<>();
    List<HaxeExtendsDeclaration> declarations = PsiTreeUtil.getStubChildrenOfTypeAsList(this, HaxeExtendsDeclaration.class);

    for (HaxeExtendsDeclaration declaration : declarations) {
      HaxeType type = declaration.getType();
      if (type != null) typeList.add(type);
    }

    return typeList;
  }

  private @NonNull List<HaxeType> getImplementTypes() {
    List<HaxeType> typeList = new ArrayList<>();
    List<HaxeImplementsDeclaration> declarations = PsiTreeUtil.getStubChildrenOfTypeAsList(this, HaxeImplementsDeclaration.class);

    for (HaxeImplementsDeclaration declaration : declarations) {
      HaxeType type = declaration.getType();
      if (type != null) typeList.add(type);
    }

    return typeList;
  }

}
