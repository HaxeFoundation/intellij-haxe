package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeConsoleFilterProvider implements ConsoleFilterProvider {

    public final Pattern compilerMessageWithFileAndLine = Pattern.compile("\\s+(?<label>\\w+)\\s+(?<path>(([\\w-]+)+(.[a-zA-Z]+)+)):(?<line>\\d+):\\s+(characters)\\s+(?<column>\\d+)\\-\\d+");

    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {

        String basePath = project.getBasePath();
        Filter psiFilter = (text, entireLength) -> {
            if(HaxeProjectSettings.getInstance(project).getDetectCodeReferencesInConsole()) {
                Matcher matcher = compilerMessageWithFileAndLine.matcher(text);
                if (matcher.matches()) {
                    String path = matcher.group("path");
                    String line = matcher.group("line");
                    String column = matcher.group("column");

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
            }
            return null;
        };
        return new Filter[]{psiFilter};
    }
}
