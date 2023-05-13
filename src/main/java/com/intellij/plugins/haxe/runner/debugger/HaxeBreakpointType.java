/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.debug.FlexBreakpointsHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
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

  public static class HaxeBreakpointTypeProvider implements FlexBreakpointsHandler.BreakpointTypeProvider {
    public Class<? extends XBreakpointType<XLineBreakpoint<XBreakpointProperties>, ?>> getBreakpointTypeClass() {
      return HaxeBreakpointType.class;
    }
  }

  private final XDebuggerEditorsProvider myEditorProvider = new HaxeDebuggerEditorsProvider();

  protected HaxeBreakpointType() {
    super("Haxe", HaxeBundle.message("haxe.break.point.title"));
  }

  public boolean canPutAt(@NotNull final VirtualFile file, final int line, @NotNull Project project) {
    return file.getFileType() == HaxeFileType.INSTANCE;
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
