/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2019 Eric Bishton
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
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
import gnu.trove.THashSet;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

  static {
    LOG.setLevel(Level.INFO);
//    LOG.setLevel(Level.TRACE);
  }  // We want warnings to get out to the log.

  @Nullable
  public static HaxeReference getLeftReference(@Nullable final PsiElement node) {
    if (node == null) return null;

    PsiElement leftExpression = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(node);
    PsiElement dot = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(leftExpression);
    //PsiElement identifier = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(dot);

    if (null == dot || dot.getNode().getElementType() != HaxeTokenTypes.ODOT) {
      return null;
    }

    if (leftExpression instanceof HaxeReference) return (HaxeReference)leftExpression;
    if (leftExpression instanceof HaxeParenthesizedExpression && ((HaxeParenthesizedExpression)leftExpression).getTypeCheckExpr() != null) {
      return ((HaxeParenthesizedExpression)leftExpression).getTypeCheckExpr();
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
      if (identifier != null && componentName.equals(identifier.getText())) {
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
  public static HaxeNamedComponent findNamedSubComponent(@Nullable HaxeClass haxeClass, @NotNull final String name) {
    if (haxeClass == null) {
      return null;
    }
    final HaxeNamedComponent result = haxeClass.findHaxeMethodByName(name);
    return result != null ? result : haxeClass.findHaxeFieldByName(name);
  }

  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(@NotNull HaxeClass... rootHaxeClasses) {
    return findNamedSubComponents(true, rootHaxeClasses);
  }

  private static void addNotNullComponents(@NotNull List<HaxeNamedComponent> collection, @Nullable List<HaxeNamedComponent> possibles) {
    if (null == possibles) return;
    for (HaxeNamedComponent component : possibles) {
      if (component.getName() != null) {
        collection.add(component);
      }
    }
  }

  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(boolean unique, @NotNull HaxeClass... rootHaxeClasses) {
    final List<HaxeNamedComponent> unfilteredResult = new ArrayList<>();
    final LinkedList<HaxeClass> classes = new LinkedList<>();
    final HashSet<HaxeClass> processed = new HashSet<>();
    classes.addAll(Arrays.asList(rootHaxeClasses));
    while (!classes.isEmpty()) {
      final HaxeClass haxeClass = classes.pollFirst();

      addNotNullComponents(unfilteredResult, getNamedSubComponents(haxeClass));
      if (haxeClass.isAbstract()) {
        HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(haxeClass);
        List<HaxeNamedComponent> subComponents = HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(haxeClass, resolver);
        addNotNullComponents(unfilteredResult, subComponents);
      }

      List<HaxeType> baseTypes = new ArrayList<>();
      baseTypes.addAll(haxeClass.getHaxeExtendsList());
      baseTypes.addAll(haxeClass.getHaxeImplementsList());
      List<HaxeClass> baseClasses = tyrResolveClassesByQName(baseTypes);
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
        body = anonymousTypeBody.getInterfaceBody();
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
   * @return - a fully resolved superclass containing the element.  If the element is not contained in a superclass,
   *           HaxeClassResolveResult.EMPTY is returned.
   */
  @NotNull
  public static HaxeClassResolveResult getSuperclassResolveResult(@Nullable PsiElement element,
                                                                  @Nullable PsiElement context,
                                                                  @Nullable HaxeGenericSpecialization contextSpecialization) {
    if (null == element || null == context) return HaxeClassResolveResult.EMPTY;

    HaxeClassResolveResult contextClassResult = getHaxeClassResolveResult(context, contextSpecialization);
    if (HaxeClassResolveResult.EMPTY == contextClassResult) {
      return HaxeClassResolveResult.EMPTY;
    }

    HaxeClassResolveResult elementClassResult = getHaxeClassResolveResult(element);
    if (HaxeClassResolveResult.EMPTY == elementClassResult) {
      HaxeClass elementPsiClass = (HaxeClass) UsefulPsiTreeUtil.getParentOfType(element, HaxeClass.class);
      if (null == elementPsiClass)
        return HaxeClassResolveResult.EMPTY;
      elementClassResult = HaxeClassResolveResult.create(elementPsiClass, null);
    }

    HaxeClass contextClass = contextClassResult.getHaxeClass();
    HaxeClass elementClass = elementClassResult.getHaxeClass();
    return (null == elementClass || null == contextClass)
           ? HaxeClassResolveResult.EMPTY
           : resolveSuperclass(elementClass, contextClass, contextClassResult.getSpecialization()); //contextSpecialization);
  }

  @NotNull
  private static HaxeClassResolveResult resolveSuperclass(@NotNull HaxeClass elementClass,
                                                          @NotNull HaxeClass contextClass,
                                                          @Nullable HaxeGenericSpecialization contextSpecialization) {
    if (elementClass.equals(contextClass)) {
      return HaxeClassResolveResult.create(elementClass, contextSpecialization);
    }

    HaxeClassResolveResult specializedResult = HaxeClassResolveResult.create(contextClass, contextSpecialization.getInnerSpecialization(contextClass));
    specializedResult.specialize(elementClass);

    PsiClass[] superClasses = contextClass.getSupers();
    for (PsiClass psiClass : superClasses) {
      if (psiClass instanceof HaxeClass) {
        HaxeClass clazz = (HaxeClass) psiClass;

        HaxeClassResolveResult superResult = HaxeClassResolveResult.create(clazz, specializedResult.getSpecialization());

        if (clazz.equals(elementClass)) {
          return superResult;
        }
        if (clazz.isInheritor(elementClass, true)) {
          return resolveSuperclass(elementClass, superResult.getHaxeClass(), superResult.getSpecialization());
        }
      }
    }
    return HaxeClassResolveResult.EMPTY;
  }

  private static ThreadLocal<Stack<PsiElement>> resolveStack = new ThreadLocal<Stack<PsiElement>>() {
    @Override
    protected Stack<PsiElement> initialValue() {
      return new Stack<PsiElement>();
    }
  };

  private static void traceMessage(String message, int depth) {
    if (LOG.isTraceEnabled()) {
      StringBuilder out = new StringBuilder();

      out.append(Thread.currentThread().getId()); // Name());
      out.append(' ');

      while (0 < depth--) {
        out.append("  ");
      }
      out.append(message);
      LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), out.toString());
    }
  }

  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element) {
    return getHaxeClassResolveResult(element, null);
  }

  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element,
                                                                 @Nullable HaxeGenericSpecialization specialization) {
    if (specialization == null) {
      specialization = new HaxeGenericSpecialization();
    }

    final Stack<PsiElement> stack = resolveStack.get();

    if (element == null || element instanceof PsiPackage) {
      traceMessage("Cannot resolve " + (element == null ? "null value" : "package statement"), stack.size());
      return HaxeClassResolveResult.EMPTY;
    }

    if (stack.search(element) > 0) {
      // We're already trying to resolve this element.  Prevent stack overflow.
      String msg = "Cannot resolve recursive/cyclic definition of " + element.getText()
                   + ", found at " + HaxeDebugUtil.elementLocation(element);
      traceMessage(msg, stack.size());
      LOG.warn(msg);
      return HaxeClassResolveResult.EMPTY;
    }

    try {
      stack.push(element);

      String elementString = null;
      if (LOG.isTraceEnabled()) {
        elementString = element instanceof HaxePsiCompositeElementImpl
                        ? ((HaxePsiCompositeElementImpl)element).toDebugString()
                        : element.toString();
        elementString = HaxeStringUtil.elideBetween(elementString, '{', '}');
        elementString = HaxeStringUtil.elide(elementString, 80);
        traceMessage("Resolving: " + elementString, stack.size()-1);
      }

      HaxeClassResolveResult result = getHaxeClassResolveResultInternal(element, specialization);

      if (LOG.isDebugEnabled()) {
        String msg = "Element " + elementString + " resolved as " + result.toString();
        if (LOG.isTraceEnabled()) {
          traceMessage(msg, stack.size()-1);
        } else if (LOG.isDebugEnabled()) {
          LOG.debug(msg);
        }
      }
      return result;
    }
    finally {
      try {
        stack.pop();
      }
      catch (EmptyStackException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Unexpected excessive stack pop. " + e.toString());
        }
      }
    }
  }

  @NotNull
  private static HaxeClassResolveResult getHaxeClassResolveResultInternal(@Nullable PsiElement element,
                                                                          @Nullable HaxeGenericSpecialization specialization) {
    if (element instanceof HaxeType) {
      HaxeClassResolveResult result = tryResolveType((HaxeType)element, element, specialization);
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
      return typeDef.getTargetClass(specialization);
    }
    if (element instanceof HaxeClass) {
      final HaxeClass haxeClass = (HaxeClass)element;
      return HaxeClassResolveResult.create(haxeClass, specialization);
    }
    if (element instanceof HaxeForStatement) {
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      if (iterable == null) {
        // iterable is @Nullable
        // (sometimes when you're typing for statement it becames null for short time)
        return HaxeClassResolveResult.EMPTY;
      }
      final HaxeExpression expression = iterable.getExpression();
      if (expression instanceof HaxeReference) {
        final HaxeClassResolveResult resolveResult = ((HaxeReference)expression).resolveHaxeClass();
        final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
        // try next
        HaxeClassResolveResult result =
          getHaxeClassResolveResult(resolveResultHaxeClass == null ? null : resolveResultHaxeClass.findHaxeMethodByName("next"),
                                    resolveResult.getSpecialization());
        if (result.getHaxeClass() != null) {
          return result;
        }
        // try iterator
        HaxeClassResolveResult iteratorResult =
          getHaxeClassResolveResult(resolveResultHaxeClass == null ? null : resolveResultHaxeClass.findHaxeMethodByName("iterator"),
                                    resolveResult.getSpecialization().getInnerSpecialization(resolveResultHaxeClass));
        HaxeClass iteratorResultHaxeClass = iteratorResult.getHaxeClass();
        // Now, look for iterator's next
        result =
          getHaxeClassResolveResult(iteratorResultHaxeClass == null ? null : iteratorResultHaxeClass.findHaxeMethodByName("next"),
                                    iteratorResult.getSpecialization());

        return result;
      }
      return HaxeClassResolveResult.EMPTY;
    }

    HaxeClassResolveResult result = tryResolveClassByTypeTag(element, specialization);
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
    return getHaxeClassResolveResult(initExpression);
  }

  @NotNull
  public static HaxeClassResolveResult tryResolveType(HaxeType type,
                                                      PsiElement specializationContext,
                                                      HaxeGenericSpecialization specialization) {

    HaxeClass haxeClass = type == null ? null : tryResolveClassByQName(type);
    if (haxeClass == null && type != null && specialization.containsKey(specializationContext, type.getText())) {
      return specialization.get(specializationContext, type.getText());
    }

    if (haxeClass instanceof HaxeTypedefDeclaration) {
      HaxeClassResolveResult temp = HaxeClassResolveResult.create(haxeClass, specialization);
      temp.specializeByParameters(type.getTypeParam());
      specialization = temp.getSpecialization();
    }

    HaxeClassResolveResult result = getHaxeClassResolveResult(haxeClass, specialization.getInnerSpecialization(specializationContext));
    if (result.getHaxeClass() != null) {
      result.specializeByParameters(type == null ? null : type.getTypeParam());
    }

    return result;
  }


  @NotNull
  public static HaxeClassResolveResult tryResolveClassByTypeTag(PsiElement element,
                                                                HaxeGenericSpecialization specialization) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = (typeTag != null) ? typeTag.getTypeOrAnonymous() : null;
    final HaxeType type = (typeOrAnonymous != null) ? typeOrAnonymous.getType() :
                          ((element instanceof HaxeType) ? (HaxeType)element : null);

    HaxeClassResolveResult resolvedType = tryResolveType(type, element, specialization);
    if (HaxeClassResolveResult.EMPTY != resolvedType) {
      return resolvedType;
    }

    if (typeTag != null) {
      return tryResolveFunctionType(typeTag.getFunctionType(), specialization);
    }

    return HaxeClassResolveResult.EMPTY;
  }

  @NotNull
  public static HaxeClassResolveResult tryResolveClassByInferringMethodReturnType(@Nullable PsiElement element,
                                                                                  HaxeGenericSpecialization specialization) {
    if (null == element) {
      return HaxeClassResolveResult.EMPTY;
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

          return HaxeClassResolveResult.create(haxeClass, resultSpecialization);
        } else {
          // It's a function return type.
          // TODO: Implement function return types.
          LOG.warn("Function return types not implemented in the resolver yet.");
        }
      }
    }
    return HaxeClassResolveResult.EMPTY;
  }

  private static HaxeClassResolveResult tryResolveFunctionType(@Nullable HaxeFunctionType functionType,
                                                               HaxeGenericSpecialization specialization) {
    if (functionType == null) {
      return HaxeClassResolveResult.EMPTY;
    }

    final HaxeFunctionReturnType returnType = functionType.getFunctionReturnType();
    if (returnType == null || returnType.getTypeOrAnonymous() == null) { return HaxeClassResolveResult.EMPTY; }
    // TODO Stub classes must be introduced in the near future to cover cases where Function is a return type, not a class type or anonymous structure.
    return tryResolveClassByTypeTag(returnType.getTypeOrAnonymous().getType(), specialization);
  }

  @NotNull
  public static List<HaxeClass> tyrResolveClassesByQName(@NotNull List<HaxeType> types) {
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
    HaxeImportModel importModel = StreamUtil.reverse(file.getImportModels().stream())
      .filter(model -> {
        PsiElement exposedItem = model.exposeByName(name);
        return exposedItem != null;
      })
      .findFirst().orElse(null);

    if (importModel != null) {
      return importModel.exposeByName(name);
    }
    return null;
  }

  @Nullable
  public static PsiElement searchInSamePackage(@NotNull HaxeFileModel file, @NotNull String name) {
    final HaxePackageModel packageModel = file.getPackageModel();
    HaxeModel result = null;

    if (packageModel != null) {
      result = packageModel.getExposedMembers().stream()
        .filter(model -> name.equals(model.getName()))
        .findFirst()
        .orElse(null);
    }

    return result != null ? result.getBasePsi() : null;
  }

  public static String getQName(PsiElement[] fileChildren, final String result, boolean searchInSamePackage) {
    final HaxeClass classForType = (HaxeClass)Arrays.stream(fileChildren)
      .filter(child -> child instanceof HaxeClass && result.equals(((HaxeClass)child).getName()))
      .findFirst()
      .orElse(null);

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
    final PsiElement candidate = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(element, true);
    if (candidate instanceof PsiComment) {
      return (PsiComment)candidate;
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
    final Set<IElementType> resultSet = new THashSet<IElementType>();
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
}
