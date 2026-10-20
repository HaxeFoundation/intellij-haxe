package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;

/**
 * User-defined actions per build file (keyed by its path): a name plus a command
 * line executed in the build file's work directory
 * ({@link com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories}),
 * optionally followed by starting a run configuration. Shares
 * {@code .idea/haxeBuildConfig.xml} with the other
 * build-config stores.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeCustomActions", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeCustomActionsStore implements PersistentStateComponent<HaxeCustomActionsStore.State> {

  /**
   * The run configuration a launching action starts: a Haxe configuration
   * created for the action, which runs the build's output, or an existing
   * configuration of any type. Either way the action's command, when it has
   * one, is the configuration's build step; {@code injectDebugArguments} says
   * whether a Debug launch adds the debug compile arguments to that step.
   */
  public sealed interface Launch permits Launch.Create, Launch.Existing {
    boolean injectDebugArguments();

    /**
     * A configuration created for the action: {@code kindId} is a
     * {@code HaxeLaunchKind} id, {@code parameters} the field overrides by
     * parameter key; a blank or missing value keeps the default derived from
     * the build file.
     */
    record Create(@NotNull String kindId, @NotNull Map<String, String> parameters, boolean injectDebugArguments) implements Launch {
    }

    /** An existing configuration, by its name in the run dropdown. */
    record Existing(@NotNull String configurationName, boolean injectDebugArguments) implements Launch {
    }
  }

  /**
   * {@code workDirectory} may carry root variables; blank means the build file's
   * directory. A null {@code launch} is a plain action: its command runs in a
   * console. {@code runBefore} names the actions that run first, in order.
   */
  public record CustomAction(@NotNull String name, @NotNull String command, @NotNull String workDirectory,
                             @Nullable Launch launch, @NotNull List<ActionReference> runBefore) {
    public CustomAction {
      runBefore = List.copyOf(runBefore);
    }

    public CustomAction(@NotNull String name, @NotNull String command, @NotNull String workDirectory, @Nullable Launch launch) {
      this(name, command, workDirectory, launch, List.of());
    }

    public CustomAction(@NotNull String name, @NotNull String command, @NotNull String workDirectory) {
      this(name, command, workDirectory, null);
    }

    @NotNull
    public CustomAction withName(@NotNull String newName) {
      return new CustomAction(newName, command, workDirectory, launch, runBefore);
    }
  }

  /**
   * An action a custom action runs first: {@code action} of the build file at
   * {@code buildFile}, or of the referring action's own file when that is
   * null - so moving a build file leaves its own references intact.
   */
  public record ActionReference(@Nullable String buildFile, @NotNull String action) {
    /** The reference with its build file filled in, read from an action of {@code ownerId}. */
    @NotNull
    public ActionReference qualifiedFor(@NotNull String ownerId) {
      return buildFile != null ? this : new ActionReference(ownerId, action);
    }
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
    public String launchKind = "";
    public List<LaunchParameterState> launchParameters = new ArrayList<>();
    public String runConfiguration = "";
    public boolean injectDebugArguments = true;
    public List<RunBeforeState> runBefore = new ArrayList<>();
  }

  /** A stored reference; an empty {@code buildFile} is the referring action's own file. */
  public static final class RunBeforeState {
    public String buildFile = "";
    public String action;
  }

  public static final class LaunchParameterState {
    public String key;
    public String value = "";
  }

  private State state = new State();
  private final @Nullable Project project;

  public HaxeCustomActionsStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise the store without a project; no events fire then. */
  @TestOnly
  public HaxeCustomActionsStore() {
    this.project = null;
  }

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
        if (actionState.launchParameters == null) {
          actionState.launchParameters = new ArrayList<>();
        }
        actionState.runBefore = cleanedReferences(actionState.runBefore);
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

  /** The build file's custom action by name, or null. */
  @Nullable
  public CustomAction findAction(@NotNull String ownerId, @NotNull String name) {
    return getActions(ownerId).stream()
      .filter(action -> action.name().equals(name))
      .findFirst()
      .orElse(null);
  }

  public void addAction(@NotNull String ownerId, @NotNull CustomAction action) {
    ContainerActions container = getOrCreate(ownerId);
    container.actions.removeIf(existing -> action.name().equals(existing.name));
    container.actions.add(toState(action));
    HaxeBuildSettingsListener.publisher(project).customActionsChanged(ownerId);
  }

  /** Replaces the action named {@code oldName}, keeping its position. */
  public void updateAction(@NotNull String ownerId, @NotNull String oldName, @NotNull CustomAction action) {
    ContainerActions container = getOrCreate(ownerId);
    for (int i = 0; i < container.actions.size(); i++) {
      if (oldName.equals(container.actions.get(i).name)) {
        container.actions.set(i, toState(action));
        HaxeBuildSettingsListener.publisher(project).customActionsChanged(ownerId);
        return;
      }
    }
    container.actions.add(toState(action));
    HaxeBuildSettingsListener.publisher(project).customActionsChanged(ownerId);
  }

  public void removeAction(@NotNull String ownerId, @NotNull String name) {
    ContainerActions container = find(ownerId);
    if (container != null) {
      container.actions.removeIf(action -> name.equals(action.name));
    }
    HaxeBuildSettingsListener.publisher(project).customActionsChanged(ownerId);
  }

  /** The custom actions, of any build file, that run {@code action} of {@code buildFile} before themselves. */
  @NotNull
  public List<ActionReference> dependents(@NotNull String buildFile, @NotNull String action) {
    List<ActionReference> dependents = new ArrayList<>();
    for (ContainerActions container : state.containers) {
      for (ActionState actionState : container.actions) {
        boolean refers = actionState.runBefore.stream().anyMatch(reference -> names(container.ownerId, reference, buildFile, action));
        if (refers) dependents.add(new ActionReference(container.ownerId, actionState.name));
      }
    }
    return dependents;
  }

  /** Points every run-before reference, from any build file, to {@code oldName} of {@code buildFile} at {@code newName}. */
  public void renameReferences(@NotNull String buildFile, @NotNull String oldName, @NotNull String newName) {
    for (ContainerActions container : state.containers) {
      for (ActionState actionState : container.actions) {
        for (RunBeforeState reference : actionState.runBefore) {
          if (names(container.ownerId, reference, buildFile, oldName)) reference.action = newName;
        }
      }
    }
    HaxeBuildSettingsListener.publisher(project).customActionsChanged(buildFile);
  }

  /** Drops every run-before reference, from any build file, to {@code action} of {@code buildFile}. */
  public void dropReferences(@NotNull String buildFile, @NotNull String action) {
    for (ContainerActions container : state.containers) {
      for (ActionState actionState : container.actions) {
        actionState.runBefore.removeIf(reference -> names(container.ownerId, reference, buildFile, action));
      }
    }
    HaxeBuildSettingsListener.publisher(project).customActionsChanged(buildFile);
  }

  /**
   * A build file keeps its custom actions, and the run-before references
   * naming them keep pointing at it, when it or a folder above it is moved
   * or renamed; whether anything changed.
   */
  public boolean moveFile(@NotNull String oldPath, @NotNull String newPath) {
    boolean changed = false;
    for (ContainerActions container : state.containers) {
      if (container.ownerId != null) {
        String movedOwner = HaxeStoredPaths.moved(container.ownerId, oldPath, newPath);
        changed |= !movedOwner.equals(container.ownerId);
        container.ownerId = movedOwner;
      }
      for (ActionState actionState : container.actions) {
        for (RunBeforeState reference : actionState.runBefore) {
          if (reference.buildFile.isEmpty()) continue;
          String movedReference = HaxeStoredPaths.moved(reference.buildFile, oldPath, newPath);
          changed |= !movedReference.equals(reference.buildFile);
          reference.buildFile = movedReference;
        }
      }
    }
    return changed;
  }

  /** Whether a stored reference of an action in {@code ownerId} names {@code action} of {@code buildFile}. */
  private static boolean names(@Nullable String ownerId, @NotNull RunBeforeState reference, @NotNull String buildFile,
                               @NotNull String action) {
    String referenced = reference.buildFile.isEmpty() ? ownerId : reference.buildFile;
    return action.equals(reference.action) && buildFile.equals(referenced);
  }

  /**
   * The actions that run before {@code actionName} of {@code buildFile}, in
   * order, each with its build file filled in: every custom action's
   * run-before list expanded depth-first, across build files, every action at
   * most once. Built-in actions run nothing before themselves. Null when the
   * references loop back to an action already on the way.
   */
  @Nullable
  public List<ActionReference> runBeforeChain(@NotNull String buildFile, @NotNull String actionName) {
    List<ActionReference> order = new ArrayList<>();
    if (!collectRunBefore(new ActionReference(buildFile, actionName), new HashSet<>(), order)) return null;
    return List.copyOf(order.subList(0, order.size() - 1));
  }

  /** Why a pick cannot run before an action: the pick runs that action first already, or its own run-before actions loop. */
  public enum RunBeforeRefusal { RUNS_IT_FIRST, LOOPS }

  /** The refusal for {@code candidate} (its build file filled in) running before {@code action} of {@code buildFile}; null when it may. */
  @Nullable
  public RunBeforeRefusal refusal(@NotNull ActionReference candidate, @NotNull String buildFile, @NotNull String action) {
    List<ActionReference> chain = runBeforeChain(Objects.requireNonNull(candidate.buildFile()), candidate.action());
    if (chain == null) return RunBeforeRefusal.LOOPS;
    return chain.contains(new ActionReference(buildFile, action)) ? RunBeforeRefusal.RUNS_IT_FIRST : null;
  }

  /** Adds {@code node}'s run-before actions, then {@code node}; false when a reference loops back onto {@code onPath}. */
  private boolean collectRunBefore(@NotNull ActionReference node, @NotNull Set<ActionReference> onPath, @NotNull List<ActionReference> order) {
    if (onPath.contains(node)) return false;
    if (order.contains(node)) return true;
    onPath.add(node);
    for (ActionReference before : runBeforeOf(node)) {
      if (!collectRunBefore(before, onPath, order)) return false;
    }
    onPath.remove(node);
    order.add(node);
    return true;
  }

  /** What {@code node} runs first, each build file filled in; nothing for a built-in or unknown action. */
  @NotNull
  private List<ActionReference> runBeforeOf(@NotNull ActionReference node) {
    String buildFile = Objects.requireNonNull(node.buildFile());
    CustomAction action = findAction(buildFile, node.action());
    return action == null ? List.of() : qualified(action.runBefore(), buildFile);
  }

  @NotNull
  private static List<ActionReference> qualified(@NotNull List<ActionReference> references, @NotNull String ownerId) {
    return references.stream()
      .map(reference -> reference.qualifiedFor(ownerId))
      .toList();
  }

  @NotNull
  private static CustomAction toAction(@NotNull ActionState actionState) {
    String command = StringUtil.notNullize(actionState.command);
    String workDirectory = StringUtil.notNullize(actionState.workDirectory);
    return new CustomAction(actionState.name, command, workDirectory, toLaunch(actionState), referencesOf(actionState));
  }

  @NotNull
  private static List<ActionReference> referencesOf(@NotNull ActionState actionState) {
    return actionState.runBefore.stream()
      .map(reference -> new ActionReference(StringUtil.nullize(reference.buildFile), reference.action))
      .toList();
  }

  // the entries a run-before list may hold; anything else came from an older format
  @NotNull
  private static List<RunBeforeState> cleanedReferences(@Nullable List<?> stored) {
    List<RunBeforeState> cleaned = new ArrayList<>();
    if (stored == null) return cleaned;
    for (Object entry : stored) {
      if (entry instanceof RunBeforeState reference && !StringUtil.isEmptyOrSpaces(reference.action)) {
        reference.buildFile = StringUtil.notNullize(reference.buildFile);
        cleaned.add(reference);
      }
    }
    return cleaned;
  }

  @NotNull
  private static RunBeforeState referenceState(@NotNull ActionReference reference) {
    RunBeforeState state = new RunBeforeState();
    state.buildFile = StringUtil.notNullize(reference.buildFile());
    state.action = reference.action();
    return state;
  }

  @Nullable
  private static Launch toLaunch(@NotNull ActionState actionState) {
    if (!StringUtil.isEmptyOrSpaces(actionState.runConfiguration)) {
      return new Launch.Existing(actionState.runConfiguration, actionState.injectDebugArguments);
    }
    if (StringUtil.isEmptyOrSpaces(actionState.launchKind)) return null;
    Map<String, String> parameters = new LinkedHashMap<>();
    for (Object entry : (List<?>)actionState.launchParameters) {
      if (entry instanceof LaunchParameterState parameter && !StringUtil.isEmptyOrSpaces(parameter.key)) {
        parameters.put(parameter.key, StringUtil.notNullize(parameter.value));
      }
    }
    return new Launch.Create(actionState.launchKind, Map.copyOf(parameters), actionState.injectDebugArguments);
  }

  @NotNull
  private static ActionState toState(@NotNull CustomAction action) {
    ActionState actionState = new ActionState();
    actionState.name = action.name();
    actionState.command = action.command();
    actionState.workDirectory = action.workDirectory();
    action.runBefore().forEach(reference -> actionState.runBefore.add(referenceState(reference)));
    switch (action.launch()) {
      case Launch.Create create -> {
        actionState.launchKind = create.kindId();
        create.parameters().forEach((key, value) -> actionState.launchParameters.add(parameterState(key, value)));
        actionState.injectDebugArguments = create.injectDebugArguments();
      }
      case Launch.Existing existing -> {
        actionState.runConfiguration = existing.configurationName();
        actionState.injectDebugArguments = existing.injectDebugArguments();
      }
      case null -> { }
    }
    return actionState;
  }

  @NotNull
  private static LaunchParameterState parameterState(@NotNull String key, @NotNull String value) {
    LaunchParameterState parameter = new LaunchParameterState();
    parameter.key = key;
    parameter.value = value;
    return parameter;
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
