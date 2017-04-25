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
package com.intellij.plugins.haxe.compilation;

import com.google.common.base.Joiner;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.ide.HaxeCompilerCompletionItem;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
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
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.compilation.HaxeCompilerUtil.verifyProjectFile;

/**
 * Created by ebishton on 11/Feb/17.
 *
 * Use the Haxe compiler to help the IDE.  The Haxe compiler has several
 * functions that are useful, such as type identification, find usages,
 * completion (see HaxeCompilerCompletionContributor), and others.
 */
public class HaxeCompilerServices {

    static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompilerServices");
    static {      // Take this out when finished debugging.
        LOG.setLevel(org.apache.log4j.Level.DEBUG);
    }

    // Cache to keep the openFL display args (read from the project.xml).
    static HaxeCompilerProjectCache openFLDisplayArguments = new HaxeCompilerProjectCache();

    // Pattern used to detect empty and/or whitespace-only lines.
    static final Pattern EMPTY_LINE_REGEX = Pattern.compile("^\\s+$" );

    // Sink to send compilation errors to.
    private HaxeCompilerUtil.ErrorNotifier myErrorNotifier = null;


    public HaxeCompilerServices(@Nullable HaxeCompilerUtil.ErrorNotifier errorNotifier) {
        myErrorNotifier = errorNotifier;
    }

