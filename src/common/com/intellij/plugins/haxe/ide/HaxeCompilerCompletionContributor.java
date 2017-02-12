/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.ide;

import com.google.common.base.Joiner;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import com.intellij.plugins.haxe.compilation.HaxeCompilerProjectCache;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.LineSeparator;
import com.intellij.util.ProcessingContext;
import com.intellij.util.text.StringTokenizer;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 25.11.14.
 */
public class HaxeCompilerCompletionContributor extends CompletionContributor {

  static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#HaxeCompilerCompletionContributor");

  // Take this out when finished debugging.
  static {
    LOG.setLevel(org.apache.log4j.Level.DEBUG);
  }

  String myErrorMessage = null;
  Project myProject;

  // Cache to keep the openFL display args (read from the project.xml).
  static HaxeCompilerProjectCache openFLDisplayArguments = new HaxeCompilerProjectCache();
  static final Pattern EMPTY_LINE_REGEX = Pattern.compile( "^\\s+$" );


  public HaxeCompilerCompletionContributor() {
    //Trigger completion only on HaxeReferenceExpressions
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HaxeTokenTypes.ID)
             .withParent(HaxeIdentifier.class)
             .withSuperParent(2, HaxeReferenceExpression.class),

           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("HaxeCompilerCompletionContributor",
                                                                    HaxeDebugTimeLog.Since.StartAndPrevious);

