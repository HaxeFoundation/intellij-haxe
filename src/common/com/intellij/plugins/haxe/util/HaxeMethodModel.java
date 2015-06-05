/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class HaxeMethodModel {
  private HaxeMethodPsiMixin haxeMethod;
  private String name;

  public HaxeMethodModel(HaxeMethodPsiMixin haxeMethod) {
    this.haxeMethod = haxeMethod;
    this.name = haxeMethod.getName();
  }

  public HaxeMethodPsiMixin getPsi() {
    return haxeMethod;
  }

  public PsiElement getNamePsi() {
    return HaxePsiUtils.getChild(HaxePsiUtils.getChild(haxeMethod, HaxeComponentName.class), HaxeIdentifier.class);
    //return haxeMethod.getNameIdentifier();
  }

  public PsiElement getNameOrBasePsi() {
    PsiElement element = getNamePsi();
    if (element == null) element = getPsi();
    return element;
  }

  private HaxeClassModel _declaringClass = null;
  public HaxeClassModel getDeclaringClass() {
    if (_declaringClass == null) {
      HaxeClass aClass = (HaxeClass)this.haxeMethod.getContainingClass();
      _declaringClass = (aClass != null) ? aClass.getModel() : null;
    }
    return _declaringClass;
  }

  public String getName() {
    return name;
  }

  public @Nullable PsiElement getOverride() {
    return HaxePsiUtils.getChild(haxeMethod, HaxeDeclarationAttribute.class, "override");
  }

  public @Nullable PsiElement getStatic() {
    return HaxePsiUtils.getChild(haxeMethod, HaxeDeclarationAttribute.class, "static");
  }

  public @Nullable PsiElement getInline() {
    return HaxePsiUtils.getChild(haxeMethod, HaxeDeclarationAttribute.class, "inline");
  }

  public @Nullable PsiElement getPublic() {
    return HaxePsiUtils.getChild(haxeMethod, HaxeDeclarationAttribute.class, "public");
  }

  public @Nullable PsiElement getPrivate() {
    return HaxePsiUtils.getChild(haxeMethod, HaxeDeclarationAttribute.class, "private");
  }

  public HaxeVisibilityType getVisibility() {
    if (getPublic() != null) return HaxeVisibilityType.PUBLIC;
    if (getPrivate() != null) return HaxeVisibilityType.PRIVATE;
    return HaxeVisibilityType.NONE;
  }

  public boolean isConstructor() {
    return this.getName().equals("new");
  }
}

