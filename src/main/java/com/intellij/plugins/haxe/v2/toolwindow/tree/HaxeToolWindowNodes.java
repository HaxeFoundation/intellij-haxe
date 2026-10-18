package com.intellij.plugins.haxe.v2.toolwindow.tree;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User objects for the Haxe tool window tree. Kept as plain data records so tree
 * nodes never retain disposable platform objects (modules, PSI).
 */
public final class HaxeToolWindowNodes {

  private HaxeToolWindowNodes() {
  }

  /**
   * Behaviour every tree user object provides itself, so the panel needs no
   * per-type switches that grow with each node kind.
   */
  public sealed interface HaxeToolWindowNode {
    /** Stable identity used to preserve expansion state across tree rebuilds. */
    @NotNull
    String expansionKey();

    /** The row's primary label as the renderer shows it; speed search matches against it too. */
    @NotNull
    String label();
  }

  public record ModuleNode(@NotNull String name) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "module:" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }

  /** Project-root container row, listing build files that belong to no module. */
  public record ProjectNode(@NotNull String name) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "project";
    }

    @Override
    public String label() {
      return name;
    }
  }

  /**
   * A build file row; at most one in the project is active (its defines drive the
   * parse context). Manual rows were added by hand and can be removed;
   * auto-detected rows can only be hidden. {@code testsFile} marks one of the
   * container's tests build files; {@code marked} = the store holds an explicit
   * mark for it (a stale mark outlives its framework lib, so it is no
   * testsFile); {@code frameworkDetected} = its libraries include a known test
   * framework; {@code toolFailure} = the build tool's last failed evaluation
   * of the file, or null.
   */
  public record BuildFileRow(@NotNull HaxeBuildFile buildFile, @NotNull String containerId, boolean active,
                             boolean manual, boolean testsFile, boolean marked, boolean frameworkDetected,
                             @Nullable String toolFailure)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "file:" + buildFile.file().getPath();
    }

    @Override
    public String label() {
      return buildFile.file().getName();
    }
  }

  /** "Build" grouping row containing the container's build files. */
  public record BuildGroupNode(@NotNull String containerId, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "build";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.build");
    }
  }

  /**
   * "Actions" grouping row under a build file; {@code ownerId} is the build
   * file's path, {@code launchOutput} what a launching action would run (null
   * when nothing is launchable).
   */
  public record ActionsGroupNode(@NotNull String ownerId, int count, @Nullable String launchOutput) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "actions";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.actions");
    }
  }

  /** "Tests" grouping row under a tests build file, holding the test-run entries. */
  public record TestsGroupNode() implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "tests";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.tests.group");
    }
  }

  /**
   * A runnable action row under a build file. The command is resolved at tree-build
   * time against that file's type, selected target and its container's environment
   * SDK. {@code presentableCommand} is the short display form (and, for custom
   * actions, the editable text).
   * A custom action may carry a {@code launch}: a run configuration started after its
   * command compiled the file - one created to run the build's output
   * ({@code launchOutput}, as the tree currently sees it), or an existing one. A null
   * {@code launchOutput} means the file produces nothing launchable right now.
   * {@code runBefore} labels the actions that run first.
   */
  public record ActionNode(@NotNull String ownerId,
                           @NotNull String name,
                           @NotNull List<String> command,
                           @Nullable String workDirectory,
                           @NotNull String presentableCommand,
                           boolean custom,
                           @Nullable Launch launch,
                           @Nullable String launchOutput,
                           @NotNull List<String> runBefore) implements HaxeToolWindowNode {
    /** The kind a created-configuration launch runs through; null for a plain action, an existing-configuration launch or an unknown stored kind. */
    @Nullable
    public HaxeLaunchKind launchKind() {
      return launch instanceof Launch.Create create ? HaxeLaunchKind.byId(create.kindId()) : null;
    }

    /** The stored action behind the row; when the store has none, the row's own display form. */
    @NotNull
    public CustomAction storedAction(@NotNull Project project) {
      CustomAction stored = HaxeCustomActionsStore.getInstance(project).findAction(ownerId, name);
      return stored != null ? stored : new CustomAction(name, presentableCommand, "");
    }

    /** What the row's gray tail names: the kind, or the existing configuration; null for a plain action or an unknown kind. */
    @Nullable
    public String launchTarget() {
      return switch (launch) {
        case Launch.Create ignored -> launchKind() == null ? null : launchKind().displayName();
        case Launch.Existing existing -> existing.configurationName();
        case null -> null;
      };
    }

    @Override
    public String expansionKey() {
      return "action:" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }

  /** Group of runnable developer tools under a container: detected tool configs plus user-added tools. */
  public record ToolsGroupNode(@NotNull String containerId, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "tools";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.tools");
    }
  }

  /**
   * A runnable tool row under a container: detected from a tool's config file
   * (checkstyle.json, hxformat.json) or added by the user. Unlike {@link ActionNode}
   * a tool never participates in compile-command selection. {@code detail} is the
   * gray tail text: the config file name for detected tools, the editable command
   * for custom ones.
   */
  public record ToolNode(@NotNull String containerId,
                         @NotNull String name,
                         @NotNull List<String> command,
                         @Nullable String workDirectory,
                         @NotNull String detail,
                         boolean custom) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "tool:" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }

  /**
   * "Build &amp; run" row under a build file's Actions: launches the target's
   * output through its run configuration (the build attached as a before-launch
   * step). Present only when the target output is launchable; {@code kind} is the
   * configuration kind shown in gray ("HashLink Application", …). Target and
   * output are captured at tree-build time - for lime/nme files they come from
   * the selected target's background evaluation.
   */
  public record ProgramNode(@NotNull HaxeBuildFile buildFile,
                            @NotNull HaxeLaunchKind kind,
                            @NotNull String targetOutput) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "program";
    }

    @Override
    public String label() {
      // "Build & run", or "Run" for a target that runs without a build step (the interpreter)
      return HaxeBundle.message(kind.compilesBeforeLaunch() ? "haxe.toolwindow.node.program" : "haxe.toolwindow.node.program.run");
    }
  }

  /**
   * "Run Unit Tests" row under a tests build file: launches
   * the tests through the unit-test run configuration (SM test console), with the
   * compile attached as a before-launch step. Deliberately NOT named after lime's
   * unrelated default {@code test} action (build-and-launch).
   */
  public record TestRunNode(@NotNull String buildFilePath) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "unittests";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.run.unit.tests");
    }
  }

  /** Target row under a build file; selectable for XML/HXP projects, static for HXML. */
  public record TargetNode(@NotNull HaxeBuildFile buildFile, @NotNull String displayName, boolean selectable)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "target:" + buildFile.file().getPath();
    }

    @Override
    public String label() {
      return displayName;
    }
  }

  /**
   * Section row under a multi-section hxml (a {@code --next} chain): which
   * compilation the tree's defines/libraries/target and test runs follow.
   * Carries every section's identity, label and descriptor (its target/output
   * summary, possibly empty) so the chooser popup needs no re-parse;
   * selections are stored by identity, index-aligned with the other lists.
   */
  public record SectionNode(@NotNull HaxeBuildFile buildFile,
                            @NotNull List<String> ids,
                            @NotNull List<String> labels,
                            @NotNull List<String> descriptors,
                            int selected) implements HaxeToolWindowNode {
    public String displayName() {
      String descriptor = displayDescriptor(selected);
      return descriptor.isEmpty() ? labels.get(selected) : labels.get(selected) + "  " + descriptor;
    }

    /** The section's descriptor parenthesized for display ("(JavaScript)"), or "" without one. */
    public String displayDescriptor(int index) {
      String descriptor = descriptors.get(index);
      return descriptor.isEmpty() ? "" : "(" + descriptor + ")";
    }

    @Override
    public String expansionKey() {
      return "section:" + buildFile.file().getPath();
    }

    @Override
    public String label() {
      return displayName();
    }
  }

  public enum GroupKind { DEFINES, LIBRARIES }

  /** "Defines" / "Libraries" grouping row with its child count. */
  public record GroupNode(@NotNull GroupKind kind, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "group:" + kind;
    }

    @Override
    public String label() {
      return kind == GroupKind.LIBRARIES
             ? HaxeBundle.message("haxe.toolwindow.node.libraries")
             : HaxeBundle.message("haxe.toolwindow.node.defines");
    }
  }

  public record DefineNode(@NotNull HaxeBuildFile owner, @NotNull String name, @Nullable String value)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "define:" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }

  /**
   * "IDE Overrides" row under a build file: what code analysis adds to the
   * build, never passed to builds or runs. {@code count} counts the define
   * entries plus a set custom target; {@code buildDefineNames} are the build's
   * own defines, which an entry of the same name overrides;
   * {@code customTargetApplicable} is false for build types that always
   * select a target.
   */
  public record IdeOverridesNode(@NotNull HaxeBuildFile buildFile,
                                 int count,
                                 @NotNull Set<String> buildDefineNames,
                                 boolean customTargetApplicable) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "ideoverrides";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.ide.overrides");
    }
  }

  /** The "Defines" row under IDE Overrides. */
  public record IdeDefinesNode(@NotNull HaxeBuildFile buildFile, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "idedefines";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.defines");
    }
  }

  /** An IDE define entry; {@code inBuildFile} = the build file declares the same name. */
  public record IdeDefineNode(@NotNull HaxeBuildFile buildFile,
                              @NotNull String name,
                              @NotNull String value,
                              @NotNull DefineEffect effect,
                              boolean inBuildFile) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "idedef:" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }

  /**
   * The custom target row under IDE Overrides ({@code --custom-target} name,
   * null when unset); clicking opens an input dialog. {@code buildTarget}
   * names the target the build selects itself, which then wins; null when it
   * selects none.
   */
  public record IdeCustomTargetNode(@NotNull HaxeBuildFile buildFile, @Nullable String customTarget, @Nullable String buildTarget)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "idecustomtarget";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.ide.custom.target");
    }
  }

  /** "Environment" row under a container: its SDK and language level. */
  public record EnvironmentNode(@NotNull String containerId) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "env";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.environment");
    }
  }

  /** Environment SDK row; clicking opens the SDK chooser popup. */
  public record EnvSdkNode(@NotNull String containerId, @NotNull String displayName, boolean missing)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envsdk";
    }

    @Override
    public String label() {
      return displayName;
    }
  }

  /** Environment language-level row; clicking opens the level chooser popup. Backed by the Haxe Compiler settings page. */
  public record EnvLanguageLevelNode(@NotNull String containerId, @NotNull String displayName)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envlevel";
    }

    @Override
    public String label() {
      return displayName;
    }
  }

  /** "Compilation" grouping row: the compile command and compilation server rows. */
  public record CompilationGroupNode(@NotNull String containerId) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "compilation";
    }

    @Override
    public String label() {
      return HaxeBundle.message("haxe.toolwindow.node.compilation");
    }
  }

  /**
   * Compilation server row: shows the project server's state for this container.
   * Clicking toggles the container's participation, or opens Build Tools settings
   * while the server is disabled project-wide. {@code contextFailure} carries the
   * container's last failed compiler request (a build context that does not
   * compile), shown as a warning on the row.
   */
  public record CompilationServerNode(@NotNull String containerId,
                                      @NotNull String display,
                                      boolean projectEnabled,
                                      boolean moduleUses,
                                      boolean running,
                                      boolean connectEligible,
                                      @Nullable String contextFailure) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "server";
    }

    @Override
    public String label() {
      return display;
    }
  }

  /** Renderer fragment tag on the server row's failure text: clicking it opens the server console's status view. */
  public record ServerFailureLink(@NotNull String containerId) {
  }

  /**
   * "Build command" row: what building this container runs.
   * {@code command} is null while there is none (or the file is gone). Candidates
   * feed the configure dialog: the container's build file paths, per file the
   * action names available as command overrides, and the file the container
   * builds without a command set.
   */
  public record EnvCompileCommandNode(@NotNull String containerId,
                                      @NotNull String display,
                                      @Nullable List<String> command,
                                      @Nullable String workDirectory,
                                      @NotNull List<String> candidateFilePaths,
                                      @NotNull Map<String, List<String>> actionNamesByFile,
                                      @Nullable String defaultFilePath,
                                      boolean connectEligible) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envcompile";
    }

    @Override
    public String label() {
      return display;
    }
  }

  /**
   * A haxelib dependency; missing = the name - or the PINNED version - is not
   * installed according to haxelib. {@code version} is what the build file pins;
   * {@code resolvedVersion} is the version haxelib has selected (possibly "dev"
   * or "git") and is used when the file pins nothing.
   */
  public record LibraryNode(@NotNull HaxeBuildFile owner,
                            @NotNull String name,
                            @Nullable String version,
                            @Nullable String resolvedVersion,
                            boolean installed,
                            @Nullable String requiredBy) implements HaxeToolWindowNode {

    /** Whether the row is a transitive dependency ({@code requiredBy} names the library pulling it in) rather than one the build file declares. */
    public boolean transitive() {
      return requiredBy != null;
    }

    /** The version to show: the pinned one when declared, otherwise haxelib's selected version. A git pin shows as {@code git#shortref}, its url stays in the tooltip. */
    @Nullable
    public String displayVersion() {
      HaxelibGitSpec gitSpec = HaxelibGitSpec.parse(version);
      if (gitSpec != null) {
        return gitSpec.ref() == null ? HaxelibSemVer.GIT_SCM
                                     : HaxelibSemVer.GIT_SCM + "#" + HaxelibGitSpec.shortRef(gitSpec.ref());
      }
      return version != null ? version : resolvedVersion;
    }

    @Override
    public String expansionKey() {
      return requiredBy == null ? "lib:" + name : "lib:" + requiredBy + "/" + name;
    }

    @Override
    public String label() {
      return name;
    }
  }
}
