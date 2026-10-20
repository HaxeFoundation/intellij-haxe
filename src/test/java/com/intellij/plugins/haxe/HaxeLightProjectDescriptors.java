package com.intellij.plugins.haxe;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Shared light-project descriptors for the Haxe suite. The platform reuses
 * ONE project for consecutive light tests whose descriptor is EQUAL, so
 * every variant lives here as a single static instance - a fresh descriptor
 * per test class would force a project rebuild on every class switch.
 */
public final class HaxeLightProjectDescriptors {
  /** A Haxe module with only the in-memory source root. */
  public static final LightProjectDescriptor BARE = new HaxeDescriptor(false);
  /** A Haxe module with the test toolkit's std mounted as a second source root. */
  public static final LightProjectDescriptor WITH_TOOLKIT = new HaxeDescriptor(true);

  private HaxeLightProjectDescriptors() {
  }

  private static final class HaxeDescriptor extends LightProjectDescriptor {
    private final boolean withToolkit;

    private HaxeDescriptor(boolean withToolkit) {
      this.withToolkit = withToolkit;
    }

    @Override
    public @NotNull String getModuleTypeId() {
      return new HaxeModuleType().getId();
    }

    @Override
    protected void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
      if (!withToolkit) return;
      String toolkitPath = HaxeTestUtils.getAbsoluteToolkitPath(HaxeTestUtils.LATEST);
      VirtualFile toolkit = LocalFileSystem.getInstance().refreshAndFindFileByPath(toolkitPath);
      assert toolkit != null : "test toolkit missing: " + toolkitPath;
      model.addContentEntry(toolkit).addSourceFolder(toolkit, false);
    }
  }
}
