package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.lookup.*;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeCallExpressionEvaluatorCacheService;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContextContainer;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;
import static com.intellij.plugins.haxe.ide.lookup.HaxeCompletionPriorityData.*;

public class HaxeCompletionPriorityUtil {

  public static Set<CompletionResult> calculatePriority(Set<CompletionResult> completions, CompletionParameters parameters) {
    // ignore any completion that does not implement boost
    List<HaxeLookupElement> lookupList =
      completions.stream().filter(result -> result.getLookupElement() instanceof HaxeLookupElement)
        .map(result -> (HaxeLookupElement)result.getLookupElement())
        .toList();

    PsiElement position = parameters.getPosition();
    if (identifierInNewExpression.accepts(position)) {
      return prioritizeConstructorsRemoveOtherMembers(completions);
    }
    // is argument (get parameter type)
    boolean sorted = false;
    if (!sorted) sorted = trySortForExtends(position, lookupList);
    if (!sorted) sorted = trySortForArgument(position, lookupList); // NOTE TO SELF: function keyword if parameter is function typ
    if (!sorted) sorted = trySortForAssign(position, lookupList);
    if (!sorted) sorted = trySortForLoops(position, lookupList);
    if (!sorted) sorted = trySortForIf(position, lookupList);
    if (!sorted) sorted = trySortForBlock(position, lookupList);

    //TODO WiP
    //TODO support functionType reference ?
    // is IF (prioritize bool)
    // is switch block (find expression type, prioritize type)
    //  - if enumvalue  compare  declaring class with type

    return completions;
  }

  private static boolean trySortForAssign(PsiElement position, List<HaxeLookupElement> list) {
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
    ResultHolder assignToType = null;

    if(reference != null && reference.getParent() instanceof HaxeAssignExpression assignExpression) {
      HaxeExpression assignTo = assignExpression.getExpressionList().get(0);
      assignToType = HaxeExpressionEvaluator.evaluate(assignTo, null).result;
    }
    if(reference != null && reference.getParent() instanceof HaxeVarInit init) {
      PsiElement parent = init.getParent();
      if(parent instanceof  HaxeLocalVarDeclaration varDeclaration) {
        HaxeTypeTag tag = varDeclaration.getTypeTag();
        if (tag != null) {
          assignToType = HaxeTypeResolver.getTypeFromTypeTag(tag, varDeclaration);
        }
      }
    }

    for (HaxeLookupElement element : list) {
      if (element instanceof HaxeMemberLookupElement memberLookup) {
        HaxeBaseMemberModel model = memberLookup.getModel();
        if (model != null) {
          if (assignToType != null && !assignToType.isUnknown()) {
            SpecificTypeReference lookupType;
            if (memberLookup.isFunctionType() && model instanceof HaxeMethodModel methodModel) {
              lookupType = methodModel.getFunctionType(null);
            }else {
              lookupType = model.getResultType(null).getType();
            }
            if (lookupType.canAssign(assignToType)) {
              memberLookup.getPriority().type += 0.5;
              if (lookupType.isAny() || lookupType.isDynamic()){
                memberLookup.getPriority().type -=0.1;// prefer more concrete types
              }
            }
            if (memberLookup.isFunctionType() && assignToType.isFunctionType()) {
              element.getPriority().type += FUNCTION_TYPE;
            }
          }

          if (model instanceof HaxeLocalVarModel || model instanceof HaxeParameterModel) {
            element.getPriority().type += LOCAL_VAR;
          }

          if (model instanceof HaxeFieldModel) {
            element.getPriority().type += FIELD;
          }

          if (model instanceof HaxeMethodModel) {
            element.getPriority().type += METHOD;
          }
        }
      }
    }
    return false;
  }

  private static boolean trySortForBlock(PsiElement position, List<HaxeLookupElement> list) {
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
    if (reference != null && reference.getParent() instanceof HaxeBlockStatement blockStatement) {
      for (HaxeLookupElement element : list) {
        if (element instanceof HaxeMemberLookupElement memberLookup) {
          if (memberLookup.isFunctionType()) {
            // its less likely we want a functionType and more likely we want the call expression
            memberLookup.getPriority().type -= 0.5;
          }
        }
      }
      return true;
    }
    return false;
  }

