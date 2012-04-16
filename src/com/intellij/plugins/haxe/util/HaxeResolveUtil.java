package com.intellij.plugins.haxe.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeComponentFileNameIndex;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
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
      result = packageName + ".";
    }
    if (className != null) {
      result += className;
    }
    return result;
  }

  @NotNull
  @NonNls
  public static String getPackageName(final PsiFile file) {
    final HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(file, HaxePackageStatement.class);
    if (packageStatement != null && packageStatement.getExpression() != null) {
      return packageStatement.getExpression().getText();
    }
    return "";
  }

  @Nullable
  public static HaxeClass findClassByQName(final @Nullable String qName, final @Nullable PsiElement context) {
    if (context == null || qName == null) {
      return null;
    }
    final Project project = context.getProject();
    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    final List<VirtualFile> classFiles = HaxeComponentFileNameIndex.
      getFilesNameByQName(qName, scope);
    final Pair<String, String> qNamePair = splitQName(qName);
    for (VirtualFile classFile : classFiles) {
      final HaxeClass componentPsiElement = findComponentDeclaration(context.getManager().findFile(classFile), qNamePair.getSecond());
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
  public static List<HaxeNamedComponent> getNamedSubComponents(HaxeClass haxeClass) {
    PsiElement body = null;
    final HaxeComponentType type = HaxeComponentType.typeOf(haxeClass);
    if (type == HaxeComponentType.CLASS) {
      body = PsiTreeUtil.getChildOfAnyType(haxeClass, HaxeClassBody.class, HaxeExternClassDeclarationBody.class);
    }
    if (type == HaxeComponentType.INTERFACE) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeInterfaceBody.class);
    }
    if (type == HaxeComponentType.ENUM) {
      body = PsiTreeUtil.getChildOfType(haxeClass, HaxeEnumBody.class);
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
        return typeTag == null ? null : typeTag.getType();
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
      return new HaxeClassResolveResult(null);
    }
    if (element instanceof HaxeComponentName) {
      return getHaxeClassResolveResult(element.getParent(), specialization);
    }
    if (element instanceof AbstractHaxeTypeDefImpl) {
      final AbstractHaxeTypeDefImpl typeDef = (AbstractHaxeTypeDefImpl)element;
      return typeDef.getTargetClass(specialization);
    }
    if (element instanceof HaxeClass) {
      return new HaxeClassResolveResult((HaxeClass)element);
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
  private static HaxeClassResolveResult tryResolveClassByTypeTag(PsiElement element,
                                                                     HaxeGenericSpecialization specialization) {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(element, HaxeTypeTag.class);
    final HaxeType type = typeTag != null ? typeTag.getType() :
                          element instanceof HaxeType ? (HaxeType)element : null;

    HaxeNamedComponent typeComponent = type == null ? null : tryResolveClassByQName(type);
    if (typeComponent == null && type != null && specialization.containsKey(element, type.getText())) {
      return specialization.get(element, type.getText());
    }

    HaxeClassResolveResult result = getHaxeClassResolveResult(typeComponent, specialization.getInnerSpecialization(element));
    if (result.getHaxeClass() != null) {
      result.specializeByParameters(type == null ? null : type.getTypeParam());
      return result;
    }

    if (typeTag != null) {
      return tryResolveFunctionType(typeTag.getFunctionType(), specialization);
    }

    return HaxeClassResolveResult.EMPTY;
  }

  private static HaxeClassResolveResult tryResolveFunctionType(@Nullable HaxeFunctionType functionType, HaxeGenericSpecialization specialization) {
    if(functionType == null){
      return HaxeClassResolveResult.EMPTY;
    }
    final HaxeClassResolveResult result = tryResolveClassByTypeTag(functionType.getType(), specialization);
    functionType = functionType.getFunctionType();
    while (functionType != null) {
      // todo: anonymous types :(
      final HaxeType[] types = PsiTreeUtil.getChildrenOfType(functionType, HaxeType.class);
      if (types != null) {
        for (int i = types.length - 1; i >= 0; --i) {
          result.getFunctionTypes().add(tryResolveClassByTypeTag(types[i], specialization));
        }
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

    HaxeClass result = findClassByQName(getQName(type, true), type.getContext());
    result = result != null ? result : findClassByQName(getQName(type, false), type.getContext());
    result = result != null ? result : tryFindHelper(type);
    return result;
  }

  @Nullable
  private static HaxeClass tryFindHelper(PsiElement element) {
    final HaxeClass ownerClass = findClassByQName(UsefulPsiTreeUtil.findHelperOwnerQName(element, element.getText()), element);
    return ownerClass == null ? null : findComponentDeclaration(ownerClass.getContainingFile(), element.getText());
  }

  public static String getQName(@NotNull PsiElement type, boolean searchInSamePackage) {
    if (type instanceof HaxeType) {
      type = ((HaxeType)type).getExpression();
    }
    String result = type.getText();
    if (result.indexOf('.') == -1) {
      final HaxeImportStatement importStatement = UsefulPsiTreeUtil.findImportByClass(type, result);
      final HaxeExpression expression = importStatement == null ? null : importStatement.getExpression();
      if (importStatement != null && expression != null) {
        result = expression.getText();
      }
      else if (searchInSamePackage) {
        final String packageName = getPackageName(type.getContainingFile());
        if (!packageName.isEmpty()) {
          result = packageName + "." + result;
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

  public static Set<IElementType> getDeclarationTypes(@Nullable HaxeDeclarationAttributeList attributeList) {
    if (attributeList == null) {
      return Collections.emptySet();
    }
    final Set<IElementType> resultSet = new THashSet<IElementType>();
    for (HaxeDeclarationAttribute attribute : attributeList.getDeclarationAttributeList()) {
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
      final HaxeExpression usingStatementExpression = usingStatement.getExpression();
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
}
