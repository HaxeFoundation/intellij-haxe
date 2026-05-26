package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.lang.psi.stubs.type.HaxeFileElementType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Triggered once per project open. Two responsibilities, in this order:
 *
 * <ol>
 *   <li>Install the {@link HaxeFileElementType#READINESS_GATE} on first invocation,
 *       so subsequent stub builds wait for define detection to complete before
 *       running the conditional-compilation lexer.</li>
 *   <li>Run synchronous define detection for this project, then hand off to the
 *       existing async path in {@link HaxelibProjectUpdater} for the rest of the
 *       classpath/library work.</li>
 * </ol>
 */
@CustomLog
public class HaxelibProjectStartActivity implements ProjectActivity {

  /** Default timeout for stub builders waiting on detection readiness. */
  private static final long STUB_BUILDER_GATE_TIMEOUT_MS = 5_000L;

  @Nullable
  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    log.debug("Project opened event for " + project);

    installHaxeFileReadinessGate();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      HaxeDefineDetectionManager.getInstance(project).recalculateDefinitionsSync(project);
      HaxelibProjectUpdater.getInstance().openProject(project);
    }
    return null;
  }

  /**
   * Installs the production readiness gate on {@link HaxeFileElementType}.
   * Safe to call repeatedly — concurrent callers race harmlessly to install
   * equivalent gate lambdas.
   *
   * <p>Package-visible for tests.
   */
  static void installHaxeFileReadinessGate() {
    HaxeFileElementType.READINESS_GATE = () -> {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        return;
      }
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
      for (Project openProject : openProjects) {
        if (openProject.isDisposed()) continue;
        HaxeDefineDetectionManager manager = HaxeDefineDetectionManager.getInstance(openProject);
        if (!manager.awaitReady(STUB_BUILDER_GATE_TIMEOUT_MS)) {
          log.warn("HaxeDefineDetectionManager not ready within "
                   + STUB_BUILDER_GATE_TIMEOUT_MS + "ms for "
                   + openProject.getName()
                   + "; proceeding with whatever defines are currently populated.");
        }
      }
    };
  }
}
