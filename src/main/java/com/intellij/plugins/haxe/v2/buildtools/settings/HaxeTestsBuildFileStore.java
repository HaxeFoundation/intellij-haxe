package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Per-container (module / project root) "tests build files": the build files
 * that supply libs, defines, target and classpaths for the container's test
 * runs. A container may hold SEVERAL — one per sub-project or framework.
 * Shares {@code .idea/haxeBuildConfig.xml} with the other build-settings stores.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeTestsBuildFiles", storages = @Storage("haxeBuildConfig.xml"))
public final class HaxeTestsBuildFileStore implements PersistentStateComponent<HaxeTestsBuildFileStore.State> {

  public static final class State {
    public List<ContainerTestsFile> testsFiles = new ArrayList<>();
    public List<ContainerTestsFile> excludedFiles = new ArrayList<>();
  }

  public static final class ContainerTestsFile {
    public String containerId;
    public String filePath;
  }

  private final @Nullable Project project;
  // cached consumers (the gutter marker context) key on this; bumped on
  // every mutation and on state load
  private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();
  private State state = new State();

  public HaxeTestsBuildFileStore(@NotNull Project project) {
    this.project = project;
  }

  /** State tests exercise load/get/set without a project; no events fire then. */
  @TestOnly
  public HaxeTestsBuildFileStore() {
    this.project = null;
  }

  @NotNull
  public static HaxeTestsBuildFileStore getInstance(@NotNull Project project) {
    return project.getService(HaxeTestsBuildFileStore.class);
  }

  /** Bumped on every marked-set change - cache dependencies use it. */
  @NotNull
  public ModificationTracker getModificationTracker() {
    return modificationTracker;
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    if (state.testsFiles == null) {
      state.testsFiles = new ArrayList<>();
    }
    if (state.excludedFiles == null) {
      state.excludedFiles = new ArrayList<>();
    }
    this.state = state;
    notifyChanged();
  }

  /** The container's marked tests build file paths (possibly none). */
  @NotNull
  public List<String> getTestsFilePaths(@NotNull String containerId) {
    return state.testsFiles.stream()
      .filter(entry -> containerId.equals(entry.containerId))
      .map(entry -> StringUtil.nullize(entry.filePath))
      .filter(Objects::nonNull)
      .toList();
  }

  /** Every marked tests build file across all containers - the gutter markers resolve a source file's owner from these. */
  @NotNull
  public List<String> getAllTestsFilePaths() {
    return state.testsFiles.stream()
      .map(entry -> StringUtil.nullize(entry.filePath))
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }

  /** Marks a tests build file in the container (clearing any exclusion); already-marked files stay marked once. */
  public void markTestsFile(@NotNull String containerId, @NotNull String filePath) {
    boolean marked = state.testsFiles.stream().anyMatch(entryFor(containerId, filePath));
    if (!marked) {
      state.testsFiles.add(newEntry(containerId, filePath));
    }
    state.excludedFiles.removeIf(entryFor(containerId, filePath));
    notifyChanged();
  }

  /**
   * Takes the file out of the container's tests set: the mark is removed AND
   * an exclusion is recorded, so a conventionally-named file (test.hxml)
   * stays out instead of reappearing as a suggestion.
   */
  public void unmarkTestsFile(@NotNull String containerId, @NotNull String filePath) {
    state.testsFiles.removeIf(entryFor(containerId, filePath));
    boolean excluded = state.excludedFiles.stream().anyMatch(entryFor(containerId, filePath));
    if (!excluded) {
      state.excludedFiles.add(newEntry(containerId, filePath));
    }
    notifyChanged();
  }

  /** Every excluded path across all containers - the gutter markers skip these. */
  @NotNull
  public List<String> getAllExcludedFilePaths() {
    return state.excludedFiles.stream()
      .map(entry -> StringUtil.nullize(entry.filePath))
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }

  /**
   * The container's tests build files among its known build files, each file
   * an independent toggle: explicitly MARKED files plus the CONVENTIONAL
   * candidates (test.hxml/tests.hxml names, files under a tests/ directory)
   * minus explicit EXCLUSIONS - what unmarking a conventional file records.
   * Candidate order is preserved.
   */
  @NotNull
  public List<String> resolveTestsFiles(@NotNull String containerId, @NotNull List<String> candidatePaths) {
    List<String> marked = getTestsFilePaths(containerId);
    List<String> excluded = state.excludedFiles.stream()
      .filter(entry -> containerId.equals(entry.containerId))
      .map(entry -> entry.filePath)
      .toList();
    return candidatePaths.stream()
      .filter(candidate -> marked.contains(candidate)
                           || (isConventionalTestsPath(candidate) && !excluded.contains(candidate)))
      .toList();
  }

  /** Whether the path is a tests build by CONVENTION - a test.hxml/tests.hxml name, or any build file under a tests/ directory. */
  public static boolean isConventionalTestsPath(@NotNull String path) {
    return isConventionalTestsFileName(path) || isUnderTestsDirectory(path);
  }

  private static boolean isConventionalTestsFileName(@NotNull String path) {
    String name = PathUtil.getFileName(path).toLowerCase(Locale.ROOT);
    return name.equals("test.hxml") || name.equals("tests.hxml");
  }

  private static boolean isUnderTestsDirectory(@NotNull String path) {
    String parents = PathUtil.getParentPath(path).toLowerCase(Locale.ROOT);
    for (String segment : StringUtil.tokenize(parents.replace('\\', '/'), "/")) {
      if (segment.equals("tests")) return true;
    }
    return false;
  }

  /** Matches the entry identified by the container/path pair. */
  @NotNull
  private static Predicate<ContainerTestsFile> entryFor(@NotNull String containerId, @NotNull String filePath) {
    return entry -> containerId.equals(entry.containerId) && filePath.equals(entry.filePath);
  }

  @NotNull
  private static ContainerTestsFile newEntry(@NotNull String containerId, @NotNull String filePath) {
    ContainerTestsFile entry = new ContainerTestsFile();
    entry.containerId = containerId;
    entry.filePath = filePath;
    return entry;
  }

  private void notifyChanged() {
    modificationTracker.incModificationCount();
    HaxeBuildSettingsListener.publish(project);
  }
}
