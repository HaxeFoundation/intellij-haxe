/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeComponentFileNameIndex;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeParameterListPsiMixinImpl;
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

import static com.intellij.util.containers.ContainerUtil.getFirstItem;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static { LOG.setLevel(Level.INFO); }  // We want warnings to get out to the log.

  @Nullable
  public static HaxeReference getLeftReference(@Nullable final PsiElement node) {
    if (node == null) return null;

    for (PsiElement sibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(node, true);
         sibling != null;
         sibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(sibling, true))
    {
      if (".".equals(sibling.getText())) continue;

      HaxeExpression tmpExpression = null;
      if (sibling instanceof HaxeExpression) {
        tmpExpression = (HaxeExpression) sibling;
      }

      if (tmpExpression != null && tmpExpression instanceof HaxeParenthesizedExpression) {
        tmpExpression = ((HaxeParenthesizedExpression) tmpExpression).getTypeCheckExpr();
      }

      HaxeReference theHaxeRef = null;
      if (tmpExpression != null) {
        if (tmpExpression instanceof HaxeAssignExpression) {
          if (2 == ((HaxeAssignExpression) tmpExpression).getExpressionList().size()) {
            final HaxeExpression rhsHaxeExpr = ((HaxeAssignExpression) tmpExpression).getExpressionList().get(1);
            final HaxeReference rhsHaxeReference = (HaxeReference) rhsHaxeExpr.getReference();
            if ((rhsHaxeReference != null) && (rhsHaxeReference.resolveHaxeClass().getHaxeClass() != null)) {
              theHaxeRef = rhsHaxeReference;
            }
            else {
              final HaxeExpression lhsHaxeExpr = ((HaxeAssignExpression) tmpExpression).getExpressionList().get(0);
              final HaxeReference lhsHaxeReference = (HaxeReference) lhsHaxeExpr.getReference();
              if ((lhsHaxeReference != null) && (lhsHaxeReference.resolveHaxeClass().getHaxeClass() != null)) {
                theHaxeRef = lhsHaxeReference;
              }
            }
          }
        }
        else
        if (tmpExpression instanceof HaxeReferenceExpression) {
          theHaxeRef = (HaxeReference) tmpExpression; // TODO: FIX: not correct for identifiers - HaxeReferenceExpresssion.getIdentifier() -- but, that's not a reference?!
        }
        else {
          theHaxeRef = (HaxeReference) tmpExpression.getReference();
        }
      }

      if (theHaxeRef != null && theHaxeRef != sibling) {
        sibling = theHaxeRef;
      }

      return ((sibling instanceof HaxeReference) && (sibling != node)) ? (HaxeReference) sibling : null;
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
    final List<VirtualFile> classFiles = HaxeComponentFileNameIndex.
      getFilesNameByQName(qName, scope);
    final Pair<String, String> qNamePair = splitQName(qName);
    for (VirtualFile classFile : classFiles) {
      final HaxeClass componentPsiElement = findComponentDeclaration(psiManager.findFile(classFile), qNamePair.getSecond());
      if (componentPsiElement != null) {
        return componentPsiElement;
      }
    }
    return null;
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
    List<? extends HaxeInherit> ext = null == extendsList  ? null : extendsList.getExtendsDeclarationList();
    return findExtendsImplementsListImpl(ext);
  }

  public static List<HaxeType> getImplementsList(@Nullable HaxeInheritList extendsList) {
    List<? extends HaxeInherit> ext = null == extendsList  ? null : extendsList.getImplementsDeclarationList();
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

  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(boolean unique, @NotNull HaxeClass... rootHaxeClasses) {
    final List<HaxeNamedComponent> unfilteredResult = new ArrayList<HaxeNamedComponent>();
    final LinkedList<HaxeClass> classes = new LinkedList<HaxeClass>();
    final HashSet<HaxeClass> processed = new HashSet<HaxeClass>();
    classes.addAll(Arrays.asList(rootHaxeClasses));
    while (!classes.isEmpty()) {
      final HaxeClass haxeClass = classes.pollFirst();
      for (HaxeNamedComponent namedComponent : getNamedSubComponents(haxeClass)) {
        if (namedComponent.getName() != null) {
          unfilteredResult.add(namedComponent);
        }
      }

      List<HaxeType> baseTypes = new ArrayList<HaxeType>();
      baseTypes.addAll(haxeClass.getHaxeExtendsList());
      baseTypes.addAll(haxeClass.getHaxeImplementsList());
      List<HaxeClass> baseClasses = tyrResolveClassesByQName(baseTypes);
      for(HaxeClass baseClass : baseClasses) {
        if(processed.add(baseClass)) {
          classes.add(baseClass);
        }
      }

    }
    if (!unique) {
      return unfilteredResult;
    }

    return new ArrayList<HaxeNamedComponent>(namedComponentToMap(unfilteredResult).values());
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
    }
    else if (type == HaxeComponentType.INTERFACE) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeInterfaceBody.class);
    }
    else if (type == HaxeComponentType.ENUM) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeEnumBody.class);
    }
    else if (haxeClass instanceof HaxeTypedefDeclaration) {
      final HaxeTypeOrAnonymous typeOrAnonymous = getFirstItem(((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymousList());
      if (typeOrAnonymous != null && typeOrAnonymous.getAnonymousType() != null) {
        HaxeAnonymousType anonymous = typeOrAnonymous.getAnonymousType();
        if(anonymous != null) {
          return getNamedSubComponents(anonymous);
        }
      }
      else if (typeOrAnonymous != null) {
        final HaxeClass typeClass = getHaxeClassResolveResult(typeOrAnonymous.getType()).getHaxeClass();
        assert typeClass != haxeClass;
        return getNamedSubComponents(typeClass);
      }
    }

    final List<HaxeNamedComponent> result = new ArrayList<HaxeNamedComponent>();
    if (haxeClass instanceof HaxeAnonymousType) {
      final HaxeAnonymousTypeFieldList typeFieldList = ((HaxeAnonymousType)haxeClass).getAnonymousTypeBody().getAnonymousTypeFieldList();
      if (typeFieldList != null) {
        result.addAll(typeFieldList.getAnonymousTypeFieldList());
      }
      body = ((HaxeAnonymousType)haxeClass).getAnonymousTypeBody().getInterfaceBody();
    }
    if (body == null) {
      return result;
    }
    final HaxeNamedComponent[] namedComponents = PsiTreeUtil.getChildrenOfType(body, HaxeNamedComponent.class);
    if (namedComponents != null) {
      for (HaxeNamedComponent namedComponent : namedComponents) {
        // Variable declarations are named components, but don't have the
        // same Psi structure as most. So, go find the actual sub-element(s)
        // that are named (that themselves have COMPONENT_NAME sub-elements).
        if (namedComponent instanceof HaxeVarDeclaration) {
          HaxeVarDeclaration varDeclaration = (HaxeVarDeclaration)namedComponent;
          result.add(varDeclaration.getVarDeclarationPart());
        }
        else {
          result.add(namedComponent);
        }
      }
    }


    return result;
  }

  public static List<HaxeVarDeclaration> getClassVarDeclarations(HaxeClass haxeClass) {
    PsiElement body = null;
    final HaxeComponentType type = HaxeComponentType.typeOf(haxeClass);
    if (type == HaxeComponentType.CLASS) {
      body = PsiTreeUtil.getChildOfAnyType(haxeClass, HaxeClassBody.class, HaxeExternClassDeclarationBody.class);
    }

    final List<HaxeVarDeclaration> result = new ArrayList<HaxeVarDeclaration>();

    if (body == null) {
      return result;
    }

    final HaxeVarDeclaration[] variables = PsiTreeUtil.getChildrenOfType(body, HaxeVarDeclaration.class);

    if (variables == null) {
      return result;
    }
    Collections.addAll(result, variables);
    return result;
  }

  @Nullable
  public static List<HaxeType> getFunctionParameters(HaxeNamedComponent component) {
    if (HaxeComponentType.typeOf(component) != HaxeComponentType.METHOD) {
      return null;
    }
    final HaxeParameterListPsiMixinImpl parameterList = PsiTreeUtil.getChildOfType(component, HaxeParameterListPsiMixinImpl.class);
    if (parameterList == null || parameterList.getParametersCount() == 0) {
      return Collections.emptyList();
    }
    return ContainerUtil.map(parameterList.getParametersAsList(), new Function<HaxeParameter, HaxeType>() {
      @Override
      public HaxeType fun(HaxeParameter parameter) {
        final HaxeTypeTag typeTag = parameter.getTypeTag();
        if (null == typeTag) return null;
        final HaxeTypeOrAnonymous typeOrAnonymous = getFirstItem(typeTag.getTypeOrAnonymousList());
        return (null == typeOrAnonymous) ? null : typeOrAnonymous.getType();
      }
    });
  }


  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element) {
    return getHaxeClassResolveResult(element, new HaxeGenericSpecialization());
  }

  private static ThreadLocal<Stack<PsiElement>> resolveStack = new ThreadLocal<Stack<PsiElement>>() {
    @Override
    protected Stack<PsiElement> initialValue() {
      return new Stack<PsiElement>();
    }
  };

  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element,
                                                                 @NotNull HaxeGenericSpecialization specialization) {
    if (element == null || element instanceof PsiPackage) {
      return HaxeClassResolveResult.EMPTY;
    }

    final Stack<PsiElement> stack = resolveStack.get();
    if (stack.search(element) > 0) {
      // We're already trying to resolve this element.  Prevent stack overflow.
      LOG.warn("Cannot resolve recursive/cyclic definition of " + element.getText()
               + "found at " + HaxeDebugUtil.elementLocation(element));
      return HaxeClassResolveResult.EMPTY;
    }

    try {
      stack.push(element);

      if (element instanceof HaxeComponentName) {
        return getHaxeClassResolveResult(element.getParent(), specialization);
      }
      if (element instanceof AbstractHaxeTypeDefImpl) {
        final AbstractHaxeTypeDefImpl typeDef = (AbstractHaxeTypeDefImpl)element;
        return typeDef.getTargetClass(specialization);
      }
      if (element instanceof HaxeClass) {
        final HaxeClass haxeClass = (HaxeClass)element;
        return HaxeClassResolveResult.create(haxeClass);
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
          result = getHaxeClassResolveResult(iteratorResultHaxeClass == null ? null : iteratorResultHaxeClass.findHaxeMethodByName("next"),
                                             iteratorResult.getSpecialization());

          return result != null ? result : HaxeClassResolveResult.EMPTY;
        }
        return HaxeClassResolveResult.EMPTY;
      }

      HaxeClassResolveResult result = tryResolveClassByTypeTag(element, specialization);
      if (result.getHaxeClass() != null) {
        return result;
      }

      result = HaxeAbstractEnumUtil.resolveFieldType(element);
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
    finally {
      try {
        stack.pop();
      } catch(EmptyStackException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Unexpected excessive stack pop. " + e.toString());
        }
      }
    }
  }

  @NotNull
  public static HaxeClassResolveResult tryResolveClassByTypeTag(PsiElement element,
                                                                HaxeGenericSpecialization specialization) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = (typeTag != null) ? getFirstItem(typeTag.getTypeOrAnonymousList()) : null;
    final HaxeType type = (typeOrAnonymous != null) ? typeOrAnonymous.getType() :
                            ((element instanceof HaxeType) ? (HaxeType)element : null);

    HaxeClass haxeClass = type == null ? null : tryResolveClassByQName(type);
    if (haxeClass == null && type != null && specialization.containsKey(element, type.getText())) {
      return specialization.get(element, type.getText());
    }

    if(haxeClass instanceof HaxeTypedefDeclaration) {
      HaxeClassResolveResult temp = HaxeClassResolveResult.create(haxeClass, specialization);
      temp.specializeByParameters(type.getTypeParam());
      specialization = temp.getSpecialization();
    }

    HaxeClassResolveResult result = getHaxeClassResolveResult(haxeClass, specialization.getInnerSpecialization(element));
    if (result.getHaxeClass() != null) {
      result.specializeByParameters(type == null ? null : type.getTypeParam());
      return result;
    }

    if (typeTag != null) {
      return tryResolveFunctionType(getFirstItem(typeTag.getFunctionTypeList()), specialization);
    }

    return HaxeClassResolveResult.EMPTY;
  }

  private static HaxeClassResolveResult tryResolveFunctionType(@Nullable HaxeFunctionType functionType,
                                                               HaxeGenericSpecialization specialization) {
    if (functionType == null) {
      return HaxeClassResolveResult.EMPTY;
    }
    final HaxeTypeOrAnonymous returnTypeOrAnonymous =
      functionType.getTypeOrAnonymousList().get(functionType.getTypeOrAnonymousList().size() - 1);
    final HaxeClassResolveResult result = tryResolveClassByTypeTag(returnTypeOrAnonymous.getType(), specialization);
    functionType = functionType.getFunctionType();
    while (functionType != null) {
      // todo: anonymous types :(
      final List<HaxeTypeOrAnonymous> typeList = functionType.getTypeOrAnonymousList();
      Collections.reverse(typeList);
      for (HaxeTypeOrAnonymous typeOrAnonymous : typeList) {
        result.getFunctionTypes().add(tryResolveClassByTypeTag(typeOrAnonymous.getType(), specialization));
      }
      functionType = functionType.getFunctionType();
    }
    Collections.reverse(result.getFunctionTypes());
    return result;
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
    result = result != null ? result : tryFindHelper(type);
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
  private static HaxeClass tryFindHelper(PsiElement element) {
    // issue #435: don't use getText(), find "Ref" instead of "Ref<String>"
    String className = element.getText();
    if (element instanceof HaxeType) {
      className = ((HaxeType)element).getReferenceExpression().getText();
    }
    final HaxeClass ownerClass = findClassByQName(UsefulPsiTreeUtil.findHelperOwnerQName(element, className), element);
    return ownerClass == null ? null : findComponentDeclaration(ownerClass.getContainingFile(), className);
  }

  @Nullable
  private static String getQName(@NotNull PsiElement type) {
    HaxeImportStatementWithInSupport importStatementWithInSupport = PsiTreeUtil.getParentOfType(type,
                                                                                                HaxeImportStatementWithInSupport.class,
                                                                                                false);
    if (importStatementWithInSupport != null) {
      return importStatementWithInSupport.getReferenceExpression().getText();
    }

    HaxeUsingStatement usingStatement = PsiTreeUtil.getParentOfType(type, HaxeUsingStatement.class, false);
    if (usingStatement != null) {
      HaxeReferenceExpression expression = usingStatement.getReferenceExpression();
      return expression == null? null : expression.getText();
    }

    return null;
  }

  @Nullable
  private static HaxeClass tryResolveClassByQNameWhenGetQNameFail(@NotNull PsiElement type) {
    if (type instanceof HaxeType) {
      type = ((HaxeType)type).getReferenceExpression();
    }

    String name = type.getText();
    HaxeClass result = null;

    //1. try searchInSamePackage, ex if type is Bar, be referenced in foo.Foo then we will find class foo.Bar
    //note if there are 2 class: Bar & foo.Bar then we need resolve foo.Bar instead of Bar.
    if (name != null && name.indexOf('.') == -1) {
      final PsiFile psiFile = type.getContainingFile();
      final PsiElement[] fileChildren = psiFile.getChildren();
      String nameWithPackage = getQName(fileChildren, name, true);
      result = findClassByQName(nameWithPackage, type.getContext());
    }
    else {
      name = tryResolveFullyQualifiedHaxeReferenceExpression(type);
      result = findClassByQName(name, type.getContext());
    }

    //2. try without searchInSamePackage,
    // ex if type is Int, be referenced in foo.Foo then we will find class has qName = Int, not foo.Int
	// (if foo.Int exist then the prev step have aready resolved & returned it)
    result = result != null ? result : findClassByQName(name, type.getContext());

    return result;
  }

  public static String getQName(PsiElement[] fileChildren, final String result, boolean searchInSamePackage) {
    final HaxeImportStatementRegular importStatement = (HaxeImportStatementRegular)ContainerUtil.find(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementRegular &&
               UsefulPsiTreeUtil.importStatementForClassName((HaxeImportStatementRegular)element, result);
      }
    });

    final HaxeImportStatementWithInSupport importStatementWithInSupport = (HaxeImportStatementWithInSupport)ContainerUtil.find(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementWithInSupport &&
               UsefulPsiTreeUtil.importInStatementForClassName(((HaxeImportStatementWithInSupport)element), result);
      }
    });

    final List<PsiElement> importStatementWithWildcardList = ContainerUtil.findAll(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementWithWildcard;
      }
    });

    final HaxeImportStatementWithWildcard importStatementWithWildcard = (HaxeImportStatementWithWildcard)ContainerUtil.find(importStatementWithWildcardList, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementWithWildcard &&
               (UsefulPsiTreeUtil.importStatementWithWildcardForClassName((HaxeImportStatementWithWildcard)element, result) ||
                UsefulPsiTreeUtil.importStatementWithWildcardTypeForClassName(
                  (HaxeImportStatementWithWildcard)element, result));
      }
    });

    /*String qName = null;

    for (PsiElement element : importStatementWithWildcardList) {
      HaxeImportStatementWithWildcard importStatementWithWildcard1 = (HaxeImportStatementWithWildcard)element;
      List<HaxeNamedComponent> namedSubComponents = UsefulPsiTreeUtil
        .getImportStatementWithWildcardTypeNamedSubComponents(importStatementWithWildcard1, element.getContainingFile());

      for (HaxeNamedComponent namedComponent : namedSubComponents) {
        if (namedComponent.getName().equals(result)) {
          //namedComponent.getComponentName()
          //namedComponent.getText()
          qName = UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(importStatementWithWildcard1) + "." + result;
          break;
        }
      }
    }*/

    /*
    final HaxeImportStatementWithWildcard importStatementWithWildcardType = (HaxeImportStatementWithWildcard)ContainerUtil.find(importStatementWithWildcardList, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        if (element instanceof HaxeImportStatementWithWildcard) {
          HaxeImportStatementWithWildcard importStatementWithWildcard1 = (HaxeImportStatementWithWildcard)element;
          List<HaxeNamedComponent> namedSubComponents = UsefulPsiTreeUtil
            .getImportStatementWithWildcardTypeNamedSubComponents(importStatementWithWildcard1, element.getContainingFile());

          for (HaxeNamedComponent namedComponent : namedSubComponents) {
            if (namedComponent.getName().equals(result)) {
              qName = UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(importStatementWithWildcard1) + "." + result;
              return true;
            }
          }
        }

        return false;
      }
    });
    */

    final HaxeExpression importStatementExpression = importStatement == null ? null : importStatement.getReferenceExpression();
    final HaxeExpression importStatementWithInExpression = importStatementWithInSupport == null ? null : importStatementWithInSupport.getReferenceExpression();
    final String packageName = getPackageName((HaxePackageStatement)ContainerUtil.find(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxePackageStatement;
      }
    }));

    final HaxeClass classForType = (HaxeClass)ContainerUtil.find(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeClass && result.equals(((HaxeClass)element).getName());
      }
    });

    if (classForType != null) {
      return classForType.getQualifiedName();
    }
    else if (importStatement != null && importStatementExpression != null) {
      return importStatementExpression.getText();
    }
    else if (importStatementWithInSupport != null && importStatementWithInExpression != null) {
      return importStatementWithInExpression.getText();
    }
    else if (importStatementWithWildcard != null) {
      String text = importStatementWithWildcard.getReferenceExpression().getText();

      if (text.endsWith("." + result + ".*")) {
        return text.substring(0, text.length() - 2);
      }
      else {
        return UsefulPsiTreeUtil.getPackageStatementForImportStatementWithWildcard(importStatementWithWildcard) + "." + result;
      }
    }
    else if (searchInSamePackage && !packageName.isEmpty()) {
      return packageName + "." + result;
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

  public static Set<IElementType> getDeclarationTypes(@Nullable HaxeDeclarationAttribute[] attributeList) {
    return attributeList == null ? Collections.<IElementType>emptySet() : getDeclarationTypes(Arrays.asList(attributeList));
  }

  public static Set<IElementType> getDeclarationTypes(@Nullable List<HaxeDeclarationAttribute> attributeList) {
    if (attributeList == null || attributeList.isEmpty()) {
      return Collections.emptySet();
    }
    final Set<IElementType> resultSet = new THashSet<IElementType>();
    for (HaxeDeclarationAttribute attribute : attributeList) {
      PsiElement result = attribute.getFirstChild();
      final HaxeAccess access = attribute.getAccess();
      if (access != null) {
        result = access.getFirstChild();
      }
      if (result instanceof LeafPsiElement) {
        resultSet.add(((LeafPsiElement)result).getElementType());
      }
    }
    return resultSet;
  }

  @NotNull
  public static List<HaxeClass> findUsingClasses(PsiFile file) {
    final HaxeUsingStatement[] usingStatements = PsiTreeUtil.getChildrenOfType(file, HaxeUsingStatement.class);
    if (usingStatements == null) {
      return Collections.emptyList();
    }
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    for (HaxeUsingStatement usingStatement : usingStatements) {
      final HaxeExpression usingStatementExpression = usingStatement.getReferenceExpression();
      if (usingStatementExpression == null) continue;

      PsiManager manager = file.getManager();
      if(manager != null) {
        GlobalSearchScope scope = getScopeForElement(file);
        final List<VirtualFile> classFiles = HaxeComponentFileNameIndex.getFilesNameByQName(usingStatementExpression.getText(), scope);
        for(VirtualFile vf : classFiles) {
          PsiFile pf = manager.findFile(vf);
          if(pf != null) {
            List<HaxeClass> classes = findComponentDeclarations(pf);
            for(HaxeClass cls : classes) {
              if(cls instanceof HaxeTypedefDeclaration) {
                HaxeTypeOrAnonymous toa = getFirstItem(((HaxeTypedefDeclaration)cls).getTypeOrAnonymousList());
                if(toa != null) {
                  HaxeType t = toa.getType();
                  if(t != null) {
                    HaxeClass typeClass = t.getReferenceExpression().resolveHaxeClass().getHaxeClass();
                    if(typeClass != null) {
                      result.add(typeClass);
                    }
                  }
                }
              }
              else {
                result.add(cls);
              }
            }
          }
        }
      }

      //final HaxeClass haxeClass = findClassByQName(usingStatementExpression.getText(), file);
      //if (haxeClass != null) {
      //  result.add(haxeClass);
      //}
    }
    return result;
  }

  @NotNull
  public static List<HaxeComponentName> getComponentNames(List<? extends HaxeNamedComponent> components) {
    return ContainerUtil.map(components, new Function<HaxeNamedComponent, HaxeComponentName>() {
      @Override
      public HaxeComponentName fun(HaxeNamedComponent component) {
        return component.getComponentName();
      }
    });
  }

  @NotNull
  public static HaxeClassResolveResult findFirstParameterClass(HaxeNamedComponent haxeNamedComponent) {
    final HaxeParameterListPsiMixinImpl parameterList = PsiTreeUtil.getChildOfType(haxeNamedComponent, HaxeParameterListPsiMixinImpl.class);
    if (parameterList == null) {
      return HaxeClassResolveResult.EMPTY;
    }
    final List<HaxeParameter> parameters = parameterList.getParametersAsList();
    if (!parameters.isEmpty()) {
      final HaxeParameter parameter = parameters.iterator().next();
      return getHaxeClassResolveResult(parameter, HaxeGenericSpecialization.EMPTY);
    }
    return HaxeClassResolveResult.EMPTY;
  }

  public static HaxeParameterListPsiMixinImpl toHaxePsiParameterList(HaxeParameterList haxeParameterList) {
    return new HaxeParameterListPsiMixinImpl(haxeParameterList.getNode());
  }

  public static HashSet<HaxeClass> getBaseClassesSet(@NotNull HaxeClass clazz) {
    return getBaseClassesSet(clazz, new HashSet<HaxeClass>());
  }

  @NotNull
  public static HashSet<HaxeClass> getBaseClassesSet(@NotNull HaxeClass clazz, @NotNull HashSet<HaxeClass> outClasses) {
    List<HaxeType> types = new ArrayList<HaxeType>();
    types.addAll(clazz.getHaxeExtendsList());
    types.addAll(clazz.getHaxeImplementsList());
    for(HaxeType baseType : types) {
      final HaxeClass baseClass = HaxeResolveUtil.tryResolveClassByQName(baseType);
      if(baseClass != null && outClasses.add(baseClass)) {
        getBaseClassesSet(baseClass, outClasses);
      }
    }
    return outClasses;
  }
}
