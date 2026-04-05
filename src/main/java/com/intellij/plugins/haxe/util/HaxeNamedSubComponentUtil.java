package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromGenericConstraint;
import static com.intellij.plugins.haxe.util.HaxeAbstractForwardUtil.getAbstractForwardingFieldsNames;

/**
 * Util class that finds NamedComponents and cache the result where possible
 */
public class HaxeNamedSubComponentUtil {

    private static final RecursionGuard<PsiElement> membersInAnonymousTypeRecursionGuard = RecursionManager.createGuard("membersInAnonymousTypeRecursionGuard");
    private static final RecursionGuard<PsiElement> membersFromClassTypeRecursionGuard = RecursionManager.createGuard("membersFromClassTypeRecursionGuard");


    /**
     * Filters list so that list only contains unique names
     * Note: What components are keep/dropped is indeterminate
     *
     * @param components list of components
     * @return list of uniquely named components
     */
    @NotNull
    public static List<HaxeNamedComponent> uniqueNamedSubComponents(List<HaxeNamedComponent> components) {
        final Map<String, HaxeNamedComponent> result = new HashMap<>();
        for (HaxeNamedComponent haxeNamedComponent : components) {
            if (result.containsKey(haxeNamedComponent.getName())) continue;
            result.put(haxeNamedComponent.getName(), haxeNamedComponent);
        }
        return new ArrayList<>(result.values());
    }

    @NotNull
    public static Map<String, HaxeNamedComponent> namedComponentToMap(@NotNull List<HaxeNamedComponent> unfilteredResult) {
        final Map<String, HaxeNamedComponent> result = new HashMap<String, HaxeNamedComponent>();
        for (HaxeNamedComponent haxeNamedComponent : unfilteredResult) {
            // need order
            if (result.containsKey(haxeNamedComponent.getName())) continue;
            result.put(haxeNamedComponent.getName(), haxeNamedComponent);
        }
        return result;
    }
    @NotNull
    public static List<HaxeNamedComponent> filterNamedComponentsByType(@NotNull List<HaxeNamedComponent> result, final HaxeComponentType type) {
        return ContainerUtil.filter(result, component -> component.getComponentType() == type);
    }


    @NotNull
    public static List<HaxeNamedComponent> getNamedSubComponents(HaxePsiCompositeElement element, boolean includeInherited) {
        if(element instanceof HaxeClass haxeClass) {
        SpecificTypeReference instance = haxeClass.getModel().getInstanceType().getType();
        return getNamedSubComponentsInType(instance, includeInherited);
        }else if (element instanceof HaxeModule haxeModule) {
            return getNamedComponentsInModule(haxeModule);
        }
        return List.of();
    }

    @NotNull
    public static  List<HaxeNamedComponent> getNamedComponentsInModule(@NotNull HaxeModule haxeModule) {
        return  CachedValuesManager.getCachedValue(haxeModule, () -> {
            final HaxeNamedComponent[] namedComponents = PsiTreeUtil.getChildrenOfType(haxeModule, HaxeNamedComponent.class);
            List<HaxeNamedComponent> result = new ArrayList<>();
            if (namedComponents != null) result.addAll(Arrays.asList(namedComponents));
            return new CachedValueProvider.Result<>(result, haxeModule);
        });
    }
    @NotNull
    public static List<HaxeNamedComponent> getNamedSubComponentsInOrder(HaxeClass haxeClass) {
        final List<HaxeNamedComponent> result = HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(haxeClass);
        result.sort(Comparator.comparingInt(PsiElement::getTextOffset));
        return result;
    }

    @NotNull
    public static List<HaxeNamedComponent>getAllNamedSubComponentsFromClassType(@NotNull HaxeClass haxeClass, HaxeComponentType... excludeFilter) {
        SpecificTypeReference instance = haxeClass.getModel().getInstanceType().getType();
        return getNamedSubComponentsInType(instance, true, excludeFilter);
    }
    @NotNull
    public static List<HaxeNamedComponent>getNamedSubComponentsFromClassType(@NotNull HaxeClass haxeClass, HaxeComponentType... excludeFilter) {
        SpecificTypeReference instance = haxeClass.getModel().getInstanceType().getType();
        return getNamedSubComponentsInType(instance, false, excludeFilter);
    }

