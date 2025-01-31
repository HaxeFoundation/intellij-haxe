/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2017-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.evaluate;
import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.findIteratorType;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeResolveUtil {

  private static final RecursionGuard<PsiElement> typeDefRecursionGuard = RecursionManager.createGuard("typeDefRecursionGuard");

  static {
    log.setLevel(LogLevel.INFO);
//    log.setLevel(LogLevel.TRACE);
  }  // We want warnings to get out to the log.

  @Nullable
  public static HaxeReference getLeftReference(@Nullable final PsiElement node) {
    if (node == null) return null;

    PsiElement leftExpression = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(node);
    PsiElement dotOrQuest = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(leftExpression);


    if (null == dotOrQuest) {
      return  null;
    }
    //  Null-safe navigation operator (?.) check
    if (dotOrQuest.getNode().getElementType() == HaxeTokenTypes.OQUEST) {
      dotOrQuest = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(dotOrQuest);
    }
    if (null == dotOrQuest || dotOrQuest.getNode().getElementType() != HaxeTokenTypes.ODOT) {
      return null;
    }

    if (leftExpression instanceof HaxeReference) return (HaxeReference)leftExpression;
    if (leftExpression instanceof HaxeParenthesizedExpression) {
      HaxeTypeCheckExpr typeCheck = ((HaxeParenthesizedExpression)leftExpression).getTypeCheckExpr();
      if (null != typeCheck) {
        return typeCheck;
      }

      PsiElement leftParen = leftExpression.getFirstChild();
      PsiElement child = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(leftParen);
      if (null != child && !HaxeTokenTypes.PRPAREN.equals(child.getNode().getElementType())) {
        return new HaxeParenthesizedExpressionReferenceImpl(child);
      }
    }
    if (leftExpression instanceof  HaxeObjectLiteral objectLiteral) {
      return  new HaxeParenthesizedExpressionReferenceImpl(objectLiteral);
    }

    return null;
  }

  @NotNull
  public static Pair<String, String> splitQName(@NotNull String qName) {
    final int dotIndex = qName.lastIndexOf('.');
    final String packageName = dotIndex == -1 ? "" : qName.substring(0, dotIndex);
    final String className = dotIndex == -1 ? qName : qName.substring(dotIndex + 1);

    return Pair.create(packageName, className);
  }

  @NotNull
  public static String joinQName(@Nullable String packageName, @Nullable String className) {
    String result = "";
    if (packageName != null && !packageName.isEmpty()) {
      result = packageName;
      if (className != null) {
        result += ".";
      }
    }
    if (className != null) {
      result += className;
    }
    return result;
  }

  @NotNull
  @NonNls
  public static String getPackageName(@Nullable final PsiFile file) {
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(file, HaxePackageStatement.class);
    return getPackageName(packageStatement);
  }

  @NotNull
  @NonNls
  public static String getPackageName(@Nullable HaxePackageStatement packageStatement) {
    HaxeReferenceExpression referenceExpression = packageStatement != null ? packageStatement.getReferenceExpression() : null;
    if (referenceExpression != null) {
      return referenceExpression.getText();
    }
    return "";
  }

  @Nullable
  public static HaxeClass findClassByQName(final @Nullable String qName, final @Nullable PsiElement context) {
    if (context == null || qName == null) {
      return null;
    }
    final PsiManager psiManager = context.getManager();
    final GlobalSearchScope scope = getScopeForElement(context);
    return findClassByQName(qName, psiManager, scope);
  }

  @NotNull
  public static GlobalSearchScope getScopeForElement(@NotNull PsiElement context) {
    final Project project = context.getProject();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return GlobalSearchScope.allScope(project);
    }
    final Module module = ModuleUtilCore.findModuleForPsiElement(context);
    return module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(project);
  }

  @Nullable
  public static HaxeClass findClassByQName(String qName, PsiManager psiManager, GlobalSearchScope scope) {
    final FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(qName);
    List<HaxeModel> result = HaxeProjectModel.fromProject(psiManager.getProject()).resolve(qualifiedInfo, scope);
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.get(0);
      if (item instanceof HaxeFileModel) {
        HaxeClassModel classModel = ((HaxeFileModel)item).getMainClassModel();
        return classModel != null ? classModel.haxeClass : null;
      }
      if (item instanceof HaxeClassModel) {
        return ((HaxeClassModel)item).haxeClass;
      }
    }

    return null;
  }

  /**
   * Locates the (parent) element above/surrounding this one in the PSI tree that
   * declares the type parameters that this element potentially uses.
   * (e.g. typedef or surrounding non-anonymous class.)
   *
   * @param element - element at which to start the search.
   * @return closest parent element which *could* provide type parameters.
   */
  @Nullable
  public static HaxeNamedComponent findTypeParameterContributor(PsiElement element) {
    while (null != element &&
           !(element instanceof PsiFile) &&
           !couldContributeTypeParameters(element)) {
      element = element.getParent();
    }
    return element instanceof HaxeNamedComponent ? (HaxeNamedComponent)element : null;
  }

  private static boolean couldContributeTypeParameters(@Nullable PsiElement element) {
    if (null == element) return false;
    if (element instanceof HaxeAnonymousType) return false; // Is also a HaxeClass.
    if (element instanceof HaxeMethod && ((HaxeMethod)element).hasTypeParameters()) {
      return true;
    }
    return /* element instanceof HaxeTypedefDeclaration || */
        element instanceof HaxeClass;
  }

  @NotNull
  public static List<HaxeClass> findComponentDeclarations(@Nullable PsiFile file) {
    if (file == null) {
      return Collections.emptyList();
    }
    HaxeFileModel model = HaxeFileModel.fromElement(file);
    if (model == null) {
      return Collections.emptyList();
    }
    return model.getClassModels().parallelStream().map(HaxeClassModel::getPsi).toList();
  }

  @Nullable
  public static HaxeClass findComponentDeclaration(@Nullable PsiFile file, @NotNull String componentName) {
    final List<HaxeClass> declarations = findComponentDeclarations(file);
    for (HaxeClass haxeClass : declarations) {
      final String name = haxeClass.getModel().getName();
      if (componentName.equalsIgnoreCase(name)) {
        return haxeClass;
      }
    }
    return null;
  }

  @NotNull
  public static List<HaxeType> findExtendsList(@Nullable HaxeInheritList extendsList) {
    List<? extends HaxeInherit> ext = null == extendsList ? null : extendsList.getExtendsDeclarationList();
    return findExtendsImplementsListImpl(ext);
  }

  public static List<HaxeType> getImplementsList(@Nullable HaxeInheritList extendsList) {
    List<? extends HaxeInherit> ext = null == extendsList ? null : extendsList.getImplementsDeclarationList();
    return findExtendsImplementsListImpl(ext);
  }

  @NotNull
  private static List<HaxeType> findExtendsImplementsListImpl(@Nullable List<? extends HaxeInherit> extendsList) {
    if (extendsList == null) {
      return Collections.emptyList();
    }
    final List<HaxeType> result = new ArrayList<HaxeType>();
    for (HaxeInherit inherit : extendsList) {
      final List<HaxeType> inheritTypes = inherit.getTypeList();
      result.addAll(inheritTypes);
    }
    return result;
  }

  public static List<HaxeFieldDeclaration> getClassVarDeclarations(HaxeClass haxeClass) {
    PsiElement body = null;
    final HaxeComponentType type = HaxeComponentType.typeOf(haxeClass);
    if (type == HaxeComponentType.CLASS) {
      body = PsiTreeUtil.getChildOfAnyType(haxeClass, HaxeClassBody.class, HaxeExternClassDeclarationBody.class);
    }

    final List<HaxeFieldDeclaration> result = new ArrayList<>();

    if (body == null) {
      return result;
    }

    final HaxeFieldDeclaration[] variables = PsiTreeUtil.getChildrenOfType(body, HaxeFieldDeclaration.class);

    if (variables == null) {
      return result;
    }
    Collections.addAll(result, variables);
    return result;
  }

  /**
   * Get the superclass of the given class containing the element.
   *
   * @param element - element to find and resolve its containing class.
   * @param context - element (thus its class) for which the superclass element must be resolved.
   * @param contextSpecialization - generic arguments at the context.
   * @return - a fully resolved superclass which contains {@code element}.  If the element is not contained in a superclass,
   *           HaxeClassResolveResult.EMPTY is returned.
   */
  @NotNull
  public static HaxeResolveResult getSuperclassResolveResult(@Nullable PsiElement element,
                                                             @Nullable PsiElement context,
                                                             @Nullable HaxeGenericSpecialization contextSpecialization) {
    if (null == element || null == context) return HaxeResolveResult.EMPTY;

    HaxeClass contextClass= UsefulPsiTreeUtil.getParentOfType(context, HaxeClass.class);// getHaxeClassResolveResult(context, contextSpecialization);
    if (null == contextClass) {
      return HaxeResolveResult.EMPTY;
    }

    HaxeClass elementClass= UsefulPsiTreeUtil.getParentOfType(element, HaxeClass.class);
    if (null == elementClass) {
      return HaxeResolveResult.EMPTY;
    }

    return resolveSuperclass(elementClass, contextClass, contextSpecialization); //contextSpecialization);
  }

  @NotNull
  private static HaxeResolveResult resolveSuperclass(@NotNull HaxeClass elementClass,
                                                     @NotNull HaxeClass contextClass,
                                                     @Nullable HaxeGenericSpecialization contextSpecialization) {
    if (elementClass.equals(contextClass)) {
      return HaxeResolveResult.create(elementClass, contextSpecialization);
    }

    if (null == contextSpecialization) {
      contextSpecialization = HaxeGenericResolverUtil.generateResolverFromScopeParents(contextClass).getSpecialization(contextClass);
    }

    PsiClass[] superClasses = contextClass.getSupers();
    for (PsiClass psiClass : superClasses) {
      if (psiClass instanceof HaxeClass) {
        HaxeClass clazz = (HaxeClass) psiClass;

        HaxeResolveResult
          specializedResult = HaxeResolveResult.create(contextClass, contextSpecialization.getInnerSpecialization(contextClass));
        specializedResult.specialize(clazz);

        HaxeResolveResult superResult = HaxeResolveResult.create(clazz, specializedResult.getSpecialization());

        if (clazz.equals(elementClass)) {
          return superResult;
        }
        if (clazz.isInheritor(elementClass, true)) {
          return resolveSuperclass(elementClass, superResult.getHaxeClass(), superResult.getSpecialization());
        }
      }
    }
    return HaxeResolveResult.EMPTY;
  }

  private static ThreadLocal<Stack<PsiElement>> resolveStack = new ThreadLocal<Stack<PsiElement>>() {
    @Override
    protected Stack<PsiElement> initialValue() {
      return new Stack<PsiElement>();
    }
  };

  private static void traceMessage(String message, int depth) {
    if (log.isTraceEnabled()) {
      StringBuilder out = new StringBuilder();

      out.append(Thread.currentThread().getId()); // Name());
      out.append(' ');

      while (0 < depth--) {
        out.append("  ");
      }
      out.append(message);
      traceAs(log, HaxeDebugUtil.getCallerStackFrame(), out.toString());
    }
  }

  @Deprecated
  @NotNull
  public static HaxeResolveResult getHaxeClassResolveResult(@Nullable PsiElement element) {
    return getHaxeClassResolveResult(element, null);
  }

  /**
   * Determine the type (class) of an element.
   *
   * @param element to find
   * @param specialization contianing generic (type parameter) information for the surrounding scope.
   * @return the found type and its specialization, or {@link HaxeResolveResult#EMPTY}.
   */
  @NotNull
  public static HaxeResolveResult getHaxeClassResolveResult(@Nullable PsiElement element,
                                                            @Nullable HaxeGenericSpecialization specialization) {
    if (specialization == null) {
      specialization = new HaxeGenericSpecialization();
    }

    final Stack<PsiElement> stack = resolveStack.get();

    if (element == null || element instanceof PsiPackage) {
      traceMessage("Cannot resolve " + (element == null ? "null value" : "package statement"), stack.size());
      return HaxeResolveResult.EMPTY;
    }

    if (stack.search(element) > 0) {
      //TODO this breaks tests, should probably make a RecursionGuard for this  instead of using
      //HaxeResolver.prohibitResultCaching(element);

      // We're already trying to resolve this element.  Prevent stack overflow.
      String msg = "Cannot resolve recursive/cyclic definition of " + element.getText()
                   + ", found at " + HaxeDebugUtil.elementLocation(element);
      traceMessage(msg, stack.size());
      // log.warn(msg); // Too wordy.
      return HaxeResolveResult.EMPTY;
    }

    try {
      stack.push(element);

      String elementString = null;
      if (log.isTraceEnabled()) {
        elementString = element instanceof HaxePsiCompositeElementImpl
                        ? ((HaxePsiCompositeElementImpl)element).toDebugString()
                        : element.toString();
        elementString = HaxeStringUtil.elideBetween(elementString, '{', '}');
        elementString = HaxeStringUtil.elide(elementString, 80);
        traceMessage("Resolving: " + elementString, stack.size()-1);
      }

      HaxeResolveResult result = getHaxeClassResolveResultInternal(element, specialization);

      if (log.isDebugEnabled()) {
        String msg = "Element " + elementString + " resolved as " + result.toString();
        if (log.isTraceEnabled()) {
          traceMessage(msg, stack.size()-1);
        } else if (log.isDebugEnabled()) {
          log.debug(msg);
        }
      }
      return result;
    }
    finally {
      try {
        stack.pop();
      }
      catch (EmptyStackException e) {
        if (log.isDebugEnabled()) {
          log.debug("Unexpected excessive stack pop. " + e.toString());
        }
      }
    }
  }

  @NotNull
  private static HaxeResolveResult getHaxeClassResolveResultInternal(@Nullable PsiElement element,
                                                                     @Nullable HaxeGenericSpecialization specialization) {
    if (element instanceof HaxeType haxeType) {
      HaxeResolveResult result = tryResolveType(haxeType, element, specialization);
      if (null == result.getHaxeClass() && specialization.containsKey(null, element.getText())) {
        return specialization.get(null, element.getText());
      }
      return result;
    }
    if (element instanceof HaxeComponentName) {
      return getHaxeClassResolveResult(element.getParent(), specialization);
    }
    if (element instanceof AbstractHaxeTypeDefImpl typeDef) {
      if (typeDef.getFunctionType() != null) {
        return HaxeResolveResult.create(typeDef.getFunctionType());
      }else{
        return typeDef.getTargetClass(specialization);
      }
    }
    if (element instanceof HaxeClass haxeClass) {
      return HaxeResolveResult.create(haxeClass, specialization);
    }
    if (element instanceof  HaxeFunctionType functionType) {
      return HaxeResolveResult.create(functionType, specialization);
    }
    if (element instanceof  HaxeSwitchCaseCaptureVar captureVar) {
      HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(captureVar, HaxeSwitchStatement.class);
      if(switchStatement != null && switchStatement.getExpression() != null){
        HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(switchStatement);
        ResultHolder type = HaxeTypeResolver.getPsiElementType(switchStatement.getExpression(), resolver);
        SpecificHaxeClassReference classType = type.getClassType();
        if(classType != null) {
          return classType.asResolveResult();
        }else {
          //TODO add support for function in  ResolveResult
        }
      }

    }
    if (element instanceof HaxeIteratorkey || element instanceof HaxeIteratorValue) {
        final HaxeForStatement forStatement = getParentForStatement(element);
        final HaxeIterable iterable = forStatement.getIterable();
        if (iterable == null) {
          // iterable is @Nullable
          // (sometimes when you're typing for statement it becames null for short time)
          return HaxeResolveResult.EMPTY;
        }
        ResultHolder type = findIteratorType(element);
        if (type!= null && !type.isUnknown() && type.isClassType()) {
          return type.getClassType().asResolveResult();
        }

        return HaxeResolveResult.EMPTY;
    }
    if (element instanceof HaxeValueIterator valueIterator) {
      final HaxeForStatement forStatement = PsiTreeUtil.getParentOfType(valueIterator, HaxeForStatement.class);
      if (forStatement != null) {
        final HaxeIterable iterable = forStatement.getIterable();
        if (iterable == null) {
          // iterable is @Nullable
          // (sometimes when you're typing for statement it becames null for short time)
          return HaxeResolveResult.EMPTY;
        }
        final HaxeExpression expression = iterable.getExpression();
        if (expression instanceof HaxeReference reference) {

          final HaxeResolveResult resolveResult = reference.resolveHaxeClass();
          List<String> circularReferenceProtection = new LinkedList<>();
          return searchForIterableTypeRecursively(resolveResult, circularReferenceProtection);
        }
      }
      return HaxeResolveResult.EMPTY;
    }

    if (element instanceof HaxeForStatement) {
      //TODO remove?
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      if (iterable == null) {
        // iterable is @Nullable
        // (sometimes when you're typing for statement it becames null for short time)
        return HaxeResolveResult.EMPTY;
      }
      final HaxeExpression expression = iterable.getExpression();
      if (expression instanceof HaxeReference reference) {

        final HaxeResolveResult resolveResult = reference.resolveHaxeClass();
        List<String> circularReferenceProtection = new LinkedList<>();
        return  searchForIterableTypeRecursively(resolveResult,circularReferenceProtection);

      }
      return HaxeResolveResult.EMPTY;
    }

    HaxeResolveResult result = tryResolveClassByTypeTag(element, specialization);
    if (result.getHaxeClass() != null) {
      return result;
    }

    result = tryResolveClassByInferringMethodReturnType(element, specialization);
    if (result.getHaxeClass() != null) {
      return result;
    }

    result = HaxeAbstractEnumUtil.resolveFieldType(element, specialization);
    if (result != null) {
      return result;
    }

    if (specialization.containsKey(null, element.getText())) {
      return specialization.get(null, element.getText());
    }
    if (element instanceof  HaxePsiField psiField) {
      // if we dont have a type search references
      if (psiField.getTypeTag() != null) {
        ResultHolder type = HaxeTypeResolver.getTypeFromTypeTag(psiField.getTypeTag(), element);
        if (!type.isUnknown()) {
          return type.getType().asResolveResult();
        }
      }else if (psiField.getVarInit() != null) {
        // do not resolve VarInit here, we want to be able to to check usage when  init expression  is = null;
        HaxeExpressionEvaluatorContext evaluate = evaluate(psiField, null);
        ResultHolder holder = evaluate.result;
        if (!holder.isUnknown()) {
          //TODO function literals does not have a HaxeType and will result in null
          HaxeResolveResult resolveResult = holder.getType().asResolveResult();
          if (resolveResult != null) {
            return resolveResult;
          }

        }
      }
      if (psiField.getTypeTag() == null &&  psiField.getVarInit() == null) {
        HaxeComponentName componentName = psiField.getComponentName();
        HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(componentName);
        ResultHolder holder = HaxeExpressionEvaluator.searchReferencesForType(componentName, context, null, null);
        if (!holder.isUnknown()) {
          //TODO function literals does not have a HaxeType and will result in null
          HaxeResolveResult resolveResult = holder.getType().asResolveResult();
          if (resolveResult != null) {
            return resolveResult;
          }
        }
      }
    }
    if (element instanceof  HaxeValueExpression valueExpression) {
      result = resolveValueExpressionClass(valueExpression, specialization);
      if (result != null) {
        return result;
      }
    }

    final HaxeVarInit varInit = PsiTreeUtil.getChildOfType(element, HaxeVarInit.class);
    final HaxeExpression initExpression = varInit == null ? null : varInit.getExpression();
    if (initExpression instanceof HaxeReference reference) {
      // init expressions are complicated and can depend on type parameters,
      HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);
      ResultHolder evalResult = evaluate(reference, genericResolver).result;
      if (!evalResult.isUnknown()) {
        if (evalResult.isClassType()) {
          return evalResult.getClassType().asResolveResult();
        }else if (evalResult.isFunctionType()) {
          HaxeResolveResult functionResult = evalResult.getFunctionType().asResolveResult();
          if (functionResult != null) return functionResult;
        }
      }
      // if result is a method declaration functionResult will be null and we end up here,
      // not sure if resolveHaxeClass will  be able to solve anything but keeping it here as a failover
      result = reference.resolveHaxeClass();
      // TODO MLO: check for missing Type parameter
      //  (var x = new Map() is allowed and typeParameters can be resolved from usage)
      result.specialize(initExpression);
      return result;
    }


    return getHaxeClassResolveResult(initExpression, specialization);
  }

  private static HaxeResolveResult resolveValueExpressionClass(HaxeValueExpression valueExpression,
                                                               @Nullable HaxeGenericSpecialization specialization) {

    HaxePsiCompositeElement element = null;
    if (valueExpression.getSwitchStatement() != null) {
      element = valueExpression.getSwitchStatement();
    }
    if (valueExpression.getIfStatement() != null) {
      element = valueExpression.getIfStatement();
    }
    if (valueExpression.getTryStatement() != null) {
      element = valueExpression.getTryStatement();
    }
    if (valueExpression.getVarInit() != null) {
      element = valueExpression.getVarInit();
    }
    if (valueExpression.getExpression() != null) {
      element = valueExpression.getExpression();

    }
    if (element != null) {
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(element);
      ResultHolder result = evaluate(element, context, specialization.toGenericResolver(valueExpression)).result;
      if (result.getClassType() != null) {
        return result.getClassType().asResolveResult();
      }
    }
    return null;
  }

  private static HaxeResolveResult searchForIterableTypeRecursively(HaxeResolveResult resolveResult,
                                                                    List<String> circularReferenceProtection) {
    final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
    final HaxeGenericResolver resolver = resolveResult.getGenericResolver();
    final HaxeGenericSpecialization resultSpecialization = resolveResult.getSpecialization();

    // try next
    HaxeResolveResult result = getResolveMethodReturnType(resolver, resolveResultHaxeClass, "next", resultSpecialization);
    if (result.getHaxeClass() != null) {
      return result;
    }
    // try iterator
    HaxeResolveResult iteratorResult =
      getResolveMethodReturnType(resolver, resolveResultHaxeClass, "iterator",
                                 resultSpecialization.getInnerSpecialization(resolveResultHaxeClass));

    HaxeClass iteratorResultHaxeClass = iteratorResult.getHaxeClass();
    // Now, look for iterator's next
    result = getResolveMethodReturnType(resolver, iteratorResultHaxeClass, "next", iteratorResult.getSpecialization());


    if (result.getHaxeClass() == null && resolveResultHaxeClass != null) {
      // check underlying types
      SpecificHaxeClassReference underlyingClassReference = resolveResultHaxeClass.getModel().getUnderlyingClassReference(resolver);
      if(underlyingClassReference != null) {
        String className = underlyingClassReference.getClassName();
        //check if we have visited class before
        if (!circularReferenceProtection.contains(className)) {
          circularReferenceProtection.add(className);
          result = searchForIterableTypeRecursively(underlyingClassReference.asResolveResult(), circularReferenceProtection);
        }
      }
    }
    return result;
  }

  @NotNull
  private static HaxeForStatement getParentForStatement(PsiElement iterator) {
    PsiElement keyValueIterator = iterator.getParent();
      return  (HaxeForStatement)keyValueIterator.getParent();
  }

  @NotNull
  private static HaxeResolveResult getResolveMethodReturnType(HaxeGenericResolver resolver, HaxeClass haxeClass,
                                                              String MethodName, HaxeGenericSpecialization specialization) {

    return getHaxeClassResolveResult(haxeClass == null ? null : haxeClass.findHaxeMethodByName(MethodName, resolver),  specialization);
  }

  @NotNull
  private static HaxeResolveResult resolveFieldType(HaxeGenericResolver resolver, HaxeClass haxeClass,
                                                    String FieldName, HaxeGenericSpecialization specialization) {
    return getHaxeClassResolveResult(haxeClass == null ? null : haxeClass.findHaxeFieldByName(FieldName, resolver), specialization);
  }

  @NotNull
  public static HaxeResolveResult tryResolveType(HaxeType type,
                                                 PsiElement specializationContext,
                                                 HaxeGenericSpecialization specialization) {

    HaxeClass haxeClass = type == null ? null : tryResolveClassByQName(type);
    if (haxeClass == null && type != null && specialization.containsKey(specializationContext, type.getText())) {
      HaxeResolveResult result = specialization.get(specializationContext, type.getText());
      if(result.getHaxeClass() instanceof  HaxeAnonymousType) {
        result.specializeByParent(specialization.toGenericResolver(null));
        result.specializeByTypeInference(type);
      }
      return result;
    }
    if (haxeClass == null && type != null) {
      PsiElement resolve = type.getReferenceExpression().resolve();
      if (resolve instanceof HaxeGenericListPart listPart) {
        // TypeParameters does not have generics so just creating an instance is ok here
        return  listPart.getModel().getInstanceType().getType().asResolveResult();
      }
    }
    if (null != haxeClass && haxeClass.isGeneric()) {
      HaxeResolveResult temp = HaxeResolveResult.create(haxeClass, specialization);
      temp.specializeByParameters(type.getTypeParam());
      specialization = temp.getSpecialization();
    }

    HaxeResolveResult result = getHaxeClassResolveResult(haxeClass, specialization.getInnerSpecialization(specializationContext));
    if (result.getHaxeClass() != null) {
      // anonymous types does not have TypeParam directly  but gets it generics from parent
      // so to avoid "skipping" a hierarchy level we only do this for non-HaxeAnonymous types
      if(!(result.getHaxeClass() instanceof HaxeAnonymousType)) {
        result.specializeByParameters(type == null ? null : type.getTypeParam());
      }
      if(result.getFunctionType() != null) {
        result.specializeByParameters(type == null ? null : type.getTypeParam());
      }
    }

    return result;
  }


  @NotNull
  public static HaxeResolveResult tryResolveClassByTypeTag(PsiElement element,
                                                           HaxeGenericSpecialization specialization) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = (typeTag != null) ? typeTag.getTypeOrAnonymous() : null;
    final HaxeType type = (typeOrAnonymous != null) ? typeOrAnonymous.getType() :
                          ((element instanceof HaxeType) ? (HaxeType)element : null);

    HaxeResolveResult resolvedType = tryResolveType(type, element, specialization);
    if (HaxeResolveResult.EMPTY != resolvedType) {
      return resolvedType;
    }

    if (typeOrAnonymous != null && typeOrAnonymous.getAnonymousType() != null) {
      SpecificHaxeClassReference anonymousTypeReference = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous).getClassType();
      if (anonymousTypeReference != null) return anonymousTypeReference.asResolveResult();
    }

    if (typeTag != null) {
      final HaxeFunctionType fnType = typeTag.getFunctionType();
      final HaxeClass psiClass = HaxeResolveUtil.findClassByQName("haxe.Constraints.Function", element);
      if (null != fnType && psiClass instanceof HaxeAbstractTypeDeclaration) {
        final HaxeClass fn = new HaxeSpecificFunction((HaxeAbstractTypeDeclaration)psiClass,
                                                      fnType, specialization);
        return HaxeResolveResult.create(fn, specialization);
      }
    }

    return HaxeResolveResult.EMPTY;
  }

  @NotNull
  public static HaxeResolveResult tryResolveClassByInferringMethodReturnType(@Nullable PsiElement element,
                                                                             HaxeGenericSpecialization specialization) {
    if (null == element) {
      return HaxeResolveResult.EMPTY;
    }

    if (element instanceof HaxeMethodDeclaration method) {
      HaxeMethodModel model = method.getModel();

      HaxeGenericResolver resolver = specialization.toGenericResolver(element);

      ResultHolder result = model.getReturnType(resolver);
      if (null != result) {
        SpecificTypeReference typeRef = result.getType();
        if (typeRef instanceof SpecificHaxeClassReference) {
          SpecificHaxeClassReference hcRef = (SpecificHaxeClassReference)typeRef;
          HaxeClass haxeClass = hcRef.getHaxeClass();
          HaxeGenericSpecialization resultSpecialization = haxeClass != null
              ? HaxeGenericSpecialization.fromGenericResolver(haxeClass, hcRef.getGenericResolver())
              : null;

          return HaxeResolveResult.create(haxeClass, resultSpecialization);
        } else {
          // It's a function return type.
          // TODO: Implement function return types.
          log.warn("Function return types not implemented in the resolver yet.");
        }
      }
    }
    return HaxeResolveResult.EMPTY;
  }

  private static HaxeResolveResult tryResolveFunctionType(@Nullable HaxeFunctionType functionType,
                                                          HaxeGenericSpecialization specialization) {
    if (functionType == null) {
      return HaxeResolveResult.EMPTY;
    }

    final HaxeFunctionReturnType returnType = functionType.getFunctionReturnType();
    if (returnType == null || returnType.getTypeOrAnonymous() == null) { return HaxeResolveResult.EMPTY; }
    // TODO Stub classes must be introduced in the near future to cover cases where Function is a return type, not a class type or anonymous structure.
    return tryResolveClassByTypeTag(returnType.getTypeOrAnonymous().getType(), specialization);
  }

  @NotNull
  public static List<HaxeClass> tryResolveClasses(@NotNull List<HaxeType> types) {
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    for (HaxeType haxeType : types) {
      PsiElement resolve = haxeType.getReferenceExpression().resolve();
      if (resolve instanceof HaxeClass type) {
        result.add(type);
      }else {
        final HaxeClass haxeClass = tryResolveClassByQName(haxeType);
        if (haxeClass != null) {
          result.add(haxeClass);
        }
      }
    }
    return result;
  }
  @NotNull
  public static List<HaxeClass> tryResolveClassesByQName(@NotNull List<HaxeType> types) {
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    for (HaxeType haxeType : types) {
      final HaxeClass haxeClass = tryResolveClassByQName(haxeType);
      if (haxeClass != null) {
        result.add(haxeClass);
      }
    }
    return result;
  }

  @Nullable
  public static HaxeClass tryResolveClassByQName(@Nullable PsiElement element) {
    if (element == null || element.getContext() == null) {
      return null;
    }
    String name = getQNameFromImportStatment(element);
    PsiElement type = tryGetReferenceExpressionFromType(element);
    HaxeClass result = name == null ? tryResolveClassByQNameWhenGetQNameFail(type) : findClassByQName(name, element.getContext());
    result = result != null ? result : findClassByQNameInSuperPackages(type);
    return result;
  }

  private static PsiElement tryGetReferenceExpressionFromType(@NotNull PsiElement element) {
    if (element instanceof  HaxeType type) return type.getReferenceExpression();
    if (element instanceof  HaxeTypeOrAnonymous typeOrAnonymous) {
      if (typeOrAnonymous.getType() != null)  typeOrAnonymous.getType().getReferenceExpression();
    }
    return element;
  }

  private static String tryResolveFullyQualifiedHaxeReferenceExpression(PsiElement type) {
    if (type instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression topmostParentOfType = PsiTreeUtil.getTopmostParentOfType(type, HaxeReferenceExpression.class);

      if (topmostParentOfType == null) {
        topmostParentOfType = (HaxeReferenceExpression)type;
      }

      HaxeClass haxeClass = findClassByQName(topmostParentOfType.getText(), topmostParentOfType.getContext());
      if (haxeClass != null) {
        return topmostParentOfType.getText();
      }

      PsiElement parent = type.getParent();
      HaxeClass classByQName = findClassByQName(parent.getText(), parent.getContext());
      if (classByQName != null) {
        return parent.getText();
      }
    }

    return null;
  }

  @Nullable
  private static HaxeClass findClassByQNameInSuperPackages(PsiElement type) {
    HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(type.getContainingFile(), HaxePackageStatement.class);
    String packageName = getPackageName(packageStatement);
    String[] packages = packageName.split("\\.");
    String typeName = (type instanceof HaxeType ? ((HaxeType)type).getReferenceExpression() : type).getText();
    if (typeName.matches("[^:<>{}()/]+")) {
      for (int i = packages.length - 1; i >= 0; --i) {
        StringBuilder qNameBuilder = new StringBuilder();
        for (int j = 0; j <= i; ++j) {
          if (!packages[j].isEmpty()) {
            qNameBuilder.append(packages[j]).append('.');
          }
        }
        qNameBuilder.append(typeName);
        HaxeClass haxeClass = findClassByQName(qNameBuilder.toString(), type);
        if (haxeClass != null) {
          return haxeClass;
        }
      }
    }
    return null;
  }

  @Nullable
  private static String getQNameFromImportStatment(@NotNull PsiElement type) {
    HaxeImportStatement importStatement = PsiTreeUtil.getParentOfType(type, HaxeImportStatement.class, false);
    if (importStatement != null) {
      HaxeReferenceExpression referenceExpression = importStatement.getReferenceExpression();
      return referenceExpression == null ? null : referenceExpression.getText();
    }

    HaxeUsingStatement usingStatement = PsiTreeUtil.getParentOfType(type, HaxeUsingStatement.class, false);
    if (usingStatement != null) {
      HaxeReferenceExpression expression = usingStatement.getReferenceExpression();
      return expression == null ? null : expression.getText();
    }

    return null;
  }

  @Nullable
  private static HaxeClass tryResolveClassByQNameWhenGetQNameFail(@NotNull PsiElement type) {
    if (type instanceof HaxeType) {
      type = ((HaxeType)type).getReferenceExpression();
    }

    String className = type.getText();
    PsiElement result = null;

    if (className != null && className.indexOf('.') == -1) {
      final HaxeFileModel fileModel = HaxeFileModel.fromElement(type);
      if (fileModel != null) {
        boolean isType =  type.getParent() instanceof HaxeType ||  PsiTreeUtil.getParentOfType(type, HaxeTypeTag.class) != null;
        result = searchInSameFile(fileModel, className, isType);
        if (result == null) {
          List<PsiElement> matchesInImport = searchInImports(fileModel, className);
          if (!matchesInImport.isEmpty()) {
            if (isType) {// typeTags should not contain EnumValues only parent enum type
              matchesInImport = matchesInImport.stream()
                .filter(element -> !(element instanceof HaxeEnumValueDeclaration))
                .filter(element -> !(element instanceof HaxeFieldDeclaration)) // @:enum abstracts have EnumValues as fields
                .toList();
            }
            // one file may contain multiple enums and have enumValues with the same name; trying to match any argument list
            if(matchesInImport.size()> 1 &&  type.getParent() instanceof  HaxeCallExpression callExpression) {
              int expectedSize = Optional.ofNullable(callExpression.getExpressionList()).map(e -> e.getExpressionList().size()).orElse(0);
              for (PsiElement element : matchesInImport) {
                if (element instanceof  HaxeEnumValueDeclarationConstructor enumValueDeclaration) {
                  int currentSize = Optional.of(enumValueDeclaration.getParameterList()).map(p ->  p.getParameterList().size()).orElse(0);
                  if (expectedSize == currentSize) {
                    result = element;
                    break;
                  }
                }
              }
            }
            if (result == null && !matchesInImport.isEmpty()) result = matchesInImport.get(0);
          }
        }
        if (result == null) result = searchInSamePackage(fileModel, className, false);
      }
    } else {
      className = tryResolveFullyQualifiedHaxeReferenceExpression(type);
      result = findClassByQName(className, type.getContext());
    }

    if (result == null && className != null) {
      //make sure we  wont try to files with invalid characters
      if (className.matches("[^:<>{}()/]+")) {
        result = findClassByQName(className, type.getContext());
      }
    }

    return result instanceof HaxeClass haxeClass ? haxeClass : null;
  }

  @Nullable
  public static PsiElement searchInSameFile(@NotNull HaxeFileModel file, @NotNull String name, boolean isType) {
    List<HaxeClassModel> models = file.getClassModels();
    if (!isType) {
      Optional<HaxeEnumValueModel> enumValueModel = models.stream().filter(model -> model instanceof HaxeEnumModel)
        .map(model -> ((HaxeEnumModel)model).getValue(name))
        .filter(Objects::nonNull)
        .findFirst();
      if (enumValueModel.isPresent()) return enumValueModel.get().getBasePsi();
    }

    final HaxeModel result  = models.stream().filter(model -> name.equals(model.getName()))
      .findFirst()
      .orElse(null);

    return result != null ? result.getBasePsi() : null;
  }
  @NotNull
  public static List<PsiElement> searchInSameFileForEnumValues(@NotNull HaxeFileModel file, @NotNull String name) {
    List<HaxeClassModel> models = file.getClassModels();
      return  models.stream().filter(model -> model instanceof HaxeEnumModel)
        .map(model -> ((HaxeEnumModel)model).getValue(name))
        .filter(Objects::nonNull)
        .map(HaxeModel::getBasePsi)
        .toList();

  }

  @NotNull
  public static List<PsiElement> searchInImports(HaxeFileModel file, String name) {
    List<PsiElement> results = new ArrayList<>();
    results.addAll(searchInSpecifiedImports(file, name));
    results.addAll(searchInDirectoryImports(file, name));
    return results;
  }

  @NotNull
  public static List<PsiElement> searchInSpecifiedImports(HaxeFileModel file, String name) {
    List<PsiElement> results = new ArrayList<>();
    List<HaxeImportableModel> models = file.getOrderedImportAndUsingModels();
    for (int i = models.size() - 1; i >= 0; i--) {
      HaxeImportableModel model = models.get(i);

      if (model instanceof HaxeImportModel importModel) {
        List<PsiElement> elements = importModel.exposeAllByName(name);
        results.addAll(elements);
      } else {
        PsiElement element = model.exposeByName(name);
        if (element != null) {
          results.add(element);
        }
      }
    }
    return results;
  }

  /**
   * Searches for import.hx files between the file's directory and the source root,
   * examining each for matches.
   *
   * @param file The file that has import statements to match.
   * @param name The name of the Type that we are searching for.
   * @return List of PSI element for the Type, if found; empty list, otherwise.
   */
  @NotNull
  private static List<PsiElement> searchInDirectoryImports(HaxeFileModel file, String name) {
    List<PsiElement> results = new ArrayList<>();
    walkDirectoryImports(file, (importModel) -> {
      List<PsiElement> elements = searchInSpecifiedImports(importModel, name);
      if (!elements.isEmpty()) {
        results.addAll(elements);
        return false;
      }
      return true;
    });
    return results;
  }

  /**
   * Calls a function on all import.hx files from the current directory toward the source root.
   * @param file the starting file
   * @param processor the function to call; it returns false to stop early, true to keep going.
   * @return the last value returned from processor; true if processor was never called.
   */
  public static boolean walkDirectoryImports(HaxeFileModel file, @NotNull java.util.function.Function<HaxeFileModel, Boolean> processor) {
    if (null == file) return true;

    final VirtualFile vfile = file.getFile().getVirtualFile();
    if (null == vfile) return true; // In memory files

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getBasePsi().getProject()).getFileIndex();
    final VirtualFile sourceRoot = fileIndex.getSourceRootForFile(vfile);
    if (null == sourceRoot) return true;

    boolean keepRunning = true;
    HaxeFile haxeFile = file.getFile();
    PsiDirectory parentDirectory = haxeFile.getContainingDirectory();
    final VirtualFile stopDir = sourceRoot.getParent(); // SrcRoot is a valid place to pick up an import.hx file.
    while (keepRunning && null != parentDirectory && !parentDirectory.getVirtualFile().equals(stopDir)) {
      PsiFile importFile = parentDirectory.findFile("import.hx");
      if (importFile instanceof HaxeFile) {
        HaxeFileModel importModel = HaxeFileModel.fromElement(importFile);
        keepRunning = processor.apply(importModel);
      }
      parentDirectory = parentDirectory.getParentDirectory();
    }
    return keepRunning;
  }

  @Nullable
  public static PsiElement searchInSamePackage(@NotNull HaxeFileModel file, @NotNull String name, boolean checkForEnumValues) {
    final HaxePackageModel packageModel = file.getPackageModel();
    if (packageModel != null) {
      List<HaxeModel> exposedMembers = packageModel.getExposedMembers();
      for (HaxeModel model : exposedMembers) {
        if (name.equals(model.getName())) {
          return model.getBasePsi();
        }else if (checkForEnumValues) {
          if (model instanceof HaxeClassModel classModel) {
            HaxeModel possibleModel = typeDefRecursionGuard.doPreventingRecursion(classModel.getPsi(), true, () -> tryResolveTypeDefClass(classModel));
            if (possibleModel != null )model = possibleModel;
          }
          if (model instanceof HaxeEnumModel enumModel) {
            Optional<HaxeEnumValueModel> match = enumModel.getValues().stream().filter(m -> name.equals(m.getName())).findFirst();
            if (match.isPresent()){
              HaxeEnumValueModel valueModel = match.get();
              if (valueModel instanceof  HaxeEnumValueFieldModel enumValueFieldModel) {
                if(enumValueFieldModel.isAbstractType()) {
                  return enumValueFieldModel.getAbstractEnumValuePsi().getComponentName();
                }else {
                  return enumValueFieldModel.getEnumValuePsi().getComponentName();
                }
              }else if (valueModel instanceof  HaxeEnumValueConstructorModel constructorModel) {
                return constructorModel.getEnumValuePsi().getComponentName();
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static @Nullable HaxeModel tryResolveTypeDefClass(HaxeClassModel classModel) {
    if (classModel.isTypedef()) {
      SpecificTypeReference resolved = classModel.getUnderlyingType();
      while (resolved instanceof  SpecificHaxeClassReference haxeClassReference) {
        HaxeClassModel resolvedModel = haxeClassReference.getHaxeClassModel();
        if (resolvedModel != null && resolvedModel.isTypedef()) {
          SpecificTypeReference underlyingType = resolvedModel.getUnderlyingType();
          if (underlyingType != null) {
            resolved = underlyingType;
          }else {
            break;
          }
        }else {
          break;
        }
      }

      if (resolved instanceof SpecificHaxeClassReference classReference) {
        return classReference.getHaxeClassModel();
      }
    }
    return classModel;
  }

  @Nullable
  public static String getQName(PsiFile file, final String name, boolean searchInSamePackage, boolean searchParentPackages, HaxeType targetReference) {



    HaxeModule module = PsiTreeUtil.getChildOfType(file, HaxeModule.class);
    if (module != null) {
      @NotNull PsiElement[] moduleChildren = module.getChildren();
      HaxeClass classForType = null;
      for (PsiElement child : moduleChildren) {
        if (child instanceof HaxeClass && name.equals(((HaxeClass)child).getName())) {
          classForType = (HaxeClass)child;
          break;
        }
      }

      if (classForType != null) {
        return classForType.getQualifiedName();
      }
    }
    @NotNull PsiElement[] fileChildren = file.getChildren();
    final HaxeImportStatement importStatement =
      (HaxeImportStatement)(StreamUtil.reverse(Arrays.stream(fileChildren))
                              .filter(element ->
                                        element instanceof HaxeImportStatement impStatement
                                        && impStatement.getModel().exposeByName(name) != null)
                              .findFirst()
                              .orElse(null));

    final HaxeExpression importStatementExpression = importStatement == null ? null : importStatement.getReferenceExpression();
    if (importStatementExpression != null) {
      PsiElement element = importStatement.getModel().exposeByName(name);
      if (element instanceof HaxeClass classModel) {
        return classModel.getQualifiedName();
      }
      // fallback if we dont have a HaxeClass
      return importStatementExpression.getText();
    }

    if (searchInSamePackage && fileChildren.length > 0) {
      final HaxeFileModel fileModel = HaxeFileModel.fromElement(fileChildren[0]);
      if (fileModel != null) {
        final HaxePackageModel packageModel = fileModel.getPackageModel();
        if (packageModel != null) {
          final HaxeClassModel classModel = packageModel.getClassModel(name);
          if (classModel != null) {
            return classModel.haxeClass.getQualifiedName();
          }
        }
      }
    }
    if (searchParentPackages && targetReference != null) {
      HaxeClass haxeClass = findClassByQNameInSuperPackages(targetReference);
      if (haxeClass != null) return haxeClass.getQualifiedName();
    }
    return null;
  }

  @Nullable
  public static PsiComment findDocumentation(HaxeNamedComponent element) {
    if (element instanceof PsiMember member) {
      PsiElement sibling = member.getPrevSibling();
      // workaround for members in module
      // TODO mlo:  maybe look into how tokens are organised when parsed so that meta and comments are inside the module
      if (sibling == null && member.getParent() instanceof HaxeModule){
        sibling  = member.getParent();
      }
      // make sure we dont back trace to docs from a different member
      while (sibling != null && !(sibling instanceof PsiMember)) {
        if (sibling instanceof PsiComment comment && comment.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT) {
          return comment;
        }
        sibling = sibling.getPrevSibling();
      }
    }
    return null;
  }
  //@Nullable
  //public static PsiComment findDocumentationOld(HaxeNamedComponent element) {
  //  PsiElement candidate =  UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(element, true);
  //  // we search backwards for comments, and skipping metas etc
  //  while (candidate != null) {
  //    // if we find other named components while searching we abort as whatever
  //    // doc we might find would belong to that named component and not ours.
  //    if (candidate instanceof  HaxeNamedComponent) {
  //      return null;
  //    }
  //    if (candidate instanceof  PsiComment psiComment) {
  //      IElementType type = psiComment.getTokenType();
  //      if (type == DOC_COMMENT) {
  //        return psiComment;
  //      }
  //    }
  //    candidate = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(candidate, true);
  //  }
  //  return null;
  //}

  public static Set<IElementType> getDeclarationTypes(@Nullable HaxePsiModifier[] attributeList) {
    return attributeList == null ? Collections.<IElementType>emptySet() : getDeclarationTypes(Arrays.asList(attributeList));
  }

  public static Set<IElementType> getDeclarationTypes(@Nullable List<? extends HaxePsiModifier> attributeList) {
    if (attributeList == null || attributeList.isEmpty()) {
      return Collections.emptySet();
    }
    final Set<IElementType> resultSet = new HashSet<IElementType>();
    for (HaxePsiModifier attribute : attributeList) {
      PsiElement result = attribute.getFirstChild();
      if (result instanceof LeafPsiElement) {
        resultSet.add(((LeafPsiElement)result).getElementType());
      }
    }
    return resultSet;
  }

  @NotNull
  public static List<HaxeComponentName> getComponentNames(List<? extends HaxeNamedComponent> components) {
    return ContainerUtil.map(components, (Function<HaxeNamedComponent, HaxeComponentName>)HaxeNamedComponent::getComponentName);
  }

  public static HashSet<HaxeClass> getBaseClassesSet(@NotNull HaxeClass clazz) {
    return getBaseClassesSet(clazz, new HashSet<HaxeClass>());
  }

  @NotNull
  public static HashSet<HaxeClass> getBaseClassesSet(@NotNull HaxeClass clazz, @NotNull HashSet<HaxeClass> outClasses) {
    List<HaxeType> types = new ArrayList<HaxeType>();
    types.addAll(clazz.getHaxeExtendsList());
    types.addAll(clazz.getHaxeImplementsList());
    for (HaxeType baseType : types) {
      final HaxeClass baseClass = HaxeResolveUtil.tryResolveClassByQName(baseType);
      if (baseClass != null && outClasses.add(baseClass)) {
        getBaseClassesSet(baseClass, outClasses);
      }
    }
    return outClasses;
  }

  public static SpecificHaxeClassReference resolveExtractorEnum(HaxeEnumArgumentExtractor extractor) {
    HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(extractor, HaxeSwitchStatement.class);
    if (switchStatement != null) {
      HaxeExpression expression = switchStatement.getExpression();
      if (expression == null) return null;
      if (expression instanceof  HaxeParenthesizedExpression parenthesizedExpression){
        expression = parenthesizedExpression.getExpression();
      }
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expression);
      ResultHolder switchExpressionResult = evaluate(expression, new HaxeExpressionEvaluatorContext(expression), resolver).result;
      if (!switchExpressionResult.isUnknown() && switchExpressionResult.getClassType() != null) {
        switchExpressionResult = switchExpressionResult.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference().createHolder();
      }


      if (switchExpressionResult.isEnum() && switchExpressionResult.getClassType() != null) {
        return switchExpressionResult.getClassType();
      }
    }
    return null;
  }

  public static HaxeEnumValueDeclaration resolveEnumValueDeclaration(SpecificHaxeClassReference enumClass, HaxeEnumArgumentExtractor extractor) {
    return resolveEnumValueDeclaration(enumClass, extractor.getEnumValueReference().getReferenceExpression().getIdentifier().getText());
  }
  public static HaxeEnumValueDeclaration resolveEnumValueDeclaration(SpecificHaxeClassReference enumClass, String memberName) {
    if (enumClass != null) {
      HaxeBaseMemberModel member = enumClass.getHaxeClassModel().getMember(memberName, enumClass.getGenericResolver());
      if (member instanceof HaxeEnumValueConstructorModel enumValueModel) {
        return enumValueModel.getEnumValuePsi();
      }
      if (member instanceof HaxeEnumValueModel enumValueModel) {
        return enumValueModel.getEnumValuePsi();
      }
    }
    return null;
  }
}
