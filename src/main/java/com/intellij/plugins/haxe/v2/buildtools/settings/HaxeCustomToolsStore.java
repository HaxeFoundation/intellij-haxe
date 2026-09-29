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
 * User-defined tools per container (keyed by container id): a name plus a
 * command line executed in the container's root directory. Tools are the
 * container-level sibling of {@link HaxeCustomActionsStore}'s per-build-file
 * actions and never participate in compile-command selection.
 * Shares {@code .idea/haxeBuildConfig.xml} with the other build-config stores.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeCustomTools", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeCustomToolsStore implements PersistentStateComponent<HaxeCustomToolsStore.State> {

  /** {@code workDirectory} may carry root variables; blank means the container root. */
  public record CustomTool(@NotNull String name, @NotNull String command, @NotNull String workDirectory) {
  }

  public static final class State {
    public List<ContainerTools> containers = new ArrayList<>();
  }

  public static final class ContainerTools {
    public String ownerId;
    public List<ToolState> tools = new ArrayList<>();
  }

  public static final class ToolState {
    public String name;
    public String command = "";
    public String workDirectory = "";
  }

  private State state = new State();

  @NotNull
  public static HaxeCustomToolsStore getInstance(@NotNull Project project) {
    return project.getService(HaxeCustomToolsStore.class);
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
    state.containers.forEach(HaxeCustomToolsStore::sanitizeTools);
    this.state = state;
  }

  // Stored lists can carry foreign objects after a format change (type erasure) - drop them.
  private static void sanitizeTools(@NotNull ContainerTools container) {
    if (container.tools == null) {
      container.tools = new ArrayList<>();
      return;
    }
    List<ToolState> cleaned = new ArrayList<>();
    for (Object entry : (List<?>)container.tools) {
      if (entry instanceof ToolState toolState && !StringUtil.isEmptyOrSpaces(toolState.name)) {
        cleaned.add(toolState);
      }
    }
    container.tools = cleaned;
  }

  /** The container's custom tools in creation order. */
  @NotNull
  public List<CustomTool> getTools(@NotNull String ownerId) {
    ContainerTools container = find(ownerId);
    if (container == null) return List.of();
    return container.tools.stream()
      .filter(tool -> !StringUtil.isEmptyOrSpaces(tool.name))
      .map(HaxeCustomToolsStore::toTool)
      .toList();
  }

  public void addTool(@NotNull String ownerId, @NotNull CustomTool tool) {
    ContainerTools container = getOrCreate(ownerId);
    container.tools.removeIf(existing -> tool.name().equals(existing.name));
    container.tools.add(toState(tool));
  }

  /** Replaces the tool named {@code oldName}, keeping its position. */
  public void updateTool(@NotNull String ownerId, @NotNull String oldName, @NotNull CustomTool tool) {
    ContainerTools container = getOrCreate(ownerId);
    for (int i = 0; i < container.tools.size(); i++) {
      if (oldName.equals(container.tools.get(i).name)) {
        container.tools.set(i, toState(tool));
        return;
      }
    }
    container.tools.add(toState(tool));
  }

  public void removeTool(@NotNull String ownerId, @NotNull String name) {
    ContainerTools container = find(ownerId);
    if (container != null) {
      container.tools.removeIf(tool -> name.equals(tool.name));
    }
  }

  @NotNull
  private static CustomTool toTool(@NotNull ToolState toolState) {
    String command = StringUtil.notNullize(toolState.command);
    return new CustomTool(toolState.name, command, StringUtil.notNullize(toolState.workDirectory));
  }

  @NotNull
  private static ToolState toState(@NotNull CustomTool tool) {
    ToolState toolState = new ToolState();
    toolState.name = tool.name();
    toolState.command = tool.command();
    toolState.workDirectory = tool.workDirectory();
    return toolState;
  }

  @Nullable
  private ContainerTools find(@NotNull String ownerId) {
    return state.containers.stream()
      .filter(container -> ownerId.equals(container.ownerId))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private ContainerTools getOrCreate(@NotNull String ownerId) {
    ContainerTools container = find(ownerId);
    if (container == null) {
      container = new ContainerTools();
      container.ownerId = ownerId;
      state.containers.add(container);
    }
    return container;
  }
}
