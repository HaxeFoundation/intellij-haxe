package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Rewrites Haxe SDK entries stored by an older plugin version in the current
 * format. Until an entry is committed again, the platform logs a "HaxeSdkData
 * mismatch" error on every save.
 */
public class HaxeSdkFormatUpgrade implements ProjectActivity {

  @Nullable
  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    if (!HaxeReadActions.compute(HaxeSdkFormatUpgrade::sdksInOldFormat).isEmpty()) {
      ApplicationManager.getApplication().invokeLater(HaxeSdkFormatUpgrade::rewriteSdksInOldFormat, ModalityState.nonModal());
    }
    return null;
  }

  /** Commits every Haxe SDK still stored in an older format; the commit writes the current one. */
  static void rewriteSdksInOldFormat() {
    WriteAction.run(() -> sdksInOldFormat().forEach(sdk -> sdk.getSdkModificator().commitChanges()));
  }

  private static List<Sdk> sdksInOldFormat() {
    List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance());
    return ContainerUtil.filter(sdks, HaxeSdkFormatUpgrade::isStoredInOldFormat);
  }

  private static boolean isStoredInOldFormat(Sdk sdk) {
    return sdk.getSdkAdditionalData() instanceof HaxeSdkData data && data.isStoredInOldFormat();
  }
}
