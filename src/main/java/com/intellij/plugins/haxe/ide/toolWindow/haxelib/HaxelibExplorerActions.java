package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibLocalDocs;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.ide.toolWindow.haxelib.HaxelibExplorerPanel.LibraryRow;
import com.intellij.plugins.haxe.ide.toolWindow.haxelib.HaxelibExplorerPanel.VersionEntry;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibInstaller;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The explorer tree's context menu: install/remove/set-current on a VERSION
 * node, install-latest/remove-all on a LIBRARY node. Every mutation runs in
 * the background through {@link HaxelibInstaller}, then refreshes the
 * installed picture and kicks the v2 library sync so External Libraries and
 * resolve follow immediately.
 */
final class HaxelibExplorerActions {

  private HaxelibExplorerActions() {
  }

  @NotNull
  static DefaultActionGroup createGroup(@NotNull HaxelibExplorerPanel panel) {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new InstallAndSetCurrent(panel));
    group.add(new InstallVersion(panel));
    group.add(new SetCurrent(panel));
    group.add(new RemoveVersion(panel));
    group.addSeparator();
    group.add(new InstallLatest(panel));
    group.add(new RemoveLibrary(panel));
    group.add(new AddLibrary(panel));
    group.addSeparator();
    group.add(new SetDevDirectory(panel));
    group.add(new RemoveDevDirectory(panel));
    group.add(new InstallFromGitRepository(panel));
    group.addSeparator();
    group.add(new ShowVersionInFileManager(panel));
    group.add(new OpenVersionInTerminal(panel));
    return group;
  }

  /** The tree toolbar's plus button — the same add flow the context menu offers (icon-less there). */
  @NotNull
  static AnAction createAddLibraryAction(@NotNull HaxelibExplorerPanel panel) {
    AnAction action = new AddLibrary(panel);
    action.getTemplatePresentation().setIcon(AllIcons.General.Add);
    return action;
  }

  private abstract static class ExplorerAction extends DumbAwareAction {
    final HaxelibExplorerPanel panel;

    ExplorerAction(@NotNull HaxelibExplorerPanel panel, @NotNull Supplier<String> text) {
      super(text);
      this.panel = panel;
    }

    @Nullable
    VersionEntry selectedVersion() {
      return panel.selectedUserObject() instanceof VersionEntry entry ? entry : null;
    }

    @Nullable
    LibraryRow selectedLibrary() {
      return panel.selectedUserObject() instanceof LibraryRow row ? row : null;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }

    boolean confirmRemoval(@NotNull String target) {
      return Messages.showYesNoDialog(panel.getProject(),
                                      HaxeBundle.message("haxelib.explorer.action.remove.confirm", target),
                                      HaxeBundle.message("haxelib.explorer.action.remove.title"),
                                      Messages.getWarningIcon()) == Messages.YES;
    }

    /** The selected version's install directory, or null when it is not on disk (a stale dev pointer, say). */
    @Nullable
    Path versionDirectory(@NotNull VersionEntry entry) {
      Path repoRoot = panel.repositoryRoot();
      return repoRoot == null ? null : HaxelibLocalDocs.versionDirectory(repoRoot, entry.library(), entry.version());
    }

    void notifyMissingDirectory(@NotNull String title, @NotNull VersionEntry entry) {
      String message = HaxeBundle.message("haxelib.explorer.action.show.in.files.missing", entry.library(), entry.version());
      HaxeCommandNotifications.notify(panel.getProject(), title, message, NotificationType.WARNING);
    }

    /** Runs the mutation in the background; on success refreshes the explorer (dropping the library's stale info) and the v2 library sync. */
    void mutate(@NotNull String progressTitle, @NotNull String libraryName, @NotNull Supplier<@Nullable String> mutation) {
      Project project = panel.getProject();
      new Task.Backgroundable(project, progressTitle, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          String failure = mutation.get();
          if (failure == null) {
            HaxeLibrarySync.sync(project, () -> { });
            ApplicationManager.getApplication().invokeLater(() -> panel.reloadAfterMutation(libraryName));
          }
          else {
            HaxeCommandNotifications.notify(project, progressTitle, failure, NotificationType.ERROR);
          }
        }
      }.queue();
    }
  }

  private static final class InstallAndSetCurrent extends ExplorerAction {
    private InstallAndSetCurrent(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.install.set.current"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      e.getPresentation().setEnabledAndVisible(entry != null && !entry.installed());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      if (entry == null) return;
      // haxelib install SELECTS the installed version as a side effect;
      // passing no version to restore keeps that selection in place
      mutate(HaxeBundle.message("haxelib.explorer.action.install.progress", entry.library(), entry.version()),
             entry.library(),
             () -> HaxelibInstaller.install(panel.getProject(), entry.library(), entry.version(), null));
    }
  }

  private static final class InstallVersion extends ExplorerAction {
    private InstallVersion(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.install.version"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      boolean applicable = entry != null && !entry.installed();
      e.getPresentation().setEnabledAndVisible(applicable);
      if (applicable) {
        e.getPresentation().setText(
          HaxeBundle.message("haxelib.explorer.action.install.version.named", entry.library(), entry.version()));
      }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      LibraryRow row = panel.selectedLibraryRow();
      if (entry == null) return;
      String selected = row != null ? row.selectedVersion() : null;
      mutate(HaxeBundle.message("haxelib.explorer.action.install.progress", entry.library(), entry.version()),
             entry.library(),
             () -> HaxelibInstaller.install(panel.getProject(), entry.library(), entry.version(), selected));
    }
  }

  private static final class SetCurrent extends ExplorerAction {
    private SetCurrent(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.set.current"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      e.getPresentation().setEnabledAndVisible(entry != null && entry.installed() && !entry.current());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      LibraryRow row = panel.selectedLibraryRow();
      if (entry == null) return;
      // a dev pointer overrides the .current selection and haxelib set never
      // touches it - without clearing it the set would look ignored
      boolean devOverride = row != null && row.dev();
      mutate(HaxeBundle.message("haxelib.explorer.action.set.current.progress", entry.library(), entry.version()),
             entry.library(),
             () -> setCurrentClearingDev(entry, devOverride));
    }

    @Nullable
    private String setCurrentClearingDev(@NotNull VersionEntry entry, boolean devOverride) {
      String failure = HaxelibInstaller.setCurrent(panel.getProject(), entry.library(), entry.version());
      if (failure != null || !devOverride) return failure;
      return HaxelibInstaller.clearDev(panel.getProject(), entry.library());
    }
  }

  private static final class RemoveVersion extends ExplorerAction {
    private RemoveVersion(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.remove.version"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      // the dev pseudo-version is a pointer, not an installed directory -
      // Remove Development Directory handles it
      boolean removable = entry != null && entry.installed() && !HaxelibSemVer.DEV.equals(entry.version());
      e.getPresentation().setEnabledAndVisible(removable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      LibraryRow row = panel.selectedLibraryRow();
      if (entry == null) return;
      String target = entry.library() + " " + entry.version();
      if (!confirmRemoval(target)) return;
      String fallback = newestOtherRelease(row, entry.version());
      mutate(HaxeBundle.message("haxelib.explorer.action.remove.progress", target),
             entry.library(),
             () -> removeSteppingOffCurrent(entry, fallback));
    }

    /**
     * haxelib refuses to remove the version its .current file names - even
     * when a dev pointer is what actually drives resolution. On that refusal
     * the newest other installed release is selected and the removal retried;
     * without one the removal cannot work (haxelib always keeps a current
     * version), reported with a pointer to Remove Library. Any other failure
     * (or a changed refusal wording in a future haxelib) surfaces as-is.
     */
    @Nullable
    private String removeSteppingOffCurrent(@NotNull VersionEntry entry, @Nullable String fallback) {
      Project project = panel.getProject();
      String failure = HaxelibInstaller.remove(project, entry.library(), entry.version());
      boolean currentRefusal = failure != null && failure.contains("Can't remove current version");
      if (!currentRefusal) return failure;
      if (fallback == null) {
        return HaxeBundle.message("haxelib.explorer.action.remove.version.last.release", entry.library());
      }
      String setFailure = HaxelibInstaller.setCurrent(project, entry.library(), fallback);
      if (setFailure != null) return setFailure;
      return HaxelibInstaller.remove(project, entry.library(), entry.version());
    }

    /** The newest OTHER installed release - what becomes current when the current version itself is removed. */
    @Nullable
    private static String newestOtherRelease(@Nullable LibraryRow row, @NotNull String removedVersion) {
      if (row == null) return null;
      return row.installedVersions().stream()
        .filter(version -> !HaxelibSemVer.isPseudoVersion(version) && !version.equals(removedVersion))
        .max(Comparator.comparing(HaxelibSemVer::create))
        .orElse(null);
    }
  }

  private static final class InstallLatest extends ExplorerAction {
    private InstallLatest(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.install.latest"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      e.getPresentation().setEnabledAndVisible(row != null && !row.installed());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      if (row == null) return;
      mutate(HaxeBundle.message("haxelib.explorer.action.install.latest.progress", row.name()),
             row.name(),
             () -> HaxelibInstaller.install(panel.getProject(), row.name(), null, null));
    }
  }

  private static final class SetDevDirectory extends ExplorerAction {
    private SetDevDirectory(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.set.dev"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      // registering a dev directory needs no installed release - haxelib
      // creates the repository entry, exactly how unpublished libs are used
      e.getPresentation().setEnabledAndVisible(selectedLibrary() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      if (row == null) return;
      FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleDir()
        .withTitle(HaxeBundle.message("haxelib.explorer.action.set.dev.chooser.title", row.name()));
      VirtualFile chosen = FileChooser.chooseFile(descriptor, panel.getProject(), currentDevDirectory(row));
      if (chosen == null) return;
      String directory = FileUtil.toSystemDependentName(chosen.getPath());
      mutate(HaxeBundle.message("haxelib.explorer.action.set.dev.progress", row.name()),
             row.name(),
             () -> HaxelibInstaller.setDev(panel.getProject(), row.name(), directory));
    }

    /** The chooser's starting point: the registered dev directory when one exists. */
    @Nullable
    private VirtualFile currentDevDirectory(@NotNull LibraryRow row) {
      String devPath = panel.devDirectoryOf(row);
      return devPath == null ? null : LocalFileSystem.getInstance().findFileByPath(devPath);
    }
  }

  private static final class AddLibrary extends ExplorerAction {
    private AddLibrary(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.add.library"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      HaxelibAddLibraryDialog dialog = new HaxelibAddLibraryDialog(panel.getProject());
      if (!dialog.showAndGet()) return;
      String name = dialog.getLibraryName();
      panel.revealAfterReload(name);
      if (dialog.isDevMethod()) {
        String directory = dialog.getDevDirectory();
        mutate(HaxeBundle.message("haxelib.explorer.action.set.dev.progress", name),
               name,
               () -> HaxelibInstaller.setDev(panel.getProject(), name, directory));
      }
      else {
        String url = dialog.getGitUrl();
        String ref = dialog.getGitRef();
        mutate(HaxeBundle.message("haxelib.explorer.action.install.git.progress", name),
               name,
               () -> HaxelibInstaller.installGit(panel.getProject(), name, url, ref));
      }
    }
  }

  private static final class InstallFromGitRepository extends ExplorerAction {
    private InstallFromGitRepository(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.install.git"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      // like a dev directory, a git install needs no installed release -
      // haxelib registers the library when unknown
      e.getPresentation().setEnabledAndVisible(selectedLibrary() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      if (row == null) return;
      HaxelibLocalDocs.GitCheckout existing = panel.gitCheckoutOf(row);
      String initialUrl = existing == null ? null : existing.remoteUrl();
      String initialRef = existing == null ? null : existing.branch();
      HaxelibGitInstallDialog dialog = new HaxelibGitInstallDialog(panel.getProject(), row.name(), initialUrl, initialRef);
      if (!dialog.showAndGet()) return;
      String url = dialog.getUrl();
      String ref = dialog.getRef();
      mutate(HaxeBundle.message("haxelib.explorer.action.install.git.progress", row.name()),
             row.name(),
             () -> HaxelibInstaller.installGit(panel.getProject(), row.name(), url, ref));
    }
  }

  private static final class RemoveDevDirectory extends ExplorerAction {
    private RemoveDevDirectory(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.remove.dev"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabledAndVisible(devLibraryName() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String library = devLibraryName();
      if (library == null) return;
      mutate(HaxeBundle.message("haxelib.explorer.action.remove.dev.progress", library),
             library,
             () -> HaxelibInstaller.clearDev(panel.getProject(), library));
    }

    /** The selection's library when its dev pointer is set - the library node or any of its version nodes. */
    @Nullable
    private String devLibraryName() {
      LibraryRow row = panel.selectedLibraryRow();
      return row != null && row.dev() ? row.name() : null;
    }
  }

  private static final class RemoveLibrary extends ExplorerAction {
    private RemoveLibrary(@NotNull HaxelibExplorerPanel panel) {
      super(panel, () -> HaxeBundle.message("haxelib.explorer.action.remove.library"));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      e.getPresentation().setEnabledAndVisible(row != null && row.installed());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      LibraryRow row = selectedLibrary();
      if (row == null) return;
      if (!confirmRemoval(row.name())) return;
      mutate(HaxeBundle.message("haxelib.explorer.action.remove.progress", row.name()),
             row.name(),
             () -> HaxelibInstaller.remove(panel.getProject(), row.name(), null));
    }
  }

  /** Reveals the selected version's install directory in the system file manager, named for that manager. */
  private static final class ShowVersionInFileManager extends ExplorerAction {
    private ShowVersionInFileManager(@NotNull HaxelibExplorerPanel panel) {
      super(panel, RevealFileAction::getActionName);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      // directory resolution reads the disk - the perform side handles a missing one
      boolean applicable = entry != null && entry.installed() && RevealFileAction.isDirectoryOpenSupported();
      e.getPresentation().setEnabledAndVisible(applicable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      if (entry == null) return;
      Path directory = versionDirectory(entry);
      if (directory == null) {
        notifyMissingDirectory(RevealFileAction.getActionName(), entry);
        return;
      }
      RevealFileAction.openDirectory(directory);
    }
  }

  /** Opens a terminal tab in the selected version's install directory, through the terminal plugin's own action. */
  private static final class OpenVersionInTerminal extends ExplorerAction {
    // one action per terminal engine; each disables itself for the other engine,
    // so the delegate is whichever reports enabled for the directory
    private static final List<String> TERMINAL_ACTION_IDS =
      List.of("Terminal.OpenInTerminal", "Terminal.OpenInReworkedTerminal");

    private OpenVersionInTerminal(@NotNull HaxelibExplorerPanel panel) {
      super(panel, OpenVersionInTerminal::terminalActionText);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      // directory resolution reads the disk - the perform side handles a missing one
      boolean applicable = entry != null && entry.installed() && registeredTerminalAction() != null;
      e.getPresentation().setEnabledAndVisible(applicable);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      VersionEntry entry = selectedVersion();
      if (entry == null) return;
      Path directory = versionDirectory(entry);
      VirtualFile directoryFile = directory == null ? null
                                                    : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
      if (directoryFile == null) {
        notifyMissingDirectory(terminalActionText(), entry);
        return;
      }

      DataContext context = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, panel.getProject())
        .add(CommonDataKeys.VIRTUAL_FILE, directoryFile)
        .build();
      for (String actionId : TERMINAL_ACTION_IDS) {
        AnAction delegate = ActionManager.getInstance().getAction(actionId);
        if (delegate == null) continue;
        AnActionEvent delegateEvent = AnActionEvent.createEvent(delegate, context, null, e.getPlace(), ActionUiKind.NONE, null);
        ActionUtil.updateAction(delegate, delegateEvent);
        if (delegateEvent.getPresentation().isEnabledAndVisible()) {
          ActionUtil.performAction(delegate, delegateEvent);
          return;
        }
      }
    }

    /** The terminal plugin's own menu text, so this entry matches the platform's Open In menu. */
    @NotNull
    private static String terminalActionText() {
      AnAction action = registeredTerminalAction();
      String text = action != null ? action.getTemplatePresentation().getText() : null;
      return text != null ? text : "";
    }

    @Nullable
    private static AnAction registeredTerminalAction() {
      ActionManager manager = ActionManager.getInstance();
      return TERMINAL_ACTION_IDS.stream()
        .map(manager::getAction)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
    }
  }
}
