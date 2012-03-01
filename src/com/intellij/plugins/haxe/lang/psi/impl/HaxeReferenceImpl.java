package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReferenceExpression, PsiPolyVariantReference {

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
    final HaxeType type = PsiTreeUtil.getParentOfType(this, HaxeType.class);
    if (type != null) {
      return resolveType(type);
    }

    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiTreeUtil.treeWalkUp(new MyPsiScopeProcessor(result), getParent(), null, new ResolveState());
    if (result.size() > 0) {
      return toCandidateInfoArray(result);
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  private ResolveResult[] toCandidateInfoArray(List<PsiElement> elements) {
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
    //todo
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  private static ResolveResult[] resolveType(@Nullable HaxeType type) {
    if (type == null || type.getContext() == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    String qName = type.getText();
    if (qName.indexOf('.') == -1) {
      final HaxeImportStatement importStatement = UsefulPsiTreeUtil.findImportByClass(type, qName);
      if (importStatement != null) {
        qName = importStatement.getExpression().getText();
      }
    }

    final HaxeClassDeclaration classDeclaration = HaxeResolveUtil.findClassByQName(qName, type.getContext());
    if (classDeclaration != null) {
      return new ResolveResult[]{new CandidateInfo(classDeclaration.getIdentifier(), null)};
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  private class MyPsiScopeProcessor implements PsiScopeProcessor {
    private final List<PsiElement> result;

    private MyPsiScopeProcessor(List<PsiElement> result) {
      this.result = result;
    }

    @Override
    public boolean execute(PsiElement element, ResolveState state) {
      if (element instanceof PsiIdentifiedElement) {
        final PsiIdentifiedElement psiIdentifiedElement = (PsiIdentifiedElement)element;
        if (psiIdentifiedElement.getIdentifier() == null) {
          return true;
        }
        if (getIdentifier().getText().equals(psiIdentifiedElement.getIdentifier().getText())) {
          result.add(((PsiIdentifiedElement)element).getIdentifier());
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
}
