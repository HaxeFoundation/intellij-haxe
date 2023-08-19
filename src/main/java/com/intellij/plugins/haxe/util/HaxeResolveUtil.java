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
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeParenthesizedExpressionReferenceImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiCompositeElementImpl;
import com.intellij.plugins.haxe.model.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.DOC_COMMENT;
import static com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator.evaluate;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeResolveUtil {

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
    final HaxeClass[] components = PsiTreeUtil.getChildrenOfType(file, HaxeClass.class);
    if (components == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(components);
  }

  @Nullable
  public static HaxeClass findComponentDeclaration(@Nullable PsiFile file, @NotNull String componentName) {
    final List<HaxeClass> declarations = findComponentDeclarations(file);
    for (HaxeClass haxeClass : declarations) {
      final HaxeComponentName identifier = haxeClass.getComponentName();
      if (identifier != null && identifier.textMatches(componentName)) {
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

  public static List<HaxeNamedComponent> filterNamedComponentsByType(List<HaxeNamedComponent> result, final HaxeComponentType type) {
    return ContainerUtil.filter(result, new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return HaxeComponentType.typeOf(component) == type;
      }
    });
  }

  @Nullable
  public static HaxeNamedComponent findNamedSubComponent(@Nullable HaxeClass haxeClass, @NotNull final String name,
                                                         @Nullable HaxeGenericResolver resolver) {
    if (haxeClass == null) {
      return null;
    }
    final HaxeNamedComponent result = haxeClass.findHaxeMethodByName(name, resolver);
    return result != null ? result : haxeClass.findHaxeFieldByName(name, resolver);
  }

  /**
   * Gets the list of named components from the given classes and their supertypes, with duplicate named components removed.
   * If multiple components share the same name, *which* of those components
   * will be returned is indeterminate.
   *
   * @param resolver - map of generic type names to their concrete types.
   * @param rootHaxeClasses - which class(es) to gather components from.
   * @return - a list of named components defined in the rootHaxeClasses and their supertypes.
   */
  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(@Nullable HaxeGenericResolver resolver, @NotNull HaxeClass... rootHaxeClasses) {
    return findNamedSubComponents(true, resolver, rootHaxeClasses);
  }

  private static void addNotNullComponents(@NotNull List<HaxeNamedComponent> collection, @Nullable List<HaxeNamedComponent> possibles) {
    if (null == possibles) return;
    for (HaxeNamedComponent component : possibles) {
      if (component.getName() != null) {
        collection.add(component);
      }
    }
  }

  /**
   * Gets the named components from a class and all its supertypes.
   *
   * @param unique - whether multiple components sharing a name are included.  If true, *which* of the components
   *               sharing a name is returned is indeterminate.
   * @param resolver - map of generic type names to real types.
   * @param rootHaxeClasses - which class(es) to gather components from.
   * @return a list of named components defined in the rootHaxeClasses and their supertypes.
   */
  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(boolean unique, @Nullable HaxeGenericResolver resolver, @NotNull HaxeClass... rootHaxeClasses) {
    ProgressIndicatorProvider.checkCanceled();

    final List<HaxeNamedComponent> unfilteredResult = new ArrayList<>();
    final HashSet<HaxeClass> processed = new HashSet<>();
    final List<HaxeClass> temp = Arrays.asList(rootHaxeClasses.clone());
    final LinkedList<HaxeClass> classes = new LinkedList<>(temp);
    while (!classes.isEmpty()) {
      final HaxeClass haxeClass = classes.pollFirst();

      if (haxeClass == null) continue;

      addNotNullComponents(unfilteredResult, getNamedSubComponents(haxeClass));
      if (haxeClass.isAbstractType()) {
        if (null == resolver) {
          resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(haxeClass);
        }
        List<HaxeNamedComponent> subComponents = HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(haxeClass, resolver);
        addNotNullComponents(unfilteredResult, subComponents);
      }
      if (haxeClass.isTypeDef()) {
        if (null == resolver) {
          resolver = haxeClass.getMemberResolver(null);
        }else {
          resolver.addAll(haxeClass.getMemberResolver(null));
        }
      }

      List<HaxeType> baseTypes = new ArrayList<>();
      baseTypes.addAll(haxeClass.getHaxeExtendsList());
      baseTypes.addAll(haxeClass.getHaxeImplementsList());
      List<HaxeClass> baseClasses = tryResolveClassesByQName(baseTypes);
      if (haxeClass.isEnum() && !haxeClass.isAbstractType() && haxeClass.getContext() != null) {
        //Enums should provide the same methods as EnumValue
        baseClasses.add(HaxeEnumValueUtil.getEnumValueClass(haxeClass.getContext()).getHaxeClass());
      }
      for (HaxeClass baseClass : baseClasses) {
        if (processed.add(baseClass)) {
          classes.add(baseClass);
        }
      }
    }
    if (!unique) {
      return unfilteredResult;
    }

    return new ArrayList<>(namedComponentToMap(unfilteredResult).values());
  }

  public static Map<String, HaxeNamedComponent> namedComponentToMap(List<HaxeNamedComponent> unfilteredResult) {
    final Map<String, HaxeNamedComponent> result = new HashMap<String, HaxeNamedComponent>();
    for (HaxeNamedComponent haxeNamedComponent : unfilteredResult) {
      // need order
      if (result.containsKey(haxeNamedComponent.getName())) continue;
      result.put(haxeNamedComponent.getName(), haxeNamedComponent);
    }
    return result;
  }

  @NotNull
  public static List<HaxeNamedComponent> getNamedSubComponentsInOrder(HaxeClass haxeClass) {
    final List<HaxeNamedComponent> result = getNamedSubComponents(haxeClass);
    Collections.sort(result, new Comparator<HaxeNamedComponent>() {
      @Override
      public int compare(HaxeNamedComponent o1, HaxeNamedComponent o2) {
        return o1.getTextOffset() - o2.getTextOffset();
      }
    });
    return result;
  }

  public static List<HaxeNamedComponent> getAllNamedSubComponentsFromClassType(HaxeClass haxeClass, HaxeComponentType... fromTypes) {
    List<HaxeNamedComponent> components = getNamedSubComponents(haxeClass);
    List<HaxeComponentType> types = Arrays.asList(fromTypes);

    components.addAll(Stream.of(haxeClass.getSupers())
                        .filter(superComponent -> types.contains(HaxeComponentType.typeOf(superComponent)))
                        .map(superComponent -> getAllNamedSubComponentsFromClassType((HaxeClass)superComponent, fromTypes))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));

    if (types.contains(HaxeComponentType.typeOf(haxeClass))) {
      components.addAll(getNamedSubComponents(haxeClass));
    }

    return components;
  }

  /**
   * Gets all elements defined in a class' body.  This does NOT check superTypes for named elements.
   *
   * @param haxeClass to inspect
   * @return a list of all named components declared in the class' body.
   */
  public static List<HaxeNamedComponent> getNamedSubComponents(HaxeClass haxeClass) {
    PsiElement body = null;
    final HaxeComponentType type = HaxeComponentType.typeOf(haxeClass);
    if (type == HaxeComponentType.CLASS) {
      body = PsiTreeUtil.getChildOfAnyType(haxeClass, HaxeClassBody.class, HaxeExternClassDeclarationBody.class);
    } else if (type == HaxeComponentType.INTERFACE) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeInterfaceBody.class);
    } else if (type == HaxeComponentType.ENUM) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeEnumBody.class);
    } else if (haxeClass instanceof HaxeTypedefDeclaration) {
      final HaxeTypeOrAnonymous typeOrAnonymous = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
      if (typeOrAnonymous != null && typeOrAnonymous.getAnonymousType() != null) {
        HaxeAnonymousType anonymous = typeOrAnonymous.getAnonymousType();
        if (anonymous != null) {
          return getNamedSubComponents(anonymous);
        }
      } else if (typeOrAnonymous != null) {
        final HaxeClass typeClass = getHaxeClassResolveResult(typeOrAnonymous.getType()).getHaxeClass();
        assert typeClass != haxeClass;
        return getNamedSubComponents(typeClass);
      }
    }

    final List<HaxeNamedComponent> result = new ArrayList<HaxeNamedComponent>();
    if (haxeClass instanceof HaxeAnonymousType) {
      final HaxeAnonymousTypeBody anonymousTypeBody = ((HaxeAnonymousType)haxeClass).getAnonymousTypeBody();
      if (anonymousTypeBody != null) {
        final HaxeAnonymousTypeFieldList typeFieldList = anonymousTypeBody.getAnonymousTypeFieldList();
        if (typeFieldList != null) {
          result.addAll(typeFieldList.getAnonymousTypeFieldList());
        }
        body = anonymousTypeBody;
      }
    }
    if (body == null) {
      return result;
    }
    final HaxeNamedComponent[] namedComponents = PsiTreeUtil.getChildrenOfType(body, HaxeNamedComponent.class);
    if (namedComponents != null) {
      result.addAll(Arrays.asList(namedComponents));
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
    if (element instanceof HaxeType) {
      HaxeResolveResult result = tryResolveType((HaxeType)element, element, specialization);
      if (null == result.getHaxeClass() && specialization.containsKey(null, element.getText())) {
        return specialization.get(null, element.getText());
      }
      return result;
    }
    if (element instanceof HaxeComponentName) {
      return getHaxeClassResolveResult(element.getParent(), specialization);
    }
    if (element instanceof AbstractHaxeTypeDefImpl) {
      final AbstractHaxeTypeDefImpl typeDef = (AbstractHaxeTypeDefImpl)element;
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
        final HaxeExpression expression = iterable.getExpression();
        if (expression instanceof HaxeReference) {
          final HaxeResolveResult resolveResult = ((HaxeReference)expression).resolveHaxeClass();
          final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
          final HaxeGenericResolver resolver = resolveResult.getGenericResolver();
          final HaxeGenericSpecialization resultSpecialization = resolveResult.getSpecialization();

          // find keyValue iterator type
          HaxeResolveResult keyValueIteratorResult =
            getResolveMethodReturnType(resolver, resolveResultHaxeClass, "keyValueIterator",
                                       resultSpecialization.getInnerSpecialization(resolveResultHaxeClass));


          HaxeClass iteratorClass = keyValueIteratorResult.getHaxeClass();
          HaxeResolveResult iteratorResult =
            getResolveMethodReturnType(resolver, iteratorClass, "next", keyValueIteratorResult.getSpecialization());

          HaxeClass keyValueType = iteratorResult.getHaxeClass();

          if (element instanceof HaxeIteratorkey) {
           return  resolveFieldType(resolver, keyValueType, "key",  iteratorResult.getSpecialization());
          }

          if (element instanceof  HaxeIteratorValue) {
            return  resolveFieldType(resolver, keyValueType, "value", iteratorResult.getSpecialization());
          }
        }
        return HaxeResolveResult.EMPTY;
    }
    if (element instanceof HaxeForStatement) {
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      if (iterable == null) {
        // iterable is @Nullable
        // (sometimes when you're typing for statement it becames null for short time)
        return HaxeResolveResult.EMPTY;
      }
      final HaxeExpression expression = iterable.getExpression();
      if (expression instanceof HaxeReference) {

        final HaxeResolveResult resolveResult = ((HaxeReference)expression).resolveHaxeClass();
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
    final HaxeVarInit varInit = PsiTreeUtil.getChildOfType(element, HaxeVarInit.class);
    final HaxeExpression initExpression = varInit == null ? null : varInit.getExpression();
    if (initExpression instanceof HaxeReference) {
      result = ((HaxeReference)initExpression).resolveHaxeClass();
      result.specialize(initExpression);
      return result;
    }

    if (element instanceof  HaxeValueExpression valueExpression) {
      result = resolveValueExpressionClass(valueExpression, specialization);
      if (result != null) {
        return result;
      }
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

    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(element);
    ResultHolder result = evaluate(element, context, specialization.toGenericResolver(valueExpression)).result;
    if (result.getClassType() != null) {
      return result.getClassType().asResolveResult();
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

    if (element instanceof HaxeMethodDeclaration) {
      HaxeMethodDeclaration method = (HaxeMethodDeclaration)element;
      HaxeMethodModel model = new HaxeMethodModel(method);

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
  public static HaxeClass tryResolveClassByQName(@Nullable PsiElement type) {
    if (type == null || type.getContext() == null) {
      return null;
    }

    final String name = getQName(type);
    HaxeClass result = name == null ? tryResolveClassByQNameWhenGetQNameFail(type) : findClassByQName(name, type.getContext());
    result = result != null ? result : findClassByQNameInSuperPackages(type);
    return result;
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
    return null;
  }

  @Nullable
  private static String getQName(@NotNull PsiElement type) {
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
        result = searchInSameFile(fileModel, className);
        if (result == null) result = searchInImports(fileModel, className);
        if (result == null) result = searchInSamePackage(fileModel, className);
      }
    } else {
      className = tryResolveFullyQualifiedHaxeReferenceExpression(type);
      result = findClassByQName(className, type.getContext());
    }

    result = result != null ? result : findClassByQName(className, type.getContext());

    return result instanceof HaxeClass ? (HaxeClass)result : null;
  }

  @Nullable
  public static PsiElement searchInSameFile(@NotNull HaxeFileModel file, @NotNull String name) {
    List<HaxeClassModel> models = file.getClassModels();
    final Stream<HaxeClassModel> classesStream = models.stream().filter(model -> name.equals(model.getName()));
    final Stream<HaxeEnumValueModel> enumsStream = models.stream().filter(model -> model instanceof HaxeEnumModel)
      .map(model -> ((HaxeEnumModel)model).getValue(name))
      .filter(Objects::nonNull);

    final HaxeModel result = Stream.concat(classesStream, enumsStream)
      .findFirst()
      .orElse(null);

    return result != null ? result.getBasePsi() : null;
  }

  @Nullable
  public static PsiElement searchInImports(HaxeFileModel file, String name) {
    PsiElement found = searchInSpecifiedImports(file, name);
    if (null == found) found = searchInDirectoryImports(file, name);
    return found;
  }

  @Nullable
  public static PsiElement searchInSpecifiedImports(HaxeFileModel file, String name) {
    List<HaxeImportableModel> models = file.getOrderedImportAndUsingModels();
    for (int i = models.size() - 1; i >= 0; i--) {
      HaxeImportableModel model = models.get(i);
      PsiElement element = model.exposeByName(name);
      if(element != null) {
        return element;
      }
    }
    return null;
  }

  /**
   * Searches for import.hx files between the file's directory and the source root,
   * examining each for matches.
   *
   * @param file The file that has import statements to match.
   * @param name The name of the Type that we are searching for.
   * @return The PSI element for the Type, if found; null, otherwise.
   */
  @Nullable
  private static PsiElement searchInDirectoryImports(HaxeFileModel file, String name) {

    final PsiElement[] found = new PsiElement[1];
    walkDirectoryImports(file, (importModel) -> {
      found[0] = searchInSpecifiedImports(importModel, name);
      return (null == found[0]);
    });
    return found[0];
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
  public static PsiElement searchInSamePackage(@NotNull HaxeFileModel file, @NotNull String name) {
    final HaxePackageModel packageModel = file.getPackageModel();
    if (packageModel != null) {
      for (HaxeModel model : packageModel.getExposedMembers()) {
        if (name.equals(model.getName())) {
          return model.getBasePsi();
        }
      }
    }
    return null;
  }

  public static String getQName(PsiElement[] fileChildren, final String result, boolean searchInSamePackage) {

    HaxeClass classForType = null;
    for(PsiElement child: fileChildren) {
      if( child instanceof HaxeClass && result.equals(((HaxeClass)child).getName())){
        classForType = (HaxeClass)child;
        break;
      }
    }

    if (classForType != null) {
      return classForType.getQualifiedName();
    }

    final HaxeImportStatement importStatement =
      (HaxeImportStatement)(StreamUtil.reverse(Arrays.stream(fileChildren))
                              .filter(element ->
                                        element instanceof HaxeImportStatement &&
                                        ((HaxeImportStatement)element).getModel().exposeByName(result) != null)
                              .findFirst()
                              .orElse(null));

    final HaxeExpression importStatementExpression = importStatement == null ? null : importStatement.getReferenceExpression();
    if (importStatementExpression != null) {
      return importStatementExpression.getText();
    }

    if (searchInSamePackage && fileChildren.length > 0) {
      final HaxeFileModel fileModel = HaxeFileModel.fromElement(fileChildren[0]);
      if (fileModel != null) {
        final HaxePackageModel packageModel = fileModel.getPackageModel();
        if (packageModel != null) {
          final HaxeClassModel classModel = packageModel.getClassModel(result);
          if (classModel != null) {
            return classModel.haxeClass.getQualifiedName();
          }
        }
      }
    }

    return result;
  }

  @Nullable
  public static PsiComment findDocumentation(HaxeNamedComponent element) {
    PsiElement candidate =  UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(element, true);;
    // we search backwards for comments, and skipping metas etc
    while (candidate != null) {
      // if we find other named components while searching we abort as whatever
      // doc we might find would belong to that named component and not ours.
      if (candidate instanceof  HaxeNamedComponent) {
        return null;
      }
      if (candidate instanceof  PsiComment psiComment) {
        IElementType type = psiComment.getTokenType();
        if (type == DOC_COMMENT) {
          return psiComment;
        }
      }
      candidate = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(candidate, true);
    }
    return null;
  }

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
      ResultHolder result = evaluate(expression, new HaxeExpressionEvaluatorContext(expression), resolver).result;
      // null type "hack" : if nullType unwrap to real type
      if(result.getType().isNullType()) {
        result = result.getClassType().getSpecifics()[0];
      }
      if (result.isEnum() && result.getClassType() != null) {
        return result.getClassType();
      }
    }
    return null;
  }

  public static HaxeEnumValueDeclaration resolveExtractorEnumValueDeclaration(SpecificHaxeClassReference enumClass, HaxeEnumArgumentExtractor extractor) {
    if (enumClass != null) {
      String memberName = extractor.getEnumValueReference().getReferenceExpression().getIdentifier().getText();
      HaxeMemberModel member = enumClass.getHaxeClassModel().getMember(memberName, enumClass.getGenericResolver());
      if (member instanceof HaxeEnumValueModel enumValueModel) {
        return enumValueModel.getEnumValuePsi();
      }
    }
    return null;
  }
}
