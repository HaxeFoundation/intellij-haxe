package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibInstalledIndex;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
  // fired when a background lime/nme evaluation lands, so the owner can re-scan
  private final Runnable onEvaluationReady;

  HaxeToolWindowModelBuilder(@NotNull Project project, @NotNull Runnable onEvaluationReady) {
    this.project = project;
    this.onEvaluationReady = onEvaluationReady;
  }

  /** {@code sectionIds}/{@code sectionLabels}/{@code sectionDescriptors} list a multi-section hxml's {@code --next} compilations (empty otherwise); {@code selectedSection} indexes into them. */
  record FileEntry(HaxeBuildFile buildFile, HaxeBuildFileInfo info, boolean manual, List<ActionNode> actions,
                   List<String> sectionIds, List<String> sectionLabels, List<String> sectionDescriptors,
                   int selectedSection) {
  }

  /** A build-file container: a module, or the project root for files outside every module. */
  record ContainerEntry(String id, String displayName, boolean projectRoot,
                        List<FileEntry> files, @Nullable String activePath,
                        List<String> testsPaths,
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
  record EnvironmentData(String sdkDisplay, boolean sdkMissing, String languageLevelDisplay,
                         List<EnvDefineNode> defines, Set<String> activeBuildFileDefines,
                         @Nullable String customTarget) {
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
    List<String> allPaths = rawContainers.stream()
      .flatMap(raw -> raw.files().stream())
      .map(entry -> entry.buildFile().file().getPath())
      .toList();

    String activePath = HaxeActiveBuildFileStore.getInstance(project).resolveActivePath(allPaths);
    FileEntry activeEntry = rawContainers.stream()
      .flatMap(raw -> raw.files().stream())
      .filter(entry -> entry.buildFile().file().getPath().equals(activePath))
      .findFirst()
      .orElse(null);

    Set<String> activeDefines = activeDefineNames(activeEntry);

    List<ContainerEntry> containers = new ArrayList<>();
    for (RawContainer raw : rawContainers) {
      EnvCompileCommandNode compileCommand = compileCommandNode(raw.id(), raw.files());
      EnvironmentData environment = buildEnvironmentData(raw.id(), activeDefines);
      CompilationServerNode server = compilationServerNode(raw.id(), compileCommand.connectEligible());
      List<String> testsPaths = resolveTestsPaths(raw);
      List<ToolNode> tools = containerTools(raw);
      boolean userConfigured = hasUserConfiguration(raw.id());
      ContainerEntry container = new ContainerEntry(raw.id(), raw.displayName(), raw.projectRoot(), raw.files(),
                                                    activePath, testsPaths, environment, compileCommand, server,
                                                    tools, userConfigured);
      containers.add(container);
    }
    return containers;
  }

  /** The active build file's define names - the environment rows mark overrides of these. */
  @NotNull
  private static Set<String> activeDefineNames(@Nullable FileEntry activeEntry) {
    if (activeEntry == null) return Set.of();
    return activeEntry.info().defines().stream()
      .map(HaxeBuildFileInfo.HaxeDefine::name)
      .collect(Collectors.toSet());
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
    VirtualFile rootDir = containerRootDir(raw);
    if (rootDir == null) return List.of();

    List<ToolNode> tools = new ArrayList<>();
    String workDirectory = rootDir.getPath();
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(raw.id());
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, environmentSdk);
    List<String> sources = sourceArguments(raw, rootDir);

    if (rootDir.findChild(HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME) != null) {
      String name = HaxeBundle.message("haxe.toolwindow.tool.checkstyle");
      List<String> command = toolCommand(haxelib, HaxeToolConfigs.CHECKSTYLE_HAXELIB, List.of(), sources);
      tools.add(detectedTool(raw.id(), name, command, HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME, workDirectory));
    }
    if (rootDir.findChild(HaxeToolConfigs.FORMATTER_CONFIG_NAME) != null) {
      String formatName = HaxeBundle.message("haxe.toolwindow.tool.format");
      String checkName = HaxeBundle.message("haxe.toolwindow.tool.format.check");
      List<String> format = toolCommand(haxelib, HaxeToolConfigs.FORMATTER_HAXELIB, List.of(), sources);
      List<String> check = toolCommand(haxelib, HaxeToolConfigs.FORMATTER_HAXELIB, List.of("--check"), sources);
      tools.add(detectedTool(raw.id(), formatName, format, HaxeToolConfigs.FORMATTER_CONFIG_NAME, workDirectory));
      tools.add(detectedTool(raw.id(), checkName, check, HaxeToolConfigs.FORMATTER_CONFIG_NAME, workDirectory));
    }
    String projectRoot = HaxeContainers.projectRootPath(project);
    for (HaxeCustomToolsStore.CustomTool custom : HaxeCustomToolsStore.getInstance(project).getTools(raw.id())) {
      String expanded = HaxeCustomCommands.expandRoots(custom.command(), workDirectory, projectRoot);
      List<String> command = HaxeCustomCommands.parse(expanded);
      String toolWorkDirectory =
        HaxeCustomCommands.resolveWorkDirectory(custom.workDirectory(), workDirectory, workDirectory, projectRoot);
      tools.add(new ToolNode(raw.id(), custom.name(), command, toolWorkDirectory, custom.command(), true));
    }
    return tools;
  }

  /** {@code haxelib run <tool> [modes] -s <source>...}: mode flags (--check) go before the source list. */
  @NotNull
  private static List<String> toolCommand(@NotNull String haxelib, @NotNull String tool,
                                          @NotNull List<String> modes, @NotNull List<String> sources) {
    List<String> command = new ArrayList<>(List.of(haxelib, "run", tool));
    command.addAll(modes);
    for (String source : sources) {
      command.addAll(List.of("-s", source));
    }
    return command;
  }

  /** The row's gray tail names the config file the tool was detected from, not the full command. */
  @NotNull
  private static ToolNode detectedTool(@NotNull String containerId, @NotNull String name, @NotNull List<String> command,
                                       @NotNull String configName, @NotNull String workDirectory) {
    return new ToolNode(containerId, name, command, workDirectory, configName, false);
  }

  @Nullable
  private VirtualFile containerRootDir(@NotNull RawContainer raw) {
    if (raw.projectRoot()) {
      return ProjectUtil.guessProjectDir(project);
    }
    Module module = ModuleManager.getInstance(project).findModuleByName(raw.id());
    if (module == null) return null;
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    return contentRoots.length == 0 ? null : contentRoots[0];
  }

  /** Source roots relative to the container root; without any, the whole container directory. */
  @NotNull
  private List<String> sourceArguments(@NotNull RawContainer raw, @NotNull VirtualFile rootDir) {
    Module module = raw.projectRoot() ? findProjectRootModule()
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

  /** One installed haxelib: the selected version (null when none is set) and every installed version. */
  record InstalledLibrary(@Nullable String selectedVersion, @NotNull Set<String> versions) {
  }

  /**
   * Haxelib's install state per library (lower-cased name), or null when haxelib
   * is unavailable. Runs an external process - call outside read actions.
   */
  @Nullable
  static Map<String, InstalledLibrary> fetchInstalledLibraryVersions(@NotNull Project project) {
    Sdk sdk = HaxeToolPathResolver.findConfiguredSdk(project);
    VirtualFile workDir = ProjectUtil.guessProjectDir(project);
    if (sdk == null || workDir == null) return null;

    HaxelibInstalledIndex index = HaxelibInstalledIndex.fetchFromHaxelib(sdk, workDir);
    Map<String, InstalledLibrary> byName = new HashMap<>();
    for (String name : index.getInstalledLibraries()) {
      InstalledLibrary library = new InstalledLibrary(index.getSelectedVersion(name), index.getInstalledVersions(name));
      byName.put(name.toLowerCase(Locale.ROOT), library);
    }
    return byName;
  }

  @NotNull
  private List<RawContainer> collectRawContainers() {
    List<RawContainer> rawContainers = new ArrayList<>();

    // The module whose content root is the project base dir IS the project - its build
    // files belong to the project node, and it gets no module row of its own.
    Module rootModule = findProjectRootModule();
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

  @Nullable
  private Module findProjectRootModule() {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    return baseDir == null ? null : ProjectFileIndex.getInstance(project).getModuleForFile(baseDir);
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

    HaxeBuildFileInfo info = effectiveInfo(containerId, buildFile);
    boolean manual = manualPaths.contains(buildFile.file().getPath());
    List<ActionNode> actions = buildFileActions(containerId, buildFile);
    return new FileEntry(buildFile, info, manual, actions, sectionIds, sectionLabels(sectionIds), sectionDescriptors, selectedSection);
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
   * The file's info for the tree. Lime-family files get their defines and libraries
   * from the LimeProjectParser evaluation for the selected target - conditionals
   * evaluated, toolchain defines included, and the library list is the FULL
   * resolved set (declared + transitive via include.xml/haxelib.json), so the
   * Libraries node shows everything the build actually loads. Falls back to the
   * raw parse until the background evaluation lands (or when only the legacy
   * lime-display path ran - its hxml carries no library identities).
   */
  @NotNull
  private HaxeBuildFileInfo effectiveInfo(@NotNull String containerId, @NotNull HaxeBuildFile buildFile) {
    HaxeBuildFileInfo raw = HaxeBuildSections.inspectSelected(project, buildFile);
    HaxeBuildFileType type = buildFile.type();
    if (type == HaxeBuildFileType.NMML) {
      return nmeEffectiveInfo(containerId, buildFile, raw);
    }
    if (!LimeProjects.isLimeFamily(type)) {
      return raw;
    }

    String targetFlag = LimeProjects.selectedTargetFlag(project, type, buildFile.file());
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    HaxeBuildFileInfo display = HaxeLimeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(buildFile, targetFlag, environmentSdk, onEvaluationReady);
    if (display == null) {
      return raw;
    }
    var libraries = !display.libraries().isEmpty() ? display.libraries() : raw.libraries();
    // target + output come from the evaluation too: the actual haxe target and
    // artifact path of the SELECTED lime target (the raw xml declares neither)
    return display.withLibraries(libraries);
  }

  /**
   * NMML info: target + artifact derive statically from the selected target
   * (the nme tool's output layout is fixed), while defines, classpaths and the
   * FULL library set (include.nmml transitives, asset handlers) come from the
   * background `nme prepare` evaluation - the raw xml parse serves until it
   * lands. The prepared hxml flattens libs into classpaths, so a run whose
   * derived library list is empty keeps the declared one.
   */
  @NotNull
  private HaxeBuildFileInfo nmeEffectiveInfo(@NotNull String containerId,
                                             @NotNull HaxeBuildFile buildFile,
                                             @NotNull HaxeBuildFileInfo raw) {
    VirtualFile file = buildFile.file();
    HaxeBuildFileInfo withArtifact = NmeProjects.withTargetArtifact(project, file, raw);

    String targetFlag = NmeProjects.selectedTargetFlag(project, file);
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    HaxeNmeProjectInfoService.Evaluation evaluation = HaxeNmeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(buildFile, targetFlag, environmentSdk, onEvaluationReady);
    if (evaluation == null) {
      return withArtifact;
    }
    HaxeBuildFileInfo prepared = evaluation.info();
    var libraries = !prepared.libraries().isEmpty() ? prepared.libraries() : raw.libraries();
    return prepared.withTarget(withArtifact.target(), withArtifact.targetOutput()).withLibraries(libraries);
  }

  /**
   * The build file's runnable actions: defaults for its type (using the file's
   * selected target and its container's environment SDK) plus its custom actions.
   * Everything runs in the build file's own directory.
   */
  @NotNull
  private List<ActionNode> buildFileActions(@NotNull String containerId, @NotNull HaxeBuildFile buildFile) {
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
      List<String> command = HaxeCustomCommands.parse(HaxeCustomCommands.expandRoots(expanded, moduleRoot, projectRoot));
      String actionWorkDirectory =
        HaxeCustomCommands.resolveWorkDirectory(custom.workDirectory(), workDirectory, moduleRoot, projectRoot);
      actions.add(new ActionNode(ownerId, custom.name(), command, actionWorkDirectory, expanded, true));
    }
    return actions;
  }

  private void addDefaultActions(@NotNull List<ActionNode> actions,
                                 @NotNull String ownerId,
                                 @NotNull HaxeBuildFile buildFile,
                                 @Nullable String environmentSdk,
                                 @Nullable String workDirectory) {
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(buildFile.type());
    for (String actionName : buildSystem.defaultActionNames()) {
      List<String> command = buildSystem.actionCommand(project, environmentSdk, buildFile, actionName);
      String presentable = buildSystem.presentableCommand(project, buildFile, actionName);
      actions.add(new ActionNode(ownerId, actionName, Objects.requireNonNull(command), workDirectory, presentable, false));
    }
  }

  /**
   * The container's Compile command row: the chosen file's default build action (or a
   * designated action of that file as override) plus extra arguments.
   */
  @NotNull
  private EnvCompileCommandNode compileCommandNode(@NotNull String containerId, @NotNull List<FileEntry> files) {
    List<String> candidatePaths = files.stream()
      .map(entry -> entry.buildFile().file().getPath())
      .toList();
    Map<String, List<String>> actionNamesByFile = files.stream()
      .collect(Collectors.toMap(entry -> entry.buildFile().file().getPath(), HaxeToolWindowModelBuilder::actionNames));

    HaxeEnvironmentStore.CompileCommand stored = HaxeEnvironmentStore.getInstance(project).getCompileCommand(containerId);
    FileEntry chosen = chosenEntry(files, stored);
    if (chosen == null) {
      String notSet = HaxeBundle.message("haxe.toolwindow.compile.command.not.set");
      return new EnvCompileCommandNode(containerId, notSet, null, null, candidatePaths, actionNamesByFile, false);
    }

    HaxeBuildFile buildFile = chosen.buildFile();
    ActionNode baseAction = resolveBaseAction(chosen, stored, buildFile);
    if (baseAction == null || baseAction.command().isEmpty()) {
      String unsupported = HaxeBundle.message("haxe.toolwindow.compile.command.unsupported", buildFile.file().getName());
      return new EnvCompileCommandNode(containerId, unsupported, null, null, candidatePaths, actionNamesByFile, false);
    }

    List<String> command = new ArrayList<>(baseAction.command());
    command.addAll(ParametersListUtil.parse(stored.arguments()));
    String display = StringUtil.trimTrailing(baseAction.presentableCommand() + " " + stored.arguments());
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    boolean connectEligible = HaxeCompileCommands.connectEligible(project, environmentSdk, command, buildFile.file());
    return new EnvCompileCommandNode(containerId, display, command, baseAction.workDirectory(), candidatePaths,
                                     actionNamesByFile, connectEligible);
  }

  /// The entry the stored compile command points at; null when unset or no longer scanned.
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
  private EnvironmentData buildEnvironmentData(@NotNull String containerId, @NotNull Set<String> activeFileDefines) {
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

    List<EnvDefineNode> defines = environmentStore.getDefines(containerId).stream()
      .map(define -> new EnvDefineNode(containerId, define.name(), define.value(), define.effect(),
                                       activeFileDefines.contains(define.name())))
      .toList();
    String customTarget = environmentStore.getCustomTarget(containerId);
    return new EnvironmentData(sdkDisplay, sdkMissing, levelDisplay, defines, activeFileDefines, customTarget);
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
