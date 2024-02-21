/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterMultiType;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.*;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.type.HaxeMacroUtil.isMacroMethod;
import static java.util.function.Predicate.not;

@CustomLog
@EqualsAndHashCode
public class SpecificHaxeClassReference extends SpecificTypeReference {
  private static final String CONSTANT_VALUE_DELIMITER = " = ";
  private static final Key<CachedValue<Set<SpecificHaxeClassReference>>> COMPATIBLE_TYPES_TO_KEY = new Key<>("HAXE_COMPATIBLE_TYPES_TO");
  private static final Key<CachedValue<Set<SpecificHaxeClassReference>>> COMPATIBLE_TYPES_FROM_KEY = new Key<>("HAXE_COMPATIBLE_TYPES_FROM");
  private static final Key<CachedValue<Set<SpecificHaxeClassReference>>> INFER_TYPES_KEY = new Key<>("HAXE_INFER_TYPES");
  private static final ThreadLocal<Stack<HaxeClass>> processedElements = ThreadLocal.withInitial(Stack::new);
  private static final ThreadLocal<Stack<SpecificHaxeClassReference>> processedElementsToString = ThreadLocal.withInitial(Stack::new);
  private static final ThreadLocal<SpecificHaxeClassReference> currentProcessingElement = new ThreadLocal<>();

  @NotNull private final HaxeClassReference classReference;
  @NotNull private final ResultHolder[] specifics;
  @Nullable private final Object constantValue;
  @Nullable private final HaxeRange rangeConstraint;

  @Nullable private SpecificFunctionReference typeDefFunction;
  @Nullable private SpecificHaxeClassReference typeDefClass;

  @Nullable private HaxeClass clazz;

  // workaround to avoid overflow when wrapping and unwrapping
  public boolean isWrapper = false;

  public SpecificHaxeClassReference(
    @NotNull HaxeClassReference classReference,
    @NotNull ResultHolder[] specifics,
    @Nullable Object constantValue,
    @Nullable HaxeRange rangeConstraint,
    @NotNull PsiElement context
  ) {
    super(context);
    this.classReference = classReference;
    this.specifics = specifics;
    this.constantValue = constantValue;
    this.rangeConstraint = rangeConstraint;
  }

