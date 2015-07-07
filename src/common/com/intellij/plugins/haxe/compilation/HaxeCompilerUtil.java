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
package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil
{
    public static final String ERROR = "Error: ";
    
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

    private static com.intellij.openapi.util.Key messageWindowAutoOpened =
        new com.intellij.openapi.util.Key("messageWindowAutoOpened");
}
