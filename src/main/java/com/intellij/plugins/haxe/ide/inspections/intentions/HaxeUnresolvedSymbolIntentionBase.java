package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.util.HaxeResolveUtil.getLeftReference;

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

    PsiElement target = getTargetPsi();

    PsiElement original = copyFileAndReturnClonedPsiElement(target);
    PsiElement copy = copyFileAndReturnClonedPsiElement(target);

    copy = perform(project, copy, editor, true);
    // this might not be the best solution but it seems to work.
    // in order to get the correct line-number we need to compare the entire file and to only get the changes we need identical formatting
    String originalFormatted = CodeStyleManager.getInstance(project).reformat(original.getContainingFile(), true).getText();
    String copyReformated = CodeStyleManager.getInstance(project).reformat(copy.getContainingFile(), true).getText();


    return new IntentionPreviewInfo.CustomDiff(HaxeFileType.INSTANCE, getPreviewName(), originalFormatted, copyReformated, true);
  }

  protected PsiElement getTargetPsi() {
    return myPsiElementPointer.getElement();
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
    if(hasClassReferenceCallie()) return true;
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

  protected  boolean needsToBePublic() {
    return hasClassReferenceCallie() || callieIsDifferentClass();
  }

  private boolean hasClassReferenceCallie() {
    HaxeExpression expression = null;
    if(myPsiElementPointer.getElement() instanceof  HaxeCallExpression callExpression) {
       expression = callExpression.getExpression();
    }else if(myPsiElementPointer.getElement() instanceof  HaxeReferenceExpression referenceExpression) {
      expression = referenceExpression;
    }
     if(expression != null)  {
       HaxeReference leftReference = getLeftReference(expression);
       if(leftReference instanceof HaxeReferenceImpl reference) {
       ResultHolder result = HaxeExpressionEvaluator.evaluate(leftReference).result;
         SpecificHaxeClassReference classType = result.getClassType();
         if(!result.isUnknown() && classType != null) {
           HaxeClass haxeClass = classType.getHaxeClass();
           if(haxeClass != null) {
             String name = haxeClass.getName();
             return  name != null && reference.textMatches(name);
           }
         }
         }
       }
    return false;
  }
  private boolean callieIsDifferentClass() {
    HaxeExpression expression = null;
    if(myPsiElementPointer.getElement() instanceof  HaxeCallExpression callExpression) {
      expression = callExpression.getExpression();
    }else if(myPsiElementPointer.getElement() instanceof  HaxeReferenceExpression referenceExpression) {
      expression = referenceExpression;
    }
     if(expression != null)  {
       HaxeReference leftReference = getLeftReference(expression);
       if(leftReference != null) {
       ResultHolder result = HaxeExpressionEvaluator.evaluate(leftReference).result;
         SpecificHaxeClassReference classType = result.getClassType();
         if(!result.isUnknown() && classType != null) {
           HaxeClass haxeClass = classType.getHaxeClass();
           if(haxeClass != null) {
             HaxeClass currentClass = PsiTreeUtil.getParentOfType(expression, HaxeClass.class);
             return currentClass != haxeClass;
           }
         }
       }
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

  private static ResultHolder findTypeFromAddExpression(HaxeBinaryExpression expression, PsiElement element) {
    HaxeExpression target = expression.getLeftExpression();
    if (target == element) {
      target = expression.getRightExpression();
    }
    ResultHolder result = HaxeExpressionEvaluator.evaluate(target, null).result;
    if (!result.isUnknown()) return result;
    return SpecificHaxeClassReference.getDynamic(target).createHolder();
  }

  protected String guessElementTypeText() {
    return getTypeName(guessElementType(myPsiElementPointer.getElement()));
  }
  public static ResultHolder guessElementType(PsiElement element) {
    PsiElement parent = element.getParent();
    if (parent instanceof HaxeCallExpressionList list) {
      return findTypeFromCallExpression(list, parent, element);
    }

    if (parent instanceof HaxeAssignExpression assign) {
      return findTypeFromAssignExpression(assign, element);
    }

    if (parent instanceof HaxeBinaryExpression expression) {
      return findTypeFromAddExpression(expression, element);
    }

    if (parent instanceof HaxeGuard) {
      return SpecificHaxeClassReference.getBool(parent).createHolder();
    }
    if(parent instanceof HaxeObjectLiteralElement objectLiteralElement) {
      ResultHolder resultType = guessObjectLiteralType(objectLiteralElement);
      if (resultType != null) return resultType;
    }

    if(parent instanceof HaxeExpressionList expressionList) {
      PsiElement parent1 = expressionList.getParent();
      if (parent1 instanceof  HaxeArrayLiteral arrayLiteral) {
        ResultHolder maybeArray = guessArrayLiteralType(expressionList, arrayLiteral);
        if (maybeArray != null) return maybeArray;
      }
    }

    if (parent instanceof HaxeVarInit init) {
      if(init.getParent() instanceof HaxePsiField declaration) {
        HaxeTypeTag tag = declaration.getTypeTag();
        if (tag != null) {
          ResultHolder tagType = HaxeTypeResolver.getTypeFromTypeTag(tag, declaration);
          if(!tagType.isUnknown()) {
            return tagType;
          }
        }
      }
    }
    return SpecificHaxeClassReference.getDynamic(parent).createHolder();
  }

  private static @Nullable ResultHolder guessArrayLiteralType(HaxeExpressionList expressionList, HaxeArrayLiteral arrayLiteral) {
    ResultHolder resultHolder = guessElementType(arrayLiteral);
    SpecificHaxeClassReference classType = resultHolder.getClassType();
    if(classType != null && !classType.isUnknown() && !classType.isDynamic()) {
      SpecificHaxeClassReference unknown = SpecificHaxeClassReference.getUnknown(expressionList);
      SpecificHaxeClassReference maybeArray = classType.tryCastTo(SpecificHaxeClassReference.createArray(unknown.createHolder(), expressionList));
      if(maybeArray != null && maybeArray.isArray()) {
        return maybeArray.getSpecifics()[0];
      }
    }
    return null;
  }

  private static @Nullable ResultHolder guessObjectLiteralType(HaxeObjectLiteralElement objectLiteralElement) {
    if (objectLiteralElement.getParent() instanceof  HaxeObjectLiteral objectLiteral) {
      ResultHolder resultHolder = guessElementType(objectLiteral);
      SpecificHaxeClassReference classType = resultHolder.getClassType();
      if(classType != null && resultHolder.isAnonymousType()) {
        HaxeClassModel haxeClassModel = classType.getHaxeClassModel();
        if(haxeClassModel != null) {
          HaxeGenericResolver genericResolver = classType.getGenericResolver();
          HaxeBaseMemberModel member = haxeClassModel.getMember(objectLiteralElement.getName(), genericResolver);
          if (member != null) {
            ResultHolder resultType = member.getResultType(genericResolver);
            if (resultType != null && !resultType.isUnknown()) {
              return resultType;
            }
          }
        }
      }
    }
    return null;
  }

  private static ResultHolder findTypeFromAssignExpression(HaxeAssignExpression assign, PsiElement element) {
    HaxeExpression expression = assign.getRightExpression();
    if (PsiTreeUtil.isAncestor(expression, element, false)) expression = assign.getLeftExpression();
    if(expression != null) {
      HaxeExpressionEvaluatorContext evaluated = HaxeExpressionEvaluator.evaluate(expression, null);
      ResultHolder result = evaluated.result;
      if (!result.isUnknown()) {
        return result;
      }
    }
    return SpecificHaxeClassReference.getUnknown(expression).createHolder();
  }

  private static ResultHolder findTypeFromCallExpression(HaxeCallExpressionList list, PsiElement parent, PsiElement element) {
    List<HaxeExpression> argList = list.getExpressionList();
    int index = argList.indexOf(element);

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
              if(paramType != null) return paramType;
            }
          }
        }
      }
    }
    return SpecificHaxeClassReference.getDynamic(parent).createHolder();
  }

  protected String  getTypeName(ResultHolder holder) {
      if(holder.isClassType()) return holder.getClassType().toPresentationString();
      else if(holder.isFunctionType()) return holder.getFunctionType().toPresentationString();
      else if(holder.isEnumValueType()) return holder.getEnumValueType().getEnumClass().getClassName();
      else return SpecificHaxeClassReference.DYNAMIC;
  }
}
