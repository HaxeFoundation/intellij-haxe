/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2017-2020 Eric Bishton
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceExpressionImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.model.type.HaxeArgument;
import com.intellij.plugins.haxe.util.HaxeAbstractForwardUtil;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ArrayListSet;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.findObjectLiteralType;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.searchInSameFileForEnumValues;
import static com.intellij.plugins.haxe.util.HaxeStringUtil.elide;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  public static final int MAX_DEBUG_MESSAGE_LENGTH = 200;
  public static final Key<String> typeHintKey = new Key<>("typeHint");
  private static final Key<Boolean> skipCacheKey = new Key<>("skipCache");

  //static {  // Remove when finished debugging.
  //  LOG.setLevel(LogLevel.DEBUG);
  //  LOG.debug(" ========= Starting up debug logger for HaxeResolver. ==========");
  //}

  public static final HaxeResolver INSTANCE = new HaxeResolver();

  public static ThreadLocal<Stack<PsiElement>> referencesProcessing = ThreadLocal.withInitial(Stack::new);



  private static boolean reportCacheMetrics = false;   // Should always be false when checked in.
  private static AtomicInteger dumbRequests = new AtomicInteger(0);
  private static AtomicInteger requests = new AtomicInteger(0);
  private static AtomicInteger resolves = new AtomicInteger(0);
  private final static int REPORT_FREQUENCY = 100;

  public static final List<? extends PsiElement> EMPTY_LIST = Collections.emptyList();

  private final RecursionGuard<PsiElement> resolveInnerRecursionGuard = RecursionManager.createGuard("resolveInnerRecursionGuard");

  public static void prohibitResultCaching(@NotNull PsiElement element) {
    INSTANCE.resolveInnerRecursionGuard.prohibitResultCaching(element);
  }

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
       /** See docs on {@link HaxeDebugUtil#isCachingDisabled} for how to set this flag. */
       boolean skipCachingForDebug = HaxeDebugUtil.isCachingDisabled();

       //// Kill circular resolutions -- before checking the cache.
       //if (isResolving(reference)) {
       //  recursiveLookupFailures.get().incrementAndGet();
       //  return null;
       //}

       // If we are in dumb mode (e.g. we are still indexing files and resolving may
       // fail until the indices are complete), we don't want to cache the (likely incorrect)
       // results.
       boolean isDumb = DumbService.isDumb(reference.getProject());
       boolean hasTypeHint = checkForTypeHint(reference);
       boolean skipCaching = skipCachingForDebug || isDumb || hasTypeHint;

        List<? extends PsiElement>  elements  = skipCaching ? doResolve(reference, incompleteCode)
                         : ResolveCache.getInstance(reference.getProject())
                       .resolveWithCaching(reference, this::doResolve, true, incompleteCode);

       if (reportCacheMetrics) {
         if (skipCachingForDebug) {
           log.debug("Resolve cache is disabled.  No metrics computed.");
           reportCacheMetrics = false;
         }
         else {
           int dumb = isDumb ? dumbRequests.incrementAndGet() : dumbRequests.get();
           int requestCount = isDumb ? requests.get() : requests.incrementAndGet();
           if ((dumb + requestCount) % REPORT_FREQUENCY == 0) {
             int res = resolves.get();
             Formatter formatter = new Formatter();
             formatter.format("Resolve requests: %d; cache misses: %d; (%2.2f%% effective); Dumb requests: %d",
                              requestCount, res,
                              (1.0 - (Float.intBitsToFloat(res) / Float.intBitsToFloat(requestCount))) * 100,
                              dumb);
             log.debug(formatter.toString());
           }
         }
       }
       return elements == null ? EMPTY_LIST : elements;
  }

  //TODO until we have type hints everywhere we need to skip caching for those refrences that rely on typeHints
  private boolean checkForTypeHint(HaxeReference reference) {
    if (reference.getUserData(typeHintKey) != null ) return true;
    if (reference.getParent() instanceof  HaxeCallExpression expression) {
      if (expression.getUserData(typeHintKey) != null ) return true;
    }
    return false;
  }

  private boolean isResolving(@NotNull HaxeReference reference) {
    Stack<PsiElement> stack = referencesProcessing.get();
    return stack.contains(reference);
  }


  @Nullable
  private List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    Stack<PsiElement> stack = referencesProcessing.get();
    boolean traceEnabled = log.isTraceEnabled();

    String referenceText = reference.getText();
    stack.push(reference);
    try {
      if (traceEnabled) {
        log.trace(traceMsg("-----------------------------------------"));
        log.trace(traceMsg("Resolving reference: " + referenceText));
      }

       List<? extends PsiElement> foundElements = resolveInnerRecursionGuard
         .computePreventingRecursion( reference, true, () ->  doResolveInner(reference, incompleteCode, referenceText));


      if (traceEnabled) {
        log.trace(traceMsg("Finished  reference: " + referenceText));
        log.trace(traceMsg("-----------------------------------------"));
      }

      return foundElements;
    }
    finally {
      stack.pop();
    }
  }

  private List<? extends PsiElement> doResolveInner(@NotNull HaxeReference reference, boolean incompleteCode, String referenceText) {

    if (reportCacheMetrics) {
      resolves.incrementAndGet();
    }

    if (reference instanceof HaxeLiteralExpression || reference instanceof HaxeConstantExpression) {
      if (!(reference instanceof HaxeRegularExpression || reference instanceof HaxeStringLiteralExpression)) {
        return EMPTY_LIST;
      }
    }
    boolean isType = reference.getParent() instanceof HaxeType ||  PsiTreeUtil.getParentOfType(reference, HaxeTypeTag.class) != null;
    List<? extends PsiElement> result = checkIsTypeParameter(reference);

    if (result == null) result = checkIsChain(reference);

    if (result == null) result = checkIsAlias(reference);
    if (result == null) result = checkEnumMemberHints(reference);
    if (result == null) result = checkIsType(reference);
    if (result == null) result = checkIsFullyQualifiedStatement(reference);
    if (result == null) result = checkIsSuperExpression(reference);
    if (result == null) result = checkMacroIdentifier(reference);

    if (result == null) result = checkIsAccessor(reference);
    if (result == null) result = checkCaptureVarReference(reference);
    if (result == null) result = checkEnumExtractor(reference);// do before walking tree
    if (result == null) result = checkIsSwitchVar(reference);
    if (result == null) result = checkByTreeWalk(reference);  // Beware: This will also locate constraints in scope.

    HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
    // search same file first (avoids incorrect resolve of common named Classes and member with same name in local file)
    if (result == null)result =  searchInSameFile(reference, fileModel, isType);
    if (result == null) result = checkIsClassName(reference);
    if (result == null) result = checkCaptureVar(reference);
    if (result == null) result = checkSwitchOnEnum(reference);
    if (result == null) result = checkMemberReference(reference); // must be after resolvers that can find identifier inside a method
    if (result == null) {


      if (fileModel != null) {
          List<PsiElement> matchesInImport = HaxeResolveUtil.searchInImports(fileModel, referenceText);
        // Remove enumValues if we are resolving typeTag as typeTags should not be EnumValues
        // We also have to remove resolved fields as abstract enums is a thing
        if (isType) {
          matchesInImport = matchesInImport.stream().filter(element ->  !(element instanceof HaxeEnumValueDeclaration)).toList();
          matchesInImport = matchesInImport.stream().filter(element ->  !(element instanceof HaxeFieldDeclaration)).toList();
        }
        if (!matchesInImport.isEmpty()) {
            // one file may contain multiple enums and have enumValues with the same name; trying to match any argument list
            if(matchesInImport.size()> 1 &&  reference.getParent() instanceof  HaxeCallExpression callExpression) {
              int expectedSize = Optional.ofNullable(callExpression.getExpressionList()).map(e -> e.getExpressionList().size()).orElse(0);

              // check type hinting for enumValues
              for (PsiElement importElement : matchesInImport) {
                if (importElement.getParent() instanceof  HaxeEnumValueDeclaration enumValueDeclaration) {
                  PsiElement typeHintPsi = reference;

                  if (reference.getParent() instanceof  HaxeCallExpression expression) {
                    typeHintPsi = expression;
                  }
                  String currentQname = enumValueDeclaration.getContainingClass().getQualifiedName();
                  String data = typeHintPsi.getUserData(typeHintKey);
                  if (currentQname != null && currentQname.equals(data)) {
                    LogResolution(reference, "via import & typeHintKey");
                    return List.of(importElement);
                  }
                }
              }

              // test  call expression if possible
              for (PsiElement importElement : matchesInImport) {
                if (importElement.getParent() instanceof HaxeEnumValueDeclarationConstructor enumValueDeclaration) {
                  boolean isValidConstructor = testAsEnumValueConstructor(enumValueDeclaration, reference);
                  if (isValidConstructor) return List.of(importElement);
                }
              }
              // fallback, check method parameters (needs work , optional are not handled)
              for (PsiElement importElement : matchesInImport) {
                if (importElement.getParent() instanceof HaxeEnumValueDeclarationConstructor enumValueDeclaration) {
                  int currentSize =
                    Optional.of(enumValueDeclaration.getParameterList()).map(p -> p.getParameterList().size()).orElse(0);
                  if (expectedSize == currentSize) {
                    LogResolution(reference, "via import  & enum value declaration");
                    return List.of(importElement);
                  }
                }
              }
            }
            return matchesInImport.isEmpty() ? null : matchesInImport;
          }
        PsiElement target = HaxeResolveUtil.searchInSamePackage(fileModel, referenceText, true);

        if (target != null) {
          LogResolution(reference, "via import.");
          return asList(target);
        }
      }

      if (PsiNameHelper.getInstance(reference.getProject()).isQualifiedName(reference.getText())) {
        List<HaxeModel> resolvedPackage =
          HaxeProjectModel.fromElement(reference).resolve(new FullyQualifiedInfo(reference.getText()), reference.getResolveScope());
        if (resolvedPackage != null && !resolvedPackage.isEmpty() && resolvedPackage.get(0) instanceof HaxePackageModel) {
          LogResolution(reference, "via project qualified name.");
          return Collections.singletonList(resolvedPackage.get(0).getBasePsi());
        }
      }
    }
    if (result == null) result = checkIsForwardedName(reference);
    if (result == null) result = checkGlobalAlias(reference);

    if (result == null) {
      LogResolution(reference, "failed after exhausting all options.");
      return EMPTY_LIST; // empty list means cache not found
    }

    if (log.isTraceEnabled()) {
      String message = "caching result for :" + referenceText;
      traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
    }
    return result;

  }

  private static boolean testAsEnumValueConstructor(@NotNull HaxeEnumValueDeclarationConstructor enumValueDeclaration, @NotNull HaxeReference reference) {
      if (reference.getParent() instanceof HaxeCallExpression haxeCallExpression) {
        HaxeMethod method = enumValueDeclaration.getModel().getMethod();
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(haxeCallExpression, method);
        HaxeCallExpressionEvaluation validation = context.evaluate();
        return validation.isCompleted() && validation.isValid();
      }
    return false;
  }

  private List<? extends PsiElement> checkIsSwitchExtractedValue(HaxeReference reference) {
    if (reference instanceof  HaxeEnumExtractedValueReference extractedValue) {
      if(extractedValue.getParent() instanceof HaxeEnumExtractorArgumentList argumentList) {
        if (argumentList.getParent() instanceof HaxeEnumArgumentExtractor extractor) {
          int argumentIndex = getExtractorArgumentIndex(extractedValue, extractor);
          if(argumentIndex > -1) {
            findExtractedValueEnumParameter(extractor, argumentIndex);
          }
        }
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkSwitchOnEnum(HaxeReference reference) {
    if (reference.getParent() instanceof HaxeSwitchCaseExpr) {
      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (switchStatement != null) {
        HaxeExpression expressionWithType = switchStatement.getExpression();
        ResultHolder possibleType = HaxeExpressionEvaluator.evaluate(expressionWithType, null).result;
        if (possibleType.isEnum() && possibleType.getClassType() != null) {
          SpecificHaxeClassReference type = possibleType.getClassType();
          HaxeClassModel classModel = type.getHaxeClassModel();
          if(classModel!= null) {
            HaxeBaseMemberModel member = classModel.getMember(reference.getText(), null);
            if (member != null) return List.of(member.getNameOrBasePsi());
          }
        }
      }

    }
    return null;
  }

  // checks if we are attempting to  assign an enum type, this makes sure we chose the enum value and not competing class names
  private List<? extends PsiElement> checkEnumMemberHints(HaxeReference reference) {
    if (reference instanceof HaxeReferenceExpressionImpl) {
      PsiElement referenceParent = reference.getParent();
      if (referenceParent instanceof HaxeEnumValueReference) {
        HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class, true);
        if (switchCaseExpr != null) {
          HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);
          if (matchExpression != null) {
            List<HaxeComponentName> names = evaluateAndFindEnumMember(reference, matchExpression.getExpression());
            if (names != null && !names.isEmpty()) return names;
          }
          HaxeSwitchStatement parentSwitch = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
          if (parentSwitch != null) {
            HaxeExpression expression = parentSwitch.getExpression();
            while (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
              expression = parenthesizedExpression.getExpression();
            }
            HaxeSwitchCaseExprArray exprArray = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExprArray.class);
            // TODO: if necessary support array inside array ?
            int index = findSwitchArrayIndex(exprArray, reference);
            if (expression instanceof HaxeArrayLiteral arrayLiteral) {
              HaxeExpressionList list = arrayLiteral.getExpressionList();
              if (list != null && index > -1) {
                List<HaxeExpression> expressionList = list.getExpressionList();
                if (expressionList.size()> index) {
                  HaxeExpression haxeExpression = expressionList.get(index);
                  List<HaxeComponentName> components = evaluateAndFindEnumMember(reference, haxeExpression);
                  if (components != null) return components;
                }
              }
            }

            List<HaxeComponentName> components = evaluateAndFindEnumMember(reference, expression);
            if (components != null) return components;
          }
        }
      }
      if(referenceParent instanceof  HaxeObjectLiteralElement literalElement) {
        HaxeObjectLiteral objectLiteral = PsiTreeUtil.getParentOfType(literalElement, HaxeObjectLiteral.class);
        if(objectLiteral != null) {
          ResultHolder objectLiteralType = findObjectLiteralType(new HaxeExpressionEvaluatorContext(objectLiteral), null, objectLiteral);
          if(objectLiteralType != null && !objectLiteralType.isUnknown()) {
            SpecificHaxeClassReference typeFromUsage = objectLiteralType.getClassType();
            if (typeFromUsage != null && typeFromUsage.getHaxeClassModel() != null) {
              HaxeBaseMemberModel objectLiteralElementAsMember = typeFromUsage.getHaxeClassModel()
                .getMember(literalElement.getName(), typeFromUsage.getGenericResolver());
              if(objectLiteralElementAsMember != null) {
                ResultHolder type = objectLiteralElementAsMember.getResultType(typeFromUsage.getGenericResolver());
                if(type != null && type.isEnum()) {
                  List<HaxeComponentName> components = findEnumMember(reference, type.getType());
                  if (components != null) return components;
                }

              }
            }
          }
        }
      }

      if (referenceParent instanceof HaxeCallExpression) {
        List<HaxeComponentName> member = checkParameterListFromCallExpressions(reference, referenceParent);
        if (member != null) return member;
      }

      if (!(referenceParent instanceof HaxeType)) {
        HaxeParameter parameterFromReferenceExpression = null;
        HaxePsiField fieldFromReferenceExpression = null;
        HaxeAssignExpression assignExpression = PsiTreeUtil.getParentOfType(reference, HaxeAssignExpression.class, true, HaxeCallExpression.class);
        if (assignExpression != null) {
          HaxeExpression left = assignExpression.getLeftExpression();
          //guard to avoid another resolve of the same reference, and attempts to check assignExpression for only part of a reference expression
          if (left != reference && !(referenceParent instanceof HaxeReferenceExpression)) {
            if (left instanceof HaxeReferenceExpression referenceExpression) {
              PsiElement resolve = referenceExpression.resolve();
              if (resolve instanceof HaxePsiField psiField) {
                fieldFromReferenceExpression = psiField;
              }
              if (resolve instanceof HaxeParameter parameter) {
                parameterFromReferenceExpression = parameter;
              }
            }
          }
        }
        if (referenceParent instanceof HaxeCompareExpression compareExpression) {
          if (compareExpression.getLeftExpression() instanceof HaxeReferenceExpression referenceExpression) {
            if (referenceExpression != reference) {//guard to avoid another resolve of the same reference
              PsiElement resolve = referenceExpression.resolve();
              if (resolve instanceof HaxePsiField psiField) {
                fieldFromReferenceExpression = psiField;
              }
            }
          }
        }

        boolean isEnumConstructor = reference.getParent() instanceof HaxeCallExpression;
        PsiElement element = isEnumConstructor ? reference.getParent().getParent() : reference.getParent();
        HaxePsiField field =
          fieldFromReferenceExpression != null ? fieldFromReferenceExpression : PsiTreeUtil.getParentOfType(element, HaxePsiField.class, true, HaxeCallExpression.class, HaxeNewExpression.class);
        HaxeParameter parameter = parameterFromReferenceExpression != null
                                  ? parameterFromReferenceExpression
                                  : PsiTreeUtil.getParentOfType(element, HaxeParameter.class, true, HaxeCallExpression.class, HaxeNewExpression.class);
        HaxeTypeTag tag = null;
        HaxeVarInit init = null;
        if (field != null) {
          tag = field.getTypeTag();
          init = field.getVarInit();
        } else if (parameter != null) {
          tag = parameter.getTypeTag();
          init = parameter.getVarInit();
        }

        if (tag != null && tag.getTypeOrAnonymous() != null) {
          ResultHolder type = HaxeTypeResolver.getTypeFromTypeOrAnonymous(tag.getTypeOrAnonymous());
          if (type.getClassType() != null) {
            SpecificTypeReference typeReference = type.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
            return findEnumMember(reference, typeReference);
          }
        }
        if (init != null) {
          // check if reference is part of init expression and if so skip to avoid circular resolve
          HaxeVarInit referenceInitParent = PsiTreeUtil.getParentOfType(reference, HaxeVarInit.class);
          if (referenceInitParent == null || referenceInitParent != init) {
            ResultHolder type = HaxeTypeResolver.getPsiElementType(init, null);
            if (type.getClassType() != null) {
              SpecificTypeReference typeReference = type.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
              return findEnumMember(reference, typeReference);
            }
          }
        }
        // check function argumentList and look for enum hints in argument types
        int index;
        PsiElement PossibleCallExpression = element;
        if (PossibleCallExpression instanceof  HaxeCallExpressionList callExpressionList) {
          PossibleCallExpression = callExpressionList.getParent();
          index = callExpressionList.getExpressionList().indexOf(reference);
        }else {
          index = 0;
        }
        if(PossibleCallExpression instanceof  HaxeCallExpression callExpression) {
          ResultHolder result = HaxeExpressionEvaluator.evaluate(callExpression.getExpression(), new HaxeGenericResolver()).result;
          SpecificFunctionReference functionType = result.getFunctionType();
          if(functionType != null) {
            List<HaxeArgument> arguments = functionType.getArguments();
            if (index > -1) {
              while (index < arguments.size()) {
                HaxeArgument argument = arguments.get(index++);
                ResultHolder type = argument.getType();
                if (type != null && type.isEnum()) {
                  SpecificHaxeClassReference classType = type.getClassType();
                  if (classType != null) {
                    SpecificTypeReference typeReference = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
                    return findEnumMember(reference, typeReference);
                  }
                  if (!argument.isOptional()) break;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static @Nullable List<HaxeComponentName> checkParameterListFromCallExpressions(HaxeReference reference, PsiElement referenceParent) {
    @NotNull PsiElement[] children = referenceParent.getChildren();
    HaxeNewExpression constructorCall;
    HaxeCallExpression methodCallCall;
    PsiElement argument;
    // if this is an enum constructor then we should be the first child in the callExpression
    // this means we have to go at least one level up to check for method calls
    if (children.length > 0  && children[0] == reference) {
      argument = referenceParent;
       constructorCall = PsiTreeUtil.getParentOfType(referenceParent, HaxeNewExpression.class);
       methodCallCall = PsiTreeUtil.getParentOfType(referenceParent, HaxeCallExpression.class);
    }else {
      argument = reference;
      constructorCall = PsiTreeUtil.getParentOfType(reference, HaxeNewExpression.class);
      methodCallCall = PsiTreeUtil.getParentOfType(reference, HaxeCallExpression.class);
    }
    if (methodCallCall != null) {
      if (methodCallCall.getExpression() instanceof HaxeReference callerReference) {
        if (callerReference.resolve() instanceof HaxeMethod haxeMethod) {
          HaxeCallExpressionList argumentList = methodCallCall.getExpressionList();
          if (argumentList != null) {
            int argumentIndex = argumentList.getExpressionList().indexOf(argument);
            if (argumentIndex > -1) {
              HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForMethodCall(methodCallCall, haxeMethod);
              HaxeCallExpressionEvaluation validation = context.evaluate();
              int parameterIndex = validation.getParameterForArgument(argumentIndex);
              ResultHolder parameterType = validation.getParameterType(parameterIndex);
              if (parameterType != null) {
                SpecificHaxeClassReference possibleType = parameterType.getClassType();
                if (possibleType != null && possibleType.isEnumType()) {
                  List<HaxeComponentName> member = findEnumMember(reference, possibleType);
                  if (member != null) return member;
                }
              }
            }
          }
        }
      }
    }
    if (constructorCall != null) {
      List<HaxeExpression> argumentList = constructorCall.getExpressionList();
      int argumentIndex = argumentList.indexOf(argument);
      if (argumentIndex> -1) {
        HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForConstructorCall(constructorCall);
        if (context != null) {
          HaxeCallExpressionEvaluation validation = context.evaluate();
          int parameterIndex = validation.getParameterForArgument(argumentIndex);
          ResultHolder parameterType = validation.getParameterType(parameterIndex);
          if (parameterType != null) {
            SpecificHaxeClassReference possibleType = parameterType.getClassType();
            if (possibleType != null && possibleType.isEnumType()) {
              List<HaxeComponentName> member = findEnumMember(reference, possibleType);
              if (member != null) return member;
            }
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeComponentName> evaluateAndFindEnumMember(HaxeReference reference, HaxeExpression haxeExpression) {
    HaxeExpressionEvaluatorContext evaluate = HaxeExpressionEvaluator.evaluate(haxeExpression, null);
    ResultHolder result = evaluate.result;
    if (result.getClassType() != null) {
      SpecificTypeReference typeReference = result.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
      return findEnumMember(reference, typeReference);
    }
    return null;
  }

  private static int findSwitchArrayIndex(HaxeSwitchCaseExprArray exprArray, HaxeReference reference) {
    int index;
    if (exprArray != null) {
      @NotNull List<HaxeExpression> list = exprArray.getExpressionList();
      for (int i = 0; i < list.size(); i++) {
        HaxeExpression haxeExpression = list.get(i);
        if (haxeExpression == reference) return i; //  check normal array one to one mapping
        //
        if (PsiTreeUtil.findCommonContext(reference, haxeExpression)  == haxeExpression) return i;
      }
    }
    return -1;
  }

  @Nullable
  private static List<HaxeComponentName> findEnumMember(HaxeReference reference, SpecificTypeReference typeReference) {
      if (typeReference instanceof  SpecificHaxeClassReference classReference) {
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        if (classModel != null && classModel.isEnum()) {
          HaxeClass haxeClass = classReference.getHaxeClass();
          if (haxeClass != null) {
            HaxeNamedComponent name = haxeClass.findHaxeMemberByName(reference.getText(), null);
            if (name != null) {
              HaxeComponentName componentName = name.getComponentName();
              if (componentName != null) {
                LogResolution(reference, "via enum member name.");
                return List.of(componentName);
              }
            }
          }
        }
      }
    return null;
  }

  private List<? extends PsiElement> checkGlobalAlias(HaxeReference reference) {
    if (reference.textMatches("trace")) {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
      HaxeProjectModel haxeProjectModel = HaxeProjectModel.fromElement(reference);
        HaxeModel model = haxeProjectModel.getLogPackage().resolveTrace();
        if (model != null){
          LogResolution(reference, "via global alias");
          return List.of(model.getBasePsi());
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<PsiElement> searchInSameFile(@NotNull HaxeReference reference, HaxeFileModel fileModel, boolean isType) {
    if(fileModel != null) {
      String className = reference.getText();
      if (reference.getParent() instanceof HaxeCallExpression) {
        // there can be multiple enum types with enumValues with the same name,
        // we use enum constructors in an attempt to find the correct one first, its not a perfect solution but should cover some cases.
        // if we dont find it  there's always a fallback in the `searchInSameFile` code below
        //Note: the more correct way in some cases is to search for expected type from its use
        List<PsiElement> elements = searchInSameFileForEnumValues(fileModel, className);
        for (PsiElement element : elements) {
          if (element instanceof HaxeNamedComponent namedComponent) {
            if (element instanceof HaxeEnumValueDeclarationConstructor enumValueDeclaration) {
              boolean isValidConstructor = testAsEnumValueConstructor(enumValueDeclaration, reference);
              if (isValidConstructor) return List.of(namedComponent.getComponentName());
            }
          }
        }
      }
      PsiElement target = HaxeResolveUtil.searchInSameFile(fileModel, className, isType);
      if (target instanceof HaxeNamedComponent namedComponent) {
        LogResolution(reference, "via search In Same File");
        HaxeComponentName componentName = namedComponent.getComponentName();
        if (componentName != null) return List.of(componentName);
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkMacroIdentifier(HaxeReference reference) {
    @NotNull PsiElement[] children = reference.getChildren();
    if (children.length == 1) {
      if (children[0] instanceof  HaxeMacroIdentifier identifier) {
        PsiElement macroId = identifier.getMacroId();
        if (macroId != null) {
          String substring = macroId.getText().substring(1);
          return checkByTreeWalk(reference, substring);
        }
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkMemberReference(HaxeReference reference) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    // check if reference is to a member in  class or abstract
    //   null:      it's a direct reference (not a chain, could be normal class member access)
    //   this:      this class member access
    //   super:      super class member access (used when overriding methods and calling base method)
    //   abstract:  similar to "this" but for abstracts
    if (leftReference == null || leftReference.textMatches("this")  || leftReference.textMatches("super")  || leftReference.textMatches("abstract")) {
      HaxeClass type = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
      List<? extends PsiElement> superElements = resolveBySuperClassAndSymbol(type, reference);
      if (!superElements.isEmpty()) {
        LogResolution(reference, "via super field.");
        return superElements;
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkIsTypeParameter(HaxeReference reference) {
    HaxeTypeTag typeTag = PsiTreeUtil.getParentOfType(reference, HaxeTypeTag.class);
    if (typeTag != null) {

      HaxeFieldDeclaration fieldDeclaration = PsiTreeUtil.getParentOfType(reference, HaxeFieldDeclaration.class);
      if (fieldDeclaration != null) {
        HaxeModel model = fieldDeclaration.getModel();
        if (model instanceof HaxeFieldModel fieldModel) {
          HaxeClassModel declaringClass = fieldModel.getDeclaringClass();
          if (declaringClass != null) {
            List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
            return findTypeParameterPsi(reference, params);
          }
        }
      }

       HaxeMethodDeclaration methodDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeMethodDeclaration.class);
      if (methodDeclaration != null) {
        List<HaxeGenericParamModel> methodParams = methodDeclaration.getModel().getGenericParams();
        List<HaxeComponentName> methodTypeParameter = findTypeParameterPsi(reference, methodParams);
        if (methodTypeParameter != null) {
          return methodTypeParameter;
        }
        HaxeClassModel declaringClass = methodDeclaration.getModel().getDeclaringClass();
        if (declaringClass != null) {
          List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
          return findTypeParameterPsi(reference, params);
        }
      }

      HaxeConstructorDeclaration constructorDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeConstructorDeclaration.class);
      if (constructorDeclaration != null) {
        // reference is a type tag in constructor, we should check  owning class type parameters
        // so we won't resolve this to a type outside the class if its a type parameter
        HaxeClassModel declaringClass = constructorDeclaration.getModel().getDeclaringClass();
        if (declaringClass != null) {
          List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
          return findTypeParameterPsi(reference, params);
        }

      }
      HaxeEnumValueDeclaration enumDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeEnumValueDeclaration.class);
      if (enumDeclaration != null) {
        // EnumValueDeclarations does not define TypeParameters, only the parent EnumType can have these.
        HaxeClassModel declaringClass = ((HaxeEnumValueModel)enumDeclaration.getModel()).getDeclaringEnum();
        if (declaringClass != null) {
          List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
          return findTypeParameterPsi(reference, params);
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeComponentName> findTypeParameterPsi(HaxeReference reference, List<HaxeGenericParamModel> params) {
    Optional<HaxeGenericListPart> first = params.stream()
      .filter(p -> p.getName().equals(reference.getText()))
      .map(HaxeGenericParamModel::getPsi)
      .findFirst();
    if (first.isPresent()) {
      LogResolution(reference, "via TypeParameter Psi");
      HaxeGenericListPart part = first.get();
      HaxeComponentName componentName = part.getComponentName();
      if (componentName != null) {
        return List.of(componentName);
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkEnumExtractor(HaxeReference reference) {
    if (reference.getParent() instanceof HaxeEnumValueReference) {
      HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class);
      SpecificHaxeClassReference classReference = HaxeResolveUtil.resolveExtractorEnum(argumentExtractor);
      if (classReference != null) {
        HaxeEnumValueDeclaration declaration = HaxeResolveUtil.resolveEnumValueDeclaration(classReference, argumentExtractor);
        if (declaration != null) {
          LogResolution(reference, "via enum extractor");
          return List.of(declaration);
        }
      }
    }
    else if (reference.getParent() instanceof  HaxeSwitchCaseExpr || reference.getParent() instanceof  HaxeExtractorMatchAssignExpression) {
      // Last attempt to resolve  enum value (not extractor), normally imports would solve this but  some typedefs can omit this.
      HaxeSwitchStatement type = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (type != null) {
        HaxeExpression expression = type.getExpression();
        while (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
          expression = parenthesizedExpression.getExpression();
        }
        if (expression instanceof HaxeReferenceExpression referenceExpression) {
          HaxeResolveResult result = referenceExpression.resolveHaxeClass();
          if (result.isHaxeTypeDef()) {
            result = result.fullyResolveTypedef();
          }
          HaxeClass haxeClass = result.getHaxeClass();
          if (haxeClass != null && haxeClass.isEnum()) {
            SpecificHaxeClassReference classReference = result.getSpecificClassReference(haxeClass, null);
            HaxeEnumValueDeclaration declaration =
              HaxeResolveUtil.resolveEnumValueDeclaration(classReference, reference.getText());
            if (declaration != null) {
              LogResolution(reference, "via enum extractor");
              return List.of(declaration);
            }
          }
        }
        if ( reference.getParent() instanceof  HaxeExtractorMatchAssignExpression assignExpression
             && assignExpression.getParent() instanceof  HaxeSwitchCaseExtractor extractor) {
          List<HaxeExpression> list = extractor.getExpressionList();
          if (!list.isEmpty()) {
            PsiElement expression1 = list.get(list.size() - 1);
            while(expression1 instanceof HaxeExtractorMatchExpression matchExpression) {
              HaxeSwitchCaseExpr expr = matchExpression.getSwitchCaseExpr();
              @NotNull PsiElement[] children = expr.getChildren();
              expression1 = children[children.length-1];
            }
            LogResolution(reference, "via switch-case extractor match assign");
            return List.of(expression1);
          }
        }else {
          LogResolution(reference, "via switch-case reference as var (without var keyword)");
          return List.of(expression);
        }
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkCaptureVarReference(HaxeReference reference) {
    if (reference instanceof HaxeReferenceExpression) {
      HaxeSwitchCase switchCase = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCase.class);
      if (switchCase != null) {
        for (HaxeSwitchCaseExpr expr : switchCase.getSwitchCaseExprList()) {
          HaxeExpression expression = expr.getExpression();
          if (expression instanceof HaxeEnumArgumentExtractor extractor) {
            List<HaxeExpression> expressionList = extractor.getEnumExtractorArgumentList().getExpressionList();
            for (HaxeExpression haxeExpression : expressionList) {
              if (haxeExpression instanceof HaxeExtractorMatchExpression matchExpression) {
                HaxeExpression PossibleCapture = matchExpression.getSwitchCaseExpr().getExpression();
                if (PossibleCapture != null && PossibleCapture.textMatches(reference)) {
                  LogResolution(reference, "via switch argument extractor");
                  return List.of(PossibleCapture);
                }
              }
            }
          }
          else if (expression instanceof HaxeExtractorMatchExpression matchExpression) {
            HaxeReferenceExpression referenceFromExtractor = getReferenceFromExtractorMatchExpression(matchExpression);
            if (referenceFromExtractor!= null && reference.textMatches(referenceFromExtractor)) {
              LogResolution(reference, "via witch extractor");
              return List.of(referenceFromExtractor);
            }
          }
        }
      }
    }
    return null;
  }

  /*
    HaxeExtractorMatchExpression can be chained so we need to loop until we get a reference
      ex.  case add(_, 1) => mul(_1, 3) => a:
   */
  private HaxeReferenceExpression getReferenceFromExtractorMatchExpression(HaxeExtractorMatchExpression expression) {
    HaxeSwitchCaseExpr caseExpr = expression.getSwitchCaseExpr();
    while (caseExpr != null) {
      if (caseExpr.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
        return referenceExpression;
      }
      else if (caseExpr.getExpression() instanceof HaxeExtractorMatchExpression matchExpression) {
        caseExpr = matchExpression.getSwitchCaseExpr();
      }
      else {
        caseExpr = null;
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkCaptureVar(HaxeReference reference) {
    HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);
    if (matchExpression!= null) {
      if (matchExpression.getSwitchCaseExpr().textMatches(reference.getText())) {
        LogResolution(reference, "via Capture Var");
        return List.of(matchExpression.getExpression());
      }
    }
    return null;
  }

  /*
   * Known problems:
   * inline vars and final vars are constants that could be used in a switch case as the constant value
   * this method will consider it a variable, this can probably be fixed by resolving exists variables first and check modifiers
   * this method has to be before walk tree
   */
  private List<? extends PsiElement> checkIsSwitchVar(HaxeReference reference) {

    HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class);

    // check "case" scope for local variables first before we go higher up and check the case expression
    List<? extends PsiElement> result = checkByTreeWalk(reference, switchCaseExpr);

    // NOTE: this one has to come before  `checkIfSwitchCaseDefaultValue`
    // check if default name in match expression (ex  `case TString(_ => captureVar)`)
    if (result == null) result = checkIfDefaultValueInMatchExpression(reference, switchCaseExpr);

    // check if enum extracted value (ex `case MyEnumVal(reference)`)
    if (result == null) result = checkIsSwitchExtractedValue(reference);

    // check if matches default name ( ex. `case _:`)
    if (result == null) result = checkIfSwitchCaseDefaultValue(reference);

    // try to match  when expression is array literal
    if (result == null) result = tryResolveCaseArrayElement(reference, switchCaseExpr);

    // try to match when expression is object literal
    if (result == null) result = tryResolveCaseObjectElement(reference);

    // checks if it matches default name inside array (ex. `case [2, _]:` when switch expression is array reference)
    if (result == null) result = checkIfDefaultNameInCaseArray(reference, switchCaseExpr);



    // try to resolve reference for guard and block (ex. `case [a, b] if ( b > a): b + ">" + a;`)
    if (result == null) result = tryResolveVariableForGuardsAndBlock(reference);
    if (result != null) {
      LogResolution(reference, "via switch var");
    }
    return result;
  }

  private List<? extends PsiElement> tryResolveCaseObjectElement(HaxeReference reference) {
    if (reference.getParent() != null && reference.getParent() instanceof HaxeEnumObjectLiteralElement objectLiteralElement) {
      Stack<String> objectNames = new Stack<>();
        objectNames.add(extractObjectLiteralName(objectLiteralElement));

        while (objectLiteralElement.getParent() instanceof HaxeEnumExtractObjectLiteral objectLiteral
               && objectLiteral.getParent() instanceof HaxeEnumObjectLiteralElement subElement) {
              objectNames.push(extractObjectLiteralName(subElement));
              objectLiteralElement = subElement;
        }
        HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
        if(switchStatement != null && switchStatement.getExpression() != null) {
          ResultHolder result = HaxeExpressionEvaluator.evaluate(switchStatement.getExpression(), null).result;
          if (result.isClassType()) {
            while (!objectNames.isEmpty()
                   && result.getClassType() != null
                   && result.getClassType().getHaxeClass() instanceof  HaxeObjectLiteral objectLiteral) {
              String memberName = objectNames.pop();
              HaxeBaseMemberModel member = objectLiteral.getModel().getMember(memberName, null);

              if (member != null)  {
                if (objectNames.isEmpty()) {
                  return List.of(member.getBasePsi());
                }
                result = member.getResultType(null);
              }else {
                break;
              }
            }
          }
        }
    }
    return null;
  }

  private static String extractObjectLiteralName(HaxeEnumObjectLiteralElement objectLiteralElement) {
    return objectLiteralElement.getComponentName().getIdentifier().getText();
  }

  @Nullable
  private static List<? extends PsiElement> tryResolveVariableForGuardsAndBlock(HaxeReference reference) {
    HaxeGuard guard = PsiTreeUtil.getParentOfType(reference, HaxeGuard.class);
    HaxeSwitchCaseBlock switchCaseBlock = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseBlock.class);
    if (switchCaseBlock != null || guard != null) {
      HaxeSwitchCase switchCase = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCase.class);
      if (switchCase!= null) {
        List<HaxeSwitchCaseExpr> list = switchCase.getSwitchCaseExprList();
        for (HaxeSwitchCaseExpr caseExpr : list) {
          HaxeSwitchCaseExprArray caseExprArray = caseExpr.getSwitchCaseExprArray();
          if (caseExprArray != null) {
            List<HaxeExpression> expressionList = caseExprArray.getExpressionList();
            for (HaxeExpression haxeExpression : expressionList) {
              if (haxeExpression instanceof  HaxeEnumArgumentExtractor extractor) {
                List<HaxeEnumExtractedValue> value = searchEnumArgumentExtractorForReference(reference, extractor);
                if (value != null) return value;
              }
              if (haxeExpression.textMatches(reference)) {
                return List.of(haxeExpression);
              }
            }
          }
          HaxeExpression expression = caseExpr.getExpression();
          if (expression instanceof  HaxeArrayLiteral arrayLiteral) {
            HaxeExpressionList expressionList = arrayLiteral.getExpressionList();
            if (expressionList!= null) {
              for (HaxeExpression haxeExpression : expressionList.getExpressionList()) {
                if (haxeExpression.textMatches(reference)) {
                  return List.of(haxeExpression);
                }
              }
            }
          }else if (expression instanceof HaxeReferenceExpression referenceExpression) {
            if (reference.textMatches(referenceExpression)) return List.of(referenceExpression);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeEnumExtractedValue> searchEnumArgumentExtractorForReference(HaxeReference reference, HaxeEnumArgumentExtractor extractor) {
    List<HaxeEnumExtractedValue> extractedValues = extractor.getEnumExtractorArgumentList().getEnumExtractedValueList();
    for (HaxeEnumExtractedValue value : extractedValues) {
      HaxeEnumExtractedValueReference valueReference = value.getEnumExtractedValueReference();
      if (valueReference != null) {
        HaxeComponentName componentName = valueReference.getComponentName();
        if (componentName.textMatches(reference)) {
          return List.of(value);
        }
      }
    }
    return null;
  }

  private static List<PsiElement> checkIfDefaultNameInCaseArray(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    if (switchCaseExpr != null && reference.getParent() instanceof HaxeSwitchCaseExprArray) {
      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (switchStatement != null && switchStatement.getExpression() != null) {
        // should be array, but can be anything
        ResultHolder type = HaxeTypeResolver.getPsiElementType(switchStatement.getExpression(), null);
        SpecificHaxeClassReference classReference = type.getClassType();
        if (classReference != null && classReference.getSpecifics().length > 0) {
          return List.of(classReference.getSpecifics()[0].getElementContext());
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<PsiElement> tryResolveCaseArrayElement(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    if (switchCaseExpr != null && reference.getParent() instanceof HaxeSwitchCaseExprArray) {
      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (switchStatement != null && switchStatement.getExpression() != null) {

        HaxeExpression haxeExpression = switchStatement.getExpression();
        if (haxeExpression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
          haxeExpression = parenthesizedExpression.getExpression();
        }
        if (haxeExpression instanceof  HaxeArrayLiteral arrayLiteral) {
          int index;
          // find  our reference index (NOTE: may not work if nested arrays or in enum extractor)
          HaxeSwitchCaseExprArray caseArray = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExprArray.class);
          if (caseArray != null) {
            List<HaxeExpression> caseExpressionList = caseArray.getExpressionList();
            index = caseExpressionList.indexOf(reference);
            if (index > -1) {
              HaxeExpressionList switchExpressionList = arrayLiteral.getExpressionList();
              if (switchExpressionList != null) {
                List<HaxeExpression> list = switchExpressionList.getExpressionList();
                if (list.size() > index) return List.of(list.get(index));
              }
            }
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private List<PsiElement> checkIfDefaultValueInMatchExpression(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    //TODO/NOTE:
    // There seems to be an issue where method calls in extractor match expressions would be mapped to the parameter type of the same index
    // this is usually wrong  so we avoid this here, there could maybe be cases where the parameter type is a function and you want to call it
    // as part of the  matching (not sure if its allowed, needs testing)
    if(reference.getParent() instanceof  HaxeCallExpression callExpression &&  PsiTreeUtil.getParentOfType(callExpression, HaxeExtractorMatchExpression.class ) != null) return null;

    HaxeEnumArgumentExtractor argumentExtractorBeforeAntSwitchExpr = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class, true, HaxeSwitchCaseExpr.class);
    if (argumentExtractorBeforeAntSwitchExpr != null) {
      int argumentIndex = getExtractorArgumentIndex(reference,  argumentExtractorBeforeAntSwitchExpr);
      if (argumentIndex > -1) {
        List<PsiElement> parameter = findExtractedValueEnumParameter(argumentExtractorBeforeAntSwitchExpr, argumentIndex);
        if (parameter != null) return parameter;
      }
    }

    HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);

    if (matchExpression != null) {
      HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class);
      if (argumentExtractor!= null) {
        SpecificHaxeClassReference enumClass = HaxeResolveUtil.resolveExtractorEnum(argumentExtractor);
        if (enumClass != null) {
          HaxeClassModel model = enumClass.getHaxeClassModel();
          if (model != null) {
            HaxeBaseMemberModel enumValue = model.getMember(argumentExtractor.getEnumValueReference().getText(), null);
            if (enumValue instanceof  HaxeEnumValueConstructorModel enumValueModel) {
              int argumentIndex = findExtractorIndex(switchCaseExpr.getChildren(), argumentExtractor);
              if (argumentIndex > -1) {
                HaxeParameterList parameters = enumValueModel.getConstructorParameters();
                return List.of(parameters.getParameterList().get(argumentIndex));
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static @Nullable List<PsiElement> findExtractedValueEnumParameter(HaxeEnumArgumentExtractor argumentExtractor1, int argumentIndex) {
    HaxeEnumValueReference enumType = argumentExtractor1.getEnumValueReference();
    HaxeResolveResult result = enumType.getReferenceExpression().resolveHaxeClass();
    HaxeClass aClass = result.getHaxeClass();
    if (aClass != null) {
      HaxeClassModel model = aClass.getModel();
      HaxeBaseMemberModel member = model.getMember(enumType.getText(), null);
      if (member instanceof HaxeEnumValueConstructorModel enumValueModel) {
        HaxeParameterList parameters = enumValueModel.getConstructorParameters();
        if (parameters != null) {
          List<HaxeParameter> list = parameters.getParameterList();
          if (list.size()> argumentIndex) {
            HaxeParameter parameter = list.get(argumentIndex);
            return List.of(parameter.getComponentName());
          }
        }
      }
    }
    return null;
  }

  private static int getExtractorArgumentIndex(HaxeReference reference, HaxeEnumArgumentExtractor argumentExtractor) {
    HaxeEnumExtractorArgumentList argumentList = argumentExtractor.getEnumExtractorArgumentList();
    List<HaxeExpression> expressions = argumentList.getExpressionList();

    int argumentIndex = -1;
    for (int i = 0; i < expressions.size(); i++) {
      HaxeExpression expression = expressions.get(i);
      PsiElement context = PsiTreeUtil.findCommonContext(expression, reference);
      // pure extractedValue reference
      if(context instanceof  HaxeEnumExtractedValue) {
        argumentIndex = i;
        break;
      }
      // reference as part of an expression
      if (context != argumentExtractor && context instanceof  HaxeExtractorMatchExpression) {
        argumentIndex = i;
        break;
      }
    }
    return argumentIndex;
  }

  @Nullable
  private static List<@NotNull PsiElement> checkIfSwitchCaseDefaultValue(HaxeReference reference) {
    if (reference.textMatches("_")) {
      // if is part of an expression
      HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class,  true, HaxeEnumArgumentExtractor.class);
      if (switchCaseExpr != null) {
        if (switchCaseExpr.getParent() instanceof HaxeExtractorMatchExpression matchExpression) {
          //  reference should be  previous matchExpression as it's the value/result from that one that is passed as _
          return List.of(matchExpression.getExpression());
        } else {
          HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(switchCaseExpr, HaxeSwitchStatement.class);
          if (switchStatement != null && switchStatement.getExpression() != null) {
            HaxeExpression expression = switchStatement.getExpression();
            if (expression instanceof HaxeArrayLiteral arrayLiteral) {
              // generate list of indexes  to look up in potentially multi-dimensional arrays
              List<PsiElement> parents = collectArrayParents(reference);
              List<Integer> indexes = findSwitchLiteralArrayIndexes(reference, parents);

              Collections.reverse(indexes);
              HaxeExpressionList list = arrayLiteral.getExpressionList();
              if(list != null) {
                HaxeExpression  target = arrayLiteral;
                target = tryArrayLookup(indexes, target);
                if(target != null) {
                  return List.of(target);
                }
              }
            }else {
              HaxeExpression switchExpression = switchStatement.getExpression();
              if (switchExpression != null) return List.of(switchExpression);
            }
          }
        }
      }

    }
    return null;
  }

  private static @NotNull List<PsiElement> collectArrayParents(HaxeReference reference) {
    List<PsiElement> parents = UsefulPsiTreeUtil.getPathToParentOfType(reference, HaxeSwitchCaseExprArray.class);
    if (parents == null) return List.of();
    return parents.stream().filter(element -> element instanceof HaxeSwitchCaseExprArray || element instanceof HaxeArrayLiteral).toList();
  }

  private static HaxeExpression tryArrayLookup(List<Integer> indexes, HaxeExpression target) {
    for (Integer index : indexes) {
      if (target instanceof HaxeArrayLiteral internalArray) {
        HaxeExpressionList internalExpList = internalArray.getExpressionList();
        if (internalExpList != null) {
          List<HaxeExpression> expressionList = internalExpList.getExpressionList();
          if (expressionList.size() > index) {
            target = expressionList.get(index);
          }
        }
      }
    }
    return target;
  }

  private static @NotNull List<Integer> findSwitchLiteralArrayIndexes(HaxeReference reference, List<PsiElement> arrayParents) {
    List<Integer> indexes = new ArrayList<>();
    for (PsiElement array : arrayParents) {

      @NotNull PsiElement[] children = new PsiElement[0];
      if (array instanceof  HaxeArrayLiteral arrayLiteral && arrayLiteral.getExpressionList() != null) {
        children =  arrayLiteral.getExpressionList().getChildren();
      }else if (array instanceof HaxeSwitchCaseExprArray caseExprArray) {
        children = caseExprArray.getChildren();
      }
        for (int i = 0; i < children.length; i++) {
          PsiElement child = children[i];
          if(child == reference || isChildOf(reference, child, array)){
            indexes.add(i);
            break;
          }
        }
      }
    return indexes;
  }

  private static boolean isChildOf(PsiElement child, PsiElement target, PsiElement giveUpAt) {
    PsiElement parent = child.getParent();
    while (parent != null) {
      if (parent == target)
        return true;
      parent = parent.getParent();
      if (parent == giveUpAt) return false;
    }
    return false;
  }

  private int findExtractorIndex(PsiElement[] children, HaxeExpression expression) {
    for (int i = 0; i < children.length; i++) {
      if(children[i] == expression) return i;
    }
    return -1;
  }

  private List<? extends PsiElement> checkIsForwardedName(HaxeReference reference) {
    List<? extends PsiElement> result = null;

    HaxeMetadataCompileTimeMeta meta = UsefulPsiTreeUtil.getParentOfType(reference, HaxeMetadataCompileTimeMeta.class);
    if (null != meta && HaxeMeta.FORWARD.matches(meta.getType())) {
      PsiElement associatedElement = HaxeMetadataUtils.getAssociatedElement(meta);
      if (null != associatedElement) {
        if (associatedElement instanceof HaxeAbstractTypeDeclaration) {
          HaxeAbstractClassModel model = new HaxeAbstractClassModel((HaxeAbstractTypeDeclaration)associatedElement);
          HaxeGenericResolver resolver = model.getGenericResolver(null);
          HaxeClass underlyingClass = model.getUnderlyingClass(resolver);
          List<? extends PsiElement> resolved = resolveByClassAndSymbol(underlyingClass, resolver, reference);
          if (!resolved.isEmpty())result = resolved;
        }
      }
    }

    if (null != result) {
      LogResolution(reference, "via forwarded field name check.");
    }
    return result;
  }

  /**
   * Walks up the scope from the reference, trying to find the named type.
   * <p>
   * For instance, it will find a type constraint from a subClass if the reference is a type parameter
   * for a sub-class.  For example: {@code myType<K:constrainedType> extends superType<K> } will
   * resolve to {@code constrainedType} if the reference being resolved is the second {@code K}.
   *
   * @param reference
   * @return
   */
  private List<? extends PsiElement> checkByTreeWalk(HaxeReference reference,  @Nullable PsiElement maxScope) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, reference.getText(), reference), reference, maxScope, new ResolveState());
    if (result.isEmpty()) return null;
    LogResolution(reference, "via tree walk.");
    return result;
  }
  private List<? extends PsiElement> checkByTreeWalk(HaxeReference reference) {
    return checkByTreeWalk(reference, (PsiElement)null);
  }

  private List<? extends PsiElement> checkByTreeWalk(HaxeReference scope, String name) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, name, null), scope, null, new ResolveState());
    if (result.isEmpty()) return null;
    LogResolution(scope, "via tree walk.");
    return result;
  }

  private List<? extends PsiElement> checkIsAccessor(HaxeReference reference) {
    if (reference instanceof HaxePropertyAccessor) {
      final HaxeAccessorType accessorType = HaxeAccessorType.fromPsi(reference);
      if (accessorType != HaxeAccessorType.GET && accessorType != HaxeAccessorType.SET) return null;

      final HaxeFieldDeclaration varDeclaration = PsiTreeUtil.getParentOfType(reference, HaxeFieldDeclaration.class);
      if (varDeclaration == null) return null;

      final HaxeFieldModel fieldModel = (HaxeFieldModel)varDeclaration.getModel();
      final HaxeMethodModel method = accessorType == HaxeAccessorType.GET ? fieldModel.getGetterMethod() : fieldModel.getSetterMethod();

      if (method != null) {
        LogResolution(reference, "via accessor.");
        HaxeNamedComponent namedComponentPsi = method.getNamedComponentPsi();
        if (namedComponentPsi != null) {
          return asList(namedComponentPsi.getComponentName());
        }
      }
    }

    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsChain(@NotNull HaxeReference reference) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    if (leftReference != null) {
      List<? extends PsiElement> result = resolveChain(leftReference, reference);
      if (result != null && !result.isEmpty()) {
        LogResolution(reference, "via simple chain using leftReference.");
        return result;
      }
      LogResolution(reference, "via simple chain against package.");
      PsiElement item = resolveQualifiedReference(reference);
      if (item != null) {
        return asList(item);
      }
    }
    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsClassName(@NotNull HaxeReference reference) {
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(reference);
    if (resultClass != null) {
      LogResolution(reference, "via class qualified name.");
      return asList(resultClass.getComponentName());
    }
    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsSuperExpression(HaxeReference reference) {
    if (reference instanceof HaxeSuperExpression && reference.getParent() instanceof HaxeCallExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
      if(haxeClass != null) {
        if (!haxeClass.getHaxeExtendsList().isEmpty()) {
          final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
          final HaxeClass superClass = ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass();
          if (superClass != null) {
            final HaxeNamedComponent constructor = superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString(), null); // Self only.
            LogResolution(reference, "because it's a super expression.");
            return asList(((constructor != null) ? constructor.getComponentName() : superClass.getComponentName()));
          }
        }
      }
    }

    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsType(HaxeReference reference) {
    final HaxeType type = PsiTreeUtil.getParentOfType(reference, HaxeType.class);
    if (type != null) {
      final HaxeClass haxeClassInType = HaxeResolveUtil.tryResolveClassByQName(type);
      if (type != null && haxeClassInType != null) {
        LogResolution(reference, "via parent type name.");
        return asList(haxeClassInType.getComponentName());
      }
      //  check if module member
      //  we might get a match on class with module name (default class), but the type we are looking for is not a child of default class
      //  so we search the parent file for other classes
      @NotNull PsiElement[] children = reference.getChildren();
      if(children.length > 1) {
        if (children[0] instanceof HaxeReference child) {
          List<? extends PsiElement> resolve = resolve(child, false);
          if (!resolve.isEmpty()) {
            PsiFile containingFile = resolve.get(0).getContainingFile();
            if (containingFile instanceof  HaxeFile haxeFile) {
              HaxeClassModel model = haxeFile.getModel().getClassModel(children[1].getText());
              if (model != null) {
                HaxeComponentName componentName = model.haxeClass.getComponentName();
                if(componentName!= null){
                  LogResolution(reference, "via module scan.");
                  return List.of(componentName);
                }
              }
            }
          }
        }
      }
    }
    return null;
  }
  @Nullable
  private List<? extends PsiElement> checkIsAlias(HaxeReference reference) {
      PsiFile file = reference.getContainingFile();
      if (file instanceof HaxeFile haxeFile) {
        List<HaxeImportStatement> statements = haxeFile.getImportStatements();
        for (HaxeImportStatement statement : statements) {
          HaxeImportAlias alias = statement.getAlias();
          if (alias != null) {
            HaxeIdentifier identifier = alias.getIdentifier();
            if (identifier.textMatches(reference)) {
              LogResolution(reference, "via import alias name.");
              return List.of(alias);
            }
          }
        }
      }
    return null;
  }

  private List<? extends PsiElement> checkIsFullyQualifiedStatement(@NotNull HaxeReference reference) {
    if (reference instanceof HaxeReferenceExpression) {
      HaxeStatementPsiMixin parent = PsiTreeUtil.getParentOfType(reference,
                                                               HaxePackageStatement.class,
                                                               HaxeImportStatement.class,
                                                               HaxeUsingStatement.class);
      if (parent != null) {

        //TODO check for @:using on haxeType and add to using (this might not be the correct place, but its a reminder to add it somewhere in the resolver logic)

        LogResolution(reference, "via parent/package import.");
        return asList(resolveQualifiedReference(reference));
      }
    }
    return null;
  }

  private static void LogResolution(HaxeReference ref, String tailmsg) {
    // Debug is always enabled if trace is enabled.
    if (log.isDebugEnabled()) {
      String message = "Resolved " + (ref == null ? "empty result" : ref.getText()) + " " + elide(tailmsg, MAX_DEBUG_MESSAGE_LENGTH);
      if (log.isTraceEnabled()) {
        traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
      }
      else {
        log.debug(message);
      }
    }
  }

  private static final RecursionGuard<PsiElement> extensionsFromMetaGuard = RecursionManager.createGuard("extensionsFromMetaGuard");
  private static final RecursionGuard<PsiElement> extensionsMethodGuard = RecursionManager.createGuard("extensionsMethodGuard");

  /**
   * Resolve a chain reference, given two references: the qualifier, and the name.
   *
   * @param lefthandExpression - qualifying expression (e.g. "((ref = reference).getProject())")
   * @param reference          - field/method name to resolve.
   * @return the resolved element, if found; null, otherwise.
   */
  @Nullable
  private List<? extends PsiElement> resolveChain(HaxeReference lefthandExpression, HaxeReference reference) {
    // TODO: Merge with resolveByClassAndSymbol()??  It is very similar to this method.
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    if (leftReference != null) {
      // recursive so we try to  resolve first element in the chain first and go up the chain
      resolveChain(leftReference, lefthandExpression);
    }
    //List<List<? extends PsiElement>> debugList = new ArrayList<>();

    String identifier = reference instanceof HaxeReferenceExpression referenceExpression ? referenceExpression.getIdentifier().getText() : reference.getText();
    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(lefthandExpression);

    ResultHolder result = extensionsMethodGuard.doPreventingRecursion(lefthandExpression, true, () -> {
      return HaxeExpressionEvaluator.evaluate(lefthandExpression, context, null).result;
    });
    if(result== null) {
      extensionsMethodGuard.prohibitResultCaching(lefthandExpression);
    }

    SpecificHaxeClassReference classType = result == null || result.isUnknown() ? null : result.getClassType();
    HaxeClass  haxeClass = classType != null ? classType.getHaxeClass() : null;

    // To avoid incorrect extension method results we avoid any results where we don't know type of left reference.
    // this is important as recursion guards might prevent us from getting the type and returning a different result depending on
    // whether or not we got the type is bad and causes issues.
    if (haxeClass != null && !result.isUnknown()) {
      // if type parameter, try to find constraints and use those ?
      haxeClass = useConstraintsIfTypeParameter(reference, haxeClass);
      haxeClass = useDefaultIfTypeParameter(reference, haxeClass);
      HaxeClassModel classModel = haxeClass.getModel();
      HaxeBaseMemberModel member = classModel.getMember(identifier, classType.getGenericResolver());
      if (member != null) {
        HaxeNamedComponent psi = member.getNamedComponentPsi();
        if (psi != null) {
          HaxeComponentName name = psi.getComponentName();
          if (name != null) {
            return Collections.singletonList(name);
          }
        }
      }
      // check extension methods from meta
      HaxeComponentName match = extensionsFromMetaGuard.doPreventingRecursion(lefthandExpression, true, () -> {
        int size = classModel.getUsingMetaReferences().size();
        List<HaxeMethodModel> meta = classModel.getExtensionMethodsFromMeta();
        if (size != meta.size()) {
          resolveInnerRecursionGuard.prohibitResultCaching(lefthandExpression);
        }
        for (HaxeMethodModel model : meta) {
          HaxeNamedComponent psi = model.getNamedComponentPsi();
          if (psi != null) {
            HaxeComponentName name = psi.getComponentName();
            if (name != null && name.getIdentifier().textMatches(identifier)) {
              if (log.isTraceEnabled()) log.trace(traceMsg("Found component name in extension methods"));
              return name;
            }
          }
        }
        return null;
      });
      if (match != null) return Collections.singletonList(match);

    // Check 'using' classes.
      HaxeFileModel fileModel = HaxeFileModel.fromElement(reference.getContainingFile());

      // Add the global usings to the top of the list (so they're checked last).
      HaxeProjectModel projectModel = HaxeProjectModel.fromElement(reference);
      HaxeStdPackageModel stdPackageModel = (HaxeStdPackageModel)projectModel.getStdPackage();
      final List<HaxeUsingModel> usingModels = new ArrayList<>(stdPackageModel.getGlobalUsings());

      HaxeResolveUtil.walkDirectoryImports(fileModel, (importModel) -> {
        usingModels.addAll(importModel.getUsingModels());
        return true;
      });

      if (fileModel != null) {
        usingModels.addAll(fileModel.getUsingModels());
      }

      HaxeMethodModel foundMethod = null;
        for (int i = usingModels.size() - 1; i >= 0; --i) {
          foundMethod = usingModels.get(i)
            .findExtensionMethod(identifier, classType);
          if (null != foundMethod && !foundMethod.HasNoUsingMeta()) {

            if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + foundMethod.getName());
            //return asList(foundMethod.getBasePsi());
            //debugList.add(asList(foundMethod.getBasePsi()));
            return List.of(foundMethod.getNamePsi());
          }
          // check other types ("using" can be used to find typedefsetc)

          //TODO mlo:  try to get namedComponent from element
          PsiElement element = usingModels.get(i).exposeByName(identifier);
          if (element != null) {
            if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + identifier);
            //return List.of(element);
            //debugList.add(List.of(element));
            return List.of(element);
          }
        }
      }

    if (log.isTraceEnabled()) log.trace(traceMsg(null));

    final HaxeComponentName componentName = tryResolveHelperClass(lefthandExpression, identifier);
    if (componentName != null) {
      if (log.isTraceEnabled()) log.trace("Found component " + componentName.getText());
      return Collections.singletonList(componentName);
    }
    if (log.isTraceEnabled()) log.trace(traceMsg("trying keywords (super, new) arrays, literals, etc."));
    // Try resolving keywords (super, new), arrays, literals, etc.
    return resolveByClassAndSymbol(haxeClass, reference);

  }



  private static HaxeClass useConstraintsIfTypeParameter(HaxeReference reference, HaxeClass haxeClass) {
    if(haxeClass instanceof HaxeTypeParameterDeclaration typeParameter) {
      HaxeGenericResolver referenceResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);
      ResultHolder resolved = referenceResolver.resolve(typeParameter);
      if (resolved != null && resolved.isClassType()) {
        SpecificHaxeClassReference classReference = resolved.getClassType();
        if (classReference != null) {
          HaxeClass newClassValue = classReference.getHaxeClass();
          if(newClassValue != null) haxeClass = newClassValue;
        }
      }
    }
    return haxeClass;
  }
  private HaxeClass useDefaultIfTypeParameter(HaxeReference reference, HaxeClass haxeClass) {
    if(haxeClass instanceof HaxeGenericListPart genericPart) {
      HaxeGenericParamModel model = genericPart.getModel();
      ResultHolder defaultType = model.getDefaultType(null);
      HaxeGenericResolver referenceResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);
      ResultHolder resolved = referenceResolver.resolve(defaultType);
      if(resolved != null && resolved.getClassType() != null ) {
        return resolved.getClassType().getHaxeClass();
      }
    }
    return haxeClass;
  }

  private PsiElement resolveQualifiedReference(HaxeReference reference) {
    String qualifiedName = reference.getText();

    final FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(qualifiedName);
    List<HaxeModel> result = HaxeProjectModel.fromElement(reference).resolve(qualifiedInfo, reference.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.get(0);
      if (item instanceof HaxeFileModel fileModel) {
        HaxeClassModel mainClass = fileModel.getMainClassModel();
        if (mainClass != null) {
          return mainClass.haxeClass.getComponentName();
        }
      }
      PsiElement psi = item.getBasePsi();
      if (psi instanceof  PsiPackage) return psi;

      HaxeComponentName type = PsiTreeUtil.findChildOfType(psi, HaxeComponentName.class);
      if (type != null) {
        return type;
      }
    }

    return null;
  }

  /**
   * Test if the leftReference is a class name (either locally or in a super-class),
   * and if so, find the named field/method declared inside of it.
   * <p>
   * If the leftReference is to a file, and helperName is a class, we return the name
   * of that class.
   *
   * @param leftReference - a potential class/file name.
   * @param helperName    - the field/method/class to find.
   * @return the name of the found field/method/class.  null if not found.
   */
  @Nullable
  private HaxeComponentName tryResolveHelperClass(HaxeReference leftReference, String helperName) {
    if (log.isTraceEnabled()) log.trace(traceMsg("leftReference=" + leftReference + " helperName=" + helperName));
    HaxeComponentName componentName = null;
    HaxeReferenceExpression referenceExpression = PsiTreeUtil.getChildOfType(leftReference, HaxeReferenceExpression.class);
    HaxeClass leftResultClass = HaxeResolveUtil.tryResolveClassByQName(referenceExpression);
    if (leftResultClass != null) {
      if (log.isTraceEnabled()) {
        log.trace(traceMsg("Found a left result via QName: " + (leftResultClass.getText() != null ? leftResultClass : "<no text>")));
      }
      // helper reference via class com.bar.FooClass.HelperClass
      final HaxeClass componentDeclaration =
        HaxeResolveUtil.findComponentDeclaration(leftResultClass.getContainingFile(), helperName);
      componentName = componentDeclaration == null ? null : componentDeclaration.getComponentName();
    }
    else {
      // try to find component at abstract forwarding underlying class
      HaxeResolveResult resolveResult = leftReference.resolveHaxeClass();
      leftResultClass = resolveResult.getHaxeClass();
      if (log.isTraceEnabled()) {
        String resultClassName = leftResultClass != null ? leftResultClass.getText() : null;
        log.trace(traceMsg("Found abstract left result:" + (resultClassName != null ? resultClassName : "<no text>")));
      }
      if (leftResultClass != null) {
        HaxeClassModel model = leftResultClass.getModel();

        if (model.isTypedef()) {
          // Resolve to the underlying type.
          HaxeResolveResult result = fullyResolveTypedef(leftResultClass, resolveResult.getSpecialization());
          if (null != result.getHaxeClass()) {
            model = result.getHaxeClass().getModel();
          }
        }

        HaxeBaseMemberModel member = model.getMember(helperName, resolveResult.getGenericResolver());
        if (member != null) return member.getNamePsi();

        if (model.isAbstractType() && ((HaxeAbstractClassModel)model).hasForwards()) {
          HaxeGenericResolver resolver = resolveResult.getSpecialization().toGenericResolver(leftResultClass);
          final List<HaxeNamedComponent> forwardingHaxeNamedComponents =
            HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(leftResultClass, resolver);
          if (forwardingHaxeNamedComponents != null) {
            for (HaxeNamedComponent namedComponent : forwardingHaxeNamedComponents) {
              final HaxeComponentName forwardingComponentName = namedComponent.getComponentName();
              if (forwardingComponentName != null && forwardingComponentName.getText().equals(helperName)) {
                componentName = forwardingComponentName;
                break;
              }
            }
          }
        }
      }
    }
    if (log.isTraceEnabled()) {
      String ctext = componentName != null ? componentName.getText() : null;
      if (log.isTraceEnabled()) log.trace(traceMsg("Found component name " + (ctext != null ? ctext : "<no text>")));
    }
    return componentName;
  }

  @NotNull
  public static HaxeResolveResult fullyResolveTypedef(@Nullable HaxeClass typedef, @Nullable HaxeGenericSpecialization specialization) {
    if (null == typedef) return HaxeResolveResult.EMPTY;

    HashSet<String> recursionGuard = new HashSet<>(); // Track which typedefs we've already resolved so we don't end up in an infinite loop.

    HaxeResolveResult result = HaxeResolveResult.EMPTY;
    HaxeClassModel model = typedef.getModel();
    while (null != model && model.isTypedef() && !recursionGuard.contains(model.getName())) {
      recursionGuard.add(model.getName());
      final HaxeTypeOrAnonymous toa = model.getUnderlyingTypeOrAnonymous();
      if (toa != null) {
        final HaxeType type = toa.getType();
        if (null == type) {
          // Anonymous structure
          result = HaxeResolveResult.create(toa.getAnonymousType(), specialization);
          break;
        }


      // If the reference is to a type parameter, resolve that instead.
      HaxeResolveResult nakedResult = specialization.get(type, type.getReferenceExpression().getIdentifier().getText());
      if (null == nakedResult) {
        nakedResult = type.getReferenceExpression().resolveHaxeClass();
      }
      // translate  type params from typedef left side to right side value
      HaxeGenericResolver genericResolver = new HaxeGenericResolver();
        HaxeTypeParam param = type.getTypeParam();
        if(param != null ) {
        HaxeGenericResolver localResolver = specialization.toGenericResolver(type);
          List<HaxeTypeParameterDeclaration> typeparameters = getTypeParameters(nakedResult);
          List<HaxeTypeListPart> typeParameterList = param.getTypeList();
        for (int i = 0; i < typeParameterList.size(); i++) {
          if (typeparameters.size() -1 < i) break;
          HaxeTypeParameterDeclaration parameter = typeparameters.get(i);
          HaxeTypeListPart part = typeParameterList.get(i);
          if (part.getTypeOrAnonymous() != null) {
            ResultHolder holder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(part.getTypeOrAnonymous(), localResolver);
            genericResolver.add(parameter, holder);
          }
          else if (part.getFunctionType() != null) {
            //TODO resolve  with resolver ?
            ResultHolder type1 = HaxeTypeResolver.getTypeFromFunctionType(part.getFunctionType());
            genericResolver.add(parameter, type1);
          }
        }
      }

      result = HaxeResolveResult.create(nakedResult.getHaxeClass(), HaxeGenericSpecialization.fromGenericResolver(null, genericResolver));
      model = null != result.getHaxeClass() ? result.getHaxeClass().getModel() : null;
      specialization = result.getSpecialization();
      }
    }
    return result;
  }

  private static List<HaxeTypeParameterDeclaration> getTypeParameters(HaxeResolveResult nakedResult) {
    HaxeClass haxeClass = nakedResult.getHaxeClass();
    if (haxeClass == null) return  List.of();
    HaxeGenericParam param = haxeClass.getGenericParam();
    if (param == null) return  List.of();
    return  param.getGenericListPartList().stream().map(HaxeTypeParameterDeclaration.class::cast).toList();
  }

  private static List<? extends PsiElement> asList(@Nullable PsiElement element) {
    if (log.isDebugEnabled()) {
      log.debug("Resolved as " + (element == null ? "empty result list."
                                                  : elide(element.toString(), MAX_DEBUG_MESSAGE_LENGTH)));
    }
    return element == null ? Collections.emptyList() : Collections.singletonList(element);
  }

  private static HaxeGenericResolver getGenericResolver(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    HaxeGenericSpecialization specialization = reference.getSpecialization();
    return null != specialization ? specialization.toGenericResolver(leftClass) : null;
  }

  private static List<? extends PsiElement> resolveBySuperClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                         @NotNull HaxeReference reference) {
    HaxeGenericResolver baseResolver = getGenericResolver(leftClass, reference);
    return resolveBySuperClassAndSymbol(leftClass, baseResolver, reference);
  }


  private static List<? extends PsiElement> resolveBySuperClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                         @Nullable HaxeGenericResolver resolver,
                                                                         @NotNull HaxeReference reference) {
    if (null == leftClass) {
      return EMPTY_LIST;
    }

    if (leftClass instanceof HaxeAbstractTypeDeclaration) {

      HaxeClassModel classModel = leftClass.getModel();
      HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel)classModel;
      return resolveByClassAndSymbol(abstractClassModel.getUnderlyingClass(resolver), resolver, reference);
    }
    else {

      Set<HaxeType> superclasses = new ArrayListSet<>();
      superclasses.addAll(leftClass.getHaxeExtendsList());
      superclasses.addAll(leftClass.getHaxeImplementsList());

      List<? extends PsiElement> result = EMPTY_LIST;
      for (HaxeType sup : superclasses) {
        HaxeReference superReference = sup.getReferenceExpression();
        HaxeResolveResult superClassResult = superReference.resolveHaxeClass();
        SpecificHaxeClassReference superClass = superClassResult.getSpecificClassReference(leftClass, resolver);
        result = resolveByClassAndSymbol(superClass.getHaxeClass(), superClass.getGenericResolver(), reference);
        if (null != result && !result.isEmpty()) {
          break;
        }
      }
      return result;
    }
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeResolveResult resolveResult,
                                                                    @NotNull HaxeReference reference) {
    if (resolveResult == null) {
      if (log.isDebugEnabled()) LogResolution(null, "(resolveByClassAndSymbol)");
    }
    return resolveResult == null ? Collections.<PsiElement>emptyList() : resolveByClassAndSymbol(resolveResult.getHaxeClass(),
                                                                                                 resolveResult.getGenericResolver(),
                                                                                                 reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    if (leftClass != null) { // no need wasting resources getting  resolver if type is null
      HaxeGenericResolver resolver = getGenericResolver(leftClass, reference);
      return resolveByClassAndSymbol(leftClass, resolver, reference);
    }
    return Collections.emptyList();
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                    @Nullable HaxeGenericResolver resolver,
                                                                    @NotNull HaxeReference reference) {
    // TODO: This method is very similar to resolveChain, and they should probably be combined.

    if (leftClass != null) {
      final HaxeClassModel leftClassModel = leftClass.getModel();
      HaxeBaseMemberModel member = leftClassModel.getMember(reference.getReferenceName(), resolver);
      if (member != null) return asList(member.getNamePsi());

      // if class is abstract try find in forwards
      if (leftClass.isAbstractType()) {
        HaxeAbstractClassModel model = (HaxeAbstractClassModel)leftClass.getModel();
        if (model.isForwarded(reference.getReferenceName())) {
          final HaxeClass underlyingClass = model.getUnderlyingClass(resolver);
          if (underlyingClass != null) {
            member = underlyingClass.getModel().getMember(reference.getReferenceName(), resolver);
            if (member != null) {
              return asList(member.getNamePsi());
            }
          }
        }
      }

      // try find using
      HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
      if (fileModel != null) {
        SpecificHaxeClassReference leftClassReference =
          SpecificHaxeClassReference.withGenerics(leftClassModel.getReference(),
                                                  null == resolver ? null : resolver.getSpecificsFor(leftClass));

        HaxeStdPackageModel stdPackageModel = (HaxeStdPackageModel)HaxeProjectModel.fromElement(leftClass).getStdPackage();
        final List<HaxeUsingModel> usingModels = new ArrayList<>(stdPackageModel.getGlobalUsings());
        usingModels.addAll(fileModel.getUsingModels());

        HaxeResolveUtil.walkDirectoryImports(fileModel, (importModel) -> {
          usingModels.addAll(importModel.getUsingModels());
          return true;
        });

        for (int i = usingModels.size() - 1; i >= 0; --i) {
          HaxeUsingModel model = usingModels.get(i);
          HaxeMethodModel method = model.findExtensionMethod(reference.getReferenceName(), leftClassReference);
          if (method != null) {
            return asList(method.getNamePsi());
          }
        }
      }
    }

    return Collections.emptyList();
  }

  private String traceMsg(String msg) {
    return HaxeDebugUtil.traceThreadMessage(msg, 120);
  }

  private static class ResolveScopeProcessor implements PsiScopeProcessor {
    private final List<PsiElement> result;
    private final PsiElement target;
    final String name;

    private ResolveScopeProcessor(List<PsiElement> result, String name, PsiElement target) {
      this.target = target;
      this.result = result;
      this.name = name;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, ResolveState state) {
      //TODO: should probably make a better solution for this using a HaxeComponentName
      if (element.getParent() instanceof HaxeEnumObjectLiteralElement) {
        // avoids adding target to list
        if (element == target) return true;
        // do not resolve a reference to elements later in the code
        if(element.getTextOffset() > target.getTextOffset())  return true;
        if (element.textMatches(name)) {
          result.add(element);
          return false;
        }
      }

      HaxeComponentName componentName = null;
      if (element instanceof HaxeComponentName) {
        componentName = (HaxeComponentName)element;
      }
      else if (element instanceof HaxeNamedComponent) {
        componentName = ((HaxeNamedComponent)element).getComponentName();
      }
      else if (element instanceof HaxeOpenParameterList parameterList) {
        componentName = parameterList.getUntypedParameter().getComponentName();
      }
      else if (element instanceof HaxeSwitchCaseExpr expr) {
        if (!executeForSwitchCase(expr)) return false;
      }
      else if (element instanceof HaxeExtractorMatchAssignExpression assignExpression) {
        if (assignExpression.getReferenceExpression() == target) return true;
        if (assignExpression.getReferenceExpression().textMatches(name)) {
          result.add(assignExpression.getReferenceExpression());
          return false;
        }
      }

      if (componentName != null &&  componentName.textMatches(name)) {
        result.add(componentName);
        return false;
      }
      return true;
    }

    private boolean executeForSwitchCase(HaxeSwitchCaseExpr expr) {
      if (expr.getSwitchCaseCaptureVar() != null) {
        HaxeComponentName componentName = expr.getSwitchCaseCaptureVar().getComponentName();
        if (name.equals(componentName.getText())) {
          result.add(componentName);
          return false;
        }
      }
      else {
        HaxeExpression expression = expr.getExpression();
        if (expression instanceof HaxeReference reference) {
          if (name.equals(reference.getText())) {
            //TODO mlo: figure out of non HaxeComponentName elements are OK in Result list
            result.add(expr);
            return false;
          }
        }
        else if (expression instanceof HaxeEnumArgumentExtractor extractor) {
          HaxeEnumExtractorArgumentList argumentList = extractor.getEnumExtractorArgumentList();

          List<HaxeEnumExtractedValue> list = argumentList.getEnumExtractedValueList();
          for (HaxeEnumExtractedValue extractedValue : list) {
            HaxeEnumExtractedValueReference valueReference = extractedValue.getEnumExtractedValueReference();
            if (valueReference != null) {
              HaxeComponentName componentName = valueReference.getComponentName();
              if (name.equals(componentName.getText())) {
                result.add(componentName);
                return false;
              }
            }
          }
        }
      }
      return true;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
      return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }
  }
}