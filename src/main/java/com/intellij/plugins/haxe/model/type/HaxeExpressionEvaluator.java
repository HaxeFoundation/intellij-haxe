/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeCallExpressionUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceExpressionImpl;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.KUNTYPED;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl.getLiteralClassName;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl.tryToFindTypeFromCallExpression;
import static com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil.createInheritedClassResolver;
import static com.intellij.plugins.haxe.model.type.SpecificFunctionReference.Argument;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.*;

@CustomLog
public class HaxeExpressionEvaluator {
  static { log.setLevel(LogLevel.INFO); }

  @NotNull
  static public HaxeExpressionEvaluatorContext evaluate(PsiElement element, HaxeGenericResolver resolver) {
    ProgressIndicatorProvider.checkCanceled();
    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(element);
    context.result = handle(element, context, resolver);
    return context;
  }
  @NotNull
  static public HaxeExpressionEvaluatorContext evaluate(PsiElement element, HaxeExpressionEvaluatorContext context,
                                                        HaxeGenericResolver resolver) {
    ProgressIndicatorProvider.checkCanceled();
    context.result = handle(element, context, resolver);
    return context;
  }

  private static ThreadLocal<HashSet<PsiElement>> resolvesInProcess = new ThreadLocal<>().withInitial(()->new HashSet<PsiElement>());
  @Nullable
  private static ResultHolder handleWithRecursionGuard(PsiElement element,
                                                       HaxeExpressionEvaluatorContext context,
                                                       HaxeGenericResolver resolver) {
    if (element == null ) return null;
    HashSet<PsiElement> elements = resolvesInProcess.get();
    try {
      if (elements.contains(element)) return null;
      elements.add(element);
      return handle(element, context, resolver);
    } finally {
      elements.remove(element);
    }
  }

  @NotNull
  static private ResultHolder handle(final PsiElement element,
                                     final HaxeExpressionEvaluatorContext context,
                                     final HaxeGenericResolver resolver) {
    try {
      ProgressIndicatorProvider.checkCanceled();
      return _handle(element, context, resolver);
    }
    catch (NullPointerException e) {
      // Make sure that these get into the log, because the GeneralHighlightingPass swallows them.
      log.error("Error evaluating expression type for element " + element.toString(), e);
      throw e;
    }
    catch (ProcessCanceledException e) {
      // Don't log these, because they are common, but DON'T swallow them, either; it makes things unresponsive.
      throw e;
    }
    catch (Throwable t) {
      // XXX: Watch this.  If it happens a lot, then maybe we shouldn't log it unless in debug mode.
      log.warn("Error evaluating expression type for element " + (null == element ? "<null>" : element.toString()), t);
    }
    return SpecificHaxeClassReference.getUnknown(element != null ? element : context.root).createHolder();
  }

  @NotNull
  static private ResultHolder _handle(final PsiElement element,
                                      final HaxeExpressionEvaluatorContext context,
                                      HaxeGenericResolver resolver) {
    if (element == null) {
      return SpecificHaxeClassReference.getUnknown(context.root).createHolder();
    }
    if (resolver == null) resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(element);

    if(log.isDebugEnabled())log.debug("Handling element: " + element);
    if (element instanceof PsiCodeBlock) {
      context.beginScope();
      context.getScope().deepSearchForReturnValues = true;

      ResultHolder type = SpecificHaxeClassReference.getUnknown(element).createHolder();
      boolean deadCode = false;
      for (PsiElement childElement : element.getChildren()) {
        type = handle(childElement, context, resolver);
        if (deadCode) {
          //context.addWarning(childElement, "Unreachable statement");
          context.addUnreachable(childElement);
        }
        if (childElement instanceof HaxeReturnStatement) {
          deadCode = true;
        }
      }
      context.endScope();
      return type;
    }
    if (element instanceof  HaxeImportAlias alias) {
      HaxeResolveResult result = alias.resolveHaxeClass();
      HaxeClass haxeClass = result.getHaxeClass();
      if (haxeClass == null) {
        return new ResultHolder(SpecificHaxeClassReference.getUnknown(element));
      }else {
        return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(haxeClass.getModel(),element)).createHolder();
      }
    }

    if (element instanceof HaxeReturnStatement returnStatement) {
      ResultHolder result = SpecificHaxeClassReference.getVoid(element).createHolder();
      if (isUntypedReturn(returnStatement)) return result;
      PsiElement[] children = element.getChildren();
      if (children.length >= 1) {
        result = handle(children[0], context, resolver);
      }
      context.addReturnType(result, element);
      return result;
    }
    if (element instanceof HaxeTryStatement tryStatement) {
     //  try-catch can be used as a value expression all blocks must be evaluated and unified
     //  we should also iterate trough so we can pick up any return statements
      @NotNull PsiElement[] children = tryStatement.getChildren();
      List<ResultHolder> blockResults = new ArrayList<>();
      for (PsiElement child : children) {
        blockResults.add(handle(child, context, resolver));
      }
      return HaxeTypeUnifier.unifyHolders(blockResults, tryStatement);
    }
    if (element instanceof HaxeIterable iterable) {
      ResultHolder iteratorParent = handle(iterable.getExpression(), context, resolver);
      SpecificTypeReference type = iteratorParent.getType();
      if (!type.isNumeric()) {
        if (iteratorParent.isClassType()) {
          SpecificHaxeClassReference haxeClassReference = iteratorParent.getClassType();
          HaxeGenericResolver localResolver =  new HaxeGenericResolver();
          localResolver.addAll(resolver);
          localResolver.addAll(haxeClassReference.getGenericResolver());// replace parent/old resolver values with newer from class reference
          if (haxeClassReference != null && haxeClassReference.getHaxeClassModel() != null) {
            HaxeForStatement parentForLoop = PsiTreeUtil.getParentOfType(iterable, HaxeForStatement.class);

            if(haxeClassReference.isTypeDefOfClass()) {
              SpecificTypeReference typeReference = haxeClassReference.fullyResolveTypeDefReference();
              if (typeReference instanceof  SpecificHaxeClassReference classReference) {
                HaxeGenericResolver typeDefResolved = classReference.getGenericResolver();
                localResolver.addAll(typeDefResolved);
              }
            }

            if (parentForLoop.getKeyValueIterator() == null) {
              HaxeMemberModel iterator = haxeClassReference.getHaxeClassModel().getMember("iterator", resolver);
              if (iterator instanceof HaxeMethodModel methodModel) {
                  return methodModel.getReturnType(localResolver);
              }
            }else {
              HaxeMemberModel iterator = haxeClassReference.getHaxeClassModel().getMember("keyValueIterator", resolver);
              if (iterator instanceof HaxeMethodModel methodModel) {
                return methodModel.getReturnType(localResolver);
              }
            }
          }
        }
      }

      return handle(((HaxeIterable)element).getExpression(), context, resolver);
    }

    if (element instanceof HaxeForStatement forStatement) {
        final HaxeExpression forStatementExpression = forStatement.getExpression();
        final HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
        final HaxeComponentName name = forStatement.getComponentName();
        final HaxeIterable iterable = forStatement.getIterable();
        final PsiElement body = element.getLastChild();
        context.beginScope();

           if(context.getScope().deepSearchForReturnValues) handle(body, context, resolver);

        try {
          final SpecificTypeReference iterableValue = handle(iterable, context, resolver).getType();
          ResultHolder iteratorResult = iterableValue.getIterableElementType(resolver);
          SpecificTypeReference type = iteratorResult != null ? iteratorResult.getType() : null;
          //TODO: HACK?
          // String class in in standard lib is  currently missing iterator methods
          // this is a workaround  so we can iterate on chars in string.
          if (type == null && iterableValue.isString()) {
            type = iterableValue;
          }
          if (type != null) {
            if (forStatementExpression != null) {
              ResultHolder handle = handle(forStatementExpression, context, resolver);
              if (handle.getType() != null) {
                return handle.getType().createHolder();
              }
            }
            if (type.isTypeParameter()) {
              if (iterable.getExpression() instanceof  HaxeReference reference) {
                HaxeResolveResult result = reference.resolveHaxeClass();
                HaxeGenericResolver classResolver = result.getGenericResolver();
                ResultHolder holder = type.createHolder();
                ResultHolder resolved = classResolver.resolve(holder);
                if (!resolved.isUnknown()) {
                  return resolved;
                }
              }
            }
            return new ResultHolder(type);
          }
          if ( type != null) {
            if (iterableValue.isConstant()) {
              if (iterableValue.getConstant() instanceof HaxeRange constant) {
                type = type.withRangeConstraint(constant);
              }
            }
            if (name != null) {
              context.setLocal(name.getText(), new ResultHolder(type));
            } else if (keyValueIterator != null) {
              context.setLocal(keyValueIterator.getIteratorkey().getComponentName().getText(), new ResultHolder(type));
              context.setLocal(keyValueIterator.getIteratorValue().getComponentName().getText(), new ResultHolder(type));
            }
            return handle(body, context, resolver);
          }
        }
        finally {
          context.endScope();
        }
      }


    if (element instanceof HaxeSwitchStatement switchStatement) {
      // TODO: Evaluating result of switch statement should properly implemented
      List<SpecificTypeReference> typeList = new LinkedList<>();
      SpecificTypeReference bestGuess = null;

      if(switchStatement.getSwitchBlock() != null) {
        List<HaxeSwitchCase> caseList = switchStatement.getSwitchBlock().getSwitchCaseList();

        for (HaxeSwitchCase switchCase : caseList) {
          HaxeSwitchCaseBlock block = switchCase.getSwitchCaseBlock();
          if (block != null) {
            ResultHolder handle = handle(block, context, resolver);
            if (!handle.isUnknown())typeList.add(handle.getType());
          }
        }

        for (SpecificTypeReference typeReference : typeList) {
          if (typeReference.isVoid()) continue;
          if (bestGuess == null) {
            bestGuess = typeReference;
            continue;
          }
          bestGuess = HaxeTypeUnifier.unify(bestGuess, typeReference, element);
        }
      }

      if (bestGuess != null) {
        return new ResultHolder(bestGuess);
      }else {
        return new ResultHolder(SpecificHaxeClassReference.getUnknown(element));
      }
    }

