/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2019 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterScope;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.type.resolver.ResolverEntry;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil.*;
import static com.intellij.plugins.haxe.model.type.ResultHolder.nullOrUnknown;


@CustomLog
public class HaxeGenericResolver {
  final private LinkedList<ResolverEntry> resolvers;
  final private LinkedList<ResolverEntry> constaints;
  final private LinkedList<ResolverEntry> arguments;

  @Getter
  @Setter
  private ResultHolder assignHint;

  public HaxeGenericResolver() {
    this.resolvers = new LinkedList<>();
    this.constaints = new LinkedList<>();
    this.arguments = new LinkedList<>();
  }

  public void add(@NotNull HaxeTypeParameterDeclaration typeParameter, @NotNull ResultHolder specificType) {
    String name = typeParameter.getQualifiedName();
    specificType = replaceAnyEnumValueWithEnumClass(specificType);
    HaxeTypeParameterScope scope = typeParameter.getTypeParameterScope();
    resolvers.removeIf(entry -> entry.typeParameter().equals(typeParameter) && entry.scope() == scope);
    resolvers.add(new ResolverEntry(name, typeParameter, specificType, scope));
  }

  public void addConstraint(@NotNull HaxeTypeParameterDeclaration typeParameter, @NotNull ResultHolder specificType) {
    String name = typeParameter.getQualifiedName();
    specificType = replaceAnyEnumValueWithEnumClass(specificType);
    HaxeTypeParameterScope scope = typeParameter.getTypeParameterScope();
    constaints.removeIf(entry -> entry.typeParameter().equals(typeParameter) && entry.scope() == scope);
    constaints.add(new ResolverEntry(name,typeParameter,  specificType, scope));
  }

  public void addArgument(@NotNull HaxeTypeParameterDeclaration typeParameter, @NotNull ResultHolder specificType) {
    String name = typeParameter.getQualifiedName();
    specificType = replaceAnyEnumValueWithEnumClass(specificType);
    arguments.removeIf(entry -> entry.typeParameter().equals(typeParameter));
    arguments.add(new ResolverEntry(name,typeParameter,  specificType, null));
  }

  public void addArguments(@NotNull HaxeGenericResolver otherResolver) {
    for (ResolverEntry argument : otherResolver.arguments) {
      addArgument(argument);
    }
  }



  private void add(@NotNull ResolverEntry entry) {
    add(entry.typeParameter(), entry.type());
  }
  private void addConstraint(@NotNull ResolverEntry entry) {
    addConstraint(entry.typeParameter(), entry.type());
  }
  private void addArgument(@NotNull ResolverEntry entry) {
    addArgument(entry.typeParameter(), entry.type());
  }



  public HaxeGenericResolver addAll(@Nullable HaxeGenericResolver parentResolver) {
    if (null != parentResolver && parentResolver != this) {
      assignHint = parentResolver.assignHint;
      // not using "collection.addAll" because there is extra logic in add() that we need to execute
      for (ResolverEntry resolver : parentResolver.resolvers) {
        this.add(resolver.typeParameter(), resolver.type());
      }
      for (ResolverEntry entry : parentResolver.constaints) {
        this.addConstraint(entry.typeParameter(), entry.type());
      }
      for (ResolverEntry entry : parentResolver.arguments) {
        this.addArgument(entry.typeParameter(), entry.type());
      }
    }
    return this;
  }

  public void addOnly(@Nullable HaxeGenericResolver parentResolver, @NotNull HaxeTypeParameterScope scope) {
    if (null != parentResolver && parentResolver != this) {
      // not using "collection.addAll" because there is extra logic in add() that we need to execute
      for (ResolverEntry resolver : parentResolver.resolvers) {
        if (resolver.scope() == scope) this.add(resolver);
      }
      for (ResolverEntry entry : parentResolver.constaints) {
        if (entry.scope() == scope)  this.addConstraint(entry);
      }
      for (ResolverEntry entry : parentResolver.arguments) {
        if (entry.scope() == scope)  this.addArgument(entry);
      }
    }
  }


