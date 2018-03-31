/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.refactoring.extractSuperclass;

import com.intellij.codeInsight.generation.OverrideImplementExploreUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.refactoring.memberPullUp.PullUpProcessor;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeInheritList;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.webcore.ModuleHelper;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author dsl
 */
public class ExtractSuperClassUtil {
  private static final Logger LOG = Logger.getInstance("com.intellij.refactoring.extractSuperclass.ExtractSuperClassUtil");
  public static final String REFACTORING_EXTRACT_SUPER_ID = "refactoring.extractSuper";

  private ExtractSuperClassUtil() {}

  public static PsiClass extractSuperClass(final Project project,
                                           final PsiDirectory targetDirectory,
                                           final String superclassName,
                                           final PsiClass subclass,
                                           final MemberInfo[] selectedMemberInfos,
                                           final DocCommentPolicy javaDocPolicy)
    throws IncorrectOperationException {

    project.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
      .refactoringStarted(REFACTORING_EXTRACT_SUPER_ID, createBeforeData(subclass, selectedMemberInfos));

    //final PsiClass superclass = JavaDirectoryService.getInstance().createClass(targetDirectory, superclassName);

    /*for (FileTemplate template : FileTemplateManager.getInstance().getInternalTemplates()) {
      Logger.getInstance(ExtractSuperClassUtil.class).error(template.getName());
    }*/

    Module[] modules = ModuleHelper.getModules(project);
    //PsiClass superclass = CreateClassUtil.createClassFromCustomTemplate(targetDirectory, modules[0], superclassName, "HaxeClass.hx.ft");
    String packageName = DirectoryIndex.getInstance(targetDirectory.getProject()).getPackageName(targetDirectory.getVirtualFile());
    PsiClass superclass = null;
    try {
      HaxeFile haxeFile = (HaxeFile)HaxeFileTemplateUtil.createClass(superclassName, packageName, targetDirectory, "HaxeClass", null);
      PsiClass[] classes = haxeFile.getClasses();

      for (PsiClass psiClass : classes) {
        if (psiClass.getName().equals(superclassName)) {
          superclass = psiClass;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    try {
      final PsiModifierList superClassModifierList = superclass.getModifierList();
      assert superClassModifierList != null;
      //superClassModifierList.setModifierProperty(PsiModifier.FINAL, false);
      //superClassModifierList.setModifierProperty(PsiModifier.PUBLIC, false);
      final PsiReferenceList subClassExtends = subclass.getExtendsList();
      if (subClassExtends != null) {
        copyPsiReferenceList(subClassExtends, superclass.getExtendsList());
      } else if (subclass instanceof PsiAnonymousClass) {
        superclass.getExtendsList().add(((PsiAnonymousClass)subclass).getBaseClassReference());
      }

      // create constructors if neccesary
      PsiMethod[] constructors = getCalledBaseConstructors(subclass);
      if (constructors.length > 0) {
        createConstructorsByPattern(project, superclass, constructors);
      }

      // clear original class' "extends" list
      if (subClassExtends != null) {
        clearPsiReferenceList(subclass.getExtendsList());
      }

      // make original class extend extracted superclass
      //PsiReferenceList extendsList = subclass.getExtendsList();

      if (PsiTreeUtil.getChildOfType(subclass, HaxeInheritList.class) == null) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(subclass.getContainingFile());
        document.insertString(subclass.getNameIdentifier().getTextRange().getEndOffset(), " extends " + superclassName);
      }

      PsiJavaCodeReferenceElement ref = createExtendingReference(superclass, subclass, selectedMemberInfos);
      if (subClassExtends != null) {
        subclass.getExtendsList().add(ref);
      } else if (subclass instanceof PsiAnonymousClass) {
        ((PsiAnonymousClass)subclass).getBaseClassReference().replace(ref);
      }

      PullUpProcessor pullUpHelper = new PullUpProcessor(subclass, superclass, selectedMemberInfos,
                                                   javaDocPolicy
      );

      pullUpHelper.moveMembersToBase();
      pullUpHelper.moveFieldInitializations();

      Collection<MethodSignature> toImplement = OverrideImplementExploreUtil.getMethodSignaturesToImplement(superclass);
      if (!toImplement.isEmpty()) {
        superClassModifierList.setModifierProperty(PsiModifier.ABSTRACT, true);
      }
      return superclass;
    }
    finally {
      project.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(REFACTORING_EXTRACT_SUPER_ID, createAfterData(superclass));
    }
  }

  private static void createConstructorsByPattern(Project project, final PsiClass superclass, PsiMethod[] patternConstructors) throws IncorrectOperationException {
    PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
    for (PsiMethod baseConstructor : patternConstructors) {
      PsiMethod constructor = (PsiMethod)superclass.add(factory.createConstructor());
      PsiParameterList paramList = constructor.getParameterList();
      PsiParameter[] baseParams = baseConstructor.getParameterList().getParameters();
      @NonNls StringBuilder superCallText = new StringBuilder();
      superCallText.append("super(");
      final PsiClass baseClass = baseConstructor.getContainingClass();
      LOG.assertTrue(baseClass != null);
      final PsiSubstitutor classSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(baseClass, superclass, PsiSubstitutor.EMPTY);
      for (int i = 0; i < baseParams.length; i++) {
        final PsiParameter baseParam = baseParams[i];
        final PsiParameter newParam = (PsiParameter)paramList.add(factory.createParameter(baseParam.getName(), classSubstitutor.substitute(baseParam.getType())));
        if (i > 0) {
          superCallText.append(",");
        }
        superCallText.append(newParam.getName());
      }
      superCallText.append(");");
      PsiStatement statement = factory.createStatementFromText(superCallText.toString(), null);
      statement = (PsiStatement)styleManager.reformat(statement);
      final PsiCodeBlock body = constructor.getBody();
      assert body != null;
      body.add(statement);
      constructor.getThrowsList().replace(baseConstructor.getThrowsList());
    }
  }

  private static PsiMethod[] getCalledBaseConstructors(final PsiClass subclass) {
    Set<PsiMethod> baseConstructors = new HashSet<PsiMethod>();
    PsiMethod[] constructors = subclass.getConstructors();
    for (PsiMethod constructor : constructors) {
      PsiCodeBlock body = constructor.getBody();
      if (body == null) continue;
      PsiStatement[] statements = body.getStatements();
      if (statements.length > 0) {
        PsiStatement first = statements[0];
        if (first instanceof PsiExpressionStatement) {
          PsiExpression expression = ((PsiExpressionStatement)first).getExpression();
          if (expression instanceof PsiMethodCallExpression) {
            PsiReferenceExpression calledMethod = ((PsiMethodCallExpression)expression).getMethodExpression();
            @NonNls String text = calledMethod.getText();
            if ("super".equals(text)) {
              PsiMethod baseConstructor = (PsiMethod)calledMethod.resolve();
              if (baseConstructor != null) {
                baseConstructors.add(baseConstructor);
              }
            }
          }
        }
      }
    }
    return baseConstructors.toArray(new PsiMethod[baseConstructors.size()]);
  }

  private static void clearPsiReferenceList(PsiReferenceList refList) throws IncorrectOperationException {
    PsiJavaCodeReferenceElement[] refs = refList.getReferenceElements();
    for (PsiJavaCodeReferenceElement ref : refs) {
      ref.delete();
    }
  }

  private static void copyPsiReferenceList(PsiReferenceList sourceList, PsiReferenceList destinationList) throws IncorrectOperationException {
    clearPsiReferenceList(destinationList);
    PsiJavaCodeReferenceElement[] refs = sourceList.getReferenceElements();
    for (PsiJavaCodeReferenceElement ref : refs) {
      destinationList.add(ref);
    }
  }

  public static PsiJavaCodeReferenceElement createExtendingReference(final PsiClass superClass,
                                                                      final PsiClass derivedClass,
                                                                      final MemberInfo[] selectedMembers) throws IncorrectOperationException {
    final PsiManager manager = derivedClass.getManager();
    Set<PsiElement> movedElements = new com.intellij.util.containers.HashSet<PsiElement>();
    for (final MemberInfo info : selectedMembers) {
      movedElements.add(info.getMember());
    }
    final Condition<PsiTypeParameter> filter = new Condition<PsiTypeParameter>() {
      @Override
      public boolean value(PsiTypeParameter parameter) {
        return findTypeParameterInDerived(derivedClass, parameter.getName()) == parameter;
      }
    };
    final PsiTypeParameterList typeParameterList = RefactoringUtil.createTypeParameterListWithUsedTypeParameters(null, filter, PsiUtilCore.toPsiElementArray(movedElements));
    final PsiTypeParameterList originalTypeParameterList = superClass.getTypeParameterList();
    assert originalTypeParameterList != null;
    //final PsiTypeParameterList newList = typeParameterList != null ? (PsiTypeParameterList)originalTypeParameterList.replace(typeParameterList) : originalTypeParameterList;
    final PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
    Map<PsiTypeParameter, PsiType> substitutionMap = new HashMap<PsiTypeParameter, PsiType>();
    //for (final PsiTypeParameter parameter : newList.getTypeParameters()) {
    //  final PsiTypeParameter parameterInDerived = findTypeParameterInDerived(derivedClass, parameter.getName());
    //  if (parameterInDerived != null) {
    //    substitutionMap.put(parameter, factory.createType(parameterInDerived));
    //  }
    //}

    final PsiClassType type = factory.createType(superClass, factory.createSubstitutor(substitutionMap));
    return factory.createReferenceElementByType(type);
  }

  @Nullable
  public static PsiTypeParameter findTypeParameterInDerived(final PsiClass aClass, final String name) {
    for (PsiTypeParameter typeParameter : PsiUtil.typeParametersIterable(aClass)) {
      if (name.equals(typeParameter.getName())) return typeParameter;
    }

    return null;
  }

  public static void checkSuperAccessible(PsiDirectory targetDirectory, MultiMap<PsiElement, String> conflicts, final PsiClass subclass) {
    final VirtualFile virtualFile = subclass.getContainingFile().getVirtualFile();
    if (virtualFile != null) {
      final boolean inTestSourceContent = ProjectRootManager.getInstance(subclass.getProject()).getFileIndex().isInTestSourceContent(virtualFile);
      final Module module = ModuleUtil.findModuleForFile(virtualFile, subclass.getProject());
      if (targetDirectory != null &&
          module != null &&
          !GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, inTestSourceContent).contains(targetDirectory.getVirtualFile())) {
        conflicts.putValue(subclass, "Superclass won't be accessible in subclass");
      }
    }
  }

  public static boolean showConflicts(DialogWrapper dialog, MultiMap<PsiElement, String> conflicts, final Project project) {
    if (!conflicts.isEmpty()) {
      fireConflictsEvent(conflicts, project);
      ConflictsDialog conflictsDialog = new ConflictsDialog(project, conflicts);
      conflictsDialog.show();
      final boolean ok = conflictsDialog.isOK();
      if (!ok && conflictsDialog.isShowConflicts()) dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
      return ok;
    }
    return true;
  }

  private static void fireConflictsEvent(MultiMap<PsiElement, String> conflicts, Project project) {
    final RefactoringEventData conflictUsages = new RefactoringEventData();
    conflictUsages.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
    project.getMessageBus()
      .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
      .conflictsDetected(REFACTORING_EXTRACT_SUPER_ID, conflictUsages);
  }

  public static RefactoringEventData createBeforeData(final PsiClass subclassClass, final MemberInfo[] members) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(subclassClass);
    data.addMembers(members, new Function<MemberInfo, PsiElement>() {
      @Override
      public PsiElement fun(MemberInfo info) {
        return info.getMember();
      }
    });
    return data;
  }

  public static RefactoringEventData createAfterData(final PsiClass subClass) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(subClass);
    return data;
  }
}
