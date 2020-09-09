/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2017-2020 Eric Bishton
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

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ArrayListSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.plugins.haxe.util.HaxeStringUtil.elide;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  public static final int MAX_DEBUG_MESSAGE_LENGTH = 200;
  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

  //static {  // Remove when finished debugging.
  //  LOG.setLevel(Level.DEBUG);
  //  LOG.debug(" ========= Starting up debug logger for HaxeResolver. ==========");
  //}

  public static final HaxeResolver INSTANCE = new HaxeResolver();

  public static ThreadLocal<Boolean> isExtension = ThreadLocal.withInitial(()->new Boolean(false));
  public static ThreadLocal<Stack<String>> referencesProcessing = ThreadLocal.withInitial(()->new Stack<String>());

  private static boolean reportCacheMetrics = false;   // Should always be false when checked in.
  private static AtomicInteger dumbRequests = new AtomicInteger(0);
  private static AtomicInteger requests = new AtomicInteger(0);
  private static AtomicInteger resolves = new AtomicInteger(0);
  private final static int REPORT_FREQUENCY = 100;

  public static final List<? extends PsiElement> EMPTY_LIST = Collections.emptyList();

  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    // Set this true when debugging the resolver.
    boolean skipCachingForDebug = false;  // Should always be false when checked in.

    // Kill circular resolutions -- before checking the cache.
    if (isResolving(reference)) {
      reportSkip(reference);
      return EMPTY_LIST;
    }

    // If we are in dumb mode (e.g. we are still indexing files and resolving may
    // fail until the indices are complete), we don't want to cache the (likely incorrect)
    // results.
    boolean isDumb = DumbService.isDumb(reference.getProject());
    boolean skipCaching = skipCachingForDebug || isDumb;
    List<? extends PsiElement> elements
      = skipCaching ? doResolve(reference, incompleteCode)
                    : ResolveCache.getInstance(reference.getProject()).resolveWithCaching(
                        reference, this::doResolve,true, incompleteCode);

    if (reportCacheMetrics) {
      if (skipCachingForDebug) {
        LOG.debug("Resolve cache is disabled.  No metrics computed.");
        reportCacheMetrics = false;
      } else {
        int dumb = isDumb ? dumbRequests.incrementAndGet() : dumbRequests.get();
        int requestCount = isDumb ? requests.get() : requests.incrementAndGet();
        if ((dumb + requestCount) % REPORT_FREQUENCY == 0) {
          int res = resolves.get();
          Formatter formatter = new Formatter();
          formatter.format("Resolve requests: %d; cache misses: %d; (%2.2f%% effective); Dumb requests: %d",
                           requestCount, res,
                           (1.0 - (Float.intBitsToFloat(res)/Float.intBitsToFloat(requestCount))) * 100,
                           dumb);
          LOG.debug(formatter.toString());
        }
      }
    }

    return elements;
  }

  private boolean isResolving(@NotNull HaxeReference reference) {
    Stack<String> stack = referencesProcessing.get();
    String referenceText = reference.getText();
    return stack.contains(referenceText);
  }

  private void reportSkip(HaxeReference reference) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(traceMsg("-----------------------------------------"));
      LOG.trace(traceMsg("Skipping circular resolve for reference: " + reference.getText()));
      LOG.trace(traceMsg("-----------------------------------------"));
    }
  }

  private List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    Stack<String> stack = referencesProcessing.get();
    boolean traceEnabled = LOG.isTraceEnabled();

    String referenceText = reference.getText();
    stack.push(referenceText);
    try {
      if (traceEnabled) {
        LOG.trace(traceMsg("-----------------------------------------"));
        LOG.trace(traceMsg("Resolving reference: " + referenceText));
      }

      List<? extends PsiElement> foundElements = doResolveInner(reference, incompleteCode);

      if (traceEnabled) {
        LOG.trace(traceMsg("Finished reference:  " + referenceText));
        LOG.trace(traceMsg("-----------------------------------------"));
      }

      return foundElements;
    } finally {
      stack.pop();
    }
  }

  private List<? extends PsiElement> doResolveInner(@NotNull HaxeReference reference, boolean incompleteCode) {

    isExtension.set(false);

    if (reportCacheMetrics) {
      resolves.incrementAndGet();
    }

    // TODO: Optimization for literals needs vetting before making it available.
    // Shortcut for literals -- optimization.
    //if (reference instanceof HaxeLiteralExpression || reference instanceof HaxeConstantExpression) {
    //  if (!(reference instanceof HaxeRegularExpression
    //        || reference instanceof HaxeRegularExpressionLiteral
    //        || reference instanceof HaxeStringLiteralExpression)) {
    //    return EMPTY_LIST;
    //  }
    //}

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
        resolveBySuperClassAndSymbol(PsiTreeUtil.getParentOfType(reference, HaxeClass.class), reference);
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
          LogResolution(reference, "via import.");
          return asList(target);
        }
      }

      if (PsiNameHelper.getInstance(reference.getProject()).isQualifiedName(reference.getText())) {
        List<HaxeModel> resolvedPackage =
          HaxeProjectModel.fromElement(reference).resolve(new FullyQualifiedInfo(reference.getText()), reference.getResolveScope());
        if (resolvedPackage != null && !resolvedPackage.isEmpty() && resolvedPackage.get(0) instanceof HaxePackageModel) {
          LogResolution(reference, "via project qualified name.");
          return Collections.singletonList(resolvedPackage.get(0).getBasePsi());
        }
      }
    }
    if (result == null) result = checkIsForwardedName(reference);

    if (result == null) {
      LogResolution(reference, "failed after exhausting all options.");
    }
    return result == null ? EMPTY_LIST : result;
  }

  private List<? extends PsiElement> checkIsForwardedName(HaxeReference reference) {
    List<? extends PsiElement> result = null;

    HaxeMetadataCompileTimeMeta meta = UsefulPsiTreeUtil.getParentOfType(reference, HaxeMetadataCompileTimeMeta.class);
    if (null != meta && HaxeMeta.FORWARD.matches(meta.getType())) {
      PsiElement associatedElement = HaxeMetadataUtils.getAssociatedElement(meta);
      if (null != associatedElement) {
        if (associatedElement instanceof HaxeAbstractClassDeclaration) {
          HaxeAbstractClassModel model = new HaxeAbstractClassModel((HaxeAbstractClassDeclaration)associatedElement);
          HaxeGenericResolver resolver = model.getGenericResolver(null);
          HaxeClass underlyingClass = model.getUnderlyingClass(resolver);
          result = resolveByClassAndSymbol(underlyingClass, resolver, reference);
        }
      }
    }

    if (null != result) {
      LogResolution(reference, "via forwarded field name check.");
    }
    return result;
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
        LogResolution(reference, "via accessor.");
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
          ((superClass == null) ? null : superClass.findHaxeMethodByName(HaxeTokenTypes.ONEW.toString(), null)); // Self only.
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

  private static void LogResolution(HaxeReference ref, String tailmsg) {
    // Debug is always enabled if trace is enabled.
    if (LOG.isDebugEnabled()) {
      String message = "Resolved " + (ref == null ? "empty result" : ref.getText()) + " " + elide(tailmsg, MAX_DEBUG_MESSAGE_LENGTH);
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
    final HaxeClassResolveResult leftExpression = lefthandExpression.resolveHaxeClass();
    if (leftExpression.getHaxeClass() != null) {
      HaxeMemberModel member = leftExpression.getHaxeClass().getModel().getMember(identifier, leftExpression.getGenericResolver());
      if (member != null) {
        return Collections.singletonList(member.getBasePsi());
      }
    }

    // Check 'using' classes.
    HaxeClass leftClass = leftExpression.getHaxeClass();
    if (leftClass != null) {
      HaxeFileModel fileModel = HaxeFileModel.fromElement(reference.getContainingFile());
      List<HaxeUsingModel> usingModels = fileModel != null ? fileModel.getUsingModels() : Collections.emptyList();
      HaxeMethodModel foundMethod = null;
      for (int i = usingModels.size() - 1; i >= 0; --i) {
        foundMethod = usingModels.get(i).findExtensionMethod(identifier, leftClass);
        if (null != foundMethod) {
          if (LOG.isTraceEnabled()) LOG.trace("Found method in 'using' import: " + foundMethod.getName());
          return Collections.singletonList(foundMethod.getBasePsi());
        }
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
    return resolveByClassAndSymbol(leftExpression, reference);
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
   * If the leftReference is to a file, and helperName is a class, we return the name
   * of that class.
   *
   * @param leftReference - a potential class/file name.
   * @param helperName    - the field/method/class to find.
   * @return the name of the found field/method/class.  null if not found.
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
      HaxeClassResolveResult resolveResult = leftReference.resolveHaxeClass();
      leftResultClass = resolveResult.getHaxeClass();
      if (LOG.isTraceEnabled()) {
        String resultClassName = leftResultClass != null ? leftResultClass.getText() : null;
        LOG.trace(traceMsg("Found abstract left result:" + (resultClassName != null ? resultClassName : "<no text>")));
      }
      if (leftResultClass != null) {
        HaxeClassModel model = leftResultClass.getModel();

        if(model.isTypedef()) {
          // Resolve to the underlying type.
          HaxeClassResolveResult result = fullyResolveTypedef(leftResultClass, resolveResult.getSpecialization());
          if (null != result.getHaxeClass()) {
            model = result.getHaxeClass().getModel();
          }
        }

        HaxeMemberModel member = model.getMember(helperName, resolveResult.getGenericResolver());
        if (member != null) return member.getNamePsi();

        if (model.isAbstract() && ((HaxeAbstractClassModel)model).hasForwards()) {
          HaxeGenericResolver resolver = resolveResult.getSpecialization().toGenericResolver(leftResultClass);
          final List<HaxeNamedComponent> forwardingHaxeNamedComponents =
            HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(leftResultClass, resolver);
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

  @NotNull
  private static HaxeClassResolveResult fullyResolveTypedef(@Nullable HaxeClass typedef, @Nullable HaxeGenericSpecialization specialization) {
    if (null == typedef) return HaxeClassResolveResult.EMPTY;

    HashSet<String> recursionGuard = new HashSet<>(); // Track which typedefs we've already resolved so we don't end up in an infinite loop.

    HaxeClassResolveResult result = HaxeClassResolveResult.EMPTY;
    HaxeClassModel model = typedef.getModel();
    while (null != model && model.isTypedef() && !recursionGuard.contains(model.getName())) {
      recursionGuard.add(model.getName());

      final HaxeTypeOrAnonymous toa = model.getUnderlyingType();
      final HaxeType type = toa.getType();
      if (null == type) {
        // Anonymous structure
        result = HaxeClassResolveResult.create(toa.getAnonymousType(), specialization);
        break;
      }

      // If the reference is to a type parameter, resolve that instead.
      HaxeClassResolveResult nakedResult = specialization.get(type, type.getReferenceExpression().getIdentifier().getText());
      if (null == nakedResult) {
        nakedResult = type.getReferenceExpression().resolveHaxeClass();
      }
      result = HaxeClassResolveResult.create(nakedResult.getHaxeClass(), specialization);
      model = null != result.getHaxeClass() ? result.getHaxeClass().getModel() : null;
    }
    return result;
  }

  private static List<? extends PsiElement> asList(@Nullable PsiElement element) {
    if (LOG.isDebugEnabled()) LOG.debug("Resolved as " + (element == null ? "empty result list."
                                                                          : elide(element.toString(), MAX_DEBUG_MESSAGE_LENGTH)));
    return element == null ? Collections.emptyList() : Collections.singletonList(element);
  }

  private static HaxeGenericResolver getGenericResolver(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    HaxeGenericSpecialization specialization = reference.getSpecialization();
    return null != specialization ? specialization.toGenericResolver(leftClass) : null;
  }

  private static List<? extends PsiElement> resolveBySuperClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                         @NotNull HaxeReference reference) {
    HaxeGenericResolver baseResolver = getGenericResolver(leftClass, reference);
    return resolveBySuperClassAndSymbol(leftClass, baseResolver, reference);
  }


  private static List<? extends PsiElement> resolveBySuperClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                         @Nullable HaxeGenericResolver resolver,
                                                                         @NotNull HaxeReference reference ) {
    if (null == leftClass) {
      return EMPTY_LIST;
    }

    if (leftClass instanceof HaxeAbstractClassDeclaration) {

      HaxeClassModel classModel = leftClass.getModel();
      HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel)classModel;
      return resolveByClassAndSymbol(abstractClassModel.getUnderlyingClass(resolver), resolver, reference);

    } else {

      Set<HaxeType> superclasses = new ArrayListSet<>();
      superclasses.addAll(leftClass.getHaxeExtendsList());
      superclasses.addAll(leftClass.getHaxeImplementsList());

      List<? extends PsiElement> result = EMPTY_LIST;
      for (HaxeType sup : superclasses) {
        HaxeReference superReference = sup.getReferenceExpression();
        HaxeClassResolveResult superClassResult = superReference.resolveHaxeClass();
        SpecificHaxeClassReference superClass = superClassResult.getSpecificClassReference(leftClass, resolver);
        result = resolveByClassAndSymbol(superClass.getHaxeClass(), superClass.getGenericResolver(), reference);
        if (null != result && !result.isEmpty()) {
          break;
        }
      }
      return result;
    }
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClassResolveResult resolveResult,
                                                                    @NotNull HaxeReference reference) {
    if (resolveResult == null) {
      if (LOG.isDebugEnabled()) LogResolution(null, "(resolveByClassAndSymbol)");
    }
    return resolveResult == null ? Collections.<PsiElement>emptyList() : resolveByClassAndSymbol(resolveResult.getHaxeClass(),
                                                                                                 resolveResult.getGenericResolver(),
                                                                                                 reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass, @NotNull HaxeReference reference) {
    HaxeGenericResolver resolver = getGenericResolver(leftClass, reference);
    return resolveByClassAndSymbol(leftClass, resolver, reference);
  }

  private static List<? extends PsiElement> resolveByClassAndSymbol(@Nullable HaxeClass leftClass,
                                                                    @Nullable HaxeGenericResolver resolver,
                                                                    @NotNull HaxeReference reference) {
    if (leftClass != null) {
      final HaxeClassModel classModel = leftClass.getModel();
      HaxeMemberModel member = classModel.getMember(reference.getReferenceName(), resolver);
      if (member != null) return asList(member.getNamePsi());

      // if class is abstract try find in forwards
      if (leftClass.isAbstract()) {
        HaxeAbstractClassModel model = (HaxeAbstractClassModel)leftClass.getModel();
        if (model.isForwarded(reference.getReferenceName())) {
          final HaxeClass underlyingClass = model.getUnderlyingClass(resolver);
          if (underlyingClass != null) {
            member = underlyingClass.getModel().getMember(reference.getReferenceName(), resolver);
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
    return HaxeDebugUtil.traceThreadMessage(msg, 120);
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