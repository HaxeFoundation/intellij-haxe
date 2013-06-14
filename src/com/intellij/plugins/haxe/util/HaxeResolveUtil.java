/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeComponentFileNameIndex;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {
  @Nullable
  public static HaxeReference getLeftReference(@Nullable final PsiElement node) {
    if (node == null) return null;
    for (PsiElement sibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(node, true);
         sibling != null;
         sibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(sibling, true)) {
      if (".".equals(sibling.getText())) continue;
      return sibling instanceof HaxeReference && sibling != node ? (HaxeReference)sibling : null;
    }
    return null;
  }

  @NotNull
  public static Pair<String, String> splitQName(@NotNull String qName) {
    final int dotIndex = qName.lastIndexOf('.');
    final String packageName = dotIndex == -1 ? "" : qName.substring(0, dotIndex);
    final String className = dotIndex == -1 ? qName : qName.substring(dotIndex + 1);

    return new Pair<String, String>(packageName, className);
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
    return findExtendsImplementsListImpl(extendsList, HaxeTokenTypes.KEXTENDS);
  }

  public static List<HaxeType> getImplementsList(@Nullable HaxeInheritList extendsList) {
    return findExtendsImplementsListImpl(extendsList, HaxeTokenTypes.KIMPLEMENTS);
  }

  @NotNull
  private static List<HaxeType> findExtendsImplementsListImpl(@Nullable HaxeInheritList extendsList,
                                                              @NotNull IElementType expectedKeyword) {
    if (extendsList == null) {
      return Collections.emptyList();
    }
    final List<HaxeType> result = new ArrayList<HaxeType>();
    for (HaxeInherit inherit : extendsList.getInheritList()) {
      final PsiElement firstChild = inherit.getFirstChild();
      final IElementType childType = firstChild instanceof ASTNode ? ((ASTNode)firstChild).getElementType() : null;
      final HaxeType inheritType = inherit.getType();
      if (childType == expectedKeyword && inheritType != null) {
        result.add(inheritType);
      }
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
    final HaxeNamedComponent result = haxeClass.findMethodByName(name);
    return result != null ? result : haxeClass.findFieldByName(name);
  }

  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(@NotNull HaxeClass... rootHaxeClasses) {
    return findNamedSubComponents(true, rootHaxeClasses);
  }

  @NotNull
  public static List<HaxeNamedComponent> findNamedSubComponents(boolean unique, @NotNull HaxeClass... rootHaxeClasses) {
    final List<HaxeNamedComponent> unfilteredResult = new ArrayList<HaxeNamedComponent>();
    final LinkedList<HaxeClass> classes = new LinkedList<HaxeClass>();
    classes.addAll(Arrays.asList(rootHaxeClasses));
    while (!classes.isEmpty()) {
      final HaxeClass haxeClass = classes.pollFirst();
      for (HaxeNamedComponent namedComponent : getNamedSubComponents(haxeClass)) {
        if (namedComponent.getName() != null) {
          unfilteredResult.add(namedComponent);
        }
      }
      classes.addAll(tyrResolveClassesByQName(haxeClass.getExtendsList()));
      classes.addAll(tyrResolveClassesByQName(haxeClass.getImplementsList()));
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
      final HaxeTypeOrAnonymous typeOrAnonymous = ((HaxeTypedefDeclaration)haxeClass).getTypeOrAnonymous();
      if (typeOrAnonymous != null && typeOrAnonymous.getAnonymousType() != null) {
        typeOrAnonymous.getAnonymousType();
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
    final HaxeVarDeclaration[] variables = PsiTreeUtil.getChildrenOfType(body, HaxeVarDeclaration.class);
    if (namedComponents != null) {
      ContainerUtil.addAll(result, namedComponents);
    }
    if (variables == null) {
      return result;
    }
    for (HaxeVarDeclaration varDeclaration : variables) {
      result.addAll(varDeclaration.getVarDeclarationPartList());
    }
    return result;
  }

  @Nullable
  public static List<HaxeType> getFunctionParameters(HaxeNamedComponent component) {
    if (HaxeComponentType.typeOf(component) != HaxeComponentType.METHOD) {
      return null;
    }
    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(component, HaxeParameterList.class);
    if (parameterList == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.map(parameterList.getParameterList(), new Function<HaxeParameter, HaxeType>() {
      @Override
      public HaxeType fun(HaxeParameter parameter) {
        final HaxeTypeTag typeTag = parameter.getTypeTag();
        return typeTag == null ? null : typeTag.getTypeOrAnonymous().getType();
      }
    });
  }


  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element) {
    return getHaxeClassResolveResult(element, new HaxeGenericSpecialization());
  }

  @NotNull
  public static HaxeClassResolveResult getHaxeClassResolveResult(@Nullable PsiElement element,
                                                                 @NotNull HaxeGenericSpecialization specialization) {
    if (element == null || element instanceof PsiPackage) {
      return HaxeClassResolveResult.create(null);
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
      return HaxeClassResolveResult.create(haxeClass);
    }
    if (element instanceof HaxeForStatement) {
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      assert iterable != null;
      final HaxeExpression expression = iterable.getExpression();
      if (expression instanceof HaxeReference) {
        final HaxeClassResolveResult resolveResult = ((HaxeReference)expression).resolveHaxeClass();
        final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
        // try next
        HaxeClassResolveResult result =
          getHaxeClassResolveResult(resolveResultHaxeClass == null ? null : resolveResultHaxeClass.findMethodByName("next"),
                                    resolveResult.getSpecialization());
        if (result.getHaxeClass() != null) {
          return result;
        }
        // try iterator
        result = getHaxeClassResolveResult(resolveResultHaxeClass == null ? null : resolveResultHaxeClass.findMethodByName("iterator"),
                                           resolveResult.getSpecialization());
        return result.getSpecialization().containsKey(null, "T")
               ? result.getSpecialization().get(null, "T")
               : HaxeClassResolveResult.EMPTY;
      }
      return HaxeClassResolveResult.EMPTY;
    }

    HaxeClassResolveResult result = tryResolveClassByTypeTag(element, specialization);
    if (result.getHaxeClass() != null) {
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
  public static HaxeClassResolveResult tryResolveClassByTypeTag(PsiElement element,
                                                                HaxeGenericSpecialization specialization) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag == null ? null : typeTag.getTypeOrAnonymous();
    final HaxeType type = typeOrAnonymous != null ? typeOrAnonymous.getType() :
                          element instanceof HaxeType ? (HaxeType)element : null;

    HaxeClass haxeClass = type == null ? null : tryResolveClassByQName(type);
    if (haxeClass == null && type != null && specialization.containsKey(element, type.getText())) {
      return specialization.get(element, type.getText());
    }

    HaxeClassResolveResult result = getHaxeClassResolveResult(haxeClass, specialization.getInnerSpecialization(element));
    if (result.getHaxeClass() != null) {
      result.specializeByParameters(type == null ? null : type.getTypeParam());
      return result;
    }

    if (typeTag != null) {
      return tryResolveFunctionType(typeTag.getFunctionType(), specialization);
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

    HaxeClass result = findClassByQName(getQName(type, false), type.getContext());
    result = result != null ? result : tryFindHelper(type);
    result = result != null ? result : findClassByQNameInSuperPackages(type);
    return result;
  }

  @Nullable
  private static HaxeClass findClassByQNameInSuperPackages(PsiElement type) {
    HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(type.getContainingFile(), HaxePackageStatement.class);
    String packageName = getPackageName(packageStatement);
    String[] packages = packageName.split("\\.");
    String typeName = type.getText();
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
    final HaxeClass ownerClass = findClassByQName(UsefulPsiTreeUtil.findHelperOwnerQName(element, element.getText()), element);
    return ownerClass == null ? null : findComponentDeclaration(ownerClass.getContainingFile(), element.getText());
  }

  public static String getQName(@NotNull PsiElement type, boolean searchInSamePackage) {
    if (type instanceof HaxeType) {
      type = ((HaxeType)type).getReferenceExpression();
    }
    final String result = type.getText();
    if (result.indexOf('.') == -1) {
      final PsiFile psiFile = type.getContainingFile();
      final PsiElement[] fileChildren = psiFile.getChildren();
      return getQName(fileChildren, result, searchInSamePackage);
    }
    return result;
  }

  public static String getQName(PsiElement[] fileChildren, final String result, boolean searchInSamePackage) {
    final HaxeImportStatement importStatement = (HaxeImportStatement)ContainerUtil.find(fileChildren, new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatement &&
               UsefulPsiTreeUtil.importStatementForClassName((HaxeImportStatement)element, result);
      }
    });
    final HaxeExpression expression = importStatement == null ? null : importStatement.getReferenceExpression();
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
    else if (importStatement != null && expression != null) {
      return expression.getText();
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
      final HaxeClass haxeClass = findClassByQName(usingStatementExpression.getText(), file);
      if (haxeClass != null) {
        result.add(haxeClass);
      }
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
    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(haxeNamedComponent, HaxeParameterList.class);
    if (parameterList == null) {
      return HaxeClassResolveResult.EMPTY;
    }
    final List<HaxeParameter> parameters = parameterList.getParameterList();
    if (!parameters.isEmpty()) {
      final HaxeParameter parameter = parameters.iterator().next();
      return getHaxeClassResolveResult(parameter, HaxeGenericSpecialization.EMPTY);
    }
    return HaxeClassResolveResult.EMPTY;
  }
}
