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
package com.intellij.plugins.haxe.ide;

import com.google.common.base.Joiner;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.LineSeparator;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by as3boyan on 25.11.14.
 */
public class HaxeCompilerCompletionContributor extends CompletionContributor {
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
               ArrayList<String> commandLineArguments = new ArrayList<String>();

               PsiFile file = parameters.getOriginalFile();

               PsiElement position = parameters.getPosition();
               int parent = position.getTextOffset();
               int offset = parent;

               String separator = FileDocumentManagerImpl.getLineSeparator(parameters.getEditor().getDocument(), file.getVirtualFile());

               //IntelliJ IDEA normalizes file line endings, so if file line endings is CRLF - then we have to shift an offset so Haxe compiler could get proper offset
               if (LineSeparator.CRLF.getSeparatorString().equals(separator)) {
                 int lineNumber =
                   com.intellij.openapi.util.text.StringUtil.offsetToLineNumber(parameters.getEditor().getDocument().getText(), offset);
                 offset += lineNumber;
               }

               Project project = file.getProject();
               Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), project);

               if (moduleForFile != null) {
                 //Make sure module is Haxe Module
                 if (ModuleUtil.getModuleType(moduleForFile).equals(HaxeModuleType.getInstance())) {
                   //Get module settings
                   HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(moduleForFile);
                   int buildConfig = moduleSettings.getBuildConfig();
                   switch (buildConfig) {
                     case HaxeModuleSettings.USE_HXML:
                       String hxmlPath = moduleSettings.getHxmlPath();
                       if (hxmlPath != null) {
                         VirtualFile file1 = LocalFileFinder.findFile(hxmlPath);
                         if (file1 != null) {
                           commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                           commandLineArguments.add(file1.getPath());
                           commandLineArguments.add("--display");
                           commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

                           List<String> stderr =
                             HaxelibClasspathUtils.getProcessStderr(commandLineArguments, BuildProperties.getProjectBaseDir(project));

                           getCompletionFromXml(result, project, stderr);
                         }
                       }
                       break;
                     case HaxeModuleSettings.USE_NMML:
                       //Not sure if NME has display command
                       //Export/flash/haxe contains build.hxml which gets generated after build
                       break;
                     case HaxeModuleSettingsBaseImpl.USE_OPENFL:
                       commandLineArguments.add(HaxelibClasspathUtils.getHaxelibPath(moduleForFile));
                       commandLineArguments.add("run");
                       commandLineArguments.add("lime");
                       commandLineArguments.add("display");
                       //flash, html5, linux, etc
                       String targetFlag = moduleSettings.getNmeTarget().getTargetFlag();
                       commandLineArguments.add(targetFlag);

                       List<String> stdout = HaxelibClasspathUtils.getProcessStdout(commandLineArguments,
                                                                                    BuildProperties.getProjectBaseDir(project));
                       commandLineArguments.clear();

                       commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));

                       formatAndAddCompilerArguments(commandLineArguments, stdout);

                       commandLineArguments.add("--display");
                       commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

                       List<String> stderr =
                         HaxelibClasspathUtils.getProcessStderr(commandLineArguments, BuildProperties.getProjectBaseDir(project));

                       getCompletionFromXml(result, project, stderr);
                       break;
                     case HaxeModuleSettingsBaseImpl.USE_PROPERTIES:
                       String arguments = moduleSettings.getArguments();
                       if (!arguments.isEmpty()) {
                         commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                         formatAndAddCompilerArguments(commandLineArguments, Arrays.asList(arguments.split("\n")));
                         commandLineArguments.add("--display");
                         commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

                         List<String> stderr1 =
                           HaxelibClasspathUtils.getProcessStderr(commandLineArguments, BuildProperties.getProjectBaseDir(project));

                         getCompletionFromXml(result, project, stderr1);
                       }
                       break;
                   }
                 }
               }
             }
           });
  }

  private void formatAndAddCompilerArguments(ArrayList<String> commandLineArguments, List<String> stdout) {
    for (int i = 0; i < stdout.size(); i++) {
      String s = stdout.get(i).trim();
      if (!s.startsWith("#")) {
        commandLineArguments.addAll(Arrays.asList(s.split(" ")));
      }
    }
  }

  private void getCompletionFromXml(CompletionResultSet result, Project project, List<String> stderr) {
    if (!stderr.isEmpty() && !stderr.get(0).contains("Error") && stderr.size() > 1) {
      String s = Joiner.on("").join(stderr);
      PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText("data.xml", XmlFileType.INSTANCE, s);

      XmlFile xmlFile = (XmlFile)fileFromText;
      XmlDocument document = xmlFile.getDocument();

      if (document != null) {
        XmlTag rootTag = document.getRootTag();
        if (rootTag != null) {
          XmlTag[] xmlTags = rootTag.findSubTags("i");
          for (XmlTag xmlTag : xmlTags) {
            String n = xmlTag.getAttribute("n").getValue();
            XmlTag t = xmlTag.findFirstSubTag("t");
            XmlTag d = xmlTag.findFirstSubTag("d");

            LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(n);

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
        String param = type.substring(startPositions.get(j), endPositions.get(j));
        if (j < startPositions.size() - 1)
        {
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
