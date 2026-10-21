package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Presence check for the optional Flash/Flex plugin, which drives every
 * flash-family debug session. Callers must pass this gate BEFORE classloading
 * anything that references the plugin's classes ({@code HaxeFlashDebuggingUtil}
 * and below), or a flex-less IDE throws NoClassDefFoundError instead of the
 * readable message.
 */
public final class FlexPluginGate {

  private static final PluginId FLEX_PLUGIN_ID = PluginId.getId("com.intellij.flex");

  private FlexPluginGate() {
  }

  public static void requireFlexPlugin() throws ExecutionException {
    if (!PluginManagerCore.isLoaded(FLEX_PLUGIN_ID)) {
      throw new ExecutionException(HaxeBundle.message(
        PluginManagerCore.isDisabled(FLEX_PLUGIN_ID) ? "enable.flex.plugin" : "install.flex.plugin"));
    }
  }

  /**
   * The full flash-family debug preamble: the plugin gate plus the resolved
   * Flex/AIR SDK name, failing with one readable message when neither the
   * Build Tools override nor the Haxe SDK names one.
   */
  @NotNull
  public static String requireFlexSdkName(@NotNull Project project) throws ExecutionException {
    return requireFlexSdkName(project, null);
  }

  /** Like {@link #requireFlexSdkName(Project)}, preferring a run configuration's own SDK selection when non-blank. */
  @NotNull
  public static String requireFlexSdkName(@NotNull Project project, @Nullable String override) throws ExecutionException {
    requireFlexPlugin();
    String flexSdkName = HaxeToolPathResolver.flexSdkNameOrEmpty(project, override);
    if (flexSdkName.isBlank()) {
      throw new ExecutionException(HaxeDebuggerBundle.message("air.runner.no.flex.sdk"));
    }
    return flexSdkName;
  }
}
