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
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.util.PsiTreeUtil;

public class HaxeTypeUtil {
  // @TODO: Check if cache works
  static public HaxeClass getFieldOrMethodType(AbstractHaxeNamedComponent comp) {
    // @TODO: cache should check if any related type has changed, which return depends
    long stamp = comp.getContainingFile().getModificationStamp();
    if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
      comp._cachedType = _getFieldOrMethodType(comp);
      comp._cachedTypeStamp = stamp;
    }

    return comp._cachedType;
  }

  static private HaxeClass _getFieldOrMethodType(AbstractHaxeNamedComponent comp) {
    if (comp instanceof PsiMethod) {
      return getFunctionReturnType(comp);
    } else {
      return getFieldType(comp);
    }
  }

  static private HaxeClass getFieldType(AbstractHaxeNamedComponent comp) {
    return getTypeFromTypeTag(comp);
  }

  static private HaxeClass getFunctionReturnType(AbstractHaxeNamedComponent comp) {
    HaxeClass type = getTypeFromTypeTag(comp);
    if (type != null) return type;

    if (comp instanceof PsiMethod) {
      return getReturnTypeFromBody(((PsiMethod)comp).getBody());
    } else {
      throw new RuntimeException("Can't get the body of a no PsiMethod");
    }
  }

  static private HaxeClass getTypeFromTypeTag(AbstractHaxeNamedComponent comp) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(comp, HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag != null ? typeTag.getTypeOrAnonymous() : null;

    if (typeOrAnonymous != null) {
      // @TODO: Do a proper type resolving
      HaxeType type = typeOrAnonymous.getType();
      if (type != null) {
        return HaxeResolveUtil.findClassByQName(type.getText(), type);
      }
    }
    return null;
  }

  static private HaxeClass getReturnTypeFromBody(PsiCodeBlock body) {
    return getPsiElementType(body);
  }

  static public HaxeClass getPsiElementType(PsiElement element) {
    //System.out.println("Handling element: " + element.getClass());
    if (element instanceof PsiCodeBlock) {
      HaxeClass type = null;
      for (PsiElement childElement : element.getChildren()) {
        type = getPsiElementType(childElement);
        if (childElement instanceof HaxeReturnStatement) {
          //System.out.println("HaxeReturnStatement:" + type);
          return type;
        }
      }
      return type;
    } else if (element instanceof HaxeReturnStatement) {
      return getPsiElementType(element.getChildren()[0]);
    } else if (element instanceof HaxeLiteralExpression) {
      return getPsiElementType(element.getFirstChild());
    } else if (element instanceof HaxeStringLiteralExpression) {
      return HaxeResolveUtil.findClassByQName("String", element);
    } else if (element instanceof PsiJavaToken) {
      String tokenType = ((PsiJavaToken)element).getTokenType().toString();
      if (tokenType.equals("LITINT") || tokenType.equals("LITHEX") || tokenType.equals("LITOCT")) {
        return HaxeResolveUtil.findClassByQName("Int", element);
      } else if (tokenType.equals("false") || tokenType.equals("true")) {
        return HaxeResolveUtil.findClassByQName("Bool", element);
      } else if (tokenType.equals("LITFLOAT")) {
        return HaxeResolveUtil.findClassByQName("Float", element);
      } else {
        //System.out.println("Unhandled token type: " + tokenType);
      }
    } else {
      //System.out.println("Unhandled " + element.getClass());
    }
    return HaxeResolveUtil.findClassByQName("Dynamic", element);
  }
}
