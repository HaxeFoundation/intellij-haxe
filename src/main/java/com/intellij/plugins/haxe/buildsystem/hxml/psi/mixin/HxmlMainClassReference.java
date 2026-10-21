package com.intellij.plugins.haxe.buildsystem.hxml.psi.mixin;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.psi.HXMLQualifiedName;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeMainClasses;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class a {@code -main} line names, looked up through the class paths of
 * the build section holding the line ({@link HaxeMainClasses}). Renaming the
 * class rewrites the type path.
 */
final class HxmlMainClassReference extends PsiReferenceBase<HXMLQualifiedName> {
  // TODO: an unresolved -main is not marked, and no class completion follows -main

  HxmlMainClassReference(@NotNull HXMLQualifiedName typePath) {
    super(typePath, TextRange.allOf(typePath.getText()));
  }

  @Override
  public @Nullable PsiElement resolve() {
    PsiFile hxmlFile = myElement.getContainingFile();
    VirtualFile file = hxmlFile.getOriginalFile().getVirtualFile();
    if (file == null) return null;
    int sectionIndex = HaxeBuildFileInspector.sectionIndexOf(hxmlFile, myElement.getParent());
    HaxeBuildFile buildFile = new HaxeBuildFile(file, HaxeBuildFileType.HXML);
    return HaxeMainClasses.find(myElement.getProject(), buildFile, sectionIndex, myElement.getText());
  }

  /** A renamed class, or module file, keeps the type path's package. */
  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    String typeName = StringUtil.trimEnd(newElementName, "." + HaxeFileType.DEFAULT_EXTENSION);
    String typePath = myElement.getText();
    String packagePrefix = typePath.substring(0, typePath.lastIndexOf('.') + 1);
    return replaceTypePath(packagePrefix + typeName);
  }

  @NotNull
  private PsiElement replaceTypePath(@NotNull String typePath) throws IncorrectOperationException {
    PsiFile parsed = PsiFileFactory.getInstance(myElement.getProject())
      .createFileFromText("main.hxml", HXMLFileType.INSTANCE, "-main " + typePath);
    HXMLQualifiedName replacement = PsiTreeUtil.findChildOfType(parsed, HXMLQualifiedName.class);
    if (replacement == null) throw new IncorrectOperationException("not a type path: " + typePath);
    return myElement.replace(replacement);
  }
}
