package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class HaxeUnresolvedSymbolIntentionBase<T extends PsiElement> extends LocalQuickFixAndIntentionActionOnPsiElement {

  protected final @NotNull SmartPsiElementPointer<T> myPsiElementPointer;

  public HaxeUnresolvedSymbolIntentionBase(@NotNull T element) {
    super(element);
    myPsiElementPointer = createPointer(element);
  }

   protected <T extends PsiElement> @NotNull SmartPsiElementPointer<T> createPointer(@NotNull T element) {
    return SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
  }

  @Override
  public @NotNull @IntentionFamilyName String getFamilyName() {
    return getText();
  }



  public static @Nullable HaxeClass getTargetClass(HaxeReferenceExpression expression) {
    @NotNull PsiElement[] children = expression.getChildren();

    if(children.length == 1) {
      return PsiTreeUtil.getParentOfType(expression, HaxeClass.class);
    }else if (children[0] instanceof HaxeReference refChild) {
      HaxeExpressionEvaluatorContext evaluate = HaxeExpressionEvaluator.evaluate(refChild, null);
      ResultHolder result = evaluate.result;
      if(!result.isUnknown() && result.isClassType()) {
        if (result.getClassType() != null)return result.getClassType().getHaxeClass();
      }
    }
    return null;
  }



  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {

    PsiElement element = myPsiElementPointer.getElement();

    PsiElement original = copyFileAndReturnClonedPsiElement(element);
    PsiElement copy = copyFileAndReturnClonedPsiElement(element);

    copy = perform(project, copy, editor, true);
    // this might not be the best solution but it seems to work.
    // in order to get the correct line-number we need to compare the entire file and to only get the changes we need identical formatting
    String originalFormatted = CodeStyleManager.getInstance(project).reformat(original.getContainingFile(), true).getText();
    String copyReformated = CodeStyleManager.getInstance(project).reformat(copy.getContainingFile(), true).getText();


    return new IntentionPreviewInfo.CustomDiff(HaxeFileType.INSTANCE, getPreviewName(), originalFormatted, copyReformated, true);
  }

  protected String getPreviewName() {
    return null;
  }


  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    perform(project, myPsiElementPointer.getElement(), editor, false);
  }

  protected abstract PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview);



  protected boolean needsToBeStatic() {
    HaxeMethodDeclaration type = PsiTreeUtil.getParentOfType(myPsiElementPointer.getElement(), HaxeMethodDeclaration.class);
    if (type != null) {
      return type.getModel().isStatic();
    }

    HaxeFieldDeclaration field = PsiTreeUtil.getParentOfType(myPsiElementPointer.getElement(), HaxeFieldDeclaration.class);
    if (field != null) {
      return ((HaxeFieldModel)field.getModel()).isStatic();
    }
    return false;
  }

  protected <T extends PsiElement> T copyFileAndReturnClonedPsiElement(T psiElement) {
    PsiFile originalFile = psiElement.getContainingFile();
    PsiFile fileCopy = (PsiFile)originalFile.copy();
    PsiElement element = fileCopy.findElementAt(psiElement.getTextOffset());
    while(element != null
          && (!element.getTextRange().equals(psiElement.getTextRange())
              || element.getClass() != psiElement.getClass())
    ) {
      element = element.getParent();
    }
    return (T)element;
  }


  protected PsiElement createNewLine(@NotNull Project project) {
    return PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n").copy();
  }

  protected String guessElementType() {
    PsiElement parent = myPsiElementPointer.getElement().getParent();
    if (parent instanceof HaxeCallExpressionList list) {
      return findTypeFromCallExpression(list, parent);
    }

    if (parent instanceof HaxeAssignExpression assign) {
      return findTypeFromAssignExpression(assign);
    }

    if (parent instanceof HaxeBinaryExpression expression) {
      return findTypeFromAddExpression(expression);
    }

    if (parent instanceof HaxeGuard) {
      return SpecificHaxeClassReference.BOOL;
    }

    if (parent instanceof HaxeVarInit init) {
      if(init.getParent() instanceof HaxePsiField declaration) {
        HaxeTypeTag tag = declaration.getTypeTag();
        if (tag != null) {
          return tag.getFunctionType() != null ? tag.getFunctionType().getText()
                                               : tag.getTypeOrAnonymous().getText();
        }
      }
    }
    return SpecificHaxeClassReference.DYNAMIC;
  }

  private String findTypeFromAddExpression(HaxeBinaryExpression expression) {
    HaxeExpression target = expression.getLeftExpression();
    if (target == myPsiElementPointer.getElement()) {
      target = expression.getRightExpression();
    }
    ResultHolder result = HaxeExpressionEvaluator.evaluate(target, null).result;
    if (!result.isUnknown()) return getTypeName(result);
    return SpecificHaxeClassReference.DYNAMIC;
  }

  private String findTypeFromAssignExpression(HaxeAssignExpression assign) {
    List<HaxeExpression> assignlist = assign.getExpressionList();
    HaxeExpression expression = assignlist.get(0);
    HaxeExpressionEvaluatorContext evaluated = HaxeExpressionEvaluator.evaluate(expression, null);
    ResultHolder result = evaluated.result;
    if (!result.isUnknown()) {
      return getTypeName(result);
    }
    return SpecificHaxeClassReference.DYNAMIC;
  }

  private String findTypeFromCallExpression(HaxeCallExpressionList list, PsiElement parent) {
    List<HaxeExpression> argList = list.getExpressionList();
    int index = argList.indexOf(myPsiElementPointer.getElement());

    if (index > -1) {
      if (parent.getParent() instanceof HaxeCallExpression callExpression) {
        if (callExpression.getExpression() instanceof HaxeReference reference) {
          PsiElement resolved = reference.resolve();
          if (resolved instanceof HaxeMethod method) {
            HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, method);
            HaxeCallExpressionEvaluation validation = context.evaluate();
            Integer parameterIndex = validation.getArgumentToParameterMapping().get(index);
            if (parameterIndex != null) {
              ResultHolder paramType = validation.getParameterType(parameterIndex);
              if(paramType != null) return getTypeName(paramType);
            }
          }
        }
      }
    }
    return SpecificHaxeClassReference.DYNAMIC;
  }

  protected String  getTypeName(ResultHolder holder) {
      if(holder.isClassType()) return holder.getClassType().getClassName();
      else if(holder.isFunctionType()) return holder.getFunctionType().toPresentationString();
      else if(holder.isEnumValueType()) return holder.getEnumValueType().getEnumClass().getClassName();
      else return SpecificHaxeClassReference.DYNAMIC;
  }
}
