package com.intellij.plugins.haxe.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One thread's call stack. The active (stopped) thread is built eagerly from the
 * frames already fetched at the stop; other threads compute their frames lazily
 * (a stackTrace request for that thread) only if the user selects them. All
 * threads are frozen at the stop, so any thread's stack is walkable.
 */
final class HashLinkExecutionStack extends XExecutionStack {
  private final HashLinkDebugProcess process;
  private final int threadId;
  private final @Nullable List<HashLinkStackFrame> eagerFrames;

  HashLinkExecutionStack(HashLinkDebugProcess process, int threadId, String displayName,
                         @Nullable List<StackFrame> activeFrames) {
    super(displayName, AllIcons.Debugger.ThreadCurrent);
    this.process = process;
    this.threadId = threadId;
    this.eagerFrames = activeFrames == null ? null : toFrames(activeFrames);
  }

  private List<HashLinkStackFrame> toFrames(List<StackFrame> dapFrames) {
    return dapFrames.stream().map(frame -> new HashLinkStackFrame(process, frame)).toList();
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
      List<HashLinkStackFrame> frames = toFrames(process.requestStackTrace(threadId));
      addFrom(frames, firstFrameIndex, container);
    });
  }

  private static void addFrom(List<HashLinkStackFrame> frames, int firstFrameIndex,
                              XStackFrameContainer container) {
    if (firstFrameIndex <= frames.size()) {
      container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
    } else {
      container.addStackFrames(List.of(), true);
    }
  }
}
