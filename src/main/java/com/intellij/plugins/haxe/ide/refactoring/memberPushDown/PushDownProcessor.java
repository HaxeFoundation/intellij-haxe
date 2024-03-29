/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.refactoring.memberPushDown;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.ChangeContextUtil;
import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.impl.CreateClassDialog;
import com.intellij.codeInsight.intention.impl.CreateSubclassAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeRefactoringBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeInterfaceDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.JavaRefactoringListenerManager;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.impl.JavaRefactoringListenerManagerImpl;
import com.intellij.refactoring.memberPushDown.JavaPushDownHandler;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.ide.refactoring.MoveUpDownUtil.collectRelatedDocsAndMetadata;
import static com.intellij.plugins.haxe.ide.refactoring.MoveUpDownUtil.addMetadataAndDocs;

@CustomLog
public class PushDownProcessor extends BaseRefactoringProcessor {

  private final MemberInfo[] myMemberInfos;
  private PsiClass myClass;
  private final DocCommentPolicy myJavaDocPolicy;
  private CreateClassDialog myCreateClassDlg;

  public PushDownProcessor(Project project,
                           MemberInfo[] memberInfos,
                           PsiClass aClass,
                           DocCommentPolicy javaDocPolicy) {
    super(project);
    myMemberInfos = memberInfos;
    myClass = aClass;
    myJavaDocPolicy = javaDocPolicy;
  }

  @Override
  protected String getCommandName() {
    return JavaPushDownHandler.getRefactoringName();
  }