    @NotNull
    public static List<HaxeNamedComponent>getAllNamedSubComponentsFromClassTypes(@NotNull List<HaxeClass> classList, HaxeComponentType... excludeFilter) {
        List<HaxeNamedComponent> components = new ArrayList<>();
        for (HaxeClass haxeClass : classList) {
            SpecificTypeReference instance = haxeClass.getModel().getInstanceType().getType();
            components.addAll(getNamedSubComponentsInType(instance, true, excludeFilter));
        }
        return components;
    }

    @NotNull
    public static List<HaxeNamedComponent> getAllNamedSubComponentsInType(@NotNull HaxeClass haxeClass, @Nullable HaxeGenericResolver resolver) {
        ResultHolder instance = haxeClass.getModel().getInstanceType();
        if(resolver != null) {
            ResultHolder resolve = resolver.resolve(instance);
            if(resolve!= null && !resolve.isUnknown()) {
                instance = resolve;
            }
        }
        return getNamedSubComponentsInType(instance.getType(), true);
    }
    @NotNull
    public static List<HaxeNamedComponent> getNamedSubComponentsInType(SpecificTypeReference typeReference) {
        return getNamedSubComponentsInType(typeReference, false);
    }

    //Wrapping the result in a collection that can be modified and sorted
    public static List<HaxeNamedComponent> getNamedSubComponentsInType(SpecificTypeReference typeReference, boolean includeInherited, HaxeComponentType... excludeTypes) {
     return new ArrayList<>(_getNamedSubComponentsInType(typeReference, includeInherited,excludeTypes));
    }

    private static List<HaxeNamedComponent> _getNamedSubComponentsInType(SpecificTypeReference typeReference, boolean includeInherited, HaxeComponentType... excludeTypes) {
        // unwrap if null<T>
        if (typeReference instanceof SpecificHaxeClassReference classReference && classReference.isNullType()) {
            typeReference = classReference.unwrapNullType();
        }

        if (typeReference instanceof SpecificHaxeClassReference classReference) {
            final HaxeClass element = classReference.getHaxeClass();

            if (element != null) {
                List<HaxeComponentType> typeFilter = Arrays.asList(excludeTypes);
                if(typeFilter.contains(element.getComponentType())) {
                    return List.of();
                }

                return switch (element) {
                    // types with class body declarations
                    case HaxeClassDeclaration declaration -> getMembersFromClassType(declaration, includeInherited, excludeTypes);
                    case HaxeExternClassDeclaration declaration -> getMembersFromClassType(declaration, includeInherited, excludeTypes);
                    case HaxeInterfaceDeclaration declaration -> getMembersFromClassType(declaration, includeInherited, excludeTypes);
                    case HaxeExternInterfaceDeclaration declaration -> getMembersFromClassType(declaration, includeInherited, excludeTypes);
                    case HaxeEnumDeclaration declaration -> getMembersFromClassType(declaration, includeInherited, excludeTypes);
                    case HaxeAbstractTypeDeclaration declaration -> getMembersFromAbstractType(declaration, true, classReference);

                    // types that inherit their members (should not be cached)
                    case HaxeTypedefDeclaration typedef -> getMembersFromTypeDef(typedef,includeInherited, classReference.getGenericResolver(), excludeTypes);
                    case HaxeGenericListPart listPart -> getMembersFromGenericPart(listPart, classReference.getGenericResolver(), includeInherited);
                    case HaxeGenericConstraintPart constraint -> getMembersFromConstraint(constraint, includeInherited);
                    //NOTE! ConstraintType currently extends AnonymousType so it needs to come before anonymousType
                    case HaxeConstraintTypeList constraint -> getMembersFromConstraintList(constraint, includeInherited);

                    // anonymous types
                    case HaxeObjectLiteral objectLiteral -> getMembersInObjectLiteral(objectLiteral);
                    case HaxeAnonymousType anonymousType -> getMembersInAnonymousType(anonymousType, includeInherited);

                    // anything else?
                    default -> List.of();
                };
            }

        }
        // functionTypes and EnumValues does not have any body
        return List.of();

    }

    private static List<HaxeNamedComponent> getMembersFromGenericPart(HaxeGenericListPart listPart, HaxeGenericResolver resolver, boolean includeInherited) {
        HaxeGenericParamModel model = listPart.getModel();
        if(model.hasConstraint()) {
            ResultHolder constraint = model.getConstraint(resolver);
            if(constraint != null) {
                return getNamedSubComponentsInType(constraint.getType(), includeInherited);
            }
        }
        return List.of();
    }


