package com.intellij.plugins.haxe.runner.debugger.exceptions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single "Haxe Exception Breakpoints" category, shared by the HashLink
 * and HXCPP debuggers and modelled on the Java debugger's:
 *
 * <ul>
 *   <li>the DEFAULT breakpoint is "Any exception", whose Notifications panel
 *       ({@link HaxeExceptionBreakpointPropertiesPanel}) selects caught /
 *       uncaught / critical-error stops;</li>
 *   <li>the "+" button adds per-class breakpoints ("Exception: my.ConfigError")
 *       that stop when that class or a subclass is thrown, caught or not.</li>
 * </ul>
 *
 * Each debug process maps the enabled breakpoints onto its own wire filters
 * (HashLink: all/uncaught/vm + filterTypes; HXCPP: thrown/uncaught/critical +
 * filterTypes). Per-target nuance: on HXCPP, caught-throw and per-class stops
 * see the {@code haxe.Exception} hierarchy only — raw-value throws
 * ({@code throw "str"}) are invisible until uncaught.
 */
public class HaxeExceptionBreakpointType
  extends XBreakpointType<XBreakpoint<HaxeExceptionBreakpointProperties>, HaxeExceptionBreakpointProperties> {

  public HaxeExceptionBreakpointType() {
    super("haxe-exception", "Haxe Exception Breakpoints");
  }

  @Override
  public @NotNull String getDisplayText(XBreakpoint<HaxeExceptionBreakpointProperties> breakpoint) {
    HaxeExceptionBreakpointProperties properties = breakpoint.getProperties();
    return properties != null && properties.isTyped()
           ? "Exception: " + properties.className
           : "Any exception";
  }

  @Override
  public @Nullable HaxeExceptionBreakpointProperties createProperties() {
    return new HaxeExceptionBreakpointProperties();
  }

  // the lightning-bolt exception icons, like the Java debugger's
  @Override
  public @NotNull Icon getEnabledIcon() {
    return AllIcons.Debugger.Db_exception_breakpoint;
  }

  @Override
  public @NotNull Icon getDisabledIcon() {
    return AllIcons.Debugger.Db_disabled_exception_breakpoint;
  }

  @Override
  public @Nullable XBreakpointCustomPropertiesPanel<XBreakpoint<HaxeExceptionBreakpointProperties>> createCustomPropertiesPanel(
    @NotNull Project project) {
    return new HaxeExceptionBreakpointPropertiesPanel();
  }

  /** The undeletable "Any exception" entry every project gets. */
  @Override
  public XBreakpoint<HaxeExceptionBreakpointProperties> createDefaultBreakpoint(
    @NotNull XBreakpointCreator<HaxeExceptionBreakpointProperties> creator) {
    // INTENTIONALLY enabled by default (unlike Java's "Any exception"):
    // uncaught + critical stops on, caught off — see the properties' field defaults
    return creator.createBreakpoint(new HaxeExceptionBreakpointProperties());
  }

  @Override
  public boolean isAddBreakpointButtonVisible() {
    return true;
  }

  /**
   * Builds the {@code setExceptionBreakpoints} request from the CURRENT state
   * of the exception breakpoints in the breakpoint manager (the single source
   * of truth): a session started with breakpoints already enabled (e.g. after
   * an IDE restart) arms them the same way as a live toggle. The "Any
   * exception" breakpoint's Notifications checkboxes map onto the given wire
   * words — each backend speaks its own vocabulary for the same three
   * meanings — and per-class breakpoints go as filterTypes, matched
   * debuggee-side against the thrown value's class chain.
   */
  public static SetExceptionBreakpointsRequest buildFiltersRequest(@NotNull Project project,
                                                                   @NotNull String caughtWord,
                                                                   @NotNull String uncaughtWord,
                                                                   @NotNull String criticalWord) {
    List<String> filters = new ArrayList<>();
    List<String> filterTypes = new ArrayList<>();
    ReadAction.nonBlocking(() -> collectEnabled(project, properties -> {
      if (properties.isTyped()) {
        filterTypes.add(properties.className.trim());
      } else {
        if (properties.notifyCaught) {
          filters.add(caughtWord);
        }
        if (properties.notifyUncaught) {
          filters.add(uncaughtWord);
        }
        if (properties.notifyCritical) {
          filters.add(criticalWord);
        }
      }
    })).executeSynchronously();
    SetExceptionBreakpointsRequest request = new SetExceptionBreakpointsRequest();
    SetExceptionBreakpointsArguments arguments = new SetExceptionBreakpointsArguments();
    arguments.setFilters(filters);
    arguments.setFilterTypes(filterTypes);
    request.setArguments(arguments);
    return request;
  }

  /**
   * Runs `consumer` for every ENABLED Haxe exception breakpoint's properties —
   * the one loop the wire filters are built from. Call inside a read action.
   */
  public static void collectEnabled(@NotNull Project project,
                                    @NotNull Consumer<HaxeExceptionBreakpointProperties> consumer) {
    XBreakpointType<?, ?> type = XDebuggerUtil.getInstance()
      .findBreakpointType(HaxeExceptionBreakpointType.class);
    if (type == null) {
      return;
    }
    XBreakpointManager manager = XDebuggerManager.getInstance(project).getBreakpointManager();
    for (XBreakpoint<?> breakpoint : manager.getBreakpoints(type)) {
      if (breakpoint.isEnabled()
          && breakpoint.getProperties() instanceof HaxeExceptionBreakpointProperties properties) {
        consumer.accept(properties);
      }
    }
  }

  @Override
  public @Nullable XBreakpoint<HaxeExceptionBreakpointProperties> addBreakpoint(Project project, JComponent parentComponent) {
    String input = Messages.showInputDialog(project,
                                            "Exception class (fully-qualified or simple name):",
                                            "Add Haxe Exception Breakpoint",
                                            Messages.getQuestionIcon());
    if (input == null || input.isBlank()) {
      return null;
    }
    String className = input.trim();
    return WriteAction.compute(() -> {
      XBreakpointManager manager = XDebuggerManager.getInstance(project).getBreakpointManager();
      HaxeExceptionBreakpointProperties properties = new HaxeExceptionBreakpointProperties();
      properties.className = className;
      return manager.addBreakpoint(this, properties);
    });
  }
}
