/*
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeRefactoringBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.ui.Messages.*;

/**
 * Handle renaming of elements.  This class accomplishes two things: it ensures that the default
 * Java handling isn't used (which magically makes things work); and it deals with the vagaries
 * of the Haxe language.
 *
 * The trouble is that the Java handling can't be completely circumvented.  So, we still have to
 * deal with issues around renaming constructors.  There are comments interspersed where we are
 * dealing with it.
 */
public class HaxeRenameProcessor extends RenamePsiElementProcessor {
  @TestOnly
  public static int alsoRenameAnswer = MessageConstants.YES;
  /**
   * Specifies whether a specific instance of an element can be
   * renamed.
   *
   * @param element Renaming candidate element instance.
   * @return true if the element can be renamed, false if not.
   */
  public static boolean canBeRenamed(PsiElement element) {
    if (element instanceof HaxeMethodPsiMixin) {
      return !((HaxeMethodPsiMixin)element).isConstructor();
    }
    if (element instanceof HaxeComponentName) {
      PsiElement parent = element.getParent();
      if (parent instanceof HaxeMethodPsiMixin) {
        return !((HaxeMethodPsiMixin)parent).isConstructor();
      }
    }

    return true;
  }


  public HaxeRenameProcessor() {
  }

  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    // NOTE: We should *NOT* use canBeRenamed() as part of this call.  Doing so
    //       will cause other (Java, Pom) renaming processors to be used instead.
    //       That causes renaming to appear to work, but with errors that get
    //       blamed on this plugin.

