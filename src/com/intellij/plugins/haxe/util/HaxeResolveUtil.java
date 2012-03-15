package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeComponentFileNameIndex;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {
  @NotNull
  public static Pair<String, String> splitQName(@NotNull String qName) {
    final int dotIndex = qName.lastIndexOf('.');
    final String packageName = dotIndex == -1 ? "" : qName.substring(0, dotIndex);
    final String className = dotIndex == -1 ? qName : qName.substring(dotIndex + 1);

    return new Pair<String, String>(packageName, className);
  }

  @NotNull
  public static String joinQName(@Nullable String packageName, @Nullable String className) {
    String result = "";
    if (packageName != null && !packageName.isEmpty()) {
      result = packageName + ".";
    }
    if (className != null) {
      result += className;
    }
    return result;
  }

  @NotNull
  @NonNls
  public static String getPackageName(final PsiFile file) {
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(file, HaxePackageStatement.class);
    if (packageStatement != null && packageStatement.getExpression() != null) {
      return packageStatement.getExpression().getText();
    }
    return "";
  }

  @Nullable
  public static HaxeNamedComponent findNamedComponentByQName(final String qName, final @Nullable PsiElement context) {
    if (context == null) {
      return null;
    }
    final Project project = context.getProject();

    final List<VirtualFile> classFiles = HaxeComponentFileNameIndex.
      getFilesNameByQName(qName, GlobalSearchScope.allScope(project));
    final Pair<String, String> qNamePair = splitQName(qName);

    for (VirtualFile classFile : classFiles) {
      final HaxeComponent componentPsiElement = findComponentDeclaration(context.getManager().findFile(classFile), qNamePair.getSecond());
      if (componentPsiElement != null) {
        return componentPsiElement;
      }
    }
    return null;
  }

  @NotNull
  public static List<HaxeComponent> findComponentDeclarations(@Nullable PsiFile file) {
    if (file == null) {
      return Collections.emptyList();
    }
    final HaxeComponent[] components = PsiTreeUtil.getChildrenOfType(file, HaxeComponent.class);
    if (components == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(components);
  }

  @Nullable
  public static HaxeComponent findComponentDeclaration(@Nullable PsiFile file, @NotNull String componentName) {
    final List<HaxeComponent> declarations = findComponentDeclarations(file);
    for (HaxeComponent component : declarations) {
      final HaxeComponentName identifier = component.getComponentName();
      if (identifier != null && componentName.equals(identifier.getText())) {
        return component;
      }
    }
    return null;
  }

  @NotNull
  public static List<HaxeType> findExtendsList(@Nullable HaxeInheritList extendsList) {
    return findExtendsImplementsListImpl(extendsList, HaxeTokenTypes.KEXTENDS);
  }

  public static List<HaxeType> getImplementsList(@Nullable HaxeInheritList extendsList) {
    return findExtendsImplementsListImpl(extendsList, HaxeTokenTypes.KIMPLEMENTS);
  }

  @NotNull
  private static List<HaxeType> findExtendsImplementsListImpl(@Nullable HaxeInheritList extendsList,
                                                              @NotNull IElementType expectedKeyword) {
    if (extendsList == null) {
      return Collections.emptyList();
    }
    final List<HaxeType> result = new ArrayList<HaxeType>();
    for (HaxeInherit inherit : extendsList.getInheritList()) {
      final PsiElement keyword = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(inherit);
      if (keyword != null && keyword == expectedKeyword) {
        result.add(inherit.getType());
      }
    }
    return result;
  }

  public static List<HaxeNamedComponent> filterNamedComponentsByType(List<HaxeNamedComponent> result, final HaxeComponentType type) {
    return ContainerUtil.filter(result, new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return HaxeComponentType.typeOf(component) == type;
      }
    });
  }

  @Nullable
  public static HaxeNamedComponent getNamedSubComponent(HaxeClass haxeClass, @NotNull final String name) {
    return ContainerUtil.find(getNamedSubComponents(haxeClass), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @NotNull
  public static List<HaxeNamedComponent> getNamedSubComponents(HaxeClass haxeClass) {
    PsiElement body = null;
    final HaxeComponentType type = HaxeComponentType.typeOf(haxeClass);
    if (type == HaxeComponentType.CLASS) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeClassBody.class);
    }
    if (type == HaxeComponentType.INTERFACE) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeInterfaceBody.class);
    }
    if (body == null) {
      return Collections.emptyList();
    }
    final HaxeNamedComponent[] namedComponents = PsiTreeUtil.getChildrenOfType(body, HaxeNamedComponent.class);
    final HaxeVarDeclaration[] variables = PsiTreeUtil.getChildrenOfType(body, HaxeVarDeclaration.class);

    final List<HaxeNamedComponent> result = new ArrayList<HaxeNamedComponent>();
    if (namedComponents != null) {
      ContainerUtil.addAll(result, namedComponents);
    }
    if (variables == null) {
      return result;
    }
    for (HaxeVarDeclaration varDeclaration : variables) {
      result.addAll(varDeclaration.getVarDeclarationPartList());
    }
    return result;
  }

  @Nullable
  public static List<HaxeType> getFunctionParameters(HaxeNamedComponent component) {
    if (HaxeComponentType.typeOf(component) != HaxeComponentType.METHOD) {
      return null;
    }
    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(component, HaxeParameterList.class);
    if (parameterList == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.map(parameterList.getParameterList(), new Function<HaxeParameter, HaxeType>() {
      @Override
      public HaxeType fun(HaxeParameter parameter) {
        final HaxeTypeTag typeTag = parameter.getTypeTag();
        return typeTag == null ? null : typeTag.getType();
      }
    });
  }

  @Nullable
  public static HaxeClass getHaxeClass(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof HaxeComponentName) {
      return getHaxeClass(element.getParent());
    }
    if (element instanceof HaxeClass) {
      return (HaxeClass)element;
    }
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeType type = typeTag == null ? null : typeTag.getType();
    final HaxeNamedComponent typeComponent = type == null ? null : resolveType(type);
    return getHaxeClass(typeComponent);
  }

  @Nullable
  public static HaxeNamedComponent resolveType(@Nullable HaxeType type) {
    if (type == null || type.getContext() == null) {
      return null;
    }
    final String qName = getQName(type);

    return findNamedComponentByQName(qName, type.getContext());
  }

  public static String getQName(@NotNull HaxeType type) {
    String result = type.getText();
    if (result.indexOf('.') == -1) {
      final HaxeImportStatement importStatement = UsefulPsiTreeUtil.findImportByClass(type, result);
      if (importStatement != null && importStatement.getExpression() != null) {
        result = importStatement.getExpression().getText();
      }
      else {
        final String packageName = HaxeResolveUtil.getPackageName(type.getContainingFile());
        if (!packageName.isEmpty()) {
          result = packageName + "." + result;
        }
      }
    }
    return result;
  }
}
