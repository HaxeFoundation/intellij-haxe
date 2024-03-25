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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
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

import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;
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

  public static ThreadLocal<Stack<PsiElement>> referencesProcessing = ThreadLocal.withInitial(() -> new Stack<PsiElement>());

  private static boolean reportCacheMetrics = false;   // Should always be false when checked in.
  private static AtomicInteger dumbRequests = new AtomicInteger(0);
  private static AtomicInteger requests = new AtomicInteger(0);
  private static AtomicInteger resolves = new AtomicInteger(0);
  private final static int REPORT_FREQUENCY = 100;

  public static final List<? extends PsiElement> EMPTY_LIST = Collections.emptyList();

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
       /** See docs on {@link HaxeDebugUtil#isCachingDisabled} for how to set this flag. */
       boolean skipCachingForDebug = HaxeDebugUtil.isCachingDisabled();

       // Kill circular resolutions -- before checking the cache.
       if (isResolving(reference)) {
         reference.putUserData(skipCacheKey, Boolean.TRUE);
         reportSkip(reference);
         return null;
       }

       // If we are in dumb mode (e.g. we are still indexing files and resolving may
       // fail until the indices are complete), we don't want to cache the (likely incorrect)
       // results.
       boolean isDumb = DumbService.isDumb(reference.getProject());
       boolean hasTypeHint = checkForTypeHint(reference);
       boolean skipCaching = skipCachingForDebug || isDumb || hasTypeHint;
       List<? extends PsiElement> elements
         = skipCaching ? doResolve(reference, incompleteCode)
                       : ResolveCache.getInstance(reference.getProject()).resolveWithCaching(
                         reference, this::doResolve, true, incompleteCode);

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

  private void reportSkip(HaxeReference reference) {
    if (log.isTraceEnabled()) {
      log.trace(traceMsg("-----------------------------------------"));
      log.trace(traceMsg("Skipping circular resolve for reference: " + reference.getText()));
      log.trace(traceMsg("-----------------------------------------"));
    }
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

       List<? extends PsiElement> foundElements = doResolveInner(reference, incompleteCode, referenceText);

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

    if (result == null) result = checkIsAlias(reference);
    if (result == null) result = checkEnumMemberHints(reference);
    if (result == null) result = checkIsType(reference);
    if (result == null) result = checkIsFullyQualifiedStatement(reference);
    if (result == null) result = checkIsSuperExpression(reference);
    if (result == null) result = checkMacroIdentifier(reference);
    if (result == null) result = checkIsChain(reference);
    if (result == null) result = checkIsAccessor(reference);
    if (result == null) result = checkIsSwitchVar(reference);
    if (result == null) result = checkByTreeWalk(reference);  // Beware: This will also locate constraints in scope.

    HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
    // search same file first (avoids incorrect resolve of common named Classes and member with same name in local file)
    if (result == null)result =  searchInSameFile(reference, fileModel, isType);
    if (result == null) result = checkIsClassName(reference);
    if (result == null) result = checkCaptureVar(reference);
    if (result == null) result = checkCaptureVarReference(reference);
    if (result == null) result = checkEnumExtractor(reference);
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
              for (PsiElement element : matchesInImport) {
                if (element instanceof  HaxeEnumValueDeclaration enumValueDeclaration) {
                  PsiElement typeHintPsi = reference;

                  if (reference.getParent() instanceof  HaxeCallExpression expression) {
                    typeHintPsi = expression;
                  }
                  String currentQname = enumValueDeclaration.getContainingClass().getQualifiedName();
                  String data = typeHintPsi.getUserData(typeHintKey);
                  if (currentQname != null && currentQname.equals(data)) {
                    LogResolution(reference, "via import & typeHintKey");
                    return List.of(element);
                  }
                }
              }
              // fallback, heck method parameters (needs work , optional are not handled)
              for (PsiElement element : matchesInImport) {
                if (element instanceof HaxeEnumValueDeclaration enumValueDeclaration) {
                  int currentSize =
                    Optional.ofNullable(enumValueDeclaration.getParameterList()).map(p -> p.getParameterList().size()).orElse(0);
                  if (expectedSize == currentSize) {
                    LogResolution(reference, "via import  & enum value declaration");
                    return List.of(element);
                  }
                }
              }
            }
            return matchesInImport.isEmpty() ? null : matchesInImport;
          }
        PsiElement target = HaxeResolveUtil.searchInSamePackage(fileModel, referenceText);

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
    }
    if (result == null || result.isEmpty()) {
      // to avoid caching empty due to already being resolved we mark
      // elements so we know if we want to cache as not found or just skip (null is not cached, empty list is cached)
      if (incompleteCode || reference.getUserData(skipCacheKey) == Boolean.TRUE) {
        if (log.isTraceEnabled()) {
          String message = "result is empty and skip cache flag is true, skipping cache for: " + referenceText;
          traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
        }
        return null;
      }else {
        if (log.isTraceEnabled()){
          String message = "result is empty caching not found for :" + referenceText;
          traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
        }
        return EMPTY_LIST;
      }
    }else {
      if (log.isTraceEnabled()){
        String message = "caching result for :" + referenceText;
        traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
      }
      return result;
    }
  }

  // checks if we are attempting to  assign an enum type, this makes sure we chose the enum value and not competing class names
  private List<? extends PsiElement> checkEnumMemberHints(HaxeReference reference) {
    HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class, true);
    if (switchCaseExpr != null) {
      HaxeSwitchStatement parentSwitch = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (parentSwitch != null) {
        HaxeExpression expression = parentSwitch.getExpression();
        HaxeExpressionEvaluatorContext evaluate = HaxeExpressionEvaluator.evaluate(expression, null);
        ResultHolder result = evaluate.result;
        if (result.getClassType() != null) {
          SpecificTypeReference typeReference = result.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
          return findEnumMember(reference, typeReference);
        }
      }
    }

    HaxePsiField fieldFromReferenceExpression = null;
    HaxeAssignExpression assignExpression = PsiTreeUtil.getParentOfType(reference, HaxeAssignExpression.class);
    if (assignExpression != null) {
      HaxeExpression left = assignExpression.getLeftExpression();
      //guard to avoid another resolve of the same reference, and attempts to check assignExpression for only part of a reference expression
      if (left != reference && !(reference.getParent() instanceof  HaxeReferenceExpression)) {
        if (left instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement resolve = referenceExpression.resolve();
          if (resolve instanceof HaxePsiField psiField) {
            fieldFromReferenceExpression = psiField;
          }
        }
      }
    }
    if (reference.getParent() instanceof HaxeCompareExpression compareExpression ) {
      if (compareExpression.getLeftExpression() instanceof HaxeReferenceExpression referenceExpression) {
        if (referenceExpression != reference) {//guard to avoid another resolve of the same reference
          PsiElement resolve = referenceExpression.resolve();
          if (resolve instanceof HaxePsiField psiField) {
            fieldFromReferenceExpression = psiField;
          }
        }
      }
    }


    HaxePsiField field = fieldFromReferenceExpression != null ? fieldFromReferenceExpression :  PsiTreeUtil.getParentOfType(reference, HaxePsiField.class);
    if (field != null) {
      HaxeTypeTag tag = field.getTypeTag();
      if (tag != null && tag.getTypeOrAnonymous() != null) {
        ResultHolder type = HaxeTypeResolver.getTypeFromTypeOrAnonymous(tag.getTypeOrAnonymous());
        if (type.getClassType() != null) {
          SpecificTypeReference typeReference = type.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
          return findEnumMember(reference, typeReference);
        }
      }
    }

    HaxeParameter parameter = PsiTreeUtil.getParentOfType(reference, HaxeParameter.class);
    if (parameter != null) {
      HaxeTypeTag tag = parameter.getTypeTag();
      if (tag != null && tag.getTypeOrAnonymous() != null) {
        ResultHolder type = HaxeTypeResolver.getTypeFromTypeOrAnonymous(tag.getTypeOrAnonymous());
        if (type.getClassType() != null) {
          SpecificTypeReference typeReference = type.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
          return findEnumMember(reference, typeReference);
        }
      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeNamedComponent> findEnumMember(HaxeReference reference, SpecificTypeReference typeReference) {
      if (typeReference instanceof  SpecificHaxeClassReference classReference) {
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        if (classModel != null && classModel.isEnum()) {
          HaxeClass haxeClass = classReference.getHaxeClass();
          if (haxeClass != null) {
            HaxeNamedComponent name = haxeClass.findHaxeMemberByName(reference.getText(), null);
            if (name != null) {
              LogResolution(reference, "via enum member name.");
              return List.of(name);
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
      PsiElement target = HaxeResolveUtil.searchInSameFile(fileModel, className, isType);
      if (target!= null) {
        LogResolution(reference, "via search In Same File");
        return List.of(target);
      }
    }
    return null;
  }

  private List<? extends PsiElement> checkMacroIdentifier(HaxeReference reference) {
    @NotNull PsiElement[] children = reference.getChildren();
    if (children.length == 1) {
      if (children[0] instanceof  HaxeIdentifier identifier) {
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
          List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
          return findTypeParameterPsi(reference, params);
        }
      }

       HaxeMethodDeclaration methodDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeMethodDeclaration.class);
      if (methodDeclaration != null) {
        List<HaxeGenericParamModel> methodParams = methodDeclaration.getModel().getGenericParams();
        List<HaxeGenericListPart> methodTypeParameter = findTypeParameterPsi(reference, methodParams);
        if (methodTypeParameter != null) {
          return methodTypeParameter;
        }
        HaxeClassModel declaringClass = methodDeclaration.getModel().getDeclaringClass();
        List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
        return findTypeParameterPsi(reference, params);
      }

      HaxeConstructorDeclaration constructorDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeConstructorDeclaration.class);
      if (constructorDeclaration != null) {
        // reference is a type tag in constructor, we should check  owning class type parameters
        // so we won't resolve this to a type outside the class if its a type parameter
        HaxeClassModel declaringClass = constructorDeclaration.getModel().getDeclaringClass();
        List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
        return findTypeParameterPsi(reference, params);

      }
      HaxeEnumValueDeclaration enumDeclaration = PsiTreeUtil.getParentOfType(typeTag, HaxeEnumValueDeclaration.class);
      if (enumDeclaration != null) {
        // EnumValueDeclarations does not define TypeParameters, only the parent EnumType can have these.
        HaxeClassModel declaringClass = ((HaxeEnumValueModel)enumDeclaration.getModel()).getDeclaringClass();
        List<HaxeGenericParamModel> params = declaringClass.getGenericParams();
        return findTypeParameterPsi(reference, params);

      }
    }
    return null;
  }

  @Nullable
  private static List<HaxeGenericListPart> findTypeParameterPsi(HaxeReference reference, List<HaxeGenericParamModel> params) {
    Optional<HaxeGenericListPart> first = params.stream()
      .filter(p -> p.getName().equals(reference.getText()))
      .map(HaxeGenericParamModel::getPsi)
      .findFirst();
    if (first.isPresent()) {
      LogResolution(reference, "via TypeParameter Psi");
      return List.of(first.get());
    }
    return null;
  }

  private List<? extends PsiElement> checkEnumExtractor(HaxeReference reference) {
    if (reference.getParent() instanceof  HaxeEnumValueReference) {
      HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class);
      SpecificHaxeClassReference classReference = HaxeResolveUtil.resolveExtractorEnum(argumentExtractor);
      if (classReference != null) {
        HaxeEnumValueDeclaration declaration = HaxeResolveUtil.resolveExtractorEnumValueDeclaration(classReference, argumentExtractor);
        if (declaration!= null) {
          LogResolution(reference, "via enum extractor");
          return List.of(declaration);
        }
      }
    }else {
      // Last attempt to resolve  enum value (not extractor), normally imports would solve this but  some typedefs can omit this.
      HaxeSwitchStatement type = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (type!= null && type.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
        HaxeResolveResult result = referenceExpression.resolveHaxeClass();
        if (result.isHaxeTypeDef()) {
          result = result.fullyResolveTypedef();
        }
        HaxeClass haxeClass = result.getHaxeClass();
        if(haxeClass != null && haxeClass.isEnum()) {
          SpecificHaxeClassReference classReference = result.getSpecificClassReference(haxeClass, null);
          HaxeEnumValueDeclaration declaration = HaxeResolveUtil.resolveExtractorEnumValueDeclaration(classReference, reference.getText());
          if (declaration!= null) {
            LogResolution(reference, "via enum extractor");
            return List.of(declaration);
          }
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

    List<? extends PsiElement> result = null;

    // NOTE: this one has to come before  `checkIfSwitchCaseDefaultValue`
    // check if default name in match expression (ex  `case TString(_ => captureVar)`)
    result = checkIfDefaultValueInMatchExpression(reference, switchCaseExpr);

    // check if matches default name ( ex. `case _:`)
    if (result == null) result = checkIfSwitchCaseDefaultValue(reference);

    // checks if it matches default name inside array (ex. `case [2, _]:`)
    if (result == null) result = checkIfDefaultNameInCaseArray(reference, switchCaseExpr);

    // try to resolve reference for guard and block (ex. `case [a, b] if ( b > a): b + ">" + a;`)
    if (result == null) result = tryResolveVariableForGuardsAndBlock(reference);
    if (result != null) {
      LogResolution(reference, "via switch var");
    }
    return result;
  }

  @Nullable
  private static List<? extends HaxeExpression> tryResolveVariableForGuardsAndBlock(HaxeReference reference) {
    HaxeGuard guard = PsiTreeUtil.getParentOfType(reference, HaxeGuard.class);
    HaxeSwitchCaseBlock switchCaseBlock = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseBlock.class);
    if (switchCaseBlock != null || guard != null) {
      HaxeSwitchCase switchCase = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCase.class);
      if (switchCase!= null) {
        List<HaxeSwitchCaseExpr> list = switchCase.getSwitchCaseExprList();
        for (HaxeSwitchCaseExpr caseExpr : list) {
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
  private static List<PsiElement> checkIfDefaultNameInCaseArray(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    if (switchCaseExpr != null && reference.getParent().getParent() instanceof  HaxeArrayLiteral) {
      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(reference, HaxeSwitchStatement.class);
      if (switchStatement != null && switchStatement.getExpression() != null) {
        // should be array, but can be anything
        ResultHolder type = HaxeTypeResolver.getPsiElementType(switchStatement.getExpression(), null);
        SpecificHaxeClassReference classReference = type.getClassType();
        if(classReference != null && classReference.getSpecifics().length > 0) {
          return List.of(classReference.getSpecifics()[0].getElementContext());
        }
      }
    }
    return null;
  }

  @Nullable
  private List<HaxeParameter> checkIfDefaultValueInMatchExpression(HaxeReference reference, HaxeSwitchCaseExpr switchCaseExpr) {
    HaxeExtractorMatchExpression matchExpression = PsiTreeUtil.getParentOfType(reference, HaxeExtractorMatchExpression.class);

    if (matchExpression != null) {
      HaxeEnumArgumentExtractor argumentExtractor = PsiTreeUtil.getParentOfType(reference, HaxeEnumArgumentExtractor.class);
      if (argumentExtractor!= null) {
        SpecificHaxeClassReference enumClass = HaxeResolveUtil.resolveExtractorEnum(argumentExtractor);
        if (enumClass != null) {
          HaxeClassModel model = enumClass.getHaxeClassModel();
          if (model != null) {
            HaxeMemberModel enumValue = model.getMember(argumentExtractor.getEnumValueReference().getText(), null);
            if (enumValue instanceof  HaxeEnumValueModel enumValueModel) {
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

  @Nullable
  private static List<@NotNull HaxeExpression> checkIfSwitchCaseDefaultValue(HaxeReference reference) {
    if (reference.textMatches("_")) {
      // if is part of an expression
      HaxeSwitchCaseExpr switchCaseExpr = PsiTreeUtil.getParentOfType(reference, HaxeSwitchCaseExpr.class);
      if (switchCaseExpr != null) {
        if (switchCaseExpr.getParent() instanceof HaxeExtractorMatchExpression matchExpression) {
          //  reference should be  previous matchExpression as it's the value/result from that one that is passed as _
          return List.of(matchExpression.getExpression());
        } else {
          HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(switchCaseExpr, HaxeSwitchStatement.class);
          if (switchStatement != null && switchStatement.getExpression() != null) {
            return List.of(switchStatement.getExpression());
          }
        }
      }
    }
    return null;
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
  private List<? extends PsiElement> checkByTreeWalk(HaxeReference reference) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, reference.getText()), reference, null, new ResolveState());
    if (result.isEmpty()) return null;
    LogResolution(reference, "via tree walk.");
    return result;
  }
  private List<? extends PsiElement> checkByTreeWalk(HaxeReference scope, String name) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, name), scope, null, new ResolveState());
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
        return asList(method.getBasePsi());
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
      assert haxeClass != null;
      if (!haxeClass.getHaxeExtendsList().isEmpty()) {
        final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
        final HaxeClass superClass = ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass();
        final HaxeNamedComponent constructor =
          ((superClass == null) ? null : superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString(), null)); // Self only.
        LogResolution(reference, "because it's a super expression.");
        return asList(((constructor != null) ? constructor : superClass));
      }
    }

    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsType(HaxeReference reference) {
    final HaxeType type = PsiTreeUtil.getParentOfType(reference, HaxeType.class);
    final HaxeClass haxeClassInType = HaxeResolveUtil.tryResolveClassByQName(type);
    if (type != null && haxeClassInType != null) {
      LogResolution(reference, "via parent type name.");
      return asList(haxeClassInType.getComponentName());
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

    String identifier = reference instanceof HaxeReferenceExpression referenceExpression ? referenceExpression.getIdentifier().getText() : reference.getText();
    final HaxeResolveResult leftExpression = lefthandExpression.resolveHaxeClass();
    if (leftExpression.getHaxeClass() != null) {
      HaxeMemberModel member = leftExpression.getHaxeClass().getModel().getMember(identifier, leftExpression.getGenericResolver());
      if (member != null) {
        return Collections.singletonList(member.getBasePsi());
      }
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

      HaxeMethodModel foundMethod = null;
      for (int i = usingModels.size() - 1; i >= 0; --i) {
        foundMethod = usingModels.get(i)
          .findExtensionMethod(identifier, leftExpression.getSpecificClassReference(reference, leftExpression.getGenericResolver()));
        if (null != foundMethod && !foundMethod.HasNoUsingMeta()) {

          if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + foundMethod.getName());
          return asList(foundMethod.getBasePsi());
        }
        // check other types ("using" can be used to find typedefsetc)
        PsiElement element = usingModels.get(i).exposeByName(identifier);
        if (element != null) {
          if (log.isTraceEnabled()) log.trace("Found method in 'using' import: " + identifier);
          return List.of(element);
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
    return resolveByClassAndSymbol(leftExpression, reference);
  }

  private PsiElement resolveQualifiedReference(HaxeReference reference) {
    String qualifiedName = reference.getText();

    final FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(qualifiedName);
    List<HaxeModel> result = HaxeProjectModel.fromElement(reference).resolve(qualifiedInfo, reference.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.get(0);
      if (item instanceof HaxeFileModel) {
        HaxeClassModel mainClass = ((HaxeFileModel)item).getMainClassModel();
        if (mainClass != null) {
          return mainClass.getBasePsi();
        }
      }
      return item.getBasePsi();
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
    HaxeClass leftResultClass = HaxeResolveUtil.tryResolveClassByQName(leftReference);
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

        HaxeMemberModel member = model.getMember(helperName, resolveResult.getGenericResolver());
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
      final HaxeTypeOrAnonymous toa = model.getUnderlyingType();
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
      if(type.getTypeParam() != null ) {
        HaxeGenericResolver localResolver = specialization.toGenericResolver(type);
        List<String> names = getGenericParamNames(nakedResult);
        List<HaxeTypeListPart> typeParameterList = type.getTypeParam().getTypeList().getTypeListPartList();
        for (int i = 0; i < typeParameterList.size(); i++) {
          if (names.size() -1 < i) break;
          String name = names.get(i);
          HaxeTypeListPart part = typeParameterList.get(i);
          if (part.getTypeOrAnonymous() != null) {
            genericResolver.add(name, HaxeTypeResolver.getTypeFromTypeOrAnonymous(part.getTypeOrAnonymous(), localResolver));
          }
          else if (part.getFunctionType() != null) {
            //TODO resolve  with resolver ?
            ResultHolder type1 = HaxeTypeResolver.getTypeFromFunctionType(part.getFunctionType());
            genericResolver.add(name, type1);
          }
        }
      }

      result = HaxeResolveResult.create(nakedResult.getHaxeClass(), HaxeGenericSpecialization.fromGenericResolver(null, genericResolver));
      model = null != result.getHaxeClass() ? result.getHaxeClass().getModel() : null;
      specialization = result.getSpecialization();
    }
    return result;
  }

  @NotNull
  private static List<String> getGenericParamNames(HaxeResolveResult nakedResult) {
    HaxeClass haxeClass = nakedResult.getHaxeClass();
    if (haxeClass == null) return  List.of();
    HaxeGenericParam param = haxeClass.getGenericParam();
    if (param == null) return  List.of();
    return  param.getGenericListPartList().stream().map(genericListPart -> genericListPart.getComponentName().getName()).toList();
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
    HaxeGenericResolver resolver = getGenericResolver(leftClass, reference);
    return resolveByClassAndSymbol(leftClass, resolver, reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                    @Nullable HaxeGenericResolver resolver,
                                                                    @NotNull HaxeReference reference) {
    // TODO: This method is very similar to resolveChain, and they should probably be combined.

    if (leftClass != null) {
      final HaxeClassModel leftClassModel = leftClass.getModel();
      HaxeMemberModel member = leftClassModel.getMember(reference.getReferenceName(), resolver);
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
    final String name;

    private ResolveScopeProcessor(List<PsiElement> result, String name) {
      this.result = result;
      this.name = name;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, ResolveState state) {
      HaxeComponentName componentName = null;
      if (element instanceof HaxeComponentName) {
        componentName = (HaxeComponentName)element;
      }
      else if (element instanceof HaxeNamedComponent) {
        componentName = ((HaxeNamedComponent)element).getComponentName();
      }
      else if (element instanceof HaxeOpenParameterList) {
        componentName = ((HaxeOpenParameterList)element).getComponentName();
      }
      else if (element instanceof HaxeSwitchCaseExpr expr) {
        if (!executeForSwitchCase(expr)) return false;
      }

      if (componentName != null && name.equals(componentName.getText())) {
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
            HaxeComponentName componentName = extractedValue.getComponentName();
            if (name.equals(componentName.getText())) {
              result.add(componentName);
              return false;
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