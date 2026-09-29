package com.intellij.plugins.haxe.v2.wizard;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.module.GeneralModuleType;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeWizardBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Creates plain {@link GeneralModuleType} modules for Haxe (v2). The custom
 * HAXE_MODULE type is legacy: Haxe-ness comes from the build files in the module,
 * not from the module type.
 */
public final class HaxeModuleBuilder extends ModuleBuilder {

  public static final String SOURCE_DIR = "src";

  @Override
  public ModuleType<?> getModuleType() {
    return GeneralModuleType.INSTANCE;
  }

  @Override
  public void setupRootModel(@NotNull ModifiableRootModel model) throws ConfigurationException {
    String contentEntryPath = getContentEntryPath();
    if (contentEntryPath == null) return;

    VirtualFile contentRoot = createContentRoot(contentEntryPath);
    ContentEntry contentEntry = model.addContentEntry(contentRoot);
    contentEntry.addSourceFolder(contentRoot.getUrl() + "/" + SOURCE_DIR, false);
    assignSdk(model);
  }

  @NotNull
  private static VirtualFile createContentRoot(@NotNull String contentEntryPath) throws ConfigurationException {
    try {
      VirtualFile contentRoot = VfsUtil.createDirectoryIfMissing(contentEntryPath);
      if (contentRoot == null) {
        throw new ConfigurationException(HaxeWizardBundle.message("haxe.wizard.cannot.create.content.root", contentEntryPath));
      }
      VfsUtil.createDirectoryIfMissing(contentRoot, SOURCE_DIR);
      return contentRoot;
    }
    catch (IOException e) {
      throw new ConfigurationException(HaxeWizardBundle.message("haxe.wizard.cannot.create.content.root", contentEntryPath));
    }
  }

  private static void assignSdk(@NotNull ModifiableRootModel model) {
    // HaxeProjectSdkStep has already installed the user's chosen SDK as the
    // project SDK by the time commit runs; an explicit module SDK would
    // override that choice. Fall back to the first Haxe SDK in the table
    // only when no Haxe project SDK is set.
    Sdk projectSdk = ProjectRootManager.getInstance(model.getProject()).getProjectSdk();
    boolean projectSdkIsHaxe = projectSdk != null && projectSdk.getSdkType() instanceof HaxeSdkType;
    if (projectSdkIsHaxe) {
      model.inheritSdk();
      return;
    }
    List<Sdk> haxeSdks = ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance());
    if (haxeSdks.isEmpty()) {
      model.inheritSdk();
    }
    else {
      model.setSdk(haxeSdks.getFirst());
    }
  }
}
