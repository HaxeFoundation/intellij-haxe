package com.intellij.plugins.haxe.ide.generation;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePropertyDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclarationPart;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class CreateGetterSetterFix extends BaseCreateMethodsFix {
  public static enum Strategy {
    GETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name));
      }
    }, SETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.setterName(name));
      }
    }, GETTERSETTER {
      @Override
      boolean accept(String name, Set<String> names) {
        return !names.contains(HaxePresentableUtil.getterName(name)) &&
               !names.contains(HaxePresentableUtil.setterName(name));
      }
    };

    abstract boolean accept(String name, Set<String> names);
  }

  private final Strategy myStratagy;

  public CreateGetterSetterFix(final HaxeClass haxeClass, Strategy strategy) {
    super(haxeClass);
    myStratagy = strategy;
  }

  @Override
  protected String buildFunctionsText(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return "";
    }
    final String typeText = HaxePresentableUtil.buildTypeText(namedComponent, ((HaxeVarDeclarationPart)namedComponent).getTypeTag());
    final StringBuilder result = new StringBuilder();
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      buildGetter(result, namedComponent.getName(), typeText);
    }
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      buildSetter(result, namedComponent.getName(), typeText);
    }
    return result.toString();
  }

  @Override
  protected void modifyElement(HaxeNamedComponent namedComponent) {
    if (!(namedComponent instanceof HaxeVarDeclarationPart)) {
      return;
    }
    if (((HaxeVarDeclarationPart)namedComponent).getPropertyDeclaration() != null) {
      // todo: modify
      return;
    }

    final HaxeVarDeclarationPart declarationPart =
      HaxeElementGenerator.createVarDeclarationPart(namedComponent.getProject(), buildVarDeclaration(namedComponent.getName()));
    final HaxePropertyDeclaration propertyDeclaration = declarationPart.getPropertyDeclaration();
    if (propertyDeclaration != null) {
      namedComponent.addAfter(propertyDeclaration, namedComponent.getComponentName());
    }
  }

  private String buildVarDeclaration(String name) {
    final StringBuilder result = new StringBuilder();
    result.append("var ");
    result.append(name);
    result.append("(");
    if (myStratagy == Strategy.GETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append(HaxePresentableUtil.getterName(name));
    }
    else {
      result.append("null");
    }
    result.append(",");
    if (myStratagy == Strategy.SETTER || myStratagy == Strategy.GETTERSETTER) {
      result.append(HaxePresentableUtil.setterName(name));
    }
    else {
      result.append("null");
    }
    result.append(")");
    result.append(";");
    return result.toString();
  }

  private static void buildGetter(StringBuilder result, String name, String typeText) {
    build(result, name, typeText, true);
  }

  private static void buildSetter(StringBuilder result, String name, String typeText) {
    build(result, name, typeText, false);
  }

  private static void build(StringBuilder result, String name, String typeText, boolean isGetter) {
    result.append(isGetter ? "@:getter" : "@:setter");
    result.append("(");
    result.append(name);
    result.append(")\n");
    result.append("public function ");
    result.append(isGetter ? HaxePresentableUtil.getterName(name) : HaxePresentableUtil.setterName(name));
    result.append("(");
    if (!isGetter) {
      result.append("value:");
      result.append(typeText);
    }
    result.append(")");
    if (isGetter) {
      result.append(":");
      result.append(typeText);
    }
    result.append("{\n");
    if (isGetter) {
      result.append("return ");
      result.append(name);
      result.append(";");
    }
    else {
      result.append("this.");
      result.append(name);
      result.append("=value;");
    }
    result.append("\n}");
  }
}
