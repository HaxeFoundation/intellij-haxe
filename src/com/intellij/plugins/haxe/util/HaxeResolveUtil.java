package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.index.HaxeComponentFileNameIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public static String joinQName(@Nullable String packageName, @NotNull String className) {
    String result = "";
    if (packageName != null && !packageName.isEmpty()) {
      result = packageName + ".";
    }
    return result + className;
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
}
