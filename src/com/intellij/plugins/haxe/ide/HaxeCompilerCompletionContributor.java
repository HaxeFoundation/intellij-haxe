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
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.diagnostic.Log;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils;
import com.intellij.plugins.haxe.haxelib.HaxelibItem;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeStatement;
import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.DocumentUtil;
import com.intellij.util.LineSeparator;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;
import util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

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

               int offset = parameters.getOffset();

               String separator = FileDocumentManagerImpl.getLineSeparator(parameters.getEditor().getDocument(), file.getVirtualFile());
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
                           commandLineArguments.add("--display " + file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

                           List<String> stderr =
                             HaxelibClasspathUtils.getProcessStderr(commandLineArguments, BuildProperties.getProjectBaseDir(project));
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

                       List<String> strings = new ArrayList<String>();

                       for (int i = 0; i < stdout.size(); i++) {
                         String s = stdout.get(i).trim();
                         if (!s.startsWith("#")) {
                           commandLineArguments.addAll(Arrays.asList(s.split(" ")));
                         }
                       }

                       commandLineArguments.add("--display");
                       commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));
                       List<String> stderr =
                         HaxelibClasspathUtils.getProcessStderr(commandLineArguments, BuildProperties.getProjectBaseDir(project));

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
                               XmlAttribute[] attributes = xmlTag.getAttributes();
                               String n = xmlTag.getAttribute("n").getValue();
                               XmlTag t = xmlTag.findFirstSubTag("t");
                               XmlTag d = xmlTag.findFirstSubTag("d");

                               LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(n);
                               if (t != null) {
                                 String text = t.getValue().getText();
                                 if (d != null) {
                                   text += " " + d.getValue().getText();
                                 }
                                 lookupElementBuilder = lookupElementBuilder.withTailText(" " + text, true);
                               }
                               result.addElement(lookupElementBuilder);
                             }
                           }
                         }
                       }
                       break;
                     case HaxeModuleSettingsBaseImpl.USE_PROPERTIES:
                       String arguments = moduleSettings.getArguments();
                       if (!arguments.isEmpty()) {
                         commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                         commandLineArguments.add(arguments);
                         commandLineArguments.add("--display " + file.getVirtualFile().getPath() + "@" + Integer.toString(offset));
                       }
                       break;
                   }
                 }
               }

               //result.addElement(LookupElementBuilder.create("test"));
             }
           });
  }
}
