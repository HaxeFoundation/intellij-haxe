package com.intellij.plugins.haxe.ide.documentation.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

/**
 * Editor behavior inside doc comments, all opt-out: code fence injection,
 * markup highlighting (tags and inline code), tag completion and the
 * enter-key indentation continuation.
 */
@Service(Service.Level.APP)
@State(name = "HaxeDocSettings", storages = @Storage("haxe-docs.xml"))
public final class HaxeDocSettings implements PersistentStateComponent<HaxeDocSettings.State> {

  public static final class State {
    public boolean injectCodeFences = true;
    public boolean highlightDocMarkup = true;
    public boolean completeDocTags = true;
    public boolean enterKeepsIndentation = true;
  }

  private State state = new State();

  @NotNull
  public static HaxeDocSettings getInstance() {
    return ApplicationManager.getApplication().getService(HaxeDocSettings.class);
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
