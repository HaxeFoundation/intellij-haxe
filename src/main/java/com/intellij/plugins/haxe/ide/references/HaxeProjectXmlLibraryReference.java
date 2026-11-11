package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The library a `<haxelib name>` attribute names, resolved like the
/// library's row in the Haxe tool window: to the root of its synced entry
/// under External Libraries, the project file's module first, then any
/// module. Soft: a library that is not synced is left alone.
final class HaxeProjectXmlLibraryReference extends PsiReferenceBase<XmlAttributeValue> {

  private final String libraryName;

  HaxeProjectXmlLibraryReference(@NotNull XmlAttributeValue value, @NotNull String libraryName) {
    super(value, true);
    this.libraryName = libraryName;
  }

  @Override
  public @Nullable PsiElement resolve() {
    VirtualFile root = libraryRoot();
    return root == null ? null : getElement().getManager().findDirectory(root);
  }

  @Nullable
  private VirtualFile libraryRoot() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement().getContainingFile().getOriginalFile());
    VirtualFile root = module == null ? null : HaxeLibrarySync.managedLibraryRoot(module, libraryName);
    return root != null ? root : HaxeLibrarySync.managedLibraryRoot(getElement().getProject(), libraryName);
  }

  /** Renaming the library's folder leaves the library's name alone. */
  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) {
    return getElement();
  }

  /** Moving the library's folder leaves the library's name alone. */
  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) {
    return getElement();
  }
}
