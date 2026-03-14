/*
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.buildsystem.hxml.model;

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.mixin.HxmlReference;
import com.intellij.plugins.haxe.hxml.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.function.Predicate.not;

/**
 * Created by ebishton on 9/8/2017.
 */
@CustomLog
public class HXMLProjectModel {

  static {
    log.setLevel(LogLevel.INFO);
  }
  public static final String SWF_NAME = "-swf";
  public static final String SWF_VERSION = "-swf-version";
  public static final String SWF_HEADER = "-swf-header";
  public static final String SWF_LIB = "-swf-lib";

  public static final String MACRO = "--macro";
  public static final String MAIN_CLASS = "-main";
  public static final String DEBUG = "-debug";

  public static final String LIBRARY = "-lib";
  public static final String CLASSPATH = "-cp";

  public static final String CWD = "--cwd";


  protected PsiFile psiFile;

  public HXMLProjectModel(PsiFile psiFile) {
    this.psiFile = psiFile;
  }

  public static HXMLProjectModel create(Project project, VirtualFile hxmlFile) {
    if (null == project || null == hxmlFile) {
      log.debug("Null project or hxmlFile");
      return null;
    }

    PsiFile psi = PsiManager.getInstance(project).findFile(hxmlFile);
    return new HXMLProjectModel(psi);
  }

  // public Map<String,String> getDefinitions();
  // public List<String> getMacros();
  // public String getMainClass();

   public List<String> getClasspath() {
     List<String> classPaths = new ArrayList<>();;
     List<PsiFile> configFiles = getConfigFiles(psiFile);
     searchAndCollectLClassPaths(configFiles, classPaths);

     return classPaths;

   }



  public List<String> getLibraries() {
    List<String> libraries = new ArrayList<>();;
    List<PsiFile> configFiles = getConfigFiles(psiFile);
    searchAndCollectLibs(configFiles, libraries);

    return libraries;
  }

  private static List<PsiFile> getConfigFiles(PsiFile psiFile) {
    List<PsiFile> configFiles = new ArrayList<>();
    configFiles.add(psiFile);
    configFiles.addAll(getHxmlReferencedFiles(psiFile));
    collectConfigFromReferencesFiles(configFiles);
    return configFiles;
  }

  private static void collectConfigFromReferencesFiles(List<PsiFile> configFiles) {
    for (PsiFile configFile : configFiles) {
      getHxmlReferencedFiles(configFile).stream()
              .filter(not(configFiles::contains))
              .forEach(configFiles::add);
    }
  }

  private static @NotNull List<PsiFile> getHxmlReferencedFiles(PsiFile psiFile) {
    List<HxmlReference> hxmlReferences = UsefulPsiTreeUtil.getChildren(psiFile, HxmlReference.class);
      return hxmlReferences.stream().map(HxmlReference::resolveFileReference).filter(Objects::nonNull).toList();
  }

  private static PsiFile findExtraParamsHxmlFile(PsiFile psiFile) {
    VirtualFile parentDir = psiFile.getVirtualFile().getParent();
    String canonicalPath = parentDir.getCanonicalPath() +"/extraParams.hxml";

    VirtualFileSystem fileSystem = parentDir.getFileSystem();
    VirtualFile resolvedFile = fileSystem.findFileByPath(canonicalPath);
    if(resolvedFile != null) {
      return PsiManager.getInstance(psiFile.getProject()).findFile(resolvedFile);
    }
    return null;
  }

  private static void searchAndCollectLClassPaths(List<PsiFile> configFiles, List<String> classPaths) {
    for (PsiFile configFile : configFiles) {
      List<HXMLClasspath> classpaths = UsefulPsiTreeUtil.getChildren(configFile, HXMLClasspath.class);
      classpaths.stream()
              .map(HXMLClasspath::getValue)
              .filter(Objects::nonNull)
              .map(PsiElement::getText)
              .forEach(classPaths::add);
    }
  }

  private static void searchAndCollectLibs(List<PsiFile> configFiles, List<String> libraries) {
      for (PsiFile configFile : configFiles) {
        List<HXMLLib> libs = UsefulPsiTreeUtil.getChildren(configFile, HXMLLib.class);
        for (HXMLLib lib : libs) {
          lib.getValueList().stream()
                  .map(PsiElement::getText)
                  .filter(Objects::nonNull)
                  .forEach(libraries::add);
        }
      }
    }

  @Nullable
  public String getSwfOutputFileName() {
    return getProperty(SWF_NAME);
  }


  @Nullable
  public String getProperty(String propertyName) {
    List<String> found = getProperties(propertyName);
    if (found != null) {
      if (found.size() == 1) {
        return found.get(0);
      } else if (found.size() > 1) {
        if (log.isDebugEnabled()) {
          StringBuilder msg = new StringBuilder(256);
          msg.append("Unexpectedly found more than one setting for ");
          msg.append(propertyName);
          msg.append(":\n");
          for (String s : found) {
            msg.append("   ");
            msg.append(s);
            msg.append('\n');
          }
          log.info(msg.toString());
        }
        return found.get(0);
      }
    }
    return null;
  }


  @Nullable
  public List<String> getProperties(@NotNull String propertyName) {
    if (propertyName.isEmpty()) {
      return null;
    }

    List<String> found = null;
    HXMLProperty[] properties = UsefulPsiTreeUtil.getChildrenOfType(psiFile, HXMLProperty.class, null);
    if (null != properties) {
      for (HXMLProperty foundProperty : properties) {
        HXMLOption option = foundProperty.getOption();
        if (null != option && propertyName.equals(option.getText())) {
          HXMLValue val = foundProperty.getValue();
          if (val != null) {
            if (found == null) {
              found = new ArrayList<String>();
            }
            found.add(val.getText());
          }
        }
      }
    }
    return found;
  }

  public String getWorkingDirectory() {
    return getProperty(CWD);
  }
}
