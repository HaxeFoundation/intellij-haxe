package com.intellij.plugins.haxe.runner.debugger.exceptions;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent state of a Haxe exception breakpoint.
 *
 * The default "Any exception" breakpoint has no {@link #className}; its three
 * notification flags select what stops the debugger (each debug process maps
 * them onto its own wire filters). A user-added per-class breakpoint carries
 * the exception class (FQN or simple name) and means "stop when this class or
 * a subclass is thrown, caught or not" — the flags are not consulted for it
 * (neither runtime can type-filter an uncaught or critical stop).
 */
public class HaxeExceptionBreakpointProperties
  extends XBreakpointProperties<HaxeExceptionBreakpointProperties> {

  @Attribute("className")
  public String className;

  /** Stop where an exception is thrown even though it will be caught. */
  @Attribute("notifyCaught")
  public boolean notifyCaught = false;

  /** Stop at a throw no live try/catch will handle (before unwinding). */
  @Attribute("notifyUncaught")
  public boolean notifyUncaught = true;

  /** Stop on runtime-raised critical errors (null access, out-of-bounds, GC). */
  @Attribute("notifyCritical")
  public boolean notifyCritical = true;

  public boolean isTyped() {
    return className != null && !className.isBlank();
  }

  @Override
  public @Nullable HaxeExceptionBreakpointProperties getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull HaxeExceptionBreakpointProperties state) {
    className = state.className;
    notifyCaught = state.notifyCaught;
    notifyUncaught = state.notifyUncaught;
    notifyCritical = state.notifyCritical;
  }
}