  @Nullable
  public ResultHolder resolveArgument(@NotNull HaxeTypeParameterDeclaration typeParameter) {
    return listSearch(arguments, typeParameter);
  }

  @Nullable
  public ResultHolder resolveConstraint(@NotNull HaxeTypeParameterDeclaration typeParameter) {
    return listSearch(constaints, typeParameter);
  }

  @Nullable
  public ResultHolder resolveTypeParameter(@NotNull HaxeTypeParameterDeclaration typeParameter) {
    return  resolveTypeParameter(typeParameter, false);
  }

  @Nullable
  public ResultHolder resolveTypeParameter(@NotNull HaxeTypeParameterDeclaration typeParameter, boolean useAssignHint) {
    // arguments has higher precedence than normal resolver values, normal resolver values have higher precedence than constraints
    ResultHolder holder = listSearch(arguments, typeParameter);
    if (holder == null) holder = listSearch(resolvers, typeParameter);
    if (nullOrUnknown(holder)) {
      ResultHolder constraint = listSearch(constaints, typeParameter);
      if(!nullOrUnknown(constraint)) holder = constraint;
    }

    // if none of the method parameters specifies the type parameter
    // in a call expression and only the return type uses the type parameter
    // then the assign value is what defines the type-parameter
    if (useAssignHint && holder != null) {
      holder = useAssignHintIfPossible(holder);
    }
    return holder;
  }

  /**
   * return values:
   * null : typeParameter not found in resolver
   * unknown : typeParameter found but type is unknown
   */
  @Nullable
  public ResultHolder resolve(@NotNull HaxeClass haxeClass) {
    if(haxeClass instanceof  HaxeTypeParameterDeclaration typeParameter) {
      return resolveTypeParameter(typeParameter);
    }
    ResultHolder type = haxeClass.getModel().getInstanceType();
    return resolve(type);
  }

  /**
   * return values:
   * null : typeParameter not found in resolver
   * unknown : typeParameter found but type is unknown
   */
  @Nullable
  public ResultHolder resolve(ResultHolder resultHolder) {
    if (resultHolder == null) return null;
    return resolve(resultHolder, false);
  }

  /**
   * return values:
   * null : typeParameter not found in resolver
   * unknown : typeParameter found but type is unknown
   */
  @Nullable
  public ResultHolder resolve(@NotNull ResultHolder resultHolder,  boolean useAssignHint) {
    //TODO useAssignHint ?
     return resolve(resultHolder.getType());
  }

  /**
   * return values:
   * null : typeParameter not found in resolver
   * unknown : typeParameter found but type is unknown
   */
  @Nullable
  public ResultHolder resolve(@NotNull SpecificTypeReference type) {
    if (type instanceof SpecificHaxeClassReference classReference) {
      if (classReference.getHaxeClass() instanceof HaxeTypeParameterDeclaration typeParameter) {
        return resolveTypeParameter(typeParameter);
      }
      return resolveParameterized(classReference).createHolder();
    }

    if (type instanceof SpecificFunctionReference functionReference) {
      return Optional.ofNullable(resolveParameterized(functionReference))
        .map(SpecificTypeReference::createHolder)
        .orElse(null);
    }

    if (type instanceof SpecificEnumValueReference functionReference) {
      return Optional.ofNullable(resolveParameterized(functionReference))
        .map(SpecificTypeReference::createHolder)
        .orElse(null);
    }
    return type.createHolder();
  }


  private SpecificEnumValueReference resolveParameterized(SpecificEnumValueReference reference) {
    SpecificTypeReference resolve = resolve(reference.getEnumClass());
    if(resolve instanceof SpecificHaxeClassReference classReference) {
      HaxeGenericResolver genericResolver = classReference.getGenericResolver();
      return reference.withResolver(genericResolver);
    }
    return reference;
  }

