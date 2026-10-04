package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleSdkApplier;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleWorkspace;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore.CompileCommand;
import com.intellij.plugins.haxe.v2.buildtools.settings.ui.HaxeBuildToolsConfigurable;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCompileCommandDialog;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeEnvironmentDialog;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The settings edits tree rows offer: choosers, the custom-target prompt, the
 * compile command dialog, the server toggle and the confirmed removals. The
 * stores publish every change, so the tree refreshes on its own. Module
 * libraries come from the module's current build context (see
 * {@link HaxeLibrarySync}), so changing the target, the section or the build
 * command's file re-syncs them.
 */
public final class HaxeToolWindowEditors {

  private record SectionChoice(int index, @NotNull String label, @NotNull String descriptor) {
  }

  private record SdkChoice(@Nullable String name, @NotNull String display) {
  }

  private record LevelChoice(@Nullable HaxeLanguageLevel level, @NotNull String display) {
  }

  private HaxeToolWindowEditors() {
  }

  /** Shows the target dropdown for a selectable target row, anchored at the given point. */
  public static void showTargetPopup(@NotNull Project project, @NotNull TargetNode targetNode, @NotNull RelativePoint point) {
    VirtualFile buildFile = targetNode.buildFile().file();
    List<HaxeTargetOptions.TargetChoice> choices = HaxeTargetOptions.choicesFor(targetNode.buildFile().type());
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(choices)
      .setTitle(HaxeBundle.message("haxe.toolwindow.select.target.title"))
      .setRenderer(BuilderKt.textListCellRenderer("", HaxeTargetOptions.TargetChoice::displayName))
      .setItemChosenCallback(choice -> selectTarget(project, buildFile, choice.id()))
      .createPopup()
      .show(point);
  }

  /** Shows the {@code --next} section dropdown for a multi-section hxml, anchored at the given point. */
  public static void showSectionPopup(@NotNull Project project, @NotNull SectionNode sectionNode, @NotNull RelativePoint point) {
    List<SectionChoice> choices = new ArrayList<>();
    for (int i = 0; i < sectionNode.labels().size(); i++) {
      choices.add(new SectionChoice(i, sectionNode.labels().get(i), sectionNode.displayDescriptor(i)));
    }
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(choices)
      .setTitle(HaxeBundle.message("haxe.toolwindow.select.section.title"))
      .setRenderer(sectionChoiceRenderer())
      .setItemChosenCallback(choice -> selectSection(project, sectionNode, choice.index()))
      .createPopup()
      .show(point);
  }

  /** Shows the environment SDK dropdown, anchored at the given point. */
  public static void showEnvironmentSdkPopup(@NotNull Project project, @NotNull EnvSdkNode sdkNode, @NotNull RelativePoint point) {
    List<SdkChoice> choices = new ArrayList<>();
    choices.add(new SdkChoice(null, HaxeBundle.message("haxe.toolwindow.environment.sdk.default.choice")));
    for (Sdk sdk : ProjectJdkTable.getInstance().getSdksOfType(HaxeSdkType.getInstance())) {
      choices.add(new SdkChoice(sdk.getName(), sdk.getName()));
    }
    HaxeModuleSdkApplier applier = HaxeModuleSdkApplier.getInstance(project);
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(choices)
      .setTitle(HaxeBundle.message("haxe.toolwindow.select.sdk.title"))
      .setRenderer(BuilderKt.textListCellRenderer("", SdkChoice::display))
      .setItemChosenCallback(choice -> applier.chooseSdk(sdkNode.containerId(), choice.name()))
      .createPopup()
      .show(point);
  }

  /** Level choices mirror the Haxe Compiler settings page: an explicit level, or null = project default. */
  public static void showLanguageLevelPopup(@NotNull Project project,
                                            @NotNull EnvLanguageLevelNode levelNode,
                                            @NotNull RelativePoint point) {
    HaxeCompilerSettings compilerSettings = HaxeCompilerSettings.getInstance(project);
    List<LevelChoice> choices = new ArrayList<>();
    String defaultLevel = compilerSettings.getDefaultLanguageLevel(levelNode.containerId()).getPresentableText();
    String defaultDisplay = HaxeBundle.message("haxe.toolwindow.node.environment.sdk.default", defaultLevel);
    choices.add(new LevelChoice(null, defaultDisplay));
    for (HaxeLanguageLevel level : HaxeLanguageLevel.values()) {
      choices.add(new LevelChoice(level, level.getPresentableText()));
    }
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(choices)
      .setTitle(HaxeBundle.message("haxe.toolwindow.select.language.level.title"))
      .setRenderer(BuilderKt.textListCellRenderer("", LevelChoice::display))
      .setItemChosenCallback(choice -> {
        compilerSettings.setContainerLanguageLevelOverride(levelNode.containerId(), choice.level());
        HaxeLanguageLevelUtil.notifyLanguageLevelChanged(project);
      })
      .createPopup()
      .show(point);
  }

  /** Edits the container's {@code --custom-target} name; an empty entry clears it. */
  public static void editCustomTarget(@NotNull Project project, @NotNull EnvCustomTargetNode customTargetNode) {
    String entered = Messages.showInputDialog(project,
                                              HaxeBundle.message("haxe.toolwindow.custom.target.prompt"),
                                              HaxeBundle.message("haxe.toolwindow.custom.target.title"),
                                              null,
                                              customTargetNode.customTarget(),
                                              HaxeEnvironmentDialog.CUSTOM_TARGET_VALIDATOR);
    if (entered == null) return;
    HaxeEnvironmentStore.getInstance(project).setCustomTarget(customTargetNode.containerId(), entered);
  }