    if (element instanceof HaxeEnumExtractedValue extractedValue) {

      HaxeEnumArgumentExtractor extractor = PsiTreeUtil.getParentOfType(extractedValue, HaxeEnumArgumentExtractor.class);

      // TODO mlo should probably move the haxe model logic to the PSI implementation so we can cache it
      HaxeEnumExtractorModel extractorModel =  new HaxeEnumExtractorModel(extractor);
      HaxeEnumValueModel enumValueModel =  extractorModel.getEnumValueModel();
      if (enumValueModel != null) {
        int index = extractorModel.findExtractValueIndex(extractedValue);
        HaxeGenericResolver extractorResolver =  extractorModel.getGenericResolver();
        return  enumValueModel.getParameterType(index, extractorResolver);
      }
    }

    if (element instanceof  HaxeSwitchCaseBlock caseBlock) {
      List<HaxeReturnStatement> list = caseBlock.getReturnStatementList();
      for (HaxeReturnStatement  statement : list) {
        ResultHolder returnType = handle(statement, context, resolver);
        context.addReturnType(returnType, statement);
      }
      List<HaxeExpression> expressions = caseBlock.getExpressionList();
      if (!expressions.isEmpty()) {
        HaxeExpression lastExpression = expressions.get(expressions.size() - 1);
        return handle(lastExpression, context, resolver);
      }
      return new ResultHolder(SpecificHaxeClassReference.getVoid(element));
    }

    if (element instanceof HaxeNewExpression expression) {
      HaxeType type = expression.getType();
      if (type != null) {
        if (isMacroVariable(type.getReferenceExpression().getIdentifier())){
          return SpecificTypeReference.getDynamic(element).createHolder();
        }
        ResultHolder hint = resolver.getAssignHint();
        ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(type, resolver);
        if (hint != null && hint.isClassType()) {
          HaxeGenericResolver localResolver = new HaxeGenericResolver();
          HaxeGenericResolver hintsResolver = hint.getClassType().getGenericResolver();
          localResolver.addAll(hintsResolver);
          ResultHolder resolvedWithHint = localResolver.resolve(typeHolder);
          if (resolvedWithHint != null && !resolvedWithHint.isUnknown()) typeHolder = resolvedWithHint;
        }

      if (!typeHolder.isUnknown() && typeHolder.getClassType() != null) {
        SpecificHaxeClassReference classReference = typeHolder.getClassType();
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        HaxeGenericResolver classResolver = classReference.getGenericResolver();
        if (classModel != null) {
          HaxeMethodModel constructor = classModel.getConstructor(classResolver);
          if (constructor != null) {
            HaxeMethod method = constructor.getMethod();
            HaxeMethodModel methodModel = method.getModel();
            if (methodModel.getGenericParams().isEmpty()) {
              HaxeCallExpressionUtil.CallExpressionValidation validation = HaxeCallExpressionUtil.checkConstructor(expression);
              HaxeGenericResolver resolverFromCallExpression = validation.getResolver();

              if (resolverFromCallExpression != null) {
                ResultHolder resolve = resolverFromCallExpression.resolve(typeHolder);
                if (!resolve.isUnknown()) typeHolder = resolve;
              }
            }
          }
        }
      }
        // if new expression is missing typeParameters try to resolve from usage
        if (type.getTypeParam() == null && typeHolder.getClassType() != null && typeHolder.getClassType().getSpecifics().length > 0) {
          HaxePsiField fieldDeclaration = PsiTreeUtil.getParentOfType(expression, HaxePsiField.class);
          if (fieldDeclaration != null && fieldDeclaration.getTypeTag() == null) {
            SpecificHaxeClassReference classType = typeHolder.getClassType();
            // if class does not have any  generics there  no need to search for references
            if (classType != null  && classType.getSpecifics().length > 0) {
              ResultHolder searchResult = searchReferencesForTypeParameters(fieldDeclaration, context, resolver, typeHolder);
              if (!searchResult.isUnknown()) {
                typeHolder = searchResult;
              }
            }
          }
        }
        if (typeHolder.getType() instanceof SpecificHaxeClassReference classReference) {
          final HaxeClassModel clazz = classReference.getHaxeClassModel();
          if (clazz != null) {
            HaxeMethodModel constructor = clazz.getConstructor(resolver);
            if (constructor == null) {
              context.addError(element, "Class " + clazz.getName() + " doesn't have a constructor", new HaxeFixer("Create constructor") {
                @Override
                public void run() {
                  // @TODO: Check arguments
                  clazz.addMethod("new");
                }
              });
            } else {
              //checkParameters(element, constructor, expression.getExpressionList(), context, resolver);
            }
          }
        }
        return typeHolder.duplicate();
      }
    }

