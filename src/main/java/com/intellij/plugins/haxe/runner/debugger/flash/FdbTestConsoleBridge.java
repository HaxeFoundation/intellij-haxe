package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The debug-session console of a flash-family TEST session: everything the
 * Flex debug process prints — fdb-relayed {@code [trace] } lines carrying the
 * TeamCity test protocol, its state messages, adl's own output — is rerouted
 * into the synthetic process handler the SM test console is attached to, so
 * the test tree builds while fdb owns the real process. The SM view renders
 * the session tab (all other calls delegate to it); print never reaches it
 * directly, because {@code ConsoleView.print} bypasses SM parsing.
 */
class FdbTestConsoleBridge implements ConsoleView {

  private final ConsoleView testConsole;
  private final ProcessHandler testOutputSink;

  FdbTestConsoleBridge(@NotNull ConsoleView testConsole, @NotNull ProcessHandler testOutputSink) {
    this.testConsole = testConsole;
    this.testOutputSink = testOutputSink;
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    Key<?> outputType = contentType == ConsoleViewContentType.ERROR_OUTPUT
                        ? ProcessOutputTypes.STDERR
                        : ProcessOutputTypes.STDOUT;
    testOutputSink.notifyTextAvailable(unwrapTraceLines(text), outputType);
  }

  /** fdb prefixes every app trace line with "[trace] " — stripped so the TeamCity messages start the line, as the converter expects. */
  @NotNull
  static String unwrapTraceLines(@NotNull String text) {
    // the literal marker at the start of any line within the chunk
    return text.replaceAll("(?m)^\\[trace] ", "");
  }

  /** Ends the synthetic test process so the SM view finalizes its test tree. */
  void endTestOutput() {
    if (!testOutputSink.isProcessTerminated()) {
      testOutputSink.destroyProcess();
    }
  }

  @Override
  public void attachToProcess(@NotNull ProcessHandler processHandler) {
    // already attached to the sink; the debug framework's attach (the fdb
    // handler, which never broadcasts output) must not rebind the printer
  }

  @Override
  public void clear() {
    testConsole.clear();
  }

  @Override
  public void scrollTo(int offset) {
    testConsole.scrollTo(offset);
  }

  @Override
  public void setOutputPaused(boolean value) {
    testConsole.setOutputPaused(value);
  }

  @Override
  public boolean isOutputPaused() {
    return testConsole.isOutputPaused();
  }

  @Override
  public boolean hasDeferredOutput() {
    return testConsole.hasDeferredOutput();
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    testConsole.performWhenNoDeferredOutput(runnable);
  }

  @Override
  public void setHelpId(@NotNull String helpId) {
    testConsole.setHelpId(helpId);
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    testConsole.addMessageFilter(filter);
  }

  @Override
  public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {
    testConsole.printHyperlink(hyperlinkText, info);
  }

  @Override
  public int getContentSize() {
    return testConsole.getContentSize();
  }

  @Override
  public boolean canPause() {
    return testConsole.canPause();
  }

  @Override
  public AnAction @NotNull [] createConsoleActions() {
    return testConsole.createConsoleActions();
  }

  @Override
  public void allowHeavyFilters() {
    testConsole.allowHeavyFilters();
  }

  @Override
  public JComponent getComponent() {
    return testConsole.getComponent();
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return testConsole.getPreferredFocusableComponent();
  }

  @Override
  public void dispose() {
    endTestOutput();
    Disposer.dispose(testConsole);
  }
}
