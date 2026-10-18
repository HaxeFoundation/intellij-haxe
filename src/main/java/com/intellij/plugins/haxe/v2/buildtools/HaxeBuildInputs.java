package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * One modification tracker for everything that changes which build files
 * exist or what they compile: a build file registered, added, moved, saved
 * or deleted; a target, section, tests mark, work directory or environment
 * change; a landed evaluation; a library sync; a cache clear. The caches
 * deriving source directories from the build files (resolve contexts, test
 * contexts) depend on {@link #dependencies}: this tracker, the project roots
 * and the hxml and xml PSI, never the whole-PSI counter.
 */
@Service(Service.Level.PROJECT)
public final class HaxeBuildInputs implements Disposable {

  private final Project project;
  private final SimpleModificationTracker tracker = new SimpleModificationTracker();

  public HaxeBuildInputs(@NotNull Project project) {
    this.project = project;
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(HaxeBuildSettingsListener.TOPIC, new BuildFileSettings());
    connection.subscribe(HaxeBuildEventListener.TOPIC, new BuildFileEvents());
  }

  @NotNull
  public static HaxeBuildInputs getInstance(@NotNull Project project) {
    return project.getService(HaxeBuildInputs.class);
  }

  /** Bumps whenever a build input changed; a cache keyed on it alone stays valid across edits of Haxe sources. */
  @NotNull
  public ModificationTracker tracker() {
    return tracker;
  }

  /** The dependencies of a cache deriving facts from the build files: this tracker, the project roots, the hxml and xml PSI. */
  public Object @NotNull [] dependencies() {
    PsiModificationTracker psiTracker = PsiModificationTracker.getInstance(project);
    return new Object[]{
      tracker,
      ProjectRootModificationTracker.getInstance(project),
      psiTracker.forLanguage(HXMLLanguage.INSTANCE),
      psiTracker.forLanguage(XMLLanguage.INSTANCE)};
  }

  @Override
  public void dispose() {
  }

  /** The choices that change which build files exist or what they compile. The active file and a pin are read per ask, not cached. */
  private final class BuildFileSettings implements HaxeBuildSettingsListener {
    @Override
    public void stateLoaded() {
      tracker.incModificationCount();
    }

    @Override
    public void buildFilesChanged() {
      tracker.incModificationCount();
    }

    @Override
    public void targetSelected(@NotNull String buildFilePath) {
      tracker.incModificationCount();
    }

    @Override
    public void sectionSelected(@NotNull String buildFilePath) {
      tracker.incModificationCount();
    }

    @Override
    public void environmentChanged(@NotNull String containerId, @NotNull EnvironmentAspect aspect) {
      tracker.incModificationCount();
    }

    @Override
    public void testsMarkChanged(@NotNull String containerId) {
      tracker.incModificationCount();
    }

    @Override
    public void workDirectoryChanged(@NotNull String buildFilePath) {
      tracker.incModificationCount();
    }
  }

  /** The facts that change the build files on disk or their evaluated classpaths. */
  private final class BuildFileEvents implements HaxeBuildEventListener {
    @Override
    public void buildFileContentChanged(@NotNull String path) {
      tracker.incModificationCount();
    }

    @Override
    public void buildFileAdded(@NotNull String path) {
      tracker.incModificationCount();
    }

    @Override
    public void buildFileDeleted(@NotNull String path) {
      tracker.incModificationCount();
    }

    @Override
    public void evaluationLanded(@NotNull String buildFilePath) {
      tracker.incModificationCount();
    }

    /** The libraries a build reaches, transitive ones included, come from the sync. */
    @Override
    public void librariesSynced() {
      tracker.incModificationCount();
    }

    @Override
    public void derivedCachesCleared() {
      tracker.incModificationCount();
    }
  }
}
