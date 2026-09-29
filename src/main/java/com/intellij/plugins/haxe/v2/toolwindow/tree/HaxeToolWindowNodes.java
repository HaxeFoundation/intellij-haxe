package com.intellij.plugins.haxe.v2.toolwindow.tree;

import com.intellij.plugins.haxe.v2.buildtools.settings.DefineEffect;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.haxelib.HaxelibGitSpec;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
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

    /** The text the tree's speed search matches against. */
    @NotNull
    String speedSearchText();
  }

  public record ModuleNode(@NotNull String name) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "module:" + name;
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
      return name;
    }
  }

  /**
   * A build file row; at most one in the project is active (its defines drive the
   * parse context). Manual rows were added by hand and can be removed;
   * auto-detected rows can only be hidden. {@code testsFile} marks one of the
   * container's tests build files; {@code frameworkDetected} = its libraries
   * include a known test framework.
   */
  public record BuildFileRow(@NotNull HaxeBuildFile buildFile, @NotNull String containerId, boolean active,
                             boolean manual, boolean testsFile, boolean frameworkDetected)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "file:" + buildFile.file().getPath();
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
      return HaxeBundle.message("haxe.toolwindow.node.build");
    }
  }

  /** "Actions" grouping row under a build file; ownerId is the build file's path. */
  public record ActionsGroupNode(@NotNull String ownerId, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "actions";
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
      return HaxeBundle.message("haxe.toolwindow.node.tests.group");
    }
  }

  /**
   * A runnable action row under a build file. The command is resolved at tree-build
   * time against that file's type, selected target and its container's environment
   * SDK. {@code presentableCommand} is the short display form (and, for custom
   * actions, the editable text).
   */
  public record ActionNode(@NotNull String ownerId,
                           @NotNull String name,
                           @NotNull List<String> command,
                           @Nullable String workDirectory,
                           @NotNull String presentableCommand,
                           boolean custom) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "action:" + name;
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
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
    public String speedSearchText() {
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
                            @NotNull String kind,
                            @NotNull HaxeTarget target,
                            @NotNull String targetOutput) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "program";
    }

    @Override
    public String speedSearchText() {
      return HaxeBundle.message("haxe.toolwindow.node.program");
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
    public String speedSearchText() {
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
    public String speedSearchText() {
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
    public String speedSearchText() {
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
    public String speedSearchText() {
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
    public String speedSearchText() {
      return name;
    }
  }

  /** "Environment" row under a container: its SDK, language level, define overrides and custom target. */
  public record EnvironmentNode(@NotNull String containerId,
                                @NotNull String displayName,
                                @NotNull Set<String> activeBuildFileDefines) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "env";
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
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
    public String speedSearchText() {
      return displayName;
    }
  }

  /** Environment "Defines" grouping row. */
  public record EnvDefinesNode(@NotNull String containerId, int count) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envdefines";
    }

    @Override
    public String speedSearchText() {
      return HaxeBundle.message("haxe.toolwindow.node.environment.defines");
    }
  }

  /** Environment "Custom target" row ({@code --custom-target} name, null when unset); clicking opens an input dialog. */
  public record EnvCustomTargetNode(@NotNull String containerId, @Nullable String customTarget)
    implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envcustomtarget";
    }

    @Override
    public String speedSearchText() {
      return HaxeBundle.message("haxe.toolwindow.node.environment.custom.target");
    }
  }

  /** "Compilation" grouping row: the compile command and compilation server rows. */
  public record CompilationGroupNode(@NotNull String containerId) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "compilation";
    }

    @Override
    public String speedSearchText() {
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
    public String speedSearchText() {
      return display;
    }
  }

  /** Renderer fragment tag on the server row's failure text: clicking it opens the server console's status view. */
  public record ServerFailureLink(@NotNull String containerId) {
  }

  /**
   * "Compile command" row: what compiling this container runs.
   * {@code command} is null while unconfigured (or the file is gone). Candidates
   * feed the configure dialog: the container's build file paths and, per file,
   * the action names available as command overrides.
   */
  public record EnvCompileCommandNode(@NotNull String containerId,
                                      @NotNull String display,
                                      @Nullable List<String> command,
                                      @Nullable String workDirectory,
                                      @NotNull List<String> candidateFilePaths,
                                      @NotNull Map<String, List<String>> actionNamesByFile,
                                      boolean connectEligible) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envcompile";
    }

    @Override
    public String speedSearchText() {
      return display;
    }
  }

  /** A user define entry; {@code inBuildFile} = the active build file declares the same name. */
  public record EnvDefineNode(@NotNull String containerId,
                              @NotNull String name,
                              @NotNull String value,
                              @NotNull DefineEffect effect,
                              boolean inBuildFile) implements HaxeToolWindowNode {
    @Override
    public String expansionKey() {
      return "envdef:" + name;
    }

    @Override
    public String speedSearchText() {
      return name;
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
                            boolean installed) implements HaxeToolWindowNode {

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
      return "lib:" + name;
    }

    @Override
    public String speedSearchText() {
      return name;
    }
  }
}
