package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.HaxeLookupElement;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
  public HaxeClassResolveResult resolveHaxeClass() {
    if (this instanceof HaxeThisExpression) {
      return new HaxeClassResolveResult(PsiTreeUtil.getParentOfType(this, HaxeClass.class));
    }
    if (this instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      assert haxeClass != null;
      if (haxeClass.getExtendsList().isEmpty()) {
        return new HaxeClassResolveResult(null);
      }
      final HaxeExpression superExpression = haxeClass.getExtendsList().get(0).getExpression();
      final HaxeClassResolveResult superClassResolveResult = superExpression instanceof HaxeReference
                                                             ? ((HaxeReference)superExpression).resolveHaxeClass()
                                                             : new HaxeClassResolveResult(null);
      superClassResolveResult.specializeByParameters(haxeClass.getExtendsList().get(0).getTypeParam());
      return superClassResolveResult;
    }
    if (this instanceof HaxeLiteralExpression) {
      final LeafPsiElement child = (LeafPsiElement)getFirstChild();
      final IElementType childTokenType = child == null ? null : child.getElementType();
      return new HaxeClassResolveResult(HaxeResolveUtil.findClassByQName(getLiteralClassName(childTokenType), this));
    }
    if (this instanceof HaxeArrayLiteral) {
      return new HaxeClassResolveResult(HaxeResolveUtil.findClassByQName(getLiteralClassName(getTokenType()), this));
    }
    if (this instanceof HaxeNewExpression) {
      final HaxeClassResolveResult result = new HaxeClassResolveResult(HaxeResolveUtil.resolveClass(((HaxeNewExpression)this).getType()));
      result.specialize(this);
      return result;
    }
    if (this instanceof HaxeCallExpression) {
      final HaxeExpression expression = ((HaxeCallExpression)this).getExpression();
      final HaxeClassResolveResult leftResult = tryGetLeftResolveResult(expression);
      if (expression instanceof HaxeReference) {
        final HaxeClassResolveResult result =
          HaxeResolveUtil.getHaxeClass(((HaxeReference)expression).resolve(), leftResult.getSpecializations());
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
          return resolveResult.getSpecializations().get(resolveResultHaxeClass, "T");
        }
        // __get method
        return HaxeResolveUtil.getHaxeClass(resolveResultHaxeClass.findMethodByName("__get"), resolveResult.getSpecializations());
      }
    }
    HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClass(resolve(), tryGetLeftResolveResult(this).getSpecializations());
    if (result.getHaxeClass() == null) {
      result = new HaxeClassResolveResult(HaxeResolveUtil.findClassByQName(getText(), this));
    }
    return result;
  }

  @NotNull
  private static HaxeClassResolveResult tryGetLeftResolveResult(HaxeExpression expression) {
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(expression, HaxeReference.class);
    final HaxeReference leftReference = childReferences != null ? childReferences[0] : null;
    return leftReference != null
           ? leftReference.resolveHaxeClass()
           : new HaxeClassResolveResult(PsiTreeUtil.getParentOfType(expression, HaxeClass.class));
  }

  @Nullable
  private static String getLiteralClassName(IElementType type) {
    if (type == HaxeTokenTypes.LITSTRING || type == HaxeTokenTypes.LITCHAR) {
      return "String";
    }
    else if (type == HaxeTokenTypes.HAXE_ARRAYLITERAL) {
      return "Array";
    }
    else if (type == HaxeTokenTypes.LITFLOAT) {
      return "Float";
    }
    else if (type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITOCT) {
      return "Int";
    }
    return null;
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final HaxeType type = PsiTreeUtil.getParentOfType(this, HaxeType.class);
    final HaxeClass haxeClassInType = HaxeResolveUtil.resolveClass(type);
    if (type != null && haxeClassInType != null) {
      return toCandidateInfoArray(haxeClassInType.getComponentName());
    }

    // if not first in chain
    // foo.bar.baz
    final HaxeReference referenceExpression = PsiTreeUtil.getPrevSiblingOfType(this, HaxeReference.class);
    if (referenceExpression != null && getParent() instanceof HaxeReference) {
      return resolveByClassAndSymbol(referenceExpression.resolveHaxeClass(), getText());
    }

    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.resolveClass(this);
    if (resultClass != null) {
      return toCandidateInfoArray(resultClass.getComponentName());
    }

    // then maybe chain
    // node(foo.node(bar)).node(baz)
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
    if (childReferences != null && childReferences.length == 2) {
      return resolveByClassAndSymbol(childReferences[0].resolveHaxeClass(), childReferences[1].getText());
    }
    if (this instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      assert haxeClass != null;
      if (!haxeClass.getExtendsList().isEmpty()) {
        final HaxeExpression superExpression = haxeClass.getExtendsList().get(0).getExpression();
        final HaxeClass superClass = superExpression instanceof HaxeReference
                                     ? ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass()
                                     : null;
        final HaxeNamedComponent constructor = superClass == null ? null : superClass.findMethodByName("new");
        return toCandidateInfoArray(constructor != null ? constructor : superClass);
      }
    }

    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result), getParent(), null, new ResolveState());
    if (result.size() > 0) {
      return toCandidateInfoArray(result);
    }

    for (HaxeClass haxeClass : HaxeResolveUtil.findUsingClasses(getContainingFile())) {
      final HaxeNamedComponent namedSubComponent = HaxeResolveUtil.findNamedSubComponent(haxeClass, getCanonicalText());
      if (namedSubComponent != null) {
        return toCandidateInfoArray(namedSubComponent);
      }
    }

    // try super field
    return resolveByClassAndSymbol(PsiTreeUtil.getParentOfType(this, HaxeClass.class), getText());
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
  private static ResolveResult[] toCandidateInfoArray(List<PsiElement> elements) {
    final ResolveResult[] result = new ResolveResult[elements.size()];
    for (int i = 0, size = elements.size(); i < size; i++) {
      result[i] = new CandidateInfo(elements.get(i), null);
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
    return this;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return resolve() == element;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final Set<HaxeComponentName> suggestedVariants = new THashSet<HaxeComponentName>();

    // if not first in chain
    // foo.bar.baz
    final HaxeReference referenceExpression = PsiTreeUtil.getPrevSiblingOfType(this, HaxeReference.class);
    if (referenceExpression != null && getParent() instanceof HaxeReference) {
      addClassVariants(suggestedVariants, referenceExpression.resolveHaxeClass().getHaxeClass(),
                       !(referenceExpression instanceof HaxeThisExpression));
    }
    else {
      // if chain
      // node(foo.node(bar)).node(baz)
      final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(this, HaxeReference.class);
      if (childReferences != null && childReferences.length == 2) {
        return resolveByClassAndSymbol(childReferences[0].resolveHaxeClass(), childReferences[1].getText());
      }
      else {
        PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
        addClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), false);
        addUsingVariants(suggestedVariants, HaxeResolveUtil.findUsingClasses(getContainingFile()));
      }
    }

    return HaxeLookupElement.convert(suggestedVariants).toArray();
  }

  private static void addUsingVariants(Set<HaxeComponentName> variants, List<HaxeClass> classes) {
    for (HaxeClass haxeClass : classes) {
      for (HaxeNamedComponent haxeNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)) {
        if (haxeNamedComponent.isPublic() && haxeNamedComponent.isStatic() && haxeNamedComponent.getComponentName() != null) {
          variants.add(haxeNamedComponent.getComponentName());
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

  private class ResolveScopeProcessor implements PsiScopeProcessor {
    private final List<PsiElement> result;

    private ResolveScopeProcessor(List<PsiElement> result) {
      this.result = result;
    }

    @Override
    public boolean execute(PsiElement element, ResolveState state) {
      if (element instanceof HaxeNamedComponent) {
        final String name = getCanonicalText();
        final HaxeComponentName componentName = ((HaxeNamedComponent)element).getComponentName();
        if (componentName != null && name.equals(componentName.getText())) {
          result.add(componentName);
          return false;
        }
      }
      return true;
    }

    @Override
    public <T> T getHint(Key<T> hintKey) {
      return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }
  }

  private static class ComponentNameScopeProcessor implements PsiScopeProcessor {
    private final Set<HaxeComponentName> result;

    private ComponentNameScopeProcessor(Set<HaxeComponentName> result) {
      this.result = result;
    }

    @Override
    public boolean execute(PsiElement element, ResolveState state) {
      if (element instanceof HaxeNamedComponent) {
        final HaxeNamedComponent haxeNamedComponent = (HaxeNamedComponent)element;
        if (haxeNamedComponent.getComponentName() != null) {
          result.add(haxeNamedComponent.getComponentName());
        }
      }
      return true;
    }

    @Override
    public <T> T getHint(Key<T> hintKey) {
      return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }
  }
}
