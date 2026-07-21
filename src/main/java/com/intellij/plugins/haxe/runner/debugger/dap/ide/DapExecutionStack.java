package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import java.util.List;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One thread's call stack. The active (stopped) thread is built eagerly from the
 * frames already fetched at the stop; other threads compute their frames lazily
 * (a stackTrace request for that thread) only if the user selects them.
 */
final class DapExecutionStack extends XExecutionStack {
  private final DapDebugProcess process;
  private final int threadId;
  private final @Nullable List<DapStackFrame> eagerFrames;
  // non-null when this thread stopped on a thrown exception: the value's text,
  // used for the gutter marker + tooltip at the throw line
  private final @Nullable String exceptionText;

  DapExecutionStack(DapDebugProcess process, int threadId, String displayName,
                      @Nullable List<StackFrame> activeFrames, @Nullable String exceptionText) {
    super(displayName, AllIcons.Debugger.ThreadCurrent);
    this.process = process;
    this.threadId = threadId;
    this.eagerFrames = activeFrames == null ? null : toFrames(activeFrames);
    this.exceptionText = exceptionText;
  }

  // When stopped on an exception, mark the throw line in the gutter with the
  // exception glyph (tooltip = the thrown value); otherwise the default.
  @Override
  public @Nullable GutterIconRenderer getExecutionLineIconRenderer() {
    return exceptionText == null ? null : new ExceptionGutterIconRenderer(exceptionText);
  }

  private static final class ExceptionGutterIconRenderer extends GutterIconRenderer {
    private final String tooltip;

    ExceptionGutterIconRenderer(String tooltip) {
      this.tooltip = tooltip;
    }

    @Override
    public @NotNull Icon getIcon() {
      return AllIcons.Debugger.Db_exception_breakpoint;
    }

    @Override
    public @Nullable String getTooltipText() {
      return tooltip;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ExceptionGutterIconRenderer other && tooltip.equals(other.tooltip);
    }

    @Override
    public int hashCode() {
      return tooltip.hashCode();
    }
  }

  private List<DapStackFrame> toFrames(List<StackFrame> dapFrames) {
    return dapFrames.stream().map(frame -> new DapStackFrame(process, frame)).toList();
  }

  @Override
  public @Nullable XStackFrame getTopFrame() {
    // only the active thread has eager frames; others resolve via computeStackFrames
    return eagerFrames == null || eagerFrames.isEmpty() ? null : eagerFrames.get(0);
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    if (eagerFrames != null) {
      addFrom(eagerFrames, firstFrameIndex, container);
      return;
    }
    // a non-active thread the user selected: fetch its stack off the EDT
    process.onRequestThread(() -> {
      List<DapStackFrame> frames = toFrames(process.requestStackTrace(threadId));
      addFrom(frames, firstFrameIndex, container);
    }, () -> container.addStackFrames(List.of(), true));
  }

  private static void addFrom(List<DapStackFrame> frames, int firstFrameIndex,
                              XStackFrameContainer container) {
    if (firstFrameIndex <= frames.size()) {
      List<DapStackFrame> visible = frames.subList(firstFrameIndex, frames.size());
      // resolve source positions HERE (computeStackFrames contract: background
      // thread), so selecting any frame in the panel only reads the cache and
      // never runs the resolver's index lookups on the EDT
      for (DapStackFrame frame : visible) {
        frame.getSourcePosition();
      }
      container.addStackFrames(visible, true);
    } else {
      container.addStackFrames(List.of(), true);
    }
  }
}
