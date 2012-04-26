package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGenericSpecialization {
  final Map<String, HaxeClassResolveResult> map;

  public HaxeGenericSpecialization() {
    this(new THashMap<String, HaxeClassResolveResult>());
  }

  protected HaxeGenericSpecialization(Map<String, HaxeClassResolveResult> map) {
    this.map = map;
  }

  public void put(PsiElement element, String genericName, HaxeClassResolveResult resolveResult) {
    map.put(getGenericKey(element, genericName), resolveResult);
  }

  public boolean containsKey(@Nullable PsiElement element, String genericName) {
    return map.containsKey(getGenericKey(element, genericName));
  }

  public HaxeClassResolveResult get(@Nullable PsiElement element, String genericName) {
    return map.get(getGenericKey(element, genericName));
  }

  public HaxeGenericSpecialization getInnerSpecialization(PsiElement element) {
    final String prefixToRemove = getGenericKey(element, "");
    final Map<String, HaxeClassResolveResult> result = new THashMap<String, HaxeClassResolveResult>();
    for (String key : map.keySet()) {
      final HaxeClassResolveResult value = map.get(key);
      String newKey = key;
      if (newKey.startsWith(prefixToRemove)) {
        newKey = newKey.substring(prefixToRemove.length());
      }
      result.put(newKey, value);
    }
    return new HaxeGenericSpecialization(result);
  }

  public static String getGenericKey(@Nullable PsiElement element, @NotNull String genericName) {
    final StringBuilder result = new StringBuilder();
    final HaxeNamedComponent namedComponent = PsiTreeUtil.getParentOfType(element, HaxeNamedComponent.class, false);
    if (namedComponent instanceof HaxeClass) {
      result.append(((HaxeClass)namedComponent).getQualifiedName());
    }
    else if (namedComponent != null) {
      HaxeClass haxeClass = PsiTreeUtil.getParentOfType(namedComponent, HaxeClass.class);
      if (haxeClass instanceof HaxeAnonymousType) {
        // class -> typeOrAnonymous -> anonymous
        final PsiElement parent = haxeClass.getParent().getParent();
        haxeClass = parent instanceof HaxeClass ? (HaxeClass)parent : haxeClass;
      }
      if (haxeClass != null) {
        result.append(haxeClass.getQualifiedName());
      }
      if (PsiTreeUtil.getChildOfType(namedComponent, HaxeGenericParam.class) != null) {
        // generic method
        result.append(":");
        result.append(namedComponent.getName());
      }
    }
    if (result.length() > 0) {
      result.append("-");
    }
    result.append(genericName);
    return result.toString();
  }
}
