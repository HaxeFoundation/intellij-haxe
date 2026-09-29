package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerMetrics;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The status half of a server console tab: whether the containers this server
 * serves currently FAIL compiler requests (full error text, selectable and
 * copyable — the tree row's tooltip is neither), the IDE-side request
 * statistics, and — on demand via Refresh — the server's own cache memory per
 * compilation context ({@code server/memory}; fetched on request only, since
 * every query queues behind compiles on the single-connection server).
 */
final class HaxeServerStatusPanel extends JPanel {

  private final JBLabel header = new JBLabel();
  private final JBTextArea details = new JBTextArea();
  private boolean failing;

  private @Nullable Project project;
  private @Nullable String serverId;
  private @Nullable String serverStats;

  HaxeServerStatusPanel() {
    super(new BorderLayout(0, JBUI.scale(4)));
    setBorder(JBUI.Borders.empty(6));
    details.setEditable(false);
    details.setLineWrap(true);
    details.setWrapStyleWord(true);
    add(header, BorderLayout.NORTH);
    add(new JBScrollPane(details), BorderLayout.CENTER);

    JButton refresh = new JButton(HaxeBundle.message("haxe.server.console.stats.refresh"), AllIcons.Actions.Refresh);
    refresh.addActionListener(event -> fetchServerStats());
    JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    buttonRow.add(refresh);
    add(buttonRow, BorderLayout.SOUTH);
  }

  /** Refreshes from the health store; the server serves every container whose effective SDK resolves to its executable. */
  void update(@NotNull Project project, @NotNull String serverId) {
    this.project = project;
    this.serverId = serverId;

    Map<String, String> failures = new LinkedHashMap<>();
    HaxeContextFailures.getInstance(project).snapshot().forEach((containerId, failure) -> {
      if (serverId.equals(HaxeCompilationServerManager.serverIdFor(project, containerId))) {
        failures.put(containerId, failure);
      }
    });

    failing = !failures.isEmpty();
    if (failing) {
      header.setIcon(AllIcons.General.Error);
      header.setText(HaxeBundle.message("haxe.server.console.status.failing", failures.size()));
    }
    else {
      header.setIcon(AllIcons.General.InspectionsOK);
      header.setText(HaxeBundle.message("haxe.server.console.status.ok"));
    }

    StringBuilder text = new StringBuilder();
    HaxeServerMetrics.Snapshot requests = HaxeServerMetrics.getInstance(project).snapshot(serverId);
    if (requests.requests() > 0) {
      String requestStats = HaxeBundle.message("haxe.server.console.stats.requests", requests.requests(), requests.failures(),
                                               requests.lastMillis(), requests.averageMillis());
      text.append(requestStats).append('\n');
    }
    if (serverStats != null) {
      text.append(serverStats).append('\n');
    }
    if (failing) {
      if (!text.isEmpty()) text.append('\n');
      failures.forEach((containerId, failure) -> text.append("[%s] %s\n\n".formatted(containerId, failure)));
    }
    details.setText(text.toString().stripTrailing());
    details.setCaretPosition(0);
  }

  boolean hasFailures() {
    return failing;
  }

  /** Drops the fetched server stats — they describe a process that no longer runs. */
  void clearFetchedStats() {
    serverStats = null;
  }

  /** Queries {@code server/memory} in the background and re-renders; no-op while the server is down. */
  private void fetchServerStats() {
    Project currentProject = project;
    String currentServerId = serverId;
    if (currentProject == null || currentServerId == null) return;
    int port = HaxeCompilationServerManager.getInstance(currentProject).runningPort(currentServerId);
    if (port <= 0) {
      serverStats = HaxeBundle.message("haxe.server.console.stats.unavailable");
      update(currentProject, currentServerId);
      return;
    }

    AppExecutorUtil.getAppExecutorService().execute(() -> {
      String rendered;
      try {
        ServerMemory memory = HaxeCompilerDisplayService.fetchServerMemory(port);
        rendered = renderMemory(memory);
      }
      catch (DisplayRequestException e) {
        rendered = HaxeBundle.message("haxe.server.console.stats.failed", StringUtil.notNullize(e.getMessage()));
      }
      String result = rendered;
      ApplicationManager.getApplication().invokeLater(() -> {
        serverStats = result;
        if (project != null && serverId != null) {
          update(project, serverId);
        }
      });
    });
  }

  @NotNull
  private static String renderMemory(@NotNull ServerMemory memory) {
    String totalCache = StringUtil.formatFileSize(memory.totalCacheBytes());
    StringBuilder text = new StringBuilder(HaxeBundle.message("haxe.server.console.stats.memory", totalCache, memory.contexts().size()));
    for (ServerMemory.ContextSize context : memory.contexts()) {
      // the signature prefix identifies the context in server logs; platform + size are the useful glance
      String signature = StringUtil.first(context.context().signature(), 8, false);
      String size = StringUtil.formatFileSize(context.bytes());
      text.append("\n  %s [%s] %s".formatted(context.context().platform(), signature, size));
    }
    return text.toString();
  }
}