    if (element instanceof HaxeThisExpression) {
      //PsiReference reference = element.getReference();
      //HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
      HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(element, HaxeClass.class);
      if (ancestor == null) return SpecificTypeReference.getDynamic(element).createHolder();
      HaxeClassModel model = ancestor.getModel();
      if (model.isAbstractType()) {
        SpecificHaxeClassReference reference = model.getUnderlyingClassReference(resolver);
        if (null != reference) {
          return reference.createHolder();
        }
      }
      ResultHolder[] specifics =  HaxeTypeResolver.resolveDeclarationParametersToTypes(model.haxeClass, resolver);
      return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(model, element), specifics).createHolder();
    }

    if (element instanceof HaxeIdentifier identifier) {
      if (isMacroVariable(identifier)) {
        return SpecificTypeReference.getDynamic(element).createHolder();
      }
      // If it has already been seen, then use whatever type is already known.
      ResultHolder holder = context.get(element.getText());

      if (holder == null) {
        // context.addError(element, "Unknown variable", new HaxeCreateLocalVariableFixer(element.getText(), element));

        return SpecificTypeReference.getUnknown(element).createHolder();
      }

      return holder;
    }

    if (element instanceof HaxeRestParameter restParameter) {
      HaxeTypeTag tag = restParameter.getTypeTag();
      ResultHolder type = HaxeTypeResolver.getTypeFromTypeTag(tag, restParameter);
      return new ResultHolder(SpecificTypeReference.getStdClass(ARRAY, restParameter, new ResultHolder[]{type}));
    }

    if (element instanceof HaxeParameter parameter) {
      HaxeTypeTag typeTag = parameter.getTypeTag();
      if (typeTag != null) {
        ResultHolder typeFromTypeTag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
        ResultHolder resolve = resolver.resolve(typeFromTypeTag);
        if (!resolve.isUnknown()) return resolve;
        return typeFromTypeTag;
      }

      HaxeVarInit init = parameter.getVarInit();
      if (init != null) {
        ResultHolder holder = handle(init, context, resolver);
        if (!holder.isUnknown()) {
          return holder;
        }
       }else {
        if (element.getParent().getParent() instanceof HaxeFunctionLiteral functionLiteral) {
          ResultHolder holder = tryToFindTypeFromCallExpression(functionLiteral, parameter);
          if (holder!= null && !holder.isUnknown()) {
            ResultHolder resolve = resolver.resolve(holder);
            return resolve != null  && !resolve.isUnknown() ? resolve : holder;
          }else {
            return SpecificHaxeClassReference.getUnknown(element).createHolder();
          }
        }else {
          HaxeMethod method = PsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
          ResultHolder holder = searchReferencesForType(parameter.getComponentName(), context, resolver, method.getBody());
          if (holder!= null && !holder.isUnknown()) {
            return holder;
          }
        }
      }
    }
    if (element instanceof HaxeSpreadExpression spreadExpression) {
      HaxeExpression expression = spreadExpression.getExpression();
      // we treat restParameters as arrays, so we need to "unwrap" the array to get the correct type.
      // (currently restParameters and Arrays are the only types you can spread afaik. and only in method calls)
      if (expression instanceof HaxeReferenceExpression referenceExpression) {
        ResultHolder type = HaxeTypeResolver.getPsiElementType(referenceExpression, resolver);
        if (type.isClassType()) {
          ResultHolder[] specifics = type.getClassType().getSpecifics();
          if (specifics.length == 1) {
            return specifics[0];
          }
        }
      }
      else if (expression instanceof HaxeArrayLiteral arrayLiteral) {
        HaxeResolveResult result = arrayLiteral.resolveHaxeClass();
        SpecificHaxeClassReference reference = result.getSpecificClassReference(expression, resolver);
        @NotNull ResultHolder[] specifics = reference.getSpecifics();
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
    }
    if (element instanceof HaxeFieldDeclaration declaration) {
      HaxeTypeTag typeTag = declaration.getTypeTag();

      if (typeTag!= null) {
        return HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
      }else {
        HaxeVarInit init = declaration.getVarInit();
        if (init != null) {
          return handle(init.getExpression(), context, resolver);
        }
      }
    }

    if (element instanceof HaxeCastExpression castExpression) {
      HaxeTypeOrAnonymous anonymous = castExpression.getTypeOrAnonymous();
      if (anonymous != null) {
        return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
      } else {
        return SpecificHaxeClassReference.getUnknown(element).createHolder();
      }
    }


    if (element instanceof HaxeWhileStatement whileStatement) {
      HaxeDoWhileBody whileBody = whileStatement.getBody();
      HaxeBlockStatement blockStatement = null != whileBody ? whileBody.getBlockStatement() : null;
      List<HaxeExpression> list = null != blockStatement ? blockStatement.getExpressionList() : Collections.emptyList();
      SpecificTypeReference type = null;
      HaxeExpression lastExpression = null;
      for (HaxeExpression expression : list) {
        type = handle(expression, context, resolver).getType();
        lastExpression = expression;
      }
      if (type == null) {
        type = SpecificTypeReference.getDynamic(element);
      }
      if (!type.isBool() && lastExpression != null) {
        context.addError(
          lastExpression,
          "While expression must be boolean",
          new HaxeCastFixer(lastExpression, type, SpecificHaxeClassReference.getBool(element))
        );
      }

      PsiElement body = element.getLastChild();
      if (body != null) {
        //return SpecificHaxeClassReference.createArray(result); // @TODO: Check this
        return handle(body, context, resolver);
      }

      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLocalVarDeclarationList varDeclarationList) {
      // Var declaration list is a statement that returns a Void type, not the type of the local vars it creates.
      // We still evaluate its sub-parts so that we can set the known value types of variables in the scope.
      for (HaxeLocalVarDeclaration part : varDeclarationList.getLocalVarDeclarationList()) {
        handle(part, context, resolver);
      }
      return SpecificHaxeClassReference.getVoid(element).createHolder();
    }

    if (element instanceof HaxeAssignExpression) {
      final PsiElement left = element.getFirstChild();
      final PsiElement right = element.getLastChild();
      if (left != null && right != null) {
        final ResultHolder leftResult = handle(left, context, resolver);
        final ResultHolder rightResult = handle(right, context, resolver);

        if (leftResult.isUnknown()) {
          leftResult.setType(rightResult.getType());
          context.setLocalWhereDefined(left.getText(), leftResult);
        }
        leftResult.removeConstant();

        final SpecificTypeReference leftValue = leftResult.getType();
        final SpecificTypeReference rightValue = rightResult.getType();

        //leftValue.mutateConstantValue(null);

        // skipping `canAssign` check if we dont have a holder to add annotations to
        // this is probably just waste of time when resolving in files we dont have open.
        // TODO try to  see if we need this or can move it so its not executed unnessesary
        if (context.holder != null) {
          if (!leftResult.canAssign(rightResult)) {

            List<HaxeExpressionConversionFixer> fixers = HaxeExpressionConversionFixer.createStdTypeFixers(right, rightValue, leftValue);
            AnnotationBuilder builder = HaxeStandardAnnotation
              .typeMismatch(context.holder, right, rightValue.toStringWithoutConstant(), leftValue.toStringWithoutConstant())
              .withFix(new HaxeCastFixer(right, rightValue, leftValue));

            fixers.forEach(builder::withFix);
            builder.create();
          }
        }

        if (leftResult.isImmutable()) {
          context.addError(element, HaxeBundle.message("haxe.semantic.trying.to.change.an.immutable.value"));
        }

        return rightResult;
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeSwitchCaseCaptureVar captureVar) {
      HaxeResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(captureVar, resolver.getSpecialization(null));
      if (result.isHaxeClass()) {
        return result.getSpecificClassReference(result.getHaxeClass(), resolver).createHolder();
      }else if (result.isFunctionType()) {
        return result.getSpecificClassReference(result.getFunctionType(), resolver).createHolder();
      }
    }
    else if (element instanceof HaxeLocalVarDeclaration varDeclaration) {
      final HaxeComponentName name = varDeclaration.getComponentName();
      final HaxeVarInit init = varDeclaration.getVarInit();
      final HaxeTypeTag typeTag = varDeclaration.getTypeTag();
      final ResultHolder unknownResult = SpecificHaxeClassReference.getUnknown(element).createHolder();
      HaxeGenericResolver localResolver = new HaxeGenericResolver();
      localResolver.addAll(resolver);
      if(init != null) {
        // find any type parameters used in init expression as the return type might be of that type
        HaxeGenericResolver initResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(init.getExpression());
        localResolver.addAll(initResolver.withoutUnknowns());
      }
      final ResultHolder initResult = init != null
                                      ? handle(init, context, localResolver)
                                      : unknownResult;
      final ResultHolder typeTagResult = typeTag != null
                                         ? HaxeTypeResolver.getTypeFromTypeTag(typeTag, element)
                                         : unknownResult;

      ResultHolder result = typeTag != null ? typeTagResult : initResult;

      if (init == null && typeTag == null) {
        // search for usage to determine type
        return searchReferencesForType(varDeclaration.getComponentName(), context, resolver, null);
      }

      if (init != null && typeTag != null) {
        if (context.holder != null) {
          if (!typeTagResult.canAssign(initResult)) {
            context.addError(
              element,
              "Can't assign " + initResult + " to " + typeTagResult,
              new HaxeTypeTagChangeFixer(typeTag, initResult.getType()),
              new HaxeTypeTagRemoveFixer(typeTag)
            );
          }
        }
      }

      context.setLocal(name.getText(), result);

      return result;
    }

    if (element instanceof HaxeVarInit varInit) {
      final HaxeExpression expression = varInit.getExpression();
      if (expression == null) {
        return SpecificTypeReference.getInvalid(element).createHolder();
      }
      return handle(expression, context, resolver);
    }
    if (element instanceof HaxeRegularExpressionLiteral) {
      HaxeClass regexClass = HaxeResolveUtil.findClassByQName(getLiteralClassName(HaxeTokenTypes.REG_EXP), element);
      if (regexClass != null) {
        return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(regexClass.getModel(),element)).createHolder();
      }
    }
    if (element instanceof HaxeValueExpression valueExpression) {
      if (valueExpression.getSwitchStatement() != null){
        return handle(valueExpression.getSwitchStatement(), context, resolver);
      }
      if (valueExpression.getIfStatement() != null){
        return handle(valueExpression.getIfStatement(), context, resolver);
      }
      if (valueExpression.getTryStatement() != null){
        return handle(valueExpression.getTryStatement(), context, resolver);
      }
      if (valueExpression.getVarInit() != null){
        return handle(valueExpression.getVarInit(), context, resolver);
      }
      if (valueExpression.getExpression() != null){
        return handle(valueExpression.getExpression(), context, resolver);
      }
    }

    if (element instanceof HaxeReferenceExpression) {
      PsiElement[] children = element.getChildren();
      ResultHolder typeHolder = children.length == 0 ? SpecificTypeReference.getUnknown(element).createHolder() :  handle(children[0], context, resolver);
      boolean resolved = !typeHolder.getType().isUnknown();
      for (int n = 1; n < children.length; n++) {
        PsiElement child = children[n];
        SpecificTypeReference typeReference = typeHolder.getType();
        if (typeReference.isString() && typeReference.isConstant() && child.textMatches("code")) {
          String str = (String)typeReference.getConstant();
          typeHolder = SpecificTypeReference.getInt(element, (str != null && !str.isEmpty()) ? str.charAt(0) : -1).createHolder();
          if (str == null || str.length() != 1) {
            context.addError(element, "String must be a single UTF8 char");
          }
        } else {

          if (typeReference.isUnknown()) continue;

          // unwrap Null
          //TODO make util for unwrap/get underlying type of Null<T>? (or fix resolver ?)
          if (typeReference.isNullType()) {
            typeHolder = typeHolder.getClassType().getSpecifics()[0];
          }

          // TODO: Yo! Eric!!  This needs to get fixed.  The resolver is coming back as Dynamic, when it should be String

          // Grab the types out of the original resolver (so we don't modify it), and overwrite them
          // (by adding) with the class' resolver. That way, we get the combination of the two, and
          // any parameters provided/set in the class will override any from the calling context.
          HaxeGenericResolver localResolver = new HaxeGenericResolver();
          localResolver.addAll(resolver);

          SpecificHaxeClassReference classType = typeHolder.getClassType();
          if (null != classType) {
            localResolver.addAll(classType.getGenericResolver());
          }
          String accessName = child.getText();
          ResultHolder access = typeHolder.getType().access(accessName, context, localResolver);
          if (access == null) {
            resolved = false;

            if (children.length == 1) {
              context.addError(children[n], "Can't resolve '" + accessName + "' in " + typeHolder.getType(),
                               new HaxeCreateLocalVariableFixer(accessName, element));
            }
            else {
              context.addError(children[n], "Can't resolve '" + accessName + "' in " + typeHolder.getType(),
                               new HaxeCreateMethodFixer(accessName, element),
                               new HaxeCreateFieldFixer(accessName, element));
            }

          }
          if (access != null) typeHolder = access;
        }
      }

      // If we aren't walking the body, then we might not have seen the reference.  In that
      // case, the type is still unknown.  Let's see if the resolver can figure it out.
      if (!resolved) {
        PsiReference reference = element.getReference();
        if (reference != null) {
          PsiElement subelement = reference.resolve();
          if (subelement != element) {
            if (subelement instanceof HaxeReferenceExpression referenceExpression) {
              PsiElement resolve = referenceExpression.resolve();
              if (resolve != element)
                typeHolder = handleWithRecursionGuard(resolve, context, resolver);
            }
            if (subelement instanceof HaxeClass haxeClass) {

              HaxeClassModel model = haxeClass.getModel();
              HaxeClassReference classReference = new HaxeClassReference(model, element);

              if (haxeClass.isGeneric()) {
                @NotNull ResultHolder[] specifics = resolver.getSpecificsFor(classReference);
                typeHolder = SpecificHaxeClassReference.withGenerics(classReference, specifics).createHolder();
              }
              else {
                typeHolder = SpecificHaxeClassReference.withoutGenerics(classReference).createHolder();
              }

              // check if pure Class Reference
              if (reference instanceof HaxeReferenceExpressionImpl expression) {
                if (expression.isPureClassReferenceOf(haxeClass.getName())) {
                  // wrap in Class<> or Enum<>
                  SpecificHaxeClassReference originalClass = SpecificHaxeClassReference.withoutGenerics(model.getReference());
                  SpecificHaxeClassReference wrappedClass =
                    SpecificHaxeClassReference.getStdClass(haxeClass.isEnum() ? ENUM : CLASS, element,
                                                           new ResultHolder[]{new ResultHolder(originalClass)});
                  typeHolder = wrappedClass.createHolder();
                }
              }
            }
            else if (subelement instanceof HaxeFieldDeclaration fieldDeclaration) {
              HaxeVarInit init = fieldDeclaration.getVarInit();
              if (init != null) {
                HaxeExpression initExpression = init.getExpression();
                HaxeGenericResolver initResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(initExpression);
                typeHolder = HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)subelement, initResolver);
              }
              else {
                HaxeTypeTag tag = fieldDeclaration.getTypeTag();
                if (tag != null) {
                  typeHolder = HaxeTypeResolver.getTypeFromTypeTag(tag, fieldDeclaration);
                  HaxeClass  usedIn = PsiTreeUtil.getParentOfType((PsiElement)reference, HaxeClass.class);
                  HaxeClass containingClass = (HaxeClass)fieldDeclaration.getContainingClass();
                  if (usedIn != null && containingClass != null && usedIn != containingClass && containingClass.isGeneric()) {
                    HaxeGenericResolver inheritedClassResolver = createInheritedClassResolver(containingClass,usedIn, resolver);
                    HaxeGenericResolver resolverForContainingClass = inheritedClassResolver.getSpecialization(null).toGenericResolver(containingClass);
                    ResultHolder resolve = resolverForContainingClass.resolve(typeHolder);
                    if (!resolve.isUnknown())typeHolder = resolve;
                  }

                }
              }
            }
            else if (subelement instanceof HaxeMethodDeclaration methodDeclaration) {
              boolean isFromCallExpression = reference instanceof  HaxeCallExpression;
              SpecificFunctionReference type = methodDeclaration.getModel().getFunctionType(isFromCallExpression ? resolver : resolver.withoutAssignHint());
              if (!isFromCallExpression) {
                //  expression is referring to the method not calling it.
                //  assign hint should be used for substituting parameters instead of being used as return type
                type = resolver.substituteTypeParamsWithAssignHintTypes(type);
              }
              typeHolder = type.createHolder();
            }

            else if (subelement instanceof HaxeForStatement forStatement) {
              // key-value iterator is not relevant here as it will be resolved to HaxeIteratorkey  or HaxeIteratorValue
              final HaxeComponentName name = forStatement.getComponentName();
              // if element text matches  for loops  iterator  i guess we can consider it a match?
              if (name != null && element.textMatches(name)) {
                final HaxeIterable iterable = forStatement.getIterable();
                if (iterable != null) {
                  ResultHolder iterator = handle(iterable, context, resolver);
                  if (iterator.isClassType()) {
                    iterator = iterator.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference().createHolder();
                  }
                  // get specific from iterator as thats the type for our variable
                  ResultHolder[] specifics = iterator.getClassType().getSpecifics();
                  if (specifics.length > 0) {
                    typeHolder = specifics[0];
                  }
                }
              }
            }
            else if (subelement instanceof HaxeIteratorkey || subelement instanceof HaxeIteratorValue) {
              typeHolder = findIteratorType(element, subelement);
            }

            else if (subelement instanceof HaxeSwitchCaseCaptureVar caseCaptureVar) {
              HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(caseCaptureVar, HaxeSwitchStatement.class);
              if (switchStatement.getExpression() != null) {
                typeHolder = handle(switchStatement.getExpression(), context, resolver);
              }
            }

            else if (subelement instanceof HaxeSwitchCaseExpr caseExpr) {
              HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(caseExpr, HaxeSwitchStatement.class);
              if (switchStatement.getExpression() != null) {
                typeHolder = handle(switchStatement.getExpression(), context, resolver);
              }
            }

            else {
              // attempt to resolve subelement using default handle logic
              if (!(subelement instanceof PsiPackage)) {
                typeHolder = handleWithRecursionGuard(subelement, context, resolver);
              }
              if (typeHolder == null) {
                typeHolder = SpecificTypeReference.getUnknown(element).createHolder();
              }
            }
          }
        }
      }

      return (typeHolder != null) ? typeHolder : SpecificTypeReference.getDynamic(element).createHolder();
    }

    if (element instanceof HaxeCallExpression callExpression) {
      HaxeExpression callLeft = callExpression.getExpression();
      SpecificTypeReference functionType = handle(callLeft, context, resolver).getType();

      // @TODO: this should be innecessary when code is working right!
      if (functionType.isUnknown()) {
        if (callLeft instanceof HaxeReference) {
          PsiReference reference = callLeft.getReference();
          if (reference != null) {
            PsiElement subelement = reference.resolve();
            if (subelement instanceof HaxeMethod haxeMethod) {
              functionType = haxeMethod.getModel().getFunctionType(resolver);
            }
          }
        }
      }

      if (functionType.isUnknown()) {
        if(log.isDebugEnabled()) log.debug("Couldn't resolve " + callLeft.getText());
      }

      List<HaxeExpression> parameterExpressions = null;
      if (callExpression.getExpressionList() != null) {
        parameterExpressions = callExpression.getExpressionList().getExpressionList();
      } else {
        parameterExpressions = Collections.emptyList();
      }

      if (functionType instanceof  SpecificHaxeClassReference classReference && classReference.isTypeDef() ) {
        functionType = classReference.fullyResolveTypeDefReference();
      }
      if (functionType instanceof SpecificEnumValueReference enumValueConstructor) {
        // TODO, this probably should not be handled here, but its detected as a call expression


        SpecificHaxeClassReference enumClass = enumValueConstructor.enumClass;
        HaxeGenericResolver enumResolver = enumClass.getGenericResolver();
        SpecificFunctionReference constructor = enumValueConstructor.getConstructor();

        List<ResultHolder> list = parameterExpressions.stream()
          .map(expression -> HaxeExpressionEvaluator.evaluate(expression, new HaxeExpressionEvaluatorContext(expression), enumResolver).result)
          .toList();


        ResultHolder holder = enumClass.createHolder();
        SpecificHaxeClassReference type = holder.getClassType();
        @NotNull ResultHolder[] specifics = type.getSpecifics();
        // convert any parameter that matches argument of type TypeParameter into specifics for enum type
        HaxeGenericParam param = enumClass.getHaxeClass().getGenericParam();
        List<HaxeGenericParamModel> params = enumClass.getHaxeClassModel().getGenericParams();

        Map<String, List<ResultHolder>> genericsMap = new HashMap<>();
        params.forEach(g -> genericsMap.put(g.getName(), new ArrayList<>()));

        for (HaxeGenericParamModel model : params) {
          String genericName = model.getName();

          int parameterIndex = 0;
          List<Argument> arguments = constructor.getArguments();
          for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
            Argument argument = arguments.get(argumentIndex);
            if (parameterIndex < list.size()) {
              ResultHolder parameter = list.get(parameterIndex++);
              if (argument.getType().canAssign(parameter)) {
                if (argument.getType().getType() instanceof SpecificHaxeClassReference classReference ){
                  if (classReference.isTypeParameter() && genericName.equals(classReference.getClassName())) {
                    genericsMap.get(genericName).add(parameter);
                  } else {
                  if (argument.getType().isClassType()) {
                    HaxeGenericResolver parameterResolver = parameter.getClassType().getGenericResolver();
                    ResultHolder test = parameterResolver.resolve(genericName);
                    if (test != null && !test.isUnknown()) {
                      genericsMap.get(genericName).add(parameter);
                    }
                  }
                }
                }
              }
            }
          }
        }
        // unify all usage of generics
        for (int i = 0; i < params.size(); i++) {
          HaxeGenericParamModel g = params.get(i);
          String name = g.getName();
          List<ResultHolder> holders = genericsMap.get(name);
          ResultHolder unified = HaxeTypeUnifier.unifyHolders(holders, element);
          enumResolver.add(name, unified, ResolveSource.CLASS_TYPE_PARAMETER);
          specifics[i] = unified;
        }
        return holder;

      }
      if (functionType instanceof SpecificFunctionReference ftype) {

        ResultHolder returnType = ftype.getReturnType();

        HaxeGenericResolver functionResolver = new HaxeGenericResolver();
        functionResolver.addAll(resolver);
        HaxeGenericResolverUtil.appendCallExpressionGenericResolver(callExpression, functionResolver);

        ResultHolder resolved = functionResolver.resolveReturnType(returnType);
        if (resolved != null && !resolved.isUnknown()) {
          returnType = resolved;
        }
        if(returnType.isUnknown() || returnType.isDynamic() || returnType.isVoid()) {
          return returnType.duplicate();
        }

        if(returnType.isFunctionType()){
          return returnType.getFunctionType().createHolder();
        }

        if(returnType.isClassType() || returnType.isEnumValueType()) {
          return returnType.withOrigin(ftype.context);
        }

      }

      if (functionType.isDynamic()) {
        for (HaxeExpression expression : parameterExpressions) {
          handle(expression, context, resolver);
        }

        return functionType.withoutConstantValue().createHolder();
      }

      // @TODO: resolve the function type return type
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLiteralExpression) {
      return handle(element.getFirstChild(), context, resolver);
    }

    if (element instanceof HaxeStringLiteralExpression) {
      // @TODO: check if it has string interpolation inside, in that case text is not constant
      return SpecificHaxeClassReference.primitive(
        "String",
        element,
        HaxeStringUtil.unescapeString(element.getText())
      ).createHolder();
    }

    if (element instanceof HaxeExpressionList expressionList) {
      ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
      for (HaxeExpression expression : expressionList.getExpressionList()) {
        references.add(handle(expression, context, resolver));
      }
      return HaxeTypeUnifier.unifyHolders(references, element);
    }

    if (element instanceof HaxeMapLiteral mapLiteral) {
      HaxeMapInitializerExpressionList listElement = mapLiteral.getMapInitializerExpressionList();
      List<HaxeExpression> initializers = new ArrayList<>();

      // In maps, comprehensions don't have expression lists, but they do have one single initializer.
      if (null == listElement) {
        HaxeMapInitializerForStatement forStatement = mapLiteral.getMapInitializerForStatement();
        HaxeMapInitializerWhileStatement whileStatement = mapLiteral.getMapInitializerWhileStatement();
        HaxeExpression fatArrow = null;
        while (null != forStatement || null != whileStatement) {
          if (null != forStatement) {
            fatArrow = forStatement.getMapInitializerExpression();
            whileStatement = forStatement.getMapInitializerWhileStatement();
            forStatement = forStatement.getMapInitializerForStatement();
          } else {
            fatArrow = whileStatement.getMapInitializer();
            forStatement = whileStatement.getMapInitializerForStatement();
            whileStatement = whileStatement.getMapInitializerWhileStatement();
          }
        }
        if (null != fatArrow) {
          initializers.add(fatArrow);
        } else {
          log.error("Didn't find an initializer in a map comprehension: " + element.toString(),
                    new HaxeDebugUtil.InvalidValueException(element.toString() + '\n' + HaxeDebugUtil.elementLocation(element)));
        }
      } else {
        initializers.addAll(listElement.getMapInitializerExpressionList());
      }

      ArrayList<SpecificTypeReference> keyReferences = new ArrayList<>(initializers.size());
      ArrayList<SpecificTypeReference> valueReferences = new ArrayList<>(initializers.size());
      HaxeGenericResolver resolverWithoutHint = resolver.withoutAssignHint();
      for (HaxeExpression ex : initializers) {
        HaxeMapInitializerExpression fatArrow = (HaxeMapInitializerExpression)ex;

        SpecificTypeReference keyType = handle(fatArrow.getFirstChild(), context, resolverWithoutHint).getType();
        if (keyType instanceof SpecificEnumValueReference enumValueReference) {
          keyType = enumValueReference.getEnumClass();
        }
        keyReferences.add(keyType);
        SpecificTypeReference valueType = handle(fatArrow.getLastChild(), context, resolverWithoutHint).getType();
        if (valueType instanceof SpecificEnumValueReference enumValueReference) {
          valueType = enumValueReference.getEnumClass();
        }
        valueReferences.add(valueType);
      }

      // XXX: Maybe track and add constants to the type references, like arrays do??
      //      That has implications on how they're displayed (e.g. not as key=>value,
      //      but as separate arrays).
      ResultHolder keyTypeHolder = HaxeTypeUnifier.unify(keyReferences, element).withoutConstantValue().createHolder();
      ResultHolder valueTypeHolder = HaxeTypeUnifier.unify(valueReferences, element).withoutConstantValue().createHolder();

      SpecificTypeReference result = SpecificHaxeClassReference.createMap(keyTypeHolder, valueTypeHolder, element);
      ResultHolder holder = result.createHolder();
      return holder;
    } // end HaxeMapLiteral

    if (element instanceof HaxeArrayLiteral arrayLiteral) {
      HaxeExpressionList list = arrayLiteral.getExpressionList();

      // Check if it's a comprehension.
      if (list != null) {
        final List<HaxeExpression> expressionList = list.getExpressionList();
        if (expressionList.isEmpty()) {
          final PsiElement child = list.getFirstChild();
          if ((child instanceof HaxeForStatement) || (child instanceof HaxeWhileStatement)) {
            return SpecificTypeReference.createArray(handle(child, context, resolver), element).createHolder();
          }
        }
      }

      ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
      ArrayList<Object> constants = new ArrayList<Object>();
      boolean allConstants = true;
      if (list != null) {
        for (HaxeExpression expression : list.getExpressionList()) {
          // dropping AssignHint as we are in an array so field type will include the array part.
          SpecificTypeReference type = handle(expression, context, resolver.withoutAssignHint()).getType();
          if (!type.isConstant()) {
            allConstants = false;
          } else {
            constants.add(type.getConstant());
          }
          // Convert enum Value types to Enum class  (you cant have an Array of EnumValue types)
          if (type instanceof  SpecificEnumValueReference enumValueReference) {
            type = enumValueReference.getEnumClass();
          }
          references.add(type);
        }
      }
      // an attempt at suggesting what to unify  types into (useful for when typeTag is an anonymous structure as those would never be used in normal unify)
      SpecificTypeReference suggestedType = null;
      ResultHolder  typeTagType = findInitTypeForUnify(element);
      if (typeTagType!= null) {
        // we expect Array<T> or collection type with type parameter (might not work properly if type is implicit cast)
        if (typeTagType.getClassType() != null) {
          @NotNull ResultHolder[] specifics = typeTagType.getClassType().getSpecifics();
          if (specifics.length == 1) {
            suggestedType = specifics[0].getType();
          }
        }
      }
      // empty expression with type tag (var x:Array<T> = []), no need to look for usage, use typetag
      if (references.isEmpty() && suggestedType != null && !suggestedType.isUnknown()) {
        return typeTagType;
      } else {
        ResultHolder elementTypeHolder = references.isEmpty()
                                         ? SpecificTypeReference.getUnknown(element).createHolder()
                                         : HaxeTypeUnifier.unify(references, element, suggestedType).withoutConstantValue().createHolder();

        SpecificTypeReference result = SpecificHaxeClassReference.createArray(elementTypeHolder, element);
        if (allConstants) result = result.withConstantValue(constants);
        ResultHolder holder = result.createHolder();

        // try to resolve typeParameter when we got empty literal array with declaration without typeTag
        if (elementTypeHolder.isUnknown()) {
          // note to avoid recursive loop we only  do this check if its part of a varInit and not part of any expression,
          // it would not make sense trying to look it up in a callExpression etc because then its type should be defined in the parameter list.
          if (element.getParent() instanceof HaxeVarInit) {
            HaxePsiField declaringField =
              UsefulPsiTreeUtil.findParentOfTypeButStopIfTypeIs(element, HaxePsiField.class, HaxeCallExpression.class);
            if (declaringField != null) {
              ResultHolder searchResult = searchReferencesForTypeParameters(declaringField, context, resolver, holder);
              if (!searchResult.isUnknown()) holder = searchResult;
            }
          }
        }
      return holder;
      }
    }

    if (element instanceof HaxePsiToken psiToken) {
      IElementType type = psiToken.getTokenType();

      if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
        return SpecificHaxeClassReference.primitive("Int", element, Long.decode(element.getText())).createHolder();
      } else if (type == HaxeTokenTypes.LITFLOAT) {
        Float value = Float.valueOf(element.getText());
        return SpecificHaxeClassReference.primitive("Float", element, Double.parseDouble(element.getText()))
          .withConstantValue(value)
          .createHolder();
      } else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
        Boolean value = type == HaxeTokenTypes.KTRUE;
        return SpecificHaxeClassReference.primitive("Bool", element, type == HaxeTokenTypes.KTRUE)
          .withConstantValue(value)
          .createHolder();
      } else if (type == HaxeTokenTypes.KNULL) {
        return SpecificHaxeClassReference.primitive("Dynamic", element, HaxeNull.instance).createHolder();
      } else {
        if(log.isDebugEnabled())log.debug("Unhandled token type: " + type);
        return SpecificHaxeClassReference.getDynamic(element).createHolder();
      }
    }

    if (element instanceof HaxeSuperExpression) {
      /*
      log.debug("-------------------------");
      final HaxeExpressionList list = HaxePsiUtils.getChildWithText(element, HaxeExpressionList.class);
      log.debug(element);
      log.debug(list);
      final List<HaxeExpression> parameters = (list != null) ? list.getExpressionList() : Collections.<HaxeExpression>emptyList();
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeMethodModel.fromPsi(element), HaxeMethodModel.class);
      if (method == null) {
        context.addError(element, "Not in a method");
      }
      if (method != null) {
        final HaxeMethodModel parentMethod = method.getParentMethod();
        if (parentMethod == null) {
          context.addError(element, "Calling super without parent constructor");
        } else {
          log.debug(element);
          log.debug(parentMethod.getFunctionType());
          log.debug(parameters);
          checkParameters(element, parentMethod.getFunctionType(), parameters, context);
          //log.debug(method);
          //log.debug(parentMethod);
        }
      }
      return SpecificHaxeClassReference.getVoid(element);
      */
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeBaseMemberModel.fromPsi(element), HaxeMethodModel.class);
      final HaxeMethodModel parentMethod = (method != null) ? method.getParentMethod(resolver) : null;
      if (parentMethod != null) {
        return parentMethod.getFunctionType(resolver).createHolder();
      }
      context.addError(element, "Calling super without parent constructor");
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeIteratorExpression iteratorExpression) {
      final List<HaxeExpression> list = iteratorExpression.getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context, resolver).getType();
        final SpecificTypeReference right = handle(list.get(1), context, resolver).getType();
        Object constant = null;
        if (left.isConstant() && right.isConstant()) {
          constant = new HaxeRange(
            HaxeTypeUtils.getIntValue(left.getConstant()),
            HaxeTypeUtils.getIntValue(right.getConstant())
          );
        }
        return SpecificHaxeClassReference.getIterator(SpecificHaxeClassReference.getInt(element)).withConstantValue(constant)
          .createHolder();
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeArrayAccessExpression arrayAccessExpression) {
      final List<HaxeExpression> list = arrayAccessExpression.getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context, resolver).getType();
        final SpecificTypeReference right = handle(list.get(1), context, resolver).getType();
        if (left.isArray()) {
          Object constant = null;
          if (left.isConstant()) {
            if (left.getConstant() instanceof List array) {
              //List array = (List)left.getConstant();
              // TODO got class cast exception here due to constant being "HaxeAbstractClassDeclarationImpl
              //  possible expression causing issue: ("this[x + 1]  in  abstractType(Array<Float>)" ?)

              final HaxeRange constraint = right.getRangeConstraint();
              HaxeRange arrayBounds = new HaxeRange(0, array.size());
              if (right.isConstant()) {
                final int index = HaxeTypeUtils.getIntValue(right.getConstant());
                if (arrayBounds.contains(index)) {
                  constant = array.get(index);
                }
                else {
                  context.addWarning(element, "Out of bounds " + index + " not inside " + arrayBounds);
                }
              }
              else if (constraint != null) {
                if (!arrayBounds.contains(constraint)) {
                  context.addWarning(element, "Out of bounds " + constraint + " not inside " + arrayBounds);
                }
              }
            }
          }
          ResultHolder arrayType = left.getArrayElementType().getType().withConstantValue(constant).createHolder();
          ResultHolder resolved = resolver.resolve(arrayType);
          return resolved.isUnknown() ? arrayType : resolved;
        }
        //if not native array, look up ArrayAccessGetter method and use result
        if(left instanceof SpecificHaxeClassReference classReference) {
          // make sure we fully resolve and unwrap any nulls and typedefs before searching for accessors
          SpecificTypeReference reference = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
          if (reference instanceof SpecificHaxeClassReference fullyResolved){
            classReference = fullyResolved;
          }

          HaxeClass haxeClass = classReference.getHaxeClass();
          if (haxeClass != null) {
            HaxeNamedComponent getter = haxeClass.findArrayAccessGetter(resolver);
            if (getter instanceof HaxeMethodDeclaration methodDeclaration) {
              HaxeMethodModel methodModel = methodDeclaration.getModel();
              HaxeGenericResolver localResolver = classReference.getGenericResolver();
              HaxeGenericResolver methodResolver = methodModel.getGenericResolver(localResolver);
              localResolver.addAll(methodResolver);// apply constraints from methodSignature (if any)
              ResultHolder returnType = methodModel.getReturnType(localResolver);
              if (returnType.getType().isNullType()) localResolver.resolve(returnType);
              if (returnType != null) return returnType;
            }
            // hack to work around external ArrayAccess interface, interface that has no methods but tells compiler that implementing class has array access
            else if (getter instanceof HaxeExternInterfaceDeclaration interfaceDeclaration) {
              HaxeGenericSpecialization leftResolver = classReference.getGenericResolver().getSpecialization(getter);
              HaxeResolveResult resolvedInterface = HaxeResolveUtil.getHaxeClassResolveResult(interfaceDeclaration, leftResolver);
              ResultHolder type = resolvedInterface.getGenericResolver().resolve("T");
              if (type != null) return type;
            }
          }
        }
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
    if (element instanceof HaxeLocalFunctionDeclaration functionDeclaration) {
      return functionDeclaration.getModel().getFunctionType(resolver).createHolder();
    }
    if (element instanceof HaxeFunctionLiteral function) {
      HaxeParameterList params = function.getParameterList(); // TODO mlo: get expected type to use if signature/parameters are without types
      if (params == null) {
        return SpecificHaxeClassReference.getInvalid(function).createHolder();
      }
      LinkedList<Argument> arguments = new LinkedList<>();
      ResultHolder returnType = null;
      context.beginScope();
      try {
        if (params instanceof HaxeOpenParameterList openParamList) {
          // Arrow function with a single, unparenthesized, parameter.

          // TODO: Infer the type from first usage in the function body.
          ResultHolder argumentType = SpecificTypeReference.getUnknown(function).createHolder();
          String argumentName = openParamList.getComponentName().getName();
          context.setLocal(argumentName, argumentType);
          // TODO check if rest param?
          arguments.add(new Argument(0, false, false, argumentType, argumentName));
        } else {
          List<HaxeParameter> list = params.getParameterList();
          for (int i = 0; i < list.size(); i++) {
            HaxeParameter parameter = list.get(i);
            //ResultHolder argumentType = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), function);
            ResultHolder argumentType = handleWithRecursionGuard(parameter, context, resolver);
            if (argumentType == null) argumentType = SpecificTypeReference.getUnknown(parameter).createHolder();
            context.setLocal(parameter.getName(), argumentType);
            // TODO check if rest param?
            boolean optional = parameter.getOptionalMark() != null || parameter.getVarInit() != null;
            arguments.add(new Argument(i, optional, false, argumentType, parameter.getName()));
          } // TODO: Add Void if list.size() == 0
        }
        context.addLambda(context.createChild(function.getLastChild()));
        HaxeTypeTag tag = (function.getTypeTag());
        if (null != tag) {
          returnType = HaxeTypeResolver.getTypeFromTypeTag(tag, function);
        } else {
          // If there was no type tag on the function, then we try to infer the value:
          // If there is a block to this method, then return the type of the block.  (See PsiBlockStatement above.)
          // If there is not a block, but there is an expression, then return the type of that expression.
          // If there is not a block, but there is a statement, then return the type of that statement.
          HaxeBlockStatement block = function.getBlockStatement();
          if (null != block) {
            //// note : as we enter a block we are leaving the scope where we could use the assignHint directly
            HaxeExpressionEvaluatorContext functionBlockContext = new HaxeExpressionEvaluatorContext(element);
            ResultHolder handled = handle(block, functionBlockContext, resolver.withoutAssignHint());

            if (!functionBlockContext.getReturnValues().isEmpty()) {
              returnType = HaxeTypeUnifier.unifyHolders(functionBlockContext.getReturnValues(), element);
            }else  if (block.getExpressionList().size() == 1){
              returnType = handled;
            }else {
              returnType = SpecificHaxeClassReference.getVoid(element).createHolder();
            }
          } else if (null != function.getExpression()) {
            returnType = handle(function.getExpression(), context, resolver);
          } else {
            // Only one of these can be non-null at a time.
            PsiElement possibleStatements[] = {function.getDoWhileStatement(), function.getForStatement(), function.getIfStatement(),
              function.getReturnStatement(), function.getThrowStatement(), function.getWhileStatement()};
            for (PsiElement statement : possibleStatements) {
              if (null != statement) {
                returnType = handle(statement, context, resolver);
                break;
              }
            }
          }
        }
      }
      finally {
        context.endScope();
      }
      return new SpecificFunctionReference(arguments, returnType, null, function, function).createHolder();
    }

    if (element instanceof HaxeIfStatement ifStatement) {
      SpecificTypeReference guardExpr = handle(ifStatement.getGuard(), context, resolver).getType();
      HaxeGuardedStatement guardedStatement = ifStatement.getGuardedStatement();
      HaxeElseStatement elseStatement = ifStatement.getElseStatement();

      PsiElement eTrue = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(guardedStatement);
      PsiElement eFalse = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(elseStatement);

      SpecificTypeReference tTrue = null;
      SpecificTypeReference tFalse = null;
      if (eTrue != null) tTrue = handle(eTrue, context, resolver).getType();
      if (eFalse != null) tFalse = handle(eFalse, context, resolver).getType();
      if (guardExpr.isConstant()) {
        if (guardExpr.getConstantAsBool()) {
          if (tFalse != null) {
            context.addUnreachable(eFalse);
          }
        } else {
          if (tTrue != null) {
            context.addUnreachable(eTrue);
          }
        }
      }

      // No 'else' clause means the if results in a Void type.
      if (null == tFalse) tFalse = SpecificHaxeClassReference.getVoid(element);

      return HaxeTypeUnifier.unify(tTrue, tFalse, element).createHolder();
    }

    if (element instanceof HaxeGuard haxeGuard) {  // Guard expression for if statement or switch case.
      HaxeExpression guardExpression = haxeGuard.getExpression();
      SpecificTypeReference expr = handle(guardExpression, context, resolver).getType();
      if (!SpecificTypeReference.getBool(element).canAssign(expr)) {
        context.addError(
          guardExpression,
          "If expr " + expr + " should be bool",
          new HaxeCastFixer(guardExpression, expr, SpecificHaxeClassReference.getBool(element))
        );
      }

      if (expr.isConstant()) {
        context.addWarning(guardExpression, "If expression constant");
      }
    }

    if (element instanceof HaxeParenthesizedExpression) {
      return handle(element.getChildren()[0], context, resolver);
    }

    if (element instanceof HaxeTernaryExpression ternaryExpression) {
      HaxeExpression[] list = ternaryExpression.getExpressionList().toArray(new HaxeExpression[0]);
      return HaxeTypeUnifier.unify(handle(list[1], context, resolver).getType(), handle(list[2], context, resolver).getType(), element).createHolder();
    }

    if (element instanceof HaxePrefixExpression prefixExpression) {
      HaxeExpression expression = prefixExpression.getExpression();
      ResultHolder typeHolder = handle(expression, context, resolver);
      SpecificTypeReference type = typeHolder.getType();
      if (type.getConstant() != null) {
        String operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
        if (operatorText != "") {
          return type.withConstantValue(HaxeTypeUtils.applyUnaryOperator(type.getConstant(), operatorText)).createHolder();
        }
      }
      return typeHolder;
    }
    if (element instanceof HaxeIsTypeExpression) {
      return SpecificHaxeClassReference.primitive("Bool", element, null).createHolder();
    }
    //check if common parent before checking all accepted variants (note should not include HaxeAssignExpression, HaxeIteratorExpression etc)
    if (element instanceof HaxeBinaryExpression) {
      if (
        (element instanceof HaxeAdditiveExpression) ||
        (element instanceof HaxeModuloExpression) ||
        (element instanceof HaxeBitwiseExpression) ||
        (element instanceof HaxeShiftExpression) ||
        (element instanceof HaxeLogicAndExpression) ||
        (element instanceof HaxeLogicOrExpression) ||
        (element instanceof HaxeCompareExpression) ||
        (element instanceof HaxeCoalescingExpression) ||
        (element instanceof HaxeMultiplicativeExpression)
      ) {
        PsiElement[] children = element.getChildren();
        String operatorText;
        if (children.length == 3) {
          operatorText = children[1].getText();
          SpecificTypeReference left = handle(children[0], context, resolver).getType();
          SpecificTypeReference right = handle(children[2], context, resolver).getType();
          left = resolveAnyTypeDefs(left);
          right = resolveAnyTypeDefs(right);
          return HaxeOperatorResolver.getBinaryOperatorResult(element, left, right, operatorText, context).createHolder();
        }
        else {
          operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
          SpecificTypeReference left = handle(children[0], context, resolver).getType();
          SpecificTypeReference right = handle(children[1], context, resolver).getType();
          left = resolveAnyTypeDefs(left);
          right = resolveAnyTypeDefs(right);
          return HaxeOperatorResolver.getBinaryOperatorResult(element, left, right, operatorText, context).createHolder();
        }
      }
    }
    if (element instanceof HaxeTypeCheckExpr typeCheckExpr) {
      PsiElement[] children = element.getChildren();
      if (children.length == 2) {
        SpecificTypeReference statementType = handle(children[0], context, resolver).getType();
        SpecificTypeReference assertedType = SpecificTypeReference.getUnknown(children[1]);
        if (children[1] instanceof HaxeTypeOrAnonymous) {
          HaxeTypeOrAnonymous toa = typeCheckExpr.getTypeOrAnonymous();
          if (toa != null ) {
            assertedType = HaxeTypeResolver.getTypeFromTypeOrAnonymous(toa).getType();
          }
        }
        // When we have proper unification (not failing to dynamic), then we should be checking if the
        // values unify.
        //SpecificTypeReference unified = HaxeTypeUnifier.unify(statementType, assertedType, element);
        //if (!unified.canAssign(statementType)) {
        if (!assertedType.canAssign(statementType)) {
          context.addError(element, "Statement of type '" + statementType.getElementContext().getText() + "' does not unify with asserted type '" + assertedType.getElementContext().getText() + ".'");
          // TODO: Develop some fixers.
          // annotation.registerFix(new HaxeCreateLocalVariableFixer(accessName, element));
        }

        return statementType.createHolder();
      }
    }

    if (element instanceof AbstractHaxeNamedComponent namedComponent) {
      return HaxeTypeResolver.getFieldOrMethodReturnType(namedComponent, resolver);
    }

    if(log.isDebugEnabled()) log.debug("Unhandled " + element.getClass());
    return SpecificHaxeClassReference.getUnknown(element).createHolder();
  }

  @Nullable
  public static ResultHolder findIteratorType(PsiElement reference, PsiElement iteratorElement) {
    HaxeForStatement forStatement = PsiTreeUtil.getParentOfType(iteratorElement, HaxeForStatement.class);
    HaxeGenericResolver forResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(forStatement);

    HaxeIterable iterable = forStatement.getIterable();
    var keyValueIteratorType = HaxeTypeResolver.getPsiElementType(iterable, reference, forResolver);

    var iteratorType = keyValueIteratorType.getClassType();
    if (iteratorType.isTypeDef()) {
      SpecificTypeReference type = iteratorType.fullyResolveTypeDefReference();
      if (type instanceof SpecificHaxeClassReference  classReference) {
        iteratorType = classReference;
      }
    }
    var iteratorTypeResolver = iteratorType.getGenericResolver();

    HaxeClassModel classModel = iteratorType.getHaxeClassModel();
    if (classModel == null) return null;
    // NOTE if "String" we need to add iterator types manually as  std string class does not have this method
    if (iteratorType.isString()) {
      if (iteratorElement instanceof HaxeIteratorkey ) {
        return SpecificTypeReference.getInt(iteratorElement).createHolder();
      }else  if (iteratorElement instanceof  HaxeIteratorValue){
        return SpecificTypeReference.getString(iteratorElement).createHolder();
      }
    }

    HaxeMethodModel iteratorReturnType = (HaxeMethodModel)classModel.getMember("next", iteratorTypeResolver);
    if (iteratorReturnType == null) return null;

    HaxeGenericResolver nextResolver = iteratorReturnType.getGenericResolver(null);
    nextResolver.addAll(iteratorTypeResolver);

    ResultHolder returnType = iteratorReturnType.getReturnType(nextResolver);
    SpecificHaxeClassReference type = returnType.getClassType();
    HaxeGenericResolver genericResolver = type.getGenericResolver();

    if (keyValueIteratorType.getClassType()!= null) {
      genericResolver.addAll(keyValueIteratorType.getClassType().getGenericResolver());
    }

    if (iteratorElement instanceof HaxeIteratorkey ) {
      return type.getHaxeClassModel().getMember("key", null).getResultType(genericResolver);
    }else  if (iteratorElement instanceof  HaxeIteratorValue){
      return type.getHaxeClassModel().getMember("value", null).getResultType(genericResolver);
    }
    return null;
  }

  private static boolean isUntypedReturn(HaxeReturnStatement statement) {
    PsiElement child = statement.getFirstChild();
    while(child != null) {
      if (child instanceof HaxePsiToken psiToken) {
        if (psiToken.getTokenType() == KUNTYPED) return true;
      }
      child = child.getNextSibling();
    }
    return false;
  }

  @NotNull
  public static ResultHolder searchReferencesForType(final HaxeComponentName componentName,
                                                     final HaxeExpressionEvaluatorContext context,
                                                     final HaxeGenericResolver resolver,
                                                     @Nullable final PsiElement searchScope
  ) {
    // search is slow so we can probably save some unnecessary searches when analyzing code
    List<PsiReference> references =
      CachedValuesManager.getProjectPsiDependentCache(componentName, (c) -> cachedSearch(c, searchScope))
        .getValue();


      for (PsiReference reference : references) {
        ResultHolder possibleType = checkSearchResult(context, resolver, reference);
        if (possibleType != null) return possibleType;
      }

    return  SpecificHaxeClassReference.getUnknown(componentName).createHolder();
  }
  @NotNull
  public static CachedValueProvider.Result<List<PsiReference>> cachedSearch(final HaxeComponentName componentName, @Nullable final PsiElement searchScope) {
    PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(componentName.getProject());
    SearchScope scope = searchScope != null ? new LocalSearchScope(searchScope) :  searchHelper.getCodeUsageScope(componentName);

    int offset = componentName.getIdentifier().getTextRange().getEndOffset();
    List<PsiReference> references = new ArrayList<>(ReferencesSearch.search(componentName, scope).findAll()).stream()
      .sorted((r1, r2) -> {
        int i1 = getDistance(r1, offset);
        int i2 = getDistance(r2, offset);
        return i1 - i2;
      }).toList();
    return CachedValueProvider.Result.create(references, ModificationTracker.EVER_CHANGED, ProjectRootModificationTracker.getInstance(componentName.getProject()));
  }

  @Nullable
  private static ResultHolder checkSearchResult(HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver, PsiReference reference) {
    if (reference instanceof HaxeExpression expression) {
      if (expression.getParent() instanceof HaxeAssignExpression assignExpression) {
        HaxeExpression rightExpression = assignExpression.getRightExpression();
        ResultHolder result = handle(rightExpression, context, resolver);
        if (!result.isUnknown()) {
          return result;
        }
        HaxeExpression leftExpression = assignExpression.getLeftExpression();
        if (leftExpression instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement resolve = referenceExpression.resolve();
          if (resolve instanceof HaxePsiField psiField) {
            HaxeTypeTag tag = psiField.getTypeTag();
            if (tag != null) {
              ResultHolder holder = HaxeTypeResolver.getTypeFromTypeTag(tag, resolve);
              if (!holder.isUnknown()) {
                return holder;
              }
            }
          }
        }
      }
    }
    return null;
  }

  @NotNull
  public static ResultHolder searchReferencesForTypeParameters(final HaxePsiField field,
                                                               final HaxeExpressionEvaluatorContext context,
                                                               final HaxeGenericResolver resolver, ResultHolder resultHolder) {
    resultHolder = resultHolder.duplicate();
    HaxeComponentName componentName = field.getComponentName();
    SpecificHaxeClassReference type = resultHolder.getClassType();
    SpecificHaxeClassReference classType = type;

    HaxeGenericResolver classResolver = classType.getGenericResolver();
    PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(componentName.getProject());
    SearchScope useScope = searchHelper.getCodeUsageScope(componentName);


    int offset = componentName.getIdentifier().getTextRange().getEndOffset();
    List<PsiReference> references = ProgressManager.getInstance().computeInNonCancelableSection(() -> {
      return new ArrayList<>(ReferencesSearch.search(componentName, useScope).findAll()).stream()
        .sorted((r1, r2) -> {
          int i1 = getDistance(r1, offset);
          int i2 = getDistance(r2, offset);
          return  i1 -i2;
        } ).toList();
    });
    for (PsiReference reference : references) {
      if (reference instanceof HaxeExpression expression) {
        if (expression.getParent() instanceof HaxeAssignExpression assignExpression) {
          HaxeExpression rightExpression = assignExpression.getRightExpression();
          ResultHolder result = handle(rightExpression, context, resolver);
          if (!result.isUnknown() && result.getType().isSameType(resultHolder.getType())) {
            HaxeGenericResolver resultResolver = result.getClassType().getGenericResolver();
            HaxeGenericResolver resultResolverWithoutUnknowns = resultResolver.withoutUnknowns();
            // check that assigned value does not contain any unknown typeParameters (ex. someArrVar = [])
            if (resultResolver.names().length == resultResolverWithoutUnknowns.names().length) {
              return result;
            }
          }
        }
        if (expression.getParent() instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement resolved = referenceExpression.resolve();
          if (resolved instanceof HaxeMethodDeclaration methodDeclaration
              && referenceExpression.getParent() instanceof HaxeCallExpression callExpression) {

            @NotNull String[] specificNames = classResolver.names();
            HaxeMethodModel methodModel = methodDeclaration.getModel();
            HaxeGenericResolver methodResolver = methodModel.getGenericResolver(null);
            @NotNull String[] methodSpecificNames = methodResolver.names();

            List<String> specificsForClass = Arrays.asList(specificNames);
            specificsForClass.removeAll(Arrays.asList(methodSpecificNames));
            // make sure we are using class level typeParameters (and not method level)
            if (methodModel.getGenericParams().isEmpty()) {
              HaxeCallExpressionList list = callExpression.getExpressionList();
              if (list != null) {
                List<HaxeExpression> arguments = list.getExpressionList();
                List<HaxeParameterModel> parameters = methodDeclaration.getModel().getParameters();
                //TODO This really need to be cleaned up and is one big messy hack at the moment

                // correct way to solve it.
                //- find method parameters that are typeParameters from class (not from method)
                // - try to match parameters and arguments and create map with typeParameters to resolved argument type
                // update type specifics  with map



                HaxeClassModel classModel = classType.getHaxeClassModel();
                if (classModel == null) continue;
                List<HaxeGenericParamModel> params = classModel.getGenericParams();
                @NotNull ResultHolder[] specifics = type.getSpecifics();
                Map<String, Integer>specificsMap = new HashMap<>();
                for (int i = 0; i < specifics.length; i++) {
                  HaxeGenericParamModel model = params.get(i);
                  ResultHolder specific = specifics[i];
                  if (specific.getType() instanceof  SpecificHaxeClassReference classReference) {
                    if (classReference.isUnknown() || classReference.isTypeParameter()) {
                      specificsMap.put(model.getName(), i);
                    }
                  }
                }

                Set<String> genericNames = specificsMap.keySet();

                int inputCount = Math.min(parameters.size(), arguments.size());
                for (int i = 0; i<inputCount; i++) {
                  SpecificTypeReference paramType = parameters.get(i).getType().getType();
                  if (paramType instanceof SpecificHaxeClassReference classReference && classReference.isTypeParameter()) {
                    String name = classReference.getClassName();

                    if (genericNames.contains(name)) {
                      Integer index = specificsMap.get(name);
                        if (specifics[index].isUnknown()) {
                          ResultHolder handle = handle(arguments.get(i), context, resolver);
                          if (specifics[index].isUnknown()) {
                            specifics[index] = handle;
                          }else {
                            ResultHolder unified = HaxeTypeUnifier.unify(handle, specifics[index]);
                            specifics[index] = unified;
                          }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        if (expression.getParent() instanceof HaxeArrayAccessExpression arrayAccessExpression) {
          // try to find setter first if that fails try getter
          if (classType.getHaxeClass() != null) { // need to check if Haxe class exists as it will be null when SDK is missing
            HaxeNamedComponent arrayAccessSetter = classType.getHaxeClass().findArrayAccessSetter(resolver);
            if (arrayAccessSetter instanceof HaxeMethodDeclaration methodDeclaration) {
              HaxeMethodModel methodModel = methodDeclaration.getModel();
              // make sure we are using class level typeParameters (and not method level)
              if (methodModel.getGenericParams().isEmpty()) {
                List<HaxeParameterModel> parameters = methodModel.getParameters();

                HaxeTypeTag keyParamPsi = parameters.get(0).getTypeTagPsi();
                HaxeTypeTag valueParamPsi = parameters.get(1).getTypeTagPsi();


                @NotNull String[] specificNames = classResolver.names();
                for (int i = 0; i < specificNames.length; i++) {
                  String keyPsiName = keyParamPsi.getTypeOrAnonymous().getType().getText();
                  // key
                  if (keyPsiName.equals(specificNames[i])) {
                    HaxeExpression keyExpression = arrayAccessExpression.getExpressionList().get(1);
                    ResultHolder handle = handle(keyExpression, context, resolver);
                    if (type.getSpecifics()[i].isUnknown()) {
                      type.getSpecifics()[i] = handle;
                    }
                    else {
                      ResultHolder unified = HaxeTypeUnifier.unify(handle, type.getSpecifics()[i]);
                      type.getSpecifics()[i] = unified;
                    }
                  }
                  // value
                  if (arrayAccessExpression.getParent() instanceof HaxeBinaryExpression binaryExpression) {
                    String valuePsiName = valueParamPsi.getTypeOrAnonymous().getType().getText();
                    if (valuePsiName.equals(specificNames[i])) {
                      HaxeExpression keyExpression = binaryExpression.getExpressionList().get(1);
                      ResultHolder handle = handle(keyExpression, context, resolver);
                      if (type.getSpecifics()[i].isUnknown()) {
                        type.getSpecifics()[i] = handle;
                      }
                      else {
                        ResultHolder unified = HaxeTypeUnifier.unify(handle, type.getSpecifics()[i]);
                        type.getSpecifics()[i] = unified;
                      }
                    }
                  }
                }
              }
            }
            else {
              HaxeNamedComponent arrayAccessGetter = classType.getHaxeClass().findArrayAccessGetter(resolver);
              if (arrayAccessGetter instanceof HaxeMethodDeclaration methodDeclaration) {
                HaxeMethodModel methodModel = methodDeclaration.getModel();
                // make sure we are using class level typeParameters (and not method level)
                if (methodModel.getGenericParams().isEmpty()) {
                  List<HaxeParameterModel> parameters = methodModel.getParameters();
                  HaxeParameterModel keyParameter = parameters.get(0);
                  HaxeTypeTag keyParamPsi = keyParameter.getTypeTagPsi();

                  @NotNull String[] specificNames = classResolver.names();
                  for (int i = 0; i < specificNames.length; i++) {
                    String keyPsiName = keyParamPsi.getTypeOrAnonymous().getType().getText();
                    if (keyPsiName.equals(specificNames[i])) {
                      HaxeExpression keyExpression = arrayAccessExpression.getExpressionList().get(1);
                      ResultHolder handle = handle(keyExpression, context, resolver);
                      if (type.getSpecifics()[i].isUnknown()) {
                        type.getSpecifics()[i] = handle;
                      }
                      else {
                        ResultHolder unified = HaxeTypeUnifier.unify(handle, type.getSpecifics()[i]);
                        type.getSpecifics()[i] = unified;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return  resultHolder;
  }

  private static int getDistance(PsiReference reference, int offset) {
    return reference.getAbsoluteRange().getStartOffset() - offset;
  }

  private static boolean isMacroVariable(HaxeIdentifier identifier) {
    return identifier.getMacroId() != null;
  }

  @Nullable
  private static ResultHolder findInitTypeForUnify(@NotNull PsiElement field) {
    HaxeVarInit varInit = PsiTreeUtil.getParentOfType(field, HaxeVarInit.class);
    if (varInit != null) {
      HaxeFieldDeclaration type = PsiTreeUtil.getParentOfType(varInit, HaxeFieldDeclaration.class);
      if (type!= null) {
        HaxeTypeTag tag = type.getTypeTag();
        if (tag != null) {
          ResultHolder typeTag = HaxeTypeResolver.getTypeFromTypeTag(tag, field);
          if (!typeTag.isUnknown()) {
            return typeTag;
          }
        }
      }
    }
    return null;
  }

  private static SpecificTypeReference resolveAnyTypeDefs(SpecificTypeReference reference) {
    if (reference instanceof SpecificHaxeClassReference classReference && classReference.isTypeDef()) {
      if(classReference.isTypeDefOfFunction()) {
        return classReference.resolveTypeDefFunction();
      }else {
        SpecificHaxeClassReference resolvedClass = classReference.resolveTypeDefClass();
        return resolveAnyTypeDefs(resolvedClass);
      }
    }
    return reference;
  }

  static private void checkParameters(
    final PsiElement callelement,
    final HaxeMethodModel method,
    final List<HaxeExpression> arguments,
    final HaxeExpressionEvaluatorContext context,
    final HaxeGenericResolver resolver
  ) {
    checkParameters(callelement, method.getFunctionType(resolver), arguments, context, resolver);
  }

  static private void checkParameters(
    PsiElement callelement,
    SpecificFunctionReference ftype,
    List<HaxeExpression> parameterExpressions,
    HaxeExpressionEvaluatorContext context,
    HaxeGenericResolver resolver
  ) {
    if (!context.isReportingErrors()) return;

    List<Argument> parameterTypes = ftype.getArguments();

    int parameterTypesSize = parameterTypes.size();
    int parameterExpressionsSize = parameterExpressions.size();
    int len = Math.min(parameterTypesSize, parameterExpressionsSize);

    for (int n = 0; n < len; n++) {
      ResultHolder type = HaxeTypeResolver.resolveParameterizedType(parameterTypes.get(n).getType(), resolver);
      HaxeExpression expression = parameterExpressions.get(n);
      ResultHolder value = handle(expression, context, resolver);

      if (context.holder != null) {
        if (!type.canAssign(value)) {
          context.addError(
            expression,
            "Can't assign " + value + " to " + type,
            new HaxeCastFixer(expression, value.getType(), type.getType())
          );
        }
      }
    }

    //log.debug(ftype.getDebugString());
    // More parameters than expected
    if (parameterExpressionsSize > parameterTypesSize) {
      for (int n = parameterTypesSize; n < parameterExpressionsSize; n++) {
        context.addError(parameterExpressions.get(n), "Unexpected argument");
      }
    }
    // Less parameters than expected
    else if (parameterExpressionsSize < ftype.getNonOptionalArgumentsCount()) {
      context.addError(callelement, "Less arguments than expected");
    }
  }

  static private String getOperator(PsiElement field, TokenSet set) {
    ASTNode operatorNode = field.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }
}
