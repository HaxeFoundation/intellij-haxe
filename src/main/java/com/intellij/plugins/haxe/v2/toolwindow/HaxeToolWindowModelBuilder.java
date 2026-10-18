package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeBuildToolFailures;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeEffectiveBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.toolwindow.tree.*;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-side model for the Haxe tool window: scans containers (modules and the
 * project root), merges detected and manually added build files, parses each
 * and resolves the per-container environment, compile-command and server rows.
 * Produces plain data - {@link HaxeToolWindowTreeBuilder} turns it into Swing nodes.
 */
final class HaxeToolWindowModelBuilder {

  private final Project project;

  HaxeToolWindowModelBuilder(@NotNull Project project) {
    this.project = project;
  }

  /** {@code sectionIds}/{@code sectionLabels}/{@code sectionDescriptors} list a multi-section hxml's {@code --next} compilations (empty otherwise); {@code selectedSection} indexes into them. */
  record FileEntry(HaxeBuildFile buildFile, HaxeBuildFileInfo info, @Nullable String toolFailure, boolean manual,
                   List<ActionNode> actions,
                   List<String> sectionIds, List<String> sectionLabels, List<String> sectionDescriptors,
                   int selectedSection, IdeOverridesData ideOverrides) {
  }

  /**
   * A build file's IDE overrides as the tree shows them.
   * {@code customTargetApplicable} is false for build types that always
   * select a target; {@code buildTarget} names the target the build selects
   * itself, null when it selects none.
   */
  record IdeOverridesData(List<DefineOverride> defines, @Nullable String customTarget,
                          boolean customTargetApplicable, @Nullable String buildTarget) {
  }

  /**
   * A build-file container: a module, or the project root for files outside
   * every module. {@code testsPaths} are its effective tests builds (marked or
   * conventional, framework-gated); {@code markedTestsPaths} the raw store
   * marks, which outlive a removed framework lib.
   */
  record ContainerEntry(String id, String displayName, boolean projectRoot,
                        List<FileEntry> files, @Nullable String activePath,
                        List<String> testsPaths,
                        List<String> markedTestsPaths,
                        EnvironmentData environment,
                        EnvCompileCommandNode compileCommand,
                        CompilationServerNode server,
                        List<ToolNode> tools,
                        boolean userConfigured) {

    /** A module row the hide-empty toggle removes: no build files, no tools and nothing user-configured. */
    boolean emptyModule() {
      return !projectRoot && files.isEmpty() && tools.isEmpty() && !userConfigured;
    }
  }

  /** The container's environment as shown in the tree, resolved during the scan read action. */
  record EnvironmentData(String sdkDisplay, boolean sdkMissing, String languageLevelDisplay) {
  }

  /** A container before global active-file resolution. */
  private record RawContainer(String id, String displayName, boolean projectRoot, List<FileEntry> files) {
  }

  /** Builds the container model the tree renders. Call under a read action. */
  @NotNull
  List<ContainerEntry> build() {
    List<RawContainer> rawContainers = collectRawContainers();

    // The active build file is a PROJECT-wide singleton: the IDE keeps one parse tree
    // per file, so conditional compilation can only follow one build configuration.
    String activePath = HaxeKnownBuildFiles.effectiveActivePath(project);

    List<ContainerEntry> containers = new ArrayList<>();
    for (RawContainer raw : rawContainers) {
      List<String> testsPaths = resolveTestsPaths(raw);
      EnvCompileCommandNode compileCommand = compileCommandNode(raw.id(), raw.files(), testsPaths);
      EnvironmentData environment = buildEnvironmentData(raw.id());
      CompilationServerNode server = compilationServerNode(raw.id(), compileCommand.connectEligible());
      List<String> markedTestsPaths = HaxeTestsBuildFileStore.getInstance(project).getTestsFilePaths(raw.id());
      List<ToolNode> tools = containerTools(raw);
      boolean userConfigured = hasUserConfiguration(raw.id());
      ContainerEntry container = new ContainerEntry(raw.id(), raw.displayName(), raw.projectRoot(), raw.files(),
                                                    activePath, testsPaths, markedTestsPaths, environment, compileCommand,
                                                    server, tools, userConfigured);
      containers.add(container);
    }
    return containers;
  }

