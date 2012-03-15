package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeInheritList;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public abstract class AbstractHaxePsiClass extends AbstractHaxeNamedComponent implements HaxeClass {
  public AbstractHaxePsiClass(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public HaxeNamedComponent getTypeComponent() {
    return this;
  }

  @Override
  public String getQualifiedName() {
    final String packageName = HaxeResolveUtil.getPackageName(getContainingFile());
    return HaxeResolveUtil.joinQName(packageName, getName());
  }

  @Override
  public boolean isInterface() {
    return HaxeComponentType.typeOf(this) == HaxeComponentType.INTERFACE;
  }

  @NotNull
  @Override
  public List<HaxeType> getExtendsList() {
    return HaxeResolveUtil.findExtendsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }

  @NotNull
  @Override
  public List<HaxeType> getImplementsList() {
    return HaxeResolveUtil.getImplementsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getMethods() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.getNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.METHOD);
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getFields() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.getNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.FIELD);
  }

  @Nullable
  @Override
  public HaxeNamedComponent findFieldByName(final String name) {
    return ContainerUtil.find(getFields(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public HaxeNamedComponent findMethodByName(String name) {
    return findMethodBySignature(name, Collections.<HaxeType>emptyList());
  }

  @Override
  public HaxeNamedComponent findMethodBySignature(final String name, final List<HaxeType> parameterTypes) {
    return ContainerUtil.find(getMethods(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        if (!name.equals(component.getName())) {
          return false;
        }
        final List<HaxeType> componentParameterTypes = HaxeResolveUtil.getFunctionParameters(component);
        return componentParameterTypes != null && parameterTypes.size() != componentParameterTypes.size();
      }
    });
  }
}
