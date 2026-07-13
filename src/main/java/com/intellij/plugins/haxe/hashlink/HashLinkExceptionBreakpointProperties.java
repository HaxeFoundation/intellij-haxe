package com.intellij.plugins.haxe.hashlink;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent state for a per-class HashLink exception breakpoint: the exception
 * class the debugger should stop on (FQN or simple name). Matched against the
 * thrown value's runtime class and its superclasses on the adapter side.
 */
public class HashLinkExceptionBreakpointProperties
  extends XBreakpointProperties<HashLinkExceptionBreakpointProperties> {

  @Attribute("className")
  public String className;

  @Override
  public @Nullable HashLinkExceptionBreakpointProperties getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull HashLinkExceptionBreakpointProperties state) {
    className = state.className;
  }
}