    @NotNull
    private static List<HaxeNamedComponent> getMembersFromAbstractType(HaxeAbstractTypeDeclaration declaration, boolean includeInherited, SpecificHaxeClassReference classReference) {
        List<HaxeNamedComponent> result = new ArrayList<>(getMembersFromClassType(declaration, includeInherited));
        if(includeInherited) result.addAll(getForwardedMembersForAbstract(declaration, classReference));
        return result;
    }
    @NotNull
    private static  List<HaxeNamedComponent> getForwardedMembersForAbstract(HaxeAbstractTypeDeclaration declaration, SpecificHaxeClassReference classReference) {
        HaxeClassModel model = declaration.getModel();
        if(model instanceof HaxeAbstractClassModel classModel) {
            if(classModel.hasForwards()) {
                HaxeUnderlyingType underlyingType = declaration.getUnderlyingType();
                if(underlyingType != null ) {
                    HaxeTypeOrAnonymous typeOrAnonymous = underlyingType.getTypeOrAnonymous();
                    if(typeOrAnonymous != null) {
                        ResultHolder underlying = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous, classReference.getGenericResolver());
                        List<HaxeNamedComponent> namedSubComponents = getNamedSubComponentsInType(underlying.getType(), true);

                        List<String> fieldNames = getAbstractForwardingFieldsNames(declaration);
                        // null or empty means include all
                        if (fieldNames != null && !fieldNames.isEmpty()) {
                            return namedSubComponents.stream()
                                    .filter(c -> c.getComponentName() != null)
                                    .filter(c -> fieldNames.contains(c.getComponentName().getText()))
                                    .toList();
                        } else {
                            return namedSubComponents;
                        }
                    }
                }
            }
        }
        return List.of();
    }

    @NotNull
    private static List<HaxeNamedComponent> getMembersFromClassType(HaxeClass classType, boolean includeInherited, HaxeComponentType ...fromTypes) {
        // cacheable
        List<HaxeNamedComponent> cacheable =  CachedValuesManager.getCachedValue(classType, () -> {
            HaxePsiCompositeElement body = PsiTreeUtil.getChildOfAnyType(classType, HaxeInterfaceBody.class, HaxeEnumBody.class, HaxeClassBody.class, HaxeExternClassDeclarationBody.class);
            List<HaxeNamedComponent> result = new ArrayList<>();
            if (body != null) {
                final HaxeNamedComponent[] namedComponents = PsiTreeUtil.getChildrenOfType(body, HaxeNamedComponent.class);
                if (namedComponents != null) result.addAll(Arrays.asList(namedComponents));
            }
            return new CachedValueProvider.Result<>(result, classType);
        });

        // nonCacheable
        List<HaxeNamedComponent> nonCacheable = new ArrayList<>();
        if(includeInherited) {
            List<HaxeType> baseTypes = new ArrayList<>();
            baseTypes.addAll(classType.getHaxeExtendsList());
            baseTypes.addAll(classType.getHaxeImplementsList());
            for (HaxeType baseType : baseTypes) {
                List<HaxeNamedComponent> members = membersFromClassTypeRecursionGuard.doPreventingRecursion(baseType, true, () -> {
                    ResultHolder type = HaxeTypeResolver.getTypeFromType(baseType);
                    return getNamedSubComponentsInType(type.getType(), includeInherited, fromTypes);
                });
                if (members != null) nonCacheable.addAll(members);
            }
        }
        List<HaxeNamedComponent> result = new ArrayList<>();
        result.addAll(cacheable);
        result.addAll(nonCacheable);
        return result;
    }

    @NotNull
    private static List<HaxeNamedComponent> getMembersInObjectLiteral(HaxeObjectLiteral objectLiteral) {
        return CachedValuesManager.getCachedValue(objectLiteral, () -> {
            List<HaxeObjectLiteralElement> list = objectLiteral.getObjectLiteralElementList();
            List<HaxeNamedComponent> result = new ArrayList<>();

            for (HaxeObjectLiteralElement member : list) {
                //filter members that do not have getComponentName (happens when  object literal has members with string identifiers that are not "valid")
                if (member.getComponentName() != null) {
                    result.add(member);
                }
            }
            return new CachedValueProvider.Result<>(result, objectLiteral);
        });
    }


    @NotNull
    private static List<HaxeNamedComponent> getMembersInAnonymousType(HaxeAnonymousType anonymousType, boolean includeInherited) {
        List<HaxeAnonymousTypeBody> list = anonymousType.getAnonymousTypeBodyList();
        List<HaxeNamedComponent> cacheable = new ArrayList<>();

        // normal declarations (cacheable)

        for (HaxeAnonymousTypeBody anonymousTypeBody : list) {
            cacheable.addAll(CachedValuesManager.getCachedValue(anonymousTypeBody, () -> {
                List<HaxeNamedComponent> result = new ArrayList<>();
                result.addAll(anonymousTypeBody.getMethodDeclarationList());
                result.addAll(anonymousTypeBody.getFieldDeclarationList());
                result.addAll(anonymousTypeBody.getOptionalFieldDeclarationList());
                //
                HaxeAnonymousTypeFieldList fieldList = anonymousTypeBody.getAnonymousTypeFieldList();
                if (fieldList != null) result.addAll(fieldList.getAnonymousTypeFieldList());

                return new CachedValueProvider.Result<>(result, anonymousTypeBody);
            }));
        }


        List<HaxeNamedComponent> nonCacheable = new ArrayList<>();
        // inherited declarations (non-cacheable)
        if (includeInherited) {
            // haxe 3 style inheritance ({> extendingType})
            for (HaxeAnonymousTypeBody anonymousTypeBody : list) {
                HaxeTypeExtendsList typeExtendsList = anonymousTypeBody.getTypeExtendsList();
                if (typeExtendsList != null) {
                    // Note these can only be structs so it should not be necessary to use generic resolver
                    List<HaxeType> typeList = typeExtendsList.getTypeList();
                    for (HaxeType haxeType : typeList) {
                        List<HaxeNamedComponent> members = membersInAnonymousTypeRecursionGuard.doPreventingRecursion(haxeType, true, () -> {
                            ResultHolder type = HaxeTypeResolver.getTypeFromType(haxeType);
                            return getNamedSubComponentsInType(type.getType(), includeInherited);
                        });
                        if (members != null) nonCacheable.addAll(members);
                    }
                }
            }
            // haxe 4 inheritance ({typeA & TypeB})
            for (HaxeType haxeType : anonymousType.getTypeList()) {
                List<HaxeNamedComponent> members = membersInAnonymousTypeRecursionGuard.doPreventingRecursion(haxeType, true, () -> {
                    ResultHolder type = HaxeTypeResolver.getTypeFromType(haxeType);
                    return getNamedSubComponentsInType(type.getType(), includeInherited);
                });
                if (members != null) nonCacheable.addAll(members);
            }

        }
        List<HaxeNamedComponent> result = new ArrayList<>();
        result.addAll(cacheable);
        result.addAll(nonCacheable);
        return result;
    }

    // non-cacheable (typdef only inherit members)
    @NotNull
    private static List<HaxeNamedComponent> getMembersFromTypeDef(HaxeTypedefDeclaration typedef, boolean includeInherited, HaxeGenericResolver resolver, HaxeComponentType ...fromTypes) {
        final HaxeTypeOrAnonymous typeOrAnonymous = typedef.getTypeOrAnonymous();
        if (typeOrAnonymous != null) {
            ResultHolder type = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous, resolver);
            if (!type.isUnknown()) {
                return getNamedSubComponentsInType(type.getType(), includeInherited, fromTypes);
            }
        }
        return List.of();
    }

    @NotNull
    private static List<HaxeNamedComponent> getMembersFromConstraint(HaxeGenericConstraintPart constraint, boolean includeInherited) {
        // TODO recursion guard ?
        ResultHolder typeFromGenericConstraint = getTypeFromGenericConstraint(constraint);
        if(typeFromGenericConstraint != null && !typeFromGenericConstraint.isUnknown()) {
            return getNamedSubComponentsInType(typeFromGenericConstraint.getType(), includeInherited);
        }
        return List.of();
    }

    @NotNull
    private static List<HaxeNamedComponent> getMembersFromConstraintList(HaxeConstraintTypeList constraint, boolean includeInherited) {
        List<HaxeTypeListPart> typeList = constraint.getConstraintTypes();
        List<HaxeNamedComponent> result = new ArrayList<>();

        for (HaxeTypeListPart haxeTypeListPart : typeList) {
            HaxeTypeOrAnonymous typeOrAnonymous = haxeTypeListPart.getTypeOrAnonymous();
            if (typeOrAnonymous != null) {
                ResultHolder constraintType = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous);
                if(!constraintType.isUnknown()) {
                    result.addAll(getNamedSubComponentsInType(constraintType.getType(), includeInherited));
                }
            }
        }
        return result;
    }
}
