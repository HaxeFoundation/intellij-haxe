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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.compilation.HaxeCompilerServices;
import com.intellij.plugins.haxe.compilation.HaxeCompilerUtil;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by as3boyan on 25.11.14.
 */
public class HaxeCompilerCompletionContributor extends CompletionContributor {

  static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#HaxeCompilerCompletionContributor");

  // Take this out when finished debugging.
  //static {
  //  LOG.setLevel(org.apache.log4j.Level.DEBUG);
  //}

  private String myErrorMessage = null;
  private Project myProject;


  public HaxeCompilerCompletionContributor() {

    final HaxeCompilerServices compilerServices = new HaxeCompilerServices(new HaxeCompilerUtil.ErrorNotifier() {
      @Override
      public void notifyError(String message) {
        advertiseError(message);
        // No super call.
      }
    });

    //Trigger completion only on HaxeReferenceExpressions
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HaxeTokenTypes.ID)
             .withParent(HaxeIdentifier.class)
             .withSuperParent(2, HaxeReferenceExpression.class),

           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               PsiFile    file    = parameters.getOriginalFile();

               // Shortcut if we aren't using compiler completions.  Do this here instead
               // of before the extend call. Otherwise, we'd have to restart if the state changes.
               if (!useCompilerCompletion(file)) {
                 LOG.debug("Skipping compiler completion.");
                 return;
               }

               PsiElement element = parameters.getPosition();
               Editor     editor  = parameters.getEditor();
               myProject          = file.getProject();

               List<HaxeCompilerCompletionItem> completions =
                   compilerServices.getPossibleCompletions(file, element, editor);
               for (HaxeCompilerCompletionItem completion : completions) {
                 result.addElement(completion.toLookupElement());
               }

               // Add the error message to the result advertisement (the help line at
               // the bottom of the selection dropdown).
               if (null != myErrorMessage && ! myErrorMessage.isEmpty()) {
                 result.addLookupAdvertisement(myErrorMessage);
               }
             }
           });
  }

  private boolean useCompilerCompletion(PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (null != vFile) { // Can't use in-memory file. TODO: Allow in-memory file for compiler completion.
      Module module = ModuleUtil.findModuleForFile(vFile, file.getProject());

      HaxeSdkAdditionalDataBase sdkData = HaxeSdkUtil.getSdkData(module);
      if (null != sdkData) {
        return sdkData.getUseCompilerCompletionFlag();
      }
    }
    return false;
  }


  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    // Clear any old error messages
    myErrorMessage = null;

    if (useCompilerCompletion(context.getFile())) {
      // TODO: Don't save if we can use the compiler stdin.  (Haxe version 3.4)
      saveEditsToDisk(context.getFile().getVirtualFile());
    }
    super.beforeCompletion(context);
  }

  @Nullable
  @Override
  public String handleEmptyLookup(@NotNull CompletionParameters parameters, Editor editor) {
    if (null != myErrorMessage) {
      return myErrorMessage;
    }
    return super.handleEmptyLookup(parameters, editor);
  }

  private void advertiseError(String message) {
    if (null == myErrorMessage || myErrorMessage.isEmpty()) {
      // Stash only the first error message, and we'll add it to the result set when we're finished.
      myErrorMessage = message;
    }

    LOG.info(message);  // XXX - May happen to often.
    StatusBarUtil.setStatusBarInfo(myProject, message);
  }

  /**
   * Save the file contents to disk so that the compiler has the correct data to work with.
   */
  private void saveEditsToDisk(VirtualFile file) {
    final Document doc = FileDocumentManager.getInstance().getDocument(file);
    if (FileDocumentManager.getInstance().isDocumentUnsaved(doc)) {
      final Application application = ApplicationManager.getApplication();
      if (application.isDispatchThread()) {
        LOG.debug("Saving buffer to disk...");
        FileDocumentManager.getInstance().saveDocumentAsIs(doc);
      } else {
        LOG.debug("Not on dispatch thread and document is unsaved.");
      }
    }
  }

}