    return element.getLanguage().isKindOf(HaxeLanguage.INSTANCE);
  }



  @NotNull
  @Override
  public Collection<PsiReference> findReferences(@NotNull PsiElement element,
                                                 @NotNull SearchScope searchScope,
                                                 boolean searchInCommentsAndStrings) {
    // To stop constructors from being renamed, this is the place where we would, ultimately, like to
    // prevent the inclusion of the constructor itself.  However, the RenameJavaClassProcessor *also*
    // tries to handle the rename and we can't preempt the constructor being included via that code path.

      return super.findReferences(element, searchScope, searchInCommentsAndStrings);
  }


  @Nullable
  @Override
  public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {

    // If the element selected is a "new" statement, then we really want to rename the class, not the constructor.
    if (element instanceof HaxeMethodDeclaration methodDeclaration) {
        if (methodDeclaration.isConstructor()) {
        return methodDeclaration.getContainingClass();
      }
    }

    // If the element cannot be renamed, then stop the process here. (Returning null does that.)
    //
    // Note: This will pre-empt the dialogs, which may be confusing to the user.  We could put up a
    // dialog or a message when we refuse to run.  However, *nothing* else does that when rename is
    // inappropriate.  It just quietly ignores the request.  The difference in most of those places
    // is that the Refactor->Rename menu item is greyed out, which we can't accomplish without
    // replacing the system-wide rename processor.  And we don't want to do that because we will
    // conflict with any other plugin that may want to do so.  (A chaining protocol would have been
    // nice here...)
    //
    if (!canBeRenamed(element)) {
      return null;
    }

    PsiElement substitute = super.substituteElementToRename(element, editor);
    return canBeRenamed(substitute) ? substitute : null;
  }

  @Override
  public void renameElement(@NotNull PsiElement element,
                            @NotNull String newName,
                            @NotNull UsageInfo[] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    // Here, the element is actually being renamed.  If it shouldn't be renamed, then we'll just skip it.
    // We have to do so, because the element to be renamed is still on the list of elements due to
    // the PomRenameProcessor stating that it could handle the rename after we've already substituted the
    // proper elements.  But since we are handling it anyway the PomRenameProcessor is cut out of the loop.
    if (canBeRenamed(element)) {
      super.renameElement(element, newName, usages, listener);
    }
  }



  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    if(element instanceof HaxeComponentName componentName) {
      if(componentName.getParent() instanceof HaxeClass haxeClass) {
        element = haxeClass;
      }
    }

    if (element instanceof HaxeModule haxeModule) {
      addRenameFile(allRenames, haxeModule, newName);
      HaxeClass mainClass = findMainClass(haxeModule);
      if(mainClass != null) {
        int response = askRenameMain(mainClass, "module");
        if(response == MessageConstants.YES) {
           addRenameMain(allRenames, mainClass, newName);
        }else if(response == MessageConstants.CANCEL) {
          allRenames.clear();
          return;
        }
      }
    }
    else  if(element instanceof HaxeFile haxeFile) {
      HaxeModule module = findModule(haxeFile);
      addRenameModule(allRenames, module, newName);
      HaxeClass mainClass = findMainClass(module);
      if(mainClass != null) {
        int response = askRenameMain(mainClass, "file");
        if (response == MessageConstants.YES) {
          addRenameMain(allRenames, mainClass, dropFileExtension(newName));
        } else if (response == MessageConstants.CANCEL) {
          allRenames.clear();
          return;
        }
      }
    } else if (element instanceof HaxeClass haxeClass) {
      if(isMainClass(haxeClass)) {
        HaxeModule module = findModule(haxeClass);
        boolean gotMembers = hasModuleMembers(module);
        if (gotMembers) {
          int response = askRenameModule(module);
          if (response == MessageConstants.YES) {
            addRenameFile(allRenames, module, newName);
            addRenameModule(allRenames, module, newName);
            //TODO if answer is NO, then references might have to add module name to be correctly resolved
          } else if (response == MessageConstants.CANCEL) {
            allRenames.clear();
            return;
          }
        }else {
          // if no other module members are present, we also want to rename the file
          addRenameFile(allRenames, module, newName);
        }
      }
    }

    super.prepareRenaming(element, newName, allRenames);
  }

  @Override
  public void findExistingNameConflicts(@NotNull PsiElement element,
                                        @NotNull String newName,
                                        @NotNull MultiMap<PsiElement, @NlsContexts.DialogMessage String> conflicts,
                                        @NotNull Map<PsiElement, String> allRenames) {
    allRenames.forEach((psiElement, name) ->  findConflict(conflicts, psiElement, name));
  }


  private void findConflict(@NotNull MultiMap<PsiElement, @NlsContexts.DialogMessage String> conflicts, PsiElement element, String newName) {
    if (element instanceof HaxeComponentName componentName) {
      element = componentName.getParent();
    }

    if (element instanceof HaxeClass haxeClass) {
      HaxeClassModel model = haxeClass.getModel();
      FullyQualifiedInfo info = model.getQualifiedInfo();
      if (info != null) {
        FullyQualifiedInfo newQname = info.withClassName(newName);
        HaxeClass resolvedClass = HaxeResolveUtil.findClassByQName(newQname.toString(), element);
        if (resolvedClass != null) {
          conflicts.putValue(resolvedClass, HaxeRefactoringBundle.message("class.0.already.exists", newName));
        }
      }
    } else if (element instanceof HaxeMethod method) {
      HaxeMethodModel model = method.getModel();
      FullyQualifiedInfo info = model.getQualifiedInfo();
      if (info != null) {
        FullyQualifiedInfo newQname = info.withMemberName(newName);
        PsiElement resolved = HaxeResolveUtil.findClassOrMemberByQName(newQname.toString(), element);
        if (resolved instanceof HaxeMethod) {
          conflicts.putValue(resolved, HaxeRefactoringBundle.message("method.0.already.exists", newName));
        }
      }
    } else if (element instanceof HaxePsiField field) {
      HaxeBaseMemberModel model = field.getModel();
      FullyQualifiedInfo info = model.getQualifiedInfo();
      if (info != null) {
        FullyQualifiedInfo newQname = info.withMemberName(newName);
        PsiElement resolved = HaxeResolveUtil.findClassOrMemberByQName(newQname.toString(), element);
        if (resolved instanceof HaxePsiField) {
          conflicts.putValue(resolved, HaxeRefactoringBundle.message("field.0.already.exists", newName));
        }
      }
    }
  }






  private int askRenameModule(HaxeModule module) {
    if(ApplicationManager.getApplication().isUnitTestMode()) {
      return alsoRenameAnswer;
    }
    return showYesNoCancelDialog(module.getProject(),
            HaxeRefactoringBundle.message("also.rename.module.message"),
            HaxeRefactoringBundle.message("also.rename.module.title"),
            getYesButton(), getNoButton(), getCancelButton(),
            getQuestionIcon());

  }

  private int askRenameMain(PsiElement mainClass, String type) {
    if(ApplicationManager.getApplication().isUnitTestMode()) {
      return alsoRenameAnswer;
    }
    return showYesNoCancelDialog(mainClass.getProject(),
            HaxeRefactoringBundle.message("also.rename.class.message", type),
            HaxeRefactoringBundle.message("also.rename.class.title"),
            getYesButton(), getNoButton(),getCancelButton(),
            getQuestionIcon());
  }

  private void addRenameFile(@NotNull Map<PsiElement, String> allRenames, HaxeModule haxeModule, @NotNull String newName) {
    HaxeFile parentOfType = PsiTreeUtil.getStubOrPsiParentOfType(haxeModule, HaxeFile.class);
    String name = parentOfType.getName();
    int end = name.lastIndexOf(".");
    String fileExtension = name.substring(end);
    allRenames.put(parentOfType, newName + fileExtension);
  }

  private void addRenameMain(@NotNull Map<PsiElement, String> allRenames, PsiElement mainClass, @NotNull String newName) {
    allRenames.put(mainClass, newName);

  }
  private void addRenameModule(@NotNull Map<PsiElement, String> allRenames, HaxeModule module, @NotNull String newName) {
    allRenames.put(module, newName);
  }

  private @NotNull String dropFileExtension(@NotNull String newName) {
    int end = newName.lastIndexOf(".");
    return newName.substring(0, end);
  }

  private boolean hasModuleMembers(HaxeModule module) {
    if (module.getModel() instanceof HaxeModuleModel model) {
      for (HaxeModel haxeModel : model.getExposedMembers()) {
        PsiElement psi = haxeModel.getBasePsi();
        if (psi instanceof HaxeModuleFieldDeclaration || psi instanceof HaxeModuleMethodDeclaration) {
          return true;
        }
      }

      List<HaxeClassModel> classes = model.getClasses();
      classes.remove(model.getMainClass());
      return !classes.isEmpty();
    }
    return false;
  }

  private boolean isMainClass(HaxeClass haxeClass) {
    HaxeModule module = findModule(haxeClass);
    return findMainClass(module) == haxeClass;
  }

  private HaxeModule findModule(PsiElement element) {
    return PsiTreeUtil.getChildOfType(element.getContainingFile(), HaxeModule.class);
  }

  private HaxeClass findMainClass(HaxeModule haxeModule) {
    if(haxeModule.getModel() instanceof HaxeModuleModel model) {
      HaxeClassModel mainClass = model.getMainClass();
      if(mainClass != null) return mainClass.haxeClass;
    }
    return null;

  }
}
