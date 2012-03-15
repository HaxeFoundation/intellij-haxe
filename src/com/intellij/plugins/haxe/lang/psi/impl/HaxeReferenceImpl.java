package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.HaxeLookupElement;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
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

  @Nullable
  @Override
  public HaxeClass getHaxeClass() {
    return HaxeResolveUtil.getHaxeClass(resolve());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final HaxeType type = PsiTreeUtil.getParentOfType(this, HaxeType.class);
    if (type != null) {
      return toCandidateInfoArray(HaxeResolveUtil.resolveType(type));
    }

    // if not first in chain
    // foo.bar.baz
    final HaxeReference referenceExpression = PsiTreeUtil.getChildOfType(this, HaxeReference.class);
    final HaxeIdentifier identifier = PsiTreeUtil.getChildOfType(this, HaxeIdentifier.class);
    if (referenceExpression != null && identifier != null) {
      final HaxeNamedComponent namedSubComponent =
        HaxeResolveUtil.getNamedSubComponent(referenceExpression.getHaxeClass(), identifier.getText());
      final HaxeComponentName componentName = namedSubComponent == null ? null : namedSubComponent.getComponentName();
      return toCandidateInfoArray(componentName);
    }
    // chain
    // foo.bar
    if (PsiTreeUtil.getChildrenOfType(this, HaxeReference.class) != null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result), getParent(), null, new ResolveState());
    if (result.size() > 0) {
      return toCandidateInfoArray(result);
    }
    return ResolveResult.EMPTY_ARRAY;
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
    //todo
    return null;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    //todo
    return null;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return resolve() == element;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final Set<HaxeComponentName> suggestedVariants = new THashSet<HaxeComponentName>();
    PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
    return HaxeLookupElement.convert(suggestedVariants).toArray();
  }

  private class ResolveScopeProcessor implements PsiScopeProcessor {
    private final List<PsiElement> result;

    private ResolveScopeProcessor(List<PsiElement> result) {
      this.result = result;
    }

    @Override
    public boolean execute(PsiElement element, ResolveState state) {
      if (element instanceof HaxeNamedComponent) {
        final HaxeNamedComponent haxeNamedComponent = (HaxeNamedComponent)element;
        if (haxeNamedComponent.getComponentName() == null) {
          return true;
        }
        if (getIdentifier().getText().equals(haxeNamedComponent.getComponentName().getText())) {
          result.add(((HaxeNamedComponent)element).getComponentName());
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