  /** Opens the compile command configuration dialog; OK applies the command. */
  public static void configureCompileCommand(@NotNull Project project, @NotNull EnvCompileCommandNode compileCommand) {
    HaxeCompileCommandDialog dialog = new HaxeCompileCommandDialog(project, compileCommand.containerId(),
                                                                   compileCommand.candidateFilePaths(),
                                                                   compileCommand.actionNamesByFile());
    if (dialog.showAndGet()) {
      applyCompileCommand(project, compileCommand.containerId(), dialog.selectedCompileCommand());
    }
  }

  /**
   * Stores the container's compile command. A module that does not own the
   * active build file takes its libraries from the command's file, so a file
   * change re-syncs them.
   */
  public static void applyCompileCommand(@NotNull Project project,
                                         @NotNull String containerId,
                                         @Nullable CompileCommand command) {
    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    String previousFile = buildFilePath(store.getCompileCommand(containerId));
    boolean fileChanged = !Objects.equals(previousFile, buildFilePath(command));
    store.setCompileCommand(containerId, command);
    if (fileChanged) HaxeLibrarySync.sync(project, null);
  }

  /** Toggles the container's server participation; while disabled project-wide, opens the settings page instead. */
  public static void toggleCompilationServer(@NotNull Project project, @NotNull CompilationServerNode serverNode) {
    if (!serverNode.projectEnabled()) {
      ShowSettingsUtil.getInstance().showSettingsDialog(project, HaxeBuildToolsConfigurable.class);
      return;
    }
    boolean enable = !serverNode.moduleUses();
    HaxeEnvironmentStore.getInstance(project).setUsingCompilationServer(serverNode.containerId(), enable);
    if (!enable) {
      // an opted-out container sends no more requests - a lingering failure could never clear itself
      HaxeContextFailures.getInstance(project).record(serverNode.containerId(), null);
    }
  }

  public static void confirmAndRemoveBuildFile(@NotNull Project project, @NotNull BuildFileRow row) {
    String confirmKey = row.manual() ? "haxe.toolwindow.remove.build.file.confirm"
                                     : "haxe.toolwindow.hide.build.file.confirm";
    String titleKey = row.manual() ? "haxe.toolwindow.remove.build.file" : "haxe.toolwindow.hide.build.file";
    int answer = Messages.showYesNoDialog(project,
                                          HaxeBundle.message(confirmKey, row.buildFile().file().getName()),
                                          HaxeBundle.message(titleKey),
                                          Messages.getQuestionIcon());
    if (answer != Messages.YES) return;
    HaxeBuildFilesStore.getInstance(project).removeFile(row.containerId(), row.buildFile().file().getPath());
  }

  public static void confirmAndRemoveDefine(@NotNull Project project, @NotNull EnvDefineNode define) {
    int answer = Messages.showYesNoDialog(project,
                                          HaxeBundle.message("haxe.toolwindow.remove.define.confirm", define.name()),
                                          HaxeBundle.message("haxe.toolwindow.remove.define"),
                                          Messages.getQuestionIcon());
    if (answer != Messages.YES) return;
    HaxeEnvironmentStore.getInstance(project).removeDefine(define.containerId(), define.name());
  }

  /** Module removal after confirmation. Files on disk are untouched - the folder folds back into the surrounding module. */
  public static void confirmAndRemoveModule(@NotNull Project project, @NotNull ModuleNode module) {
    int answer = Messages.showYesNoDialog(project,
                                          HaxeBundle.message("haxe.toolwindow.remove.module.confirm", module.name()),
                                          HaxeBundle.message("haxe.toolwindow.remove.module"),
                                          Messages.getWarningIcon());
    if (answer != Messages.YES) return;
    HaxeModuleWorkspace.getInstance(project).removeModuleAsync(module.name());
  }

  /** Stores the target and, when it changed, re-syncs the module libraries. */
  private static void selectTarget(@NotNull Project project, @NotNull VirtualFile buildFile, @NotNull String targetId) {
    HaxeTargetSelectionStore store = HaxeTargetSelectionStore.getInstance(project);
    boolean changed = !targetId.equals(store.getSelectedTargetId(buildFile));
    store.setSelectedTargetId(buildFile, targetId);
    if (changed) HaxeLibrarySync.sync(project, null);
  }

  /** Stores the section and, when it changed, re-syncs the module libraries. */
  private static void selectSection(@NotNull Project project, @NotNull SectionNode sectionNode, int index) {
    VirtualFile buildFile = sectionNode.buildFile().file();
    HaxeSectionSelectionStore store = HaxeSectionSelectionStore.getInstance(project);
    boolean changed = index != store.getSelectedSection(buildFile, sectionNode.ids());
    store.setSelectedSection(buildFile, sectionNode.ids().get(index));
    if (changed) HaxeLibrarySync.sync(project, null);
  }

  @Nullable
  private static String buildFilePath(@Nullable CompileCommand command) {
    return command == null ? null : command.buildFilePath();
  }

  /** Section label first, its target/output descriptor grayed - one glance tells the sections apart. */
  @NotNull
  private static ListCellRenderer<SectionChoice> sectionChoiceRenderer() {
    return BuilderKt.listCellRenderer(row -> {
      SectionChoice choice = row.getValue();
      row.text(choice.label(), null);
      if (!choice.descriptor().isEmpty()) {
        row.text(choice.descriptor(), params -> {
          params.setAttributes(SimpleTextAttributes.GRAYED_ATTRIBUTES);
          return Unit.INSTANCE;
        });
      }
      return Unit.INSTANCE;
    });
  }
}
