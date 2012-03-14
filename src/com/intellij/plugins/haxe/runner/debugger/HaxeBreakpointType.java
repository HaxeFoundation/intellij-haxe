package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointGroupingRule;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBreakpointType extends XLineBreakpointType<XBreakpointProperties> {
  private final XDebuggerEditorsProvider myEditorProvider = new HaxeDebuggerEditorsProvider();

  protected HaxeBreakpointType() {
    super("haXe", HaxeBundle.message("haxe.break.point.title"));
  }

  public boolean canPutAt(@NotNull final VirtualFile file, final int line, @NotNull Project project) {
    return file.getFileType() == HaxeFileType.HAXE_FILE_TYPE;
  }

  public XBreakpointProperties createBreakpointProperties(@NotNull final VirtualFile file, final int line) {
    return null;
  }

  @Override
  public List<XBreakpointGroupingRule<XLineBreakpoint<XBreakpointProperties>, ?>> getGroupingRules() {
    XBreakpointGroupingRule<XLineBreakpoint<XBreakpointProperties>, ?> byFile = XDebuggerUtil.getInstance().getGroupingByFileRule();
    List<XBreakpointGroupingRule<XLineBreakpoint<XBreakpointProperties>, ?>> rules =
      new ArrayList<XBreakpointGroupingRule<XLineBreakpoint<XBreakpointProperties>, ?>>();
    rules.add(byFile);
    return rules;
  }

  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return myEditorProvider;
  }

  @Override
  public String getBreakpointsDialogHelpTopic() {
    return "reference.dialogs.breakpoints";
  }
}
