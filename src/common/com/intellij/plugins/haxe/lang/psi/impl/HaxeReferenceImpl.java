/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Aleksandr Kuzmenko
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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.HaxeLookupElement;
import com.intellij.plugins.haxe.ide.refactoring.move.HaxeFileMoveHandler;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.JavaSourceUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.text.StringUtil.defaultIfEmpty;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.*;

abstract public class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReference {

  public static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  public static final String DOT = ".";
  private static final Key<HaxeGenericSpecialization> SPECIALIZATION_KEY = new Key<>("HAXE_SPECIALIZATION_KEY");

  //static {
  //  LOG.setLevel(Level.TRACE);
  //}  // Pull this out after debugging.

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
    PsiElement nameElement = getReferenceNameElement();
    if (nameElement != null) {
      int startOffset = nameElement.getStartOffsetInParent();
      return new TextRange(startOffset, startOffset + nameElement.getTextLength());
    }

    PsiElement dot = getLastChild();
    if (DOT.equals(dot.getText())) {
      int index = dot.getStartOffsetInParent() + dot.getTextLength();
      return new TextRange(index, index);
    }

    return new TextRange(0, getTextLength());
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public int getTextOffset() {
    PsiElement nameElement = getReferenceNameElement();
    return nameElement != null ? nameElement.getTextOffset() : super.getTextOffset();
  }

  @Nullable
  public HaxeGenericSpecialization getSpecialization() {
    // CallExpressions need to resolve their child, rather than themselves.
    HaxeExpression expression = this;
    if (this instanceof HaxeCallExpression) {
      expression = ((HaxeCallExpression)this).getExpression();
    } else if (this instanceof HaxeNewExpression) {
      HaxeNewExpression newExpression = (HaxeNewExpression)this;
      PsiElement resolved = newExpression.getType().getReferenceExpression().resolve();
      HaxeClass haxeClass;
      if (resolved instanceof HaxeClass) {
        haxeClass = (HaxeClass) resolved;
      } else {
        SpecificHaxeClassReference typeReference = SpecificTypeReference.getUnknown(newExpression);
        haxeClass = null != typeReference ? typeReference.getHaxeClass() : null;
      }
      final HaxeClassResolveResult result = HaxeClassResolveResult.create(haxeClass);
      result.specializeByParameters(newExpression.getType().getTypeParam());
      return result.getSpecialization();
    }

    // The specialization for a reference comes from either the type of the left-hand side of the
    // expression, or failing that, from the class in which the reference appears, which is
    // exactly what tryGetLeftResolveResult() gives us.
    final HaxeClassResolveResult result = tryGetLeftResolveResult(expression);
    return result != HaxeClassResolveResult.EMPTY ? result.getSpecialization() : null;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  private List<? extends PsiElement> resolveNamesToParents(List<? extends PsiElement> nameList) {
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("namelist: " + nameList.toString()));
    if (nameList == null || nameList.isEmpty()) {
      return Collections.emptyList();
    }

    List<PsiElement> result = new ArrayList<>();
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
    Boolean result;
    if(this instanceof  HaxeCallExpression) {
      PsiReference child = this.getFirstChild().getReference();
      if(!(child instanceof HaxeReferenceExpression)) return false;
      HaxeReferenceExpression reference = (HaxeReferenceExpression)child;
      result = reference.getUserData(HaxeResolver.isExtensionKey);
      if(result == null) {
        HaxeResolver.INSTANCE.resolve(reference, true);
        result = reference.getUserData(HaxeResolver.isExtensionKey);
      }
    }else {
      HaxeResolver.INSTANCE.resolve(this, true);
      result = this.getUserData(HaxeResolver.isExtensionKey);
    }
    return result == null ? false :result;
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

    List<? extends PsiElement> cachedNames = doResolve(this, incompleteCode);

    // CandidateInfo does some extra resolution work when checking validity, so
    // the results have to be turned into a CandidateInfoArray, and not just passed
    // around as the list that HaxeResolver returns.
    return toCandidateInfoArray(resolveToParents ? resolveNamesToParents(cachedNames) : cachedNames);
  }

