package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.profiler.HaxeProfilerSnapshotOpener;
import com.intellij.profiler.actions.ImportProfilerResultAction;
import com.intellij.util.ExceptionUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/**
 * Hands the snapshot to the IU profiler's import path — provider lookup by
 * extension and signature included. The import takes the snapshot as a
 * {@link Path} from 2026.3 on and as a {@link File} before; no member exists
 * in both, so the call is looked up in the running IDE.
 */
// TODO: call ImportProfilerResultAction.Companion.importProfilerDumpAsAssociated(project, snapshot) directly once 2026.2 support ends
@CustomLog
public class HaxeIuSnapshotOpener implements HaxeProfilerSnapshotOpener {

  private static final String ASSOCIATED_IMPORT = "importProfilerDumpAsAssociated";

  @Override
  public void open(@NotNull Project project, @NotNull Path snapshot) {
    try {
      importAsAssociated(project, snapshot);
    }
    catch (InvocationTargetException e) {
      // the import itself failed: surface it as a direct call would
      ExceptionUtil.rethrowAllAsUnchecked(e.getCause());
    }
    catch (ReflectiveOperationException e) {
      log.error("The IU profiler offers no " + ASSOCIATED_IMPORT + " this plugin knows", e);
    }
  }

  /** The 2026.3 form when the IDE has it, else the 2026.2 one. */
  private static void importAsAssociated(@NotNull Project project, @NotNull Path snapshot) throws ReflectiveOperationException {
    ImportProfilerResultAction.Companion importer = ImportProfilerResultAction.Companion;
    try {
      importer.getClass().getMethod(ASSOCIATED_IMPORT, Project.class, Path.class).invoke(importer, project, snapshot);
    }
    catch (NoSuchMethodException e) {
      importer.getClass().getMethod(ASSOCIATED_IMPORT, Project.class, File.class).invoke(importer, project, snapshot.toFile());
    }
  }
}
