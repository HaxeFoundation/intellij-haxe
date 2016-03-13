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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.HaxeLookupElement;
import com.intellij.plugins.haxe.ide.refactoring.move.HaxeFileMoveHandler;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.JavaSourceUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

abstract public class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReference {

  public static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

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

  @Nullable
  public HaxeGenericSpecialization getSpecialization() {
    // CallExpressions need to resolve their child, rather than themselves.
    HaxeExpression expression = this;
    if (this instanceof HaxeCallExpression) {
      expression = ((HaxeCallExpression)this).getExpression();
    }

    // The specialization for a reference comes from either the type of the left-hand side of the
    // expression, or failing that, from the class in which the reference appears, which is
    // exactly what tryGetLeftResolveResult() gives us.
    final HaxeClassResolveResult result = tryGetLeftResolveResult(expression);
    return result != null ? result.getSpecialization() : null;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  private List<? extends PsiElement> resolveNamesToParents(List<? extends PsiElement> nameList) {
    if (nameList == null) {
      return Collections.emptyList();
    }

    List<PsiElement> result = new ArrayList<PsiElement>();
    for (PsiElement element : nameList) {
      PsiElement elementToAdd = element;
      if (element instanceof HaxeComponentName) {
        PsiElement parent = element.getParent();
        if (null != parent && parent.isValid()) {
          // Don't look for package parents. It turns 'com' into 'com.xx'.
          // XXX: May need to walk the tree until we get to the PACKAGE_STATEMENT
          // element;
          if (!(parent instanceof PsiPackage)) {
            elementToAdd = parent;
          }
        }
      }
      result.add(elementToAdd);
    }
    return result;
  }


  @Override
  public PsiElement resolve() {
    return resolve(true);
  }

  public boolean resolveIsStaticExtension() {
    // @TODO: DIRTY HACK! to avoid rewriting all the code!
    HaxeResolver.INSTANCE.resolve(this, true);
    return HaxeResolver.isExtension.get();
  }

  @NotNull
  @Override
  public JavaResolveResult advancedResolve(boolean incompleteCode) {
    final PsiElement resolved = resolve(incompleteCode);
    // TODO: Determine if we are using the right substitutor.
    // ?? XXX: Is the internal element here supposed to be a PsiClass sub-class ??
    return null != resolved ? new CandidateInfo(resolved, EmptySubstitutor.getInstance()) : JavaResolveResult.EMPTY;
  }

  @NotNull
  private JavaResolveResult[] multiResolve(boolean incompleteCode, boolean resolveToParents) {
    //
    // Resolving through this.resolve, or through the ResolveCache.resolve,
    // resolves to the *name* of the component.  That's what is cached, that's
    // what is returned.  For the Java processing code, the various reference types
    // are sub-classed, along with the base reference being aware of the type of the
    // entity.  Still, the base reference (PsiJavaReference) resolves to the
    // COMPONENT_NAME element, NOT the element type.  The various sub-classes
    // of PsiJavaReference (and PsiJavaCodeReferenceElement) return the actual
    // element type.  For example, you have to have to use PsiClassType.resolve
    // to get back a PsiClassType.
    //
    // For the Haxe code, we don't have a large number of reference sub-classes,
    // so we have to figure out what the expected parent type is and return that.
    // Luckily, most references have a COMPONENT_NAME element located immediately
    // below the parent in the PSI tree.  Therefore, when requested, we're going
    // to return the parent type.
    //
    // The root of the problem appears to be that the Java language processing
    // always expected the COMPONENT_NAME field.  However, the Haxe processing
    // (plugin) code was written to expect the type *containing* the
    // COMPONENT_NAME element (e.g. the named element not the name of the element).
    // Therefore, we now have an adapter, and have to tweak some things to make them
    // compatible.  Perhaps the proper answer is to make all of the plug-in code
    // expect the COMPONENT_NAME field, to be consistent, and then we won't need
    // the resolveToParents logic (here, at least).
    //

    // For the moment (while debugging the resolver) let's do this without caching.
    boolean skipCaching = false;
    List<? extends PsiElement> cachedNames
              = skipCaching ? (HaxeResolver.INSTANCE).resolve(this, incompleteCode)
                            : ResolveCache.getInstance(getProject()).resolveWithCaching(this, HaxeResolver.INSTANCE, true, incompleteCode);


    // CandidateInfo does some extra resolution work when checking validity, so
    // the results have to be turned into a CandidateInfoArray, and not just passed
    // around as the list that HaxeResolver returns.
    JavaResolveResult [] result = toCandidateInfoArray(resolveToParents ? resolveNamesToParents(cachedNames) : cachedNames);
    return result;
  }

  /**
   * Resolve a reference, returning the COMPONENT_NAME field of the found
   * PsiElement.
   *
   * @return the component name of the found element, or null if not (or
   *         more than one) found.
   */
  @Nullable
  public PsiElement resolveToComponentName() {
    final ResolveResult[] resolveResults = multiResolve(true, false);

    return resolveResults.length == 0 ||
           resolveResults.length > 1 ||
           !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
  }

  /**
   * Resolve a reference, returning a list of possible candidates.
   *
   * @param incompleteCode Whether to treat the code as a fragment or not.
   *                       Usually, code is considered incomplete.
   *
   * @return a (possibly empty) list of candidates that this reference matches.
   */
  @NotNull
  @Override
  public JavaResolveResult[] multiResolve(boolean incompleteCode) {
    return multiResolve(incompleteCode, true);
  }

  /**
   * Resolve this reference to a PsiElement -- *NOT* it's name.
   *
   * @param incompleteCode  Whether to treat the code as a fragment or not.
   *                        Usually, code is considered incomplete.
   *
   * @return the element this reference refers to, or null if none (or more
   *         than one) is found.
   */
  @Nullable
  public PsiElement resolve(boolean incompleteCode) {
    final ResolveResult[] resolveResults = multiResolve(incompleteCode);

    return resolveResults.length == 0 ||
           resolveResults.length > 1 ||
           !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
  }

  @NotNull
  @Override
  public HaxeClassResolveResult resolveHaxeClass() {
    if (this instanceof HaxeThisExpression) {
      HaxeClass clazz = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      // this has different semantics on abstracts
      if (clazz != null && clazz.getModel().isAbstract()) {
        HaxeTypeOrAnonymous type = clazz.getModel().getAbstractUnderlyingType();
        if (type != null) {
          return HaxeClassResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(type));
        }
      }
      return HaxeClassResolveResult.create(clazz);
    }
    if (this instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      assert haxeClass != null;
      if (haxeClass.getHaxeExtendsList().isEmpty()) {
        return HaxeClassResolveResult.create(null);
      }
      final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
      final HaxeClassResolveResult superClassResolveResult = superExpression instanceof HaxeReference
                                                             ? ((HaxeReference)superExpression).resolveHaxeClass()
                                                             : HaxeClassResolveResult.create(null);
      superClassResolveResult.specializeByParameters(haxeClass.getHaxeExtendsList().get(0).getTypeParam());
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
      List<HaxeExpression> haxeExpressionList = expressionList != null ? expressionList.getExpressionList() : new ArrayList<HaxeExpression>();
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
                  commonTypeList.addAll(haxeClass.getHaxeImplementsList());
                  commonTypeList.addAll(haxeClass.getHaxeExtendsList());
                }
              }
            }

            if (haxeClass != null && !haxeClass.equals(haxeClassResolveResultHaxeClass)) {
              List<HaxeType> haxeTypeList = new ArrayList<HaxeType>();
              haxeTypeList.addAll(haxeClass.getHaxeImplementsList());
              haxeTypeList.addAll(haxeClass.getHaxeExtendsList());

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
        if ("Array".equals(resolveResultHaxeClass.getQualifiedName())) {
          HaxeClassResolveResult arrayResolveResult = resolveResult.getSpecialization().get(resolveResultHaxeClass, "T");

          if (arrayResolveResult != null) {
            return arrayResolveResult;
          }
        }
        // __get method
        return HaxeResolveUtil.getHaxeClassResolveResult(resolveResultHaxeClass.findHaxeMethodByName("__get"),
                                                         resolveResult.getSpecialization());
      }
    }

    PsiElement resolve = resolve();
    if (resolve instanceof PsiPackage) {
      // Packages don't ever resolve to classes. (And they don't have children!)
      return HaxeClassResolveResult.EMPTY;
    }
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
  private static JavaResolveResult[] toCandidateInfoArray(@Nullable PsiElement element) {
    if (element == null) {
      return JavaResolveResult.EMPTY_ARRAY;
    }
    return new JavaResolveResult[]{new CandidateInfo(element, null)};
  }

  @NotNull
  private static JavaResolveResult[] toCandidateInfoArray(List<? extends PsiElement> elements) {
    final JavaResolveResult[] result = new JavaResolveResult[elements.size()];
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
    else if (element instanceof PsiClass) {
      bindToClass((PsiClass)element);
    }
    return this;
  }

  private void bindToClass(PsiClass element) {
    handleElementRename(element.getName());
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
    // Resolving is (relatively) expensive, so if we're going to ignore the answer anyway, then don't bother.
    if (!(element instanceof HaxeFile)) {
      final HaxeReference[] references = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
      final boolean chain = references != null && references.length == 2;
      if (chain) return false;
    }
    final PsiElement resolve = element instanceof HaxeComponentName ? resolveToComponentName() : resolve();
    if (element instanceof HaxeFile && resolve instanceof HaxeClass) {
      return element == resolve.getContainingFile();
    }
    return resolve == element;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final Set<HaxeComponentName> suggestedVariants = new THashSet<HaxeComponentName>();
    final Set<HaxeComponentName> suggestedVariantsExtensions = new THashSet<HaxeComponentName>();

    // if not first in chain
    // foo.bar.baz
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(this);
    // TODO: This should use getName() instead of getQualifiedName(), but it isn't implemented properly and getName() NPEs.
    HaxeClassResolveResult result = null;
    HaxeClass haxeClass = null;
    String name = null;
    if (leftReference != null) {
      result = leftReference.resolveHaxeClass();
      if (result != null) {
        haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          name = haxeClass.getName();
        }
      }
    }
    if (leftReference != null && getParent() instanceof HaxeReference && name != null &&
        HaxeResolveUtil.splitQName(leftReference.getText()).getSecond().equals(name)) {

      addClassStaticMembersVariants(suggestedVariants, result.getHaxeClass(),
                                    !(leftReference instanceof HaxeThisExpression));
      addChildClassVariants(suggestedVariants, result.getHaxeClass());
    }
    else if (leftReference != null && getParent() instanceof HaxeReference && !result.isFunctionType()) {
      if (null == haxeClass) {
        // TODO: fix haxeClass by type inference. Use compiler code assist?!
      }
      if (haxeClass != null) {
        addClassNonStaticMembersVariants(suggestedVariants, haxeClass,
                                         !(leftReference instanceof HaxeThisExpression));
        addUsingVariants(suggestedVariants, suggestedVariantsExtensions, haxeClass,
                         HaxeResolveUtil.findUsingClasses(getContainingFile()));
      }
    }
    else {
      // if chain
      // node(foo.node(bar)).node(baz)
      final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
      final boolean isChain = childReferences != null && childReferences.length == 2;
      if (!isChain) {
        final boolean isElementInForwardMeta = HaxeAbstractForwardUtil.isElementInForwardMeta(this);
        if (isElementInForwardMeta) {
          addAbstractUnderlyingClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), true);
        } else {
          PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
          addClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), false);
          PsiFile psiFile = this.getContainingFile();
          addImportStatementWithWildcardTypeClassVariants(suggestedVariants, psiFile);
        }
      }
    }

    Object[] variants = HaxeLookupElement.convert(result, suggestedVariants, suggestedVariantsExtensions).toArray();
    PsiElement leftTarget = leftReference != null ? leftReference.resolve() : null;

    if (leftTarget instanceof PsiPackage) {
      return ArrayUtil.mergeArrays(variants, ((PsiPackage)leftTarget).getSubPackages());
    }
    else if (leftTarget instanceof HaxeFile) {
      return ArrayUtil.mergeArrays(variants, ((HaxeFile)leftTarget).getClasses());
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

  private static void addUsingVariants(Set<HaxeComponentName> variants, Set<HaxeComponentName> variantsWithExtension, @Nullable HaxeClass ourClass, List<HaxeClass> classes) {
    for (HaxeClass haxeClass : classes) {
      for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
        if (haxeNamedComponent.isPublic() && haxeNamedComponent.isStatic() && haxeNamedComponent.getComponentName() != null) {
          final HaxeClassResolveResult resolveResult = HaxeResolveUtil.findFirstParameterClass(haxeNamedComponent);
          final HaxeClass resolvedClass = resolveResult.getHaxeClass();
          final HashSet<HaxeClass> baseClassesSet = ourClass != null ? HaxeResolveUtil.getBaseClassesSet(ourClass) : null;
          final boolean needToAdd = resolvedClass == null || resolvedClass == ourClass
            || (baseClassesSet != null && baseClassesSet.contains(resolvedClass));
          if (needToAdd) {
            variants.add(haxeNamedComponent.getComponentName());
            variantsWithExtension.add(haxeNamedComponent.getComponentName());
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

  private static void addAbstractUnderlyingClassVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    final HaxeClass underlyingClass = HaxeAbstractUtil.getAbstractUnderlyingClass(haxeClass);
    if (underlyingClass == null) {
      return;
    }
    addClassVariants(suggestedVariants, underlyingClass, filterByAccess);
  }

  private static void addClassStaticMembersVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }

    boolean extern = haxeClass.isExtern();
    boolean isEnum = haxeClass instanceof HaxeEnumDeclaration;
    boolean isAbstractEnum = HaxeAbstractEnumUtil.isAbstractEnum(haxeClass);

    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      final HaxeComponentName componentName = namedComponent.getComponentName();
      if(componentName != null) {
        if(isAbstractEnum && HaxeAbstractEnumUtil.couldBeAbstractEnumField(namedComponent)) {
          suggestedVariants.add(componentName);
        }
        else if ((extern || !needFilter) && (namedComponent.isStatic() || isEnum)) {
          suggestedVariants.add(componentName);
        }
      }
    }
  }

  private static void addClassNonStaticMembersVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }

    boolean extern = haxeClass.isExtern();
    boolean isAbstractEnum = HaxeAbstractEnumUtil.isAbstractEnum(haxeClass);
    boolean isAbstractForward = HaxeAbstractForwardUtil.isAbstractForward(haxeClass);

    if (isAbstractForward) {
      final List<HaxeNamedComponent> forwardingHaxeNamedComponents = HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(haxeClass);
      if (forwardingHaxeNamedComponents != null) {
        for (HaxeNamedComponent namedComponent : forwardingHaxeNamedComponents) {
          final boolean needFilter = filterByAccess && !namedComponent.isPublic();
          if ((extern || !needFilter) && !namedComponent.isStatic() && namedComponent.getComponentName() != null) {
            suggestedVariants.add(namedComponent.getComponentName());
          }
        }
      }
    }

    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if(isAbstractEnum && HaxeAbstractEnumUtil.couldBeAbstractEnumField(namedComponent)) {
        continue;
      }
      if ((extern || !needFilter) && !namedComponent.isStatic() && namedComponent.getComponentName() != null) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  @Nullable
  @Override
  public PsiElement getReferenceNameElement() {
    PsiElement child = findChildByType(HaxeTokenTypes.REFERENCE_EXPRESSION); // REFERENCE_NAME in Java
    return child;
  }

  @Nullable
  @Override
  public PsiReferenceParameterList getParameterList() {
    // TODO:  Unimplemented.
    LOG.warn("getParameterList is unimplemented");

    // REFERENCE_PARAMETER_LIST  in Java
    HaxeTypeParam child = (HaxeTypeParam) findChildByType(HaxeTokenTypes.TYPE_PARAM);
    //return child == null ? null : child.getTypeList();
    return null;
  }

  @NotNull
  @Override
  public PsiType[] getTypeParameters() {
    // TODO:  Unimplemented.
    LOG.warn("getTypeParameters is unimplemented");
    return new PsiType[0];
  }

  @Override
  public boolean isQualified() {
    return null != getQualifier();
  }

  @Override
  public String getQualifiedName() {
    return JavaSourceUtil.getReferenceText(this);
  }

  // PsiJavaReference overrides

  @Override
  public void processVariants(@NotNull PsiScopeProcessor processor) {
    // TODO:  Unimplemented.
    LOG.warn("processVariants is unimplemented");
  }

  // PsiQualifiedReference overrides

  @Nullable
  @Override
  public PsiElement getQualifier() {
    // Package/class that this type is part of; the part before
    // the last '.'.  However, that may only be partial, so adding
    // package information may also be necessary.
    PsiElement left = UsefulPsiTreeUtil.getChildOfType(this, HaxeTokenTypes.REFERENCE_EXPRESSION);
    boolean hasDot = nextSiblingIsADot(left);
    return hasDot ? left : null;
  }

  /* Determine if the element to the right of the given element in the AST
   * (at the same level) is a dot '.' separator.
   * Workhorse for getQualifier().
   * XXX: If we use this more than once, move it to a utility class, such as UsefulPsiTreeUtil.
   */
  private static boolean nextSiblingIsADot(PsiElement element) {
    if (null == element) return false;

    PsiElement   next = element.getNextSibling();
    ASTNode      node = ((null != next) ? next.getNode() : null);
    IElementType type = ((null != node) ? node.getElementType() : null);
    boolean      ret  = (null != type && type.equals(HaxeTokenTypes.ODOT));
    return ret;
  }

  @Nullable
  @Override
  public String getReferenceName() {
    // Unqualified name; the base name without any preceding
    // package/class name.
    // TODO: Figure out if this needs to split out any prefix.
    return getText();
  }

  // PsiExpression implementations

  @Nullable
  public PsiType getPsiType() {
    // XXX: EMB: Not sure about this.  Does a reference really have a sub-node giving the type?
    HaxeType ht = findChildByClass(HaxeType.class);
    return ((null == ht) ? null : ht.getPsiType());
  }

  // PsiExpression implementations

  //@Nullable
  //public PsiExpression getQualifierExpression() {
  //  final PsiElement qualifier = getQualifier();
  //  return qualifier instanceof PsiExpression ? (PsiExpression)qualifier : null;
  //}

  @Override
  public String toString() {
    String ss = super.toString();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // Unit tests don't want the extra data.  (Maybe we should fix the goldens?)
      String clazzName = this.getClass().getSimpleName();
      String text = getCanonicalText();
      ss += ":" + (null == text ? "<no text>" : text);
      ss += ":" + (null == clazzName ? "<anonymous>" : clazzName);
    }
    return ss;
  }
}
