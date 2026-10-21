package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * The Settings | Haxe tree: an empty "Haxe" root and "Frameworks" group whose
 * children are the per-framework target tables. Grouping pages carry no UI of
 * their own — the platform renders their children.
 */
public final class HaxeFrameworkConfigurables {

  private HaxeFrameworkConfigurables() {
  }

  private abstract static class GroupPage implements SearchableConfigurable {
    private final String id;
    private final String displayName;

    GroupPage(@NotNull String id, @NotNull String displayName) {
      this.id = id;
      this.displayName = displayName;
    }

    @Override
    public @NotNull String getId() {
      return id;
    }

    @Override
    public String getDisplayName() {
      return displayName;
    }

    @Override
    public @Nullable JComponent createComponent() {
      return null;
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public void apply() {
    }
  }

  public static final class HaxeRoot extends GroupPage {
    public HaxeRoot() {
      super("settings.haxe", HaxeBundle.message("haxe.settings.root.name"));
    }
  }

  public static final class Frameworks extends GroupPage {
    public Frameworks() {
      super("settings.haxe.frameworks", HaxeBundle.message("haxe.settings.frameworks.name"));
    }
  }

  public static final class LimeTargets extends HaxeFrameworkTargetsConfigurable {
    public LimeTargets() {
      super(Framework.LIME, "settings.haxe.frameworks.lime", HaxeBundle.message("haxe.frameworks.targets.lime"));
    }
  }

  public static final class OpenflTargets extends HaxeFrameworkTargetsConfigurable {
    public OpenflTargets() {
      super(Framework.OPENFL, "settings.haxe.frameworks.openfl", HaxeBundle.message("haxe.frameworks.targets.openfl"));
    }
  }

  public static final class NmeTargets extends HaxeFrameworkTargetsConfigurable {
    public NmeTargets() {
      super(Framework.NME, "settings.haxe.frameworks.nme", HaxeBundle.message("haxe.frameworks.targets.nme"));
    }
  }
}
