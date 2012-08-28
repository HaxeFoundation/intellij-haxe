package com.intellij.plugins.haxe.ide.library;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeIcons;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLibraryType extends LibraryType<DummyLibraryProperties> {
  public static final PersistentLibraryKind<DummyLibraryProperties> HAXE_LIBRARY = new PersistentLibraryKind<DummyLibraryProperties>("haXe") {
    @NotNull
    @Override
    public DummyLibraryProperties createDefaultProperties() {
      return new DummyLibraryProperties();
    }
  };

  public HaxeLibraryType() {
    super(HAXE_LIBRARY);
  }

  @NotNull
  @Override
  public String getCreateActionName() {
    return "haXe";
  }

  @Override
  public boolean isSuitableModule(@NotNull Module module, @NotNull FacetsProvider facetsProvider) {
    return ModuleType.get(module).equals(HaxeModuleType.getInstance());
  }

  @Override
  public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent,
                                                  @Nullable VirtualFile contextDirectory,
                                                  @NotNull Project project) {

    return LibraryTypeService.getInstance()
      .createLibraryFromFiles(createLibraryRootsComponentDescriptor(), parentComponent, contextDirectory, this, project);
  }

  @NotNull
  @Override
  public LibraryRootsComponentDescriptor createLibraryRootsComponentDescriptor() {
    return new HaxeLibraryRootsComponentDescriptor();
  }

  @Override
  public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<DummyLibraryProperties> component) {
    return null;
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  public static HaxeLibraryType getInstance() {
    return LibraryType.EP_NAME.findExtension(HaxeLibraryType.class);
  }
}