  private SpecificFunctionReference resolveParameterized(SpecificFunctionReference reference) {
    ResultHolder returnType = resolve(reference.getReturnType());
    if(returnType == null) returnType = reference.getReturnType();

    List<HaxeArgument> argumentList = new ArrayList<>();
    for (HaxeArgument argument : reference.getArguments()) {
      if(argument.getType().isTypeParameter()) {
        ResultHolder resolve = resolve(argument.getType());
        if(resolve != null) {
          argumentList.add(argument.withType(resolve));
        }else {
          argumentList.add(argument.copy());
        }
      }else {
        argumentList.add(argument.copy());
      }
    }

    return reference.withTypes(argumentList, returnType);
  }

  private SpecificHaxeClassReference resolveParameterized(SpecificHaxeClassReference reference) {
    @NotNull ResultHolder[] specifics = reference.getSpecifics();
    @NotNull ResultHolder[] resolvedSpecifics =  new ResultHolder[specifics.length];

    for (int i = 0, length = specifics.length; i < length; i++) {
      ResultHolder resolve = resolve(specifics[i].getType());
      resolvedSpecifics[i] = Optional.ofNullable(resolve).orElse(specifics[i]);
    }

    return SpecificHaxeClassReference.withGenerics(reference.getHaxeClassReference(), resolvedSpecifics);
  }



  @Nullable
  public SpecificEnumValueReference resolve(SpecificEnumValueReference enumValueReference) {
    return resolveParameterized(enumValueReference);
  }

  @Nullable
  public SpecificFunctionReference resolve(SpecificFunctionReference functionReference) {
    return  resolveParameterized(functionReference);
  }

  @Nullable
  public SpecificTypeReference resolve(SpecificHaxeClassReference classReference) {
    HaxeClass referenceHaxeClass = classReference.getHaxeClass();
    if (referenceHaxeClass == null) return null;
    ResultHolder resolve = resolve(referenceHaxeClass);
    if(resolve == null ) return null;
    return resolve.getType();
  }


  @Nullable
  public SpecificFunctionReference resolve(SpecificFunctionReference functionReference, boolean useAssignHint) {
    if (null == functionReference ) return null;
    if (resolvers.isEmpty() && constaints.isEmpty()) return functionReference;

    List<HaxeArgument> arguments = functionReference.getArguments();
    ResultHolder returnType = functionReference.getReturnType();

      if (useAssignHint) {
        if (assignHint != null) {
          if (assignHint.isFunctionType()) {
            SpecificFunctionReference hintFunction = assignHint.getFunctionType();

            List<HaxeArgument> hintArguments = hintFunction.getArguments();
            List<HaxeArgument> newArgumentList = new ArrayList<>();
            int argumentCount = Math.min(arguments.size(), hintArguments.size());
            for (int i = 0; i < argumentCount; i++) {
              HaxeArgument argument = arguments.get(i);
              if (argument.getType().isTypeParameter()) {
                newArgumentList.add(argument.withType(hintArguments.get(i).getType()));
              }
              else {
                newArgumentList.add(argument);
              }
            }
            if (returnType.isTypeParameter()) {
              returnType = hintFunction.getReturnType();
            }
            if (functionReference.functionType != null) {
              return new SpecificFunctionReference(newArgumentList, returnType, functionReference.functionType, functionReference.context);
            }else {
              return new SpecificFunctionReference(newArgumentList, returnType, functionReference.method, functionReference.context);
            }
          }
        }
      }


      List<HaxeArgument> newArgList = arguments.stream()
        .map(argument -> argument.withType(Optional.ofNullable(resolve(argument.getType())).orElse(argument.getType())))
        .toList();

      ResultHolder newReturnType = resolve(returnType);
      ResultHolder returnValue = Optional.ofNullable(newReturnType).orElse(returnType);

    if (functionReference.functionType != null) {
      return new SpecificFunctionReference(newArgList, returnValue, functionReference.functionType, functionReference.context);
    }else {
      return new SpecificFunctionReference(newArgList, returnValue, functionReference.method, functionReference.context);
    }


  }

  private ResultHolder replaceAnyEnumValueWithEnumClass(@NotNull ResultHolder specificType) {
    // EnumValues cant be typeParameters, replacing with declaring EnumClass
    if (specificType.isEnumValueType()){
      specificType = specificType.getEnumValueType().getEnumClass().createHolder();
    }
    return specificType;
  }

