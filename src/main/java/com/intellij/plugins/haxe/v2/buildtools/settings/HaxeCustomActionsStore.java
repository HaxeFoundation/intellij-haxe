package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * User-defined actions per build file (keyed by its path): a name plus a command
 * line executed in the build file's work directory
 * ({@link com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories}).
 * Shares {@code .idea/haxeBuildConfig.xml} with the other build-config stores.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeCustomActions", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeCustomActionsStore implements PersistentStateComponent<HaxeCustomActionsStore.State> {

  /** {@code workDirectory} may carry root variables; blank means the build file's directory. */
  public record CustomAction(@NotNull String name, @NotNull String command, @NotNull String workDirectory) {
  }

  public static final class State {
    public List<ContainerActions> containers = new ArrayList<>();
  }

  public static final class ContainerActions {
    public String ownerId;
    public List<ActionState> actions = new ArrayList<>();
  }

  public static final class ActionState {
    public String name;
    public String command = "";
    public String workDirectory = "";
  }

  private State state = new State();

  @NotNull
  public static HaxeCustomActionsStore getInstance(@NotNull Project project) {
    return project.getService(HaxeCustomActionsStore.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.containers == null) {
      state.containers = new ArrayList<>();
    }
    state.containers.forEach(HaxeCustomActionsStore::sanitizeActions);
    this.state = state;
  }

  // Stored lists can carry foreign objects after a format change (type erasure) - drop them.
  private static void sanitizeActions(@NotNull ContainerActions container) {
    if (container.actions == null) {
      container.actions = new ArrayList<>();
      return;
    }
    List<ActionState> cleaned = new ArrayList<>();
    for (Object entry : (List<?>)container.actions) {
      if (entry instanceof ActionState actionState && !StringUtil.isEmptyOrSpaces(actionState.name)) {
        cleaned.add(actionState);
      }
    }
    container.actions = cleaned;
  }

  /** The build file's custom actions in creation order. */
  @NotNull
  public List<CustomAction> getActions(@NotNull String ownerId) {
    ContainerActions container = find(ownerId);
    if (container == null) return List.of();
    return container.actions.stream()
      .filter(action -> !StringUtil.isEmptyOrSpaces(action.name))
      .map(HaxeCustomActionsStore::toAction)
      .toList();
  }

  public void addAction(@NotNull String ownerId, @NotNull CustomAction action) {
    ContainerActions container = getOrCreate(ownerId);
    container.actions.removeIf(existing -> action.name().equals(existing.name));
    container.actions.add(toState(action));
  }

  /** Replaces the action named {@code oldName}, keeping its position. */
  public void updateAction(@NotNull String ownerId, @NotNull String oldName, @NotNull CustomAction action) {
    ContainerActions container = getOrCreate(ownerId);
    for (int i = 0; i < container.actions.size(); i++) {
      if (oldName.equals(container.actions.get(i).name)) {
        container.actions.set(i, toState(action));
        return;
      }
    }
    container.actions.add(toState(action));
  }

  public void removeAction(@NotNull String ownerId, @NotNull String name) {
    ContainerActions container = find(ownerId);
    if (container != null) {
      container.actions.removeIf(action -> name.equals(action.name));
    }
  }

  @NotNull
  private static CustomAction toAction(@NotNull ActionState actionState) {
    String command = StringUtil.notNullize(actionState.command);
    return new CustomAction(actionState.name, command, StringUtil.notNullize(actionState.workDirectory));
  }

  @NotNull
  private static ActionState toState(@NotNull CustomAction action) {
    ActionState actionState = new ActionState();
    actionState.name = action.name();
    actionState.command = action.command();
    actionState.workDirectory = action.workDirectory();
    return actionState;
  }

  @Nullable
  private ContainerActions find(@NotNull String ownerId) {
    return state.containers.stream()
      .filter(container -> ownerId.equals(container.ownerId))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private ContainerActions getOrCreate(@NotNull String ownerId) {
    ContainerActions container = find(ownerId);
    if (container == null) {
      container = new ContainerActions();
      container.ownerId = ownerId;
      state.containers.add(container);
    }
    return container;
  }
}
