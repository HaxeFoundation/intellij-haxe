package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildFileIncludes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A project-file path attribute, resolved against the project file's folder;
/// an `<include>` naming a folder resolves to the include file Lime picks
/// inside it ([HaxeBuildFileIncludes#includeFile]). Soft: a path naming
/// nothing is left alone.
final class HaxeProjectXmlPathReference extends PsiReferenceBase<XmlAttributeValue> {

  private final String path;

  HaxeProjectXmlPathReference(@NotNull XmlAttributeValue value, @NotNull String path) {
    super(value, true);
    this.path = path;
  }

  @Override
  public @Nullable PsiElement resolve() {
    VirtualFile folder = folder();
    if (folder == null) return null;
    VirtualFile target = isInclude()
                         ? HaxeBuildFileIncludes.includeFile(folder, path)
                         : HaxeBuildFileIncludes.fileOrFolder(folder, path);
    if (target == null) return null;
    PsiManager psiManager = getElement().getManager();
    return target.isDirectory() ? psiManager.findDirectory(target) : psiManager.findFile(target);
  }

  /** A renamed target replaces the path's last segment only; an include naming a folder keeps naming it. */
  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    if (namesFolderInclude()) return getElement();
    int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return super.handleElementRename(path.substring(0, lastSeparator + 1) + newElementName);
  }

  @Nullable
  private VirtualFile folder() {
    return HaxeProjectXmlFiles.folderOf(getElement().getContainingFile());
  }

  private boolean isInclude() {
    XmlTag tag = PsiTreeUtil.getParentOfType(getElement(), XmlTag.class);
    return tag != null && ProjectXmlVocabulary.INCLUDE_ELEMENT.equalsIgnoreCase(tag.getLocalName());
  }

  /** Whether the path names a folder whose include file is the target. */
  private boolean namesFolderInclude() {
    VirtualFile folder = folder();
    if (folder == null || !isInclude()) return false;
    VirtualFile named = HaxeBuildFileIncludes.fileOrFolder(folder, path);
    return named != null && named.isDirectory();
  }
}
