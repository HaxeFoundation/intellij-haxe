package com.intellij.plugins.haxe.v2.testing.run;

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
import com.intellij.plugins.haxe.v2.testing.HaxeTestClasses;
import com.intellij.plugins.haxe.v2.testing.HaxeTestContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Run/Debug gutter markers on test classes and methods. A marker appears only
 * when a marked-or-conventional tests build owns the file (see
 * {@link HaxeTestContext}) and that build's framework recognizes the element -
 * class markers run the suite through the framework's single-suite template,
 * method markers the one test (frameworks without a method mechanism show
 * class markers only).
 */
public final class HaxeTestRunLineMarkerContributor extends RunLineMarkerContributor {

  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (element.getFirstChild() != null) return null; // leaves only, per the contributor contract
    PsiElement identifier = element.getParent();
    if (!(identifier instanceof HaxeIdentifier)) return null;
    PsiElement componentName = identifier.getParent();
    if (!(componentName instanceof HaxeComponentName)) return null;

    PsiElement owner = componentName.getParent();
    if (owner instanceof HaxeClass haxeClass) {
      return classInfo(element, haxeClass);
    }
    if (owner instanceof HaxeMethod method) {
      return methodInfo(element, method);
    }
    return null;
  }

  @Nullable
  private static Info classInfo(@NotNull PsiElement element, @NotNull HaxeClass haxeClass) {
    HaxeTestContext context = HaxeTestContext.forFile(element.getContainingFile());
    if (context == null || !context.framework().isTestClass(haxeClass)) return null;
    String reference = HaxeTestClasses.templateReference(haxeClass);
    if (reference == null) return null;
    return info(context, reference, null, haxeClass.getName(), AllIcons.RunConfigurations.TestState.Run_run);
  }

  @Nullable
  private static Info methodInfo(@NotNull PsiElement element, @NotNull HaxeMethod method) {
    HaxeTestContext context = HaxeTestContext.forFile(element.getContainingFile());
    if (context == null || context.framework().singleRunTemplate(true) == null) return null;
    HaxeClass enclosing = PsiTreeUtil.getParentOfType(method, HaxeClass.class);
    if (enclosing == null) return null;
    if (!context.framework().isTestClass(enclosing) || !context.framework().isTestMethod(method)) return null;
    String reference = HaxeTestClasses.templateReference(enclosing);
    if (reference == null) return null;
    String presentable = enclosing.getName() + "." + method.getName();
    return info(context, reference, method.getName(), presentable, AllIcons.RunConfigurations.TestState.Run);
  }

  @NotNull
  private static Info info(@NotNull HaxeTestContext context,
                           @NotNull String testClass,
                           @Nullable String testMethod,
                           @Nullable String presentable,
                           @NotNull Icon icon) {
    String shown = presentable != null ? presentable : testClass;
    AnAction[] actions = {
      new SingleRunAction(false, context.testsBuildPath(), testClass, testMethod, shown),
      new SingleRunAction(true, context.testsBuildPath(), testClass, testMethod, shown)
    };
    return new Info(icon, actions, ignored -> HaxeBundle.message("haxe.test.gutter.tooltip", shown));
  }

  /** One gutter popup entry: runs (or debugs) the selected suite/test through the tests build's configuration. */
  private static final class SingleRunAction extends AnAction {
    private final boolean debug;
    private final String buildFilePath;
    private final String testClass;
    private final @Nullable String testMethod;

    SingleRunAction(boolean debug,
                    @NotNull String buildFilePath,
                    @NotNull String testClass,
                    @Nullable String testMethod,
                    @NotNull String presentable) {
      super(HaxeBundle.message(debug ? "haxe.test.gutter.debug" : "haxe.test.gutter.run", presentable),
            null,
            debug ? AllIcons.Actions.StartDebugger : AllIcons.Actions.Execute);
      this.debug = debug;
      this.buildFilePath = buildFilePath;
      this.testClass = testClass;
      this.testMethod = testMethod;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;
      if (debug) {
        HaxeTestRunConfigurations.debugSingle(project, buildFilePath, testClass, testMethod);
      }
      else {
        HaxeTestRunConfigurations.runSingle(project, buildFilePath, testClass, testMethod);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      boolean available = project != null
        && (!debug || HaxeTestRunConfigurations.isDebugSupported(project, buildFilePath));
      e.getPresentation().setEnabled(available);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }
  }
}