  /** Whether the user attached anything to the container: environment overrides, custom actions or custom tools. */
  private boolean hasUserConfiguration(@NotNull String containerId) {
    return HaxeEnvironmentStore.getInstance(project).hasUserOverrides(containerId)
           || !HaxeCustomActionsStore.getInstance(project).getActions(containerId).isEmpty()
           || !HaxeCustomToolsStore.getInstance(project).getTools(containerId).isEmpty();
  }

  /**
   * The container's tool rows: commands for tool configs detected at the container
   * root, then the user's custom tools. Everything runs in the container root so
   * the tools' own config discovery and relative excludes resolve as if run there;
   * haxelib forwards that directory to the tool (the HAXELIB_RUN cwd convention).
   */
  @NotNull
  private List<ToolNode> containerTools(@NotNull RawContainer raw) {
    VirtualFile rootDir = HaxeContainers.containerRootDir(project, raw.id());
    if (rootDir == null) return List.of();

    List<ToolNode> tools = new ArrayList<>();
    String workDirectory = rootDir.getPath();
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(raw.id());
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
    List<String> sources = sourceArguments(raw, rootDir);

    for (HaxeDetectedTools.Spec spec : HaxeDetectedTools.in(rootDir)) {
      List<String> command = HaxeToolConfigs.toolCommand(haxelib, spec.haxelib(), spec.modes(), sources);
      tools.add(detectedTool(raw.id(), spec.name(), command, spec.configName(), workDirectory));
    }
    String projectRoot = HaxeContainers.projectRootPath(project);
    for (HaxeCustomToolsStore.CustomTool custom : HaxeCustomToolsStore.getInstance(project).getTools(raw.id())) {
      List<String> command = HaxeCustomCommands.commandLine(project, environmentSdk, custom.command(), workDirectory, projectRoot);
      String toolWorkDirectory =
        HaxeCustomCommands.resolveWorkDirectory(custom.workDirectory(), workDirectory, workDirectory, projectRoot);
      tools.add(new ToolNode(raw.id(), custom.name(), command, toolWorkDirectory, custom.command(), true));
    }
    return tools;
  }

  /** The row's gray tail names the config file the tool was detected from, not the full command. */
  @NotNull
  private static ToolNode detectedTool(@NotNull String containerId, @NotNull String name, @NotNull List<String> command,
                                       @NotNull String configName, @NotNull String workDirectory) {
    return new ToolNode(containerId, name, command, workDirectory, configName, false);
  }

