package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
@CustomLog
public class HaxelibProjectStartActivity implements ProjectActivity {
  @Nullable
  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    log.debug("Project opened event  for " + project);

    if (!ApplicationManager.getApplication().isUnitTestMode() && hasLegacyHaxeModules(project)) {
      HaxelibProjectUpdater.getInstance().openProject(project);
    }
    return null;
  }

  /**
   * The legacy updater (library sync, auto-import, define detection) only serves
   * HAXE_MODULE-type modules. Plain modules are handled by the v2 build system —
   * running the legacy engine there would fight its library sync and reload
   * tracking.
   */
  static boolean hasLegacyHaxeModules(@NotNull Project project) {
    return !ReadAction.computeBlocking(() -> ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance())).isEmpty();
  }
}
