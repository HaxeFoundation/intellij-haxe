package com.intellij.plugins.haxe.ide;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIconProvider extends IconProvider {
  @Override
  public Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof HaxeFile) {
      return getHaxeFileIcon((HaxeFile)element, flags);
    }
    return null;
  }

  @Nullable
  private static Icon getHaxeFileIcon(HaxeFile file, @Iconable.IconFlags int flags) {
    final String fileName = FileUtil.getNameWithoutExtension(file.getName());
    for (HaxeComponent component : HaxeResolveUtil.findComponentDeclarations(file)) {
      if (fileName.equals(component.getName())) {
        return component.getIcon(flags);
      }
    }
    return null;
  }
}
