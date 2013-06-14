/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.runner.debugger.hxcpp.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.connection.HXCPPResponse;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.io.socketConnection.AbstractResponseToRequestHandler;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HXCPPStackFrame extends XStackFrame {
  private final HXCPPDebugProcess myDebugProcess;
  private final String myFunctionName;
  private final String myFileUrl;
  private final int myLine;
  private final int myId;

  private static final Pattern FRAME_PATTERN = Pattern.compile("(\\d+):FilePos\\(Method\\(([^,]+),([^,]+)\\),([^,]+),(\\d+)\\)");

  public static List<HXCPPStackFrame> parse(@Nullable HXCPPDebugProcess debugProcess, String text) {
    final List<HXCPPStackFrame> result = new ArrayList<HXCPPStackFrame>();
    final Matcher matcher = FRAME_PATTERN.matcher(text);
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      final String className = matcher.group(2);
      final String methodName = matcher.group(3);
      final String fileUrl = VfsUtilCore.pathToUrl(matcher.group(4));
      int line = Integer.parseInt(matcher.group(5)) - 1;
      result.add(new HXCPPStackFrame(debugProcess, index, className + "::" + methodName, fileUrl, line));
    }
    return result;
  }

  protected HXCPPStackFrame(HXCPPDebugProcess debugProcess,
                            int id,
                            String functionName,
                            String fileUrl,
                            int line) {
    myDebugProcess = debugProcess;
    myFunctionName = functionName;
    myFileUrl = fileUrl;
    myLine = line;
    myId = id;
  }

  public String getFileUrl() {
    return myFileUrl;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(myFileUrl);
    if (file == null) {
      final String fileName = VfsUtil.extractFileName(myFileUrl);
      final Project project = myDebugProcess.getSession().getProject();
      Collection<VirtualFile> files =
        FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.moduleScope(myDebugProcess.getModule()));
      if (files.isEmpty()) {
        files = FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.allScope(project));
      }
      file = files.isEmpty() ? null : files.iterator().next();
    }
    return XSourcePositionImpl.create(file, myLine);
  }

  @Override
  public void computeChildren(@NotNull final XCompositeNode node) {
    myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
      @Override
      public boolean processResponse(HXCPPResponse response) {
        myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
          @Override
          public boolean processResponse(HXCPPResponse response) {
            String responseString = response.getResponseString();
            int i = responseString.indexOf('[');
            int j = responseString.indexOf(']');
            final boolean goodFormat = i != -1 && j != -1 && i < j;
            if (!goodFormat) {
              return true;
            }
            final XValueChildrenList childrenList = new XValueChildrenList();
            responseString = responseString.substring(i + 1, j);
            for (String varName : responseString.split(",")) {
              if (!StringUtil.isEmpty(varName)) {
                childrenList.add(varName, new HXCPPValue(myDebugProcess, varName));
              }
            }
            node.addChildren(childrenList, false);
            return true;
          }
        }, "vars");
        return true;
      }
    }, "frame", Integer.toString(myId));
  }

  @Override
  public void customizePresentation(SimpleColoredComponent component) {
    XSourcePosition position = getSourcePosition();
    component.append(myFunctionName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    component.append(" in ", SimpleTextAttributes.REGULAR_ATTRIBUTES);

    if (position != null) {
      component.append(position.getFile().getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      component.append(":" + (position.getLine() + 1), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    else {
      component.append("<file name is not available>", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    component.setIcon(AllIcons.Debugger.StackFrame);
  }
}
