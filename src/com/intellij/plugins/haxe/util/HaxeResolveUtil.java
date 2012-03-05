package com.intellij.plugins.haxe.util;

import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {

  public static PsiElement getTopReferenceParent(final PsiElement parent) {
    PsiElement currentParent = parent;
    while (currentParent instanceof HaxeReferenceExpression) {
      currentParent = currentParent.getParent();
    }
    return currentParent;
  }

  @Nullable
  public static HaxeNamedComponent findNamedComponentByQName(final String link, final @Nullable PsiElement context) {
    if (context == null) {
      return null;
    }
    final List<VirtualFile> classFiles =
      findComponentFilesByQName(link, DirectoryIndex.getInstance(context.getProject()), context.getResolveScope());

    for (VirtualFile classFile : classFiles) {
      final HaxeComponent componentPsiElement = findComponentDeclaration(context.getManager().findFile(classFile));
      if (componentPsiElement != null) {
        return componentPsiElement;
      }
    }
    return null;
  }

  @NotNull
  private static List<VirtualFile> findComponentFilesByQName(String link, DirectoryIndex directoryIndex, GlobalSearchScope scope) {
    final int dotIndex = link.lastIndexOf('.');
    final String packageName = dotIndex == -1 ? "" : link.substring(0, dotIndex);
    final String className = dotIndex == -1 ? link : link.substring(dotIndex + 1);

    Query<VirtualFile> dirs = directoryIndex.getDirectoriesByPackageName(packageName, true);
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    for (VirtualFile dir : dirs) {
      final VirtualFile child = dir.findChild(className + '.' + HaxeFileType.DEFAULT_EXTENSION);
      if (child != null && scope.accept(child)) {
        result.add(child);
      }
    }
    return result;
  }

  @Nullable
  private static HaxeComponent findComponentDeclaration(@Nullable PsiFile file) {
    if (file == null) {
      return null;
    }
    final HaxeComponent[] declarations = PsiTreeUtil.getChildrenOfType(file, HaxeComponent.class);
    if (declarations == null) {
      return null;
    }
    for (HaxeComponent component : declarations) {
      final String fileName = FileUtil.getNameWithoutExtension(file.getName());
      final HaxeComponentName identifier = component.getComponentName();
      if (identifier != null && fileName.equals(identifier.getText())) {
        return component;
      }
    }
    return null;
  }
}
