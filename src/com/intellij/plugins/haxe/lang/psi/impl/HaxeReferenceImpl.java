/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.HaxeLookupElement;
import com.intellij.plugins.haxe.ide.refactoring.move.HaxeFileMoveHandler;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReference, PsiPolyVariantReference {

  public HaxeReferenceImpl(ASTNode node) {
    super(node);
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public PsiReference getReference() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    final TextRange textRange = getTextRange();
    return new TextRange(0, textRange.getEndOffset() - textRange.getStartOffset());
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Override
  public PsiElement resolve() {
    final ResolveResult[] resolveResults = multiResolve(true);

    return resolveResults.length == 0 ||
           resolveResults.length > 1 ||
           !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final List<? extends PsiElement> elements =
      ResolveCache.getInstance(getProject()).resolveWithCaching(this, HaxeResolver.INSTANCE, true, incompleteCode);
    return toCandidateInfoArray(elements);
  }

  @NotNull
  @Override
  public HaxeClassResolveResult resolveHaxeClass() {
    if (this instanceof HaxeThisExpression) {
      return HaxeClassResolveResult.create(PsiTreeUtil.getParentOfType(this, HaxeClass.class));
    }
    if (this instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      assert haxeClass != null;
      if (haxeClass.getExtendsList().isEmpty()) {
        return HaxeClassResolveResult.create(null);
      }
      final HaxeExpression superExpression = haxeClass.getExtendsList().get(0).getReferenceExpression();
      final HaxeClassResolveResult superClassResolveResult = superExpression instanceof HaxeReference
                                                             ? ((HaxeReference)superExpression).resolveHaxeClass()
                                                             : HaxeClassResolveResult.create(null);
      superClassResolveResult.specializeByParameters(haxeClass.getExtendsList().get(0).getTypeParam());
      return superClassResolveResult;
    }
    if (this instanceof HaxeStringLiteralExpression) {
      return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("String", this));
    }
    if (this instanceof HaxeLiteralExpression) {
      final LeafPsiElement child = (LeafPsiElement)getFirstChild();
      final IElementType childTokenType = child == null ? null : child.getElementType();
      return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(childTokenType), this));
    }
    if (this instanceof HaxeArrayLiteral) {
      HaxeArrayLiteral haxeArrayLiteral = (HaxeArrayLiteral)this;
      HaxeExpressionList expressionList = haxeArrayLiteral.getExpressionList();
      boolean isMap = false;
      boolean isString = false;
      boolean sameClass = false;
      boolean implementOrExtendSameClass = false;
      HaxeClass haxeClass = null;
      List<HaxeType> commonTypeList = new ArrayList<HaxeType>();
      List<HaxeExpression> haxeExpressionList = expressionList.getExpressionList();
      if (!haxeExpressionList.isEmpty()) {
        isMap = true;
        isString = true;
        sameClass = true;

        for (HaxeExpression expression : haxeExpressionList) {
          if (!(expression instanceof HaxeFatArrowExpression)) {
            isMap = false;
          }
          if (!(expression instanceof HaxeStringLiteralExpression)) {
            isString = false;
          }

          if (sameClass || implementOrExtendSameClass) {
            HaxeReferenceExpression haxeReference = null;
            if (expression instanceof HaxeNewExpression || expression instanceof HaxeCallExpression) {
              haxeReference = PsiTreeUtil.findChildOfType(expression, HaxeReferenceExpression.class);
            }
            if (expression instanceof HaxeReferenceExpression) {
              haxeReference = (HaxeReferenceExpression)expression;
            }

            HaxeClass haxeClassResolveResultHaxeClass = null;
            if (haxeReference != null) {
              HaxeClassResolveResult haxeClassResolveResult = haxeReference.resolveHaxeClass();
              haxeClassResolveResultHaxeClass = haxeClassResolveResult.getHaxeClass();
              if (haxeClassResolveResultHaxeClass != null) {
                if (haxeClass == null) {
                  haxeClass = haxeClassResolveResultHaxeClass;
                  commonTypeList.addAll(haxeClass.getImplementsList());
                  commonTypeList.addAll(haxeClass.getExtendsList());
                }
              }
            }

            if (haxeClass != null && !haxeClass.equals(haxeClassResolveResultHaxeClass)) {
              List<HaxeType> haxeTypeList = new ArrayList<HaxeType>();
              haxeTypeList.addAll(haxeClass.getImplementsList());
              haxeTypeList.addAll(haxeClass.getExtendsList());

              commonTypeList.retainAll(haxeTypeList);
              if (!commonTypeList.isEmpty()) {
                implementOrExtendSameClass = true;
              }
              else {
                implementOrExtendSameClass = false;
              }
            }

            if (haxeClass == null || !haxeClass.equals(haxeClassResolveResultHaxeClass)) {
              sameClass = false;
            }
          }
        }

        if (isMap) {
          return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("Map", this));
        }
      }
      HaxeClassResolveResult resolveResult =
        HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(getTokenType()), this));

      HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();

      HaxeGenericSpecialization specialization = resolveResult.getSpecialization();
      if (resolveResultHaxeClass != null && specialization.get(resolveResultHaxeClass, "T") == null) {
        if (isString) {
          specialization.put(resolveResultHaxeClass, "T", HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("String", this)));
        }
        else if (sameClass) {
          specialization.put(resolveResultHaxeClass, "T", HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(haxeClass.getQualifiedName(), this)));
        }
        else if (implementOrExtendSameClass) {
          HaxeReferenceExpression haxeReferenceExpression = commonTypeList.get(commonTypeList.size() - 1).getReferenceExpression();
          if (haxeReferenceExpression != null) {
            HaxeClassResolveResult resolveHaxeClass = haxeReferenceExpression.resolveHaxeClass();

            if (resolveHaxeClass != null) {
              HaxeClass resolveHaxeClassHaxeClass = resolveHaxeClass.getHaxeClass();

              if (resolveHaxeClassHaxeClass != null) {
                specialization.put(resolveResultHaxeClass, "T", HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(
                  resolveHaxeClassHaxeClass.getQualifiedName(), this)));
              }
            }
          }
        }
      }

      return resolveResult;
    }
    if (this instanceof HaxeNewExpression) {
      final HaxeClassResolveResult result = HaxeClassResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(
        ((HaxeNewExpression)this).getType()));
      result.specialize(this);
      return result;
    }
    if (this instanceof HaxeCallExpression) {
      final HaxeExpression expression = ((HaxeCallExpression)this).getExpression();
      final HaxeClassResolveResult leftResult = tryGetLeftResolveResult(expression);
      if (expression instanceof HaxeReference) {
        final HaxeClassResolveResult result =
          HaxeResolveUtil.getHaxeClassResolveResult(((HaxeReference)expression).resolve(), leftResult.getSpecialization());
        result.specialize(this);
        return result;
      }
    }
    if (this instanceof HaxeArrayAccessExpression) {
      // wrong generation. see HaxeCallExpression
      final HaxeReference reference = PsiTreeUtil.getChildOfType(this, HaxeReference.class);
      if (reference != null) {
        final HaxeClassResolveResult resolveResult = reference.resolveHaxeClass();
        final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
        if (resolveResultHaxeClass == null) {
          return resolveResult;
        }
        // std Array
        if ("Array".equalsIgnoreCase(resolveResultHaxeClass.getQualifiedName())) {
          HaxeClassResolveResult arrayResolveResult = resolveResult.getSpecialization().get(resolveResultHaxeClass, "T");

          if (arrayResolveResult != null) {
            return arrayResolveResult;
          }
        }
        // __get method
        return HaxeResolveUtil.getHaxeClassResolveResult(resolveResultHaxeClass.findMethodByName("__get"),
                                                         resolveResult.getSpecialization());
      }
    }
    PsiElement resolve = resolve();
    if (resolve != null) {
      PsiElement parent = resolve.getParent();

      if (parent != null) {
        if (parent instanceof HaxeFunctionDeclarationWithAttributes || parent instanceof HaxeExternFunctionDeclaration) {
          return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("Dynamic", this));
        }
        HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(parent, HaxeTypeTag.class);

        if (typeTag != null) {
          HaxeFunctionType functionType = PsiTreeUtil.getChildOfType(typeTag, HaxeFunctionType.class);
          if (functionType != null) {
            return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("Dynamic", this));
          }
        }
      }
    }

    HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(resolve(), tryGetLeftResolveResult(this).getSpecialization());
    if (result.getHaxeClass() == null) {
      result = HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getText(), this));
    }
    return result;
  }

  @NotNull
  private static HaxeClassResolveResult tryGetLeftResolveResult(HaxeExpression expression) {
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(expression, HaxeReference.class);
    final HaxeReference leftReference = childReferences != null ? childReferences[0] : null;
    return leftReference != null
           ? leftReference.resolveHaxeClass()
           : HaxeClassResolveResult.create(PsiTreeUtil.getParentOfType(expression, HaxeClass.class));
  }

  @Nullable
  private static String getLiteralClassName(IElementType type) {
    if (type == HaxeTokenTypes.STRING_LITERAL_EXPRESSION) {
      return "String";
    }
    else if (type == HaxeTokenTypes.ARRAY_LITERAL) {
      return "Array";
    }
    else if (type == HaxeTokenTypes.LITFLOAT) {
      return "Float";
    }
    else if (type == HaxeTokenTypes.REG_EXP) {
      return "EReg";
    }
    else if (type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITOCT) {
      return "Int";
    }
    return null;
  }

  private static ResolveResult[] resolveByClassAndSymbol(@Nullable HaxeClassResolveResult resolveResult, @NotNull String symbolName) {
    return resolveResult == null ? ResolveResult.EMPTY_ARRAY : resolveByClassAndSymbol(resolveResult.getHaxeClass(), symbolName);
  }

  private static ResolveResult[] resolveByClassAndSymbol(@Nullable HaxeClass referenceClass, @NotNull String symbolName) {
    final HaxeNamedComponent namedSubComponent =
      HaxeResolveUtil.findNamedSubComponent(referenceClass, symbolName);
    final HaxeComponentName componentName = namedSubComponent == null ? null : namedSubComponent.getComponentName();
    return toCandidateInfoArray(componentName);
  }

  @NotNull
  private static ResolveResult[] toCandidateInfoArray(@Nullable PsiElement element) {
    if (element == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    return new ResolveResult[]{new CandidateInfo(element, null)};
  }

  @NotNull
  private static ResolveResult[] toCandidateInfoArray(List<? extends PsiElement> elements) {
    final ResolveResult[] result = new ResolveResult[elements.size()];
    for (int i = 0, size = elements.size(); i < size; i++) {
      result[i] = new CandidateInfo(elements.get(i), EmptySubstitutor.getInstance());
    }
    return result;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    PsiElement element = this;
    if (getText().indexOf('.') != -1) {
      // qName
      final PsiElement lastChild = getLastChild();
      element = lastChild == null ? this : lastChild;
    }
    final HaxeIdentifier identifier = PsiTreeUtil.getChildOfType(element, HaxeIdentifier.class);
    final HaxeIdentifier identifierNew = HaxeElementGenerator.createIdentifierFromText(getProject(), newElementName);
    if (identifier != null && identifierNew != null) {
      element.getNode().replaceChild(identifier.getNode(), identifierNew.getNode());
    }
    return this;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof HaxeFile) {
      bindToFile(element);
    }
    else if (element instanceof PsiPackage) {
      bindToPackage((PsiPackage)element);
    }
    return this;
  }

  private void bindToPackage(PsiPackage element) {
    final HaxeImportStatementRegular importStatement =
      HaxeElementGenerator.createImportStatementFromPath(getProject(), element.getQualifiedName());
    HaxeReferenceExpression referenceExpression = importStatement != null ? importStatement.getReferenceExpression() : null;
    assert referenceExpression != null;
    replace(referenceExpression);
  }

  private void bindToFile(PsiElement element) {
    String destinationPackage = element.getUserData(HaxeFileMoveHandler.destinationPackageKey);
    if (destinationPackage == null) {
      destinationPackage = "";
    }
    final String importPath = (destinationPackage.isEmpty() ? "" : destinationPackage + ".") +
                              FileUtil.getNameWithoutExtension(((HaxeFile)element).getName());

    if (resolve() == null) {
      if (getParent() instanceof HaxeImportStatementRegular && !destinationPackage.isEmpty()) {
        final HaxeImportStatementRegular importStatement =
          HaxeElementGenerator.createImportStatementFromPath(getProject(), importPath);
        assert importStatement != null;
        getParent().replace(importStatement);
      }
      else if (getParent() instanceof HaxeImportStatementRegular && destinationPackage.isEmpty()) {
        // need remove, empty destination
        getParent().getParent().deleteChildRange(getParent(), getParent());
      }
      else if (getText().indexOf('.') != -1) {
        // qName
        final String newQName = destinationPackage.equals(HaxeResolveUtil.getPackageName(getContainingFile())) ?
                                FileUtil.getNameWithoutExtension(((HaxeFile)element).getName()) :
                                importPath;
        final HaxeImportStatementRegular importStatement =
          HaxeElementGenerator.createImportStatementFromPath(getProject(), newQName);
        HaxeReferenceExpression referenceExpression = importStatement != null ? importStatement.getReferenceExpression() : null;
        assert referenceExpression != null;
        replace(referenceExpression);
      }
      else if (UsefulPsiTreeUtil.findImportByClassName(this, getText()) == null && UsefulPsiTreeUtil.findImportWithInByClassName(this, getText()) == null && !destinationPackage.isEmpty()) {
        // need add import
        HaxeAddImportHelper.addImport(importPath, getContainingFile());
      }
    }
    else {
      final HaxeImportStatementRegular importStatement = UsefulPsiTreeUtil.findImportByClassName(this, getText());
      HaxeReferenceExpression referenceExpression = importStatement != null ? importStatement.getReferenceExpression() : null;
      if (referenceExpression != null && !importPath.equals(referenceExpression.getText())) {
        // need remove, cause can resolve without
        importStatement.getParent().deleteChildRange(importStatement, importStatement);
      }
    }
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    final PsiElement resolve = resolve();
    if (element instanceof HaxeFile &&
        resolve instanceof HaxeComponentName &&
        resolve.getParent() instanceof HaxeClass) {
      return element == resolve.getContainingFile();
    }
    final HaxeReference[] references = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
    final boolean chain = references != null && references.length == 2;
    return !chain && resolve == element;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final Set<HaxeComponentName> suggestedVariants = new THashSet<HaxeComponentName>();


    // if not first in chain
    // foo.bar.baz
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(this);
    if (leftReference != null && getParent() instanceof HaxeReference && leftReference.getText().equals(leftReference.resolveHaxeClass().getHaxeClass().getName())) {
      addClassStaticMembersVariants(suggestedVariants, leftReference.resolveHaxeClass().getHaxeClass(),
                       !(leftReference instanceof HaxeThisExpression));
      addChildClassVariants(suggestedVariants, leftReference.resolveHaxeClass().getHaxeClass());
    }
    else if (leftReference != null && getParent() instanceof HaxeReference && !leftReference.resolveHaxeClass().isFunctionType()) {
      addClassNonStaticMembersVariants(suggestedVariants, leftReference.resolveHaxeClass().getHaxeClass(),
                       !(leftReference instanceof HaxeThisExpression));
      addUsingVariants(suggestedVariants, leftReference.resolveHaxeClass().getHaxeClass(),
                       HaxeResolveUtil.findUsingClasses(getContainingFile()));
      addChildClassVariants(suggestedVariants, leftReference.resolveHaxeClass().getHaxeClass());
    }
    else {
      // if chain
      // node(foo.node(bar)).node(baz)
      final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
      final boolean isChain = childReferences != null && childReferences.length == 2;
      if (!isChain) {
        PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
        addClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), false);
        PsiFile psiFile = this.getContainingFile();
        addImportStatementWithWildcardTypeClassVariants(suggestedVariants, psiFile);
      }
    }

    Object[] variants = HaxeLookupElement.convert(suggestedVariants).toArray();
    PsiElement leftTarget = leftReference != null ? leftReference.resolve() : null;

    if (leftTarget instanceof PsiPackage) {
      return ArrayUtil.mergeArrays(variants, ((PsiPackage)leftTarget).getSubPackages());
    }
    else if (leftReference == null) {
      PsiPackage rootPackage = JavaPsiFacade.getInstance(getElement().getProject()).findPackage("");
      return rootPackage == null ? variants : ArrayUtil.mergeArrays(variants, rootPackage.getSubPackages());
    }
    return variants;
  }

  private void addImportStatementWithWildcardTypeClassVariants(Set<HaxeComponentName> suggestedVariants, PsiFile psiFile) {
    List<PsiElement> importStatementWithWildcardList = ContainerUtil.findAll(psiFile.getChildren(), new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementWithWildcard;
      }
    });

    for (PsiElement element : importStatementWithWildcardList) {
      List<HaxeNamedComponent> namedSubComponents =
        UsefulPsiTreeUtil.getImportStatementWithWildcardTypeNamedSubComponents((HaxeImportStatementWithWildcard)element, psiFile);
      for (HaxeNamedComponent namedComponent : namedSubComponents) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  private void addChildClassVariants(Set<HaxeComponentName> variants, HaxeClass haxeClass) {
    if (haxeClass != null) {
      PsiFile psiFile = haxeClass.getContainingFile();
      VirtualFile virtualFile = psiFile.getVirtualFile();

      if (virtualFile != null) {
        String nameWithoutExtension = virtualFile.getNameWithoutExtension();

        String name = haxeClass.getName();
        if (name != null && name.equals(nameWithoutExtension)) {
          List<HaxeClass> haxeClassList = HaxeResolveUtil.findComponentDeclarations(psiFile);

          for (HaxeClass aClass : haxeClassList) {
            if (!aClass.getName().equals(nameWithoutExtension)) {
              variants.add(aClass.getComponentName());
            }
          }
        }
      }
    }
  }

  private static void addUsingVariants(Set<HaxeComponentName> variants, @Nullable HaxeClass ourClass, List<HaxeClass> classes) {
    for (HaxeClass haxeClass : classes) {
      for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
        if (haxeNamedComponent.isPublic() && haxeNamedComponent.isStatic() && haxeNamedComponent.getComponentName() != null) {
          final HaxeClassResolveResult resolveResult = HaxeResolveUtil.findFirstParameterClass(haxeNamedComponent);
          final boolean needToAdd = resolveResult.getHaxeClass() == null || resolveResult.getHaxeClass() == ourClass;
          if (needToAdd) {
            variants.add(haxeNamedComponent.getComponentName());
          }
        }
      }
    }
  }

  private static void addClassVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }
    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if (!needFilter && namedComponent.getComponentName() != null) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  private static void addClassStaticMembersVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }
    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if (!needFilter && namedComponent.isStatic() && namedComponent.getComponentName() != null) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  private static void addClassNonStaticMembersVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }
    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if (!needFilter && !namedComponent.isStatic() && namedComponent.getComponentName() != null) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }
}
