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

    Pattern compilerMessageWithFileAndLine = Pattern.compile("(\\s*(?<label>\\[?\\w+\\]?)\\s+)?" // optional label/prefix (WARNING, ERROR etc ("[ERROR]" when haxe 5 format))
                                                             + "(?<path>((\\w:)?/)?([a-z_\\-\\s0-9.,]+(/)?)+\\.(\\w+))" // file path (note absolute path for windows expects forward slashes)
                                                             + ":(?<line>([0-9]+))" // line
                                                             + ":\\s+(?<type>(characters|lines))\\s+"
                                                             + "(?<column>([0-9]+))-\\d+" // position
                                                             + ".*", // rest of message
            Pattern.CASE_INSENSITIVE);

    Pattern stacktraceWithFileAndLine = Pattern.compile("(called\\sfrom\\s)"
                                                        // qname: Class.method, incl. Haxe's synthetic closure names
                                                        // like Class.~method.1 and fun$N — so allow . $ ~ (\w has _)
                                                        + "(?<qname>[\\w.$~]+)"
                                                        + "\\s*(\\("
                                                        + "(?<path>((\\w:)?/)?([a-z_\\-\\s0-9.,]+(/)?)+\\.(\\w+))"
                                                        + "((\\sline\\s|:)(?<line>\\d+))"
                                                        + "\\))",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {

        String basePath = project.getBasePath();
        Filter psiFilter = (text, entireLength) -> {
                // Filter.Result offsets are absolute in the whole console document, not relative
                // to this line, so anchor every within-line index to the line's start. Do NOT
                // trim text first: entireLength - text.length() must use the real line length.
                int lineStartOffset = entireLength - text.length();
                if (HaxeProjectSettings.getInstance(project).getDetectCodeReferencesInConsole()) {
                    Matcher compilerMessageMatcher = compilerMessageWithFileAndLine.matcher(text);
                    // find() not matches(): console lines arrive with their trailing newline,
                    // and matches() requires the whole input to match (and `.` never matches \n),
                    // so a full-line anchor would never fire on real console output.
                    if (compilerMessageMatcher.find()) {
                        String path = compilerMessageMatcher.group("path");
                        String line = compilerMessageMatcher.group("line");
                        String type = compilerMessageMatcher.group("type");
                        String column = compilerMessageMatcher.group("column");
                        // if message only provides lines, we set column to null
                        if (type.equalsIgnoreCase("lines")) {
                            column = "0";
                        }

                        int lineNo = Integer.parseInt(line);
                        int columnNo = Integer.parseInt(column);
                        int offsetPath = text.indexOf(path);


                        VirtualFile virtualFile = HaxeFileUtil.locateFile(path, basePath);

                        if (virtualFile != null) {
                            OpenFileHyperlinkInfo openFileHyperlinkInfo = new OpenFileHyperlinkInfo(project, virtualFile, lineNo - 1, columnNo - 1);
                            int highlightStartOffset = lineStartOffset + offsetPath;
                            int highlightEndOffset = highlightStartOffset + path.length() + line.length() + 1; // path + ':' + line
                            return new Filter.Result(highlightStartOffset, highlightEndOffset, openFileHyperlinkInfo);
                        }
                    }
                    Matcher stacktrace = stacktraceWithFileAndLine.matcher(text);
                    // find() not matches(): see the compiler-message matcher above — the
                    // trailing newline on a console line makes a full-line match() never fire.
                    if (stacktrace.find()) {
                        String path = stacktrace.group("path");
                        String line = stacktrace.group("line");

                        int lineNo = Integer.parseInt(line);
                        // highlight the whole "path:line" span (matcher offsets, not indexOf)
                        int pathStart = stacktrace.start("path");
                        int lineEnd = stacktrace.end("line");

                        VirtualFile virtualFile = HaxeFileUtil.locateFile(path, basePath);
                        if (virtualFile == null) {
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
                            OpenFileHyperlinkInfo openFileHyperlinkInfo = new OpenFileHyperlinkInfo(project, virtualFile, Math.max(0, lineNo - 1));
                            int highlightStartOffset = lineStartOffset + pathStart;
                            int highlightEndOffset = lineStartOffset + lineEnd;
                            return new Filter.Result(highlightStartOffset, highlightEndOffset, openFileHyperlinkInfo);
                        }
                    }
        }
            return null;
        };
        return new Filter[]{psiFilter};
    }
}