               try {
                 myErrorMessage = null;
                 PsiFile file = parameters.getOriginalFile();
                 myProject = file.getProject();
                 Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), myProject);

                 ArrayList<String> commandLineArguments = new ArrayList<String>();


                 //Make sure module is Haxe Module
                 if (moduleForFile != null
                     && ModuleUtil.getModuleType(moduleForFile).equals(HaxeModuleType.getInstance())) {
                   //Get module settings
                   HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(moduleForFile);
                   int buildConfig = moduleSettings.getBuildConfig();
                   VirtualFile projectFile = null;
                   switch (buildConfig) {
                     case HaxeModuleSettings.USE_HXML:
                       projectFile = verifyProjectFile(moduleForFile, "HXML", moduleSettings.getHxmlPath());
                       if (null == projectFile) {
                         break;
                       }

                       commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                       commandLineArguments.add(projectFile.getPath());

                       collectCompletionsFromCompiler(parameters, result, commandLineArguments, timeLog);
                       break;
                     case HaxeModuleSettings.USE_NMML:
                       projectFile = verifyProjectFile(moduleForFile, "NMML", moduleSettings.getNmmlPath());
                       if (null == projectFile) {
                         break;
                       }
                       formatAndAddCompilerArguments(commandLineArguments, moduleSettings.getNmeFlags());
                       collectCompletionsFromNME(parameters, result, commandLineArguments, timeLog);
                       break;
                     case HaxeModuleSettingsBaseImpl.USE_OPENFL:
                       projectFile = verifyProjectFile(moduleForFile, "OpenFL", moduleSettings.getOpenFLPath());
                       if (null == projectFile) {
                         break;
                       }

                       String targetFlag = moduleSettings.getOpenFLTarget().getTargetFlag();
                       // Load the project defines, etc, from the project.xml file.  Cache them.
                       List<String> compilerArgsFromProjectFile = openFLDisplayArguments.get(moduleForFile, projectFile.getUrl(), targetFlag);
                       if (compilerArgsFromProjectFile == null) {
                         ArrayList<String> limeArguments = new ArrayList<String>();

                         limeArguments.add(HaxelibCommandUtils.getHaxelibPath(moduleForFile));
                         limeArguments.add("run");
                         limeArguments.add("lime");
                         limeArguments.add("display");
                         //flash, html5, linux, etc
                         limeArguments.add(targetFlag);

                         // Add arguments from the settings panel.  They get echoed out via display,
                         // if appropriate.
                         formatAndAddCompilerArguments(limeArguments, moduleSettings.getOpenFLFlags());

                         timeLog.stamp("Get display vars from lime.");
                         List<String> stdout = HaxelibCommandUtils.getProcessStdout(limeArguments,
                                                                                    BuildProperties.getProjectBaseDir(myProject),
                                                                                    HaxeSdkUtilBase.getSdkData(moduleForFile));

                         // Need to filter out empty/blank lines.  They cause an empty argument to
                         // haxelib, which errors out and breaks completion.
                         compilerArgsFromProjectFile = filterEmptyLines(stdout);
                         openFLDisplayArguments.put(moduleForFile, projectFile.getUrl(), targetFlag, compilerArgsFromProjectFile);
                       }

                       commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                       formatAndAddCompilerArguments(commandLineArguments, compilerArgsFromProjectFile);

                       collectCompletionsFromCompiler(parameters, result, commandLineArguments, timeLog);

                       break;
                     case HaxeModuleSettingsBaseImpl.USE_PROPERTIES:
                       commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                       formatAndAddCompilerArguments(commandLineArguments, moduleSettings.getArguments());
                       collectCompletionsFromCompiler(parameters, result, commandLineArguments, timeLog);
                       break;
                   }
                 }
               }
               finally {
                 timeLog.stamp("Finished");
                 timeLog.print();
               }
             }
           });
  }

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    saveEditsToDisk(context.getFile().getVirtualFile());
    super.beforeCompletion(context);
  }

  @Nullable
  @Override
  public String handleEmptyLookup(@NotNull CompletionParameters parameters, Editor editor) {
    if (null != myErrorMessage) {
      return myErrorMessage;
    }
    return super.handleEmptyLookup(parameters, editor);
  }

  private List<String> filterEmptyLines(List<String> lines) {
    List<String> filtered = new ArrayList<String>(lines.size());
    for (String l : lines) {
      if ( ! (l.isEmpty() || EMPTY_LINE_REGEX.matcher(l).matches())) {
        filtered.add(l);
      }
    }
    return filtered;
  }

  private void advertiseError(String message) {
    myErrorMessage = message;
    LOG.info(message);  // XXX - May happen to often.
    StatusBarUtil.setStatusBarInfo(myProject, message);
  }

  /**
   * Verify that the Haxe project file (.xml, .nmml, etc.; NOT an IDEA project file)
   * specified in the build settings is available.  Put up an error message if it's not.
   */
  private VirtualFile verifyProjectFile(@NotNull Module module, @NotNull String type, @NotNull String path) {
    if (path.isEmpty()) {
      advertiseError("Completion error: No " + type + " project type is specified in project settings.");  // TODO: Externalize string.
      return null;
    }

    // Look up the project file.
    // XXX Might want to use CoreLocalFileSystem instead?
    VirtualFile file = LocalFileFinder.findFile(path);

    // If we didn't find it, check the module content roots for it.
    if (null == file) {
      ModuleRootManager mgr = ModuleRootManager.getInstance(module);
      for (VirtualFile contentRoot : mgr.getContentRoots()) {
        file = contentRoot.findFileByRelativePath(path);
        if (null != file) {
          break;
        }
      }
    }

    // If that didn't work, then try the directory the module file (.iml) is in.
    if (null == file) {
      VirtualFile moduleFile = module.getModuleFile();
      VirtualFile moduleDir = null != moduleFile ? moduleFile.getParent() : null;
      file = null != moduleDir ? moduleDir.findFileByRelativePath(path) : null;
    }


    // Still no luck? Try the project root (actually, the directory where the .prj file is).
    if (null == file) {
      VirtualFile projectDirectory = myProject.getBaseDir();
      file = projectDirectory != null ? projectDirectory.findFileByRelativePath(path) : null;
    }

    if (null == file) {
      String message = "Completion error: " + type + " project file does not exist: " + path;  // TODO: Externalize string.
      advertiseError(message);
    }
    return file;

  }

  private void formatAndAddCompilerArguments(ArrayList<String> commandLineArguments, List<String> stdout) {
    for (int i = 0; i < stdout.size(); i++) {
      String s = stdout.get(i).trim();
      if (!s.startsWith("#")) {
        commandLineArguments.addAll(Arrays.asList(s.split(" ")));
      }
    }
  }

  private void formatAndAddCompilerArguments(ArrayList<String> commandLineArguments, String flags) {
    if (null != flags && !flags.isEmpty()) {
      final StringTokenizer flagsTokenizer = new StringTokenizer(flags);
      while (flagsTokenizer.hasMoreTokens()) {
        String nextToken = flagsTokenizer.nextToken();
        if (!nextToken.isEmpty()) {
          commandLineArguments.add(nextToken);
        }
      }
    }
  }

  private int recalculateFileOffset(@NotNull CompletionParameters parameters) {
    PsiElement position = parameters.getPosition();
    int offset = position.getTextOffset();;

    // Get the separator, checking the file if we don't know yet.  May still return null.
    String separator = LoadTextUtil.detectLineSeparator(parameters.getOriginalFile().getVirtualFile(), true);

    // IntelliJ IDEA normalizes file line endings, so if file line endings is
    // CRLF - then we have to shift an offset so Haxe compiler could get proper offset
    if (LineSeparator.CRLF.getSeparatorString().equals(separator)) {
      int lineNumber =
        com.intellij.openapi.util.text.StringUtil.offsetToLineNumber(parameters.getEditor().getDocument().getText(), offset);
      offset += lineNumber;
    }
    return offset;
  }

  /**
   * Save the file contents to disk so that the compiler has the correct data to work with.
   */
  private void saveEditsToDisk(VirtualFile file) {
    // TODO: Don't save if we can use the compiler stdin or compiler completion isn't turned off.
    // TODO: Add checkbox/setting to turn off compiler completion.
    final Document doc = FileDocumentManager.getInstance().getDocument(file);
    if (FileDocumentManager.getInstance().isDocumentUnsaved(doc)) {
      final Application application = ApplicationManager.getApplication();
      if (application.isDispatchThread()) {
        LOG.debug("Saving buffer to disk...");
        FileDocumentManager.getInstance().saveDocumentAsIs(doc);
      } else {
        LOG.debug("Not on dispatch thread and document is unsaved.");
      }
    }
  }

  private void collectCompletionsFromCompiler(@NotNull CompletionParameters parameters,
                                              @NotNull CompletionResultSet result,
                                              ArrayList<String> commandLineArguments,
                                              HaxeDebugTimeLog timeLog) {
    // There is a problem here in that the current buffer may not have been saved.
    // If that is the case, then the position is also incorrect, and the compiler
    // doesn't have access to the correct sources.  If the haxe compiler is version 3.4 or
    // later, then it has the -D display-stdin parameter available and we can pump the
    // unsaved buffer through to the compiler. (Though that does nothing for completion
    // from related but also unsaved buffers.)  Doing so will also require the compiler
    // server (if used) to be started with the "--wait stdin" parameter.

    PsiFile file = parameters.getOriginalFile();
    Project project = file.getProject();
    Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), project);
    int offset = recalculateFileOffset(parameters);

    // Tell the compiler we want field completion, adding the type (var or method)
    commandLineArguments.add("-D");
    commandLineArguments.add("display-details");
    commandLineArguments.add("--display");

    commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

    timeLog.stamp("Calling compiler");
    List<String> stderr =
    HaxelibCommandUtils.getProcessStderr(commandLineArguments,
                                         BuildProperties.getProjectBaseDir(project),
                                         HaxeSdkUtilBase.getSdkData(moduleForFile));
    timeLog.stamp("Compiler finished");
    parseCompletionFromXml(result, project, stderr);
  }

  private void collectCompletionsFromNME(@NotNull CompletionParameters parameters,
                                         @NotNull CompletionResultSet result,
                                         ArrayList<String> commandLineArguments,
                                         HaxeDebugTimeLog timeLog) {
    // See note on collectCompletionsFromCompiler.

    PsiFile file = parameters.getOriginalFile();
    Project project = file.getProject();
    Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), project);
    int offset = recalculateFileOffset(parameters);

    HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(moduleForFile);
    String nmmlPath = moduleSettings.getNmmlPath();
    String nmeTarget = moduleSettings.getNmeTarget().getTargetFlag();

    // To get completions out of the haxe compiler when NME calls it, we have to
    // add <haxeflag /> nodes to the project.nmml.  We'll do that by creating a
    // temporary NMML file and including the real project file.
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    xml.append("<project>");

    xml.append("<include path=\"");
    xml.append(nmmlPath);
    xml.append("\" />");

    xml.append("<haxeflag name=\"--display\" value=\"");
    xml.append(file.getVirtualFile().getPath());
    xml.append("@");
    xml.append(Integer.toString(offset));
    xml.append("\" />");

    xml.append("<haxeflag name=\"-D\" value=\"display-details\" />");
    xml.append("</project>");

    File tempFile = null;
    try {
      tempFile = File.createTempFile("HaxeProjectWrapper", ".nmml");
      FileWriter writer = new FileWriter(tempFile);
      writer.write(xml.toString());
      writer.close();

      commandLineArguments.add(HaxelibCommandUtils.getHaxelibPath(moduleForFile));
      commandLineArguments.add("run");
      commandLineArguments.add("nme");
      commandLineArguments.add("build");
      commandLineArguments.add(tempFile.getPath());
      commandLineArguments.add(nmeTarget);

      timeLog.stamp("Calling NME");
      List<String> stderr =
        HaxelibCommandUtils.getProcessStderr(commandLineArguments,
                                             BuildProperties.getProjectBaseDir(project),
                                             HaxeSdkUtilBase.getSdkData(moduleForFile));
      timeLog.stamp("NME finished");
      parseCompletionFromXml(result, project, stderr);
    } catch (IOException e) {
      advertiseError("Completion failed: Could not create temporary file."); // TODO: Externalize string.
    } finally {
      if (tempFile != null) {
        tempFile.delete();
      }
    }


  }

  private void parseCompletionFromXml(CompletionResultSet result, Project project, List<String> stderr) {
    try {
      if (!stderr.isEmpty() && stderr.get(0).contains("<list>") && stderr.size() > 1) {
        String s = Joiner.on("").join(stderr);
        PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText("data.xml", XmlFileType.INSTANCE, s);

        XmlFile xmlFile = (XmlFile)fileFromText;
        XmlDocument document = xmlFile.getDocument();

        if (document != null) {
          XmlTag rootTag = document.getRootTag();
          if (rootTag != null) {
            XmlTag[] xmlTags = rootTag.findSubTags("i");
            for (XmlTag xmlTag : xmlTags) {
              XmlAttribute nAttr = xmlTag.getAttribute("n");
              XmlAttribute kAttr = xmlTag.getAttribute("k");
              String n = null == nAttr ? null : nAttr.getValue();
              String k = null == kAttr ? null : kAttr.getValue();
              XmlTag t = xmlTag.findFirstSubTag("t");
              XmlTag d = xmlTag.findFirstSubTag("d");

              LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(n);
              if (k != null) {
                lookupElementBuilder = lookupElementBuilder.withIcon(
                  k.equals("var") ? HaxeIcons.Field_Haxe : HaxeIcons.Method_Haxe);
              }

              String formattedType = "";
              String formattedDescription = "";

              if (t != null) {
                formattedType = getFormattedText(t.getValue().getText());
                HaxeCompilerCompletionItem item = parseFunctionParams(formattedType);
                String text = "";

                if (item.parameters != null) {
                  String presentableText = n + "(" + Joiner.on(", ").join(item.parameters) + "):" + item.retType;
                  lookupElementBuilder = lookupElementBuilder.withPresentableText(presentableText);
                }
                else {
                  text = formattedType;
                }

                if (d != null) {
                  String text1 = d.getValue().getText();
                  text1 = getFormattedText(text1);
                  formattedDescription = text1;
                  text += " " + formattedDescription;
                }

                lookupElementBuilder = lookupElementBuilder.withTailText(" " + text, true);
              }
              result.addElement(lookupElementBuilder);
            }
          }
        }
      }
      else {
        if (!stderr.isEmpty()) {
          LOG.debug(stderr.toString());
          // Could be a syntax warning.
          HaxeCompilerError compilerError = HaxeCompilerError.create(
            project.getBaseDir().getPath(),
            stderr.get(0));
          StringBuilder msg = new StringBuilder();
          msg.append("Compiler completion");        // TODO: Externalize string.
          if (compilerError.isErrorMessage()) {
            msg.append(" error");                   // TODO: Externalize and don't build the string.
          }
          msg.append(": ");
          msg.append(compilerError.getErrorMessage());

          String smsg = msg.toString();
          result.addLookupAdvertisement(smsg);
          advertiseError(smsg);
        }
      }
    }
    catch (ProcessCanceledException e) {
      advertiseError("Haxe compiler completion canceled.");  // TODO: Externalize string.
      LOG.debug("Haxe compiler completion canceled.", e);
      throw e;
    }
  }

  private String getFormattedText(String text1) {
    text1 = text1.replaceAll("\t", "");
    text1 = text1.replaceAll("\n", "");
    text1 = text1.replaceAll("&lt;", "<");
    text1 = text1.replaceAll("&gt;", ">");
    text1 = text1.trim();
    return text1;
  }

  //Ported from HIDE
  //https://github.com/HaxeIDE/HIDE/blob/master/src/core/FunctionParametersHelper.hx#L193
  public HaxeCompilerCompletionItem parseFunctionParams(String type)
  {
    List<String> parameters = null;
    String retType = null;
    if (type != null && type.indexOf("->") != -1)
    {
      int openBracketsCount = 0;
      List<Integer> startPositions = new ArrayList<Integer>();
      List<Integer> endPositions = new ArrayList<Integer>();
      int i = 0;
      int lastPos = 0;
      while (i < type.length())
      {
        switch (type.charAt(i))
        {
          case '-':
            if (openBracketsCount == 0 && type.charAt(i + 1) == '>') {
              startPositions.add(lastPos);
              endPositions.add(i - 1);
              i++;
              i++;
              lastPos = i;
            }
          case '(':
            openBracketsCount++;
          case ')':
            openBracketsCount--;
          default:
        }
        i++;
      }
      startPositions.add(lastPos);
      endPositions.add(type.length());
      parameters = new ArrayList<String>();

      for (int j = 0; j < startPositions.size(); j++) {
        String param = type.substring(startPositions.get(j), endPositions.get(j)).trim();
        if (j < startPositions.size() - 1)
        {
          int pos = param.indexOf(" : ", 0);
          if (pos > -1) {
            StringBuilder unspaced = new StringBuilder();
            unspaced.append(param.substring(0, pos));
            unspaced.append(":");
            unspaced.append(param.substring(pos+3));
            param = unspaced.toString();
          }
          parameters.add(param);
        }
        else
        {
          retType = param;
        }
      }
      if (parameters.size() == 1 && parameters.get(0) == "Void")
      {
        parameters.clear();
      }
    }
    return new HaxeCompilerCompletionItem(parameters, retType);
  }
}