    @NotNull
    public List<HaxeCompilerCompletionItem> getPossibleCompletions(@NotNull PsiFile file,
                                                                   @NotNull PsiElement element,
                                                                   @NotNull Editor editor) {

        HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("HaxeCompilerCompletionContributor",
                                                             HaxeDebugTimeLog.Since.StartAndPrevious);
        List<HaxeCompilerCompletionItem> completions = HaxeCompilerCompletionItem.EMPTY_LIST;

        try {
            Project project = file.getProject();
            VirtualFile vfile = file.getVirtualFile();
            VirtualFile compileRoot = HaxeCompilerUtil.findCompileRoot(file);
            if (null == vfile || null == compileRoot) {
                // Can't run a completion on an in-memory file (at least for now).
                // TODO: Allow completion on in-memory files for 3.4 compilers.
                return completions;
            }
            Module moduleForFile = ModuleUtil.findModuleForFile(vfile, project);

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
                        projectFile = verifyProjectFile(moduleForFile, "HXML", moduleSettings.getHxmlPath(), myErrorNotifier);
                        if (null == projectFile) {
                            break;
                        }

                        commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                        commandLineArguments.add(projectFile.getPath());

                        completions = collectCompletionsFromCompiler(file, element, editor, commandLineArguments, timeLog);
                        break;
                    case HaxeModuleSettings.USE_NMML:
                        projectFile = verifyProjectFile(moduleForFile, "NMML", moduleSettings.getNmmlPath(), myErrorNotifier);
                        if (null == projectFile) {
                            break;
                        }
                        formatAndAddCompilerArguments(commandLineArguments, moduleSettings.getNmeFlags());
                        completions = collectCompletionsFromNME(file, element, editor, commandLineArguments, timeLog);
                        break;
                    case HaxeModuleSettingsBaseImpl.USE_OPENFL:
                        projectFile = verifyProjectFile(moduleForFile, "OpenFL", moduleSettings.getOpenFLPath(), myErrorNotifier);
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
                            List<String> stdout = new ArrayList<String>();
                            HaxeCompilerUtil.runInterruptibleCompileProcess(limeArguments, false,
                                                                            compileRoot, HaxeSdkUtilBase.getSdkData(moduleForFile),
                                                                            stdout, null, timeLog);

                            // Need to filter out empty/blank lines.  They cause an empty argument to
                            // haxelib, which errors out and breaks completion.
                            compilerArgsFromProjectFile = filterEmptyLines(stdout);
                            openFLDisplayArguments.put(moduleForFile, projectFile.getUrl(), targetFlag, compilerArgsFromProjectFile);
                        }

                        commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                        formatAndAddCompilerArguments(commandLineArguments, compilerArgsFromProjectFile);

                        completions = collectCompletionsFromCompiler(file, element, editor, commandLineArguments, timeLog);

                        break;
                    case HaxeModuleSettingsBaseImpl.USE_PROPERTIES:
                        commandLineArguments.add(HaxeHelpUtil.getHaxePath(moduleForFile));
                        formatAndAddCompilerArguments(commandLineArguments, moduleSettings.getArguments());
                        completions = collectCompletionsFromCompiler(file, element, editor, commandLineArguments, timeLog);
                        break;
                }
            }
        }
        finally {
            timeLog.stamp("Finished");
            timeLog.print();
        }
        return completions;
    }

    protected void advertiseError(String message) {
        HaxeCompilerUtil.advertiseError(message, myErrorNotifier);
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

    @NotNull
    private List<String> filterEmptyLines(List<String> lines) {
        List<String> filtered = new ArrayList<String>(lines.size());
        for (String l : lines) {
            if ( ! (l.isEmpty() || EMPTY_LINE_REGEX.matcher(l).matches())) {
                filtered.add(l);
            }
        }
        return filtered;
    }

    private int recalculateFileOffset(@NotNull PsiFile file, @NotNull PsiElement position, Editor editor) {
        int offset = position.getTextOffset();;

        VirtualFile virtualFile = file.getVirtualFile();
        if (null == virtualFile) {  // In memory file, the position doesn't change.
            return offset;
        }

        // Get the separator, checking the file if we don't know yet.  May still return null.
        String separator = LoadTextUtil.detectLineSeparator(virtualFile, true);

        // IntelliJ IDEA normalizes file line endings, so if file line endings is
        // CRLF - then we have to shift an offset so Haxe compiler could get proper offset
        if (LineSeparator.CRLF.getSeparatorString().equals(separator)) {
            int lineNumber =
                com.intellij.openapi.util.text.StringUtil.offsetToLineNumber(editor.getDocument().getText(), offset);
            offset += lineNumber;
        }
        return offset;
    }

    @NotNull
    private List<HaxeCompilerCompletionItem> collectCompletionsFromCompiler(@NotNull PsiFile file,
                                                                            @NotNull PsiElement element,
                                                                            @NotNull Editor editor,
                                                                            ArrayList<String> commandLineArguments,
                                                                            HaxeDebugTimeLog timeLog) {
        // There is a problem here in that the current buffer may not have been saved.
        // If that is the case, then the position is also incorrect, and the compiler
        // doesn't have access to the correct sources.  If the haxe compiler is version 3.4 or
        // later, then it has the -D display-stdin parameter available and we can pump the
        // unsaved buffer through to the compiler. (Though that does nothing for completion
        // from related but also unsaved buffers.)  Doing so will also require the compiler
        // server (if used) to be started with the "--wait stdin" parameter.

        if (null == file) {
            // TODO: Handle in-memory files for Haxe 3.4.
            advertiseError("Error: Compiler completion requested for in-memory-only file.");  // TODO: Externalize string.
            return HaxeCompilerCompletionItem.EMPTY_LIST;
        }

        Project project = file.getProject();
        Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), project);
        int offset = recalculateFileOffset(file, element, editor);

        // Tell the compiler we want field completion, adding the type (var or method)
        commandLineArguments.add("-D");
        commandLineArguments.add("display-details");
        commandLineArguments.add("--display");

        commandLineArguments.add(file.getVirtualFile().getPath() + "@" + Integer.toString(offset));

        timeLog.stamp("Calling compiler");
        List<String> stderr = new ArrayList<String>();
        List<String> stdout = new ArrayList<String>();
        int status = HaxeCompilerUtil.runInterruptibleCompileProcess(commandLineArguments, false,
                                                                     HaxeCompilerUtil.findCompileRoot(file),
                                                                     HaxeSdkUtilBase.getSdkData(moduleForFile),
                                                                     stdout, stderr, timeLog);

        timeLog.stamp("Compiler finished. Output found on " + (stdout.isEmpty() ? "" : "stdout ") + (stderr.isEmpty() ? "" : "stderr"));
        // LOG.debug("Compiler finished. Output found on " + (stdout.isEmpty() ? "" : "stdout ") + (stderr.isEmpty() ? "" : "stderr"));
        if (0 != status) {
            return handleCompilerError(project, stderr);
        }
        return parseCompletionFromXml(project, stderr);
    }

    @NotNull
    private List<HaxeCompilerCompletionItem> collectCompletionsFromNME(@NotNull PsiFile file,
                                                                       @NotNull PsiElement element,
                                                                       @NotNull Editor editor,
                                                                       ArrayList<String> commandLineArguments,
                                                                       HaxeDebugTimeLog timeLog) {
        // See note on collectCompletionsFromCompiler.
        Project project = file.getProject();
        Module moduleForFile = ModuleUtil.findModuleForFile(file.getVirtualFile(), project);
        int offset = recalculateFileOffset(file, element, editor);

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
            List<String> stderr = new ArrayList<String>();
            int status = HaxeCompilerUtil.runInterruptibleCompileProcess(commandLineArguments, false,
                                                                         HaxeCompilerUtil.findCompileRoot(file),
                                                                         HaxeSdkUtilBase.getSdkData(moduleForFile),
                                                                         null, stderr, timeLog);
            timeLog.stamp("NME finished");
            if (0 != status) {
                return handleCompilerError(project, stderr);
            }
            return parseCompletionFromXml(project, stderr);
        } catch (IOException e) {
            advertiseError("Completion failed: Could not create temporary file."); // TODO: Externalize string.
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return HaxeCompilerCompletionItem.EMPTY_LIST;
    }

    @NotNull
    private List<HaxeCompilerCompletionItem> handleCompilerError(Project project, List<String> stderr) {
        List<String> completionList = new ArrayList<String>();
        if (findCompletionXmlInErrorOutput(stderr, completionList)) {
            return parseCompletionFromXml(project, completionList);
        }
        reportErrors(project, stderr);
        return HaxeCompilerCompletionItem.EMPTY_LIST;
    }

    private void reportErrors(Project project, List<String> errors) {
        for (String error : errors) {

            StringBuilder msg = new StringBuilder();
            msg.append("Compiler completion");        // TODO: Externalize string.

            // Could be a syntax warning.
            String projectPath = project.getBaseDir().getPath();
            HaxeCompilerError compilerError = HaxeCompilerError.create(projectPath, error);
            if (null != compilerError) {
                if (compilerError.isErrorMessage()) {
                    msg.append(" error");                   // TODO: Externalize and don't build the string.
                }
                msg.append(": ");

                msg.append(compilerError.getErrorMessage());
                msg.append(" @ ");

                // XXX: We can make a link show up in the tooltip by enabling this, but
                //      clicking on it does nothing.
                //msg.append("<a href=\"");
                //msg.append(compilerError.getUrl());
                //msg.append("\">");

                String path = compilerError.getPath();
                if (path.startsWith(projectPath)) {
                    msg.append(path.subSequence(projectPath.length(), path.length()));
                } else {
                    msg.append(path);
                }
                msg.append(" (");
                msg.append(compilerError.getLine());
                msg.append(":");
                msg.append(compilerError.getColumn());
                msg.append(")");

                //msg.append("</a>");
            }
            else {
                msg.append(": ");
                msg.append(error);
            }
            advertiseError(msg.toString());
        }
    }

    private boolean findCompletionXmlInErrorOutput(List<String> compilerOutput, List<String> xml) {
        if (null == compilerOutput || compilerOutput.isEmpty()) {
            return false;
        }

        boolean foundStart = false;
        boolean foundEnd = false;
        String endTag = null;
        for (String s : compilerOutput) {
            if (!foundStart) {
                if (s.equals("<list>")) {
                    foundStart = true;
                    endTag = "</list>";
                    xml.add(s);
                } else if (s.equals("<type>")) {
                    foundStart = true;
                    endTag = "</type>";
                    xml.add(s);
                }
                // Otherwise, ignored.
            } else {
                xml.add(s);
                if (s.equals(endTag)) {
                    foundEnd = true;
                    break;
                }
            }
        }

        return foundStart && foundEnd;
    }

    private List<HaxeCompilerCompletionItem> parseCompletionFromXml(Project project, List<String> stderr) {
        List<HaxeCompilerCompletionItem> completions = new ArrayList<HaxeCompilerCompletionItem>();
        try {
            LOG.debug(stderr.toString());
            String s = Joiner.on("").join(stderr);

            if (s.isEmpty()) {
                LOG.warn("Empty compiler output from completion query.");
                return completions;
            }

            PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText("data.xml", XmlFileType.INSTANCE, s);

            XmlFile xmlFile = (XmlFile)fileFromText;
            XmlDocument document = xmlFile.getDocument();
            XmlTag rootTag = null != document ? document.getRootTag() : null;

            if (null == rootTag) {
                LOG.warn("Failure to parse XML: " + s);
                return completions;
            }

            if ("list".equals(rootTag.getName())) {
                XmlTag[] xmlTags = rootTag.findSubTags("i");
                for (XmlTag xmlTag : xmlTags) {
                    XmlAttribute nAttr = xmlTag.getAttribute("n");
                    XmlAttribute kAttr = xmlTag.getAttribute("k");
                    String name = null == nAttr ? null : nAttr.getValue();
                    String memberType = null == kAttr ? null : kAttr.getValue();
                    XmlTag typeTag = xmlTag.findFirstSubTag("t");
                    XmlTag docTag = xmlTag.findFirstSubTag("d");


                    HaxeCompilerCompletionItem item = new HaxeCompilerCompletionItem(name);
                    item.setMemberType(memberType);
                    if (typeTag != null) {
                        String formattedType = getFormattedText(typeTag.getValue().getText());
                        parseFunctionParams(formattedType, /*modifies*/ item);
                    }
                    if (docTag != null) {
                        String text1 = getFormattedText(docTag.getValue().getText());
                        item.setDocumentation(text1);
                    }
                    completions.add(item);
                }
            } else if ("type".equals(rootTag.getName())) {
                String type = getFormattedText(rootTag.getValue().getTrimmedText());
                HaxeCompilerCompletionItem item = new HaxeCompilerCompletionItem("Type");
                parseFunctionParams(type, item);
                completions.add(item);
            }
            // TODO: Add other completion types here. (e.g. Call argument completion "type").
            else {
                LOG.warn("Failure to parse XML: " + s );
                return completions;
            }
        }
        catch (ProcessCanceledException e) {
            advertiseError("Haxe compiler completion canceled.");  // TODO: Externalize string.
            LOG.debug("Haxe compiler completion canceled.", e);
            throw e;
        }
        return completions;
    }

    private String getFormattedText(String text1) {
        if (null == text1)
            return null;
        text1 = text1.replaceAll("\t", "");
        text1 = text1.replaceAll("\n", "");
        text1 = text1.replaceAll("&lt;", "<");
        text1 = text1.replaceAll("&gt;", ">");
        text1 = text1.trim();
        return text1;
    }

    //Originally ported from HIDE
    //https://github.com/HaxeIDE/HIDE/blob/master/src/core/FunctionParametersHelper.hx#L193
    public void parseFunctionParams(String type, HaxeCompilerCompletionItem item)
    {
        List<String> parameters = new ArrayList<String>();;
        String retType = type;
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
        item.setParameters(parameters);
        item.setReturnType(retType);
    }

}
