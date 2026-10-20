package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.v2.runconfig.HaxeMainClassLaunches.MainLaunch;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Run and Debug gutter marks on the class a build starts from: on its
 * {@code static function main()}, else on the class name (a Lime main class
 * has no main; Lime's generated {@code ApplicationMain} constructs it). A Run
 * entry per build file and launch kind ({@link HaxeMainClassLaunches}), plus
 * a Debug entry where the target has a debug lane, each starting that build's
 * Build &amp; run configuration, whose
 * before-launch step compiles the build.
 */
public final class HaxeMainClassRunLineMarkerContributor extends RunLineMarkerContributor {

  private static final String MAIN_METHOD_NAME = "main";

  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (element.getFirstChild() != null) return null; // leaves only, per the contributor contract
    if (!(element.getParent() instanceof HaxeIdentifier identifier)) return null;
    if (!(identifier.getParent() instanceof HaxeComponentName componentName)) return null;

    HaxeClass mainClass = markedClass(componentName.getParent());
    if (mainClass == null) return null;
    List<MainLaunch> launches = HaxeMainClassLaunches.launchesOf(mainClass);
    return launches.isEmpty() ? null : info(mainClass, launches);
  }

  /** The class a declaration's mark stands for: its static main's class, or a class without one; null for anything else. */
  @Nullable
  private static HaxeClass markedClass(@Nullable PsiElement declaration) {
    return switch (declaration) {
      case HaxeClass haxeClass when staticMainOf(haxeClass) == null -> haxeClass;
      case HaxeMethod method when isStaticMain(method) -> PsiTreeUtil.getParentOfType(method, HaxeClass.class);
      case null, default -> null;
    };
  }

  @Nullable
  private static HaxeMethod staticMainOf(@NotNull HaxeClass haxeClass) {
    for (HaxeMethod method : haxeClass.getHaxeMethodsSelf(null)) {
      if (isStaticMain(method)) return method;
    }
    return null;
  }

  private static boolean isStaticMain(@NotNull HaxeMethod method) {
    return MAIN_METHOD_NAME.equals(method.getName()) && method.isStatic();
  }

  @NotNull
  private static Info info(@NotNull HaxeClass mainClass, @NotNull List<MainLaunch> launches) {
    List<AnAction> actions = new ArrayList<>();
    for (MainLaunch launch : launches) {
      actions.add(new LaunchAction(launch, false));
      if (HaxeDebugSupport.supportsProgramDebug(launch.kind().target())) {
        actions.add(new LaunchAction(launch, true));
      }
    }
    // TODO: no Profile entries yet; the tool window's Build & run row offers them through the profiler executors
    AnAction[] actionArray = actions.toArray(AnAction.EMPTY_ARRAY);
    String tooltip = HaxeBundle.message("haxe.main.gutter.tooltip", mainClass.getName());
    return new Info(AllIcons.RunConfigurations.TestState.Run, actionArray, ignored -> tooltip);
  }

  /** One gutter popup entry: runs (or debugs) a build's output through its Build &amp; run configuration. */
  private static final class LaunchAction extends AnAction {
    private final MainLaunch launch;
    private final boolean debug;

    LaunchAction(@NotNull MainLaunch launch, boolean debug) {
      super(label(launch, debug), null, debug ? AllIcons.Actions.StartDebugger : AllIcons.Actions.Execute);
      this.launch = launch;
      this.debug = debug;
    }

    @NotNull
    private static String label(@NotNull MainLaunch launch, boolean debug) {
      String configurationName = launch.kind().configurationName(launch.buildFile().file().getName());
      return HaxeBundle.message(debug ? "haxe.main.gutter.debug" : "haxe.main.gutter.run", configurationName);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;
      Executor executor = debug
                          ? DefaultDebugExecutor.getDebugExecutorInstance()
                          : DefaultRunExecutor.getRunExecutorInstance();
      HaxeProgramLaunches.launchBuildAndRun(project, launch.buildFile(), launch.kind(), launch.targetOutput(), executor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }
  }
}
