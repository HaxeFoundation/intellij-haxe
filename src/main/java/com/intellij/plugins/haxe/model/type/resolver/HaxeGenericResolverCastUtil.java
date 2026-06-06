package com.intellij.plugins.haxe.model.type.resolver;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeAbstractTypeDeclarationImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class HaxeGenericResolverCastUtil {

    private static final RecursionGuard<Object> transformRecursionGuard = RecursionManager.createGuard("transformRecursionGuard");
    private record RecursionKey(@Nullable HaxeClass source, @Nullable HaxeClass target){}

    @NotNull
    public static HaxeGenericResolver translateFromTo(@NotNull HaxeGenericResolver resolver, @Nullable HaxeClass source, @Nullable HaxeClass target) {
        HaxeGenericResolver newResolver = transformRecursionGuard.doPreventingRecursion(new RecursionKey(source, target), true, () -> _translateFromTo(resolver, source, target));
        if (newResolver == null) return resolver;
        return newResolver;
    }
    @NotNull
    private static HaxeGenericResolver _translateFromTo(@NotNull HaxeGenericResolver resolver, @Nullable HaxeClass source, @Nullable HaxeClass target) {
        if (source == null) return resolver;
        if (target == null) return resolver;
        if (target == source) return resolver;

        source = tryGetConstraintFromTypeParameter(source);
        target = tryGetConstraintFromTypeParameter(target);

        HaxeGenericResolver newResolver = resolver.withoutClassTypeParameters();

        List<SpecificHaxeClassReference> classHierarchy = findCastPath(source, target, resolver);
        boolean sourceToTarget = !classHierarchy.isEmpty();
        if (sourceToTarget) {
            newResolver.addAll(createInheritedClassResolver(resolver, target, source, classHierarchy));
            return newResolver;
        }

        classHierarchy = findCastPath(target, source, resolver);
        boolean targetToSource = !classHierarchy.isEmpty();
        if (targetToSource) {
            newResolver.addAll(createExtendingClassResolver(resolver, source, target, classHierarchy));
            return newResolver;
        }

        if (source instanceof HaxeAbstractTypeDeclarationImpl abstractTypeDeclaration) {
            // if abstract check if target is underlying HaxeClass
            HaxeClass underlyingHaxeClass = findUnderlyingHaxeClass(abstractTypeDeclaration);
//            if (underlyingHaxeClass == target) {
            if (underlyingHaxeClass != null && Objects.equals(underlyingHaxeClass.getQualifiedName(), target.getQualifiedName())) {
                return translateAbstractToUnderlying(resolver, source);
            }
        }
        // no change
        return resolver;
    }


    public static List<SpecificHaxeClassReference> findCastPath(HaxeClass from, HaxeClass to) {
        List<SpecificHaxeClassReference> path = new ArrayList<>();
        findCastPath(from,to, path, new HaxeGenericResolver());
        return path;
    }
    private static List<SpecificHaxeClassReference> findCastPath(HaxeClass from, HaxeClass to, HaxeGenericResolver resolver) {
        List<SpecificHaxeClassReference> path = new ArrayList<>();
        findCastPath(from,to, path, resolver);
        return path;
    }
    public static List<SpecificHaxeClassReference> findClassHierarchy(HaxeClass from, HaxeClass to) {
        List<SpecificHaxeClassReference> path = new ArrayList<>();
        findClassHierarchy(from,to, path, new HaxeGenericResolver());
        return path;
    }

    private static boolean findCastPath(HaxeClass from, HaxeClass to, List<SpecificHaxeClassReference> path,  HaxeGenericResolver parentResolver) {
        //TODO mlo: maybe add recursion-guard to prevent typedef loops etc?

        // stop if "from" is typeParameter
        if(from instanceof HaxeGenericListPart)return false;

        HaxeClassModel fromModel = from.getModel();
        HaxeGenericResolver fromResolver = fromModel.getGenericResolver(parentResolver);
        fromResolver.addAll(parentResolver);
        if (fromModel.isTypedef()) {
            SpecificHaxeClassReference reference = fromModel.getUnderlyingClassReference(fromResolver);
            if (reference!= null) {
                // NOTE: anonymous extendingTypes can extend/ combine multiple definitions
                // we therefor only add to path when we have found and reached the target (when the recursive calls returns true)
                SpecificTypeReference resolvedTypeDef = reference.resolveTypeDefOfClassOrTypeParam();
                if (resolvedTypeDef  instanceof SpecificHaxeClassReference classReference) {
                    HaxeClass childClass = classReference.getHaxeClass();
                    if (childClass == to){
                        path.add(reference);
                        return true;
                    }
                    if (childClass != null){
                        return findClassHierarchy(childClass, to, path, classReference.getGenericResolver());
                    }
                }else {
                    HaxeClass underlyingClass = reference.getHaxeClass();
                    if (underlyingClass == to) {
                        path.add(reference);
                        return true;
                    }else if (findClassHierarchy(underlyingClass, to, path, reference.getGenericResolver())) {
                        path.add(reference);
                        return true;
                    }
                }
            }
        }
        if (fromModel instanceof HaxeAbstractClassModel abstractFromModel) {
            if (canExplicitCast(abstractFromModel, to, path, parentResolver)) {
                path.add(to.getModel().getInstanceReference());
                return true;
            }
            SpecificHaxeClassReference underlyingReference = fromModel.getUnderlyingClassReference(fromResolver);
            if (underlyingReference != null) {
                HaxeClass underlyingClass = underlyingReference.getHaxeClass();
                if (underlyingClass == to) {
                    path.add(underlyingReference);
                    return true;
                } else if (findClassHierarchy(underlyingClass, to, path, underlyingReference.getGenericResolver())) {
                    path.add(underlyingReference);
                    return true;
                } else {
//                    boolean multiType = HaxeAbstractAssignUtil.isMultiType(fromModel.getInstanceReference());
//                    if(multiType) {
//                        // if abstract is multiType we follow the underlying interface
//                        // TODO this changes the "direction" casting, how do we handle that?
//                        if (findClassHierarchy(to,underlyingClass, path)) {
//                            // TODO this probably needs to  be before findClassHierarchy, however if findClassHierarchy fails it should not be included
//                            //path.add(underlyingReference);
//                            return true;
//                        }
//                    }
                }
            }
        }
        // checking typeParameter constraints (can contain references or its own anonymous structures)
        if (fromModel instanceof HaxeConstraintTypeListModel constraintModel) {
            List<ResultHolder> compositeTypes = constraintModel.getCompositeTypes();
            for (ResultHolder compositeType : compositeTypes) {
                if(compositeType.getType() instanceof  SpecificHaxeClassReference reference) {
                    HaxeClass haxeClass = reference.getHaxeClass();
                    if(haxeClass == to) {
                        return path.add(reference);
                    }else {
                        if (findClassHierarchy(haxeClass, to, path, reference.getGenericResolver())) {
                            path.add(reference);
                            return true;
                        }
                    }
                }
            }

        }

        List<HaxeClassReferenceModel> extendingTypes = fromModel.getExtendingTypes();
        for (HaxeClassReferenceModel model : extendingTypes) {
            HaxeClassModel classModel = model.getHaxeClassModel();
            if (classModel != null) {
                HaxeClass childClass = classModel.haxeClass;
                SpecificHaxeClassReference classReference = model.getSpecificHaxeClassReference();
                if (childClass == to) {
                    return path.add(classReference);
                } else {
                    if (findClassHierarchy(childClass, to, path, classReference.getGenericResolver())) {
                        path.add(classReference);
                        return true;
                    }
                }
            }
        }

        List<HaxeClassReferenceModel> interfaces = fromModel.getImplementingInterfaces();
        for (HaxeClassReferenceModel model : interfaces) {
            HaxeClassModel classModel = model.getHaxeClassModel();
            if (classModel != null) {
                HaxeClass childClass = classModel.haxeClass;
                SpecificHaxeClassReference classReference = model.getSpecificHaxeClassReference();
                if (childClass == to) {
                    return path.add(classReference);
                } else {
                    if (findClassHierarchy(childClass, to, path, classReference.getGenericResolver())) {
                        path.add(classReference);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean canExplicitCast(HaxeAbstractClassModel from, HaxeClass to, List<SpecificHaxeClassReference> path, HaxeGenericResolver parentResolver) {
        List<SpecificTypeReference> directCastToTypes = from.getDirectCastToTypes(parentResolver);
        for (SpecificTypeReference directCastToType : directCastToTypes) {
            if(directCastToType.getTypePsi() == to) {
                return true;
            }
        }
        return false;
    }

    private static final RecursionGuard<PsiElement> findClassHierarchyRecursionGuard = RecursionManager.createGuard("findClassHierarchyRecursionGuard");

    private static boolean findClassHierarchy(HaxeClass from, HaxeClass to, List<SpecificHaxeClassReference> path, HaxeGenericResolver parentResolver) {
        // stop if "from" is typeParameter without constraints
        Boolean result = findClassHierarchyRecursionGuard.computePreventingRecursion(from, true, () -> {
            if (from instanceof HaxeGenericListPart part && part.getGenericConstraintPart() == null) return false;

            HaxeClassModel fromModel = from.getModel();
            HaxeGenericResolver fromResolver = fromModel.getGenericResolver(parentResolver);
            fromResolver.addAll(parentResolver);
            if (fromModel.isTypedef()) {
                SpecificHaxeClassReference reference = fromModel.getUnderlyingClassReference(fromResolver);
                if (reference != null) {
                    // NOTE: anonymous extendingTypes can extend/ combine multiple definitions
                    // we therefor only add to path when we have found and reached the target (when the recursive calls returns true)
                    SpecificTypeReference resolvedTypeDef = reference.resolveTypeDefOfClassOrTypeParam();
                    if (resolvedTypeDef  instanceof  SpecificHaxeClassReference classReference) {
                        HaxeClass childClass = classReference.getHaxeClass();
//                        if (childClass == to) {
                        if (to.getQualifiedName().equals(childClass.getQualifiedName())) {
                            path.add(reference);
                            return true;
                        }
                        if (childClass != null) {
                            return findClassHierarchy(childClass, to, path, classReference.getGenericResolver());
                        }
                    } else {
                        HaxeClass underlyingClass = reference.getHaxeClass();
//                        if (underlyingClass == to) {
                        if (to.getQualifiedName().equals(underlyingClass.getQualifiedName())) {
                            path.add(reference);
                            return true;
                        } else if (findClassHierarchy(underlyingClass, to, path, reference.getGenericResolver())) {
                            path.add(reference);
                            return true;
                        }
                    }
                }
            }
            // if typeParameter with constraints, treat it as a subtype of that constraint
            if(fromModel instanceof  HaxeGenericParamModel genericParamModel) {
                ResultHolder constraint = genericParamModel.getConstraint(fromResolver);
                if(constraint != null && !constraint.isUnknown()) {
                    SpecificHaxeClassReference constraintClassReference = constraint.getClassType();
                    if(constraintClassReference != null) {
                        HaxeClassModel classModel = constraintClassReference.getHaxeClassModel();
                        if (classModel != null) {
                            HaxeClass childClass = classModel.haxeClass;
//                            if (childClass == to) {
                            if (to.getQualifiedName().equals(childClass.getQualifiedName())) {
                                return path.add(constraintClassReference);
                            } else {
                                if (findClassHierarchy(childClass, to, path, constraintClassReference.getGenericResolver())) {
                                    path.add(constraintClassReference);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            // checking typeParameter constraints (can contain references or its own anonymous structures)
            if (fromModel instanceof HaxeConstraintTypeListModel constraintModel) {
                List<ResultHolder> compositeTypes = constraintModel.getCompositeTypes();
                for (ResultHolder compositeType : compositeTypes) {
                    if (compositeType.getType() instanceof SpecificHaxeClassReference reference) {
                        HaxeClass haxeClass = reference.getHaxeClass();
//                        if (haxeClass == to) {
                        if (to.getQualifiedName().equals(haxeClass.getQualifiedName())) {
                            return path.add(reference);
                        } else {
                            if (findClassHierarchy(haxeClass, to, path, reference.getGenericResolver())) {
                                path.add(reference);
                                return true;
                            }
                        }
                    }
                }

            }

            List<HaxeClassReferenceModel> extendingTypes = fromModel.getExtendingTypes();
            for (HaxeClassReferenceModel model : extendingTypes) {
                HaxeClassModel classModel = model.getHaxeClassModel();
                if (classModel != null) {
                    HaxeClass childClass = classModel.haxeClass;
                    SpecificHaxeClassReference classReference = model.getSpecificHaxeClassReference();
//                    if (childClass == to) {
                    if (to.getQualifiedName().equals(childClass.getQualifiedName())) {
                        return path.add(classReference);
                    } else {
                        if (findClassHierarchy(childClass, to, path, classReference.getGenericResolver())) {
                            path.add(classReference);
                            return true;
                        }
                    }
                }
            }

            List<HaxeClassReferenceModel> interfaces = fromModel.getImplementingInterfaces();
            for (HaxeClassReferenceModel model : interfaces) {
                HaxeClassModel classModel = model.getHaxeClassModel();
                if (classModel != null) {
                    HaxeClass childClass = classModel.haxeClass;
                    SpecificHaxeClassReference classReference = model.getSpecificHaxeClassReference();
                    if (to.getQualifiedName().equals(childClass.getQualifiedName())) {
//                    if (childClass == to) {
                        return path.add(classReference);
                    } else {
                        if (findClassHierarchy(childClass, to, path, classReference.getGenericResolver())) {
                            path.add(classReference);
                            return true;
                        }
                    }
                }
            }
            //experimental fix for typeParam constraint recursion issue
            // makes it so that a path/hierarchy is found so that we can perform
            // SpecificHaxeClassReference#tryCastToClass
            if (to.getModel() instanceof HaxeGenericParamModel genericParamModel) {
                if (genericParamModel.hasConstraint()) {
                    ResultHolder constraint = genericParamModel.getConstraint(null);
                    if (constraint != null) {
                        SpecificHaxeClassReference constraintClassType = constraint.getClassType();
                        if (constraintClassType != null) {
                            HaxeClass haxeClass = constraintClassType.getHaxeClass();
//                            if (from == haxeClass) {
                            if (from.getQualifiedName().equals(haxeClass.getQualifiedName())) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        });

        return result == Boolean.TRUE;
    }

    public static HaxeGenericResolver createInheritedClassResolver(@Nullable HaxeGenericResolver localResolver,
                                                                   @NotNull HaxeClass targetClass,
                                                                   @NotNull HaxeClass sourceClass, List<SpecificHaxeClassReference> path) {

        if(targetClass == sourceClass) return localResolver;
        Collections.reverse(path);

        // typdefs `getMemberResolver` converts resolver to resolver for underlying type
        // while this is useful when resolving for members, it would break our logic here
        // as it would skip one level, so we stick with localResolver in this case
        HaxeGenericResolver resolver = (sourceClass instanceof HaxeTypedefDeclaration)
                ? localResolver
                : sourceClass.getMemberResolver(localResolver);


        if(resolver == null) resolver = new HaxeGenericResolver();
        for (SpecificHaxeClassReference reference : path) {
            ResultHolder resolved = resolver.resolve(reference.createHolder());
            if(resolved != null && resolved.isClassType()) {
                HaxeGenericResolver genericResolver = resolved.getClassType().getGenericResolver();
                genericResolver.setAssignHint(resolver.getAssignHint());
                resolver = genericResolver;
            }
        }

        // todo TranslateAbstractToUnderlying(source);

        return resolver;
    }



    @Nullable
    public static HaxeGenericResolver createExtendingClassResolver(@NotNull HaxeGenericResolver resolver,
                                                                   @NotNull HaxeClass from,
                                                                   @NotNull HaxeClass to, List<SpecificHaxeClassReference> path) {
        if (from == to) return  resolver;

        HaxeClassModel toModel = to.getModel();
        ResultHolder type = toModel.getInstanceType();
        SpecificHaxeClassReference toInstance = type.getClassType();
        if(toInstance == null) return null;

        // TODO write tests to verify  (need 3 or more levels of inheritance to know if first or last)
        path.remove(0);
        path.add(toInstance);

        HaxeGenericResolver mappedResolver = resolver.withoutMethodTypeParameters();
        for (SpecificHaxeClassReference reference : path) {
            HaxeClassModel classModel = reference.getHaxeClassModel();
            if(classModel!= null) {
                HaxeGenericResolver nextResolver = reference.getGenericResolver();
                List<HaxeGenericParamModel> params = classModel.getGenericParams();
                for (HaxeGenericParamModel param : params) {
                    HaxeTypeParameterDeclaration typeParameter = param.getTypeParameter();
                    HaxeClass replaced = param.getReplacedTypeParameter();
                    if (replaced instanceof HaxeTypeParameterDeclaration replacedTypeParameter) {
                        ResultHolder resolve = mappedResolver.resolve(replacedTypeParameter);
                        if (resolve != null) {
                            if (resolve.getClassType() != null &&  resolve.getClassType().getHaxeClass() == replaced) {
                                // if the typeParameter we are updating is pointing to itself we update the type to new typeParameter as well
                                // we dont want to update just typeParameter and keep type as that would cause issues down the line.

                                // TODO  need some deeper research, might check if class is same ?
                                // also do we need to do something regarding constraints  now being lost?
                                nextResolver.add(typeParameter, typeParameter.getModel().getInstanceType());
                            }else {
                                nextResolver.add(typeParameter, resolve);
                            }
                        }
                    }
                }
                mappedResolver = nextResolver;
            }


        }
        return mappedResolver;
    }


    private static @NotNull HaxeClass tryGetConstraintFromTypeParameter(@NotNull HaxeClass source) {
        if (source instanceof HaxeGenericListPart genericListPart) {
            HaxeGenericConstraintPart genericConstraintPart = genericListPart.getGenericConstraintPart();
            if (genericConstraintPart != null) {
                ResultHolder constraint = HaxeTypeResolver.getTypeFromGenericConstraint(genericConstraintPart);
                if (constraint != null && constraint.getClassType() != null) {
                    HaxeClass haxeClass = constraint.getClassType().getHaxeClass();
                    if (haxeClass != null) return  haxeClass;
                }
            }
        }
        return source;
    }

    private static @Nullable HaxeClass findUnderlyingHaxeClass(HaxeAbstractTypeDeclarationImpl abstractTypeDeclaration) {
        HaxeUnderlyingType type = abstractTypeDeclaration.getUnderlyingType();
        if(type != null && type.getTypeOrAnonymous() != null) {
            SpecificTypeReference underlyingType = HaxeTypeResolver.getTypeFromTypeOrAnonymous(type.getTypeOrAnonymous()).getType();
            if(underlyingType instanceof  SpecificHaxeClassReference classReference) {
                return classReference.getHaxeClass();
            }
        }
        return null;
    }

    private static HaxeGenericResolver translateAbstractToUnderlying(@NotNull HaxeGenericResolver resolver, @NotNull HaxeClass source) {
        HaxeGenericResolver newResolver = resolver.withoutClassTypeParameters();
        //TODO mlo: move to its own method and add support for recursion, (underlying type might be another abstract with underlying type)
        // mapping abstract to underlying type
        if (source instanceof HaxeAbstractTypeDeclaration declaration) {
            HaxeUnderlyingType type = declaration.getUnderlyingType();
            if(type != null && type.getTypeOrAnonymous() != null) {
                ResultHolder holder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(type.getTypeOrAnonymous());
                SpecificHaxeClassReference classType = holder.getClassType();
                if(classType != null) {
                    HaxeGenericResolver genericResolver = classType.getGenericResolver();

                    for (@NotNull ResolverEntry entry : genericResolver.entries()) {
                        String lookupName = entry.typeParameter().getName();
                        for (ResolverEntry resolverEntry : resolver.getResolvers()) {
                            if(Objects.equals(resolverEntry.typeParameter().getName(), lookupName)) {
                                newResolver.add(entry.withType(resolverEntry.type()));
                            }
                        }
                    }
                    for (@NotNull ResolverEntry entry : genericResolver.getConstaints()) {
                        String lookupName = entry.typeParameter().getName();
                        for (ResolverEntry constraintEntry : resolver.getConstaints()) {
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

}
