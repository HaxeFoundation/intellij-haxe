/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2017-2018 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

  //static {  // Remove when finished debugging.
  //  LOG.setLevel(Level.TRACE);
  //  LOG.debug(" ========= Starting up debug logger for HaxeResolver. ==========");
  //}

  public static final HaxeResolver INSTANCE = new HaxeResolver();

  public static ThreadLocal<Boolean> isExtension = new ThreadLocal<>();

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("-----------------------------------------"));
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Resolving reference: " + reference.getText()));

    isExtension.set(false);

    List<? extends PsiElement> result = checkIsType(reference);
    if (result == null) result = checkIsFullyQualifiedStatement(reference);
    if (result == null) result = checkIsSuperExpression(reference);
    if (result == null) result = checkIsClassName(reference);
    if (result == null) result = checkIsChain(reference);
    if (result == null) result = checkIsAccessor(reference);
    if (result == null) result = checkByTreeWalk(reference);
    if (result == null) {
      // try super field
      List<? extends PsiElement> superElements =
        resolveByClassAndSymbol(PsiTreeUtil.getParentOfType(reference, HaxeClass.class), reference);
      if (!superElements.isEmpty()) {
        LogResolution(reference, "via super field.");
        return superElements;
      }

      HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
      if (fileModel != null) {
        String className = reference.getText();

        PsiElement target = HaxeResolveUtil.searchInSameFile(fileModel, className);
        if (target == null) target = HaxeResolveUtil.searchInImports(fileModel, className);
        if (target == null) target = HaxeResolveUtil.searchInSamePackage(fileModel, className);

        if (target != null) {
          return asList(target);
        }
      }

      LogResolution(reference, "failed after exhausting all options.");

      if (PsiNameHelper.getInstance(reference.getProject()).isQualifiedName(reference.getText())) {
        List<HaxeModel> resolvedPackage =
          HaxeProjectModel.fromElement(reference).resolve(new FullyQualifiedInfo(reference.getText()), reference.getResolveScope());
        if (resolvedPackage != null && !resolvedPackage.isEmpty() && resolvedPackage.get(0) instanceof HaxePackageModel) {
          LogResolution(reference, "via project qualified name.");
          return Collections.singletonList(resolvedPackage.get(0).getBasePsi());
        }
      }
    }

    return result == null ? ContainerUtil.emptyList() : result;
  }

  private List<? extends PsiElement> checkByTreeWalk(HaxeReference reference) {
    final List<PsiElement> result = new ArrayList<>();
    PsiTreeUtil.treeWalkUp(new ResolveScopeProcessor(result, reference.getText()), reference, null, new ResolveState());
    if (result.isEmpty()) return null;
    LogResolution(reference, "via tree walk.");
    return result;
  }

  private List<? extends PsiElement> checkIsAccessor(HaxeReference reference) {
    if (reference instanceof HaxePropertyAccessor) {
      final HaxeAccessorType accessorType = HaxeAccessorType.fromPsi(reference);
      if (accessorType != HaxeAccessorType.GET && accessorType != HaxeAccessorType.SET) return null;

      final HaxeFieldDeclaration varDeclaration = PsiTreeUtil.getParentOfType(reference, HaxeFieldDeclaration.class);
      if (varDeclaration == null) return null;

      final HaxeFieldModel fieldModel = new HaxeFieldModel(varDeclaration);
      final HaxeMethodModel method = accessorType == HaxeAccessorType.GET ? fieldModel.getGetterMethod() : fieldModel.getSetterMethod();

      if (method != null) {
        return asList(method.getBasePsi());
      }
    }

    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsChain(@NotNull HaxeReference reference) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(reference);
    if (leftReference != null) {
      List<? extends PsiElement> result = resolveChain(leftReference, reference);
      if (result != null && !result.isEmpty()) {
        LogResolution(reference, "via simple chain using leftReference.");
        return result;
      }
      LogResolution(reference, "via simple chain against package.");
      PsiElement item = resolveQualifiedReference(reference);
      if (item != null) {
        return asList(item);
      }
    }
    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsClassName(@NotNull HaxeReference reference) {
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(reference);
    if (resultClass != null) {
      LogResolution(reference, "via class qualified name.");
      return asList(resultClass.getComponentName());
    }
    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsSuperExpression(HaxeReference reference) {
    if (reference instanceof HaxeSuperExpression && reference.getParent() instanceof HaxeCallExpression) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(reference, HaxeClass.class);
      assert haxeClass != null;
      if (!haxeClass.getHaxeExtendsList().isEmpty()) {
        final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
        final HaxeClass superClass = ((HaxeReference)superExpression).resolveHaxeClass().getHaxeClass();
        final HaxeNamedComponent constructor =
          ((superClass == null) ? null : superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString()));
        LogResolution(reference, "because it's a super expression.");
        return asList(((constructor != null) ? constructor : superClass));
      }
    }

    return null;
  }

  @Nullable
  private List<? extends PsiElement> checkIsType(HaxeReference reference) {
    final HaxeType type = PsiTreeUtil.getParentOfType(reference, HaxeType.class);
    final HaxeClass haxeClassInType = HaxeResolveUtil.tryResolveClassByQName(type);
    if (type != null && haxeClassInType != null) {
      LogResolution(reference, "via parent type name.");
      return asList(haxeClassInType.getComponentName());
    }
    return null;
  }

  private List<? extends PsiElement> checkIsFullyQualifiedStatement(@NotNull HaxeReference reference) {
    if (PsiTreeUtil.getParentOfType(reference,
                                    HaxePackageStatement.class,
                                    HaxeImportStatement.class,
                                    HaxeUsingStatement.class) != null && reference instanceof HaxeReferenceExpression) {
      LogResolution(reference, "via parent/package import.");
      return asList(resolveQualifiedReference((HaxeReferenceExpression)reference));
    }
    return null;
  }

  private void LogResolution(HaxeReference ref, String tailmsg) {
    // Debug is always enabled if trace is enabled.
    if (LOG.isDebugEnabled()) {
      String message = "Resolved " + ref.getText() + " " + tailmsg;
      if (LOG.isTraceEnabled()) {
        LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), message);
      } else {
        LOG.debug(message);
      }
    }
  }

  /**
   * Resolve a chain reference, given two references: the qualifier, and the name.
   *
   * @param lefthandExpression - qualifying expression (e.g. "((ref = reference).getProject())")
   * @param reference          - field/method name to resolve.
   * @return the resolved element, if found; null, otherwise.
   */
  @Nullable
  private List<? extends PsiElement> resolveChain(HaxeReference lefthandExpression, HaxeReference reference) {
    String identifier =
      reference instanceof HaxeReferenceExpression ? ((HaxeReferenceExpression)reference).getIdentifier().getText() : reference.getText();
    HaxeClassResolveResult leftExpression = lefthandExpression.resolveHaxeClass();
    if (leftExpression.getHaxeClass() != null) {
      HaxeMemberModel member = leftExpression.getHaxeClass().getModel().getMember(identifier);
      if (member != null) {
        return Collections.singletonList(member.getBasePsi());
      }
    }
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg(null));
    final HaxeComponentName componentName = tryResolveHelperClass(lefthandExpression, identifier);
    if (componentName != null) {
      if (LOG.isTraceEnabled()) LOG.trace("Found component " + componentName.getText());
      return Collections.singletonList(componentName);
    }
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("trying keywords (super, new) arrays, literals, etc."));
    // Try resolving keywords (super, new), arrays, literals, etc.
    return resolveByClassAndSymbol(lefthandExpression.resolveHaxeClass(), reference);
  }

  private PsiElement resolveQualifiedReference(HaxeReference reference) {
    String qualifiedName = reference.getText();

    final FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(qualifiedName);
    List<HaxeModel> result = HaxeProjectModel.fromElement(reference).resolve(qualifiedInfo, reference.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.get(0);
      if (item instanceof HaxeFileModel) {
        HaxeClassModel mainClass = ((HaxeFileModel)item).getMainClassModel();
        if (mainClass != null) {
          return mainClass.getBasePsi();
        }
      }
      return item.getBasePsi();
    }

    return null;
  }

  /**
   * Test if the leftReference is a class name (either locally or in a super-class),
   * and if so, find the named field/method declared inside of it.
   *
   * @param leftReference - a potential class name.
   * @param helperName    - the field/method to find.
   * @return the name of the found field/method.  null if not found.
   */
  @Nullable
  private HaxeComponentName tryResolveHelperClass(HaxeReference leftReference, String helperName) {
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("leftReference=" + leftReference + " helperName=" + helperName));
    HaxeComponentName componentName = null;
    HaxeClass leftResultClass = HaxeResolveUtil.tryResolveClassByQName(leftReference);
    if (leftResultClass != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace(traceMsg("Found a left result via QName: " + (leftResultClass.getText() != null ? leftResultClass : "<no text>")));
      }
      // helper reference via class com.bar.FooClass.HelperClass
      final HaxeClass componentDeclaration =
        HaxeResolveUtil.findComponentDeclaration(leftResultClass.getContainingFile(), helperName);
      componentName = componentDeclaration == null ? null : componentDeclaration.getComponentName();
    } else {
      // try to find component at abstract forwarding underlying class
      leftResultClass = leftReference.resolveHaxeClass().getHaxeClass();
      if (LOG.isTraceEnabled()) {
        String resultClassName = leftResultClass != null ? leftResultClass.getText() : null;
        if (LOG.isTraceEnabled()) {
          LOG.trace(traceMsg("Found abstract left result:" + (resultClassName != null ? resultClassName : "<no text>")));
        }
      }
      if (leftResultClass != null) {
        HaxeClassModel model = leftResultClass.getModel();
        HaxeMemberModel member = model.getMember(helperName);
        if (member != null) return member.getNamePsi();

        if (model.isAbstract() && ((HaxeAbstractClassModel)model).hasForwards()) {
          final List<HaxeNamedComponent> forwardingHaxeNamedComponents =
            HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(leftResultClass);
          if (forwardingHaxeNamedComponents != null) {
            for (HaxeNamedComponent namedComponent : forwardingHaxeNamedComponents) {
              final HaxeComponentName forwardingComponentName = namedComponent.getComponentName();
              if (forwardingComponentName != null && forwardingComponentName.getText().equals(helperName)) {
                componentName = forwardingComponentName;
                break;
              }
            }
          }
        }
      }
    }
    if (LOG.isTraceEnabled()) {
      String ctext = componentName != null ? componentName.getText() : null;
      if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Found component name " + (ctext != null ? ctext : "<no text>")));
    }
    return componentName;
  }

  private static List<? extends PsiElement> asList(@Nullable PsiElement element) {
    if (LOG.isDebugEnabled()) LOG.debug("Resolved as " + (element == null ? "empty result list." : element.toString()));
    return element == null ? Collections.emptyList() : Collections.singletonList(element);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClassResolveResult resolveResult,
                                                                    @NotNull HaxeReference reference) {
    if (resolveResult == null) {
      if (LOG.isDebugEnabled()) LOG.debug("Resolved as empty result list. (resolveByClassAndSymbol)");
    }
    return resolveResult == null ? Collections.<PsiElement>emptyList() : resolveByClassAndSymbol(resolveResult.getHaxeClass(), reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    if (leftClass != null) {
      final HaxeClassModel classModel = leftClass.getModel();
      HaxeMemberModel member = classModel.getMember(reference.getReferenceName());
      if (member != null) return asList(member.getNamePsi());

      // if class is abstract try find in forwards
      if (leftClass.isAbstract()) {
        HaxeAbstractClassModel model = (HaxeAbstractClassModel)leftClass.getModel();
        if (model.isForwarded(reference.getReferenceName())) {
          final HaxeClass underlyingClass = model.getUnderlyingClass();
          if (underlyingClass != null) {
            member = underlyingClass.getModel().getMember(reference.getReferenceName());
            if (member != null) {
              return asList(member.getNamePsi());
            }
          }
        }
      }
      // try find using
      HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
      if (fileModel != null) {
        for (HaxeUsingModel model : fileModel.getUsingModels()) {
          HaxeMethodModel method = model.findExtensionMethod(reference.getReferenceName(), leftClass);
          if (method != null) {
            isExtension.set(true);
            return asList(method.getNamePsi());
          }
        }
      }
    }

    return Collections.emptyList();
  }

  private String traceMsg(String msg) {
    return HaxeDebugUtil.traceMessage(msg, 120);
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
      HaxeComponentName componentName = null;
      if (element instanceof HaxeNamedComponent) {
        componentName = ((HaxeNamedComponent)element).getComponentName();
      } else if (element instanceof HaxeOpenParameterList) {
        componentName = ((HaxeOpenParameterList)element).getComponentName();
      }
      if (componentName != null && name.equals(componentName.getText())) {
        result.add(componentName);
        return false;
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