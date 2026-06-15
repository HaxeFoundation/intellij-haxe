package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
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

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      HaxelibProjectUpdater.getInstance().openProject(project);
    }
    return null;
  }
}
