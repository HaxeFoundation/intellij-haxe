package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Remembers which {@code --next} compilation section of an hxml the user works
 * against — the section the tool window shows and tests/libraries follow.
 * Stored by section IDENTITY (see {@code HxmlFileParser.sectionIds}), so the
 * choice sticks to the same section while the chain is edited; a selection
 * whose section disappears falls back to the FIRST section, matching the
 * compiler's own display mode. Workspace file: a per-developer setting, like
 * the target choice it mirrors.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeToolWindowSections", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class HaxeSectionSelectionStore implements PersistentStateComponent<HaxeSectionSelectionStore.State> {

  public static final class State {
    public Map<String, String> sectionsByFile = new TreeMap<>();
  }

  private final @Nullable Project project;
  private State state = new State();

  public HaxeSectionSelectionStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeSectionSelectionStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeSectionSelectionStore getInstance(@NotNull Project project) {
    return project.getService(HaxeSectionSelectionStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.sectionsByFile == null) {
      state.sectionsByFile = new TreeMap<>();
    }
    this.state = state;
    HaxeBuildSettingsListener.publish(project);
  }

  /** The selected section's index among the given identities; the first section when nothing (or a removed section) is stored. */
  public int getSelectedSection(@NotNull VirtualFile buildFile, @NotNull List<String> sectionIds) {
    String stored = state.sectionsByFile.get(buildFile.getPath());
    if (stored == null) return 0;
    int index = sectionIds.indexOf(stored);
    return index < 0 ? 0 : index;
  }

  public void setSelectedSection(@NotNull VirtualFile buildFile, @NotNull String sectionId) {
    state.sectionsByFile.put(buildFile.getPath(), sectionId);
    HaxeBuildSettingsListener.publish(project);
  }
}