  @Override
  @NotNull
  protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages) {
    return new PushDownUsageViewDescriptor(myClass);
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.push.down";
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myClass);
    data.addMembers(myMemberInfos, new Function<MemberInfo, PsiElement>() {
      @Override
      public PsiElement fun(MemberInfo info) {
        return info.getMember();
      }
    });
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(UsageInfo[] usages) {
    final List<PsiElement> elements = new ArrayList<PsiElement>();
    for (UsageInfo usage : usages) {
      PsiElement element = usage.getElement();
      if (element instanceof PsiClass) {
        elements.add(element);
      }
    }
    RefactoringEventData data = new RefactoringEventData();
    data.addElements(elements);
    return data;
  }

  @Override
  protected UsageInfo @NotNull [] findUsages() {
    GlobalSearchScope scope = GlobalSearchScope.projectScope(myClass.getProject());
    List<UsageInfo> usageInfos = Arrays.asList(MoveClassesOrPackagesUtil.findUsages(myClass, scope,false, false, null));
    final List<UsageInfo> usages = new ArrayList<UsageInfo>();
    PsiReference reference;
    PsiClass psiClass;

    for (int i = 0; i < usageInfos.size(); i++) {
      reference = usageInfos.get(i).getReference();
      if (reference != null) {
        psiClass = PsiTreeUtil.getParentOfType((PsiElement)reference, PsiClass.class);
        if (psiClass != null) {
          usages.add(new UsageInfo(psiClass));
        }
      }
    }

   /* final PsiMethod interfaceMethod = LambdaUtil.getFunctionalInterfaceMethod(myClass);
    if (interfaceMethod != null && isMoved(interfaceMethod)) {
      FunctionalExpressionSearch.search(myClass).forEach(new Processor<PsiFunctionalExpression>() {
        @Override
        public boolean process(PsiFunctionalExpression expression) {
          usages.add(new UsageInfo(expression));
          return true;
        }
      });
    }*/

    return usages.toArray(new UsageInfo[0]);
  }

  private boolean isMoved(PsiMember member) {
    for (MemberInfo info : myMemberInfos) {
      if (member == info.getMember()) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean preprocessUsages(final Ref<UsageInfo[]> refUsages) {
    final UsageInfo[] usagesIn = refUsages.get();
    final PushDownConflicts pushDownConflicts = new PushDownConflicts(myClass, myMemberInfos);
    pushDownConflicts.checkSourceClassConflicts();

    if (usagesIn.length == 0) {
      if (myClass.isEnum() || myClass.hasModifierProperty(PsiModifier.FINAL)) {
        String message = (myClass.isEnum()
                          ? "Enum " + myClass.getQualifiedName() + " doesn't have constants to inline to. "
                          : "Final class " + myClass.getQualifiedName() + "does not have inheritors. ") +
                         "Pushing members down will result in them being deleted. " +
                         "Would you like to proceed?";
        if (Messages.showOkCancelDialog(message, JavaPushDownHandler.getRefactoringName(),
                                        Messages.getOkButton(),
                                        Messages.getCancelButton(),
                                        Messages.getWarningIcon()) != Messages.OK) {
          return false;
        }
      } else {
        String noInheritors = myClass.isInterface() ?
                              HaxeRefactoringBundle.message("interface.0.does.not.have.inheritors", myClass.getQualifiedName()) :
                              RefactoringBundle.message("class.0.does.not.have.inheritors", myClass.getQualifiedName());
        Messages.showMessageDialog("No subclass to push down to ", JavaPushDownHandler.getRefactoringName(), Messages.getWarningIcon());
        return false;
        //TODO mlo this code seems to create a Java class (CreateSubclassAction),  need to create haxe equivalent

        //final String message = noInheritors + "\n" + RefactoringBundle.message("push.down.will.delete.members");
        //final int answer = Messages.showYesNoCancelDialog(message, JavaPushDownHandler.getRefactoringName(), Messages.getWarningIcon());
        //if (answer == Messages.YES) {
        //  myCreateClassDlg = CreateSubclassAction.chooseSubclassToCreate(myClass);
        //  if (myCreateClassDlg != null) {
        //    //pushDownConflicts.checkTargetClassConflicts(null, false, myCreateClassDlg.getTargetDirectory());
        //    //return showConflicts(pushDownConflicts.getConflicts(), usagesIn);
        //  } else {
        //    return false;
        //  }
        //} else if (answer != Messages.NO) return false;
      }
    }
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          @Override
          public void run() {
            for (UsageInfo usage : usagesIn) {
              final PsiElement element = usage.getElement();
              if (element instanceof PsiClass) {
                pushDownConflicts.checkTargetClassConflicts((PsiClass)element, usagesIn.length > 1, element);
              }
            }
          }
        });
      }
    };

    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, RefactoringBundle.message("detecting.possible.conflicts"), true, myProject)) {
      return false;
    }

    for (UsageInfo info : usagesIn) {
      final PsiElement element = info.getElement();
      if (element instanceof PsiFunctionalExpression) {
        pushDownConflicts.getConflicts().putValue(element, HaxeRefactoringBundle.message("functional.interface.broken"));
      }
    }
    final PsiAnnotation annotation = AnnotationUtil.findAnnotation(myClass, CommonClassNames.JAVA_LANG_FUNCTIONAL_INTERFACE);
    if (annotation != null && isMoved(LambdaUtil.getFunctionalInterfaceMethod(myClass))) {
      pushDownConflicts.getConflicts().putValue(annotation, HaxeRefactoringBundle.message("functional.interface.broken"));
    }
    return showConflicts(pushDownConflicts.getConflicts(), usagesIn);
  }

  @Override
  protected void refreshElements(PsiElement[] elements) {
    if(elements.length == 1 && elements[0] instanceof PsiClass) {
      myClass = (PsiClass) elements[0];
    }
    else {
      log.assertTrue(false, "Assertion failed");
    }
  }

  @Override
  protected void performRefactoring(UsageInfo[] usages) {
    try {
      encodeRefs();
      if (myCreateClassDlg != null) { //usages.length == 0
        final PsiClass psiClass =
          CreateSubclassAction.createSubclass(myClass, myCreateClassDlg.getTargetDirectory(), myCreateClassDlg.getClassName());
        if (psiClass != null) {
          pushDownToClass(psiClass);
        }
      }

      PsiReference reference;
      PsiElement resolve;

      for (UsageInfo usage : usages) {
        if (usage.getElement() instanceof PsiClass) {
          final PsiClass targetClass = (PsiClass)usage.getElement();
          pushDownToClass(targetClass);
        }
      }
      updateInTargetClass();
    }
    catch (IncorrectOperationException e) {
      log.error(e);
    }
  }

  private static final Key<Boolean> REMOVE_QUALIFIER_KEY = Key.create("REMOVE_QUALIFIER_KEY");
  private static final Key<PsiClass> REPLACE_QUALIFIER_KEY = Key.create("REPLACE_QUALIFIER_KEY");

  protected void encodeRefs() {
    final Set<PsiMember> movedMembers = new HashSet<PsiMember>();
    for (MemberInfo memberInfo : myMemberInfos) {
      movedMembers.add(memberInfo.getMember());
    }

    for (MemberInfo memberInfo : myMemberInfos) {
      final PsiMember member = memberInfo.getMember();
      member.accept(new JavaRecursiveElementVisitor() {
        @Override public void visitReferenceExpression(PsiReferenceExpression expression) {
          encodeRef(expression, movedMembers, expression);
          super.visitReferenceExpression(expression);
        }

        @Override public void visitNewExpression(PsiNewExpression expression) {
          final PsiJavaCodeReferenceElement classReference = expression.getClassReference();
          if (classReference != null) {
            encodeRef(classReference, movedMembers, expression);
          }
          super.visitNewExpression(expression);
        }

        @Override
        public void visitTypeElement(final PsiTypeElement type) {
          final PsiJavaCodeReferenceElement referenceElement = type.getInnermostComponentReferenceElement();
          if (referenceElement != null) {
            encodeRef(referenceElement, movedMembers, type);
          }
          super.visitTypeElement(type);
        }
      });
      ChangeContextUtil.encodeContextInfo(member, false);
    }
  }

  private void encodeRef(final PsiJavaCodeReferenceElement expression, final Set<PsiMember> movedMembers, final PsiElement toPut) {
    final PsiElement resolved = expression.resolve();
    if (resolved == null) return;
    final PsiElement qualifier = expression.getQualifier();
    for (PsiMember movedMember : movedMembers) {
      if (movedMember.equals(resolved)) {
        if (qualifier == null) {
          toPut.putCopyableUserData(REMOVE_QUALIFIER_KEY, Boolean.TRUE);
        } else {
          if (qualifier instanceof PsiJavaCodeReferenceElement &&
              ((PsiJavaCodeReferenceElement)qualifier).isReferenceTo(myClass)) {
            toPut.putCopyableUserData(REPLACE_QUALIFIER_KEY, myClass);
          }
        }
      } else if (movedMember instanceof PsiClass && PsiTreeUtil.getParentOfType(resolved, PsiClass.class, false) == movedMember) {
        if (qualifier instanceof PsiJavaCodeReferenceElement && ((PsiJavaCodeReferenceElement)qualifier).isReferenceTo(movedMember)) {
          toPut.putCopyableUserData(REPLACE_QUALIFIER_KEY, (PsiClass)movedMember);
        }
      } else {
        if (qualifier instanceof PsiThisExpression) {
          final PsiJavaCodeReferenceElement qElement = ((PsiThisExpression)qualifier).getQualifier();
          if (qElement != null && qElement.isReferenceTo(myClass)) {
            toPut.putCopyableUserData(REPLACE_QUALIFIER_KEY, myClass);
          }
        }
      }
    }
  }

  private void decodeRefs(final PsiMember member, final PsiClass targetClass) {
    try {
      ChangeContextUtil.decodeContextInfo(member, null, null);
    }
    catch (IncorrectOperationException e) {
      log.error(e);
    }

    final PsiElementFactory factory = JavaPsiFacade.getInstance(myProject).getElementFactory();
    member.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override public void visitReferenceExpression(PsiReferenceExpression expression) {
        decodeRef(expression, factory, targetClass, expression);
        super.visitReferenceExpression(expression);
      }

      @Override public void visitNewExpression(PsiNewExpression expression) {
        final PsiJavaCodeReferenceElement classReference = expression.getClassReference();
        if (classReference != null) decodeRef(classReference, factory, targetClass, expression);
        super.visitNewExpression(expression);
      }

      @Override
      public void visitTypeElement(final PsiTypeElement type) {
        final PsiJavaCodeReferenceElement referenceElement = type.getInnermostComponentReferenceElement();
        if (referenceElement != null)  decodeRef(referenceElement, factory, targetClass, type);
        super.visitTypeElement(type);
      }
    });
  }

  private void decodeRef(final PsiJavaCodeReferenceElement ref,
                         final PsiElementFactory factory,
                         final PsiClass targetClass,
                         final PsiElement toGet) {
    try {
      if (toGet.getCopyableUserData(REMOVE_QUALIFIER_KEY) != null) {
        toGet.putCopyableUserData(REMOVE_QUALIFIER_KEY, null);
        final PsiElement qualifier = ref.getQualifier();
        if (qualifier != null) qualifier.delete();
      }
      else {
        PsiClass psiClass = toGet.getCopyableUserData(REPLACE_QUALIFIER_KEY);
        if (psiClass != null) {
          toGet.putCopyableUserData(REPLACE_QUALIFIER_KEY, null);
          PsiElement qualifier = ref.getQualifier();
          if (qualifier != null) {

            if (psiClass == myClass) {
              psiClass = targetClass;
            } else if (psiClass.getContainingClass() == myClass) {
              psiClass = targetClass.findInnerClassByName(psiClass.getName(), false);
              log.assertTrue(psiClass != null, "Assertion failed");
            }

            if (!(qualifier instanceof PsiThisExpression) && ref instanceof PsiReferenceExpression) {
              ((PsiReferenceExpression)ref).setQualifierExpression(factory.createReferenceExpression(psiClass));
            }
            else {
              if (qualifier instanceof PsiThisExpression) {
                qualifier = ((PsiThisExpression)qualifier).getQualifier();
              }
              qualifier.replace(factory.createReferenceElementByType(factory.createType(psiClass)));
            }
          }
        }
      }
    }
    catch (IncorrectOperationException e) {
      log.error(e);
    }
  }

  private void updateInTargetClass() throws IncorrectOperationException {
    for (MemberInfo memberInfo : myMemberInfos) {
      final PsiElement member = memberInfo.getMember();

      if (member instanceof PsiField) {
        member.delete();
      }
      else if (member instanceof HaxeMethod method) {
        if (memberInfo.isToAbstract()) {
          if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
            PsiUtil.setModifierProperty(method, PsiModifier.PROTECTED, true);
          }
          PsiCodeBlock body = method.getBody();
          if (body != null) {
            body.replace(HaxeElementGenerator.createSemi(myProject).copy());
          }
          HaxeModifiersModel modifiers = method.getModel().getModifiers();
          if (!modifiers.hasModifier(HaxePsiModifier.ABSTRACT)) {
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(method);
            modifiers.addModifier(HaxePsiModifier.ABSTRACT);
          }
          //myJavaDocPolicy.processOldJavaDoc(method.getDocComment());
        }
        else {
          member.delete();
        }
      }
      else if (member instanceof PsiClass) {
        //if (Boolean.FALSE.equals(memberInfo.getOverrides())) {
          //RefactoringUtil.removeFromReferenceList(myClass.getImplementsList(), (PsiClass)member);
        //}
        //else {
        //}

        member.delete();
      }
    }
  }

  protected void pushDownToClass(PsiClass targetClass) throws IncorrectOperationException {
    final PsiElementFactory factory = JavaPsiFacade.getInstance(myClass.getProject()).getElementFactory();
    final PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(myClass, targetClass, PsiSubstitutor.EMPTY);
    for (MemberInfo memberInfo : myMemberInfos) {
      PsiMember member = memberInfo.getMember();
      final List<PsiReference> refsToRebind = new ArrayList<PsiReference>();
      final PsiModifierList list = member.getModifierList();
      log.assertTrue(list != null, "Assertion failed");
      if (list.hasModifierProperty(PsiModifier.STATIC)) {
        for (final PsiReference reference : ReferencesSearch.search(member)) {
          final PsiElement element = reference.getElement();
          if (element instanceof PsiReferenceExpression) {
            final PsiExpression qualifierExpression = ((PsiReferenceExpression)element).getQualifierExpression();
            if (qualifierExpression instanceof PsiReferenceExpression && !(((PsiReferenceExpression)qualifierExpression).resolve() instanceof PsiClass)) {
              continue;
            }
          }
          refsToRebind.add(reference);
        }
      }
      List<PsiElement> psiElements = collectRelatedDocsAndMetadata(member);
      member = (PsiMember)member.copy();
      RefactoringUtil.replaceMovedMemberTypeParameters(member, PsiUtil.typeParametersIterable(myClass), substitutor, factory);
      PsiMember newMember = null;
      if (member instanceof PsiField) {
        ((PsiField)member).normalizeDeclaration();
        if (myClass.isInterface() && !targetClass.isInterface()) {
          PsiUtil.setModifierProperty(member, PsiModifier.PUBLIC, true);
          PsiUtil.setModifierProperty(member, PsiModifier.STATIC, true);
          PsiUtil.setModifierProperty(member, PsiModifier.FINAL, true);
        }
        newMember = (PsiMember)targetClass.getRBrace().getParent().addBefore(member, targetClass.getRBrace());
        addMetadataAndDocs(psiElements, newMember, true);
      }
      else if (member instanceof PsiMethod) {
        PsiMethod method = (PsiMethod)member;
        PsiMethod methodBySignature = MethodSignatureUtil.findMethodBySuperSignature(targetClass, method.getSignature(substitutor), false);
        if (methodBySignature == null) {
          newMember = null;
          if (myClass.isInterface()) {
            if (!(targetClass instanceof HaxeInterfaceDeclaration)) {
              String text = member.getText();

              if (text.endsWith(";")) {
                text = text.substring(0, text.length() - 1) + " {}";
              }

              HaxeMethod functionDeclarationWithAttributes =
                HaxeElementGenerator.createMethodDeclaration(myProject, text);
              newMember = (PsiMethod)targetClass.addBefore(functionDeclarationWithAttributes, targetClass.getRBrace());
            }
            else {
              newMember = (PsiMethod)targetClass.addBefore(member, targetClass.getRBrace());
            }

            if (!targetClass.isInterface()) {
              PsiUtil.setModifierProperty(newMember, PsiModifier.PUBLIC, true);
              if (newMember.hasModifierProperty(PsiModifier.DEFAULT)) {
                PsiUtil.setModifierProperty(newMember, PsiModifier.DEFAULT, false);
              }
              else {
                PsiUtil.setModifierProperty(newMember, PsiModifier.ABSTRACT, true);
              }
            }

            reformat(newMember);
          }
          else if (memberInfo.isToAbstract()) {
            PsiElement brace = targetClass.getRBrace();
            newMember = (PsiMember)brace.getParent().addBefore(method, brace);
            if (newMember.hasModifierProperty(PsiModifier.PRIVATE)) {
              PsiUtil.setModifierProperty(newMember, PsiModifier.PROTECTED, true);
            }
            addMetadataAndDocs(psiElements, newMember, false);
            //myJavaDocPolicy.processNewJavaDoc(((PsiMethod)newMember).getDocComment());
          }
          else {
            String text = member.getText();

            if (text.endsWith(";")) {
              text = text.substring(0, text.length() - 1) + " {}";
            }


            newMember = HaxeElementGenerator.createMethodDeclaration(myProject, text);
            PsiMember element = (PsiMember)targetClass.getRBrace().getParent().addBefore(newMember, targetClass.getRBrace());
            addMetadataAndDocs(psiElements, element, true);
            reformat(newMember);
          }
        }
        else { //abstract method: remove @Override
          final PsiAnnotation annotation = AnnotationUtil.findAnnotation(methodBySignature, "java.lang.Override");
          if (annotation != null && !leaveOverrideAnnotation(substitutor, method)) {
            annotation.delete();
          }
          final PsiDocComment oldDocComment = method.getDocComment();
          if (oldDocComment != null) {
            final PsiDocComment docComment = methodBySignature.getDocComment();
            final int policy = myJavaDocPolicy.getJavaDocPolicy();
            if (policy == DocCommentPolicy.COPY || policy == DocCommentPolicy.MOVE) {
              if (docComment != null) {
                docComment.replace(oldDocComment);
              }
              else {
                methodBySignature.getParent().addBefore(oldDocComment, methodBySignature);
              }
            }
          }
        }
      }
      else if (member instanceof PsiClass) {
        if (Boolean.FALSE.equals(memberInfo.getOverrides())) {
          final PsiClass aClass = (PsiClass)memberInfo.getMember();
          PsiClassType classType = null;
          if (!targetClass.isInheritor(aClass, false)) {
            final PsiClassType[] types = memberInfo.getSourceReferenceList().getReferencedTypes();
            for (PsiClassType type : types) {
              if (type.resolve() == aClass) {
                classType = (PsiClassType)substitutor.substitute(type);
              }
            }
            PsiJavaCodeReferenceElement classRef = classType != null ? factory.createReferenceElementByType(classType) : factory.createClassReferenceElement(aClass);
            if (aClass.isInterface()) {
              targetClass.getImplementsList().add(classRef);
            } else {
              targetClass.getExtendsList().add(classRef);
            }
          }
        }
        else {
          newMember = (PsiMember)targetClass.add(member);
        }
      }

      if (newMember != null) {
        decodeRefs(newMember, targetClass);
        //rebind imports first
        Collections.sort(refsToRebind, new Comparator<PsiReference>() {
          @Override
          public int compare(PsiReference o1, PsiReference o2) {
            return PsiUtil.BY_POSITION.compare(o1.getElement(), o2.getElement());
          }
        });
        for (PsiReference psiReference : refsToRebind) {
          JavaCodeStyleManager.getInstance(myProject).shortenClassReferences(psiReference.bindToElement(newMember));
        }
        final JavaRefactoringListenerManager listenerManager = JavaRefactoringListenerManager.getInstance(newMember.getProject());
        ((JavaRefactoringListenerManagerImpl)listenerManager).fireMemberMoved(myClass, newMember);
      }
    }
  }

  private void reformat(final PsiMember movedElement) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final TextRange range = movedElement.getTextRange();
        final PsiFile file = movedElement.getContainingFile();
        final PsiFile baseFile = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());
        CodeStyleManager.getInstance(myProject).reformatText(baseFile, range.getStartOffset(), range.getEndOffset());
      }
    });
  }

  private boolean leaveOverrideAnnotation(PsiSubstitutor substitutor, PsiMethod method) {
    /*final PsiMethod methodBySignature = MethodSignatureUtil.findMethodBySignature(myClass, method.getSignature(substitutor), false);
    if (methodBySignature == null) return false;
    final PsiMethod[] superMethods = methodBySignature.findDeepestSuperMethods();
    if (superMethods.length == 0) return false;
    final boolean is15 = !PsiUtil.isLanguageLevel6OrHigher(methodBySignature);
    if (is15) {
      for (PsiMethod psiMethod : superMethods) {
        final PsiClass aClass = psiMethod.getContainingClass();
        if (aClass != null && aClass.isInterface()) {
          return false;
        }
      }
    }*/
    return true;
  }
}