  protected List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    return (HaxeResolver.INSTANCE).resolve(reference, incompleteCode);
  }

  /**
   * Resolve a reference, returning the COMPONENT_NAME field of the found
   * PsiElement.
   *
   * @return the component name of the found element, or null if not (or
   *   more than one) found.
   */
  @Nullable
  public PsiElement resolveToComponentName() {
    final ResolveResult[] resolveResults = multiResolve(true, false);
    final PsiElement result = resolveResults.length == 0 ||
                              resolveResults.length > 1 ||
                              !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();

    if (result != null && result instanceof HaxeNamedComponent) {
      return ((HaxeNamedComponent)result).getComponentName();
    }

    return result;
  }

  /**
   * Resolve a reference, returning a list of possible candidates.
   *
   * @param incompleteCode Whether to treat the code as a fragment or not.
   *                       Usually, code is considered incomplete.
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
   * @param incompleteCode Whether to treat the code as a fragment or not.
   *                       Usually, code is considered incomplete.
   * @return the element this reference refers to, or null if none (or more
   *   than one) is found.
   */
  @Nullable
  public PsiElement resolve(boolean incompleteCode) {
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg(null));
    final ResolveResult[] resolveResults = multiResolve(incompleteCode);

    PsiElement resolved = resolveResults.length == 0 ||
                          resolveResults.length > 1 ||
                          !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Resolved to " + (resolved != null ? resolved.toString() : "<null>")));
    return resolved;
  }

  @NotNull
  @Override
  public HaxeClassResolveResult resolveHaxeClass() {
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Begin resolving Haxe class:" + this.getText()));
    HaxeClassResolveResult result = resolveHaxeClassInternal();
    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Finished resolving Haxe class " + this.getText() + " as " + result.debugDump()));
    return result;
  }

  /** Replacement for instanceof that has better logging and is easier to step over :) */
  @Contract("null->false")
  private boolean isType(Class clazz) {
    if (clazz.isInstance(this)) {
      if (LOG.isTraceEnabled()) LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), "Resolving " + this.getDebugName() + " as class type " + clazz.getName());
      return true;
    }
    if (LOG.isTraceEnabled()) LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), this.getDebugName() + " is not a " + clazz.getName());
    return false;
  }

  /** Replacement for instanceof that has better logging and is easier to step over :) */
  @Contract("null,_->false")
  private boolean isType(Object o, Class clazz) {
    if (clazz.isInstance(o)) {
      if (LOG.isTraceEnabled()) LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), "Resolving " + o + " as class type " + clazz.getName());
      return true;
    }
    if (LOG.isTraceEnabled()) LOG.traceAs(HaxeDebugUtil.getCallerStackFrame(), o + " is not a " + clazz.getName());
    return false;
  }

  @NotNull
  private HaxeClassResolveResult resolveHaxeClassInternal() {

    if (isType(HaxeThisExpression.class)) {
      HaxeClass clazz = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      // this has different semantics on abstracts
      if (clazz != null && clazz.getModel().isAbstract()) {
        HaxeTypeOrAnonymous type = clazz.getModel().getUnderlyingType();
        if (type != null) {
          return HaxeClassResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(type));
        }
      }
      return HaxeClassResolveResult.create(clazz);
    }

    if (isType(HaxeSuperExpression.class)) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      assert haxeClass != null;
      if (haxeClass.getHaxeExtendsList().isEmpty()) {
        return HaxeClassResolveResult.create(null);
      }
      final HaxeExpression superExpression = haxeClass.getHaxeExtendsList().get(0).getReferenceExpression();
      final HaxeClassResolveResult superClassResolveResult = ((HaxeReference)superExpression).resolveHaxeClass();
      superClassResolveResult.specializeByParameters(haxeClass.getHaxeExtendsList().get(0).getTypeParam());
      return superClassResolveResult;
    }

    if (isType(HaxeStringLiteralExpression.class)) {
      return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("String", this));
    }

    if (isType(HaxeLiteralExpression.class)) {
      final PsiElement firstChild = getFirstChild();
      if (firstChild instanceof LeafPsiElement) {
        final LeafPsiElement child = (LeafPsiElement)getFirstChild();
        final IElementType childTokenType = child == null ? null : child.getElementType();
        return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(childTokenType), this));
      }
      // Else, it's a block statement and not a named literal.
      return HaxeClassResolveResult.create(null);
    }

    if (isType(HaxeMapLiteral.class)) {
      // Maps are created as specific types, using these rules (from Map.hx constructor documentation):
      //   This becomes a constructor call to one of the specialization types in
      //   the output. The rules for that are as follows:
      //
      //     1. if K is a `String`, `haxe.ds.StringMap` is used
      //     2. if K is an `Int`, `haxe.ds.IntMap` is used
      //     3. if K is an `EnumValue`, `haxe.ds.EnumValueMap` is used
      //     4. if K is any other class or structure, `haxe.ds.ObjectMap` is used
      //     5. if K is any other type, it causes a compile-time error
      //
      // Also, maps can be created via comprehensions, so if the expression is anything other than a FAT_ARROW,
      // we will have to get the type of the statement.
      HaxeMapLiteral haxeMapLiteral = (HaxeMapLiteral)this;
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(this, null);
      HaxeExpressionEvaluator.evaluate(haxeMapLiteral, context, HaxeGenericResolverUtil.generateResolverFromScopeParents(this));

      SpecificHaxeClassReference mapClass = context.result.getClassType();
      return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(mapClass.getClassName(), this));
    }

    if (isType(HaxeArrayLiteral.class)) {
      HaxeArrayLiteral haxeArrayLiteral = (HaxeArrayLiteral)this;
      HaxeExpressionList expressionList = haxeArrayLiteral.getExpressionList();
      boolean isString = false;
      boolean sameClass = false;
      boolean implementOrExtendSameClass = false;
      HaxeClass haxeClass = null;
      List<HaxeType> commonTypeList = new ArrayList<>();
      List<HaxeExpression> haxeExpressionList = expressionList != null ? expressionList.getExpressionList() : new ArrayList<>();
      if (!haxeExpressionList.isEmpty()) {
        isString = true;
        sameClass = true;

        for (HaxeExpression expression : haxeExpressionList) {
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
              List<HaxeType> haxeTypeList = new ArrayList<>();
              haxeTypeList.addAll(haxeClass.getHaxeImplementsList());
              haxeTypeList.addAll(haxeClass.getHaxeExtendsList());

              commonTypeList.retainAll(haxeTypeList);
              implementOrExtendSameClass = !commonTypeList.isEmpty();
            }

            if (haxeClass == null || !haxeClass.equals(haxeClassResolveResultHaxeClass)) {
              sameClass = false;
            }
          }
        }
      }
      HaxeClassResolveResult resolveResult =
        HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(getTokenType()), this));

      HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();

      HaxeGenericSpecialization specialization = resolveResult.getSpecialization();
      if (resolveResultHaxeClass != null &&
          specialization.get(resolveResultHaxeClass, "T") == null) {  // TODO: 'T' should not be hard-coded.
        if (isString) {
          specialization.put(resolveResultHaxeClass, "T", HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("String", this)));
        } else if (sameClass) {
          specialization.put(resolveResultHaxeClass, "T",
                             HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(haxeClass.getQualifiedName(), this)));
        } else if (implementOrExtendSameClass) {
          HaxeReferenceExpression haxeReferenceExpression = commonTypeList.get(commonTypeList.size() - 1).getReferenceExpression();
          HaxeClassResolveResult resolveHaxeClass = haxeReferenceExpression.resolveHaxeClass();

          if (resolveHaxeClass != HaxeClassResolveResult.EMPTY) {
            HaxeClass resolveHaxeClassHaxeClass = resolveHaxeClass.getHaxeClass();

            if (resolveHaxeClassHaxeClass != null) {
              specialization.put(resolveResultHaxeClass, "T", HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(
                resolveHaxeClassHaxeClass.getQualifiedName(), this)));
            }
          }
        }
      }

      return resolveResult;
    } // end (this instanceof HaxeArrayLiteral)

    if (isType(HaxeNewExpression.class)) {
      final HaxeClassResolveResult result = HaxeClassResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(
        ((HaxeNewExpression)this).getType()));
      result.specialize(this);
      return result;
    }

    if (isType(HaxeCallExpression.class)) {
      final HaxeExpression expression = ((HaxeCallExpression)this).getExpression();
      final HaxeClassResolveResult leftResult = tryGetLeftResolveResult(expression);
      if (expression instanceof HaxeReference) {
        // creating a resolver that combines parent(leftExpression) current(expression) and children (parameterExpressions)
        HaxeGenericResolver resolver = new HaxeGenericResolver();

        if(leftResult.getHaxeClass() != null) {
          resolver.addAll(HaxeGenericResolverUtil
                            .getResolverSkipAbstractNullScope(leftResult.getHaxeClass().getModel(), leftResult.getGenericResolver()));
        }else {
          resolver.addAll(leftResult.getGenericResolver());
        }
        PsiElement resolvedExpression = ((HaxeReference)expression).resolve();

        if (resolvedExpression instanceof HaxeMethod) {
          HaxeMethod method = (HaxeMethod)resolvedExpression;
          HaxeGenericResolver modelResolver = method.getModel().getGenericResolver(leftResult.getGenericResolver());

          HaxeExpressionList list = ((HaxeCallExpression)this).getExpressionList();
          if(list != null) {
            List<HaxeExpression> parameterExpressions = list.getExpressionList();
            for(HaxeExpression exp:  parameterExpressions) {
              HaxeClassResolveResult parameterResult = ((HaxeReference)exp).resolveHaxeClass();
              modelResolver.addAll(parameterResult.getGenericResolver());
            }
            resolver.addAll(modelResolver);
          }
        }
        final HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(resolvedExpression, resolver.getSpecialization(resolvedExpression));
        result.specialize(this);
        return result;
      }
    }

    if (isType(HaxeArrayAccessExpression.class)) {
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
        // @:arrayAccess methods, such as in Map or openfl.Vector
        HaxeNamedComponent arrayAccessGetter = resolveResultHaxeClass.findArrayAccessGetter(resolveResult.getGenericResolver());
        HaxeGenericResolver memberResolver = resolveResultHaxeClass.getMemberResolver(resolveResult.getGenericResolver());
        HaxeGenericSpecialization memberSpecialization = HaxeGenericSpecialization.fromGenericResolver(arrayAccessGetter, memberResolver);
        return HaxeResolveUtil.getHaxeClassResolveResult(arrayAccessGetter, memberSpecialization);
      }
    }

    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Calling resolve()"));
    PsiElement resolve = resolve();
    if (isType(resolve, PsiPackage.class)) {
      // Packages don't ever resolve to classes. (And they don't have children!)
      return HaxeClassResolveResult.EMPTY;
    }
    if (isType(resolve, HaxeAnonymousTypeField.class)) {
      HaxeAnonymousTypeField field = (HaxeAnonymousTypeField)resolve;
      HaxeTypeTag typeTag = field.getTypeTag();
      if (null != typeTag && typeTag.getTypeOrAnonymous() != null) {
        HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
        if (typeOrAnonymous != null) {
          if (typeOrAnonymous.getAnonymousType() != null) {
            return HaxeClassResolveResult.create(typeOrAnonymous.getAnonymousType(), getSpecialization());
          } else {
            HaxeType type = typeOrAnonymous.getType();
            if (type != null) {
              PsiElement resolvedType = type.getReferenceExpression().resolve();
              if(resolvedType instanceof HaxeGenericListPart) {
                final HaxeComponentName componentName = ((HaxeGenericListPart)resolvedType).getComponentName();
                final HaxeGenericSpecialization specialization = getSpecialization();
                if(specialization != null && componentName != null) {
                  String genericName = componentName.getText();
                  final HaxeClassResolveResult result = specialization.get(resolve, genericName);
                  if (result != null) {
                    return result;
                  }
                }
              }
              if (resolvedType instanceof HaxeClass) {
                return HaxeClassResolveResult.create((HaxeClass)resolvedType, getSpecialization());
              }
            }
          }
        }
      }
      return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName("Dynamic", this));
    }

    if (isType(resolve, HaxeGenericListPart.class)) {
      final HaxeComponentName componentName = ((HaxeGenericListPart)resolve).getComponentName();
      if (componentName != null) {
        HaxeGenericSpecialization innerSpecialization =
          tryGetLeftResolveResult(this).getSpecialization();
        String genericName = componentName.getText();
        final HaxeClassResolveResult result = innerSpecialization.get(resolve, genericName);
        if (result != null) {
          return result;
        }
      }
    }

    if (isType(resolve, HaxeEnumValueDeclaration.class)) {
      final HaxeEnumDeclaration enumDeclaration = UsefulPsiTreeUtil.getParentOfType(resolve, HaxeEnumDeclaration.class);
      return HaxeClassResolveResult.create(enumDeclaration, getSpecialization());
    }

    if (isType(resolve, HaxeClass.class) || isType(resolve, HaxeFunctionLiteral.class)) {
      // Classes (particularly typedefs) that are already resolved should not be
      // re-resolved to their component parts.
      return HaxeClassResolveResult.create((HaxeClass)resolve, getSpecialization());
    }

    if (isType(resolve, HaxeMethod.class)) {
      // If the resolved element is a method, but the reference's parent is not a call expression, then
      // we want to return the method type, and not the method's return type.  (e.g. String->String->Void, rather than Void.)
      List<PsiElement> typeList = UsefulPsiTreeUtil.getPathToParentOfType(this, HaxeCallExpression.class);
      if (null == typeList || typeList.size() != 1) {

        HaxeGenericSpecialization specialization = getSpecialization();
        if (null == specialization) {
          specialization = new HaxeGenericSpecialization();
        }

        final HaxeClass fn = new HaxeSpecificFunction((HaxeMethod)resolve, specialization);
        return HaxeClassResolveResult.create(fn, specialization);
      }
    }
    if(isType(resolve, HaxeParameter.class)) {
      // check if  type parameters has multiple constraints and try to unify
      HaxeTypeTag tag = ((HaxeParameter)resolve).getTypeTag();
      String typeName = tag != null  && tag.getTypeOrAnonymous() != null ? tag.getTypeOrAnonymous().getText() : null;
      PsiElement parameterList = resolve.getParent();

      if(parameterList != null && parameterList.getParent() instanceof HaxeMethodDeclaration ) {
        HaxeMethodDeclaration method = (HaxeMethodDeclaration)parameterList.getParent();
        HaxeGenericParam genericParam = method.getGenericParam();
        List<HaxeGenericListPart> partList =genericParam != null ? genericParam.getGenericListPartList() : null;
        if(partList!= null) {
          Optional<HaxeGenericListPart> match = partList.stream().filter(part -> Objects.equals(typeName, part.getName())).findFirst();
          if(match.isPresent()) {
            HaxeGenericListPart listPart = match.get();
            HaxeTypeList list = listPart.getTypeList();
            if(list != null) {
              list.getTypeListPartList();
              List<HaxeType> classReferences = list.getTypeListPartList().stream()
                .map(part -> part.getTypeOrAnonymous() == null ? null : part.getTypeOrAnonymous().getType())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

              HaxeTypeParameterMultiType constraint = new HaxeTypeParameterMultiType(listPart.getContext().getNode(), classReferences);
              return HaxeClassResolveResult.create(constraint);
            }
          }
        }
      }
    }
    if (resolve != null) {
      return HaxeResolveUtil.getHaxeClassResolveResult(resolve, getSpecialization());
    }

    if (LOG.isTraceEnabled()) LOG.trace(traceMsg("Trying class resolve by fully qualified name."));

    return HaxeClassResolveResult.create(HaxeResolveUtil.findClassByQName(getText(), this));
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    PsiElement element = this;
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
    } else if (element instanceof PsiPackage) {
      bindToPackage((PsiPackage)element);
    } else if (element instanceof PsiClass) {
      bindToClass((PsiClass)element);
    }
    return this;
  }

  private void bindToClass(PsiClass element) {
    String ref = getReferenceName();
    //The name was not changed. Are we moving a class to another package?
    if(element instanceof HaxeClassDeclaration && ref != null && ref.equals(element.getName())) {
      handleClassMovement(element);
    //rename
    } else {
      handleElementRename(element.getName());
    }
  }

  private void handleClassMovement(PsiClass element) {
    String thisFqn = getQualifiedName();
    //This reference is not a fully qualified name. Nothing to do.
    if(!thisFqn.contains(".")) {
      return;
    }
    String newFqn = ((HaxeClassDeclaration)element).getModel().haxeClass.getQualifiedName();
    updateFullyQualifiedReference(newFqn);
  }

  private void bindToPackage(PsiPackage element) {
    final HaxeImportStatement importStatement =
      HaxeElementGenerator.createImportStatementFromPath(getProject(), element.getQualifiedName());
    HaxeReferenceExpression referenceExpression = importStatement != null ? importStatement.getReferenceExpression() : null;
    if (referenceExpression == null) {
      LOG.error("ReferenceExpression generated by HaxeElementGenerator is null!");
    } else {
      replace(referenceExpression);
    }
  }

  private void bindToFile(PsiElement element) {
    final HaxeFileModel elementFileModel = HaxeFileModel.fromElement(element);
    final HaxeFileModel selfFileModel = HaxeFileModel.fromElement(this);
    String destinationPackage = element.getUserData(HaxeFileMoveHandler.destinationPackageKey);
    if (destinationPackage == null) {
      destinationPackage = "";
    }
    final String importPath = (destinationPackage.isEmpty() ? "" : destinationPackage + ".") +
                              elementFileModel.getName();

    if (resolve() == null) {
      final List<HaxeImportModel> imports = selfFileModel.getImportModels();
      final boolean isInImportStatement = getParent() instanceof HaxeImportStatement;
      final boolean isInQualifiedPath = getText().indexOf('.') != -1;
      final boolean inSamePackage = Objects.equals(selfFileModel.getPackageName(), elementFileModel.getPackageName());
      final boolean exposedByImports = imports.stream().anyMatch(
        model -> {
          return model.getExposedMembers().stream().anyMatch(item -> elementFileModel.getQualifiedInfo().equals(item.getQualifiedInfo()));
        });
      final boolean requiredToAddImport = !inSamePackage && (imports.isEmpty() || !exposedByImports);

      if (isInImportStatement) {
        if (inSamePackage || destinationPackage.isEmpty() || exposedByImports) {
          deleteImportStatement();
        } else {
          updateImportStatement(importPath);
        }
      } else if (isInQualifiedPath) {
        final String newName = destinationPackage.equals(selfFileModel.getPackageName()) ? elementFileModel.getName() : importPath;
        updateFullyQualifiedReference(newName);
      } else if (requiredToAddImport) {
        selfFileModel.addImport(importPath);
      }
    } else {
      final HaxeImportStatement importStatement = selfFileModel.getImportStatements().stream()
        .filter(statement -> statement.getReferenceExpression() != null && importPath.equals(statement.getReferenceExpression().getText()))
        .findFirst()
        .orElse(null);

      if (importStatement != null) {
        importStatement.delete();
      }
    }
  }

  private void updateFullyQualifiedReference(String newName) {
    HaxeReferenceExpression referenceExpression =
      HaxeElementGenerator.createReferenceExpressionFromText(getProject(), newName);
    if (referenceExpression == null) {
      LOG.error("ReferenceExpression generated by HaxeElementGenerator is null!");
    } else {
      replace(referenceExpression);
    }
  }

  private void updateImportStatement(String newImportPath) {
    HaxeImportStatement importStatement = (HaxeImportStatement)getParent();
    final HaxeImportStatement newImportStatement = HaxeElementGenerator.createImportStatementFromPath(getProject(), newImportPath);
    assert newImportStatement != null : "New import statement can't be null - HaxeElementGenerator failed";
    importStatement.replace(newImportStatement);
  }

  private void deleteImportStatement() {
    if (getParent() instanceof HaxeImportStatement) {
      try {
        getParent().delete();
      }
      catch (IncorrectOperationException ignored) {
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
    final Set<HaxeComponentName> suggestedVariants = new HashSet<>();
    final Set<HaxeComponentName> suggestedVariantsExtensions = new HashSet<>();

    // if not first in chain
    // foo.bar.baz
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(this);
    HaxeClassResolveResult result = null;
    HaxeClass haxeClass = null;
    String name = null;
    HaxeGenericResolver resolver = null;
    if (leftReference != null) {
      result = leftReference.resolveHaxeClass();
      if (result != HaxeClassResolveResult.EMPTY) {
        haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          name = haxeClass.getName();
        }
        resolver = result.getSpecialization().toGenericResolver(haxeClass);
      }
    }

    boolean isThis = leftReference instanceof HaxeThisExpression;
    if (leftReference != null && name != null &&
        HaxeResolveUtil.splitQName(leftReference.getText()).getSecond().equals(name)) {

      if (!isInUsingStatement() && !(isInImportStatement() && (haxeClass.isEnum() || haxeClass instanceof HaxeAbstractClassDeclaration))) {
        addClassStaticMembersVariants(suggestedVariants, haxeClass, !(isThis));
      }

      addChildClassVariants(suggestedVariants, haxeClass);
    } else if (leftReference != null && !result.isFunctionType()) {
      if (null == haxeClass) {
        // TODO: fix haxeClass by type inference. Use compiler code assist?!
      }
      if (haxeClass != null) {
        boolean isSuper = leftReference instanceof HaxeSuperExpression;
        addClassNonStaticMembersVariants(suggestedVariants, haxeClass, resolver, !(isThis || isSuper));
        addUsingVariants(suggestedVariants, suggestedVariantsExtensions, haxeClass, this);
      }
    } else {
      if (leftReference == null) {
        final boolean isElementInForwardMeta = HaxeAbstractForwardUtil.isElementInForwardMeta(this);
        if (isElementInForwardMeta) {
          final HaxeMeta meta = HaxeMetadataUtils.getEnclosingMeta(this);
          final PsiElement element = HaxeMetadataUtils.getAssociatedElement(meta);
          final HaxeClass clazz = element instanceof HaxeClass ? (HaxeClass)element : null;
          addAbstractUnderlyingClassVariants(suggestedVariants, clazz, resolver);
        } else {
          PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
          addClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), false, resolver);
        }
      }
    }

    Object[] variants = HaxeLookupElement.convert(result, suggestedVariants, suggestedVariantsExtensions).toArray();
    PsiElement leftTarget = leftReference != null ? leftReference.resolve() : null;

    if (leftTarget instanceof PsiPackage) {
      return ArrayUtil.mergeArrays(variants, ((PsiPackage)leftTarget).getSubPackages());
    } else if (leftTarget instanceof HaxeFile) {
      return ArrayUtil.mergeArrays(variants, ((HaxeFile)leftTarget).getClasses());
    } else if (leftReference == null) {
      PsiPackage rootPackage = JavaPsiFacade.getInstance(getElement().getProject()).findPackage("");
      return rootPackage == null ? variants : ArrayUtil.mergeArrays(variants, rootPackage.getSubPackages());
    }
    return variants;
  }

  private boolean isInUsingStatement() {
    return UsefulPsiTreeUtil.getParentOfType(this, HaxeUsingStatement.class) != null;
  }

  private boolean isInImportStatement() {
    return UsefulPsiTreeUtil.getParentOfType(this, HaxeImportStatement.class) != null;
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

  @NotNull
  private static HaxeClassResolveResult tryGetLeftResolveResult(HaxeExpression expression) {
    final HaxeReference leftReference = PsiTreeUtil.getChildOfType(expression, HaxeReference.class);
    return leftReference != null
           ? leftReference.resolveHaxeClass()
           : HaxeClassResolveResult.create(PsiTreeUtil.getParentOfType(expression, HaxeClass.class));
  }

  @Nullable
  private static String getLiteralClassName(IElementType type) {
    if (type == HaxeTokenTypes.STRING_LITERAL_EXPRESSION) {
      return "String";
    } else if (type == HaxeTokenTypes.ARRAY_LITERAL) {
      return "Array";
    } else if (type == HaxeTokenTypes.LITFLOAT) {
      return "Float";
    } else if (type == HaxeTokenTypes.REG_EXP) {
      return "EReg";
    } else if (type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITOCT) {
      return "Int";
    }
    return null;
  }

  @NotNull
  private static JavaResolveResult[] toCandidateInfoArray(List<? extends PsiElement> elements) {
    final JavaResolveResult[] result = new JavaResolveResult[elements.size()];
    for (int i = 0, size = elements.size(); i < size; i++) {
      result[i] = new CandidateInfo(elements.get(i), EmptySubstitutor.getInstance());
    }
    return result;
  }

  private static void addUsingVariants(Set<HaxeComponentName> variants,
                                       Set<HaxeComponentName> variantsWithExtension,
                                       final @Nullable HaxeClass ourClass,
                                       HaxeReferenceImpl reference) {

    if (ourClass == null) return;

    HaxeFileModel.fromElement(reference).getUsingModels().stream()
      .flatMap(model -> model.getExtensionMethods(ourClass).stream())
      .map(HaxeMemberModel::getNamePsi)
      .forEach(name -> {
        variants.add(name);
        variantsWithExtension.add(name);
      });
  }

  private static void addClassVariants(Set<HaxeComponentName> suggestedVariants, @Nullable HaxeClass haxeClass, boolean filterByAccess,
                                       @Nullable HaxeGenericResolver resolver) {
    if (haxeClass == null) {
      return;
    }
    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(resolver, haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if (!needFilter && namedComponent.getComponentName() != null) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  private static void addAbstractUnderlyingClassVariants(Set<HaxeComponentName> suggestedVariants,
                                                         @Nullable HaxeClass haxeClass, @Nullable HaxeGenericResolver resolver) {
    if (haxeClass == null || !haxeClass.isAbstract()) return;

    final HaxeAbstractClassModel model = (HaxeAbstractClassModel)haxeClass.getModel();
    final HaxeClass underlyingClass = model.getUnderlyingClass(resolver);
    if (underlyingClass != null) {
      addClassVariants(suggestedVariants, underlyingClass, true, resolver);
    }
  }

  private static void addClassStaticMembersVariants(@NotNull final Set<HaxeComponentName> suggestedVariants,
                                                    @NotNull final HaxeClass haxeClass,
                                                    boolean filterByAccess) {

    final boolean isEnum = haxeClass.isEnum();

    List<HaxeComponentName> staticMembers = haxeClass.getModel().getMembersSelf().stream()
      .filter(member -> (isEnum && member instanceof HaxeEnumValueModel) || member.isStatic())
      .filter(member -> !filterByAccess || member.isPublic())
      .map(HaxeMemberModel::getNamePsi)
      .collect(Collectors.toList());

    suggestedVariants.addAll(staticMembers);
  }

  private void addClassNonStaticMembersVariants(Set<HaxeComponentName> suggestedVariants,
                                                       @Nullable HaxeClass haxeClass,
                                                       @Nullable HaxeGenericResolver resolver,
                                                       boolean filterByAccess) {
    if (haxeClass == null) {
      return;
    }

    HaxeClassModel classModel = haxeClass.getModel();

    boolean extern = haxeClass.isExtern();
    boolean isAbstractEnum = haxeClass.isAbstract() && haxeClass.isEnum();
    boolean isAbstractForward = haxeClass.isAbstract() && ((HaxeAbstractClassModel)classModel).hasForwards();

    if (isAbstractForward) {
      final List<HaxeNamedComponent> forwardingHaxeNamedComponents =
        HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(haxeClass, resolver);
      if (forwardingHaxeNamedComponents != null) {
        for (HaxeNamedComponent namedComponent : forwardingHaxeNamedComponents) {
          final boolean needFilter = filterByAccess && !namedComponent.isPublic();
          if ((extern || !needFilter) &&
              !namedComponent.isStatic() &&
              namedComponent.getComponentName() != null &&
              !isConstructor(namedComponent)) {
            suggestedVariants.add(namedComponent.getComponentName());
          }
        }
      }
    }

    for (HaxeNamedComponent namedComponent : HaxeResolveUtil.findNamedSubComponents(resolver, haxeClass)) {
      final boolean needFilter = filterByAccess && !namedComponent.isPublic();
      if (isAbstractEnum && HaxeAbstractEnumUtil.couldBeAbstractEnumField(namedComponent)) {
        continue;
      }
      if ((extern || !needFilter) &&
          !namedComponent.isStatic() &&
          namedComponent.getComponentName() != null &&
          !isConstructor(namedComponent)) {
        suggestedVariants.add(namedComponent.getComponentName());
      }
    }
  }

  private static boolean isConstructor(HaxeNamedComponent component) {
    return component instanceof HaxeMethodPsiMixin && ((HaxeMethodPsiMixin)component).isConstructor();
  }

  /* Determine if the element to the right of the given element in the AST
   * (at the same level) is a dot '.' separator.
   * Workhorse for getQualifier().
   * XXX: If we use this more than once, move it to a utility class, such as UsefulPsiTreeUtil.
   */
  private static boolean nextSiblingIsADot(PsiElement element) {
    if (null == element) return false;

    PsiElement next = element.getNextSibling();
    ASTNode node = ((null != next) ? next.getNode() : null);
    IElementType type = ((null != node) ? node.getElementType() : null);
    boolean ret = (null != type && type.equals(HaxeTokenTypes.ODOT));
    return ret;
  }

  @Nullable
  @Override
  public PsiElement getReferenceNameElement() {
    PsiElement name = findChildByType(HaxeTokenTypes.IDENTIFIER);
    if (name == null) name = getLastChild();
    return name;
  }

  @Nullable
  @Override
  public PsiReferenceParameterList getParameterList() {
    // TODO:  Unimplemented.
    LOG.warn("getParameterList is unimplemented");

    // REFERENCE_PARAMETER_LIST  in Java
    HaxeTypeParam child = (HaxeTypeParam)findChildByType(HaxeTokenTypes.TYPE_PARAM);
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

  @Override
  public void processVariants(@NotNull PsiScopeProcessor processor) {
    // TODO:  Unimplemented.
    LOG.warn("processVariants is unimplemented");
  }

  @Nullable
  @Override
  public PsiElement getQualifier() {
    PsiElement expression = getFirstChild();
    return expression != null && expression instanceof HaxeReference ? expression : null;
  }

  @Nullable
  @Override
  public String getReferenceName() {
    PsiElement nameElement = getReferenceNameElement();
    return nameElement == null ? getText() : nameElement.getText();
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

  public String toDebugString() {
    String ss = super.toString();
    // Unit tests don't want the extra data.  (Maybe we should fix the goldens?)
    String clazzName = this.getClass().getSimpleName();
    String text = getCanonicalText();
    ss += ":" + defaultIfEmpty(text, "<no text>");
    ss += ":" + defaultIfEmpty(clazzName, "<anonymous>");
    return ss;
  }

  @Override
  public String toString() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      return toDebugString();
    }

    // Unit tests don't want the extra data.  (Maybe we should fix the goldens?)
    String ss = super.toString();
    return ss;
  }

  public String traceMsg(String message) {
    StringBuilder msg = new StringBuilder();
    String nodeText = this.getText();
    msg.append(null != nodeText ? nodeText : "<no text>");
    msg.append(':');
    if (null != message) {
      msg.append(message);
    }
    return HaxeDebugUtil.traceMessage(msg.toString(), 2048);
  }

  // HaxeLiteralExpression
  @Nullable
  @Override
  public HaxeBlockStatement getBlockStatement() {
    return HaxeStatementUtils.getBlockStatement(this);
  }
}
