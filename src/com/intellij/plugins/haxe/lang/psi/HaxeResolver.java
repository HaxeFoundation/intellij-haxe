package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  public static final HaxeResolver INSTANCE = new HaxeResolver();

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    final HaxeType type = PsiTreeUtil.getParentOfType(reference, HaxeType.class);
    final HaxeClass haxeClassInType = HaxeResolveUtil.tryResolveClassByQName(type);
    if (type != null && haxeClassInType != null) {
      return toCandidateInfoArray(haxeClassInType.getComponentName());
    }

    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(reference);
    if (resultClass != null) {
      return toCandidateInfoArray(resultClass.getComponentName());
    }

    final PsiPackage psiPackage = JavaPsiFacade.getInstance(reference.getProject()).findPackage(reference.getText());
    if (psiPackage != null) {
      return toCandidateInfoArray(psiPackage);
    }

    // if not first in chain
    // foo.bar.baz
    final HaxeReference referenceExpression = HaxeResolveUtil.getLeftReference(reference);
    if (referenceExpression != null && reference.getParent() instanceof HaxeReference) {
      return resolveByClassAndSymbol(referenceExpression.resolveHaxeClass(), reference.getText());
    }

    // then maybe chain
    // node(foo.node(bar)).node(baz)
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(reference, HaxeReference.class);
    if (childReferences != null && childReferences.length == 2) {
      return resolveByClassAndSymbol(childReferences[0].resolveHaxeClass(), childReferences[1].getText());
    }
    if (this instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
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
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, reference.getCanonicalText()), reference, null, new ResolveState());
    if (!result.isEmpty()) {
      return result;
    }

    for (HaxeClass haxeClass : HaxeResolveUtil.findUsingClasses(reference.getContainingFile())) {
      final HaxeNamedComponent namedSubComponent = HaxeResolveUtil.findNamedSubComponent(haxeClass, reference.getCanonicalText());
      if (namedSubComponent != null) {
        return toCandidateInfoArray(namedSubComponent);
      }
    }

    // try super field
    return resolveByClassAndSymbol(PsiTreeUtil.getParentOfType(reference, HaxeClass.class), reference.getText());
  }

  private static List<? extends PsiElement> toCandidateInfoArray(@Nullable PsiElement element) {
    return element == null ? Collections.<PsiElement>emptyList() : Arrays.asList(element);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClassResolveResult resolveResult,
                                                                    @NotNull String symbolName) {
    return resolveResult == null ? Collections.<PsiElement>emptyList() : resolveByClassAndSymbol(resolveResult.getHaxeClass(), symbolName);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass referenceClass, @NotNull String symbolName) {
    final HaxeNamedComponent namedSubComponent =
      HaxeResolveUtil.findNamedSubComponent(referenceClass, symbolName);
    final HaxeComponentName componentName = namedSubComponent == null ? null : namedSubComponent.getComponentName();
    return toCandidateInfoArray(componentName);
  }

  private class ResolveScopeProcessor implements PsiScopeProcessor {
    private final List<PsiElement> result;
    final String name;

    private ResolveScopeProcessor(List<PsiElement> result, String name) {
      this.result = result;
      this.name = name;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, ResolveState state) {
      if (element instanceof HaxeNamedComponent) {
        final HaxeComponentName componentName = ((HaxeNamedComponent)element).getComponentName();
        if (componentName != null && name.equals(componentName.getText())) {
          result.add(componentName);
          return false;
        }
      }
      return true;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
      return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }
  }
}
