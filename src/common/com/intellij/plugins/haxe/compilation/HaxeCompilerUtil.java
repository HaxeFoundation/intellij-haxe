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
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil
{
    static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.compilation.HaxeCompilerUtil");
    {
        LOG.setLevel(Level.DEBUG);
    }

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
            String message = "Completion error: No " + type + " project type is specified in project settings.";  // TODO: Externalize string.
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
     * Send errors off to the appropriate handler.
     * @param message - Error message
     * @param notifier - Error handler
     */
    private static void advertiseError(String message, @Nullable ErrorNotifier notifier) {
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
}