  /** Source roots relative to the container root; without any, the whole container directory. */
  @NotNull
  private List<String> sourceArguments(@NotNull RawContainer raw, @NotNull VirtualFile rootDir) {
    Module module = raw.projectRoot() ? HaxeContainers.projectRootModule(project)
                                      : ModuleManager.getInstance(project).findModuleByName(raw.id());
    if (module == null) return List.of(".");
    List<String> sources = new ArrayList<>();
    for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots()) {
      String relative = VfsUtilCore.getRelativePath(sourceRoot, rootDir);
      sources.add(relative != null ? relative : sourceRoot.getPath());
    }
    return sources.isEmpty() ? List.of(".") : sources;
  }

  /** The container's tests build files - the framework gating and store resolution live in {@link HaxeTestFrameworks#testsBuildPaths}. */
  @NotNull
  private List<String> resolveTestsPaths(@NotNull RawContainer raw) {
    Map<String, List<HaxeBuildFileInfo.HaxeLibDependency>> librariesByPath = new LinkedHashMap<>();
    for (var entry : raw.files()) {
      librariesByPath.put(entry.buildFile().file().getPath(), entry.info().libraries());
    }
    return HaxeTestFrameworks.testsBuildPaths(project, raw.id(), librariesByPath);
  }

  @NotNull
  private List<RawContainer> collectRawContainers() {
    List<RawContainer> rawContainers = new ArrayList<>();

    Module rootModule = HaxeContainers.projectRootModule(project);
    String rootContainerId = rootModule != null ? rootModule.getName() : HaxeContainers.PROJECT_ROOT_CONTAINER;
    List<HaxeBuildFile> rootDetected = new ArrayList<>(HaxeBuildFileScanner.scanProjectRoot(project));
    if (rootModule != null) {
      rootDetected.addAll(HaxeBuildFileScanner.scan(rootModule));
    }
    List<FileEntry> rootFiles = mergeAndInspect(rootContainerId, rootDetected);
    // Even with no files yet, a root module means the project node gets its Build group
    // so files can be added manually.
    if (!rootFiles.isEmpty() || rootModule != null) {
      rawContainers.add(new RawContainer(rootContainerId, project.getName(), true, rootFiles));
    }

    // getModules() hands out the manager's cached array: sorting it in place mutates
    // platform state, and two concurrent rebuilds sorting the same array break TimSort
    List<Module> modules = new ArrayList<>(Arrays.asList(ModuleManager.getInstance(project).getModules()));
    modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
    for (Module module : modules) {
      if (module.equals(rootModule)) continue;
      List<FileEntry> files = mergeAndInspect(module.getName(), HaxeBuildFileScanner.scan(module));
      rawContainers.add(new RawContainer(module.getName(), module.getName(), false, files));
    }
    return rawContainers;
  }

  /** Combines auto-detected files with manual additions, drops hidden ones, and parses each file. */
  @NotNull
  private List<FileEntry> mergeAndInspect(@NotNull String containerId, @NotNull List<HaxeBuildFile> detected) {
    Set<String> manualPaths = new LinkedHashSet<>(HaxeBuildFilesStore.getInstance(project).getAddedPaths(containerId));

    return HaxeKnownBuildFiles.mergeWithStore(project, containerId, detected).stream()
      .sorted(Comparator.comparing(buildFile -> buildFile.file().getName(), String.CASE_INSENSITIVE_ORDER))
      .map(buildFile -> fileEntry(containerId, buildFile, manualPaths))
      .toList();
  }

  @NotNull
  private FileEntry fileEntry(@NotNull String containerId, @NotNull HaxeBuildFile buildFile,
                              @NotNull Set<String> manualPaths) {
    List<String> sections = multiSectionContents(buildFile);
    List<String> sectionIds = sections.isEmpty()
                              ? List.of()
                              : HxmlFileParser.sectionIds(buildFile.file().getName(), sections);
    List<String> sectionDescriptors = sections.isEmpty() ? List.of() : HxmlFileParser.sectionDescriptors(sections);
    int selectedSection = sectionIds.isEmpty()
                          ? 0
                          : HaxeSectionSelectionStore.getInstance(project).getSelectedSection(buildFile.file(), sectionIds);

    HaxeBuildFileInfo info = HaxeEffectiveBuildFileInfo.of(project, buildFile);
    String toolFailure = HaxeBuildToolFailures.getInstance(project).lastFailure(buildFile.file().getPath());
    boolean manual = manualPaths.contains(buildFile.file().getPath());
    List<ActionNode> actions = buildFileActions(containerId, buildFile, HaxeProgramLaunches.launchOutput(info));
    IdeOverridesData ideOverrides = ideOverridesData(buildFile, info);
    return new FileEntry(buildFile, info, toolFailure, manual, actions, sectionIds, sectionLabels(sectionIds), sectionDescriptors,
                         selectedSection, ideOverrides);
  }

  @NotNull
  private IdeOverridesData ideOverridesData(@NotNull HaxeBuildFile buildFile, @NotNull HaxeBuildFileInfo info) {
    HaxeIdeOverridesStore store = HaxeIdeOverridesStore.getInstance(project);
    String path = buildFile.file().getPath();
    boolean customTargetApplicable = buildFile.type() == HaxeBuildFileType.HXML;
    String buildTarget = ownTargetOfHxml(buildFile, info);
    return new IdeOverridesData(store.getDefines(path), store.getCustomTarget(path), customTargetApplicable, buildTarget);
  }

  /** The target an hxml selects itself, by name; null for other build types and for an hxml selecting none. */
  @Nullable
  private String ownTargetOfHxml(@NotNull HaxeBuildFile buildFile, @NotNull HaxeBuildFileInfo info) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return null;
    if (!HaxeIdeOverrides.buildSelectsTarget(project, buildFile)) return null;
    if (info.target() != null) return info.target().toString();
    String customTarget = info.customTargetName();
    return customTarget != null ? customTarget : HaxeBundle.message("haxe.toolwindow.node.ide.custom.target.build.own");
  }

  /** The {@code --next} sections of a multi-section hxml; empty for single-section files and other build types. */
  @NotNull
  private List<String> multiSectionContents(@NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return List.of();
    List<String> sections = HaxeBuildFileInspector.sectionContents(project, buildFile.file());
    return sections.size() < 2 ? List.of() : sections;
  }

  /**
   * Section labels are position-prefixed FILENAMES: the file the section's
   * content came from, the build file's own name for inline sections. The
   * leading number is already the row's unique handle, so an id's "#n"
   * occurrence suffix is NOT repeated in the label; what DISTINGUISHES the
   * sections - target, output, main class - is the separate (gray)
   * descriptor list.
   */
  @NotNull
  private static List<String> sectionLabels(@NotNull List<String> sectionIds) {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < sectionIds.size(); i++) {
      String id = sectionIds.get(i);
      int hash = id.lastIndexOf('#');
      String name = hash < 0 ? id : id.substring(0, hash);
      labels.add((i + 1) + ": " + name);
    }
    return labels;
  }


  /**
   * The build file's runnable actions: defaults for its type (using the file's
   * selected target and its container's environment SDK) plus its custom actions.
   * Everything runs in the build file's own directory.
   */
  @NotNull
  private List<ActionNode> buildFileActions(@NotNull String containerId,
                                            @NotNull HaxeBuildFile buildFile,
                                            @Nullable String launchOutput) {
    List<ActionNode> actions = new ArrayList<>();
    String ownerId = buildFile.file().getPath();
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    String workDirectory = HaxeBuildWorkDirectories.workDirectory(project, buildFile.file());

    addDefaultActions(actions, ownerId, buildFile, environmentSdk, workDirectory);
    String moduleRoot = HaxeContainers.containerRootPath(project, containerId);
    String projectRoot = HaxeContainers.projectRootPath(project);
    for (HaxeCustomActionsStore.CustomAction custom : HaxeCustomActionsStore.getInstance(project).getActions(ownerId)) {
      // the row shows the expanded command, so a ${target} action reads like the default ones
      String expanded = HaxeCustomCommands.expandTarget(project, buildFile.file(), buildFile.type(), custom.command());
      List<String> command = HaxeCustomCommands.commandLine(project, environmentSdk, expanded, moduleRoot, projectRoot);
      String actionWorkDirectory =
        HaxeCustomCommands.resolveWorkDirectory(custom.workDirectory(), workDirectory, moduleRoot, projectRoot);
      List<String> runBefore = custom.runBefore().stream()
        .map(reference -> HaxeActionReferences.labelOrMissing(project, ownerId, reference))
        .toList();
      ActionNode actionNode = new ActionNode(ownerId, custom.name(), command, actionWorkDirectory, expanded, true, custom.launch(),
                                             launchOutput, runBefore);
      actions.add(actionNode);
    }
    return actions;
  }

  private void addDefaultActions(@NotNull List<ActionNode> actions,
                                 @NotNull String ownerId,
                                 @NotNull HaxeBuildFile buildFile,
                                 @Nullable String environmentSdk,
                                 @Nullable String workDirectory) {
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(buildFile.type());
    for (String actionName : buildSystem.actionNames(project, buildFile)) {
      List<String> command = Objects.requireNonNull(buildSystem.actionCommand(project, environmentSdk, buildFile, actionName));
      String presentable = buildSystem.presentableCommand(project, buildFile, actionName);
      String ownWorkDirectory = buildSystem.actionWorkDirectory(project, buildFile, actionName);
      String actionWorkDirectory = ownWorkDirectory != null ? ownWorkDirectory : workDirectory;
      ActionNode actionNode = new ActionNode(ownerId, actionName, command, actionWorkDirectory, presentable, false, null, null, List.of());
      actions.add(actionNode);
    }
  }

  /**
   * The container's Build command row: the chosen file's default build action (or a
   * designated action of that file as override) plus extra arguments. Without a
   * command set, the row shows the container's default command, marked as such.
   */
  @NotNull
  private EnvCompileCommandNode compileCommandNode(@NotNull String containerId,
                                                   @NotNull List<FileEntry> files,
                                                   @NotNull List<String> testsPaths) {
    List<String> candidatePaths = files.stream()
      .map(entry -> entry.buildFile().file().getPath())
      .toList();
    Map<String, List<String>> actionNamesByFile = files.stream()
      .collect(Collectors.toMap(entry -> entry.buildFile().file().getPath(), HaxeToolWindowModelBuilder::actionNames));

    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    boolean skipped = store.isBuildSkipped(containerId);
    HaxeEnvironmentStore.CompileCommand stored = store.getCompileCommand(containerId);
    HaxeEnvironmentStore.CompileCommand defaultCommand =
      HaxeCompileCommands.defaultCommand(project, candidatePaths, testsPaths);
    String defaultPath = defaultCommand == null ? null : defaultCommand.buildFilePath();
    HaxeEnvironmentStore.CompileCommand compileCommand = stored != null ? stored : defaultCommand;
    FileEntry chosen = skipped ? null : chosenEntry(files, compileCommand);
    if (chosen == null) {
      String unset = skipped
                     ? HaxeBundle.message("haxe.toolwindow.compile.command.skipped")
                     : HaxeBundle.message("haxe.toolwindow.compile.command.not.set");
      return new EnvCompileCommandNode(containerId, unset, null, null, candidatePaths, actionNamesByFile, defaultPath,
                                       false);
    }

    HaxeBuildFile buildFile = chosen.buildFile();
    ActionNode baseAction = resolveBaseAction(chosen, compileCommand, buildFile);
    if (baseAction == null || baseAction.command().isEmpty()) {
      String unsupported = HaxeBundle.message("haxe.toolwindow.compile.command.unsupported", buildFile.file().getName());
      return new EnvCompileCommandNode(containerId, unsupported, null, null, candidatePaths, actionNamesByFile, defaultPath,
                                       false);
    }

    List<String> command = new ArrayList<>(baseAction.command());
    command.addAll(ParametersListUtil.parse(compileCommand.arguments()));
    String display = StringUtil.trimTrailing(baseAction.presentableCommand() + " " + compileCommand.arguments());
    String shown = stored == null ? HaxeBundle.message("haxe.toolwindow.compile.command.default", display) : display;
    String environmentSdk = store.getSdkName(containerId);
    boolean connectEligible = HaxeCompileCommands.connectEligible(project, environmentSdk, command, buildFile.file());
    return new EnvCompileCommandNode(containerId, shown, command, baseAction.workDirectory(), candidatePaths,
                                     actionNamesByFile, defaultPath, connectEligible);
  }

  /// The entry the compile command points at; null when there is none or its file is no longer scanned.
  private static FileEntry chosenEntry(List<FileEntry> files, @Nullable HaxeEnvironmentStore.CompileCommand stored) {
    if (stored == null) return null;
    return files.stream()
      .filter(entry -> entry.buildFile().file().getPath().equals(stored.buildFilePath()))
      .findFirst()
      .orElse(null);
  }

  /// The stored action override when it still exists, else the type's default build action.
  private static ActionNode resolveBaseAction(FileEntry chosen, HaxeEnvironmentStore.CompileCommand stored,
                                              HaxeBuildFile buildFile) {
    if (stored.actionName() != null) {
      ActionNode overrideAction = findAction(chosen.actions(), stored.actionName());
      if (overrideAction != null) return overrideAction;
    }
    return findAction(chosen.actions(), HaxeBuildSystem.of(buildFile.type()).defaultBuildActionName());
  }

  @NotNull
  private static List<String> actionNames(@NotNull FileEntry entry) {
    return entry.actions().stream()
      .map(ActionNode::name)
      .toList();
  }

  @Nullable
  private static ActionNode findAction(@NotNull List<ActionNode> actions, @NotNull String name) {
    return actions.stream()
      .filter(action -> action.name().equals(name))
      .findFirst()
      .orElse(null);
  }

  @NotNull
  private EnvironmentData buildEnvironmentData(@NotNull String containerId) {
    HaxeEnvironmentStore environmentStore = HaxeEnvironmentStore.getInstance(project);

    String sdkName = environmentStore.getSdkName(containerId);
    String sdkDisplay;
    boolean sdkMissing = false;
    if (sdkName == null) {
      String projectSdk = HaxeBuildToolSettings.getInstance(project).getSdkName();
      String fallback = projectSdk != null ? projectSdk : HaxeBundle.message("haxe.toolwindow.node.environment.sdk.none");
      sdkDisplay = HaxeBundle.message("haxe.toolwindow.node.environment.sdk.default", fallback);
    }
    else {
      sdkDisplay = sdkName;
      sdkMissing = ProjectJdkTable.getInstance().findJdk(sdkName) == null;
    }

    // same store the Haxe Compiler settings page edits - the two stay in sync
    HaxeCompilerSettings compilerSettings = HaxeCompilerSettings.getInstance(project);
    HaxeLanguageLevel levelOverride = compilerSettings.getContainerLanguageLevelOverride(containerId);
    String levelDisplay = levelOverride != null
                          ? levelOverride.getPresentableText()
                          : defaultLevelDisplay(compilerSettings, containerId);
    return new EnvironmentData(sdkDisplay, sdkMissing, levelDisplay);
  }

  /** The "Project default (X)" label following the compiler settings' per-container default level. */
  @NotNull
  private static String defaultLevelDisplay(@NotNull HaxeCompilerSettings settings, @NotNull String containerId) {
    String defaultLevel = settings.getDefaultLanguageLevel(containerId).getPresentableText();
    return HaxeBundle.message("haxe.toolwindow.node.environment.sdk.default", defaultLevel);
  }

  @NotNull
  private CompilationServerNode compilationServerNode(@NotNull String containerId, boolean connectEligible) {
    boolean projectEnabled = HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled();
    boolean moduleUses = HaxeEnvironmentStore.getInstance(project).isUsingCompilationServer(containerId);
    // per-module SDKs mean per-SDK server instances - this row reports the one
    // THIS container's compiles connect to, not whichever server happens to run
    String serverId = HaxeCompilationServerManager.serverIdFor(project, containerId);
    int port = HaxeCompilationServerManager.getInstance(project).runningPort(serverId);
    boolean running = port > 0;

    String display;
    if (!HaxeProjectTrust.isTrusted(project)) {
      display = HaxeBundle.message("haxe.trust.toolwindow.hint");
    }
    else if (!projectEnabled) {
      display = HaxeBundle.message("haxe.toolwindow.server.disabled.project");
    }
    else if (!moduleUses) {
      display = HaxeBundle.message("haxe.toolwindow.server.off");
    }
    else if (!connectEligible) {
      display = HaxeBundle.message("haxe.toolwindow.server.not.applicable");
    }
    else {
      // port passed as text - MessageFormat would render the int with grouping separators
      display = running ? HaxeBundle.message("haxe.toolwindow.server.running", String.valueOf(port))
                        : HaxeBundle.message("haxe.toolwindow.server.on.idle");
    }
    String contextFailure = HaxeContextFailures.getInstance(project).lastFailure(containerId);
    return new CompilationServerNode(containerId, display, projectEnabled, moduleUses, running, connectEligible,
                                     contextFailure);
  }
}