  public static SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, null, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, constantValue, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics) {
      return new SpecificHaxeClassReference(clazz, specifics != null ?  specifics : ResultHolder.EMPTY, null, null, clazz.elementContext);
  }

  public static SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics, Object constantValue) {
    return new SpecificHaxeClassReference(clazz,  specifics != null ?  specifics : ResultHolder.EMPTY, constantValue, null, clazz.elementContext);
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    if(clazz == null || !clazz.isValid()) {
      clazz = this.getHaxeClassReference().getHaxeClass();
    }
    return clazz;
  }


  @Nullable
  public HaxeClassModel getHaxeClassModel() {
    final HaxeClass aClass = getHaxeClass();
    return (aClass != null) ? aClass.getModel() : null;
  }

  @Nullable
  public String getClassName() {
    return this.getHaxeClassReference().getName();
  }

  public SpecificHaxeClassReference withConstantValue(Object constantValue) {
    return new SpecificHaxeClassReference(getHaxeClassReference(), getSpecifics().clone(), constantValue, null, context);
  }

  @Override
  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    if (this.getRangeConstraint() == range) return this;
    return new SpecificHaxeClassReference(getHaxeClassReference(), getSpecifics().clone(), getConstant(), range, context);
  }

  @Override
  public HaxeRange getRangeConstraint() {
    return this.rangeConstraint;
  }

  @Override
  public Object getConstant() {
    return constantValue;
  }

  public String toPresentationString() {
    Stack<SpecificHaxeClassReference> stack = processedElementsToString.get();
    try {
      HaxeClassModel classModel = getHaxeClassModel();
      // stack overflow guard
      if (stack.contains(this) && classModel != null) {
        List<HaxeGenericParamModel> params = classModel.getGenericParams();
        if (!params.isEmpty()) {
          log.warn("toString overflow prevention");
          return "?"; // prevent overflow
        }
      }
      stack.add(this);

      StringBuilder out = new StringBuilder(this.getHaxeClassReference().getName());
      ResultHolder[] specifics = getSpecifics();
      if (specifics.length > 0) {
        out.append("<");
        for (int n = 0; n < specifics.length; n++) {
          if (n > 0) out.append(", ");
          ResultHolder specific = specifics[n];
          if (specific == null) {
            out.append(UNKNOWN);
          }
          else if (specific.getType() == this) {
            List<HaxeGenericParamModel> params = classModel.getGenericParams();
            if (params.size() > n) {
              HaxeGenericParamModel model = params.get(n);
              out.append(model.getName());
            }
            else {
              out.append("*Recursion Error*");
            }
            log.warn("`this` and `specific.getType()` are the same object (Recursion protection)");
          }
          else {
            out.append(specific.toStringWithoutConstant());
          }
        }
        out.append(">");
      }
      String result = out.toString();
      if (result.equals("Dynamic<Dynamic>")) return "Dynamic";
      if (result.equals("Dynamic<unknown>")) return "Dynamic";
      return result;
    }finally {
      stack.remove(this);
    }
  }

  public String toStringWithoutConstant() {
    return toPresentationString();
  }

  public String toStringWithConstant() {
    String out = toStringWithoutConstant();
    if (getConstant() != null) {
      out += CONSTANT_VALUE_DELIMITER + getConstant().toString();

      //switch (out) {
      //  case SpecificTypeReference.INT:
      //    out += CONSTANT_VALUE_DELIMITER + (int)HaxeTypeUtils.getDoubleValue(getConstant());
      //    break;
      //  case SpecificTypeReference.STRING:
      //    out += CONSTANT_VALUE_DELIMITER + getConstant();
      //    break;
      //  default:
      //    out += CONSTANT_VALUE_DELIMITER + getConstant();
      //    break;
      //}
    }
    if (getRangeConstraint() != null) {
      out += " [" + getRangeConstraint() + "]";
    }
    return out;
  }

  @Override
  public String toString() {
    return toStringWithConstant();
  }

  @NotNull
  public HaxeGenericResolver getGenericResolver() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    HaxeClassModel model = getHaxeClassModel();
    if (model != null) {
      if (model instanceof HaxeAnonymousTypeModel anonymousTypeModel
          && anonymousTypeModel.haxeClass instanceof HaxeTypeParameterMultiType multiType) {
        //TODO move into HaxeAnonymousTypeModel or HaxeTypeParameterMultiType maybe solve as getGenericParam
        List<HaxeGenericResolver> list =
          multiType.getHaxeExtendsList().stream()
            .map(HaxeTypeResolver::getTypeFromType)
            .filter(not(ResultHolder::isUnknown))
            .filter(ResultHolder::isClassType)
            .map(resultHolder -> resultHolder.getClassType().getGenericResolver())
            .toList();

        list.forEach(resolver::addAll);
      } else {
        List<HaxeGenericParamModel> params = model.getGenericParams();
        for (int n = 0; n < params.size(); n++) {
          HaxeGenericParamModel paramModel = params.get(n);
          ResultHolder specific = (n < getSpecifics().length) ? this.getSpecifics()[n] : getUnknown(context).createHolder();
          if (specific == null) specific = getUnknown(context).createHolder();// null safety
          //TODO check constraints
          resolver.add(paramModel.getName(), specific, ResolveSource.CLASS_TYPE_PARAMETER);
        }
      }
    }
    return resolver;
  }

  /** Get the return type of the named method or field in the class referenced by this object. */
  @Nullable
  @Override
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver) {
    if (this.isDynamic()) return this.withoutConstantValue().createHolder();

    if (name == null) {
      return null;
    }

    HaxeClass aClass = this.getHaxeClassReference().getHaxeClass();
    if (aClass == null) {
      return null;
    }
    HaxeGenericResolver localResolver = new HaxeGenericResolver();
    localResolver.addAll(resolver);
    if (aClass.isTypeDef()) {
      HaxeResolveResult result = HaxeResolver.fullyResolveTypedef(aClass, resolver.getSpecialization(aClass));
      if (result.isHaxeClass()) {
        aClass = result.getHaxeClass();
        localResolver.addAll(result.getGenericResolver());
      }
    }
    HaxeNamedComponent method = aClass.findHaxeMethodByName(name, localResolver);
    if (method != null) {
      if (context.root == method) return null;
      if (isMacroMethod(method)) {
        // if macro method replace Expr / ExprOf types
        ResultHolder functionType = HaxeTypeResolver.getMethodFunctionType(method, localResolver.withoutUnknowns());
        return HaxeMacroUtil.resolveMacroTypesForFunction(functionType);
      }
      return HaxeTypeResolver.getMethodFunctionType(method, localResolver);
    }

    HaxeNamedComponent field = aClass.findHaxeFieldByName(name, localResolver);
    if (field != null) {
      if (context.root == field) return null;
      return HaxeTypeResolver.getFieldOrMethodReturnType(field, localResolver);
    }
    return null;
  }

  public void isExprOf() {
  }

  public enum Compatibility {
    ASSIGNABLE_TO,   // Assignable via @:to or "to <Type>" on an abstract.
    ASSIGNABLE_FROM  // Assignable via @:from or "from <Type>" on an abstract.
  }

  Set<SpecificHaxeClassReference> getCompatibleTypes(Compatibility direction) {
      Set<SpecificHaxeClassReference>result = getCompatibleTypesIInternalCached(direction);
      result.add(this); // adding this only for the type that is being checked (we don't want this done recursively)
      return result;
  }

  private Set<SpecificHaxeClassReference> getCompatibleTypesIInternalCached(Compatibility direction) {
    /** See docs on {@link HaxeDebugUtil#isCachingDisabled} for how to set this flag. */
    boolean skipCachingForDebug =  HaxeDebugUtil.isCachingDisabled();
    HaxeClassModel model = getHaxeClassModel();

    if (!skipCachingForDebug &&  null != model && !model.hasGenericParams()) {

      Key<CachedValue<Set<SpecificHaxeClassReference>>> key = direction == Compatibility.ASSIGNABLE_TO
                                                 ? COMPATIBLE_TYPES_TO_KEY
                                                 : COMPATIBLE_TYPES_FROM_KEY;

      final Stack<HaxeClass> stack = processedElements.get();
      if (stack.contains(model.haxeClass)) return  new HashSet<>();// recursion guard

      Set<SpecificHaxeClassReference> cache;
      // caching that only cache values until any psi element changes, might speed up annotators etc while no code changes are made
      // tracking all classes sub-classes interfaces or anything else that might change type compatibility would be very complex

      // in order to use CachedValuesManager our CachedValueProvider can not be a lambda or method as part of a class instance
      // that contains PSI elements as the lambda/method reference would indirectly keep that psi elementand cause memory leaks
      // or access to an invalid PSI
      currentProcessingElement.set(this);
      if ( direction == Compatibility.ASSIGNABLE_TO) {
        cache = CachedValuesManager.getCachedValue(model.haxeClass, key, SpecificHaxeClassReference::toCachedValueProvider);
      }else {
        cache = CachedValuesManager.getCachedValue(model.haxeClass, key, SpecificHaxeClassReference::fromCachedValueProvider);
      }
      currentProcessingElement.remove();

      processedElements.get().clear();
      // create a new set to avoid  other code to tamper with the cached values
      return new HashSet<>(cache);
    } else {
      Set<SpecificHaxeClassReference> compatibleTypes = getCompatibleTypesInternal(direction);
      processedElements.get().clear();
      return compatibleTypes;
    }
  }

  private static CachedValueProvider.Result<Set<SpecificHaxeClassReference>> toCachedValueProvider() {
    SpecificHaxeClassReference reference = currentProcessingElement.get();
    Set<SpecificHaxeClassReference> result = simpleRemoveDuplicates(reference.getCompatibleTypesInternal(Compatibility.ASSIGNABLE_TO));
    List<HaxeClass> nonGenericClasses = findNonGenericTypes(result);
    boolean onlyNonGeneric = result.size() == nonGenericClasses.size();
    return new CachedValueProvider.Result<>(Set.copyOf(result), onlyNonGeneric
                                                                ? nonGenericClasses.toArray()
                                                                : PsiModificationTracker.MODIFICATION_COUNT);

  }
  private static  CachedValueProvider.Result<Set<SpecificHaxeClassReference>> fromCachedValueProvider() {
    SpecificHaxeClassReference reference = currentProcessingElement.get();
    Set<SpecificHaxeClassReference> result = simpleRemoveDuplicates(reference.getCompatibleTypesInternal(Compatibility.ASSIGNABLE_FROM));

    List<HaxeClass> nonGenericClasses = findNonGenericTypes(result);
    boolean onlyNonGeneric = result.size() == nonGenericClasses.size();
    return new CachedValueProvider.Result<>(Set.copyOf(result), onlyNonGeneric
                                                                ? nonGenericClasses.toArray()
                                                                : PsiModificationTracker.MODIFICATION_COUNT);

  }
  /*
    We want to minimize the amount of work when checking compatibility and we dont want to check the same type multiple times
   (interfaces can be repeated as many times as they are implemented by classes,a common repated use is EventListener interfaces)
   so we do a quick and dirty filtering based on the haxeClass declaration and only keep one instance
   (we ignore specifics as they are also ignored in the caching logic)
   */
  private static Set<SpecificHaxeClassReference> simpleRemoveDuplicates(Set<SpecificHaxeClassReference> set) {
    List<HaxeClass> haxeDeclarations = new ArrayList<>();
    Set<SpecificHaxeClassReference> newSet = new HashSet<>();
    for (SpecificHaxeClassReference reference : set) {
      HaxeClass haxeClass = reference.getHaxeClass();
      if(haxeClass== null) {
        newSet.add(reference);
        continue;
      }
      if (!haxeDeclarations.contains(haxeClass)) {
        haxeDeclarations.add(haxeClass);
        newSet.add(reference);
      }
    }
    return newSet;
  }

  Set<SpecificHaxeClassReference> getInferTypes() {
    HaxeClassModel model = getHaxeClassModel();
    if (null != model && !model.hasGenericParams()) {

      currentProcessingElement.set(this);
      Set<SpecificHaxeClassReference> result = CachedValuesManager.getCachedValue(model.haxeClass, INFER_TYPES_KEY, SpecificHaxeClassReference::inferTypesProvider);
      currentProcessingElement.remove();

      processedElements.get().clear();

      return  new HashSet<>(result);
    } else {
      processedElements.get().clear();
      return getInferTypesInternal();
    }
  }
  private static  CachedValueProvider.Result<Set<SpecificHaxeClassReference>> inferTypesProvider() {
    SpecificHaxeClassReference reference = currentProcessingElement.get();
    Set<SpecificHaxeClassReference> result = simpleRemoveDuplicates(reference.getInferTypesInternal());

    List<HaxeClass> nonGenericClasses = findNonGenericTypes(result);
    boolean onlyNonGeneric = result.size() == nonGenericClasses.size();
    return new CachedValueProvider.Result<>(Set.copyOf(result), onlyNonGeneric
                                                                ? nonGenericClasses.toArray()
                                                                : PsiModificationTracker.MODIFICATION_COUNT);
  }

  @NotNull
  private static List<HaxeClass> findNonGenericTypes(Set<SpecificHaxeClassReference> result) {
    List<HaxeClass> nonGenericClasses = new ArrayList<>();
    for (SpecificHaxeClassReference haxeClassReference : result) {
      HaxeClass aClass = haxeClassReference.getHaxeClass();
      if (aClass != null) {
        if (aClass.isGeneric()) {
          nonGenericClasses.add(aClass);
        }
      }
    }
    return nonGenericClasses;
  }


  private Set<SpecificHaxeClassReference> getCompatibleTypesInternal(Compatibility direction) {
    final Stack<HaxeClass> stack = processedElements.get();
    final HaxeClassModel model = getHaxeClassModel();
    final HaxeGenericResolver genericResolver = getGenericResolver();

    final Set<SpecificHaxeClassReference> list = new HashSet<>();
    if (model == null) return list;
    if (stack.contains(model.haxeClass)) return list;
    stack.push(model.haxeClass);

    // TODO: list.addAll(getCompatibleFunctionTypes(model, genericResolver));
    literalCollectionAssignment(direction, list);
    emptyCollectionAssignment(direction, list);

    if (!model.isAbstractType()) {
      if (model.haxeClass instanceof AbstractHaxeTypeDefImpl typedefDeclaration) {
        SpecificHaxeClassReference type = typedefDeclaration.getTargetClass(genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypesIInternalCached(direction));
        }
      } else for (HaxeType extendsType : model.haxeClass.getHaxeExtendsList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          if(direction == Compatibility.ASSIGNABLE_TO)  list.add(type);
          list.addAll(type.getCompatibleTypesIInternalCached(direction));
        }
      }
      // var myVar:MyClass can not be assigned any object with the same interface,
      // but an interface can be assigned any object that implements it
      if(direction == Compatibility.ASSIGNABLE_TO) {
        final List<HaxeClassReferenceModel> interfaces = model.getImplementingInterfaces();
        for (HaxeClassReferenceModel interfaceReference : interfaces) {
          SpecificHaxeClassReference type = propagateGenericsToType(interfaceReference.getPsi(), genericResolver);
          if (type != null) {
            list.add(type);
            list.addAll(type.getCompatibleTypesIInternalCached(direction));
          }
        }
      }
    } else {

      List<HaxeType> typeList = direction == Compatibility.ASSIGNABLE_FROM ? model.getAbstractFromList() : model.getAbstractToList();
      for (HaxeType extendsType : typeList) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypesIInternalCached(direction));
        }
      }
    }
    return list;
  }

  private void emptyCollectionAssignment(Compatibility direction, Set<SpecificHaxeClassReference> list) {
    if (direction == Compatibility.ASSIGNABLE_TO && context instanceof HaxeArrayLiteral && null == ((HaxeArrayLiteral)context).getExpressionList()) {
      ResultHolder unknownHolderKey = SpecificTypeReference.getUnknown(context).createHolder();
      ResultHolder unknownHolderValue = SpecificTypeReference.getDynamic(context).createHolder();
      SpecificHaxeClassReference holder = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(unknownHolderKey, unknownHolderValue, context);
      list.add(holder);
    }
  }
  private void literalCollectionAssignment(Compatibility direction, Set<SpecificHaxeClassReference> list) {
    // adds "Any" collections to compatibility list if collection is literal
    if (direction == Compatibility.ASSIGNABLE_TO) {
      if (isLiteralArray()) {
        ResultHolder unknownHolder = SpecificTypeReference.getAny(context).createHolder();;
        SpecificHaxeClassReference array = (SpecificHaxeClassReference)SpecificHaxeClassReference.createArray(unknownHolder, context);
        list.add(array);
      }
      if (isLiteralMap()) {
        ResultHolder[] specifics = this.getSpecifics();

        ResultHolder unknownHolderKey = SpecificTypeReference.getAny(context).createHolder();
        ResultHolder unknownHolderValue = SpecificTypeReference.getAny(context).createHolder();

        SpecificHaxeClassReference anyAnyMap = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(unknownHolderKey, unknownHolderValue, context);
        SpecificHaxeClassReference xAnymap = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(specifics[0], unknownHolderValue, context);
        SpecificHaxeClassReference anyXmap = (SpecificHaxeClassReference)SpecificHaxeClassReference.createMap(unknownHolderKey, specifics[1], context);
        list.add(anyAnyMap);
        list.add(xAnymap);
        list.add(anyXmap);
      }
    }
  }


  public boolean isContextAnEnumType() {
    if(context instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression element = (HaxeReferenceExpression)context;
      PsiElement resolve = element.resolve();

      if(resolve instanceof HaxeClass) {
        HaxeClass resolved = (HaxeClass) resolve;
        return resolved.isEnum();
      }
    }
    return false;
  }
  public  boolean isContextAnEnumDeclaration() {
    return context instanceof HaxeEnumDeclaration;
  }

  public  boolean isContextAType() {
    if (context instanceof HaxeType) {
      HaxeParameter type = PsiTreeUtil.getParentOfType(context, HaxeParameter.class);
      return type == null;
    }
    else if (context instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      return resolve instanceof HaxeClass;
    }
    return false;
  }

  public boolean isTypeDef() {
    if(clazz instanceof HaxeTypedefDeclaration) return true;
      return getHaxeClassModel() != null && getHaxeClassModel().isTypedef();
  }
  //TODO MLO: Warning, typedef of typedef will be considered class, should probably return false in this case and create istypeDefOfTypeDef or something
  public boolean isTypeDefOfClass() {
    return isTypeDef() && ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getTargetClass() != null;
  }

  public boolean isTypeDefOfFunction() {
    return isTypeDef() && ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getFunctionType() != null;
  }

  public SpecificHaxeClassReference resolveTypeDefClass() {
    if (typeDefClass != null && typeDefClass.getGenericResolver().isEmpty())  return typeDefClass;
    if (isTypeDef()) {
      typeDefClass = ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getTargetClass(getGenericResolver());
      return typeDefClass;
    }
    return null;
  }
  public SpecificTypeReference fullyResolveTypeDefReference() {
    if (isTypeDefOfFunction()) {
      return  resolveTypeDefFunction();
    }

    SpecificHaxeClassReference reference = resolveTypeDefClass();

    HaxeClass haxeClass = getHaxeClass();
    HaxeGenericResolver resolver = getGenericResolver();
    while (haxeClass instanceof AbstractHaxeTypeDefImpl typeDef) {
      HaxeFunctionType functionType = typeDef.getFunctionType();
      if (functionType != null) {
        SpecificFunctionReference reference1 = reference.resolveTypeDefFunction();
        return resolver.resolve(reference1);
      }
      reference = typeDef.getTargetClass(resolver);
      if (reference.isTypeDefOfClass()) {
        haxeClass = reference.getHaxeClass();
        resolver = reference.getGenericResolver();
      }else{
        break;
      }
    }
    return reference;
  }
  public SpecificTypeReference fullyResolveTypeDefAndUnwrapNullTypeReference() {
    if (isNullType()) {
      SpecificTypeReference typeReference = unwrapNullType();
      if (typeReference instanceof SpecificHaxeClassReference reference) {
        if (reference.isTypeDef()) return reference.fullyResolveTypeDefAndUnwrapNullTypeReference();
      }
      return typeReference;
    }

    if (isTypeDefOfFunction()) {
      return  resolveTypeDefFunction();
    }

    if (isTypeDefOfClass()) {
      SpecificHaxeClassReference reference = resolveTypeDefClass();

      HaxeClass haxeClass = getHaxeClass();
      HaxeGenericResolver resolver = getGenericResolver();
      while (haxeClass instanceof AbstractHaxeTypeDefImpl typeDef) {
        HaxeFunctionType functionType = typeDef.getFunctionType();
        if (functionType != null) {
          SpecificFunctionReference reference1 = reference.resolveTypeDefFunction();
          return resolver.resolve(reference1);
        }
        reference = typeDef.getTargetClass(resolver);
        if (reference.isTypeDefOfClass()) {
          haxeClass = reference.getHaxeClass();
          resolver = reference.getGenericResolver();
        }
        else if (reference.isNullType()) {
          SpecificTypeReference unwrapped = reference.unwrapNullType();
          if (unwrapped instanceof SpecificHaxeClassReference haxeClassReference) {
            haxeClass = haxeClassReference.getHaxeClass();
            resolver = haxeClassReference.getGenericResolver();
            reference = haxeClassReference;
          }
          else if (unwrapped instanceof SpecificFunctionReference functionReference) {
            return resolver.resolve(functionReference);
          }
        }
        else {
          break;
        }
      }
      return reference;
    }
    return this;
  }

  public SpecificTypeReference unwrapNullType() {
    return specifics[0].getType();
  }

  public SpecificFunctionReference resolveTypeDefFunction() {
    if (typeDefFunction != null) return typeDefFunction;
    if (isTypeDef()) {
      HaxeFunctionType type = ((AbstractHaxeTypeDefImpl)getHaxeClassModel().haxeClass).getFunctionType();
      if (type != null) {
        HaxeSpecificFunction function = new HaxeSpecificFunction(type, getGenericResolver().getSpecialization(this.getElementContext()));
        typeDefFunction = SpecificFunctionReference.create(function);
        return typeDefFunction;
      }
    }
    return null;
  }

  public boolean isCoreType() {
    HaxeMetadataList list = HaxeMetadataUtils.getMetadataList(this.getHaxeClass());
    for (HaxeMeta meta : list) {
      if (meta.isCompileTimeMeta() && meta.isType("coreType")) {
        return true;
      }
    }
    return false;
  }

  private String getTypeName(PsiElement context) {
    if (context instanceof HaxeType) {
      return context.getText();
    }
    else if (context instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression element = (HaxeReferenceExpression)context;
      PsiElement resolve = element.resolve();

      if (resolve instanceof HaxeClass) {
        HaxeClass resolved = (HaxeClass)resolve;
        if (element.getText().equals(resolved.getName())) {
          return resolved.getName();
        }
      }
    }
    return null;
  }

  private Set<SpecificHaxeClassReference> getInferTypesInternal() {
    final Stack<HaxeClass> stack = processedElements.get();
    final HaxeClassModel model = getHaxeClassModel();
    final HaxeGenericResolver genericResolver = getGenericResolver();

    final Set<SpecificHaxeClassReference> list = new HashSet<>();
    list.add(this);

    if (model == null) return list;
    if (stack.contains(model.haxeClass)) return list;
    stack.push(model.haxeClass);

    if (!model.isAbstractType()) {
      if (model.haxeClass instanceof HaxeTypedefDeclaration) {
        SpecificHaxeClassReference type = ((AbstractHaxeTypeDefImpl)model.haxeClass).getTargetClass(genericResolver);
        if (type != null) {
          list.add(type);
          list.addAll(type.getCompatibleTypes(Compatibility.ASSIGNABLE_FROM));
        }
      } else
      for (HaxeType extendsType : model.haxeClass.getHaxeExtendsList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }

      final List<HaxeClassReferenceModel> interfaces = model.getImplementingInterfaces();
      for (HaxeClassReferenceModel interfaceReference : interfaces) {
        SpecificHaxeClassReference type = propagateGenericsToType(interfaceReference.getPsi(), genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }
    } else {
      for (HaxeType extendsType : model.getAbstractToList()) {
        SpecificHaxeClassReference type = propagateGenericsToType(extendsType, genericResolver);
        if (type != null) {
          list.addAll(type.getInferTypesInternal());
        }
      }
    }

    return list;
  }

  public static SpecificHaxeClassReference propagateGenericsToType(@Nullable HaxeType type,
                                                             HaxeGenericResolver genericResolver) {
    if (type == null) return null;
    SpecificHaxeClassReference classType = HaxeTypeResolver.getTypeFromType(type, genericResolver).getClassType();
    return propagateGenericsToType(classType, genericResolver);
  }

  public static SpecificHaxeClassReference propagateGenericsToType(@Nullable SpecificHaxeClassReference originalType, @Nullable HaxeGenericResolver genericResolver) {
    return propagateGenericsToType(originalType, genericResolver, false);
  }
  public static SpecificHaxeClassReference propagateGenericsToType(@Nullable SpecificHaxeClassReference originalType,
                                                                   @Nullable HaxeGenericResolver genericResolver, boolean isReturnType) {
    SpecificHaxeClassReference type = originalType;
    if (type == null) return null;
    if (genericResolver == null || genericResolver.isEmpty()) return type;

    if (type.isTypeParameter()) {
      String typeVariableName = type.getHaxeClassReference().name;
      ResultHolder possibleValue = isReturnType ? genericResolver.resolveReturnType(type) : genericResolver.resolve(typeVariableName);
      if (possibleValue != null) {
        SpecificTypeReference possibleType = possibleValue.getType();
        if (possibleType instanceof SpecificHaxeClassReference classReference) {
          type = classReference;
        }
      }
      genericResolver = genericResolver.without(typeVariableName);
    }
    for (ResultHolder specific : type.getSpecifics()) {
      // recursive guard (remove type parameters that has been used)
      if (specific.getClassType() != originalType) {
        genericResolver = genericResolver.without(specific);
        if (!genericResolver.isEmpty()) {
          final SpecificTypeReference typeReference = propagateGenericsToType(specific.getType(), genericResolver);
          if (null != typeReference) {
            specific.setType(typeReference);
          }
        }
      }
      else {
        log.warn("can not propagate Generics To Type, type and specific is the same type");
      }
    }
    return type;
  }


  @NotNull
  public HaxeClassReference getHaxeClassReference() {
    return classReference;
  }

  @NotNull
  public ResultHolder[] getSpecifics() {
    return specifics;
  }

  @NotNull
  public HaxeResolveResult asResolveResult() {
    HaxeClass clazz = getHaxeClass();
    return HaxeResolveResult.create(clazz, getGenericResolver().getSpecialization(clazz));
  }


  public List<HaxeMethodModel> getOperatorOverloads(String operator) {
    if (classReference.classModel == null) return List.of();
    List<HaxeMethodModel> members = new ArrayList<>();
    for (HaxeMemberModel memberModel : classReference.classModel.getMembers(null)) {
      if (memberModel instanceof HaxeMethodModel methodModel) {
        members.add(methodModel);
      }
    }

    List<HaxeMethodModel> list = new ArrayList<>();
    for (HaxeMethodModel member : members) {
      if (member.hasOperatorMeta()) {
        list.add(member);
      }
    }

    List<HaxeMethodModel> result = new ArrayList<>();
    for (HaxeMethodModel model : list) {
      if (model.isOperator(operator)) {
        result.add(model);
      }
    }
    
    return result;
  }
}
