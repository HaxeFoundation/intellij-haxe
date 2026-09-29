package com.intellij.plugins.haxe.v2.tools;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolConfigs;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Shows an IDE diff of the editor buffer against the formatter's output,
 * through the tool's stdin pipe mode - nothing touches the disk, and unsaved
 * changes are formatted as they are. The single {@code -s} argument only tells
 * the formatter which file the content belongs to for config discovery.
 */
@CustomLog
public final class HaxeShowFormattingDiffAction extends HaxeFileToolAction {

  private static final int FORMAT_TIMEOUT_MS = 60_000;

  // the formatter's --stdin exit codes: 0 formatted, 1 formatting disabled for
  // the file, 2 format failure, 3 usage/path errors, -1 stdin read errors
  private static final int EXIT_FORMATTED = 0;
  private static final int EXIT_DISABLED = 1;

  @Override
  @NotNull
  String configName() {
    return HaxeToolConfigs.FORMATTER_CONFIG_NAME;
  }

  @Override
  @NotNull
  String toolHaxelib() {
    return HaxeToolConfigs.FORMATTER_HAXELIB;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = haxeFile(e);
    VirtualFile configDirectory = configDirectory(e);
    if (project == null || file == null || configDirectory == null) return;
    Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return;

    String before = document.getText();
    String haxelib = haxelibFor(project, file);
    String title = HaxeBundle.message("haxe.tools.format.diff.progress", file.getName());
    new Task.Backgroundable(project, title, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        formatAndShow(project, file, document, before, haxelib, configDirectory.getPath());
      }
    }.queue();
  }

  private static void formatAndShow(@NotNull Project project, @NotNull VirtualFile file, @NotNull Document document,
                                    @NotNull String before, @NotNull String haxelib, @NotNull String workDirectory) {
    GeneralCommandLine commandLine = new GeneralCommandLine(haxelib, "run", HaxeToolConfigs.FORMATTER_HAXELIB, "--stdin", "-s", file.getPath())
      .withWorkingDirectory(Path.of(workDirectory))
      .withCharset(StandardCharsets.UTF_8);

    ProcessOutput output;
    try {
      CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
      feedStdin(handler.getProcessInput(), before);
      output = handler.runProcess(FORMAT_TIMEOUT_MS);
    }
    catch (Exception exception) {
      notifyFailure(project, file, exception.getMessage());
      return;
    }

    if (output.isTimeout()) {
      notifyFailure(project, file, HaxeBundle.message("haxe.tools.format.diff.timeout"));
      return;
    }
    if (output.getExitCode() == EXIT_DISABLED) {
      String message = HaxeBundle.message("haxe.tools.format.diff.disabled", file.getName());
      HaxeCommandNotifications.notify(project, diffTitle(file), message, NotificationType.INFORMATION);
      return;
    }
    if (output.getExitCode() != EXIT_FORMATTED) {
      notifyFailure(project, file, StringUtil.defaultIfEmpty(output.getStderr().trim(), output.getStdout().trim()));
      return;
    }

    // neko on windows can emit CRLF; documents are LF
    String formatted = StringUtil.convertLineSeparators(output.getStdout());
    if (formatted.strip().equals(before.strip())) {
      String message = HaxeBundle.message("haxe.tools.format.diff.clean", file.getName());
      HaxeCommandNotifications.notify(project, diffTitle(file), message, NotificationType.INFORMATION);
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;
      DiffContentFactory factory = DiffContentFactory.getInstance();
      DocumentContent current = factory.create(project, document);
      DocumentContent formattedContent = factory.create(project, formatted, file.getFileType());
      SimpleDiffRequest request = new SimpleDiffRequest(diffTitle(file), current, formattedContent,
                                                        HaxeBundle.message("haxe.tools.format.diff.current"),
                                                        HaxeBundle.message("haxe.tools.format.diff.formatted"));
      DiffManager.getInstance().showDiff(project, request);
    });
  }

  /** The pipe fills ahead of the pump; a writer thread keeps large buffers from deadlocking it. */
  private static void feedStdin(@NotNull OutputStream processInput, @NotNull String content) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try (processInput) {
        processInput.write(content.getBytes(StandardCharsets.UTF_8));
      }
      catch (IOException exception) {
        log.info("formatter stdin feed failed: " + exception.getMessage());
      }
    });
  }

  private static void notifyFailure(@NotNull Project project, @NotNull VirtualFile file, @NotNull String message) {
    HaxeCommandNotifications.notify(project, diffTitle(file), message, NotificationType.ERROR);
  }

  @NotNull
  private static String diffTitle(@NotNull VirtualFile file) {
    return HaxeBundle.message("haxe.tools.format.diff.title", file.getName());
  }
}
