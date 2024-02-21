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
import com.intellij.openapi.progress.ProgressIndicatorProvider;
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
import lombok.CustomLog;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.text.StringUtil.defaultIfEmpty;
import static com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator.searchReferencesForType;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.ARRAY;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.CLASS;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;

@CustomLog
abstract public class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReference {

  public static final String DOT = ".";
  private static final Key<HaxeGenericSpecialization> SPECIALIZATION_KEY = new Key<>("HAXE_SPECIALIZATION_KEY");
  private static boolean skipUnimplementedWarnings = true;

  //static {
  //  log.setLevel(LogLevel.TRACE);
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
    }
    else if (this instanceof HaxeNewExpression newExpression) {
      if (newExpression.getType() != null) {
        PsiElement resolved = newExpression.getType().getReferenceExpression().resolve();
        HaxeClass haxeClass;
        if (resolved instanceof HaxeClass resolvedAsClass) {
          haxeClass = resolvedAsClass;
        }
        else {
          SpecificHaxeClassReference typeReference = SpecificTypeReference.getUnknown(newExpression);
          haxeClass = null != typeReference ? typeReference.getHaxeClass() : null;
        }
        final HaxeResolveResult result = HaxeResolveResult.create(haxeClass);
        result.specializeByParameters(newExpression.getType().getTypeParam());
        return result.getSpecialization();
      }
    }

    // The specialization for a reference comes from either the type of the left-hand side of the
    // expression, or failing that, from the class in which the reference appears, which is
    // exactly what tryGetLeftResolveResult() gives us.
    final HaxeResolveResult result = tryGetLeftResolveResult(expression);
    return result != HaxeResolveResult.EMPTY ? result.getSpecialization() : null;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  private List<? extends PsiElement> resolveNamesToParents(List<? extends PsiElement> nameList) {
    if (log.isTraceEnabled()) log.trace(traceMsg("namelist: " + (nameList== null ? null : nameList.toString())));
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
    if (this instanceof HaxeCallExpression) {
      PsiReference referenceChain = this.getFirstChild().getReference();
      if (referenceChain instanceof HaxeReferenceExpression referenceExpression) {
        PsiElement method = referenceExpression.resolve();
        if (method instanceof HaxeMethod haxeMethod) {
          if (!haxeMethod.isStatic()) return false; // only static methods can be extensions (compiler: Cannot access static field XXX from a class instance)

          PsiElement ChainBeforeMethod = referenceExpression.getChildren()[0];
          if (ChainBeforeMethod instanceof  HaxeIdentifier) return false; // not chain, got method identifer
          if (ChainBeforeMethod instanceof HaxeReferenceExpression referenceExpression1) {
            PsiElement caller = referenceExpression1.resolve();
            if (caller == method) return false; // probably a function bind or similar
            // todo find using import statement for methods declaring class and confirm "using"
            return !(caller instanceof HaxeClass || caller instanceof HaxeImportAlias);
          }else {
            return true;
          }
        }
      }
    }
    return false;
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
   * more than one) found.
   */
  @Nullable
  public PsiElement resolveToComponentName() {
    final ResolveResult[] resolveResults = multiResolve(true, false);
    final PsiElement result = resolveResults.length != 1 ||
                              !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();

    if (result instanceof HaxeNamedComponent namedComponent) {
      return namedComponent.getComponentName();
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
   * than one) is found.
   */
  @Nullable
  public PsiElement resolve(boolean incompleteCode) {
    if (log.isTraceEnabled()) log.trace(traceMsg(null));
    final ResolveResult[] resolveResults = multiResolve(incompleteCode);

    PsiElement resolved = resolveResults.length != 1 || !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
    if (log.isTraceEnabled()) log.trace(traceMsg("Resolved to " + (resolved != null ? resolved.toString() : "<null>")));
    return resolved;
  }

  @NotNull
  @Override
  public HaxeResolveResult resolveHaxeClass() {
    if (log.isTraceEnabled()) log.trace(traceMsg("Begin resolving Haxe class:" + this.getText()));
    HaxeResolveResult result = resolveHaxeClassInternal();
    if (log.isTraceEnabled()) log.trace(traceMsg("Finished resolving Haxe class " + this.getText() + " as " + result.debugDump()));
    return result;
  }

  /**
   * Replacement for instanceof that has better logging and is easier to step over :)
   */
  @Contract("null->false")
  private boolean isType(Class clazz) {
    if (clazz.isInstance(this)) {
      if (log.isTraceEnabled()) {
        traceAs(log, HaxeDebugUtil.getCallerStackFrame(), "Resolving " + this.getDebugName() + " as class type " + clazz.getName());
      }
      return true;
    }
    if (log.isTraceEnabled()) traceAs(log, HaxeDebugUtil.getCallerStackFrame(), this.getDebugName() + " is not a " + clazz.getName());
    return false;
  }

  /**
   * Replacement for instanceof that has better logging and is easier to step over :)
   */
  @Contract("null,_->false")
  private boolean isType(Object o, Class clazz) {
    if (clazz.isInstance(o)) {
      if (log.isTraceEnabled()) traceAs(log, HaxeDebugUtil.getCallerStackFrame(), "Resolving " + o + " as class type " + clazz.getName());
      return true;
    }
    if (log.isTraceEnabled()) traceAs(log, HaxeDebugUtil.getCallerStackFrame(), o + " is not a " + clazz.getName());
    return false;
  }

  @NotNull
  private HaxeResolveResult resolveHaxeClassInternal() {
    HaxeResolveResult result = _resolveHaxeClassInternal();

    //extract type from expression if this is a macro ExprOf before we return result
    if (isMacroIdentifier()) {
      HaxeResolveResult type = extractTypeFromMacro(result);
      if (type != null) result = type;
    }

    return result;
  }

  @Nullable
  private static HaxeResolveResult extractTypeFromMacro(HaxeResolveResult result) {
    HaxeClass aClass = result.getHaxeClass();
    if(aClass != null) {
      if (aClass.getQualifiedName().equals("haxe.macro.ExprOf")
          // TODO : TEMP hack since typeDef is resolved and `ExprOf` is typedef of `Expr`
          || aClass.getQualifiedName().equals("haxe.macro.Expr")) {
        HaxeGenericResolver resolver = result.getGenericResolver();
        if (resolver.isEmpty()) {
          ResultHolder resolve = resolver.resolve("T");
          if (resolve != null && !resolve.isUnknown()) {
            SpecificHaxeClassReference type = resolve.getClassType();
            if (type != null) return type.asResolveResult();
          }
        }
      }
    }
    return null;
  }

  private boolean isMacroIdentifier() {
    PsiElement[] children = this.getChildren();
    if(children.length == 1) {
      if (children[0] instanceof HaxeIdentifier identifier) {
        if (identifier.getMacroId() != null) return true;
      }
    }
    return false;
  }

  @NotNull
  private HaxeResolveResult _resolveHaxeClassInternal() {
    ProgressIndicatorProvider.checkCanceled();

    PsiElement resolve = null;

    if (isType(HaxeThisExpression.class)) {
      HaxeClass clazz = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
      // this has different semantics on abstracts
      if (clazz != null && clazz.getModel().isAbstractType()) {
        HaxeTypeOrAnonymous type = clazz.getModel().getUnderlyingType();
        if (type != null) {
          return HaxeResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(type));
        }
      }
      return HaxeResolveResult.create(clazz);
    }

    if (isType(HaxeSuperExpression.class)) {
      final HaxeClass haxeClass = PsiTreeUtil.getParentOfType(this, HaxeClass.class);
       if (haxeClass == null) return HaxeResolveResult.createEmpty();
      List<HaxeType> extendsList = haxeClass.getHaxeExtendsList();
      if (extendsList.isEmpty()) {
        return HaxeResolveResult.createEmpty();
      }
      HaxeType first = extendsList.get(0);
      final HaxeReference superExpression = first.getReferenceExpression();
      final HaxeResolveResult superClassResolveResult = superExpression.resolveHaxeClass();
      superClassResolveResult.specializeByParameters(first.getTypeParam());
      return superClassResolveResult;
    }

    if (isType(HaxeStringLiteralExpression.class)) {
      return HaxeResolveResult.create(HaxeResolveUtil.findClassByQName("String", this));
    }

    if (isType(HaxeLiteralExpression.class)) {
      final PsiElement firstChild = getFirstChild();
      if (firstChild instanceof LeafPsiElement child) {
        final IElementType childTokenType = child.getElementType();
        return HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(childTokenType), this));
      }
      // Else, it's a block statement and not a named literal.
      return HaxeResolveResult.createEmpty();
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
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(this);
      HaxeExpressionEvaluator.evaluate(haxeMapLiteral, context, HaxeGenericResolverUtil.generateResolverFromScopeParents(this));

      SpecificHaxeClassReference mapClass = context.result.getClassType();
      return HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(mapClass.getClassName(), this));
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
            HaxeReference haxeReference = null;
            if (expression instanceof HaxeNewExpression || expression instanceof HaxeCallExpression) {
              haxeReference = PsiTreeUtil.findChildOfType(expression, HaxeReferenceExpression.class);
            }
            if (expression instanceof HaxeReference reference) {
              haxeReference = reference;
            }

            HaxeClass haxeClassResolveResultHaxeClass = null;
            if (haxeReference != null) {
              HaxeResolveResult haxeResolveResult = haxeReference.resolveHaxeClass();
              haxeClassResolveResultHaxeClass = haxeResolveResult.getHaxeClass();
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
      HaxeResolveResult resolveResult =
        HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(getLiteralClassName(getTokenType()), this));

      HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();

      HaxeGenericSpecialization specialization = resolveResult.getSpecialization();
      if (resolveResultHaxeClass != null &&
          specialization.get(resolveResultHaxeClass, "T") == null) {  // TODO: 'T' should not be hard-coded.
        if (isString) {
          specialization.put(resolveResultHaxeClass, "T", HaxeResolveResult.create(HaxeResolveUtil.findClassByQName("String", this)));
        }
        else if (sameClass) {
          specialization.put(resolveResultHaxeClass, "T",
                             HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(haxeClass.getQualifiedName(), this)));
        }
        else if (implementOrExtendSameClass) {
          HaxeReferenceExpression haxeReferenceExpression = commonTypeList.get(commonTypeList.size() - 1).getReferenceExpression();
          HaxeResolveResult resolveHaxeClass = haxeReferenceExpression.resolveHaxeClass();

          if (resolveHaxeClass != HaxeResolveResult.EMPTY) {
            HaxeClass resolveHaxeClassHaxeClass = resolveHaxeClass.getHaxeClass();

            if (resolveHaxeClassHaxeClass != null) {
              specialization.put(resolveResultHaxeClass, "T", HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(
                resolveHaxeClassHaxeClass.getQualifiedName(), this)));
            }
          }
        }
      }

      return resolveResult;
    } // end (this instanceof HaxeArrayLiteral)

    if (isType(HaxeNewExpression.class)) {
      final HaxeResolveResult result = HaxeResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(
        ((HaxeNewExpression)this).getType()));
      result.specialize(this);
      return result;
    }

    if (isType(HaxeCallExpression.class)) {
      final HaxeExpression expression = ((HaxeCallExpression)this).getExpression();
      final HaxeResolveResult leftResult = tryGetLeftResolveResult(expression);
      if (expression instanceof HaxeReference) {
        // creating a resolver that combines parent(leftExpression) current(expression) and children (parameterExpressions)
        HaxeGenericResolver resolver = new HaxeGenericResolver();

        if (leftResult.getHaxeClass() != null) {
          resolver.addAll(HaxeGenericResolverUtil
                            .getResolverSkipAbstractNullScope(leftResult.getHaxeClass().getModel(), leftResult.getGenericResolver()));
        }
        else {
          resolver.addAll(leftResult.getGenericResolver());
        }
        PsiElement resolvedExpression = ((HaxeReference)expression).resolve();

        if (resolvedExpression instanceof HaxeMethod method) {
          HaxeGenericResolver modelResolver = method.getModel().getGenericResolver(leftResult.getGenericResolver());

          HaxeExpressionList argumentList = ((HaxeCallExpression)this).getExpressionList();
          if (argumentList != null) {
            List<HaxeExpression> ArgumentExpressions = argumentList.getExpressionList();
            for (HaxeExpression exp : ArgumentExpressions) {
              if (exp instanceof HaxeReference reference) {
                HaxeResolveResult ArgumentResult = reference.resolveHaxeClass();
                modelResolver.addAll(ArgumentResult.getGenericResolver());
              }
            }
            resolver.addAll(modelResolver);
          }
        }
        //TODO should not be necessary  with both (resolve from parent scope and  "manual" from models)
        // gets chain generics
        HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(this);
        // adds generics from class and method where this  reference is in.
        genericResolver.addAll(resolver);
        ResultHolder holder = HaxeExpressionEvaluator.evaluate(this, genericResolver).result;
        if (!holder.isUnknown()){
          return holder.getType().asResolveResult();
        }
        // should not be necessary
        final HaxeResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(resolvedExpression, resolver.getSpecialization(null));
        result.specialize(this);
        return result;
      }
    }

    if (isType(HaxeArrayAccessExpression.class)) {
      // wrong generation. see HaxeCallExpression
      final HaxeReference reference = PsiTreeUtil.getChildOfType(this, HaxeReference.class);
      if (reference != null) {
        final HaxeResolveResult resolveResult = reference.resolveHaxeClass();
        final HaxeClass resolveResultHaxeClass = resolveResult.getHaxeClass();
        if (resolveResultHaxeClass == null) {
          return resolveResult;
        }
        // std Array
        if ("Array".equals(resolveResultHaxeClass.getQualifiedName())) {
          HaxeResolveResult arrayResolveResult = resolveResult.getSpecialization().get(resolveResultHaxeClass, "T");

          if (arrayResolveResult != null) {
            return arrayResolveResult;
          }
        }
        // @:arrayAccess methods, such as in Map or openfl.Vector
        HaxeNamedComponent arrayAccessGetter = resolveResultHaxeClass.findArrayAccessGetter(resolveResult.getGenericResolver());
        HaxeGenericResolver memberResolver = resolveResultHaxeClass.getMemberResolver(resolveResult.getGenericResolver());
        HaxeGenericSpecialization memberSpecialization = HaxeGenericSpecialization.fromGenericResolver(arrayAccessGetter, memberResolver);

        // hack to work around external ArrayAccess interface, interface that has no methods but tells compiler that implementing class has array access
        if (arrayAccessGetter instanceof  HaxeExternInterfaceDeclaration  declaration && declaration.getComponentName().textMatches("ArrayAccess")){
          HaxeResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(arrayAccessGetter, memberSpecialization);
          HaxeResolveResult getter = result.getSpecialization().get(arrayAccessGetter, "T");
          if (getter != null) return getter;
        }

        return HaxeResolveUtil.getHaxeClassResolveResult(arrayAccessGetter, memberSpecialization);
      }
    }
    if (isType(HaxeReferenceExpression.class)) {
      // check if its a type reference (ex. its just class name no chaining or access, ust the name of the class)
      // if this is the case threat as "class reference" Class<theClass>
      //HaxeResolveResult resolveResult = resolveHaxeClass();
      resolve = resolve();
      if (resolve != null) {
        // find real type for import alias
        if (resolve instanceof HaxeImportAlias alias) {
          return alias.resolveHaxeClass();
        }

        // Note: this is a bit of a hack for switch extractor arguments that are not named components but a reference to a reference
        if (resolve instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement second = referenceExpression.resolve();
          if (second instanceof HaxeReference reference) {
            // small attempt at guarding against recursive loop
            if (reference != this) {
              return reference.resolveHaxeClass();
            }
          }
        }
        if (resolve instanceof HaxeAnonymousTypeField anonymousTypeField) {
          HaxeTypeTag tag = anonymousTypeField.getTypeTag();
          ResultHolder resolvedType = HaxeTypeResolver.getTypeFromTypeTag(tag, anonymousTypeField);
          HaxeResolveResult result = null;
          HaxeGenericSpecialization specialization = getSpecialization();
          if (resolvedType.getClassType() != null && !resolvedType.isUnknown()) {
            if (!resolvedType.getClassType().isTypeParameter()) {
              result = resolvedType.getClassType().asResolveResult();
              if (specialization != null) {
                result.specializeByParent(specialization.toGenericResolver(null));
              }
            }
            else {
              if (specialization != null) {
                ResultHolder resolveTypeParameter = specialization.toGenericResolver(resolve).resolve(resolvedType);
                if (resolveTypeParameter != null
                    && !resolveTypeParameter.isUnknown()
                    && resolveTypeParameter.getClassType() != null
                ) {
                  result = resolveTypeParameter.getClassType().asResolveResult();
                }
              }
            }
            if (result != null) return result;
          }
          else if (resolvedType.isFunctionType()) {
            return HaxeResolveResult.create(resolvedType.getFunctionType().functionType);
          }
        }
        if (resolve instanceof HaxeMethodDeclaration methodDeclaration) {
          //TODO mlo: try to make a Resolve result with method
          //SpecificFunctionReference functionReference = SpecificFunctionReference.create(methodDeclaration.getModel());
          //return HaxeResolveResult.create(functionReference, getSpecialization());
        }
        if (resolve instanceof HaxeClassDeclaration haxeClassDeclaration) {
          String className = haxeClassDeclaration.getComponentName().getName();


          boolean isPure = isPureClassReferenceOf(className);

          if (isPure) {
            // wrap in Class<>
            SpecificHaxeClassReference reference =
              SpecificHaxeClassReference.withoutGenerics(haxeClassDeclaration.getModel().getReference());
            SpecificHaxeClassReference aClass =
              SpecificHaxeClassReference.getStdClass(CLASS, this, new ResultHolder[]{new ResultHolder(reference)});
            return aClass.asResolveResult();
          }
        }
      }
    }

    if (log.isTraceEnabled()) log.trace(traceMsg("Calling resolve()"));
    if (resolve == null) {
      resolve = resolve();
    }
    if (resolve != null) {

      if (isType(resolve, PsiPackage.class)) {
        // Packages don't ever resolve to classes. (And they don't have children!)
        return HaxeResolveResult.EMPTY;
      }
      if (isType(resolve, HaxeAnonymousTypeField.class)) {
        HaxeAnonymousTypeField field = (HaxeAnonymousTypeField)resolve;
        HaxeTypeTag typeTag = field.getTypeTag();
        if (typeTag.getTypeOrAnonymous() != null) {
          HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
          if (typeOrAnonymous != null) {
            if (typeOrAnonymous.getAnonymousType() != null) {
              return HaxeResolveResult.create(typeOrAnonymous.getAnonymousType(), getSpecialization());
            }
            else {
              HaxeType type = typeOrAnonymous.getType();
              if (type != null) {
                PsiElement resolvedType = type.getReferenceExpression().resolve();
                if (resolvedType instanceof HaxeGenericListPart genericListPart) {
                  final HaxeComponentName componentName = genericListPart.getComponentName();
                  final HaxeGenericSpecialization specialization = getSpecialization();
                  if (specialization != null && componentName != null) {
                    String genericName = componentName.getText();
                    final HaxeResolveResult result = specialization.get(resolve, genericName);
                    if (result != null) {
                      return result;
                    }
                  }
                }
                if (resolvedType instanceof HaxeClass) {
                  return HaxeResolveResult.create((HaxeClass)resolvedType, getSpecialization());
                }
              }
            }
          }
        }
        return HaxeResolveResult.create(HaxeResolveUtil.findClassByQName("Dynamic", this));
      }

      if (isType(resolve, HaxeGenericListPart.class)) {
        final HaxeComponentName componentName = ((HaxeGenericListPart)resolve).getComponentName();
        if (componentName != null) {
          HaxeGenericSpecialization innerSpecialization =
            tryGetLeftResolveResult(this).getSpecialization();
          String genericName = componentName.getText();
          final HaxeResolveResult result = innerSpecialization.get(resolve, genericName);
          if (result != null) {
            return result;
          }
        }
      }

      if (isType(resolve, HaxeEnumValueDeclaration.class)) {
        final HaxeEnumDeclaration enumDeclaration = UsefulPsiTreeUtil.getParentOfType(resolve, HaxeEnumDeclaration.class);
        return HaxeResolveResult.create(enumDeclaration, getSpecialization());
      }

      if (isType(resolve, HaxeClass.class) || isType(resolve, HaxeFunctionLiteral.class)) {
        // Classes (particularly typedefs) that are already resolved should not be
        // re-resolved to their component parts.
        return HaxeResolveResult.create((HaxeClass)resolve, getSpecialization());
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
          //failsafe check that we can get function model from SDK
          if (SpecificTypeReference.getFunction(resolve).getHaxeClass() != null) {
            final HaxeClass fn = new HaxeSpecificFunction((HaxeMethod)resolve, specialization);
            return HaxeResolveResult.create(fn, specialization);
          }
        }
      }
      if (isType(resolve, HaxeEnumExtractedValue.class)) {
        HaxeEnumExtractedValue extractedValue = (HaxeEnumExtractedValue)resolve;
        HaxeEnumArgumentExtractor extractor = PsiTreeUtil.getParentOfType(extractedValue, HaxeEnumArgumentExtractor.class);
        if (extractor != null) {
          int index = -1;
          @NotNull PsiElement[] children = extractor.getEnumExtractorArgumentList().getChildren();
          for (int i = 0; i < children.length; i++) {
            if (children[i] == resolve) {
              index = i;
              break;
            }
          }
          SpecificHaxeClassReference enumClass = HaxeResolveUtil.resolveExtractorEnum(extractor);
          HaxeEnumValueDeclaration enumValueDeclaration = HaxeResolveUtil.resolveExtractorEnumValueDeclaration(enumClass, extractor);
          if (enumValueDeclaration  != null) {
            HaxeParameter parameter = enumValueDeclaration.getParameterList().getParameterList().get(index);
            HaxeGenericResolver resolver = enumClass.getGenericResolver();
            ResultHolder type = HaxeTypeResolver.getPsiElementType(parameter, resolver);
            ResultHolder resultHolder = resolver.resolve(type);
            if(resultHolder != null && resultHolder.getClassType()!= null) {
              return resultHolder.getClassType().asResolveResult();
            }
          }
        }
      }

      // RestParameter can have a normal typeTag but the argument is treated as an array, so we have to wrap it in an array
      if (isType(resolve, HaxeRestParameter.class)) {
        HaxeTypeTag tag = ((HaxeParameter)resolve).getTypeTag();
        ResultHolder type = HaxeTypeResolver.getTypeFromTypeTag(tag, resolve);
        return SpecificTypeReference.getStdClass(ARRAY, resolve, new ResultHolder[]{type}).asResolveResult();
      }
      if (isType(resolve, HaxeParameter.class)) {
        // check if  type parameters has multiple constraints and try to unify
        HaxeParameter parameter = (HaxeParameter)resolve;
        HaxeTypeTag tag = parameter.getTypeTag();
        String typeName = tag != null && tag.getTypeOrAnonymous() != null ? tag.getTypeOrAnonymous().getText() : null;
        PsiElement parameterList = resolve.getParent();
        if (parameterList != null) {
          if (parameterList.getParent() instanceof HaxeFunctionLiteral literal) {
          // if parameter type is unknown  (allowed in function literals) we can try to find it from assignment, ex. callExpression
          ResultHolder holder = tryToFindTypeFromCallExpression(literal, resolve);
          if (holder != null && !holder.isUnknown()) return holder.getType().asResolveResult();
        }
          else if (parameterList.getParent() instanceof HaxeMethodDeclaration method) {
          HaxeGenericParam methodGenericParam = method.getGenericParam();
          List<HaxeGenericListPart> methodPartList = methodGenericParam != null ? methodGenericParam.getGenericListPartList() : null;

          List<HaxeGenericListPart> classPartList = null;
          if (method.getContainingClass() instanceof HaxeClassDeclaration declaringClass) {
            // check class for type parameters
            HaxeGenericParam classGenericParam = declaringClass.getGenericParam();
            classPartList = classGenericParam != null ? classGenericParam.getGenericListPartList() : null;
          }

          if (methodPartList != null || classPartList != null) {
            HaxeGenericListPart listPart = null;
            // check method declaration first if it got generic params
            if (methodPartList != null) {
              for (HaxeGenericListPart genericListPart : methodPartList) {
                if (Objects.equals(typeName, genericListPart.getName())) {
                  listPart = genericListPart;
                  break;
                }
              }
            }
            // if not found in method, then check class
            if (listPart == null  && classPartList != null) {
              for (HaxeGenericListPart genericListPart : classPartList) {
                if (Objects.equals(typeName, genericListPart.getName())) {
                  listPart = genericListPart;
                  break;
                }
              }
            }

            if (listPart != null) {
              HaxeTypeList list = listPart.getTypeList();
              HaxeTypeListPart typeListPart = listPart.getTypeListPart();
              ASTNode node = listPart.getContext().getNode();
              if (list != null) {
                List<HaxeType> classReferences = new ArrayList<>();
                for (HaxeTypeListPart part : list.getTypeListPartList()) {
                  HaxeType type = part.getTypeOrAnonymous() == null ? null : part.getTypeOrAnonymous().getType();
                  if (type != null) {
                    classReferences.add(type);
                  }
                }

                HaxeTypeParameterMultiType constraint = HaxeTypeParameterMultiType.withTypeList(node, classReferences);
                return HaxeResolveResult.create(constraint);
              }else if (typeListPart != null) {
                HaxeTypeOrAnonymous typeOrAnonymous = typeListPart.getTypeOrAnonymous();
                if (typeOrAnonymous != null) {
                  if (typeOrAnonymous.getType() != null) {
                    HaxeTypeParameterMultiType constraint =
                      HaxeTypeParameterMultiType.withTypeList(node, List.of(typeOrAnonymous.getType()));

                    return HaxeResolveResult.create(constraint);
                  }
                  else if (typeOrAnonymous.getAnonymousType() != null) {
                    HaxeAnonymousType anonymousType = typeOrAnonymous.getAnonymousType();
                    HaxeTypeParameterMultiType constraint =
                      HaxeTypeParameterMultiType.withAnonymousList(node, anonymousType.getAnonymousTypeBodyList());

                    return HaxeResolveResult.create(constraint);
                  }
                }
              }
            }
          }
        }
          // try to search for usage to determine type
          if (tag == null && parameter.getVarInit() == null) {
            HaxeComponentName componentName = parameter.getComponentName();
            HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(parameterList);
            HaxeMethod method = PsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
            ResultHolder holder = searchReferencesForType(componentName, context, null, method == null ? null : method.getBody());
            if (!holder.isUnknown()) {
              return holder.getType().asResolveResult();
            }
          }
        }
      }

      return HaxeResolveUtil.getHaxeClassResolveResult(resolve, getSpecialization());
    }

    if (log.isTraceEnabled()) log.trace(traceMsg("Trying class resolve by fully qualified name."));

    return HaxeResolveResult.create(HaxeResolveUtil.findClassByQName(getText(), this));
  }

  @Nullable
  public static ResultHolder tryToFindTypeFromCallExpression(HaxeFunctionLiteral literal, @NotNull PsiElement parameter) {
    if (literal.getParent().getParent() instanceof  HaxeCallExpression callExpression) {
      if (callExpression.getExpression() instanceof HaxeReference reference) {
        if (reference.resolve() instanceof  HaxeMethod haxeMethod) {
          // TODO find correct parameter, this is just a quick and dirty attempt might get wrong type
          HaxeCallExpressionList list = callExpression.getExpressionList();
          if (list == null) return null;
          int callExpressionIndex = list.getExpressionList().indexOf(literal);
          List<HaxeParameterModel> parameters = haxeMethod.getModel().getParameters();
          if (parameters.size() < callExpressionIndex) return null;
          for (int i = 0; i<callExpressionIndex; i++) {
            if (parameters.get(i).isOptional()) return null; //TODO,  lazy hack to avoid incorrect results, need parameter map
          }
          HaxeParameterModel model = parameters.get(callExpressionIndex);
          ResultHolder type = model.getType();
          if (type.isFunctionType()) {
            SpecificFunctionReference functionType = type.getFunctionType();
            HaxeParameterList parameterList = literal.getParameterList();
            if (parameterList == null || functionType == null) return null;
            int parameterMappedToArgument = parameterList.getParameterList().indexOf(parameter);
            List<SpecificFunctionReference.Argument> arguments = functionType.getArguments();
            SpecificFunctionReference.Argument argument = arguments.get(parameterMappedToArgument);
            //TODO this is a bit hackish way to get resolver
            HaxeResolveResult result = reference.resolveHaxeClass();
            HaxeGenericResolver resolver = result.getSpecialization().toGenericResolver(haxeMethod);
            ResultHolder resolved = resolver.resolve(argument.getType());
            if (resolved != null && !resolved.isUnknown())return resolved.getType().createHolder();
          }
        }
      }
    }
    return null;
  }

  public boolean isPureClassReferenceOf(@NotNull String className) {
    PsiElement resolve = resolve();
    return getParent() instanceof HaxeExpressionList
           && resolve instanceof HaxeClass haxeClass
           && className.equalsIgnoreCase(haxeClass.getName());// identical classname and elementText
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
    String ref = getReferenceName();
    //The name was not changed. Are we moving a class to another package?
    if (element instanceof HaxeClassDeclaration && ref != null && ref.equals(element.getName())) {
      handleClassMovement(element);
      //rename
    }
    else {
      handleElementRename(element.getName());
    }
  }

  private void handleClassMovement(PsiClass element) {
    String thisFqn = getQualifiedName();
    //This reference is not a fully qualified name. Nothing to do.
    if (!thisFqn.contains(".")) {
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
      log.error("ReferenceExpression generated by HaxeElementGenerator is null!");
    }
    else {
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
        }
        else {
          updateImportStatement(importPath);
        }
      }
      else if (isInQualifiedPath) {
        final String newName = destinationPackage.equals(selfFileModel.getPackageName()) ? elementFileModel.getName() : importPath;
        updateFullyQualifiedReference(newName);
      }
      else if (requiredToAddImport) {
        selfFileModel.addImport(importPath);
      }
    }
    else {
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
      log.error("ReferenceExpression generated by HaxeElementGenerator is null!");
    }
    else {
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
    HaxeResolveResult result = null;
    HaxeClass haxeClass = null;
    String name = null;
    HaxeGenericResolver resolver = null;
    if (leftReference != null) {
      result = leftReference.resolveHaxeClass();
      if (result != HaxeResolveResult.EMPTY) {
        haxeClass = result.getHaxeClass();
        if (haxeClass != null) {
          name = haxeClass.getName();
        }
        resolver = result.getSpecialization().toGenericResolver(haxeClass);
      }
      if (leftReference.resolve() instanceof  HaxeImportAlias alias) {
        name = alias.getIdentifier().getText();
      }
    }


    boolean isThis = leftReference instanceof HaxeThisExpression;
    if (leftReference != null && name != null &&
        HaxeResolveUtil.splitQName(leftReference.getText()).getSecond().equals(name)) {

      if (!isInUsingStatement() && !(isInImportStatement() && (haxeClass.isEnum() || haxeClass instanceof HaxeAbstractTypeDeclaration))) {
        addClassStaticMembersVariants(suggestedVariants, haxeClass, !(isThis));
      }

      addChildClassVariants(suggestedVariants, haxeClass);
    }
    else if (leftReference != null && !result.isFunctionType()) {
      if (null == haxeClass) {
        // TODO: fix haxeClass by type inference. Use compiler code assist?!
      }
      if (haxeClass != null) {
        boolean isSuper = leftReference instanceof HaxeSuperExpression;
        addClassNonStaticMembersVariants(suggestedVariants, haxeClass, resolver, !(isThis || isSuper));
        addUsingVariants(suggestedVariants, suggestedVariantsExtensions, haxeClass, this);
      }
    }
    else {
      if (leftReference == null) {
        final boolean isElementInForwardMeta = HaxeAbstractForwardUtil.isElementInForwardMeta(this);
        if (isElementInForwardMeta) {
          final HaxeMeta meta = HaxeMetadataUtils.getEnclosingMeta(this);
          PsiElement element = HaxeMetadataUtils.getAssociatedElement(meta);
          // TODO mlo : needs a fix: there's a problem with how module element and metadata is parsed som metadata is outside module.
          if (element instanceof  HaxeModule module) {
            element = module.getFirstChild();
          }
          final HaxeClass clazz = element instanceof HaxeClass ? (HaxeClass)element : null;
          addAbstractUnderlyingClassVariants(suggestedVariants, clazz, resolver);
        }
        else {
          PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), this, null, new ResolveState());
          addClassVariants(suggestedVariants, PsiTreeUtil.getParentOfType(this, HaxeClass.class), false, resolver);
        }
      }
    }

    Object[] variants = HaxeLookupElement.convert(result, suggestedVariants, suggestedVariantsExtensions, resolver).toArray();
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
  private static HaxeResolveResult tryGetLeftResolveResult(HaxeExpression expression) {
    final HaxeReference leftReference = PsiTreeUtil.getChildOfType(expression, HaxeReference.class);
    return leftReference != null
           ? leftReference.resolveHaxeClass()
           : HaxeResolveResult.create(PsiTreeUtil.getParentOfType(expression, HaxeClass.class));
  }

  @Nullable
  public static String getLiteralClassName(IElementType type) {
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

  @NotNull
  private static JavaResolveResult[] toCandidateInfoArray(List<? extends PsiElement> elements) {
    if (elements == null) return new JavaResolveResult[0];
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
    if (haxeClass == null || !haxeClass.isAbstractType()) return;

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
    boolean isAbstractEnum = haxeClass.isAbstractType() && haxeClass.isEnum();
    boolean isAbstractForward = haxeClass.isAbstractType() && ((HaxeAbstractClassModel)classModel).hasForwards();

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
    if (!skipUnimplementedWarnings) {
      log.warn("getParameterList is unimplemented" + this.getClass().getName());
    }
    // REFERENCE_PARAMETER_LIST  in Java
    HaxeTypeParam child = (HaxeTypeParam)findChildByType(HaxeTokenTypes.TYPE_PARAM);
    //return child == null ? null : child.getTypeList();
    return null;
  }

  @NotNull
  @Override
  public PsiType[] getTypeParameters() {
    // TODO:  Unimplemented.
    if (!skipUnimplementedWarnings) {
      log.warn("getTypeParameters is unimplemented " + this.getClass().getName());
    }
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
    if (!skipUnimplementedWarnings) {
      log.warn("processVariants is unimplemented" + this.getClass().getName());
    }
  }

  @Nullable
  @Override
  public PsiElement getQualifier() {
    PsiElement expression = getFirstChild();
    if (expression instanceof HaxeIdentifier identifier) expression = identifier.getParent();
    if (expression instanceof HaxeReference reference) return reference;
    return null;
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
