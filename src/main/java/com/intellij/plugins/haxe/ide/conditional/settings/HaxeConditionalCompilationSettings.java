package com.intellij.plugins.haxe.ide.conditional.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

/**
 * Display of inactive conditional-compilation branches: how far their syntax
 * colors fade toward the editor background (0 = full colors, 100 = invisible).
 * The color scheme UI only holds color/effect attributes, so the numeric
 * strength lives here instead.
 */
@Service(Service.Level.APP)
@State(name = "HaxeConditionalCompilationSettings", storages = @Storage("haxe-conditional-compilation.xml"))
public final class HaxeConditionalCompilationSettings
  implements PersistentStateComponent<HaxeConditionalCompilationSettings.State> {

  public static final class State {
    public int dimIntensityPercent = 40;
  }

  private State state = new State();

  @NotNull
  public static HaxeConditionalCompilationSettings getInstance() {
    return ApplicationManager.getApplication().getService(HaxeConditionalCompilationSettings.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }
}
