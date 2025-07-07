package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
import com.intellij.plugins.haxe.model.HaxeSourceRootModel;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeConsoleFilterProvider implements ConsoleFilterProvider {

    Pattern compilerMessageWithFileAndLine = Pattern.compile("(\\s*(?<label>\\[?\\w+\\]?)\\s+)?" // optional label/prefix (WARNING, ERROR etc ("[ERROR]" when haxe 5 fromat))
                                                             + "(?<path>((\\w:)?/)?([a-z_\\-\\s0-9.,]+(/)?)+\\.(\\w+))" // file path (note absolute path for windows expects forward slashes)
                                                             + ":(?<line>([0-9]+))" // line
                                                             + ":\\s+(characters)\\s+"
                                                             + "(?<column>([0-9]+))-\\d+" // position
                                                             + ".*", // rest of message
            Pattern.CASE_INSENSITIVE);

    Pattern stacktraceWithFileAndLine = Pattern.compile("(called\\sfrom\\s)"
                                                        + "(?<qname>([\\w_.$]+)+)"
                                                        + "\\s(\\("
                                                        + "((?<path>((\\w:)?/)?([a-z_\\-\\s0-9.,]+(/)?)+\\.(\\w+)))"
                                                        + "\\sline\\s(?<line>\\d+)"
                                                        + "\\))",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {

        String basePath = project.getBasePath();
        Filter psiFilter = (text, entireLength) -> {
            if(HaxeProjectSettings.getInstance(project).getDetectCodeReferencesInConsole()) {
                Matcher compilerMessageMatcher = compilerMessageWithFileAndLine.matcher(text);
                if (compilerMessageMatcher.matches()) {
                    String path = compilerMessageMatcher.group("path");
                    String line = compilerMessageMatcher.group("line");
                    String column = compilerMessageMatcher.group("column");

                    int lineNo = Integer.parseInt(line);
                    int columnNo = Integer.parseInt(column);
                    int offsetPath = text.indexOf(path);


                    VirtualFile virtualFile = HaxeFileUtil.locateFile(path, basePath);

                    if (virtualFile != null) {
                        OpenFileHyperlinkInfo openFileHyperlinkInfo = new OpenFileHyperlinkInfo(project, virtualFile, lineNo-1, columnNo-1);
                        int endOffset = offsetPath + path.length() + line.length();
                        return new Filter.Result(offsetPath, endOffset + 1, openFileHyperlinkInfo);
                    }
                }
                Matcher stacktrace = stacktraceWithFileAndLine.matcher(text);
                if (stacktrace.matches()) {
                    String path = stacktrace.group("path");
                    String line = stacktrace.group("line");

                    int lineNo = Integer.parseInt(line);
                    int offsetPath = text.indexOf(path);


                    VirtualFile virtualFile = HaxeFileUtil.locateFile(path, basePath);
                    if(virtualFile == null) {
                        //TODO check if this slows down things

                        // search all source roots for file
                        HaxeProjectModel haxeProjectModel = HaxeProjectModel.fromProject(project);
                        List<HaxeSourceRootModel> roots = haxeProjectModel.getRoots();
                        virtualFile = roots.stream().map(haxeSourceRootModel -> haxeSourceRootModel.directory)
                                .filter(Objects::nonNull)
                                .map(directory -> directory.getVirtualFile().getUrl())
                                .map(url -> HaxeFileUtil.locateFile(url + "/" + path))
                                .filter(Objects::nonNull)
                                .findFirst().orElse(null);
                    }

                    if (virtualFile != null) {
                        OpenFileHyperlinkInfo openFileHyperlinkInfo = new OpenFileHyperlinkInfo(project, virtualFile, lineNo-1);
                        int endOffset = offsetPath + path.length();
                        return new Filter.Result(offsetPath, endOffset, openFileHyperlinkInfo);
                    }
                }
            }
            return null;
        };
        return new Filter[]{psiFilter};
    }
}
