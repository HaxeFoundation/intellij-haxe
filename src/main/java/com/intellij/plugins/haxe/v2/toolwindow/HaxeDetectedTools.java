package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** The tools the tool window detects from their config files in a container root, and the row names they take. */
public final class HaxeDetectedTools {

  /** A detected tool: the bundle key of its row name, the haxelib running it, its modes, the config file announcing it. */
  record Spec(@NotNull String nameKey, @NotNull String haxelib, @NotNull List<String> modes, @NotNull String configName) {

    @NotNull
    String name() {
      return HaxeBundle.message(nameKey);
    }
  }

  private static final List<Spec> SPECS = List.of(
    new Spec("haxe.toolwindow.tool.checkstyle", HaxeToolConfigs.CHECKSTYLE_HAXELIB, List.of(), HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME),
    new Spec("haxe.toolwindow.tool.format", HaxeToolConfigs.FORMATTER_HAXELIB, List.of(), HaxeToolConfigs.FORMATTER_CONFIG_NAME),
    new Spec("haxe.toolwindow.tool.format.check", HaxeToolConfigs.FORMATTER_HAXELIB, List.of("--check"), HaxeToolConfigs.FORMATTER_CONFIG_NAME));

  private HaxeDetectedTools() {
  }

  /** The tools whose config file sits in the container root. */
  @NotNull
  static List<Spec> in(@NotNull VirtualFile rootDir) {
    return SPECS.stream().filter(spec -> rootDir.findChild(spec.configName()) != null).toList();
  }

  /** The row names of the tools detected in the container root; a custom tool of the container may not take one of them. */
  @NotNull
  public static List<String> namesIn(@NotNull VirtualFile rootDir) {
    return in(rootDir).stream().map(Spec::name).toList();
  }
}
