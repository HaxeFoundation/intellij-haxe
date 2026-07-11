package com.intellij.plugins.haxe.hashlink;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The stopped thread's call stack, one {@link HashLinkStackFrame} per DAP frame.
 */
final class HashLinkExecutionStack extends XExecutionStack {
  private final List<HashLinkStackFrame> frames;

  HashLinkExecutionStack(HashLinkDebugProcess process, List<StackFrame> dapFrames) {
    super("HashLink");
    this.frames = dapFrames.stream()
      .map(frame -> new HashLinkStackFrame(process, frame))
      .toList();
  }

  @Override
  public @Nullable XStackFrame getTopFrame() {
    return frames.isEmpty() ? null : frames.get(0);
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    if (firstFrameIndex <= frames.size()) {
      container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
    } else {
      container.addStackFrames(List.of(), true);
    }
  }
}
