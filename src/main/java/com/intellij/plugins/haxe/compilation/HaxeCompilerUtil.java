/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.util.HaxeCommonCompilerUtil;

import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.plugins.haxe.util.HaxeProcessUtil;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeCompilerUtil
{
    //static {      // Take this out when finished debugging.
    //    log.setLevel(LogLevel.DEBUG);
    //}

    public static final String ERROR = "Error: ";

    /**
     * Upper bound for a single Haxe compiler-completion query (--display file@offset).
     * The external process is spawned on a pooled thread; this bounds how long the
     * completion thread blocks on {@code Future.get} before giving up.
     */
    private static final int COMPILER_COMPLETION_TIMEOUT_MS = 10_000;

    private static com.intellij.openapi.util.Key messageWindowAutoOpened =
      new com.intellij.openapi.util.Key("messageWindowAutoOpened");


    /**
     * Error notification callback.
     */
    public static class ErrorNotifier {
        public void notifyError(String message) {
            log.info(message);
        }
    }
    private static ErrorNotifier defaultNotifier = new ErrorNotifier();


    /**
     * Verify that the Haxe project file (.xml, .nmml, etc.; NOT an IDEA project file)
     * specified in the build settings is available.  Put up an error message if it's not.
     *
     * Returns a VirtualFile to the located project file, or null on error.
     */
    public static VirtualFile verifyProjectFile(@NotNull Module module, @NotNull String type, @NotNull String path) {
        return verifyProjectFile(module, type, path, null);
    }

    /**
     * Verify that the Haxe project file (.xml, .nmml, etc.; NOT an IDEA project file)
     * specified in the build settings is available.  Put up an error message if it's not.
     *
     * Returns a VirtualFile to the located project file, or null on error.
     */
    public static VirtualFile verifyProjectFile(@NotNull Module module, @NotNull String type, @NotNull String path, @Nullable ErrorNotifier notifier) {

        if (path.isEmpty()) {
            String message = "Completion error: No " + type + " project file is specified in project settings.";  // TODO: Externalize string.
            advertiseError(message, notifier);
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
            VirtualFile moduleDir = ProjectUtil.guessModuleDir(module);
            file = null != moduleDir ? moduleDir.findFileByRelativePath(path) : null;
        }


        // Still no luck? Try the project root (actually, the directory where the .prj file is).
        if (null == file) {
            VirtualFile projectDirectory = module.getProject().getBaseDir();
            file = projectDirectory != null ? projectDirectory.findFileByRelativePath(path) : null;
        }

        if (null == file) {
            String message = "Completion error: " + type + " project file does not exist: " + path;  // TODO: Externalize string.
            advertiseError(message, notifier);
        }
        return file;

    }

    /**
     * Send errors off to the given handler, or a default hander if notifier is null.
     *
     * @param message - Error message
     * @param notifier - Error handler or null to use default.
     */
    public static void advertiseError(String message, @Nullable ErrorNotifier notifier) {
        if (null == notifier) {
            defaultNotifier.notifyError(message);
        }
        else {
            notifier.notifyError(message);
        }
    }


    /**
     * Add errors to the compile context.
     *
     * @param context
     * @param errorRoot
     * @param errors
     */
    public static void fillContext(CompileContext context, String errorRoot,
                                   String[] errors)
    {
        for (String error : errors) {
            addErrorToContext(error, context, errorRoot);
        }
    }

    private static void addErrorToContext(String error, CompileContext context,
                                          String errorRoot)
    {
        // TODO: Add a button to the Haxe module settings to control whether we always open the window or not.
        if (context.getUserData(messageWindowAutoOpened) == null) {
            openCompilerMessagesWindow(context);
            context.putUserData(messageWindowAutoOpened, "yes");
        }

        final HaxeCompilerMessage compilerError = HaxeCompilerMessage.create
            (errorRoot,
             error,
             !ApplicationManager.getApplication().isUnitTestMode());
        

        if (null != compilerError) {
            String path = compilerError.getPath();
            context.addMessage
                (compilerError.getCategory(),
                 compilerError.getMessage(),
                 path == null ? null : VfsUtilCore.pathToUrl(compilerError.getPath()),
                 compilerError.getLine(),
                 compilerError.getColumn());
        }
    }

    private static boolean isHeadless() {
      return ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment();
    }

    private static void openCompilerMessagesWindow(final CompileContext context) {
        // Force the compile window open.  We should probably have a configuration button
        // on the compile gui page to force it open or not.
        if (!isHeadless()) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    // This was lifted from intellij-community/java/compiler/impl/src/com/intellij/compiler/progress/CompilerTask.java
                    final ToolWindow tw = ToolWindowManager.getInstance(context.getProject()).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
                    if (tw != null) {
                        tw.activate(null, false);
                    }
                }
            });
        }
    }

    /**
     * Find the source root containing the given file.
     *
     * @param file
     * @return
     */
    @Nullable
    public static VirtualFile findSourceRoot(@NotNull PsiFile file) {
        VirtualFile vfile = file.getVirtualFile();
        if (null == vfile) {
            return null;
        }
        VirtualFile vsrcroot = ProjectRootManager.getInstance(file.getProject()).getFileIndex().getSourceRootForFile(vfile);
        return vsrcroot;
    }

    @Nullable
    public static VirtualFile findCompileRoot(@NotNull PsiFile file) {
        // This will work for most people.  For those whose project files are in different trees from their sources,
        // this has to be changed.
        // TODO: Add a "compile directory" to the Haxe module settings.
        return file.getProject().getBaseDir();
        //return findSourceRoot(file);
    }

    @Nullable
    public static VirtualFile findCompileRoot(@NotNull Module module) {
        // This will work for most people.  For those whose project files are in different trees from their sources,
        // this has to be changed.
        // TODO: Add a "compile directory" to the Haxe module settings.
        return module.getProject().getBaseDir();
    }

    /**
     * Run an interruptible process with the Haxe SDK compiler environment.  Checks
     * whether the process/thread has been canceled (from within IDEA).
     *
     * @param command - Command and parameters.
     * @param mixedOutput - include stderr in stdout output.
     * @param dir - directory to run the command in.
     * @param sdkData - sdk to use to set the command environment.
     * @param stdout - List to append output to.  Will not be cleared on start.
     * @param stderr - List to append error output to. Will not be cleared on start.
     *
     * @return The exit status of the command.
     */
    public static int runInterruptibleCompileProcess(List<String> command,
                                                     boolean mixedOutput,
                                                     VirtualFile dir,
                                                     HaxeSdkAdditionalDataBase sdkData,
                                        /*modifies*/ List<String> stdout,
                                        /*modifies*/ List<String> stderr,
                                                     HaxeDebugTimeLog timeLog) {
        // Spawn the external haxe process from a pooled thread so OSProcessHandler.waitFor()
        // runs without a Read Action — that is the only thread state the platform's
        // checkEdtAndReadAction() guard inspects. The completion thread (which DOES hold a
        // Read Action) waits via Future.get, which the platform does not flag.
        //
        // We deliberately do not poll cancellation here. The completion framework's outer
        // indicator is cancelled aggressively (every keystroke retriggers a new completion),
        // and threading that cancellation into the wait kills the haxe process before it
        // can return its --display output — that regression is what broke dot-completion
        // in the earlier ProgressManager.runProcessWithProgressSynchronously attempt.
        Future<Integer> future = ApplicationManager.getApplication().executeOnPooledThread(() ->
            HaxeProcessUtil.runProcess(command, mixedOutput, dir, sdkData, stdout, stderr, timeLog, true));
        try {
            return future.get(COMPILER_COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException te) {
            future.cancel(true);
            log.warn("Haxe compiler-completion timed out after " + COMPILER_COMPLETION_TIMEOUT_MS + " ms");
            return 124;
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return 255;
        }
        catch (ExecutionException ee) {
            log.warn("Haxe compiler-completion threw", ee.getCause());
            return 255;
        }
    }


    public static String calculateCompilerOutput(@NotNull Module module) {
        HaxeCommonCompilerUtil.CompilationContext context = HaxeCompiler.createDummyCompilationContext(module);
        return HaxeCommonCompilerUtil.calculateOutputPath(context);
    }
}
