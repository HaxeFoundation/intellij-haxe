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
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentBindMethod;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakePsiElement;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentStringCode;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeCallExpressionEvaluatorCacheService;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContextContainer;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
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

import static com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentStringCode.FAKE_PSI_KEY;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.canBeQname;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.textCanBeQname;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.findObjectLiteralType;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorHandlers.getArrayAccessTypeFromClass;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.EnumValueMatchUtil.isInsidePatternMatcher;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.EnumValueMatchUtil.isPatternMatcher;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.createContextForConstructorCall;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.createContextForMethodCall;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.*;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.getReferenceTextFromStubOrPsi;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.searchInSameFileForEnumValues;
import static com.intellij.plugins.haxe.util.HaxeStringUtil.elide;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  public static final int MAX_DEBUG_MESSAGE_LENGTH = 200;

  //static {  // Remove when finished debugging.
  //  LOG.setLevel(LogLevel.DEBUG);
  //  LOG.debug(" ========= Starting up debug logger for HaxeResolver. ==========");
  //}

  public static final HaxeResolver INSTANCE = new HaxeResolver();

  private static boolean reportCacheMetrics = false;   // Should always be false when checked in.
  private static final AtomicInteger dumbRequests = new AtomicInteger(0);
  private static final AtomicInteger requests = new AtomicInteger(0);
  private static final AtomicInteger resolves = new AtomicInteger(0);
  private final static int REPORT_FREQUENCY = 100;

  public static final List<? extends PsiElement> EMPTY_LIST = Collections.emptyList();

  private final RecursionGuard<PsiElement> resolveInnerRecursionGuard = RecursionManager.createGuard("resolveInnerRecursionGuard");

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
       /** See docs on {@link HaxeDebugUtil#isCachingDisabled} for how to set this flag. */
       boolean skipCachingForDebug = HaxeDebugUtil.isCachingDisabled();

      ProgressIndicatorProvider.checkCanceled();


       // If we are in dumb mode (e.g. we are still indexing files and resolving may
       // fail until the indices are complete), we don't want to cache the (likely incorrect)
       // results.
       boolean isDumb = DumbService.isDumb(reference.getProject());
       boolean skipCaching = skipCachingForDebug || isDumb;

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


  @Nullable
  private List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    boolean traceEnabled = log.isTraceEnabled();
    String referenceText = traceEnabled ?   getReferenceTextFromStubOrPsi(reference) : null;
    if (traceEnabled) {
      log.trace(traceMsg("-----------------------------------------"));
      log.trace(traceMsg("Resolving reference: " + referenceText));
    }

    List<? extends PsiElement> foundElements = resolveInnerRecursionGuard
            .computePreventingRecursion(reference, true, () -> doResolveInner(reference, incompleteCode, referenceText));


    if (traceEnabled) {
      log.trace(traceMsg("Finished  reference: " + referenceText));
      log.trace(traceMsg("-----------------------------------------"));
    }

    return foundElements;
  }


  private List<? extends PsiElement> doResolveInner(@NotNull HaxeReference reference, boolean incompleteCode, String referenceDebugText) {

    if (reportCacheMetrics) {
      resolves.incrementAndGet();
    }

    if (reference instanceof HaxeLiteralExpression || reference instanceof HaxeConstantExpression) {
      if (!(reference instanceof HaxeRegularExpression || reference instanceof HaxeStringLiteralExpression)) {
        return EMPTY_LIST;
      }
    }
    if(reference instanceof HaxeCallExpression) {
      return EMPTY_LIST;
    }

    PsiElement parent = reference.getParent();
    boolean isType = parent instanceof HaxeType || PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeTypeTag.class) != null;
    List<? extends PsiElement> result = checkIsTypeParameter(reference);

    // NOTE: always Keep checkIsType  high up, it is used a lot (ex. when resolving type for HaxeType)
    // and moving it down the stack will only result in unnecessary overhead and potential recursion problems
    if (result == null) result = checkIsType(reference); //HaxeReferenceExpression
    if (result == null) result = checkIsChain(reference);  //HaxeReferenceExpression

    if (result == null) result = checkIsAlias(reference);
    if (result == null) result = checkEnumMemberHints(reference);

    if (result == null) result = checkIsFullyQualifiedStatement(reference);
    if (result == null) result = checkIsSuperExpression(reference);
    if (result == null) result = checkIsNewExpression(reference);
    if (result == null) result = checkMacroIdentifier(reference);

    if (result == null) result = checkIsAccessor(reference);
    if (result == null) result = checkElementUsage(reference);


    // if we know we are looking for a type; and references got multiple parts we can skip
    // anything checking members and walking the tree structure
    if(!isQualifiedNameReferenceStructure(reference)) {
      if (result == null) result = checkEnumExtractor(reference);// do before walking tree
      if (result == null) result = checkReferenceInExtractorMatchExpression(reference);
      if (result == null) result = checkIsSwitchVar(reference);
      if (result == null) result = checkCaptureVarReference(reference);
      if (result == null) result = checkByTreeWalk(reference);  // Beware: This will also locate constraints in scope.
    }

    HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
    // search same file first (avoids incorrect resolve of common named Classes and member with same name in local file)
    if (result == null)result =  searchInSameFile(reference, fileModel, isType);
    if (result == null) result = checkIsModuleName(reference);
    if (result == null) result = checkIsClassName(reference);
    if (result == null) result = checkCaptureVar(reference);
    if (result == null) result = checkSwitchOnEnum(reference);
    if (result == null) result = checkMemberReference(reference); // must be after resolvers that can find identifier inside a method
    if (result == null) {


      if (fileModel != null) {
          List<PsiElement> matchesInImport = HaxeResolveUtil.searchInImports(fileModel, reference.getText());
        // Remove enumValues if we are resolving typeTag as typeTags should not be EnumValues
        // We also have to remove resolved fields as abstract enums is a thing
        if (isType) {
          matchesInImport = matchesInImport.stream().filter(this::removeNonTypeComponents).toList().reversed();
        }
        if (!matchesInImport.isEmpty()) {
            // one file may contain multiple enums and have enumValues with the same name; trying to match any argument list
            if (matchesInImport.size() > 1)
                if (parent instanceof HaxeCallExpression callExpression) {
                    int expectedSize = Optional.ofNullable(callExpression.getExpressionList()).map(e -> e.getExpressionList().size()).orElse(0);

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
                } else if (parent instanceof HaxeType type) {
                  // handle cases where we got both module and main-class match and we know we want the class
                  String memberName = type.getText();
                  for (PsiElement element : matchesInImport) {
                    if (element instanceof HaxeClass haxeClass) {
                      if (haxeClass.getModel().getName().equals(memberName)) {
                        return List.of(element);
                      }
                    }
                  }
                }
            return matchesInImport.isEmpty() ? null : matchesInImport;
          }
        boolean expectedEnumIsConstructor = parent instanceof HaxeCallExpression|| parent.getParent() instanceof  HaxeEnumArgumentExtractor;
        PsiElement target = HaxeResolveUtil.searchInSamePackage(fileModel, reference.getText(), true, expectedEnumIsConstructor);

        if (target != null) {
          LogResolution(reference, "via import.");
          return asList(target);
        }
      }

      List<@NotNull PsiElement> resolvedPackage = checkQualifiedName(reference);
      if (resolvedPackage != null) return resolvedPackage;
    }
    if (result == null) result = checkIsForwardedName(reference);
    if (result == null) result = checkGlobalAlias(reference);
    if (result == null) result = checkIsLocalModule(reference);

    if(result == null) {
      // check if this can be a switch extract variable,
      // checking here (and not in switch var method) because we want to make sure all other type resolve has been tried
      result = checkIfNamedSwitchValue(reference);
    }

    if (result == null) {
      LogResolution(reference, "failed after exhausting all options.");
      return EMPTY_LIST; // empty list means cache not found
    }

    if (log.isTraceEnabled()) {
      String message = "caching result for :" + referenceDebugText;
      traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
    }

    return result;

  }

  private boolean removeNonTypeComponents(PsiElement element) {
    if (element instanceof HaxeComponentName componentName) {
      element = componentName.getParent();
    }
    return switch (element) {
      case HaxeEnumValueDeclaration declaration -> false;
      case HaxePsiField declaration -> false;
      case HaxeMethod declaration -> false;
      default -> true;
    };
  }

  private static @Nullable List<@NotNull PsiElement> checkQualifiedName(@NotNull HaxeReference reference) {
    if (PsiNameHelper.getInstance(reference.getProject()).isQualifiedName(reference.getText())) {
      List<HaxeModel> resolvedPackage =
        HaxeProjectModel.fromElement(reference).resolve(new FullyQualifiedInfo(reference.getText()), reference.getResolveScope());
      if (resolvedPackage != null && !resolvedPackage.isEmpty() && resolvedPackage.getFirst() instanceof HaxePackageModel) {
        LogResolution(reference, "via project qualified name.");
        return Collections.singletonList(resolvedPackage.get(0).getBasePsi());
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkIsLocalModule(@NotNull HaxeReference reference) {
    PsiFile containingFile = reference.getContainingFile();
    PsiDirectory containingDirectory = containingFile.getContainingDirectory();
    if(containingDirectory != null) {
      // TODO skip  if  contains symbols
      PsiFile file = containingDirectory.findFile(reference.getText() + ".hx");
      if (file instanceof HaxeFile haxeFile) {
        HaxeFileModel model = haxeFile.getModel();
        if (model.getModuleBody() != null) return List.of(model.getModuleBody());
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkIfNamedSwitchValue(@NotNull HaxeReference reference) {
    if(reference.getParent() instanceof  HaxeEnumExtractedValue ) {
      return checkIfSwitchCaseDefaultValue(reference, true);
    }
    return null;
  }

  // Experimental
  // attempt at correctly resolve types that are not in import statements when valueExpressions
  // are used as parameters or assigned to other classes.
  private List<? extends PsiElement> checkElementUsage(@NotNull PsiElement reference) {

    if (reference instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement parent = reference.getParent();
      if (parent == null) return null;
      ResultHolder expectedType = findParentAssignType(referenceExpression);
      if (expectedType != null) {
        if (expectedType.getClassType() != null) {
          HaxeClass haxeClass = expectedType.getClassType().getHaxeClass();
          if (haxeClass != null) {
            HaxeClassModel model = haxeClass.getModel();
            // check if typedef of Enum
            if(model.isTypedef()) {
              ResultHolder resolved = model.getInstanceReference().fullyResolveTypeDefAndUnwrapNullTypeReference().createHolder();
              if(resolved.getClassType() != null) {
                if(resolved.getClassType().isEnumType()) {
                  HaxeClass resolvedHaxeClass = resolved.getClassType().getHaxeClass();
                  if(resolvedHaxeClass != null) haxeClass = resolvedHaxeClass;
                }
              }
            }
            // check Enum values
            if(haxeClass.isEnum()) {
              if(haxeClass.getModel() instanceof  HaxeEnumModel enumModel) {
                boolean isInPatternMatcher = isInsidePatternMatcher(reference);
                for (HaxeEnumValueModel value : enumModel.getValues()) {
                  if(reference.textMatches(value.getName())) {
                    if (value instanceof HaxeEnumValueConstructorModel constructorModel) {
                      // validate parameters if callExpression ignore if EnumExtractor
                      if (!(reference.getParent() instanceof HaxeEnumValueReference)) {
                        if (isInPatternMatcher) {
                          return List.of(constructorModel.getNamePsi());
                        }
                        boolean isValidConstructor = testAsEnumValueConstructor(constructorModel.getEnumValuePsi(), referenceExpression);
                        if (isValidConstructor) {
                          return List.of(constructorModel.getNamePsi());
                        }
                      }
                    } else if (value instanceof HaxeEnumValueFieldModel fieldModel) {
                      return List.of(fieldModel.getNamePsi());
                    }
                  }
                }
              }
              // check Class names
            } else {
              HaxeComponentName componentName = haxeClass.getComponentName();
              if (componentName != null && componentName.textMatches(reference)) {
                return List.of(componentName);
              }

            }
          }
        }
      }
    }
    return null;
  }


  // Experimental
  // try to step one level up until we find a type definition and then pass that back down
  private ResultHolder findParentAssignType(@NotNull PsiElement reference) {
    return findParentAssignType(reference, false);
  }
  private ResultHolder findParentAssignType(@NotNull PsiElement reference, boolean isValueExpression) {
    PsiElement parent = reference.getParent();
    if (parent == null) return null;

    if(parent instanceof  HaxeValueExpression) {
      isValueExpression = true;
    }

    // final psi types (the ones where we can get expected type from)
    if(isValueExpression) {

      if (parent instanceof HaxeNewExpression newExpression) {
        PsiElement resolve = newExpression.getType().getReferenceExpression().resolve();
        if (resolve instanceof HaxeClassDeclaration declaration) {
          HaxeMethodModel constructor = declaration.getModel().getConstructor(null);
          if (constructor != null) {
            int index = newExpression.getExpressionList().indexOf(reference);
            HaxeCallExpressionContextContainer contextContainer = createContextForConstructorCall(newExpression);
            HaxeCallExpressionEvaluation evaluation = contextContainer.evaluateContexts();
            if (evaluation != null) {
              return evaluation.getParameterType(index);
            }
          }
        }
      }
      if (parent instanceof HaxeCallExpressionList expressionList) {
        HaxeCallExpression callExpression = PsiTreeUtil.getParentOfType(expressionList, HaxeCallExpression.class);
        if (callExpression != null) {
          if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
            PsiElement resolve = referenceExpression.resolve();
            if (resolve instanceof HaxeMethod method) {
              int index = expressionList.getExpressionList().indexOf(reference);


              HaxeCallExpressionEvaluation evaluate = cachedHaxeCallExpressionEvaluation(method, callExpression);
              if(evaluate != null){
                ResultHolder parameterType = evaluate.getParameterType(index);
                if(parameterType != null) return parameterType;
              }

              //TODO mlo  - HACK:
              // A workaround for recursion issues when resolving enum value constructor and the call expression got
              // anonymous structures as arguments, the CallExpression evaluation fails and we are unable to determine the type.
              // .
              // The problem occurs when the code tries to evaluate callExpression and logic to find genericResolver from parent causes issues.
              //  ex. the canAssign for anonymous structures triggers another resolve when it tries to get genericResolver.
              // .
              // This hack only works for EnumValues that does not have generics and dont use optional  parameters
              //
              if (method.getModel() instanceof HaxeEnumValueConstructorModel enumValueConstructorModel) {
                HaxeClassModel declaringEnum = enumValueConstructorModel.getDeclaringEnum();
                if (declaringEnum != null) {
                  List<HaxeGenericParamModel> genericParams = declaringEnum.getGenericParams();
                  if (genericParams.isEmpty()) {
                    List<HaxeParameterModel> parameters = enumValueConstructorModel.getParameters();
                    for (int i = 0; i < parameters.size(); i++) {
                      HaxeParameterModel parameter = parameters.get(i);
                      if (parameter.isOptional()) break;
                      if (i == index) return parameter.getType(null);
                    }
                  }
                }
              }
              return null;
            }
          }
        }
      }
      if (parent instanceof HaxeVarInit varInit) {
        HaxePsiField field = PsiTreeUtil.getStubOrPsiParentOfType(varInit, HaxePsiField.class);
        if (field != null) {
          HaxeTypeTag typeTag = field.getTypeTag();
          if (typeTag != null) {
            return HaxeTypeResolver.getTypeFromTypeTag(typeTag, field);
          }
        }
      }
      if (parent instanceof HaxeReturnStatement returnStatement) {
        HaxeMethod method = PsiTreeUtil.getStubOrPsiParentOfType(returnStatement, HaxeMethod.class);
        if(method != null) {
          HaxeMethodModel model = method.getModel();
          HaxeTypeTag tagPsi = model.getReturnTypeTagPsi();
          if(tagPsi != null) {
            return model.getReturnType(null);
          }
        }
      }
    }else {
      if (parent instanceof HaxeReturnStatement returnStatement) {
        HaxeMethod method = PsiTreeUtil.getStubOrPsiParentOfType(returnStatement, HaxeMethod.class);
        if(method != null) {
          HaxeMethodModel model = method.getModel();
          HaxeTypeTag tagPsi = model.getReturnTypeTagPsi();
          if(tagPsi != null) {
            return model.getReturnType(null);
          }
        }
      }
      if (parent instanceof HaxeNewExpression newExpression) {
        int index = newExpression.getExpressionList().indexOf(reference);
        if(index>-1) {
          HaxeCallExpressionContextContainer contextContainer = createContextForConstructorCall(newExpression);
          HaxeCallExpressionEvaluation evaluation = contextContainer.evaluateContexts();
          if (evaluation != null) {
            Integer paramIndex = evaluation.getArgumentToParameterMapping().get(index);
            if (paramIndex != null) {
              return evaluation.getParameterType(index);
            }
          }
        }
      }
      if (parent instanceof HaxeCallExpressionList expressionList && parent.getParent() instanceof HaxeCallExpression callExpression) {
        if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement resolve = referenceExpression.resolve();
          if (resolve instanceof HaxeMethod method) {

            // enumRef.match(enumMember) expects members from the same Enum so it should resolve members without the need of imports.
            if (isPatternMatcher(method)) {
              ResultHolder type = findTypeFromPatternMatchExpression(referenceExpression);
              if(type != null && type.isEnum()) return type;
            }

            int index = expressionList.getExpressionList().indexOf(reference);
            if(index>-1) {
              HaxeCallExpressionEvaluation evaluate = cachedHaxeCallExpressionEvaluation(method, callExpression);
              if (evaluate != null && evaluate.isValid() && evaluate.isCompleted()) {
                return evaluate.getParameterType(index);
              }
            }
          }
        }
      }
    }

    if (parent instanceof HaxeObjectLiteralElement literalElement) {
      if (literalElement.getParent() instanceof HaxeObjectLiteral objectLiteral) {
        ResultHolder parentAssignType = findParentAssignType(objectLiteral, true);
        if (parentAssignType != null) {
         boolean canBeObjectLiteral = parentAssignType.isAnonymousType();
          SpecificHaxeClassReference classType = parentAssignType.getClassType();
          if(!canBeObjectLiteral && classType != null) {
            HaxeClassModel haxeClassModel = classType.getHaxeClassModel();
            canBeObjectLiteral = haxeClassModel != null && haxeClassModel.isStructInit();
          }
          if(canBeObjectLiteral) {
            HaxeClassModel model = classType.getHaxeClassModel();
            HaxeBaseMemberModel member = model.getMember(literalElement.getName(), null);
            if (member != null) {
              ResultHolder resultType = member.getResultType(null);
              if (resultType != null && !resultType.isUnknown()) {
                return resultType;
              }
            }
          }
        }
      }
    }

    if (parent instanceof HaxeArrayLiteral) {
      ResultHolder type = findParentAssignType(parent, isValueExpression);
      // unwrap array type since its a literal we are in
      if(type!= null && !type.isUnknown() && type.getClassType() != null) {
        @NotNull ResultHolder[] specifics = type.getClassType().getSpecifics();
        if(specifics.length > 0 ) return specifics[0];
      }
      return null;
    }

    if (reference instanceof HaxeEnumValueReference) {
      if (parent instanceof HaxeEnumArgumentExtractor extractor) {
        // same kind of relation between the reference expression to a method and a callExpression
        // parent is  not a "new" element, just more of the same expression
        if(extractor.getEnumValueReference().getReferenceExpression().textMatches(reference)) {
          // check if our extractor is inside another extractor
          if (extractor.getParent() instanceof HaxeEnumExtractorArgumentList argumentList) {
            int index = List.of(argumentList.getChildren()).indexOf(extractor);
            if (index > -1) {
              if(argumentList.getParent() instanceof HaxeEnumArgumentExtractor parentArgumentExtractor) {
                PsiElement resolve = parentArgumentExtractor.getEnumValueReference().getReferenceExpression().resolve();
                if(resolve instanceof HaxeEnumValueDeclarationConstructor constructor) {
                  List<HaxeParameterModel> parameters = constructor.getModel().getParameters();
                  if(parameters.size()>index) {
                    return parameters.get(index).getType();
                  }
                }
              }
            }
          }
        }
      }
    }

    return findParentAssignType(parent, isValueExpression);
  }

  private @Nullable ResultHolder findTypeFromPatternMatchExpression(HaxeReferenceExpression referenceExpression) {
    HaxeReference leftReference = HaxeResolveUtil.getLeftReference(referenceExpression);
    if (leftReference instanceof HaxeReferenceExpression callieReference) {
      ResultHolder callieType = HaxeExpressionEvaluator.evaluate(callieReference).result;
      if (callieType != null && callieType.getType() instanceof SpecificHaxeClassReference classReference) {
        SpecificTypeReference resolvedType = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
        if (resolvedType.isEnumReference()) return resolvedType.createHolder();
      }
    }
    return null;
  }



  private static @Nullable HaxeCallExpressionEvaluation cachedHaxeCallExpressionEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {

    HaxeCallExpressionEvaluatorCacheService service = method.getProject().getService(HaxeCallExpressionEvaluatorCacheService.class);
    HaxeCallExpressionEvaluation evaluation = service.callExpressionCachedEvaluation(method, callExpression);
    return evaluation;
  }

  private static boolean testAsEnumValueConstructor(@NotNull HaxeEnumValueDeclarationConstructor enumValueDeclaration, @NotNull HaxeReference reference) {
      if (reference.getParent() instanceof HaxeCallExpression haxeCallExpression) {
        HaxeMethod method = enumValueDeclaration.getModel().getMethod();
        HaxeCallExpressionEvaluation validation = cachedHaxeCallExpressionEvaluation(method, haxeCallExpression);
        return validation != null && validation.isCompleted() && validation.isValid();
      }
    return false;
  }

  private List<? extends PsiElement> checkIsSwitchExtractedValue(PsiElement psiElement) {
    if (psiElement instanceof  HaxeEnumExtractedValueReference extractedValueReference) {
      if(extractedValueReference.getParent() instanceof HaxeEnumExtractedValue extractedValue)
      if(extractedValue.getParent() instanceof HaxeEnumExtractorArgumentList argumentList) {
        if (argumentList.getParent() instanceof HaxeEnumArgumentExtractor extractor) {
          int argumentIndex = getExtractorArgumentIndex(extractedValueReference, extractor);
          if(argumentIndex > -1) {
            HaxeParameter parameter = findExtractedValueEnumParameter(extractor, argumentIndex);
            if(parameter != null) return List.of(parameter.getComponentName());
          }
        }
      }
    }
    HaxeComponentName haxeComponentName = searchInEnumObjectAndArrayExtractors(psiElement);
    if(haxeComponentName != null) return List.of(haxeComponentName);

    return null;
  }

  private HaxeComponentName searchInEnumObjectAndArrayExtractors(PsiElement psiElement) {
    if(psiElement.getParent() instanceof  HaxeEnumExtractedValue extractedValue) {
      if (extractedValue.getParent() instanceof HaxeEnumObjectLiteralElement objectLiteral) {
        HaxeModel model = getModelForElement(objectLiteral);
        if (model instanceof HaxeBaseMemberModel memberModel) {
          return memberModel.getNamePsi();
        }
      }
      if (extractedValue.getParent() instanceof HaxeEnumExtractArrayLiteral arrayLiteral) {
        HaxeModel model = getModelForElement(arrayLiteral.getParent());
        if (model instanceof HaxeBaseMemberModel memberModel) {
          ResultHolder resultType = memberModel.getResultType(null);
          if (resultType != null && resultType.getClassType() != null) {
            ResultHolder arrayAccessType = getArrayAccessTypeFromClass(resultType.getClassType());
            if (arrayAccessType != null) {
              HaxeModel typeModel = getGetModelFromResultHolder(arrayAccessType);
              if (typeModel instanceof HaxeClassModel classModelModel) {
                return classModelModel.getPsi().getComponentName();
              }
            }
          }
        }
      }
    }
    return null;
  }


  private HaxeModel getModelForElement(PsiElement psiElement) {
    PsiElement parent = psiElement.getParent();
    if(psiElement instanceof  HaxeEnumObjectLiteralElement objectLiteralElement) {
      if(parent instanceof  HaxeEnumExtractObjectLiteral objectLiteral) {
        int index = new ArrayList<>(objectLiteral.getEnumObjectLiteralElementList()).indexOf(objectLiteralElement);
        if(index> -1) {
          HaxeModel modelForElement = getModelForElement(objectLiteral.getParent());
          if (modelForElement instanceof HaxeBaseMemberModel fieldModel) {
            HaxeModel fieldTypeModel = getGetModelFromResultHolder(fieldModel.getResultType());
            if(fieldTypeModel instanceof HaxeClassModel fieldClassModel) {
              return fieldClassModel.getMember(objectLiteralElement.getName(), null);
            }
          }
          if (modelForElement instanceof HaxeClassModel classModel) {
            return classModel.getMember(objectLiteralElement.getName(), null);
          }
          if (modelForElement instanceof HaxeEnumValueConstructorModel constructorModel) {
            ResultHolder parameterType = constructorModel.getParameterType(index, null);
            if(parameterType != null && !parameterType.isUnknown()) {
              return getGetModelFromResultHolder(parameterType);
            }
          }
        }
      }
    }


    if(psiElement instanceof  HaxeEnumExtractedValue extractedValue) {
      if(parent instanceof  HaxeEnumExtractorArgumentList argumentList) {
        if (argumentList.getParent() instanceof HaxeEnumArgumentExtractor extractor) {
          int index = List.of(argumentList.getChildren()).indexOf(extractedValue);
          if (index > -1) {
            HaxeParameter parameter = findExtractedValueEnumParameter(extractor, index);
            if(parameter != null) {
              ResultHolder result = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), extractor);
              return getGetModelFromResultHolder(result);
            }
          }
        }
      }
    }

    if(psiElement instanceof  HaxeEnumArgumentExtractor extractor) {}
    if(psiElement instanceof  HaxeConstructorDeclaration constructor) {
      return constructor.getModel();
    }

    //

    return null;
  }

  private HaxeModel getGetModelFromResultHolder(ResultHolder result ) {
    if (result.getClassType() != null) {
      return result.getClassType().getHaxeClassModel();
    }else if(result.getEnumValueType() != null) {
      return result.getEnumValueType().getModel();
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

      boolean isMethodOrConstructor = reference.getParent() instanceof HaxeCallExpression;
      PsiElement referenceParent = isMethodOrConstructor ? reference.getParent().getParent() : reference.getParent();

      if (referenceParent instanceof HaxeEnumValueReference enumValueReference) {
        HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class, true);
        if (switchCaseExpr != null) {
          HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);
          if (matchExpression != null) {
            List<HaxeComponentName> names = evaluateAndFindEnumMember(reference, matchExpression.getExtractorExpression());
            if (names != null && !names.isEmpty()) return names;
          }
          HaxeEnumExtractorArgumentList argumentList = PsiTreeUtil.getParentOfType(reference, HaxeEnumExtractorArgumentList.class);
          if(argumentList != null) {
            HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(enumValueReference.getParent(), HaxeEnumArgumentExtractor.class);
            if (argumentExtractor != null) {
              HaxeReferenceExpression referenceExpression = argumentExtractor.getEnumValueReference().getReferenceExpression();
              PsiElement resolve = referenceExpression.resolve();
              if (resolve instanceof HaxeEnumValueDeclarationConstructor constructor) {
                if (constructor.getModel() instanceof HaxeEnumValueConstructorModel enumValueModel) {
                  int argumentIndex = findExtractorIndex(switchCaseExpr.getChildren(), argumentExtractor);
                  if (argumentIndex > -1) {
                    HaxeParameterList parameters = enumValueModel.getConstructorParameters();
                    if(parameters != null){
                      HaxeParameter haxeParameter = parameters.getParameterList().get(argumentIndex);
                      ResultHolder typeGuess = HaxeTypeResolver.getTypeFromTypeTag(haxeParameter.getTypeTag(), haxeParameter);
                      SpecificTypeReference expectedType = typeGuess.getType();
                      if(typeGuess.isTypeDef()) {
                        expectedType = typeGuess.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
                      }
                      if(expectedType instanceof  SpecificHaxeClassReference classReference) {
                        HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
                        if(haxeClassModel != null) {
                          HaxeBaseMemberModel member = haxeClassModel.getMember(reference.getText(), null);
                          if (member != null) {
                            return List.of(member.getBasePsi());
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          HaxeSwitchStatement parentSwitch = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
          if (parentSwitch != null) {
            HaxeExpression expression = parentSwitch.getExpression();
            while (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
              expression = parenthesizedExpression.getExpression();
            }

            PsiElement fromPath = enumMemberTraverseUsagePath(reference);
            if (fromPath instanceof HaxeEnumDeclaration enumDeclaration) {
              HaxeBaseMemberModel member = enumDeclaration.getModel().getMember(reference.getText(), null);
              if (member != null) {
                return List.of(member.getNamedComponentPsi());
              }
            } else if (fromPath instanceof HaxeExpression haxeExpression) {
              List<HaxeComponentName> components = evaluateAndFindEnumMember(reference, haxeExpression);
              if (components != null) return components;
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
        HaxeAssignExpression assignExpression = PsiTreeUtil.getParentOfType(referenceParent, HaxeAssignExpression.class, false, HaxeCallExpression.class);
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


        HaxePsiField field =
          fieldFromReferenceExpression != null ? fieldFromReferenceExpression : PsiTreeUtil.getParentOfType(referenceParent, HaxePsiField.class, true, HaxeCallExpression.class, HaxeNewExpression.class);
        HaxeParameter parameter = parameterFromReferenceExpression != null
                                  ? parameterFromReferenceExpression
                                  : PsiTreeUtil.getParentOfType(referenceParent, HaxeParameter.class, true, HaxeCallExpression.class, HaxeNewExpression.class);
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
        PsiElement PossibleCallExpression = referenceParent;
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

      if(referenceParent instanceof  HaxeSwitchCaseExpr || referenceParent instanceof HaxeSwitchCaseExprArray) {
        List<? extends PsiElement> psiElements = checkIsSwitchVar(reference);
        if(psiElements != null && !psiElements.isEmpty()) {
          SpecificTypeReference result = HaxeExpressionEvaluator.evaluateFullyResolved(psiElements.getFirst());
          if (!result.isUnknown()) {
            return findEnumMember(reference, result);
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
              HaxeCallExpressionEvaluation validation = cachedHaxeCallExpressionEvaluation(haxeMethod, methodCallCall);
              if(validation != null) {
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
    }
    if (constructorCall != null) {
      List<HaxeExpression> argumentList = constructorCall.getExpressionList();
      int argumentIndex = argumentList.indexOf(argument);
      if (argumentIndex> -1) {
        HaxeCallExpressionContextContainer contextContainer = createContextForConstructorCall(constructorCall);
        HaxeCallExpressionEvaluation evaluation = contextContainer.evaluateContexts();
        if (evaluation != null) {
          int parameterIndex = evaluation.getParameterForArgument(argumentIndex);
          ResultHolder parameterType = evaluation.getParameterType(parameterIndex);
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
            List<HaxeNamedComponent> members = haxeClass.findHaxeMemberByName(reference.getText(), null);
            if (!members.isEmpty()) {
              HaxeNamedComponent name = members.getFirst();
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
      boolean isCallExpression = false;
      if (reference.getParent() instanceof HaxeCallExpression) {
        isCallExpression = true;
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
        // guard against resolving incorrect enum when there's a mismatch between value and constructor enumType
        if (namedComponent instanceof HaxeEnumValueDeclarationField) {
          if(isCallExpression) return null;
        }
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
    //   abstract:  access abstract members
    //   this:      this class member access / underlying type access (abstract)
    //   super:      super class member access (used when overriding methods and calling base method)

    boolean isEmpty = leftReference == null;
    boolean isAbstract = !isEmpty && leftReference.textMatches("abstract");
    boolean isSuper = !isEmpty && leftReference.textMatches("super");
    boolean isThis = !isEmpty && leftReference.textMatches("this");
    if (isEmpty| isThis || isSuper || isAbstract) {

      HaxeClass type = PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeClass.class);
      if (type instanceof HaxeAbstractTypeDeclaration) {

        if(isAbstract || isEmpty) {
          List<? extends PsiElement> superElements = resolveBySuperClassAndSymbol(type, reference, false);
          if (!superElements.isEmpty()) {
            LogResolution(reference, "via abstract field. (Abstract - abstract keyword)");
            return superElements;
          }
        }else if (isThis) {
          List<? extends PsiElement> superElements = resolveBySuperClassAndSymbol(type, reference, true);
          superElements = removeStaticMembersFromElementList(superElements);
          if (!superElements.isEmpty()) {
            LogResolution(reference, "via super field. (Abstract - this keyword)");
            return superElements;
          }
        }
      } else {
        List<? extends PsiElement> superElements = resolveBySuperClassAndSymbol(type, reference, false);
        superElements = removeStaticMembersFromElementList(superElements);
        if (!superElements.isEmpty()) {
          LogResolution(reference, "via super field. (class)");
          return superElements;
        }
      }

    }
    return null;
  }

  private List<? extends PsiElement> removeStaticMembersFromElementList(List<? extends PsiElement> superElements) {
    return superElements.stream()
            .filter(psiElement -> !(psiElement.getParent() instanceof HaxePsiField field && field.isStatic()))
            .toList();
  }

  private List<? extends PsiElement> checkIsTypeParameter(HaxeReference reference) {
    if(reference instanceof HaxeReferenceExpression) {
      HaxeTypeTag typeTag = PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeTypeTag.class);
      if (typeTag != null) {
        //HaxeReferenceExpression.class
        HaxeFieldDeclaration fieldDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeFieldDeclaration.class);
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

        HaxeMethodDeclaration methodDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(typeTag, HaxeMethodDeclaration.class);
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

        HaxeConstructorDeclaration constructorDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(typeTag, HaxeConstructorDeclaration.class);
        if (constructorDeclaration != null) {
          // reference is a type tag in constructor, we should check  owning class type parameters
          // so we won't resolve this to a type outside the class if its a type parameter
          HaxeClassModel declaringClass = constructorDeclaration.getModel().getDeclaringClass();
          if (declaringClass != null) {
            List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
            return findTypeParameterPsi(reference, params);
          }
        }
        HaxeEnumValueDeclaration enumDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(typeTag, HaxeEnumValueDeclaration.class);
        if (enumDeclaration != null) {
          // EnumValueDeclarations does not define TypeParameters, only the parent EnumType can have these.
          HaxeClassModel declaringClass = ((HaxeEnumValueModel)enumDeclaration.getModel()).getDeclaringEnum();
          if (declaringClass != null) {
            List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
            return findTypeParameterPsi(reference, params);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeComponentName> findTypeParameterPsi(HaxeReference reference, List<HaxeGenericParamModel> params) {
    String referenceText = reference.getText();
    Optional<HaxeGenericListPart> first = params.stream()
      .filter(p -> p.getName().equals(referenceText))
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

  private static List<? extends PsiElement> checkEnumExtractor(HaxeReference reference) {


    PsiElement parent = reference.getParent();
    if (parent instanceof HaxeEnumValueReference) {
      HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class);
      SpecificHaxeClassReference classReference = HaxeResolveUtil.resolveExtractorEnum(argumentExtractor);
      if (classReference != null) {
        PsiElement declaration = HaxeResolveUtil.resolveEnumValueDeclaration(classReference, argumentExtractor);
        if (declaration != null) {
          LogResolution(reference, "via enum extractor");
          return List.of(declaration);
        }
      }
    } else if (parent instanceof HaxeExtractorMatchAssignExpression assignExpression) {
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
            PsiElement declaration = HaxeResolveUtil.resolveEnumValueDeclaration(classReference, reference.getText());
            if (declaration != null) {
              LogResolution(reference, "via enum extractor");
              return List.of(declaration);
            }
          }
        }
        if (assignExpression.getParent() instanceof HaxeSwitchCaseExtractor extractor) {
          List<HaxeExpression> list = extractor.getExpressionList();
          if (!list.isEmpty()) {
            PsiElement expression1 = list.getLast();
            while (expression1 instanceof HaxeExtractorMatchExpression matchExpression) {
              HaxeSwitchCaseExpr expr = matchExpression.getMatch();
              @NotNull PsiElement[] children = expr.getChildren();
              expression1 = children[children.length - 1];
            }
            LogResolution(reference, "via switch-case extractor match assign");
            return List.of(expression1);
          }
        } else {
          LogResolution(reference, "via switch-case reference as var (without var keyword)");
          if (expression != null) return List.of(expression);
        }
      }
    }
    // check if enum inside enum ex. case SomeEnum(AnotherEnum(x,y,z))
    else if (parent instanceof HaxeType  type
             && type.getParent() instanceof HaxeEnumExtractorArgumentList argumentList
             && argumentList.getParent() instanceof HaxeEnumArgumentExtractor extractor
    ) {
      int index = -1;
        @NotNull PsiElement @NotNull [] children = argumentList.getChildren();
        for (int i = 0; i < children.length; i++) {
            PsiElement child = children[i];
            if (child == type) {
                index = i;
                break;
            }
        }
        if(index > -1) {
          PsiElement resolve = extractor.getEnumValueReference().getReferenceExpression().resolve();
          if(resolve  instanceof HaxeEnumValueDeclarationConstructor constructor) {
            List<HaxeParameterModel> parameters = constructor.getModel().getParameters();
            if(parameters.size()> index) {
              ResultHolder paramType = parameters.get(index).getType();
              if(paramType.isEnum()){
                SpecificHaxeClassReference classType = paramType.getClassType();
                if(classType != null && classType.getHaxeClassModel() instanceof HaxeEnumModel enumModel) {
                  HaxeEnumValueModel value = enumModel.getValue(reference.getText());
                  if(value != null && value.getEnumValuePsi() != null){
                    return List.of(value.getEnumValuePsi());
                  }
                }
              }
            }
          }
        }

    }
    return null;
  }

  private static @Nullable List<@NotNull PsiElement> checkReferenceInExtractorMatchExpression(HaxeReference reference) {
    if(reference.getFirstChild() instanceof HaxeIdentifier ) {
      HaxeExtractorMatchExpression extractorMatchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);
      if (extractorMatchExpression != null) {

        HaxeExpression extractorExpression = extractorMatchExpression.getExtractorExpression();
        HaxeSwitchCaseExpr match = extractorMatchExpression.getMatch();
          if (match.getExpression() == reference) {
          return List.of(extractorExpression);
          }

          boolean isFromExtractorPart = PsiTreeUtil.isAncestor(extractorExpression, reference, false);
        if(isFromExtractorPart) {
          HaxeCallExpressionList expressionList = PsiTreeUtil.getParentOfType(reference, HaxeCallExpressionList.class);
          if(expressionList != null) {
            int enumParameterIndex = expressionList.getExpressionList().indexOf(extractorMatchExpression);
            if(enumParameterIndex>-1) {
              if(expressionList.getParent() instanceof HaxeCallExpression callExpression) {
                if(callExpression.getExpression() instanceof HaxeReferenceExpression enumReference) {
                  PsiElement resolve = enumReference.resolve();
                  if(resolve instanceof HaxeEnumValueDeclarationConstructor constructor) {
                    HaxeParameterList parameterList = constructor.getParameterList();
                    PsiParameter parameter = parameterList.getParameter(enumParameterIndex);
                    if(parameter instanceof HaxeParameterImpl haxeParameter) {
                      return List.of(haxeParameter.getComponentName());
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkCaptureVarReference(HaxeReference reference) {
    if (reference instanceof HaxeReferenceExpression || reference instanceof  HaxeEnumExtractedValueReference) {
      HaxeSwitchCase switchCase = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCase.class);
      if (switchCase != null) {
        // references in "case" expression (ex. case myVar)
        if(reference.getParent() instanceof HaxeSwitchCaseExpr switchCaseExpr) {
          if (switchCaseExpr.getParent() instanceof HaxeExtractorMatchExpression matchExpression) {
            HaxeExpression PossibleCapture = matchExpression.getMatch().getExpression();
            if (PossibleCapture != null && PossibleCapture.textMatches(reference)) {
              LogResolution(reference, "via switch argument extractor");
              return List.of(PossibleCapture);
            }
          }
        }
        HaxeSwitchCaseBlock switchCaseBlock = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseBlock.class);
        if(switchCaseBlock != null) {
          for (HaxeSwitchCaseExpr expr : switchCase.getSwitchCaseExprList()) {
            Collection<HaxeSwitchCaseExpr> haxeSwitchCaseExprs = PsiTreeUtil.findChildrenOfType(expr, HaxeSwitchCaseExpr.class);
            for (HaxeSwitchCaseExpr haxeSwitchCaseExpr : haxeSwitchCaseExprs) {
              if (haxeSwitchCaseExpr.getParent() instanceof HaxeEnumArgumentExtractor extractor) {
                List<HaxeExpression> expressionList = extractor.getEnumExtractorArgumentList().getExpressionList();
                for (HaxeExpression haxeExpression : expressionList) {
                  if (haxeExpression instanceof HaxeExtractorMatchExpression matchExpression) {
                    HaxeExpression PossibleCapture = matchExpression.getMatch().getExpression();
                    if (PossibleCapture != null && PossibleCapture.textMatches(reference)) {
                      LogResolution(reference, "via switch argument extractor");
                      return List.of(PossibleCapture);
                    }
                  }
                }
              } else if (haxeSwitchCaseExpr.getParent() instanceof HaxeExtractorMatchExpression matchExpression) {
                HaxeReferenceExpression referenceFromExtractor = getReferenceFromExtractorMatchExpression(matchExpression);
                if (referenceFromExtractor != null && reference.textMatches(referenceFromExtractor)) {
                  LogResolution(reference, "via witch extractor");
                  return List.of(referenceFromExtractor);
                }
              }
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
    HaxeSwitchCaseExpr caseExpr = expression.getMatch();
    while (caseExpr != null) {
      if (caseExpr.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
        return referenceExpression;
      }
      else if (caseExpr.getExpression() instanceof HaxeExtractorMatchExpression matchExpression) {
        caseExpr = matchExpression.getMatch();
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
      if (matchExpression.getMatch().textMatches(reference.getText())) {
        LogResolution(reference, "via Capture Var");
        return List.of(matchExpression.getMatch());
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
    List<? extends PsiElement> result = null ;
    if(switchCaseExpr == null) {
      HaxeSwitchCase switchCase = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCase.class);
        result = searchCaseBlockForReference(reference, switchCase);
    }

    // NOTE: this one has to come before  `checkIfSwitchCaseDefaultValue`
    // check if default name in match expression (ex  `case TString(_ => captureVar)`)
    if (result == null) result = checkIfDefaultValueInMatchExpression(reference, switchCaseExpr);

    // check if enum extracted value (ex `case MyEnumVal(reference)`)
    if (result == null) result = checkIsSwitchExtractedValue(reference);

    if (result == null) result =  tryResolveExtractedValue(reference);

    // check if matches default name ( ex. `case _:`)
    if (result == null) result = checkIfSwitchCaseDefaultValue(reference, false);


    // try to resolve reference for guard and block (ex. `case [a, b] if ( b > a): b + ">" + a;`)
    if (result == null) result = tryResolveVariableForGuardsAndBlock(reference);
    if (result != null) {
      LogResolution(reference, "via switch var");
    }
    return result;
  }

  private @Nullable List<? extends PsiElement> searchCaseBlockForReference(HaxeReference reference, PsiElement switchCase) {
    if (switchCase == null) return null;

    List<? extends PsiElement> result = checkByTreeWalk(reference, switchCase);
    if(result != null && !result.isEmpty()){
      if (PsiTreeUtil.isAncestor(reference, result.getFirst(), true)) {
        // enumExtractor references can be either a reference to a field outside the switch scope
        // or a variable itself depending on the precent of a parent variable with the same name
        // since this reference can also be a variable we make sure we dont resolve it to itself.
        return null;

      }
    }
    return result;
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
      if (switchCase != null) {
        HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(switchCase, HaxeSwitchStatement.class);
        if (switchStatement != null) {
          HaxeExpression switchStatementExpression = switchStatement.getExpression();
          List<HaxeSwitchCaseExpr> list = switchCase.getSwitchCaseExprList();
          for (HaxeSwitchCaseExpr caseExpr : list) {
            HaxeSwitchCaseExprArray caseExprArray = caseExpr.getSwitchCaseExprArray();
            if (caseExprArray != null) {
              List<HaxeExpression> expressionList = caseExprArray.getExpressionList();
              for (HaxeExpression haxeExpression : expressionList) {
                if (haxeExpression instanceof HaxeEnumArgumentExtractor extractor) {
                  List<HaxeEnumExtractedValue> value = searchEnumArgumentExtractorForReference(reference, extractor);
                  if (value != null) return value;
                }
                if (haxeExpression instanceof HaxeReferenceExpression referenceExpression) {
                  List<? extends PsiElement> psiElements = checkEnumExtractor(referenceExpression);
                  if(psiElements != null && !psiElements.isEmpty()) {
                    // if resolve of referenceExpression equals the switch statement expression or is a part of it
                    //  (ex. objectLiterals/ ArrayLiterals  switch([x,y]){..})
                    // then we know it should be a switch variable and not something else like a type or member
                      PsiElement match = psiElements.getFirst();
                      if (match == switchStatementExpression || PsiTreeUtil.isAncestor(switchStatementExpression, match, true)) {
                        if (haxeExpression.textMatches(reference)) {
                          return List.of(haxeExpression);
                        }
                      }
                    }
                }
              }
            }
            HaxeExpression expression = caseExpr.getExpression();
            if (expression instanceof HaxeArrayLiteral arrayLiteral) {
              HaxeExpressionList expressionList = arrayLiteral.getExpressionList();
              if (expressionList != null) {
                for (HaxeExpression haxeExpression : expressionList.getExpressionList()) {
                  if (haxeExpression.textMatches(reference)) {
                    return List.of(haxeExpression);
                  }
                }
              }
            } else if (expression instanceof HaxeReferenceExpression referenceExpression) {
              List<? extends PsiElement> psiElements = checkEnumExtractor(referenceExpression);
              if(psiElements != null && !psiElements.isEmpty()) {
                // if resolved value is the switch statement expression or a part of it (ex.  array literals  switch([x,y]){..})
                // then we know it should be a switch variable and not something else like a type or member
                PsiElement match = psiElements.getFirst();
                if (match == switchStatementExpression || PsiTreeUtil.isAncestor(switchStatementExpression, match, true)) {
                  if (reference.textMatches(referenceExpression)) {
                    return List.of(referenceExpression);
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  public static boolean isCaptureVariable(HaxeSwitchCaseExpr switchCaseExpr) {
    PsiElement child = switchCaseExpr.getFirstChild();
    if(child instanceof  HaxeReference reference) {

      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (switchStatement != null) {
        HaxeExpression switchStatementExpression = switchStatement.getExpression();
        List<? extends PsiElement> psiElements = checkEnumExtractor(reference);
        if (psiElements != null && !psiElements.isEmpty()) {
          // if resolved value is the switch statement expression or a part of it (ex.  array literals  switch([x,y]){..})
          // then we know it should be a switch variable and not something else like a type or member
          PsiElement match = psiElements.getFirst();
          if (match == switchStatementExpression || PsiTreeUtil.isAncestor(switchStatementExpression, match, true)) {
            if (reference.textMatches(reference)) {
              return true;
            }
          }
        }
      }
    }
    return false;
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

  @Nullable
  private static List<PsiElement> tryResolveExtractedValue(HaxeReference reference) {
    PsiElement parent = reference.getParent();
    if (parent instanceof HaxeEnumExtractedValue ||  reference instanceof HaxeUnnamedExtractedValueReference) {
      Stack<Object> objectPath = new Stack<>();
      boolean checkParent = true;

      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      PsiElement pathElement = buildExtractVarPath(parent, checkParent, objectPath, switchStatement);

      HaxeNamedComponent lastElement = null;
      Collections.reverse(objectPath);
      
      for (int i = 0; i < objectPath.size(); i++) {
        Object path = objectPath.get(i);
        if (pathElement instanceof HaxeReferenceExpression referenceExpression) {
          pathElement = referenceExpression.resolve();
          if (pathElement instanceof HaxePsiField psiField) {
            ResultHolder result = HaxeExpressionEvaluator.evaluate(psiField).result;
            if (result != null && result.getClassType() != null && path instanceof String memberName) {
              HaxeClassModel haxeClassModel = result.getClassType().getHaxeClassModel();
              if (haxeClassModel != null) {
                HaxeBaseMemberModel member = haxeClassModel.getMember(memberName, null);
                if(member != null) {
                  lastElement = member.getNamedComponentPsi();
                  continue;
                }
              }
            }
          }
        }
        if (pathElement instanceof HaxeArrayLiteral arrayLiteral && path instanceof Integer index) {
          HaxeExpressionList expressionList = arrayLiteral.getExpressionList();
          if (expressionList != null) {
            pathElement = expressionList.getExpressionList().get(index);
          }

        } else if (pathElement instanceof HaxeObjectLiteral objectLiteral && path instanceof String member) {
          List<HaxeNamedComponent> members = objectLiteral.findHaxeMemberByName(member, null);
          lastElement = members.isEmpty() ? null : members.getFirst();
          
        } else if(switchStatement != null){
          ResultHolder resultHolder = HaxeExpressionEvaluator.evaluate(switchStatement.getExpression()).result;
          if (resultHolder != null && resultHolder.getClassType() != null) {
            HaxeClass haxeClass = resultHolder.getClassType().getHaxeClass();
            if (haxeClass != null) {
              if( path instanceof String memberName) {
                HaxeBaseMemberModel member1 = haxeClass.getModel().getMember(memberName, null);
                if(member1!= null) {
                  lastElement = member1.getNamedComponentPsi();
                } else {
                  lastElement = haxeClass.getTypeComponent();
                }
              }
            }
          }
        }
      }
      if(lastElement != null) return List.of(lastElement);
      if(pathElement != null) return List.of(pathElement);
    }
    return null;
  }
  @Nullable
  private PsiElement enumMemberTraverseUsagePath(HaxeReference reference) {
    PsiElement parent = reference.getParent();
    if (parent instanceof HaxeEnumValueReference) {
      Stack<Object> objectPath = new Stack<>();

      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      PsiElement pathElement = buildExtractVarPath(parent, true, objectPath, switchStatement);

      HaxeNamedComponent lastElement = null;
      Collections.reverse(objectPath);

      for (int i = 0; i < objectPath.size(); i++) {
        Object path = objectPath.get(i);
        if (pathElement instanceof HaxeReferenceExpression referenceExpression) {
          pathElement = referenceExpression.resolve();

        }
        if (pathElement instanceof HaxePsiField psiField) {
          ResultHolder result = HaxeExpressionEvaluator.evaluate(psiField).result;
          if (result != null && result.getClassType() != null) {
            HaxeClassModel haxeClassModel = result.getClassType().getHaxeClassModel();
            if (path instanceof String memberName) {
              if (haxeClassModel != null) {
                HaxeBaseMemberModel member = haxeClassModel.getMember(memberName, null);
                if (member != null) {
                  lastElement = member.getNamedComponentPsi();
                  continue;
                }
              }
            } else if (path instanceof Integer) { // Integer means array access
              SpecificTypeReference fullyResolved = result.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
              if (fullyResolved instanceof SpecificHaxeClassReference classReference) {
                ResultHolder iterableType = classReference.getIterableElementType(null);
                if (iterableType != null && iterableType.getType() instanceof SpecificHaxeClassReference classType)
                  lastElement = classType.getHaxeClass();
                continue;
              }
            }
          }
        }

        if (pathElement instanceof HaxeArrayLiteral arrayLiteral && path instanceof Integer index) {
          HaxeExpressionList expressionList = arrayLiteral.getExpressionList();
          if (expressionList != null) {
            pathElement = expressionList.getExpressionList().get(index);
          }

        } else if (pathElement instanceof HaxeObjectLiteral objectLiteral && path instanceof String member) {
          List<HaxeNamedComponent> members = objectLiteral.findHaxeMemberByName(member, null);
          lastElement = members.isEmpty() ? null : members.getFirst();

        }
        else if(pathElement instanceof HaxeEnumExtractObjectLiteral objectLiteral && path instanceof String member) {
          List<HaxeEnumObjectLiteralElement> elementList = objectLiteral.getEnumObjectLiteralElementList();
          for (HaxeEnumObjectLiteralElement element : elementList) {
            if(element.getComponentName().textMatches(member)) {
                HaxeModel model = getModelForElement(element);
                if (model instanceof HaxeBaseMemberModel memberModel) {
                  pathElement = memberModel.getNamedComponentPsi();
                  break;
                }
            }
          }
        }
      }
      if(lastElement != null) return lastElement;
      if(pathElement != null) return pathElement;
    }
    return null;
  }

  public static @Nullable PsiElement buildExtractVarPath(PsiElement extractedValue, boolean checkParent, Stack<Object> objectPath, HaxeSwitchStatement switchStatement) {
    PsiElement pastParent = extractedValue;
    PsiElement valueParent = extractedValue.getParent();
    PsiElement pathElement = null;

    while (valueParent != null && checkParent) {

      if (valueParent instanceof HaxeEnumObjectLiteralElement objectLiteralElement) {
        objectPath.add(extractObjectLiteralName(objectLiteralElement));

      } else if (valueParent instanceof HaxeEnumExtractArrayLiteral arrayLiteral) {
        List<PsiElement> caseExpressionList = Arrays.asList(arrayLiteral.getChildren());
        objectPath.add(caseExpressionList.indexOf(pastParent));

      } else if (valueParent instanceof HaxeSwitchCaseExprArray caseExprArray) {
        List<PsiElement> caseExpressionList = Arrays.asList(caseExprArray.getChildren());
        objectPath.add(caseExpressionList.indexOf(pastParent));

      } else if (valueParent instanceof HaxeSwitchCaseExpr) {
        checkParent = false;
        if (switchStatement != null) {
          HaxeExpression expression = switchStatement.getExpression();
          while (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
            expression = parenthesizedExpression.getExpression();
          }
          if (expression instanceof HaxeArrayLiteral || expression instanceof HaxeObjectLiteral) {
            pathElement = expression;
          }
        }
      } else if (valueParent instanceof HaxeEnumExtractObjectLiteral objectLiteral) {
        checkParent = false;
        pathElement = objectLiteral;
      }
      pastParent = valueParent;
      valueParent = valueParent.getParent();
    }
    return pathElement;
  }


  @Nullable
  private List<PsiElement> checkIfDefaultValueInMatchExpression(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    //TODO/NOTE:
    // There seems to be an issue where method calls in extractor match expressions would be mapped to the parameter type of the same index
    // this is usually wrong  so we avoid this here, there could maybe be cases where the parameter type is a function and you want to call it
    // as part of the  matching (not sure if its allowed, needs testing)
    if(reference.getParent() instanceof  HaxeCallExpression callExpression &&  PsiTreeUtil.getParentOfType(callExpression, HaxeExtractorMatchExpression.class ) != null) return null;

    HaxeEnumArgumentExtractor argumentExtractorBeforeAntSwitchExpr = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class, true, HaxeSwitchCaseExpr.class, HaxeEnumObjectLiteralElement.class, HaxeEnumExtractArrayLiteral.class);
    if (argumentExtractorBeforeAntSwitchExpr != null) {
      int argumentIndex = getExtractorArgumentIndex(reference,  argumentExtractorBeforeAntSwitchExpr);
      if (argumentIndex > -1) {
        HaxeParameter parameter = findExtractedValueEnumParameter(argumentExtractorBeforeAntSwitchExpr, argumentIndex);
        if(parameter != null) return List.of(parameter.getComponentName());
      }
    }

    HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);
    if (matchExpression != null) {
      HaxeSwitchCaseExpr match = matchExpression.getMatch();
      if (PsiTreeUtil.isAncestor(match, reference, false)) {
        return List.of(matchExpression.getExtractorExpression());
      }
    }

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

  private static @Nullable HaxeParameter findExtractedValueEnumParameter(HaxeEnumArgumentExtractor argumentExtractor1, int argumentIndex) {
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
            return list.get(argumentIndex);

          }
        }
      }
    }
    return null;
  }

  private static int getExtractorArgumentIndex(HaxeReference reference, HaxeEnumArgumentExtractor argumentExtractor) {
    HaxeEnumExtractorArgumentList argumentList = argumentExtractor.getEnumExtractorArgumentList();
    // using children  here as we need to get correct index
    @NotNull PsiElement[] children = argumentList.getChildren();

    int argumentIndex = -1;
    for (int i = 0; i < children.length; i++) {
      PsiElement  element = children[i];
      PsiElement context = PsiTreeUtil.findCommonContext(element, reference);
      // pure extractedValue reference
      if(context instanceof  HaxeEnumExtractedValue) return  i;

      context = PsiTreeUtil.findCommonContext(element, reference);
      // reference as part of an expression
      if (context != argumentExtractor && context instanceof  HaxeExtractorMatchExpression) {
        return i;
      }
    }
    return argumentIndex;
  }

  @Nullable
  private static List<@NotNull PsiElement> checkIfSwitchCaseDefaultValue(HaxeReference reference, boolean ignoreName) {
    if (reference.textMatches("_") || ignoreName) {
      // if is part of an expression
      HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class,  true, HaxeEnumArgumentExtractor.class);
      if (switchCaseExpr != null) {
        if (switchCaseExpr.getParent() instanceof HaxeExtractorMatchExpression matchExpression) {
          //  reference should be  previous matchExpression as it's the value/result from that one that is passed as _
          return List.of(matchExpression.getExtractorExpression());
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
          SpecificTypeReference underlyingType = model.getUnderlyingType(resolver);
          List<? extends PsiElement> resolved = resolveByClassAndSymbol(underlyingType, resolver, reference);
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
    boolean shouldCollectAll = reference.getParent() instanceof HaxeCallExpression;

    final List<PsiElement> result = new ArrayList<>();
    String referenceText = reference.getText();
    if(referenceText.contains(".")) return null; // no field or type name can contain "." so we ignore refs with that and avoid walking tree
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, referenceText, reference, shouldCollectAll), reference, maxScope, new ResolveState());
    if (result.isEmpty()) return null;
    if (result.size() > 1) {
      // more than one item matches reference in tree, trying to filter by expected type
      List<@NotNull PsiElement> psiElement = searchForBestMatch(reference, result);
      if (psiElement != null) return psiElement;
    }
    //TODO this is a hackish tmp workaround for overloads
    if(result.getFirst().getParent() instanceof HaxeMethodDeclaration methodDeclaration) {
      if (methodDeclaration.isOverload()) {
        result.clear();
        PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, referenceText, reference, true), reference, maxScope, new ResolveState());
        List<HaxeBaseMemberModel> methodModels = result.stream()
                .map(PsiElement::getParent)
                .filter(Objects::nonNull)
                .map(HaxeMethodDeclaration.class::cast)
                .map(HaxeMethodPsiMixin::getModel)
                .map(HaxeBaseMemberModel.class::cast)
                .toList();
        List<HaxeNamedComponent> components = checkMethodOverloads(reference, methodModels);
        if(components != null) return components;
      }
    }
    LogResolution(reference, "via tree walk.");
    return List.of(result.getFirst());
  }

  private static @Nullable List<PsiElement> searchForBestMatch(HaxeReference reference, List<PsiElement> result) {
    PsiElement bestGuess = result.getFirst();
    if (reference.getParent() instanceof HaxeCallExpression callExpression) {
      for (PsiElement psiElement : result) {
        if (psiElement.getParent() instanceof HaxeMethod method) {
          // while we want a perfect match  where the call expression is valid
          // the code might be incomplete and validation might fail so we keep track of the element
          // as a method  match is more correct than a field match
          bestGuess = psiElement;
          HaxeCallExpressionEvaluation evaluation = cachedHaxeCallExpressionEvaluation(method, callExpression);
          if (evaluation != null && evaluation.isValid()) {
            LogResolution(reference, "via tree walk. (method filtered)");
            return List.of(psiElement);
          }
        } else {
          ResultHolder resultHolder = HaxeExpressionEvaluator.evaluate(psiElement.getParent()).result;
          SpecificTypeReference type = resultHolder.getType();
            if (type instanceof SpecificHaxeClassReference classReference) {
               type = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
            }
          if (type instanceof SpecificFunctionReference functionReference) {
            // todo evaluate args vs params
            return List.of(psiElement);
          }
          if (type instanceof SpecificHaxeClassReference classReference ) {
            HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
            if(haxeClassModel != null && haxeClassModel.isCallable()) {
              return List.of(psiElement);
            }

          }
        }
      }
      if(bestGuess != null) {
        return List.of(bestGuess);
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkByTreeWalk(HaxeReference reference) {
    return checkByTreeWalk(reference, (PsiElement)null);
  }


  private List<? extends PsiElement> checkByTreeWalk(HaxeReference scope, String name) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, name, null, false), scope, null, new ResolveState());
    if (result.isEmpty()) return null;
    LogResolution(scope, "via tree walk.");
    return result;
  }

  private List<? extends PsiElement> checkIsAccessor(HaxeReference reference) {
    if (reference instanceof HaxePropertyAccessor) {
      final HaxeAccessorType accessorType = HaxeAccessorType.fromPsi(reference);
      if (!accessorType.isGetter()  && !accessorType.isSetter())return null;

      final HaxeFieldDeclaration varDeclaration = PsiTreeUtil.getParentOfType(reference, HaxeFieldDeclaration.class);
      if (varDeclaration == null) return null;

      final HaxeFieldModel fieldModel = (HaxeFieldModel)varDeclaration.getModel();
      final HaxeMethodModel method = accessorType.isGetter() ? fieldModel.getGetterMethod() : fieldModel.getSetterMethod();

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
    if (reference instanceof HaxeReferenceExpression referenceExpression) {
      final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(referenceExpression);
      if (leftReference != null) {
          HaxePsiCompositeElement parentOfType = PsiTreeUtil.getParentOfType(reference, HaxeType.class);
          // chain resolve is intended to find members, skipping resolveChain if reference is part of a Type / Qname
          if(parentOfType == null) {
            List<? extends PsiElement> result = resolveChain(leftReference, reference);
            if (result != null && !result.isEmpty()) {
              LogResolution(reference, "via simple chain using leftReference.");
              return result;
            }
          }
        if (canBeQname(reference)) {
          PsiElement item = resolveQualifiedReference(reference);
          if (item != null) {
            LogResolution(reference, "via simple chain against package or module.");
            return asList(item);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsModuleName(@NotNull HaxeReference reference) {
    final PsiElement element = HaxeResolveUtil.tryResolveModuleReference(reference);
    if (element != null) {
      LogResolution(reference, "via module qualified name.");
      return asList(element);
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

  private static boolean isQualifiedNameReferenceStructure(HaxeReference reference) {
    int refCount = 0;
    while (reference.getParent() instanceof HaxeReference parent) {
      reference = parent;
      refCount++;
    }
    if(refCount == 0) return false;
    if(reference.getParent() instanceof HaxeType) return true;
    if(reference.getParent() instanceof HaxeImportStatement) return true;
    return false;
  }



  @Nullable
  private List<? extends PsiElement> checkIsSuperExpression(HaxeReference reference) {
    if (reference instanceof HaxeSuperExpression && reference.getParent() instanceof HaxeCallExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeClass.class);
      if(haxeClass != null) {
        if (!haxeClass.getHaxeExtendsList().isEmpty()) {
          final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
          final HaxeClass superClass = ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass();
          if (superClass != null) {
            List<HaxeNamedComponent> haxeMethodByName = superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString(), null);
            if(!haxeMethodByName.isEmpty() && haxeMethodByName.getFirst() instanceof HaxeNamedComponent constructor) {
              LogResolution(reference, "because it's a super expression.");
              return asList(constructor.getComponentName());
            } else {
              return asList(superClass.getComponentName());
            }
          }
        }
      }
    }

    return null;
  }

  private List<? extends PsiElement> checkIsNewExpression(@NotNull HaxeReference reference) {
    if (reference instanceof HaxeNewExpression  newExpression) {
      ResultHolder type = HaxeTypeResolver.getTypeFromType(newExpression.getType());
      SpecificHaxeClassReference classType = type.getClassType();
      if(classType == null) return null;

      HaxeClassModel haxeClassModel = classType.getHaxeClassModel();
      if(haxeClassModel == null) return null;
      List<HaxeMethodModel> constructors = haxeClassModel.getConstructors(null);
      return checkConstructorOverloads(newExpression, constructors);
    }
    return null;
  }


  @Nullable
  private List<? extends PsiElement> checkIsType(HaxeReference reference) {
    if (reference instanceof HaxeReferenceExpression referenceExpression) {
      // only check one "level up", if we go multiple parents up we might get a different reference's value
      // if we are resolving  a.b  in  a.b.c.Type we want to resolve the package "b" and not Type in package "c".
      if (referenceExpression.getParent() instanceof HaxeType type) {
          final HaxeClass haxeClassInType = HaxeResolveUtil.tryResolveClassByQName(type);
          if (haxeClassInType != null) {
            LogResolution(reference, "via parent type name.");
            return asList(haxeClassInType.getComponentName());
          }
          //  check if module member
          //  we might get a match on class with module name (default class), but the type we are looking for is not a child of default class
          //  so we search the parent file for other classes
          @NotNull PsiElement[] children = reference.getChildren();
          if (children.length > 1) {
            if (children[0] instanceof HaxeReference child) {
              List<? extends PsiElement> resolve = resolve(child, false);
              if (!resolve.isEmpty()) {
                PsiFile containingFile = resolve.getFirst().getContainingFile();
                if (containingFile instanceof HaxeFile haxeFile) {
                  HaxeClassModel model = haxeFile.getModel().getClassModel(children[1].getText());
                  if (model != null) {
                    HaxeComponentName componentName = model.haxeClass.getComponentName();
                    if (componentName != null) {
                      LogResolution(reference, "via module scan.");
                      return List.of(componentName);
                    }
                  }
                }
              }
            }
          }
      }
      else {
        String maybeQname = reference.getText();
        // a sanity check before we try Qname resolve
        // should be a chain and not contain any method call, array access or typeParameters
        if(textCanBeQname(maybeQname)) {
          HaxeClass classByQName = HaxeResolveUtil.findClassByQName(maybeQname, reference);
          if(classByQName != null) {
            // if part of a longer chain, need to check if it is a module or mainclass reference
            if(referenceExpression.getParent() instanceof HaxeReferenceExpression expression) {
              HaxeModuleModel module = classByQName.getModel().getModule();
              HaxeClassModel classModel = module.getMainClass();
              if(classModel != null && classModel.haxeClass == classByQName) {
                HaxeBaseMemberModel member = module.getMember(expression.getIdentifier().getText(), null);
                if (member != null) {
                  // if the parent reference is a member of the module, we return the module instead of the class
                  LogResolution(reference, "via fully qualified module name.");
                  return List.of(module.module);
                }
              }
            }
            HaxeComponentName componentName = classByQName.getComponentName();
            if(componentName != null) {
              LogResolution(reference, "via fully qualified class name.");
              return List.of(componentName);
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
    ProgressIndicatorProvider.checkCanceled();

    // TODO: Merge with resolveByClassAndSymbol()??  It is very similar to this method.
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    List<PsiElement> parentResolve = new ArrayList<>();
    PsiElement resolve = null;
    if (leftReference != null) {
      // recursive so we try to  resolve first element in the chain first and go up the chain
      // using normal resolve so result is cached (resolveChain is early in the resolve logic so should not cause much overhead)
      resolve = leftReference.resolve();
      if(resolve != null) parentResolve.add(resolve);
    }

    if (canBeQname(reference)) {
        PsiElement item = resolveQualifiedReference(reference);
        if (item != null) {
          LogResolution(reference, "via simple chain against package or module.");
          return List.of(item);
        }
    }

    if(!parentResolve.isEmpty()) {
      PsiElement first = parentResolve.getFirst();
      String rightHandText = reference.getLastChild().getText();
      if(first instanceof HaxeModule module && module.getModel() instanceof HaxeModuleModel model) {
        List<PsiElement> member = resolveModuleMemberOrClass(reference, model, rightHandText);
        if (member != null) return member;
      }
      // check if resolved class is main class in module, and if so, check if our references could be a module member and not a class member
      if(first instanceof HaxeClass haxeClass){
        HaxeModule module = haxeClass.getModule();
        if(module.getModel() instanceof HaxeModuleModel model) {
          HaxeClassModel aClass = model.getMainClass();
          if(aClass != null && aClass.haxeClass == haxeClass) {
            List<PsiElement> member = resolveModuleMemberOrClass(reference, model, rightHandText);
            if (member != null) return member;
          }
        }
      }
    }

    //List<List<? extends PsiElement>> debugList = new ArrayList<>();

    PsiElement identifier = reference instanceof HaxeReferenceExpression referenceExpression ? referenceExpression.getIdentifier() : reference;
    String identifierText = identifier.getText();

    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(lefthandExpression);
    ResultHolder result = extensionsMethodGuard.doPreventingRecursion(lefthandExpression, true, () -> {
      return HaxeExpressionEvaluator.evaluate(lefthandExpression, context, null).result;
    });
    if(result== null || result.isUnknown()) {
      extensionsMethodGuard.prohibitResultCaching(lefthandExpression);
    }
    // TODO mlo: clean up (separate members and extension methods)
    SpecificTypeReference type = result != null && !result.isUnknown() ? result.getType()  : null;
    //enum values does not have a HaxeClass but we need a class for a lot of the checks below (extension methods etc),
    // so we use the EnumValue as class as a replacement
    boolean fromEnumValue = false;
    if (type instanceof SpecificEnumValueReference valueReference) {
      type = getEnumValue(valueReference.context);
      fromEnumValue = true;
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
      if (haxeClass != null) {
        HaxeClassModel classModel = haxeClass.getModel();
        List<HaxeBaseMemberModel> members = classModel.getMembers(identifierText, classType.getGenericResolver());
        if (!members.isEmpty()) {
          if (members.size() == 1) {
            HaxeNamedComponent psi = members.getFirst().getNamedComponentPsi();
            if (psi != null) {
              HaxeComponentName name = psi.getComponentName();
              if (name != null) {
                return Collections.singletonList(name);
              }
            }
          } else {
            List<HaxeNamedComponent> member = checkMethodOverloads(reference, members);
            if (member != null) return member;
          }
        }
        //
        // mapping the string-literal "member" .code to a fake Psi with docs
        // making sure it's a string literal and only 1 char long
        if (identifierText.equals("code") && lefthandExpression instanceof HaxeStringLiteralExpression literalExpression) {
          if (identifier instanceof HaxeIdentifier haxeIdentifier) {
            if (literalExpression.getTextLength() == 3) { // quotes + char = 3
              synchronized (reference) {
                HaxeFakePsiElement fakePsi = reference.getUserData(FAKE_PSI_KEY);
                if (fakePsi != null) {
                  return Collections.singletonList(fakePsi);
                } else {
                  HaxeFakeComponentStringCode fakeElement = new HaxeFakeComponentStringCode(haxeIdentifier);
                  reference.putUserData(FAKE_PSI_KEY, fakeElement);
                  return Collections.singletonList(fakeElement);
                }
              }
            }
          }
        }
        // TODO should probably handle this in a different place and in a better way
        // clear if no longer resolvable
        clearFakePsi(reference);

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
              if (name != null && name.getIdentifier().textMatches(identifierText)) {
                if (log.isTraceEnabled()) log.trace(traceMsg("Found component name in extension methods"));
                return name;
              }
            }
          }
          return null;
        });
        if (match != null) return Collections.singletonList(match);
      }

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
      SpecificHaxeClassReference extensionType = classType;
      // check if reference is to a Class or Enum and if so wrap in Class<> or Enum<> so we
      // can match stuff like methods in EnumTools and/or other extensions for Enum/Class types.
      if (leftReference != null && haxeClass != null) {
        if (leftReference.getParent() instanceof HaxeReferenceExpression parent) {
          // make sure our HaxeReferenceExpression is the fist element  in parent (ex. MyClass.someMember)
          if (parent.getFirstChild() == leftReference && !(parent.getParent() instanceof HaxeReferenceExpression)) {
            HaxeClassModel model = haxeClass.getModel();
            String name = model.getName();
            if (name != null && leftReference.textMatches(name)) {
              extensionType = getStdClass(haxeClass.isEnum() ? ENUM : CLASS, leftReference, new ResultHolder[]{new ResultHolder(classType)});
            }
          }
        }
      }

      HaxeMethodModel foundMethod = null;
        for (int i = usingModels.size() - 1; i >= 0; --i) {
          foundMethod = usingModels.get(i)
            .findExtensionMethod(identifierText, extensionType);
          if (null != foundMethod && !foundMethod.HasNoUsingMeta()) {

            if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + foundMethod.getName());
            //return asList(foundMethod.getBasePsi());
            //debugList.add(asList(foundMethod.getBasePsi()));
            return List.of(foundMethod.getNamePsi());
          }
          // check other types ("using" can be used to find typedefsetc)

          //TODO mlo:  try to get namedComponent from element
          PsiElement element = usingModels.get(i).exposeByName(identifierText);
          if (element != null) {
            if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + identifierText);
            //return List.of(element);
            //debugList.add(List.of(element));
            return List.of(element);
          }
        }
      }

    if (log.isTraceEnabled()) log.trace(traceMsg(null));

    final HaxeComponentName componentName = tryResolveHelperClass(lefthandExpression, identifierText);
    if (componentName != null) {
      if (log.isTraceEnabled()) log.trace("Found component " + componentName.getText());
      return Collections.singletonList(componentName);
    }
    if (log.isTraceEnabled()) log.trace(traceMsg("trying keywords (super, new) arrays, literals, etc."));
    // Try resolving keywords (super, new), arrays, literals, etc.
    if(type instanceof SpecificFunctionReference) {
      //check if reference is bind
      // Note: there is no code to resolve  to so we use a fakePsi with a reference to the method being bound.
      if ("bind".equals(identifierText)) {
        if (identifier instanceof HaxeIdentifier haxeIdentifier) {
          if (resolve != null) {
            synchronized (FAKE_PSI_KEY) {
              if (resolve instanceof HaxeNamedComponent namedComponent) {
                HaxeFakePsiElement fakePsi = reference.getUserData(FAKE_PSI_KEY);
                if (fakePsi != null) {
                  return Collections.singletonList(fakePsi);
                } else {
                  HaxeFakePsiElement fakeElement = new HaxeFakeComponentBindMethod(haxeIdentifier, namedComponent);
                  reference.putUserData(FAKE_PSI_KEY, fakeElement);
                  return Collections.singletonList(fakeElement);
                }
              }
            }
          }
        }
      }
      clearFakePsi(reference);
    }

    if(fromEnumValue) {
      //  type was replaced with EnumValue, check original class
      List<? extends PsiElement> psiElements = resolveByClassAndSymbol(result.getType(), null, reference);
      if(psiElements!= null && !psiElements.isEmpty())return psiElements;
    }

    if(type != null) return resolveByClassAndSymbol(type, null, reference);
    return  List.of();
  }

  private static @Nullable List<PsiElement> resolveModuleMemberOrClass(HaxeReference reference,
                                                                       HaxeModuleModel model,
                                                                       String name) {
    HaxeBaseMemberModel member = model.getMember(name, null);
    if (member != null) {
      LogResolution(reference, "via simple chain against module members.");
      return List.of(member.getNamedComponentPsi());
    }
    HaxeClassModel aClass = model.getClass(name);
    if (aClass != null) {
      HaxeComponentName componentName = aClass.haxeClass.getComponentName();
      if (componentName != null) return List.of(componentName);
    }
    return null;
  }

  private static void clearFakePsi(HaxeReference reference) {
    if(reference.getUserData(FAKE_PSI_KEY) != null) {
      reference.putUserData(FAKE_PSI_KEY, null);
    }
  }

  private static @Nullable List<HaxeNamedComponent> checkMethodOverloads(HaxeReference reference, List<HaxeBaseMemberModel> members) {
    // this is probably far from the best solution for method overloads but it seems to work for method calls
    // it wont work for function type assign, but might attempt to add that later if its necessary (mlo).
    for (HaxeBaseMemberModel member : members) {
      if (member instanceof HaxeMethodModel methodModel) {
        if (reference.getParent() instanceof HaxeCallExpression callExpression) {
          HaxeCallExpressionEvaluation evaluate = cachedHaxeCallExpressionEvaluation(methodModel.getMethod(), callExpression);
          if (evaluate != null && evaluate.isValid()) {
            return Collections.singletonList(member.getNamedComponentPsi());
          }
        } else if (reference.getParent() instanceof HaxeCallExpressionList argumentList) {
          int argIndex = argumentList.getExpressionList().indexOf(reference);
          if (argumentList.getParent() instanceof HaxeCallExpression callExpression) {
            if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
              if (referenceExpression.resolve() instanceof HaxeMethod haxeMethod) {
                HaxeCallExpressionEvaluation evaluate = cachedHaxeCallExpressionEvaluation(haxeMethod, callExpression);
                if(evaluate != null) {
                  int paramIndex = evaluate.getParameterForArgument(argIndex);
                  if (paramIndex != -1) {
                    ResultHolder parameterType = evaluate.getParameterType(paramIndex);
                    if (parameterType != null) {
                      SpecificFunctionReference functionType = methodModel.getFunctionType(null);
                      if (functionType.canAssign(parameterType)) {
                        return Collections.singletonList(member.getNamedComponentPsi());
                      }
                    }
                  }
                }
              }
            }
          }
        }else if(reference.getParent() instanceof HaxeVarInit varInit){
          ResultHolder expected = null;
          if(varInit.getParent() instanceof HaxePsiField field){
            if(field.getModel() instanceof HaxeFieldModel fieldModel) {
              expected = fieldModel.getResultType(null);
            }
            if(field.getModel() instanceof HaxeLocalVarModel localVarModel) {
              expected = localVarModel.getResultType(null);
            }
            if(expected  != null) {
              ResultHolder functionType = methodModel.getFunctionType(null).createHolder();
              if(functionType.canAssign(expected)){
                return Collections.singletonList(member.getNamedComponentPsi());
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static @Nullable List<HaxeNamedComponent> checkConstructorOverloads(HaxeNewExpression newExpression, List<HaxeMethodModel> constructors) {
    for (HaxeMethodModel constructor : constructors) {
      HaxeCallExpressionContextContainer contextContainer = createContextForConstructorCall(newExpression, constructor.getMethod().getModel());
      HaxeCallExpressionEvaluation evaluation = contextContainer.evaluateContexts();
      if (evaluation != null && evaluation.isValid()) {
        return Collections.singletonList(constructor.getNamedComponentPsi());
      }
    }
    return null;
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
    // if chain is longer than just to a member  we ignore it
    // ex. Package.MyClass.someVariable.SomeValueInVariableType
    if(!qualifiedName.equals(qualifiedInfo.getPresentableText())) return null;

    List<HaxeModel> result = HaxeProjectModel.fromElement(reference).resolve(qualifiedInfo, reference.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.getFirst();
      if (item instanceof HaxeFileModel fileModel) {
        HaxeClassModel mainClass = fileModel.getMainClassModel();
        if (mainClass != null) {
          return mainClass.haxeClass.getComponentName();
        }
      }
      PsiElement psi = item.getBasePsi();
      if (psi instanceof  PsiPackage) return psi;
      if(psi instanceof HaxeFile haxeFile) {
        HaxeModule module = haxeFile.getModule();
        if (module != null) {
          HaxeModuleModel model = (HaxeModuleModel) module.getModel();
          if (model.getQName().equals(qualifiedName)) {
            return module;
          }
        }
      }

      if(psi instanceof HaxePsiField field) {
        if(field.getName().equals(qualifiedInfo.memberName)) {
          return field.getComponentName();
        }
      }
      if(psi instanceof HaxeMethod method) {
        if(method.getName().equals(qualifiedInfo.memberName)) {
          return method.getComponentName();
        }
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
                                                                         @NotNull HaxeReference reference,
                                                                         boolean useUnderlyingForAbstract
  ) {
    HaxeGenericResolver baseResolver = getGenericResolver(leftClass, reference);
    return resolveBySuperClassAndSymbol(leftClass, baseResolver, reference, useUnderlyingForAbstract);
  }


  private static List<? extends PsiElement> resolveBySuperClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                         @Nullable HaxeGenericResolver resolver,
                                                                         @NotNull HaxeReference reference,
                                                                         boolean useUnderlyingForAbstract) {
    if (null == leftClass) {
      return EMPTY_LIST;
    }

    if (leftClass instanceof HaxeAbstractTypeDeclaration) {
      if (useUnderlyingForAbstract) {
        HaxeClassModel classModel = leftClass.getModel();
        HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel) classModel;
        return resolveByClassAndSymbol(abstractClassModel.getUnderlyingType(resolver), resolver, reference);
      }else {
        return resolveByClassAndSymbol(leftClass.getModel().getInstanceType().getType(), resolver, reference);
      }
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
        result = resolveByClassAndSymbol(superClass,null, reference);
        if (null != result && !result.isEmpty()) {
          break;
        }
      }
      return result;
    }
  }




  private static List<? extends PsiElement> resolveByClassAndSymbol(@NotNull SpecificTypeReference typeReference,
                                                                    @Nullable HaxeGenericResolver resolver,
                                                                    @NotNull HaxeReference reference) {
    // TODO: This method is very similar to resolveChain, and they should probably be combined.

    if (typeReference  instanceof   SpecificHaxeClassReference classReference) {
      if(classReference.isNullType() || classReference.isTypeDefOfClass()) {
        SpecificTypeReference specificTypeReference = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
        if(specificTypeReference instanceof SpecificHaxeClassReference haxeClassReference) {
          classReference = haxeClassReference;
        }
      }
      HaxeClass leftClass = classReference.getHaxeClass();
      if(leftClass!= null ) {
        final HaxeClassModel leftClassModel = leftClass.getModel();
        HaxeBaseMemberModel member = leftClassModel.getMember(reference.getReferenceName(), resolver);
        if (member != null) return asList(member.getNamePsi());

        // if class is abstract try find in forwards
        if (leftClass.isAbstractType()) {
          HaxeAbstractClassModel model = (HaxeAbstractClassModel) leftClass.getModel();
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
      }
    }
    // try find using
    return tryResolveExtensionMethod(typeReference, reference);
  }
  private  static  List<? extends PsiElement> tryResolveExtensionMethod(@NotNull SpecificTypeReference typeReference, @NotNull HaxeReference reference) {
    PsiElement elementContext = typeReference.getElementContext();
    HaxeFileModel fileModel = HaxeFileModel.fromElement(elementContext);
    if (fileModel != null) {
      HaxeStdPackageModel stdPackageModel = (HaxeStdPackageModel)HaxeProjectModel.fromElement(elementContext).getStdPackage();
      final List<HaxeUsingModel> usingModels = new ArrayList<>(stdPackageModel.getGlobalUsings());
      usingModels.addAll(fileModel.getUsingModels());

      if(reference.getContainingFile() instanceof  HaxeFile haxeFile) {
        HaxeFileModel model = haxeFile.getModel();
        if(model != null) {
          usingModels.addAll(model.getUsingModels());
        }
      }

      for (int i = usingModels.size() - 1; i >= 0; --i) {
        HaxeUsingModel model = usingModels.get(i);
        HaxeMethodModel method = model.findExtensionMethod(reference.getReferenceName(), typeReference);
        if (method != null) {
          return asList(method.getNamePsi());
        }
      }
    }
    return Collections.emptyList();
  }

  private String traceMsg(String msg) {
    return HaxeDebugUtil.traceThreadMessage(msg, 120);
  }

  private static class ResolveScopeProcessor implements PsiScopeProcessor {
    private final boolean collectAll;
    private final List<PsiElement> result;
    private final PsiElement target;
    final String name;

      private ResolveScopeProcessor(List<PsiElement> result, String name, PsiElement target, boolean collectAll) {
      this.target = target;
      this.result = result;
      this.name = name;
      this.collectAll = collectAll;
      }

    @Override
    public boolean execute(@NotNull PsiElement element, ResolveState state) {
      //TODO: should probably make a better solution for this using a HaxeComponentName
      if (element.getParent() instanceof HaxeEnumObjectLiteralElement || element.getParent() instanceof HaxeEnumExtractArrayLiteral) {
        // avoids adding target to list
        if (element == target) return true;
        // do not resolve a reference to elements later in the code
        if(element.getTextOffset() > target.getTextOffset())  return true;
        if (element.textMatches(name)) {
          result.add(element);
          return collectAll;
        }
      }

      // hackish workaround for captureVariables in array (HaxeSwitchCaseExprArray)
      if(element.textMatches(name) && !PsiTreeUtil.isAncestor(target, element, false)) {
        if (element instanceof HaxeReferenceExpression referenceExpression) {
          if (element.getParent() instanceof HaxeSwitchCaseExprArray || element.getParent() instanceof HaxeSwitchCaseExpr) {
            if (HaxeReferenceUtil.isCaptureVar(referenceExpression)) {
              result.add(element);
              return collectAll;
            }else if(HaxeReferenceUtil.isExtractorMatchReference(element)) {
              result.add(element);
              return collectAll;
            }
          }
        }
      }



      HaxeComponentName componentName = null;
      if (element instanceof HaxeComponentName) {
        componentName = (HaxeComponentName)element;
      }
      else if (element instanceof HaxeNamedComponent) {
        componentName = ((HaxeNamedComponent)element).getComponentName();
      }
      else if (element instanceof HaxeEnumExtractedValueReference reference) {
        componentName = reference.getComponentName();
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
          return collectAll;
        }
      }

      if (componentName != null
          &&  componentName.textMatches(name)
          && !PsiTreeUtil.isAncestor(target, element, false))
      {
        result.add(componentName);
        return collectAll;
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