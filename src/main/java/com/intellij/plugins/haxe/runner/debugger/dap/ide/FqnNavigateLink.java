package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.frame.XValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Turns a debugger value naming a class or member into a "Navigate" link on its tree node. */
final class FqnNavigateLink {

  static void attach(@NotNull XValueNode node, @NotNull Project project, @NotNull String value) {
    String possibleQname = stripFunctionPrefix(value);
    if (HaxeReferenceUtil.textCanBeQname(possibleQname)) {
      new Task.Backgroundable(project, HaxeDebuggerBundle.message("dap.debugger.value.navigate.resolving"), true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          PsiElement element = resolve(project, possibleQname);
          if (element instanceof HaxeNamedComponent component) {
            // getName() reads the stub tree - back under the read lock, the
            // resolve() above releases it before returning
            String componentName = HaxeReadActions.compute(component::getName);
            String message = HaxeDebuggerBundle.message("dap.debugger.value.navigate.link");
            String tooltip = HaxeDebuggerBundle.message("dap.debugger.value.navigate.tooltip", componentName);
            node.setFullValueEvaluator(new NavigatableValue(message, tooltip, possibleQname, project).setShowValuePopup(false));
          }
        }
      }.queue();
    }
  }

  private static @Nullable PsiElement resolve(@NotNull Project project, @NotNull String value) {
    try {
      return ReadAction.computeCancellable(() -> HaxeQnameResolveUtil.findClassOrMember(value, project));
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      return null;
    }
  }

  private static String stripFunctionPrefix(String componentName) {
    if (componentName == null) return "<unknown>";
    return componentName.startsWith("function ") ? componentName.substring("function ".length()) : componentName;
  }

  private static class NavigatableValue extends XFullValueEvaluator {
    private final @NotNull Project myProject;
    private final @NotNull String myValue;

    public NavigatableValue(String message, String tooltip, @NotNull String value, @NotNull Project project) {
      super(message, new LinkAttributes(tooltip, null, null));
      myProject = project;
      myValue = value;
    }

    @Override
    public void startEvaluation(@NotNull XFullValueEvaluationCallback callback) {
      ReadAction.nonBlocking(this::getNavigatable)
        .finishOnUiThread(ModalityState.any(), this::performNavigation)
        .submit(AppExecutorUtil.getAppExecutorService());

      callback.evaluated("");
    }

    private void performNavigation(Navigatable navigatable) {
      if (navigatable != null && navigatable.canNavigate()) {
        navigatable.navigate(true);
      }
    }

    private @Nullable Navigatable getNavigatable() {
      if (resolve(myProject, myValue) instanceof Navigatable navigatable) {
        return navigatable;
      }
      return null;
    }
  }
}
