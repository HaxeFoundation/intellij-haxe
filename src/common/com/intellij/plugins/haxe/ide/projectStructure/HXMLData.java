/*
 * Copyright 2018 Aleksandr Kuzmenko
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
package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.hxml.psi.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.IElementType;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Collects compiler arguments data from HXML file.
 */
public class HXMLData {
  private List<String> myClassPaths;
  private Set<String> myLibraries;
  @Nullable
  private HaxeTarget myTarget;
  @Nullable
  private String myTargetPath;

  private HXMLData() {
    myClassPaths = new ArrayList<>();
    myLibraries = new HashSet<>();
  }

  /**
   * This method returns separate HXMLData instance for each --next section in hxml file.
   * Recursively handles other hxml files referenced in the current one.
   */
  static public List<HXMLData> load(@Nullable Project project, String cwd, String hxmlPath) throws HXMLDataException {
    HXMLData each = new HXMLData();
    HXMLData current = new HXMLData();
    List<HXMLData> result = new ArrayList<>();

    FileASTNode root = getRootNode(project, getPath(cwd, hxmlPath));
    ASTNode node = root.getFirstChildNode();
    Stack<ASTNode> nodeStack = new Stack<>();
    while(node != null) {
      ASTNode next = node.getTreeNext();
      if(next != null) {
        nodeStack.push(next);
      }

      IElementType type = node.getElementType();

      if(type == HXMLTypes.CLASSPATH) {
        HXMLValue value = node.getPsi(HXMLClasspath.class).getValue();
        if(value != null) {
          current.myClassPaths.add(getPath(cwd, value.getText()));
        }

      } else if(type == HXMLTypes.HXML) {
        String anotherPath = getPath(cwd, node.getPsi(HXMLHxml.class).getText());
        next = getRootNode(project, anotherPath).getFirstChildNode();
        if(next != null) {
          nodeStack.push(next);
        }

      } else if(type == HXMLTypes.LIB) {
        List<HXMLValue> values = node.getPsi(HXMLLib.class).getValueList();
        for(HXMLValue value:values) {
          current.myLibraries.add(value.getText());
        }
      } else if(type == HXMLTypes.PROPERTY) {
        HXMLProperty property = node.getPsi(HXMLProperty.class);
        switch (property.getOption().getText()) {
          case "--each":
            each = current;
            current = each.copy();
            break;
          case "--cwd":
            HXMLValue value = property.getValue();
            if (value != null) {
              cwd = getPath(cwd, value.getText());
            }
            break;
          case "--next":
            result.add(current);
            current = each.copy();
            break;
          default:
            readProperty(current, property, cwd);
        }
      }
      //TODO: implement collection of other data from hxml

      node = nodeStack.isEmpty() ? null : nodeStack.pop();
    }
    result.add(current);
    return result;
  }

  static public List<HXMLData> load(String cwd, String hxmlPath) throws HXMLDataException {
    return load(null, cwd, hxmlPath);
  }

  private static String getPath(String cwd, String path) {
    return Paths.get(path).isAbsolute() ? path : Paths.get(cwd, path).toString();
  }

  private HXMLData copy() {
    HXMLData result = new HXMLData();
    result.myClassPaths.addAll(myClassPaths);
    result.myLibraries.addAll(myLibraries);
    return result;
  }

  static private void readProperty(HXMLData data, HXMLProperty property, String cwd) throws HXMLDataException {
    HXMLOption option = property.getOption();
    HXMLValue valuePsi = property.getValue();
    String value = valuePsi == null ? null : valuePsi.getText();
    switch(option.getText()) {
      case "--interp":
        setTarget(data, HaxeTarget.INTERP, cwd,null);
        break;
      case "-js": case "--js":
        setTarget(data, HaxeTarget.JAVA_SCRIPT, cwd, value);
        break;
      case "-lua": case "--lua":
        setTarget(data, HaxeTarget.LUA, cwd, value);
        break;
      case "-swf": case "--swf":
        setTarget(data, HaxeTarget.FLASH, cwd, value);
        break;
      case "-neko": case "--neko":
        setTarget(data, HaxeTarget.NEKO, cwd, value);
        break;
      case "-php": case "--php":
        setTarget(data, HaxeTarget.PHP, cwd, value);
        break;
      case "-cpp": case "--cpp":
        setTarget(data, HaxeTarget.CPP, cwd, value);
        break;
      case "-cppia": case "--cppia":
        setTarget(data, HaxeTarget.CPPIA, cwd, value);
        break;
      case "-cs": case "--cs":
        setTarget(data, HaxeTarget.CSHARP, cwd, value);
        break;
      case "-java": case "--java":
        setTarget(data, HaxeTarget.JAVA, cwd, value);
        break;
      case "-python": case "--python":
        setTarget(data, HaxeTarget.PYTHON, cwd, value);
        break;
      case "-hl": case "--hl":
        setTarget(data, HaxeTarget.HL, cwd, value);
        break;
    }
  }

  static private void setTarget(HXMLData data, HaxeTarget target, String cwd, @Nullable String targetPath) throws HXMLDataException {
    if(data.myTarget != null) {
      throw new HXMLDataException("Multiple compilation targets.");
    }
    if(targetPath == null && !target.isNoOutput()) {
      throw new HXMLDataException("The compilation target is missing an output path.");
    }
    data.myTarget = target;
    if(targetPath != null) {
      data.myTargetPath = getPath(cwd, targetPath);
    }
  }

  static private FileASTNode getRootNode(Project project, String hxmlPath) throws HXMLDataException {
    VirtualFile path = LocalFileSystem.getInstance().findFileByPath(hxmlPath);
    if(path == null) {
      throw new HXMLDataException(new FileNotFoundException(hxmlPath));
    }
    if(project == null) {
      project = DefaultProjectFactory.getInstance().getDefaultProject();
    }
    PsiFile file = PsiManager.getInstance(project).findFile(path);
    if(file == null) {
      throw new HXMLDataException("PsiFile not available for " + hxmlPath);
    }
    return file.getNode();
  }

  public boolean hasTarget() {
    return myTarget != null;
  }

  @Nullable
  public HaxeTarget getTarget() {
    return myTarget;
  }

  @Nullable
  public String getTargetPath() {
    return myTargetPath;
  }

  public Set<String> getLibraries() {
    return myLibraries;
  }

  static public class HXMLDataException extends Exception {
    public HXMLDataException(String message) {
      super(message);
    }

    public HXMLDataException(Throwable cause) {
      super(cause);
    }
  }
}
