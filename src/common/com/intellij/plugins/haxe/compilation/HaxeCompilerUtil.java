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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil
{
    static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
    //static {  // Remove when finished debugging.
    //    LOG.setLevel(Level.DEBUG);
    //}

    public static final String ERROR = "Error: ";

    private static com.intellij.openapi.util.Key messageWindowAutoOpened =
      new com.intellij.openapi.util.Key("messageWindowAutoOpened");


    /**
     * Error notification callback.
     */
    public static class ErrorNotifier {
        public void notifyError(String message) {
            LOG.info(message);
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
            VirtualFile moduleFile = module.getModuleFile();
            VirtualFile moduleDir = null != moduleFile ? moduleFile.getParent() : null;
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

        final HaxeCompilerError compilerError = HaxeCompilerError.create
            (errorRoot,
             error,
             !ApplicationManager.getApplication().isUnitTestMode());
        

        if (null != compilerError) {
            context.addMessage
                (compilerError.getCategory(),
                 compilerError.getErrorMessage(),
                 VfsUtilCore.pathToUrl(compilerError.getPath()),
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
        int ret = 255;
        Process process = null;

        try {
            LOG.debug("Starting runInterruptibleCompileProcess for " + command.toString());
            ProgressManager.checkCanceled();

            File fdir = null != dir ? new File(dir.getPath()) : null;
            LOG.debug("dir is " + dir.getPath());
            ProcessBuilder builder = HaxeSdkUtilBase.createProcessBuilder(command, fdir, sdkData);
            if (mixedOutput)
                builder.redirectErrorStream(true);

            if (null != timeLog) {
                timeLog.stamp("Executing " + command.get(0));
            }
            process = builder.start();
            LOG.debug("Compiler process has started.");
            InputStreamReader stdoutReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedStdout = new BufferedReader(stdoutReader);

            InputStreamReader stderrReader = mixedOutput ? null : new InputStreamReader(process.getErrorStream());
            BufferedReader bufferedStderr = mixedOutput ? null : new BufferedReader(stderrReader);

            do {
                try {
                    // Let our forked process get a little work done.
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    ; // Swallow it.
                }
                GatherOutput(mixedOutput, stdout, stderr, bufferedStdout, bufferedStderr);
            }
            while (processIsAlive(process));
            ret = process.exitValue();

            // Pick up any uncollected output.
            GatherOutput(mixedOutput, stdout, stderr, bufferedStdout, bufferedStderr);

            String message = "Process exited cleanly: Return value = " + Integer.toString(ret);
            LOG.debug(message);
            if (null != timeLog) {
                timeLog.stamp(message);
            }
        }
        catch (IOException e) {
            String message = "I/O exception running command " + command.get(0);
            LOG.info(message);
            ret = 255;
            if (null != timeLog) {
                timeLog.stamp(message);
            }
        } catch (ProcessCanceledException e) {
            String message = "Copmpletion canceled.";
            LOG.debug(message);
            ret = 255;
            if (null!= timeLog) {
                timeLog.stamp(message);
            }
        }finally {
            if (null != process && processIsAlive(process)) {
                process.destroy();
            }
        }
        return ret;
    }

    private static void GatherOutput(boolean mixedOutput,
                                     List<String> stdout,
                                     List<String> stderr,
                                     BufferedReader bufferedStdout,
                                     BufferedReader bufferedStderr) throws IOException {
        ProgressManager.checkCanceled();

        while (bufferedStdout.ready()) {
            if (null != stdout) {
                stdout.add(bufferedStdout.readLine());
            }
            else {
                // Goes to the bit bucket.
                bufferedStdout.readLine();
            }
        }
        if (!mixedOutput) {
            while (bufferedStderr.ready()) {
                if (null != stderr) {
                    stderr.add(bufferedStderr.readLine());
                }
                else {
                    // bit bucket.
                    bufferedStderr.readLine();
                }
            }
        }
    }

    /**
     * Check whether the process is still alive the hard way,
     * Java8 gives us process.isAlive(), but Java6 doesn't.
     * @param process
     */
    private static boolean processIsAlive(Process process) {
        try {
            int exitStatus = process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            ; // swallow it.  The thread hasn't exited yet.
        }
        return true;
    }
}