  /**
   * @return The names of all generics in this resolver in order of their adding.
   */
  @NotNull
  public String[] names() {
    return resolvers.stream().map(ResolverEntry::name).toArray(String[]::new);
  }
  @NotNull
  public ResolverEntry[] entries() {
    return resolvers.toArray(ResolverEntry[]::new);
  }
  @NotNull
  public ResolverEntry[] constraints() {
    return constaints.toArray(ResolverEntry[]::new);
  }


  /**
   * @return All specific generic types in this resolver in the order of their adding.
   */
  @NotNull
  public ResultHolder[] getSpecifics() {
    if (resolvers.isEmpty()) return ResultHolder.EMPTY;
    ResultHolder results[] = new ResultHolder[resolvers.size()];
    int i = 0;
    for (ResolverEntry resolverEntry : resolvers) {
      results[i++] = resolverEntry.type();
    }
    return results;
  }

  @NotNull
  public ResultHolder[] getSpecificsFor(@Nullable HaxeClassReference clazz) {
    return getSpecificsFor(clazz != null ? clazz.getHaxeClass() : null);
  }

  @NotNull
  public ResultHolder[] getSpecificsFor(@Nullable HaxeClass hc) {
    if (null == hc) return ResultHolder.EMPTY;

    return HaxeTypeResolver.resolveDeclarationParametersToTypes(hc, this);
  }

  /**
   * @return whether or not this resolver has any entries.
   */
  public boolean isEmpty() {
    return resolvers.isEmpty() && constaints.isEmpty();
  }

  @NotNull
  public HaxeGenericSpecialization getSpecialization(@Nullable PsiElement element) {
    return HaxeGenericSpecialization.fromGenericResolver(element, this);
  }


