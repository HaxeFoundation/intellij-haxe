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
package com.intellij.plugins.haxe.runner.debugger.hxcpp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.runner.debugger.HaxeBreakpointType;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.connection.HXCPPResponse;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.io.socketConnection.AbstractResponseToRequestHandler;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPBreakpointsHandler {
  private final HXCPPDebugProcess myDebugProcess;
  private XBreakpointHandler<?>[] myBreakpointHandlers;
  private final Map<XLineBreakpoint<XBreakpointProperties>, Integer> myBreakpointToIndexMap =
    new THashMap<XLineBreakpoint<XBreakpointProperties>, Integer>();
  private final Map<Integer, XLineBreakpoint<XBreakpointProperties>> myIndexToBreakpointMap =
    new THashMap<Integer, XLineBreakpoint<XBreakpointProperties>>();
  int id = 0;

  public HXCPPBreakpointsHandler(HXCPPDebugProcess process) {
    myDebugProcess = process;

    myBreakpointHandlers = new XBreakpointHandler<?>[]{
      new XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>>(HaxeBreakpointType.class) {
        public void registerBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint) {
          final XSourcePosition position = breakpoint.getSourcePosition();
          if (position != null) {
            final String path =
              getRelativePath(myDebugProcess.getSession().getProject(), position.getFile());
            myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
              @Override
              public boolean processResponse(HXCPPResponse response) {
                if (!response.getResponseString().contains("ok")) {
                  myDebugProcess.getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint, null);
                  myDebugProcess.getSession().reportError("Cannot set exception breakpoint: " + response.getResponseString());
                  return true;
                }
                myBreakpointToIndexMap.put(breakpoint, id);
                myIndexToBreakpointMap.put(id, breakpoint);
                ++id;
                return true;
              }
            }, "break", path, Integer.toString(position.getLine() + 1));
          }
        }

        public void unregisterBreakpoint(@NotNull final XLineBreakpoint<XBreakpointProperties> breakpoint, final boolean temporary) {
          final Integer id = myBreakpointToIndexMap.remove(breakpoint);
          if (id != null) {
            myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
              @Override
              public boolean processResponse(HXCPPResponse response) {
                if (myIndexToBreakpointMap.containsKey(id)) {
                  myIndexToBreakpointMap.remove(id);
                }
                return true;
              }
            }, "delete", Integer.toString(id));
          }
        }
      }
    };
  }

  private static String getRelativePath(Project project, VirtualFile file) {
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    final String packageName = HaxeResolveUtil.getPackageName(psiFile);
    final String fileName = VfsUtil.extractFileName(file.getPath());
    return getPath(packageName, fileName);
  }

  private static String getPath(String packageName, String fileName) {
    if (StringUtil.isEmpty(packageName)) return fileName;
    return packageName.replaceAll("\\.", "/") + "/" + fileName;
  }

  public XLineBreakpoint<XBreakpointProperties> getBreakpointById(int id) {
    return myIndexToBreakpointMap.get(id);
  }

  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return myBreakpointHandlers;
  }
}