  private static boolean trySortForIf(PsiElement position, List<HaxeLookupElement> list) {
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
    if (reference != null && reference.getParent() instanceof HaxeGuard) {
      for (HaxeLookupElement element : list) {
        if (element instanceof  HaxeMemberLookupElement memberLookup) {
          HaxeBaseMemberModel model = memberLookup.getModel();
          if (model != null) {
            ResultHolder type = model.getResultType(null);
            if (type.isClassType()) {
              if (type.getClassType().isBool()) {
                memberLookup.getPriority().assignable += 3;
              }
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  private static boolean trySortForLoops(PsiElement position, List<HaxeLookupElement> list) {
    HaxeIterable iterable = PsiTreeUtil.getParentOfType(position, HaxeIterable.class);
    if (iterable != null) {
      for (HaxeLookupElement element : list) {
        if (element instanceof  HaxeMemberLookupElement memberLookup) {
          HaxeBaseMemberModel model = memberLookup.getModel();
          if (model != null) {
            ResultHolder type = model.getResultType(null);
            if (type.isClassType()) {
              SpecificHaxeClassReference classType = type.getClassType();
              ResultHolder iterableType = classType.getIterableElementType(null);
              if (iterableType != null) {
                memberLookup.getPriority().assignable += 5;
              }
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  private boolean hasIterator(HaxeReference reference) {
    if (reference == null) return false;

    ResultHolder holder = HaxeExpressionEvaluator.evaluate(reference, null).result;
    SpecificHaxeClassReference resolvedType = holder.getClassType();
    if (resolvedType == null) return false;

    return resolvedType.isLiteralArray() || hasIterator(resolvedType);
  }

  private boolean hasIterator(SpecificHaxeClassReference type) {
    if (type.getHaxeClassModel() == null) return false;
    return  type.getHaxeClassModel().getMember("iterator", null) != null;
  }

  /*
   * If part of an inherit list, prioritize classes and interfaces
   *   - if part of an interface, prioritize other interfaces to extend
   *   - if part of class,  prioritize classes if in extends, and interfaces in implements lists
   */
  private static boolean trySortForExtends(PsiElement position, List<HaxeLookupElement> list) {
    HaxeInheritList inheritList = PsiTreeUtil.getParentOfType(position, HaxeInheritList.class);
    if (inheritList != null) {
      HaxeClassDeclaration classDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(inheritList, HaxeClassDeclaration.class);
      if (classDeclaration != null) {
        HaxeExtendsDeclaration extendsDeclaration = PsiTreeUtil.getParentOfType(position, HaxeExtendsDeclaration.class);
        if (extendsDeclaration != null) {
          list.stream()
            .filter(element -> element instanceof  HaxePsiLookupElement lookupElement && lookupElement.getType() == HaxeComponentType.CLASS)
            .forEach(element -> element.getPriority().type += 1);
          return true;
        }
        HaxeImplementsDeclaration implementsDeclaration = PsiTreeUtil.getParentOfType(position, HaxeImplementsDeclaration.class);
        if (implementsDeclaration != null) {
          list.stream()
            .filter(element -> element instanceof  HaxePsiLookupElement lookupElement && lookupElement.getType() == HaxeComponentType.INTERFACE)
            .forEach(element -> element.getPriority().type += 1);
          return true;
        }
      }else {
        list.stream()
          .filter(element -> element instanceof  HaxePsiLookupElement lookupElement && lookupElement.getType() == HaxeComponentType.INTERFACE)
          .forEach(element -> element.getPriority().type += 1);
        return true;
      }
    }
    return false;
  }


  private static boolean trySortForArgument(PsiElement position, List<HaxeLookupElement> lookupElements) {
    HaxeCallExpression callExpression = PsiTreeUtil.getParentOfType(position, HaxeCallExpression.class, true, HaxeNewExpression.class);
    HaxeNewExpression newExpression = PsiTreeUtil.getParentOfType(position, HaxeNewExpression.class, true, HaxeCallExpression.class);

    if (newExpression == null &&  callExpression == null) return false;

    int argumentIndex = 0;
    HaxeCallExpressionEvaluation validation = null;
    if (callExpression != null) {
      validation = getValidationForMethod(callExpression);
      HaxeCallExpressionList type = PsiTreeUtil.getParentOfType(position, HaxeCallExpressionList.class);
      if (type != null) {
        HaxeReferenceExpression ref = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
        int index = type.getExpressionList().indexOf(ref);
        if (index> -1) argumentIndex = index;
      }
    }
    if (newExpression != null && newExpression.getType() != null) {
      boolean completeType = newExpression.getType().textMatches(position);
      // if completing type name
      if (completeType) {
        lookupElements.stream()
          .filter(element -> element instanceof HaxeClassLookupElement)
          .forEach(e -> e.getPriority().type += 1);
        return true;
      }else {
        // else if completing parameters
        HaxeCallExpressionContextContainer contextContainer = HaxeCallExpressionUtil.createContextForConstructorCall(newExpression);
        validation = contextContainer.evaluateContexts();
      }
    }

    if (validation!= null) {
      List<ResultHolder> parameterTypes = validation.getParameterTypes();
      List<String> names = validation.getParameterNames();


      for (int i = 0; i < lookupElements.size(); i++) {
        HaxeLookupElement element = lookupElements.get(i);

        if (element instanceof HaxePackageLookupElement lookupElement) lookupElement.getPriority().type -= 0.1;
        if (element instanceof HaxeMemberLookupElement memberLookupElement) {
          memberCalculation(memberLookupElement, names, parameterTypes, argumentIndex);
          element.getPriority().type += 1;

        }
        if (element instanceof HaxeStaticMemberLookupElement staticMemberLookupElement) {
          staticMemberAssignCalculation(staticMemberLookupElement, parameterTypes);
          element.getPriority().type += 0.5;
        }
      }


      return true;
    }
    return false;
  }



  private static HaxeCallExpressionEvaluation getValidationForMethod(HaxeCallExpression callExpression) {
    if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if (resolve instanceof HaxeMethod method) {
        return HaxeCallExpressionEvaluatorCacheService.cachedHaxeCallExpressionEvaluation(method, callExpression);
      }
    }
    return null;
  }

  private static void staticMemberAssignCalculation(HaxeStaticMemberLookupElement element, Collection<ResultHolder> types) {
    for (ResultHolder type : types) {
      if (type.isUnknown()) continue;
      if(type.isClassType()) {
        String name = type.getClassType().getClassName();
        if (element.getTypeValue().contains(name)) {
          element.getPriority().assignable += 5;
        }
      }
    }



  }

  private static void memberCalculation(HaxeMemberLookupElement element, List<String> names, List<ResultHolder> parameterTypes,
                                        int argumentIndex) {
    HaxeBaseMemberModel model = element.getModel();
    if (model == null) return;

    ResultHolder lookupType = model.getResultType(null);

    if(model instanceof HaxeLocalVarModel || model instanceof  HaxeParameterModel) {
      element.getPriority().type += LOCAL_VAR;
    }

    if(model instanceof HaxeFieldModel ) {
      element.getPriority().type += FIELD;
    }

    if (model instanceof HaxeMethodModel methodModel) {
      element.getPriority().type += METHOD;
      // update lookupType with  functionType instead of return type
      if (element.isFunctionType()) {
        lookupType = methodModel.getFunctionType(null).createHolder();
      }
    }

    if (parameterTypes.size() <= argumentIndex) return;

    String lookupName = model.getName();
    List<String> lookupWords = getWords(lookupName);

    if(!calculateParameterPriority(element, names, parameterTypes, lookupType, argumentIndex, lookupWords, 1)){

      for (int i = 0; i < parameterTypes.size(); i++) {
        if (i == argumentIndex) continue;
        calculateParameterPriority(element, names,parameterTypes, lookupType, i, lookupWords, 0.1);
      }
    }

  }

  private static @NotNull List<String> getWords(String lookupName) {
    String[] camelCaseWords = lookupName.split("(?=[A-Z])");
    return Arrays.stream(camelCaseWords).map(String::toLowerCase).toList();
  }

  private static boolean calculateParameterPriority(HaxeMemberLookupElement element,
                                                    List<String> names,
                                                    List<ResultHolder> parameterTypes,
                                                    ResultHolder lookupType,
                                                    int argumentIndex,
                                                    List<String> words, double boost) {
    boolean matched = false;

    ResultHolder expectedParameterType = parameterTypes.get(argumentIndex);
    if(expectedParameterType.canAssign(lookupType)) {
      element.getPriority().assignable += 5 * boost;
      matched = true;
    }

    if (expectedParameterType.isFunctionType() && lookupType.isFunctionType()) {
      element.getPriority().assignable += 0.2 * boost;
    }

    if (!expectedParameterType.isFunctionType() && !lookupType.isFunctionType()) {
      element.getPriority().assignable += 0.3 * boost;
    }

    String expectedParameterName = names.get(argumentIndex);
    List<String> expectedWords = getWords(expectedParameterName);
    int matches = findMatchingWords(words, expectedWords);
    element.getPriority().name +=matches * boost;

    return matched;
  }

  private static int findMatchingWords(List<String> words, List<String> expectedWords) {
    HashSet<String> unique = new HashSet<>();
    unique.addAll(expectedWords);
    unique.addAll(words);
    return expectedWords.size() + words.size() - unique.size();
  }


  private static @NotNull CompletionResult boost(CompletionResult result, double finalBoost) {
    LookupElement element = result.getLookupElement();
    if (element instanceof HaxeMemberLookupElement lookupElement) {
      if (lookupElement.getModel() instanceof HaxeFieldModel) {
        return result.withLookupElement(PrioritizedLookupElement.withPriority(element, finalBoost));
      }
    }
    return result;
  }

  public static CompletionResult convertToPrioritized(CompletionResult result) {
    if (result.getLookupElement() instanceof HaxeLookupElement element) {
      return CompletionResult.wrap(element.toPrioritized(), result.getPrefixMatcher(), result.getSorter());
    }
    return result;
  }

}
