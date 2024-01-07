/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2019 Eric Bishton
 * Copyright 2018 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.impl.HaxeClassWrapperForTypeParameter;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.ThreadLocalCounter;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeResolveResult implements Cloneable {


  //static {      // Take this out when finished debugging.
  //  log.setLevel(LogLevel.DEBUG);
  //}

  private static ThreadLocalCounter debugNestCountForCreate = new ThreadLocalCounter("debugNestCountForCreate");
  private static ThreadLocal<HashSet<HaxeClass>> resolvesInProcess = new ThreadLocal<>().withInitial(()->new HashSet<HaxeClass>());

  public static final HaxeResolveResult EMPTY = new HaxeResolveResult(null);
  @Nullable
  private final HaxeClass haxeClass;
  @Nullable
  private final HaxeFunctionType functionType;
  private final HaxeGenericSpecialization specialization;

  private HaxeResolveResult(@Nullable HaxeClass aClass) {
    this(aClass, new HaxeGenericSpecialization());
  }

  private HaxeResolveResult(@Nullable HaxeFunctionType functionType, @NotNull HaxeGenericSpecialization specialization) {
    this.haxeClass = null;
    this.functionType = functionType;
    this.specialization = specialization;
  }
  private HaxeResolveResult(@Nullable HaxeClass aClass, @NotNull HaxeGenericSpecialization specialization) {
    this.haxeClass = aClass;
    this.functionType = null;
    this.specialization = specialization;
  }

  @Override
  protected HaxeResolveResult clone() {
    if (isFunctionType()) {
      return new HaxeResolveResult(functionType, specialization.clone());
    }else {
      return new HaxeResolveResult(haxeClass, specialization.clone());
    }
  }

  @NotNull
  public static HaxeResolveResult create(@Nullable HaxeFunctionType functionType) {
    return create(functionType, new HaxeGenericSpecialization());
  }

  @NotNull
  public static HaxeResolveResult create(@Nullable HaxeClass aClass) {
    return create(aClass, new HaxeGenericSpecialization());
  }
  @NotNull
  public static HaxeResolveResult createEmpty() {
    return create((HaxeClass)null, new HaxeGenericSpecialization());
  }

  @NotNull
  private static HaxeResolveResult createEmpty(HaxeGenericSpecialization specialization) {
    return create((HaxeClass)null, specialization);
  }

  @NotNull
  public static HaxeResolveResult create(@Nullable HaxeFunctionType functionType, HaxeGenericSpecialization specialization) {
    if (functionType  == null) {
      return new HaxeResolveResult(null);
    }
    if (specialization == null) {
      specialization = new HaxeGenericSpecialization(); // Better than chasing @NotNull all over the code base.
    }
    //TODO implement specialization?
    return new HaxeResolveResult(functionType, specialization);
  }

  /**
   * Creates a new resolve result for the given class.  The specialization (if any) is used to
   * resolve type parameters on the class.
   *
   * @param aClass - Class to wrap up and specialize.
   * @param specialization - A set of type names and real types to map them to.
   * @return A HaxeClassResolveResult for the class, with parameters fully typed, if possible.
   */
  @NotNull
  public static HaxeResolveResult create(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    if (aClass == null) {
      return new HaxeResolveResult(null);
    }
    if (specialization == null) {
      specialization = new HaxeGenericSpecialization(); // Better than chasing @NotNull all over the code base.
    }
    try {
      if (resolvesInProcess.get().contains(aClass)) {
        return HaxeResolveResult.EMPTY;
      }
      resolvesInProcess.get().add(aClass);

      debugNestCountForCreate.increment();
      if (log.isDebugEnabled()) {
        log.debug(debugNestCountForCreate +
                  "Resolving class " +
                  aClass.getName() +
                  " using specialization " +
                  specialization.debugDump("  "));
      }
      HaxeResolveResult resolveResult = getResult(aClass, specialization);

        try {
          List<HaxeType> superclasses = new ArrayList<HaxeType>(aClass.getHaxeExtendsList());
          superclasses.addAll(aClass.getHaxeImplementsList());

          final HaxeGenericSpecialization innerSpecialization = specialization.getInnerSpecialization(aClass);
          for (HaxeType haxeType : superclasses) {
            // For each of our superclasses, resolve the specialization *WITHOUT* resolving all of their superclasses.
            // The purpose here is to create a specialization with the mapping of names to real types before going down
            // the superclass chain.  (e.g. turn 'extends<T>' into 'extends<String>')

            // NOTE: mlo: resolving class by reference seems  be slightly faster here when working with larger projects
            // could be due to cache for Psi resolve while it looks like  resolve by QName does not and searches file tree.
            HaxeClass superclass = null;
            PsiElement resolve = haxeType.getReferenceExpression().resolve();
            if (resolve instanceof HaxeClass haxeClass) {
              superclass = haxeClass.getModel().haxeClass;
            }

            if (superclass == null) {
              // if reference resolve fails, do QName resolve as fallback
              superclass = HaxeResolveUtil.tryResolveClassByQName(haxeType);
            }
            // hopefully it won't be necessary to traverse the entire type hierarchy and we only need to check classes and interfaces that are generic
            // any method we try to override or implement in out main class should  end up resolving super class and any specifics it might have.
            if (superclass != null && !superclass.isGeneric()) continue;

            final HaxeResolveResult superResult = new HaxeResolveResult(superclass, innerSpecialization);
            superResult.specializeByParameters(generateParameterList(haxeType.getTypeParam(), innerSpecialization));

            // Now keep only the specializations that weren't inner.
            HaxeGenericSpecialization filteredSpecialization = superResult.specialization.filterInnerKeys();

            // Now that we have a specialization with real types, we can let the superclass be resolved.
            if (!PsiManager.getInstance(aClass.getProject()).areElementsEquivalent(superclass, aClass)) {
              final HaxeResolveResult result = create(superclass, filteredSpecialization);
              result.specializeByParameters(generateParameterList(haxeType.getTypeParam(), innerSpecialization));
              if (log.isDebugEnabled()) {
                log.debug(debugNestCountForCreate +
                          "  Adding superclass specialization for " +
                          aClass.getName() +
                          "<" +
                          haxeType.getName() +
                          "> -> " +
                          result.getSpecialization().debugDump("    "));
              }
              resolveResult.merge(result.getSpecialization());
            }
          }
        }
        catch (StackOverflowError e) {
          log.error("Stack Overflow trying to resolve " + aClass.getName());
          throw e;
        }

      resolveResult.softMerge(specialization);
      return resolveResult;
    } finally {
      debugNestCountForCreate.decrement();
      resolvesInProcess.get().remove(aClass);
    }
  }

  private static HaxeResolveResult getResult(@NotNull HaxeClass aClass, HaxeGenericSpecialization specialization) {
    HaxeResolveResult resolveResult = HaxeClassResolveCache.getInstance(aClass.getProject()).get(aClass);

    if (resolveResult == null) {
      resolveResult = new HaxeResolveResult(aClass);
      loadResultWithConstraints(resolveResult, aClass, specialization);
      HaxeClassResolveCache.getInstance(aClass.getProject()).put(aClass, resolveResult);
    }
    return resolveResult.clone();
  }

  private static void loadResultWithConstraints(HaxeResolveResult resolveResult,
                                                @Nullable HaxeClass aClass,
                                                HaxeGenericSpecialization specialization) {
    // This block of code loads the specialization with the type _constraint_, not the target types.
    // The constraints are used by the completion engine to create suggestions for parameterized types.
    // Most everything else uses the specialization to figure out the real types for generics.
    // FIXME: Constraints should be a separate value in the specialization tuple.
    final HaxeGenericParam genericParam = aClass.getGenericParam();
    List<HaxeGenericListPart> genericListPartList = genericParam != null ?
                                                    genericParam.getGenericListPartList() :
                                                    Collections.<HaxeGenericListPart>emptyList();
    List<HaxeGenericListPart> lazyGenericReferences = new SmartList<>();
    for (HaxeGenericListPart genericListPart : genericListPartList) {
      final HaxeComponentName componentName = genericListPart.getComponentName();
      final HaxeType constrainedType = getTypeOfGenericListPart(genericListPart);
      if (constrainedType != null) {
        PsiElement referenceElement = constrainedType.getReferenceExpression().resolve();
        if(referenceElement instanceof HaxeGenericListPart && PsiTreeUtil.isAncestor(genericParam, referenceElement, true)) {
          lazyGenericReferences.add(genericListPart);
          continue;
        }
        HaxeResolveResult specializedTypeResult = HaxeResolveUtil.getHaxeClassResolveResult(constrainedType, specialization);
        if (log.isDebugEnabled()) {
          log.debug(debugNestCountForCreate.toString() +
                    "  Adding constraint for " +
                    aClass.getName() +
                    "<" +
                    componentName.getName() +
                    "> -> " +
                    specializedTypeResult.debugDump("    "));
        }
        resolveResult.specialization.put(aClass,
                                         componentName.getName(),
                                         specializedTypeResult);
      }
      else {
        if (log.isDebugEnabled()) {
          log.debug(debugNestCountForCreate +
                    "  Not adding constraint for " +
                    aClass.getName() +
                    "<" +
                    componentName.getName() +
                    ">. No constraint type found.");
        }
      }
    }
    if(!lazyGenericReferences.isEmpty()) {
      for (HaxeGenericListPart genericListPart : lazyGenericReferences) {
        final HaxeComponentName componentName = genericListPart.getComponentName();
        final HaxeType specializedType = getTypeOfGenericListPart(genericListPart);
        if(specializedType != null) {
          String referencedGenericName = specializedType.getReferenceExpression().getText();
          HaxeResolveResult referencedSpecialization = resolveResult.specialization.get(aClass, referencedGenericName);
          if(referencedSpecialization == null) {
            referencedSpecialization = HaxeResolveResult.createEmpty(specialization);
          }
          resolveResult.specialization.put(aClass, componentName.getName(), referencedSpecialization);
        }
      }
    }
  }

  @Nullable
  private static HaxeType getTypeOfGenericListPart(HaxeGenericListPart genericListPart) {
    final HaxeTypeListPart typeListPart = genericListPart.getTypeListPart();
    final HaxeTypeOrAnonymous typeOrAnonymous = ((typeListPart != null) ? typeListPart.getTypeOrAnonymous() : null);
    return ((typeOrAnonymous != null) ? typeOrAnonymous.getType() : null);
  }

  @Nullable
  private static PsiElement getTypeOfTypeListPart(HaxeTypeListPart typeListPart) {
    if (typeListPart.getFunctionType() != null) return typeListPart.getFunctionType();
    final HaxeTypeOrAnonymous typeOrAnonymous = typeListPart.getTypeOrAnonymous();
    if(typeOrAnonymous != null) {
      if(typeOrAnonymous.getType() != null) {
        return typeOrAnonymous.getType();
      } else {
        return typeOrAnonymous.getAnonymousType();
      }
    }

    return null;
  }


  private void merge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()) {
      specialization.map.put(key, otherSpecializations.map.get(key));
    }
  }

  private void softMerge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()) {
      if (!specialization.map.containsKey(key)) {
        specialization.map.put(key, otherSpecializations.map.get(key));
      }
    }
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    return haxeClass;
  }
  @Nullable
  public HaxeFunctionType getFunctionType() {
    return functionType;
  }

  @NotNull
  public HaxeGenericSpecialization getSpecialization() {
    return specialization;
  }

  @NotNull
  public HaxeGenericResolver getGenericResolver() {
    return specialization.toGenericResolver(haxeClass);
  }

  @NotNull
  public SpecificHaxeClassReference getSpecificClassReference(@NotNull PsiElement context, @Nullable HaxeGenericResolver resolver) {
      HaxeClassModel clazz = null != haxeClass ? haxeClass.getModel() : SpecificHaxeClassReference.getUnknown(context).getHaxeClassModel();
      HaxeClassReference classReference =
        null != clazz ? new HaxeClassReference(clazz, context) : new HaxeClassReference(SpecificHaxeClassReference.UNKNOWN, context);
      HaxeClass clazzPsi = null != clazz ? clazz.getPsi() : null;
      softMerge(HaxeGenericSpecialization.fromGenericResolver(clazzPsi, resolver));
      if (classReference.getHaxeClass().isGeneric()) {
        HaxeGenericResolver newResolver = getGenericResolver();
        return SpecificHaxeClassReference.withGenerics(classReference, newResolver.getSpecificsFor(clazzPsi));
      }
      else {
        return SpecificHaxeClassReference.withoutGenerics(classReference);
      }
  }
  @NotNull
  public SpecificTypeReference getSpecificFunctionReference(@NotNull PsiElement context, @Nullable HaxeGenericResolver resolver) {
    if (functionType == null) return SpecificHaxeClassReference.getUnknown(context);

    List<HaxeFunctionArgument> argumentList = functionType.getFunctionArgumentList();
    HaxeFunctionReturnType functionReturnType = functionType.getFunctionReturnType();
    ResultHolder returnType;

    if (functionReturnType != null) {
      returnType = convertReturnType(functionReturnType);
    }else {
      returnType = SpecificHaxeClassReference.getUnknown(context).createHolder();
    }

    int i = 0;
    List<SpecificFunctionReference.Argument> argList = new ArrayList<>();
    for (HaxeFunctionArgument argument : argumentList) {
      argList.add(convertToArgument(argument, i++));
    }
    // TODO use resolver ?

    return new SpecificFunctionReference(argList, returnType, (HaxeMethodModel)null, functionType);
  }

  private SpecificFunctionReference.Argument convertToArgument(HaxeFunctionArgument argument, int index) {
    ResultHolder type;
    if (argument.getTypeOrAnonymous() != null) {
        type = HaxeTypeResolver.getTypeFromTypeOrAnonymous(argument.getTypeOrAnonymous());
    }else {
       type = HaxeTypeResolver.getTypeFromFunctionType(argument.getFunctionType());
    }
    boolean optional = argument.getOptionalMark() != null;
    return new SpecificFunctionReference.Argument(index, optional, type, null);
  }

  @NotNull
  private static ResultHolder convertReturnType(HaxeFunctionReturnType functionReturnType) {
    if (functionReturnType.getTypeOrAnonymous() != null) {
      return HaxeTypeResolver.getTypeFromTypeOrAnonymous(functionReturnType.getTypeOrAnonymous());
    }else {
     return HaxeTypeResolver.getTypeFromFunctionType(functionReturnType.getFunctionType());
    }
  }

  public void specialize(@Nullable PsiElement element) {
    if (element == null || haxeClass == null || !haxeClass.isGeneric()) {
      return;
    }
    if (element instanceof HaxeNewExpression) {
      specializeByParameters(((HaxeNewExpression)element).getType().getTypeParam());
    } else if (element instanceof HaxeMapLiteral || element instanceof HaxeArrayLiteral) {
      specializeByTypeInference(element);
    }
  }

  /**
   * Creates a resolved parameter list that can be used with specializeByParameters() when resolving
   * subclasses.
   *
   * @param targetParam - the haxe class that is being resolved
   * @param innerSpecialization - the specializationlist
   * @return
   */
  @Nullable
  private static List<PsiElement> generateParameterList(HaxeTypeParam targetParam, HaxeGenericSpecialization innerSpecialization) {
    HaxeTypeList typeList = targetParam == null ? null : targetParam.getTypeList();
    if (null == typeList) {
      return null;
    }
    List<PsiElement> instantiationParams = new ArrayList<PsiElement>();
    for (HaxeTypeListPart part : typeList.getTypeListPartList()) {
      final PsiElement type = getTypeOfTypeListPart(part);
      final String name = type != null ? type.getText() : null;

      HaxeResolveResult resolvedParam = name != null ? innerSpecialization.get(null, name) : null;
      HaxeClass resolvedClass = null != resolvedParam ? resolvedParam.getHaxeClass() : null;
      if (null == resolvedClass) {
        resolvedParam = HaxeResolveUtil.getHaxeClassResolveResult(type); // No specialization??
        resolvedClass = null != resolvedParam ? resolvedParam.getHaxeClass() : null;
      }
      if (resolvedClass != null) {
        instantiationParams.add(resolvedClass);
      }else {
        instantiationParams.add(type);
      }
    }
    return instantiationParams;
  }

  public void specializeByParameters(@Nullable HaxeTypeParam param) {
    if (param == null || haxeClass == null || !haxeClass.isGeneric()) {
      return;
    }
    List<PsiElement> specializedTypes = new ArrayList<PsiElement>();
    final HaxeTypeList typeList = param.getTypeList();
    for (int i = 0; i < typeList.getTypeListPartList().size(); i++) {
      final PsiElement specializedType = getTypeOfTypeListPart(typeList.getTypeListPartList().get(i));
      specializedTypes.add(specializedType);  // OK to be null
    }
    specializeByParameters(specializedTypes);
  }

  public void specializeByParameters(@Nullable List<PsiElement> typeList) {
    if (typeList == null || haxeClass == null || !haxeClass.isGeneric()) {
      return;
    }
    final HaxeGenericParam genericParam = haxeClass.getGenericParam();
    assert genericParam != null;
    int size = Math.min(genericParam.getGenericListPartList().size(), typeList.size());
    for (int i = 0; i < size; i++) {
      final HaxeGenericListPart genericListPart = genericParam.getGenericListPartList().get(i);
      final HaxeComponentName genericComponentName = genericListPart != null ? genericListPart.getComponentName() : null;
      final String genericParamName = genericComponentName != null ? genericComponentName.getText() : null;

      final PsiElement specializedType = typeList.get(i);

      if (genericParamName == null) continue;
      if (specializedType == null) continue;

      final HaxeResolveResult specializedTypeResult = HaxeResolveUtil.getHaxeClassResolveResult(specializedType, specialization);
      if(specializedTypeResult.getHaxeClass() != null) {
        specialization.put(haxeClass, genericParamName, specializedTypeResult);
      }
      else if(specializedTypeResult.getFunctionType() != null) {
        specialization.put(haxeClass, genericParamName, specializedTypeResult);
      }else {
          //TODO: Experimental (adding fake haxeClass for generic types that are not connected to any type yet)
          HaxeClassWrapperForTypeParameter aClass = new HaxeClassWrapperForTypeParameter(specializedType.getNode(), List.of());
          HaxeResolveResult result = HaxeResolveResult.create(aClass, new HaxeGenericSpecialization());
          specialization.put(haxeClass, genericParamName, result);
      }

    }
    if (log.isDebugEnabled()) {
      log.debug(specialization.debugDump());
    }
  }

  public void specializeByTypeInference(PsiElement element) {
    HaxeGenericResolver resolver = specialization != null ? specialization.toGenericResolver(element) : new HaxeGenericResolver();
    HaxeExpressionEvaluatorContext evaluated = HaxeExpressionEvaluator.evaluate(element, new HaxeExpressionEvaluatorContext(this.getHaxeClass()), resolver);
    if (null != evaluated.result) {
      SpecificHaxeClassReference classType = evaluated.result.getClassType();
      if (null != classType) {
        softMerge(HaxeGenericSpecialization.fromGenericResolver(classType.getHaxeClass(), classType.getGenericResolver()));
      }
    }
  }

  public void specializeByParent(@NotNull HaxeGenericResolver parentResolver) {
    softMerge(HaxeGenericSpecialization.fromGenericResolver(null, parentResolver));
  }

  public boolean isFunctionType() {
    return functionType != null;
  }
  public boolean isHaxeClass() {
    return haxeClass != null;
  }
  public boolean isHaxeTypeDef() {
    return haxeClass instanceof HaxeTypedefDeclaration;
  }

  public @Nullable HaxeResolveResult fullyResolveTypedef() {
    if(haxeClass instanceof HaxeTypedefDeclaration typedefDeclaration) {
      return HaxeResolver.fullyResolveTypedef(this.getHaxeClass(), this.getSpecialization());
    }
    return null;
  }


  public String debugDump() {
    return debugDump(null);
  }

  public String debugDump( String linePrefix ) {
    StringBuilder builder = new StringBuilder();

    if (linePrefix == null) {
      linePrefix="";
    }

    builder.append(linePrefix);
    if (functionType != null) {
      builder.append(functionType.getName() == null ? "<anonymous haxeFunctionType>" : functionType.getName());
    }else if (haxeClass != null) {
      builder.append(haxeClass.getName() == null ? "<anonymous haxeClass>" : haxeClass.getName());
    }else {
      builder.append("<null haxeClass/HaxeFunctionType>");
    }
    builder.append(":\n");
    String prefix = linePrefix + "  ";
    builder.append(null == specialization ? "<null specialization>" : specialization.debugDump(prefix));
    return builder.toString();
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (functionType != null) {
      builder.append(functionType.getName() == null ? "<anonymous haxeFunctionType>" : functionType.getName());
    }else if (haxeClass != null) {
      builder.append(haxeClass.getName() == null ? "<anonymous haxeClass>" : haxeClass.getName());
    }else {
      builder.append("<null haxeClass/HaxeFunctionType>");
    }
    if (null != haxeClass && haxeClass.isGeneric() && null != specialization) {
      ResultHolder specifics[] = HaxeTypeResolver.resolveDeclarationParametersToTypes(haxeClass, specialization.toGenericResolver(haxeClass), false);
      builder.append('<');
      boolean first = true;
      for(ResultHolder holder : specifics) {
        if (!first) {
          builder.append(',');
        } else {
          first = false;
        }

        if (holder.isFunctionType()) {
          SpecificFunctionReference ref = holder.getFunctionType();
          builder.append(ref == null ? "'unknown'" : ref.toStringWithConstant());
        }else {
          SpecificHaxeClassReference ref = holder.getClassType();
          builder.append(ref == null ? "'unknown'" : ref.toStringWithConstant());
        }
      }
      builder.append('>');
    }
    return builder.toString();
  }

  public JavaResolveResult toJavaResolveResult() {
    return new JavaResult(this);
  }

  private static class JavaResult implements JavaResolveResult {
    private HaxeResolveResult originalResult = null;
    public JavaResult(HaxeResolveResult result) { originalResult = result; }
    @Override public PsiElement getElement() { return (originalResult != null ? originalResult.getHaxeClass() : null); }
    @NotNull
    @Override public PsiSubstitutor getSubstitutor() { return PsiSubstitutor.EMPTY; }
    @Override public boolean isValidResult() { return null != this.getElement(); }
    @Override public boolean isAccessible() { return true; }
    @Override public boolean isStaticsScopeCorrect() { return true; } // TODO: How to check scope?
    @Override public PsiElement getCurrentFileResolveScope() { return (this.getElement() != null ? this.getElement().getOriginalElement() : null); } // TODO: Verify
    @Override public boolean isPackagePrefixPackageReference() { return false; }  // TODO: No idea what to do with this.
  }

}
