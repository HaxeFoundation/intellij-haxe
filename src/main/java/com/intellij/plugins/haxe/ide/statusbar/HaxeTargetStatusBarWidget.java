package com.intellij.plugins.haxe.ide.statusbar;

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.ide.projectStructure.autoimport.HaxelibAutoImport;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Status bar widget that shows the Haxe compilation target of the module owning the file in the
 * selected editor (falling back to the first Haxe module in the project, e.g. when inspecting
 * library sources) and lets the user switch target in place.
 *
 * The list of selectable targets mirrors the "Target" combo box in the module settings
 * ({@link com.intellij.plugins.haxe.ide.projectStructure.ui.HaxeConfigurationEditor}): it depends
 * on the module's build configuration (NMML, OpenFL, or plain Haxe/HXML/custom properties).
 */
public class HaxeTargetStatusBarWidget implements StatusBarWidget, StatusBarWidget.Multiframe,
                                                  StatusBarWidget.MultipleTextValuesPresentation {

  public static final String WIDGET_ID = "HaxeTargetWidget";

  private final Project myProject;
  private @Nullable StatusBar myStatusBar;

  public HaxeTargetStatusBarWidget(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public @NotNull String ID() {
    return WIDGET_ID;
  }

  @Override
  public @Nullable WidgetPresentation getPresentation() {
    return this;
  }

  @Override
  public StatusBarWidget copy() {
    return new HaxeTargetStatusBarWidget(myProject);
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    myStatusBar = statusBar;
    MessageBusConnection connection = myProject.getMessageBus().connect(this);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        update();
      }
    });
    connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        update();
      }
    });
  }

  @Override
  public void dispose() {
    myStatusBar = null;
  }

  private void update() {
    StatusBar statusBar = myStatusBar;
    if (statusBar != null) {
      statusBar.updateWidget(WIDGET_ID);
    }
  }

  @Override
  public @Nullable String getSelectedValue() {
    Module module = findHaxeModule();
    if (module == null) return null;
    Object target = getCurrentTarget(HaxeModuleSettings.getInstance(module));
    return HaxeBundle.message("haxe.target.widget.text", target == null ? "?" : target.toString());
  }

  @Override
  public @Nullable String getTooltipText() {
    Module module = findHaxeModule();
    if (module == null) return null;
    return HaxeBundle.message("haxe.target.widget.tooltip", module.getName());
  }

  @Override
  public @Nullable JBPopup getPopup() {
    final Module module = findHaxeModule();
    if (module == null) return null;
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    final List<Object> targets = getAvailableTargets(settings);

    BaseListPopupStep<Object> step =
      new BaseListPopupStep<>(HaxeBundle.message("haxe.target.widget.popup.title", module.getName()), targets) {
        @Override
        public PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
          return doFinalStep(() -> applyTarget(module, settings, selectedValue));
        }
      };
    step.setDefaultOptionIndex(targets.indexOf(getCurrentTarget(settings)));
    return JBPopupFactory.getInstance().createListPopup(step);
  }

  private void applyTarget(@NotNull Module module, @NotNull HaxeModuleSettings settings, @Nullable Object target) {
    if (target instanceof NMETarget nmeTarget) {
      settings.setNmeTarget(nmeTarget);
    }
    else if (target instanceof OpenFLTarget openFLTarget) {
      settings.setOpenFLTarget(openFLTarget);
    }
    else if (target instanceof HaxeTarget haxeTarget) {
      settings.setHaxeTarget(haxeTarget);
    }
    else {
      return;
    }
    // Same refresh as HaxeConfigurationEditor.apply() so haxelib sync and define detection pick up the new target.
    ExternalSystemProjectTracker tracker = ExternalSystemProjectTracker.getInstance(module.getProject());
    tracker.scheduleProjectRefresh();
    tracker.markDirty(HaxelibAutoImport.mySystemProjectId);
    update();
  }

  /**
   * Same target sets as HaxeConfigurationEditor.updateTargetCombo().
   */
  private static @NotNull List<Object> getAvailableTargets(@NotNull HaxeModuleSettings settings) {
    return switch (HaxeConfiguration.translateBuildConfig(settings.getBuildConfig())) {
      case NMML -> Arrays.asList((Object[])NMETarget.values());
      case OPENFL -> Arrays.asList((Object[])OpenFLTarget.values());
      default -> Arrays.asList((Object[])HaxeTarget.values());
    };
  }

  private static @Nullable Object getCurrentTarget(@NotNull HaxeModuleSettings settings) {
    return switch (HaxeConfiguration.translateBuildConfig(settings.getBuildConfig())) {
      case NMML -> settings.getNmeTarget();
      case OPENFL -> settings.getOpenFLTarget();
      default -> settings.getHaxeTarget();
    };
  }

  /**
   * The Haxe module owning the file in the selected editor, or the first Haxe module in the
   * project when the file does not belong to one (e.g. haxelib or SDK sources).
   */
  private @Nullable Module findHaxeModule() {
    VirtualFile file = getSelectedFile();
    if (file != null) {
      Module module = ModuleUtilCore.findModuleForFile(file, myProject);
      if (module != null && ModuleType.get(module) == HaxeModuleType.getInstance()) {
        return module;
      }
    }
    Collection<Module> haxeModules = ModuleUtil.getModulesOfType(myProject, HaxeModuleType.getInstance());
    return haxeModules.isEmpty() ? null : haxeModules.iterator().next();
  }

  private @Nullable VirtualFile getSelectedFile() {
    if (myProject.isDisposed()) return null;
    VirtualFile[] files = FileEditorManager.getInstance(myProject).getSelectedFiles();
    return files.length > 0 ? files[0] : null;
  }

  @Override
  public @Nullable Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
  }
}