  public HaxeGenericResolver copy() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolver.resolvers.addAll(resolvers);
    resolver.constaints.addAll(constaints);
    resolver.arguments.addAll(arguments);
    resolver.assignHint = assignHint;
    return resolver;
  }

  private ResultHolder useAssignHintIfPossible(ResultHolder type) {
    if(assignHint != null) {
      if (type.canAssign(assignHint)) return assignHint;
    }
    return type;
  }



  public SpecificFunctionReference substituteTypeParamsWithAssignHintTypes(SpecificFunctionReference type) {
    if (assignHint == null) return type;

    SpecificFunctionReference functionTypeHint = assignHint.getFunctionType();
    if (functionTypeHint == null) return type;

    List<HaxeArgument> originalArguments = type.getArguments();
    List<HaxeArgument> hintArguments = functionTypeHint.getArguments();

    LinkedList<HaxeArgument> args = new LinkedList<>();

    int hintArgumentCount = hintArguments.size();
    int orignalArgumentCount = originalArguments.size();
    for (int i = 0; i < orignalArgumentCount; i++) {
      HaxeArgument argument = originalArguments.get(i);
      if (argument.isTypeParameter() && i < hintArgumentCount) {
        HaxeArgument hint = hintArguments.get(i);
        args.add(new HaxeArgument(i, argument.isOptional(), argument.isRest(), hint.getType(), argument.getName()));
      }else {
        args.add(argument);
      }
    }
    ResultHolder returnType = type.getReturnType();
    if (returnType.isTypeParameter()) {
      returnType = functionTypeHint.getReturnType();
    }
    if (type.method != null) {
      return new SpecificFunctionReference(args, returnType,  type.method, type.context );
    }else {
      return new SpecificFunctionReference(args, returnType,  type.functionType, type.context);
    }
  }

  public String toCacheString() {
    if (isEmpty()) return "EMPTY";

    StringBuilder builder = new StringBuilder(128);

    builder.append("resolvers:[");
    for (ResolverEntry resolver : resolvers) {
      builder.append(resolver.name()).append(":").append(resolver.type().toPresentationString()).append(":")
        .append(resolver.scope());
    }

    builder.append("], constraints: [");
    for (ResolverEntry entry : constaints) {
      builder.append(entry.name()).append(":").append(entry.type().toPresentationString()).append(":").append(entry.scope());
    }
    builder.append("]");
    //TODO assign hint ?

    return builder.toString();

  }

  public HaxeGenericResolver withoutMethodTypeParameters() {
    return without(HaxeTypeParameterScope.METHOD);
  }

  public HaxeGenericResolver withoutClassTypeParameters() {
    return without(HaxeTypeParameterScope.CLASS);
  }

  public HaxeGenericResolver without(HaxeTypeParameterScope scope) {
    HaxeGenericResolver copy = copy();
    copy.resolvers.removeIf(entry -> entry.scope() == scope);
    copy.constaints.removeIf(entry -> entry.scope() == scope);
    return copy;
  }

  public HaxeGenericResolver withoutArgumentType() {
    HaxeGenericResolver copy = copy();
    copy.arguments.clear();
    return copy;
  }

  public HaxeGenericResolver withoutUnknowns() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();

    resolver.assignHint = assignHint;

    for (ResolverEntry resolverEntry : resolvers) {
      if (!resolverEntry.type().isUnknown()) {
        resolver.resolvers.add(resolverEntry);
      }
    }
    for (ResolverEntry entry : constaints) {
      if (!entry.type().isUnknown()) {
        resolver.constaints.add(entry);
      }
    }
    for (ResolverEntry entry : arguments) {
      if (!entry.type().isUnknown()) {
        resolver.arguments.add(entry);
      }
    }
    return resolver;
  }

  public HaxeGenericResolver withoutAssignHint() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolver.resolvers.addAll(resolvers);
    resolver.constaints.addAll(constaints);
    resolver.arguments.addAll(arguments);
    return resolver;
  }


  /*
     when resolving types inside a method with generic parameters we want to show the generic types and not unknown
      this method creates a new resolver and replaces its unknowns with  GenericParams from generic params models
   */
  public HaxeGenericResolver withTypeParametersAsType(@NotNull List<HaxeGenericParamModel> params) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    resolver.resolvers.addAll(resolvers);

    for (HaxeGenericParamModel param : params) {
      String name = param.getName();
      Optional<ResolverEntry> match = resolver.resolvers.stream()
        .filter(entry -> entry.name().equals(name))
        .filter(entry -> entry.type().isUnknown())
        .findAny();

      if(match.isPresent()) {
        HaxeClassReference classReference = new HaxeClassReference(name, param.getPsi(), true);
        ResultHolder holder = new ResultHolder(SpecificHaxeClassReference.withoutGenerics(classReference));
        resolver.resolvers.add(new ResolverEntry(name, param.getTypeParameter(), holder, match.get().scope()));
      }
    }
    resolver.constaints.addAll(constaints);
    return resolver;
  }

  //TODO remove
  //TODO probably should not rely on this
  public HaxeClass resolversClass() {
    for (ResolverEntry resolver : resolvers) {
      if(resolver.scope() == HaxeTypeParameterScope.CLASS) {
        if(resolver.typeParameter().getOwner() instanceof  HaxeClass haxeClass) {
          return haxeClass;
        }
      }
    }
    for (ResolverEntry resolver : constaints) {
      if(resolver.scope() == HaxeTypeParameterScope.CLASS) {
        if(resolver.typeParameter().getOwner() instanceof  HaxeClass haxeClass) {
          return haxeClass;
        }
      }
    }
    return null;
  }



  //TODO all members needing resolver should do a remap of resolver results
  // reference should get resolver as it got spesifics
  // TODO also move to generic resolver so we dont neeed to pass resolver
  //@NotNull
  //public HaxeGenericResolver translateTo(@NotNull SpecificHaxeClassReference reference) {
  //  HaxeClassModel classModel = reference.getHaxeClassModel();
  //  if(classModel != null) {
  //    return translateTo(classModel);
  //  }else {
  //    log.warn("Unable to get classModel for " + reference);
  //    return this;
  //  }
  //}
  //TODO !!!! does not work if our resolver id empty, not able to find current class
  // write up some javadoc explanation
  @NotNull
  public HaxeGenericResolver translateFromTo(@Nullable HaxeClass source, @Nullable HaxeClass target) {
    if(source == null) return this;
    if(target == null) return this;
    if(target == source) return this;

    HaxeGenericResolver newResolver = withoutClassTypeParameters();
    boolean sourceToTarget = !findClassHierarchy(source, target).isEmpty();
    boolean targetToSource = !findClassHierarchy(target, source).isEmpty();

      if (sourceToTarget) {
        newResolver.addAll(createInheritedClassResolver(this, target, source));
        return newResolver;
      }
      else if(targetToSource) {
        newResolver.addAll(createExtendingClassResolver(this, source, target));
        return newResolver;
      }else {
        // no change
        return this;
      }
  }

  private HaxeGenericResolver TranslateAbstractToUnderlying(@NotNull HaxeClass source) {
    HaxeGenericResolver newResolver = withoutClassTypeParameters();
    //TODO move  to its own method and add support for recursion, ( underlying type might be another abstract with underlying type)
    // mapping  abstract to underlying  type
    if (source instanceof HaxeAbstractTypeDeclaration declaration) {
      HaxeUnderlyingType type = declaration.getUnderlyingType();
      if(type != null && type.getTypeOrAnonymous() != null) {
        ResultHolder holder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(type.getTypeOrAnonymous());
        SpecificHaxeClassReference classType = holder.getClassType();
        if(classType != null) {
          HaxeGenericResolver genericResolver = classType.getGenericResolver();

          for (@NotNull ResolverEntry entry : genericResolver.entries()) {
            String lookupName = entry.typeParameter().getName();
            for (ResolverEntry resolverEntry : resolvers) {
              if(Objects.equals(resolverEntry.typeParameter().getName(), lookupName)) {
                newResolver.add(entry.withType(resolverEntry.type()));
              }
            }
          }
          for (@NotNull ResolverEntry entry : genericResolver.constaints) {
            String lookupName = entry.typeParameter().getName();
            for (ResolverEntry constraintEntry : constaints) {
              if(Objects.equals(constraintEntry.typeParameter().getName(), lookupName)) {
                newResolver.addConstraint(entry.withType(constraintEntry.type()));
              }
            }
          }
        }
      }
    }
    return newResolver;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HaxeGenericResolver otherResolver) {
      if (otherResolver.resolvers.size() != resolvers.size()) return false;
      if (otherResolver.constaints.size() != constaints.size()) return false;
      if (otherResolver.arguments.size() != arguments.size()) return false;

      if (listCompare(otherResolver.resolvers, resolvers)) return false;
      if (listCompare(otherResolver.constaints, constaints)) return false;
      if (listCompare(otherResolver.arguments, arguments)) return false;

      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return resolvers.hashCode() * constaints.hashCode() * arguments.hashCode() ;
  }


  private static @Nullable ResultHolder listSearch(LinkedList<ResolverEntry> resolvers, @NotNull HaxeTypeParameterDeclaration typeParameter) {
    return resolvers.stream()
      .filter(entry -> entry.typeParameter() == typeParameter)
      .findFirst()
      .map(ResolverEntry::type)
      .orElse(null);
  }

  private static boolean listCompare(LinkedList<ResolverEntry> otherResolver, LinkedList<ResolverEntry> resolvers) {
    for (int i = 0; i < otherResolver.size(); i++) {
      ResolverEntry otherEntry = otherResolver.get(i);
      ResolverEntry thisEntry = resolvers.get(i);
      if (!otherEntry.equals(thisEntry)) return true;
    }
    return false;
  }

  public boolean contains(HaxeTypeParameterDeclaration parameter) {
    return listSearch(resolvers, parameter) != null;
  }

  public void update(HaxeTypeParameterDeclaration typeParameter, ResultHolder resultHolder) {
    Optional<ResolverEntry> match = resolvers.stream()
      .filter(entry -> entry.typeParameter() == typeParameter)
      .findFirst();

    if (match.isPresent()) {
      ResolverEntry old = match.get();
      resolvers.remove(old);
      resolvers.add(old.withType(resultHolder));
    }
  }
}
