package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
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
      final HaxeComponentName componentName = tryResolveHelperClass(referenceExpression, reference.getText());
      return componentName != null
             ? Arrays.asList(componentName)
             : resolveByClassAndSymbol(referenceExpression.resolveHaxeClass(), reference);
    }

    // then maybe chain
    // node(foo.node(bar)).node(baz)
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(reference, HaxeReference.class);
    if (childReferences != null && childReferences.length == 2) {
      final HaxeComponentName componentName = tryResolveHelperClass(childReferences[0], childReferences[1].getText());
      // try member
      return componentName != null
             ? Arrays.asList(componentName)
             : resolveByClassAndSymbol(childReferences[0].resolveHaxeClass(), childReferences[1]);
    }
    if (reference instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
      assert haxeClass != null;
      if (!haxeClass.getExtendsList().isEmpty()) {
        final HaxeExpression superExpression = haxeClass.getExtendsList().get(0).getReferenceExpression();
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

    // try super field
    List<? extends PsiElement> superElements = resolveByClassAndSymbol(PsiTreeUtil.getParentOfType(reference, HaxeClass.class), reference);
    if (!superElements.isEmpty()) {
      return superElements;
    }

    if (JavaPsiFacade.getInstance(reference.getProject()).getNameHelper().isQualifiedName(reference.getText())) {
      PsiPackageReference packageReference = new PackageReferenceSet(reference.getText(), reference, 0).getLastReference();
      PsiElement packageTarget = packageReference != null ? packageReference.resolve() : null;
      if (packageTarget != null) {
        return Arrays.asList(packageTarget);
      }
    }

    return ContainerUtil.emptyList();
  }

  @Nullable
  private HaxeComponentName tryResolveHelperClass(HaxeReference leftReference, String helperName) {
    HaxeComponentName componentName = null;
    final HaxeClass leftResultClass = HaxeResolveUtil.tryResolveClassByQName(leftReference);
    if (leftResultClass != null) {
      // helper reference via class com.bar.FooClass.HelperClass
      final HaxeClass componentDeclaration =
        HaxeResolveUtil.findComponentDeclaration(leftResultClass.getContainingFile(), helperName);
      componentName = componentDeclaration == null ? null : componentDeclaration.getComponentName();
    }
    return componentName;
  }

  private static List<? extends PsiElement> toCandidateInfoArray(@Nullable PsiElement element) {
    return element == null ? Collections.<PsiElement>emptyList() : Arrays.asList(element);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClassResolveResult resolveResult,
                                                                    @NotNull HaxeReference reference) {
    return resolveResult == null ? Collections.<PsiElement>emptyList() : resolveByClassAndSymbol(resolveResult.getHaxeClass(), reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    final HaxeNamedComponent namedSubComponent =
      HaxeResolveUtil.findNamedSubComponent(leftClass, reference.getText());
    final HaxeComponentName componentName = namedSubComponent == null ? null : namedSubComponent.getComponentName();
    if (componentName != null) {
      return toCandidateInfoArray(componentName);
    }
    // try find using
    for (HaxeClass haxeClass : HaxeResolveUtil.findUsingClasses(reference.getContainingFile())) {
      final HaxeNamedComponent haxeNamedComponent = HaxeResolveUtil.findNamedSubComponent(haxeClass, reference.getCanonicalText());
      if (haxeNamedComponent != null &&
          haxeNamedComponent.isPublic() &&
          haxeNamedComponent.isStatic() &&
          haxeNamedComponent.getComponentName() != null) {
        final HaxeClassResolveResult resolveResult = HaxeResolveUtil.findFirstParameterClass(haxeNamedComponent);
        final boolean needToAdd = resolveResult.getHaxeClass() == null || resolveResult.getHaxeClass() == leftClass;
        if (needToAdd) {
          return toCandidateInfoArray(haxeNamedComponent.getComponentName());
        }
      }
    }
    return Collections.emptyList();
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
