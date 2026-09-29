package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project-level Haxe build tool configuration (v2), shown under Settings | Build Tools | Haxe:
 * the Haxe SDK plus paths to the commonly used companion tools. Empty paths mean
 * "auto-detect" (from the SDK or the system PATH).
 */
public interface HaxeBuildToolSettings {

  @NotNull
  static HaxeBuildToolSettings getInstance(@NotNull Project project) {
    return project.getService(HaxeBuildToolSettings.class);
  }

  /** Name of the Haxe SDK in the SDK table, or {@code null} when none is selected. */
  @Nullable
  String getSdkName();

  void setSdkName(@Nullable String sdkName);

  @NotNull
  String getNekoPath();

  void setNekoPath(@NotNull String path);

  @NotNull
  String getHashlinkPath();

  void setHashlinkPath(@NotNull String path);

  @NotNull
  String getHaxelibPath();

  void setHaxelibPath(@NotNull String path);

  @NotNull
  String getNodePath();

  void setNodePath(@NotNull String path);

  /** Standalone Flash player (projector) overriding the Haxe SDK's runtimes entry; empty = no override. */
  @NotNull
  String getFlashPlayerPath();

  void setFlashPlayerPath(@NotNull String path);

  /** Name of a Flex/AIR SDK entry in the SDK table overriding the Haxe SDK's runtimes entry; empty = no override. */
  @NotNull
  String getFlexSdkName();

  void setFlexSdkName(@NotNull String name);

  /** Whether the project keeps a haxe compilation server (`haxe --wait`) for faster compiles. */
  boolean isCompilationServerEnabled();

  void setCompilationServerEnabled(boolean enabled);

  /** Fixed server port, or 0 to pick a free port automatically. */
  int getCompilationServerPort();

  void setCompilationServerPort(int port);

  /** Extra arguments passed to the server process (e.g. "-v"). */
  @NotNull
  String getCompilationServerArguments();

  void setCompilationServerArguments(@NotNull String arguments);

  /**
   * Whether test compiles inject the live utest reporter (a {@code --macro}
   * patching utest's Runner), streaming per-test events instead of waiting for
   * the end-of-run batch. Injection degrades to the batch reporter on any
   * incompatibility; this switch turns it off entirely.
   */
  boolean isLiveTestReporting();

  void setLiveTestReporting(boolean enabled);
}
