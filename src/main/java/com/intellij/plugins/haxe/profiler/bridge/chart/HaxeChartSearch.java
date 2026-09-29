package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;

/**
 * The chart's search bar, docked above the minimap and hidden until typing
 * on the chart (or Ctrl+F) opens it. Highlighting follows every keystroke;
 * the session-wide match list loads debounced on a pooled thread, since a
 * zone capture scans its whole file. Enter/F3 and Shift+Enter/Shift+F3 walk
 * the matches in time order; Escape returns the focus to the chart.
 */
final class HaxeChartSearch implements HaxeCallChartPanel.SearchUi {
  private static final int MATCH_CAP = 10_000;
  private static final int SEARCH_DEBOUNCE_MS = 250;

  private final BorderLayoutPanel bar = new BorderLayoutPanel();
  private final SearchTextField field = new SearchTextField(false);
  private final JBLabel counter = new JBLabel();
  private final HaxeCallChartPanel chart;
  private final Supplier<HaxeChartData> data;
  private final Supplier<ProfilerThread> thread;
  private final Timer debounce = new Timer(SEARCH_DEBOUNCE_MS, event -> startScan());
  private final AtomicInteger generation = new AtomicInteger();
  private List<HaxeCallChartPanel.SearchMatch> matches = List.of();
  private int index = -1;

  HaxeChartSearch(HaxeCallChartPanel chart, Supplier<HaxeChartData> data, Supplier<ProfilerThread> thread) {
    this.chart = chart;
    this.data = data;
    this.thread = thread;
    debounce.setRepeats(false);
    counter.setBorder(JBUI.Borders.empty(0, 8));
    bar.addToCenter(field);
    bar.addToRight(counter);
    bar.setVisible(false);
    field.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent event) {
        chart.setSearchQuery(field.getText());
        counter.setText("");
        debounce.restart();
      }
    });
    JTextField editor = field.getTextEditor();
    editor.registerKeyboardAction(event -> nextMatch(), KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);
    editor.registerKeyboardAction(event -> previousMatch(), KeyStroke.getKeyStroke("shift ENTER"), JComponent.WHEN_FOCUSED);
    editor.registerKeyboardAction(event -> nextMatch(), KeyStroke.getKeyStroke("F3"), JComponent.WHEN_FOCUSED);
    editor.registerKeyboardAction(event -> previousMatch(), KeyStroke.getKeyStroke("shift F3"), JComponent.WHEN_FOCUSED);
    editor.registerKeyboardAction(event -> close(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_FOCUSED);
  }

  JComponent component() {
    return bar;
  }

  @Override
  public void open(@NotNull String seedText) {
    bar.setVisible(true);
    if (!seedText.isEmpty()) {
      field.setText(seedText);
    }
    JTextField editor = field.getTextEditor();
    editor.requestFocusInWindow();
    editor.setCaretPosition(editor.getText().length());
  }

  @Override
  public boolean close() {
    if (!bar.isVisible()) return false;
    bar.setVisible(false);
    field.setText("");
    chart.setSearchQuery(null);
    clearMatches();
    chart.requestFocusInWindow();
    return true;
  }

  @Override
  public void nextMatch() {
    step(1);
  }

  @Override
  public void previousMatch() {
    step(-1);
  }

  /** A thread switch or a live rebuild invalidated the match list; rescans while the bar shows a query. */
  void refresh() {
    if (bar.isVisible() && !field.getText().isBlank()) {
      debounce.restart();
    }
  }

  private void step(int direction) {
    if (matches.isEmpty()) return;
    index = Math.floorMod(index + direction, matches.size());
    counter.setText((index + 1) + "/" + totalText());
    chart.showSearchMatch(matches.get(index));
  }

  private String totalText() {
    return matches.size() >= MATCH_CAP ? MATCH_CAP + "+" : String.valueOf(matches.size());
  }

  private void clearMatches() {
    matches = List.of();
    index = -1;
    counter.setText("");
  }

  private void startScan() {
    String query = field.getText();
    if (query.isBlank()) {
      clearMatches();
      return;
    }
    ProfilerThread selectedThread = thread.get();
    if (selectedThread == null) return;

    int expected = generation.incrementAndGet();
    HaxeChartData source = data.get();
    int threadId = selectedThread.id();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      List<HaxeCallChartPanel.SearchMatch> found = source.searchMatches(threadId, query, MATCH_CAP);
      ApplicationManager.getApplication().invokeLater(() -> {
        if (generation.get() != expected) return; // a newer query superseded this scan
        matches = found;
        index = -1;
        counter.setText(totalText());
      });
    });
  }
}
