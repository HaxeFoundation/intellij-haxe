package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.frame.XValueNode;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;


final class FqnNavigateLink {

  static void attach(@NotNull XValueNode node, @NotNull Project project, @NotNull String value) {
    String possibleQname = stripFunctionPrefix(value);
    if (HaxeReferenceUtil.textCanBeQname(possibleQname)) {
      new Task.Backgroundable(project, "Resolving",  true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(possibleQname);
          PsiElement element = resolve(project, possibleQname);
          if (element instanceof HaxeNamedComponent component) {
            String message = HaxeDebuggerBundle.message("dap.debugger.value.navigate.link");
            String tooltip = HaxeDebuggerBundle.message("dap.debugger.value.navigate.tooltip", component.getName());
            node.setFullValueEvaluator(new NavigatableValue(message, tooltip, possibleQname, project).setShowValuePopup(false));
          }
        }
      }.queue();
    }
  }

  private static @Nullable PsiElement resolve(@NonNull Project project, @NonNull String value) {
    try {
      return ReadAction.computeCancellable(() ->{
        PsiElement element = HaxeResolveUtil.findClassOrMemberByQName(value, project);
        if(element != null) return element;
        return resolveRuntimeName(project, value);
      });
    }catch (ProcessCanceledException e) {
      throw e;
    }catch (Exception e) {
      return null;
    }
  }

  // modules are apparently a compile time concept, so FQN for classes will never contain the module name
  private static @Nullable PsiElement resolveRuntimeName(@NonNull Project project, @NonNull String value) {
    FullyQualifiedInfo info = getRuntimeQualifiedInfo(value);
    if (info == null) return null;

    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    List<HaxeClass> haxeClasses = HaxeClassNameUnifiedIndex.getByNameFiltered(info.className, project, scope)
      .stream()
      .filter(aClass -> {
        String qualifiedName = aClass.getFullyQualifiedName();
        FullyQualifiedInfo withNoModuleName = new FullyQualifiedInfo(qualifiedName).withModuleName(null);
        return withNoModuleName.equals(info.toClassQualifiedName());
      }).toList();

    if(!haxeClasses.isEmpty()) {
      HaxeClass haxeClass = haxeClasses.getFirst();
      if(!info.hasMemberName()) {
        return haxeClass;
      } else {
        String name = info.getMemberName();
        HaxeBaseMemberModel member = haxeClass.getModel().getMember(name, null);
        if(member != null) {
          return member.getBasePsi();
        }
      }
    }

    return null;
  }

  private static @Nullable FullyQualifiedInfo getRuntimeQualifiedInfo(@NonNull String value) {
    // Runtime does not contain module info, so it is dropped and only the class is used
    FullyQualifiedInfo info = new FullyQualifiedInfo(value);
    if(!info.hasModuleName()) return null;
    if(!info.hasClassName()) {
      info = info.withModuleName(null).withClassName(info.moduleName);
    }
    return info;
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
