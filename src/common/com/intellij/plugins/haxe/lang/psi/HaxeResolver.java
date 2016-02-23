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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
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
  public static final String IMPORT_EXTENSION = ".hx";

  public static ThreadLocal<Boolean> isExtension = new ThreadLocal<Boolean>();

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    isExtension.set(false);

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

    // See if it's a source file we're importing... (most likely a convenience library, such as haxe.macro.Tools)
    final PsiFile importFile = resolveImportFile(reference);
    if (null != importFile) {
      return toCandidateInfoArray(importFile);
    }

    // Awaiting statement with package references
    // TODO: optimize, get root element and check class
    if(PsiTreeUtil.getParentOfType(reference,
                                   HaxePackageStatement.class,
                                   HaxeImportStatementRegular.class,
                                   HaxeImportStatementWithInSupport.class,
                                   HaxeImportStatementWithWildcard.class,
                                   HaxeUsingStatement.class) != null) {
      return toCandidateInfoArray(resolvePackage(reference));
    }

    // if not first in chain
    // foo.bar.baz
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    if (leftReference != null && reference.getParent() instanceof HaxeReference) {
      List<? extends PsiElement> result = resolveChain(leftReference, reference);
      if(result != null && !result.isEmpty()) {
        return result;
      }
      return toCandidateInfoArray(resolvePackage(reference));
    }

    // then maybe chain
    // node(foo.node(bar)).node(baz)
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(reference, HaxeReference.class);
    if (childReferences != null && childReferences.length == 2) {
      List<? extends PsiElement> result = resolveChain(childReferences[0], childReferences[1]);
      if(result != null && !result.isEmpty()) {
        return result;
      }
      return toCandidateInfoArray(resolvePackage(reference));
    }

    if (reference instanceof HaxeSuperExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
      assert haxeClass != null;
      if (!haxeClass.getHaxeExtendsList().isEmpty()) {
        final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
        final HaxeClass superClass = superExpression instanceof HaxeReference
                                     ? ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass()
                                     : null;
        final HaxeNamedComponent constructor = ((superClass == null) ? null : superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString()));
        return toCandidateInfoArray(((constructor != null) ? constructor : superClass));
      }
    }

    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, reference.getCanonicalText()), reference, null, new ResolveState());
    if (!result.isEmpty()) {
      return result;
    }

    PsiFile psiFile = reference.getContainingFile();

    if (reference instanceof HaxeReferenceExpression) {
      String text1 = reference.getText();
      PsiElement element = null;
      if (reference.getParent() instanceof HaxeReferenceExpression) {
        element = reference;
        while (element.getParent() instanceof HaxeReferenceExpression) {
          element = element.getParent();
        }

        if (element != null) {
            element = element.getFirstChild();
            PsiElement lastChild = element.getLastChild();
            if (element instanceof HaxeReferenceExpression && lastChild instanceof HaxeReferenceExpression && element.getChildren().length == 3) {
              HaxeClass classByQName = HaxeResolveUtil.findClassByQName(element.getText(), element.getContext());
              if (classByQName != null) {
                HaxeNamedComponent namedSubComponent =
                  HaxeResolveUtil.findNamedSubComponent(classByQName, ((HaxeReferenceExpression)lastChild).getIdentifier().getText());

                if (namedSubComponent != null) {
                  result.add(namedSubComponent.getComponentName().getIdentifier());
                  return result;
                }
              }
            }
        }
      }
    }

    // try super field
    List<? extends PsiElement> superElements = resolveByClassAndSymbol(PsiTreeUtil.getParentOfType(reference, HaxeClass.class), reference);
    if (!superElements.isEmpty()) {
      return superElements;
    }

    if (PsiNameHelper.getInstance(reference.getProject()).isQualifiedName(reference.getText())) {
      PsiPackageReference packageReference = new PackageReferenceSet(reference.getText(), reference, 0).getLastReference();
      PsiElement packageTarget = packageReference != null ? packageReference.resolve() : null;
      if (packageTarget != null) {
        return Arrays.asList(packageTarget);
      }
    }

    List<PsiElement> importStatementWithWildcardList = ContainerUtil.findAll(psiFile.getChildren(), new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        return element instanceof HaxeImportStatementWithWildcard &&
               UsefulPsiTreeUtil.isImportStatementWildcardForType(UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(
                 (HaxeImportStatementWithWildcard)element));
      }
    });

    for (PsiElement importStatementWithWildcard : importStatementWithWildcardList) {
      HaxeImportStatementWithWildcard importStatementWithWildcard1 = (HaxeImportStatementWithWildcard)importStatementWithWildcard;
      String qNameForImportStatementWithWildcardType =
        UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(importStatementWithWildcard1);

      HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(qNameForImportStatementWithWildcardType, importStatementWithWildcard1.getContext());

      if (haxeClass != null) {
        String referenceText = reference.getText();
        HaxeNamedComponent namedSubComponent = HaxeResolveUtil.findNamedSubComponent(haxeClass, referenceText);

        if (namedSubComponent != null && namedSubComponent.isStatic()) {
          result.add(namedSubComponent.getComponentName().getIdentifier());
          return result;
        }
      }
    }

    List<PsiElement> importStatementWithInSupportList = ContainerUtil.findAll(psiFile.getChildren(), new Condition<PsiElement>() {
      @Override
      public boolean value(PsiElement element) {
        if (element instanceof HaxeImportStatementWithInSupport) {
          PsiElement resolve;
          HaxeReferenceExpression referenceExpression = ((HaxeImportStatementWithInSupport)element).getReferenceExpression();
          resolve = referenceExpression.resolve();

          if (resolve != null) {
            return resolve instanceof PsiMethod;
          }
        }
        return false;
      }
    });

    HaxeImportStatementWithInSupport importStatementWithInSupport;
    for (int i = 0; i < importStatementWithInSupportList.size(); i++) {
      importStatementWithInSupport = (HaxeImportStatementWithInSupport)importStatementWithInSupportList.get(i);
      if (reference.getText().equals(importStatementWithInSupport.getIdentifier().getText())) {
        result.add(importStatementWithInSupport.getReferenceExpression().resolve());
        return result;
      }
    }

    return ContainerUtil.emptyList();
  }

  /**
   * Resolve a chain reference, given two references: the qualifier, and the name.
   *
   * @param lefthandExpression - qualifying expression (e.g. "((ref = reference).getProject())")
   * @param reference - field/method name to resolve.
   * @return the resolved element, if found; null, otherwise.
   */
  @Nullable
  private List<? extends PsiElement> resolveChain(HaxeReference lefthandExpression, HaxeReference reference) {
    final HaxeComponentName componentName = tryResolveHelperClass(lefthandExpression, reference.getText());
    if (componentName != null) {
      return Arrays.asList(componentName);
    }
    // Try resolving keywords (super, new), arrays, literals, etc.
    List<? extends PsiElement> resolvedList = resolveByClassAndSymbol(lefthandExpression.resolveHaxeClass(), reference);
    return resolvedList;
  }

  /**
   * Resolve a reference into a specific source file.  (.hx extension is added to the reference text.)
   *
   * @param reference to resolve.
   * @return a PsiFile if the file was found and is part of the project; null, otherwise.
   */
  @Nullable
  private PsiFile resolveImportFile(HaxeReference reference) {
    if (null == reference) return null;

    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    final String leftName = leftReference == null ? "" : leftReference.getQualifiedName();

    final String ctext = reference.getQualifiedName();
    final String packageName = leftName + StringUtil.getPackageName(ctext);
    final String fileName = StringUtil.getShortName(ctext) + IMPORT_EXTENSION;

    PsiFile importPsiFile = null;
    final PsiPackage importPackage = JavaPsiFacade.getInstance(reference.getProject()).findPackage(packageName);
    if (null != importPackage) {
      for (PsiDirectory dir : importPackage.getDirectories()) {
        VirtualFile importDir = dir == null ? null : dir.getVirtualFile();
        VirtualFile importVFile = importDir == null ? null : importDir.findChild(fileName);
        importPsiFile = importVFile == null ? null : PsiManager.getInstance(reference.getProject()).findFile(importVFile);
        if (importPsiFile != null) {
          // for addition case-sensetive check because find file is not case-sensetive
          if(!fileName.equals(importPsiFile.getName())) {
            importPsiFile = null;
          }
          else {
            break;
          }
        }
      }
    }

    return importPsiFile;
  }

  private PsiPackage resolvePackage(HaxeReference reference) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    String packageName = reference.getText();
    if(leftReference != null && reference.getParent() instanceof HaxeReference) {
      packageName = leftReference.getText() + "." + packageName;
    }
    return JavaPsiFacade.getInstance(reference.getProject()).findPackage(packageName);
  }

  /**
   * Test if the leftReference is a class name (either locally or in a super-class),
   * and if so, find the named field/method declared inside of it.
   *
   * @param leftReference - a potential class name.
   * @param helperName - the field/method to find.
   * @return the name of the found field/method.  null if not found.
   */
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
          isExtension.set(true);
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
