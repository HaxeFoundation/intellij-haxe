package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.ide.lookup.*;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentBindMethod;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentStringCode;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.plugins.haxe.util.HaxeAbstractForwardUtil;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.wrapTypeInClassOrEnum;

@CustomLog
public class HaxeReferenceSuggestionUtil {

    public static Object[] getVariants(HaxeReferenceImpl haxeReference) {
        List<HaxeLookupElement> variants = new ArrayList<>();

        final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(haxeReference);
        HaxeReference targetReference = leftReference == null ? haxeReference : leftReference;

        PsiElement resolvedPsi = targetReference.resolve();
        ResultHolder resolvedType = HaxeExpressionEvaluator.evaluate(targetReference).result;

        // if we do not have a left reference to resolve we try to use the class that
        // knowing the references parent type is important  in order to check @:forward etc.
        if(leftReference == null) {
            ResultHolder possibleParentType = tryToFindValidParentType(targetReference);
            if(possibleParentType != null) resolvedType = possibleParentType;
        }

        boolean hasProcessedTypeMembers = false;
        boolean isStaticAccess = isStaticAccess(leftReference, resolvedPsi);

        switch (resolvedPsi) {
            case PsiPackage psiPackage -> addPackageSuggestions(variants, psiPackage);
            case HaxeFile haxeFile -> addFileMemberSuggestions(variants, haxeFile);
            case HaxeModule haxeModule -> addModuleMemberSuggestions(variants, haxeModule, false);
            case HaxeClass haxeClass -> {
                HaxeClass refClass =  findContainingClass(targetReference);
                boolean ignorePrivateMembers = haxeClass != refClass;
                boolean hasModuleName = leftReference.textMatches(haxeClass.getName());
                if(isStaticAccess) {
                    addClassStaticMemberSuggestions(variants, haxeClass, ignorePrivateMembers);
                    // ExtensionMethods can accept Class<T> and Enum<T>
                    ResultHolder wrappedType = wrapTypeInClassOrEnum(resolvedPsi, haxeClass);
                    addExtensionMethodSuggestions(variants, wrappedType, targetReference);
                }
                // to avoid ambiguity when it comes to class vs module resolve we add both  when module name and class name are identical.
                if(hasModuleName)addModuleMemberSuggestions(variants, haxeClass.getModule(), true);

            }
            case HaxeMethod haxeMethod -> addMethodBindSuggestion(variants, haxeMethod);
            case HaxePsiField haxeField -> {
                if(resolvedType.isClassType()) {
                    addClassMemberSuggestions(variants, resolvedType, targetReference);
                }else if (resolvedType.isFunctionType()) {
                    addFunctionBindSuggestion(variants, haxeField, resolvedType.getFunctionType(), haxeReference);
                }
                addExtensionMethodSuggestions(variants, resolvedType, targetReference);
                hasProcessedTypeMembers = true;
            }
            case null, default -> {}
        }
        if (!isStaticAccess) {
            boolean skipTypeMembers = switch (targetReference) {
                case HaxeStringLiteralExpression stringLiteral -> {
                    addStringCodeSuggestion(variants, stringLiteral, haxeReference);
                    yield false;
                }
                case HaxeThisExpression thisExpression -> {
                    addThisSuggestions(variants, thisExpression, resolvedType);
                    yield true;
                }
                case HaxeSuperExpression superExpression -> {
                    addSuperSuggestions(variants, superExpression, resolvedType);
                    yield true;
                }
                case HaxeObjectLiteral objectLiteral -> {
                    addObjectLiteralSuggestions(variants, objectLiteral, resolvedType, targetReference);
                    yield true;
                }
                case null, default -> {
                    // handle some special cases
                    if (HaxeAbstractForwardUtil.isElementInForwardMeta(targetReference)) {
                        addAbstractUnderlyingClassSuggestions(variants, targetReference);
                    }
                    yield false;
                }
            };
        if(!skipTypeMembers && !hasProcessedTypeMembers){
            addClassMemberSuggestions(variants, resolvedType, targetReference);
        }
        if(leftReference!= null) {
            if(!hasProcessedTypeMembers) {
                addExtensionMethodSuggestions(variants, resolvedType, targetReference);
            }
        }else {
            // if we do not have leftReference it means the complteion suggestion is a single word
            // and that means we should show anything that can be a valid references, packages, local members etc.
            addRootPackageSuggestions(variants, targetReference);
            addLocalMembersSuggestions(variants, targetReference);
        }
    }

        return variants.toArray();
    }


    private static void addModuleMemberSuggestions(List<HaxeLookupElement> variants, HaxeModule haxeModule, boolean skipMainClass) {
        HaxeModuleModel moduleModel = (HaxeModuleModel)haxeModule.getModel();
        List<HaxeModel> exposedMembers = moduleModel.getExposedMembers();

        Set<HaxeComponentName> suggestedVariants = new HashSet<>();
        for (HaxeModel exposedMember : exposedMembers) {
            PsiElement base = exposedMember.getBasePsi();
            if (base instanceof HaxeNamedComponent haxeNamedComponent) {
                HaxeComponentName componentName = haxeNamedComponent.getComponentName();
                if (skipMainClass && componentName.getName().equals(moduleModel.getName())) continue;
                suggestedVariants.add(componentName);
            }
        }

        variants.addAll(HaxeMemberLookupElement.createModuleMembers(suggestedVariants));

        for (HaxeComponentName suggestedVariant : suggestedVariants) {
            PsiElement parent = suggestedVariant.getParent();
            if (parent instanceof HaxeClass haxeClass) {
                variants.add(new HaxeClassLookupElement(haxeClass, haxeClass.getComponentName()));
            }
        }
    }

    private static void addAbstractUnderlyingClassSuggestions(List<HaxeLookupElement> variants, HaxeReference targetReference) {
        final HaxeMeta meta = HaxeMetadataUtils.getEnclosingMeta(targetReference);
        PsiElement element = HaxeMetadataUtils.getAssociatedElement(meta);
        // TODO mlo : needs a fix: there's a problem with how module element and metadata is parsed some metadata is outside module.
        if (element instanceof  HaxeModule module) {
            element = module.getFirstChild();
        }
        if(element instanceof HaxeClass haxeClass) {
            if (haxeClass == null || !haxeClass.isAbstractType()) return;
            if(haxeClass.getModel() instanceof HaxeAbstractClassModel abstractClassModel) {
                final HaxeClass underlyingClass = abstractClassModel.getUnderlyingClass(null);
                HaxeClassModel underlyingModel = underlyingClass.getModel();
                if(underlyingModel!= null) {
                    SpecificHaxeClassReference instanceReference = underlyingModel.getInstanceReference();
                    ResultHolder holder = instanceReference.createHolder();
                    addClassMemberSuggestions(variants, holder, targetReference, true, true);
                }
            }

        }
    }


    private static ResultHolder tryToFindValidParentType(HaxeReference targetReference) {
        HaxeClass containingClass = findContainingClass(targetReference);
        if(containingClass != null) {
            HaxeClassModel model = containingClass.getModel();
            if(model != null) {
                return model.getInstanceReference().createHolder();
            }
        }
        return null;
    }


    private static void addLocalMembersSuggestions(List<HaxeLookupElement> variants, HaxeReference targetReference) {
        Set<HaxeComponentName> localMembers = new HashSet<>();
        //avoid walking the entire file, addClassMemberSuggestions and addModuleMemberSuggestions should cover these
        HaxeMethod parentMethod = PsiTreeUtil.getParentOfType(targetReference, HaxeMethodDeclaration.class);
        PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(localMembers), targetReference, parentMethod, new ResolveState());
        HaxeClass containingClass = findContainingClass(targetReference);
        localMembers = deduplicateLocalMembers(localMembers);
        if(containingClass != null) {
            SpecificHaxeClassReference instanceReference = containingClass.getModel().getInstanceReference();
            HaxeGenericResolver genericResolver = instanceReference.getGenericResolver();
            variants.addAll(HaxeMemberLookupElement.createLocalMembers(instanceReference, genericResolver, localMembers));
        }else {
            // probably inside a module method
            variants.addAll(HaxeMemberLookupElement.createLocalMembers(null, new HaxeGenericResolver(), localMembers));
        }
    }

    // we don't want to show duplicates when local variables and functions shadows other local members
  private static Set<HaxeComponentName> deduplicateLocalMembers(Set<HaxeComponentName> members) {
      HashMap<String, HaxeComponentName> dedupeMap = new HashMap<>(members.size());
      for (HaxeComponentName member : members) {
          dedupeMap.put(member.getName(), member);
      }
      return Set.copyOf(dedupeMap.values());
  }

    private static void addClassMemberSuggestions( List<HaxeLookupElement> variants, ResultHolder resolvedType, HaxeReference targetReference) {
        if(resolvedType.isClassType()) {
            SpecificHaxeClassReference classType = resolvedType.getClassType();
            HaxeClass haxeClass = classType.getHaxeClass();
            HaxeGenericResolver genericResolver = classType.getGenericResolver();

            boolean ignorePrivateMembers = shouldIgnorePrivateMembers(classType, targetReference);
            Set<HaxeComponentName> nonStaticMembers = findClassNonStaticMembers(haxeClass, targetReference, genericResolver, ignorePrivateMembers);
            variants.addAll(HaxeMemberLookupElement.createClassMembers(classType, genericResolver, nonStaticMembers));
        }
    }

    private static void addClassMemberSuggestions(List<HaxeLookupElement> variants, ResultHolder resolvedType, HaxeReference targetReference,  boolean ignorePrivateMembers, boolean excludeCallSuggestions) {
        if(resolvedType.isClassType()) {
            SpecificHaxeClassReference classType = resolvedType.getClassType();
            HaxeClass haxeClass = classType.getHaxeClass();
            HaxeGenericResolver genericResolver = classType.getGenericResolver();

            Set<HaxeComponentName> nonStaticMembers = findClassNonStaticMembers(haxeClass, targetReference, genericResolver, ignorePrivateMembers);
            variants.addAll(HaxeMemberLookupElement.createClassMembers(classType, genericResolver, nonStaticMembers, true, !excludeCallSuggestions));
        }
    }

    private static boolean shouldIgnorePrivateMembers(SpecificHaxeClassReference classType, HaxeReference targetReference) {
        HaxeClass containingClass = findContainingClass(targetReference);
        if(containingClass == null) return true;
        HaxeClassModel model = containingClass.getModel();
        if(model != null) {
            SpecificTypeReference targetResolved = model.getInstanceReference().fullyResolveTypeDefAndUnwrapNullTypeReference();
            SpecificTypeReference classResolved = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
            return !classType.isSameType(targetResolved);
        }
        return true;
    }

    private static void addStringCodeSuggestion(List<HaxeLookupElement> variants, HaxeStringLiteralExpression stringLiteral, HaxeReferenceImpl haxeReference) {
        if(stringLiteral.getTextLength() == 3) { // 2x quotes + single char
            HaxeIdentifier identifier = PsiTreeUtil.getChildOfType(haxeReference, HaxeIdentifier.class);
            HaxeFakeComponentStringCode bind = new HaxeFakeComponentStringCode(identifier);
            variants.add(HaxeSynteticLookupElements.code(bind));
        }
    }

    private static void addMethodBindSuggestion(List<HaxeLookupElement> variants, HaxeMethod method) {
            HaxeComponentName componentName = method.getComponentName();
            HaxeIdentifier identifier = componentName.getIdentifier();
            HaxeFakeComponentBindMethod bind = new HaxeFakeComponentBindMethod(identifier, method);
            variants.add(HaxeSynteticLookupElements.bind(bind));
    }
    private static void addFunctionBindSuggestion(List<HaxeLookupElement> variants, HaxePsiField haxeField, @NotNull SpecificFunctionReference functionReference, HaxeReferenceImpl haxeReference) {

        PsiElement elementContext = functionReference.getElementContext();
        if(elementContext instanceof HaxeMethodDeclaration method) {
            HaxeComponentName componentName = method.getComponentName();
            HaxeIdentifier identifier = componentName.getIdentifier();
            HaxeFakeComponentBindMethod bind = new HaxeFakeComponentBindMethod(identifier, method);
            variants.add(HaxeSynteticLookupElements.bind(bind));
        } else {
            HaxeIdentifier identifier = haxeField.getComponentName().getIdentifier();
            HaxeFakeComponentBindMethod bind = new HaxeFakeComponentBindMethod(identifier, haxeField);
            variants.add(HaxeSynteticLookupElements.bind(bind));
        }
    }


    private static void addObjectLiteralSuggestions(List<HaxeLookupElement> variants, HaxeObjectLiteral objectLiteral, ResultHolder resolvedType, HaxeReference targetReference) {

        SpecificHaxeClassReference classType = resolvedType.getClassType();
        HaxeClass haxeClass = classType.getHaxeClass();
        HaxeGenericResolver genericResolver = classType.getGenericResolver();

        Set<HaxeComponentName> nonStaticMembers = findClassNonStaticMembers(haxeClass, targetReference, genericResolver, false);
        variants.addAll(HaxeMemberLookupElement.createClassMembers(classType, genericResolver, nonStaticMembers));
    }

    private static void addSuperSuggestions(List<HaxeLookupElement> variants, HaxeSuperExpression superExpression, ResultHolder resolvedType) {
        SpecificHaxeClassReference superClassRef = resolvedType.getClassType();
        HaxeGenericResolver genericResolver = superClassRef.getGenericResolver();
        HaxeClass superClass = superClassRef.getHaxeClass();

        Set<HaxeComponentName> nonStaticMembers = findClassNonStaticMembers(superClass, superExpression, genericResolver, false);
        SpecificHaxeClassReference instanceReference = superClass.getModel().getInstanceReference();

        variants.addAll(HaxeMemberLookupElement.createClassMembers(instanceReference, genericResolver, nonStaticMembers));

    }

    private static void addThisSuggestions(List<HaxeLookupElement> variants, HaxeThisExpression thisExpression, ResultHolder resolvedType) {
        SpecificHaxeClassReference currentClass = resolvedType.getClassType();
        if (currentClass == null || currentClass.getHaxeClass() == null) return;
        HaxeClass haxeClass = currentClass.getHaxeClass();
        HaxeGenericResolver genericResolver = currentClass.getGenericResolver();
        Set<HaxeComponentName> nonStaticMembers = findClassNonStaticMembers(haxeClass, thisExpression, genericResolver, false);
        variants.addAll(HaxeMemberLookupElement.createClassMembers(currentClass, genericResolver, nonStaticMembers));
    }

    private static void addExtensionMethodSuggestions(List<HaxeLookupElement> variants, ResultHolder holder, HaxeReference targetReference) {
        SpecificTypeReference specificTypeReference = holder.getType();
        HaxeGenericResolver genericResolver = specificTypeReference instanceof  SpecificHaxeClassReference classReference
                ? classReference.getGenericResolver() :  new HaxeGenericResolver();

        Set<HaxeComponentName> usingVariants = findUsingVariants(specificTypeReference, targetReference);
        variants.addAll(HaxeMemberLookupElement.createExtensionMembers(specificTypeReference, genericResolver, usingVariants));
    }

    private static void addClassStaticMemberSuggestions(List<HaxeLookupElement> variants, HaxeClass haxeClass, boolean ignorePrivateMembers) {
        final boolean isEnum = haxeClass.isEnum();

        Set<HaxeComponentName> staticMembers = new HashSet<>();
        for (HaxeBaseMemberModel member : haxeClass.getModel().getMembersSelf()) {
            if (member instanceof HaxeMemberModel memberModel) {
                if (isEnum && member instanceof HaxeEnumValueModel || memberModel.isStatic()) {
                    if (!ignorePrivateMembers || memberModel.isPublic()) {
                        HaxeComponentName psi = member.getNamePsi();
                        staticMembers.add(psi);
                    }
                }
            }
        }
        SpecificHaxeClassReference instanceReference = haxeClass.getModel().getInstanceReference();
        HaxeGenericResolver genericResolver = instanceReference.getGenericResolver();
        variants.addAll(HaxeMemberLookupElement.createExtensionMembers(instanceReference, genericResolver, staticMembers));
    }

    private static void addPackageSuggestions(List<HaxeLookupElement> variants, PsiPackage psiPackage) {
        variants.addAll(HaxePackageLookupElement.convert(psiPackage.getSubPackages()));
    }

    private static void addRootPackageSuggestions(List<HaxeLookupElement> variants, HaxeReference targetReference) {
        PsiPackage rootPackage = JavaPsiFacade.getInstance(targetReference.getProject()).findPackage("");
        variants.addAll(HaxePackageLookupElement.convert(rootPackage.getSubPackages()));

    }

    private static void addFileMemberSuggestions(List<HaxeLookupElement> variants, HaxeFile haxeFile) {
        variants.addAll(HaxeClassLookupElement.convert(haxeFile.getClasses()));
    }

    private static Set<HaxeComponentName> findUsingVariants(SpecificTypeReference typeReference, HaxeReference reference) {

        Set<HaxeComponentName> variants = new HashSet<>();
        HaxeFileModel haxeFileModel = HaxeFileModel.fromElement(reference);
        if (haxeFileModel != null) {

            List<HaxeUsingModel> importHxUsingModels = findImportHxFileUsingModels(haxeFileModel);
            for (HaxeUsingModel importHxUsingModel : importHxUsingModels) {
                for (HaxeMethodModel methodModel : importHxUsingModel.getExtensionMethods(typeReference, null)) {
                    HaxeComponentName psi = methodModel.getNamePsi();
                    variants.add(psi);
                }
            }

          for (HaxeUsingModel model : haxeFileModel.getUsingModels()) {
            for (HaxeMethodModel methodModel : model.getExtensionMethods(typeReference, null)) {
              HaxeComponentName psi = methodModel.getNamePsi();
              variants.add(psi);
            }
          }
        }
        if (typeReference instanceof SpecificHaxeClassReference classReference) {
            HaxeClass haxeClass = classReference.getHaxeClass();
            if (haxeClass != null) {
                List<HaxeMethodModel> extensionMethodsFromMeta = haxeClass.getModel().getExtensionMethodsFromMeta();
                for (HaxeMethodModel model : extensionMethodsFromMeta) {
                    HaxeComponentName psi = model.getNamePsi();
                    variants.add(psi);
                }
            }
        }

    return variants;
    }

    private static  @NotNull List<HaxeUsingModel> findImportHxFileUsingModels(HaxeFileModel haxeFileModel) {
        final List<HaxeUsingModel> usingModels = new ArrayList<>();
        HaxeResolveUtil.walkDirectoryImports(haxeFileModel, (importModel) ->{
            usingModels.addAll(importModel.getUsingModels());
            return true;
        });
        return usingModels;
    }

    private static Set<HaxeComponentName> findClassNonStaticMembers(
                                                     @Nullable HaxeClass haxeClass,
                                                     @Nullable HaxeReference reference,
                                                     @Nullable HaxeGenericResolver resolver,
                                                     boolean ignorePrivateMembers) {

        if(haxeClass == null) return Set.of();
        Set<HaxeComponentName> suggestedVariants = new HashSet<>();
        HaxeClassModel classModel = haxeClass.getModel();

        boolean extern = haxeClass.isExtern();
        boolean isAbstractEnum = haxeClass.isAbstractType() && haxeClass.isEnum();
        boolean isAbstractForward = haxeClass.isAbstractType() && ((HaxeAbstractClassModel) classModel).hasForwards();

        if (isAbstractForward) {
            final List<HaxeNamedComponent> forwardingHaxeNamedComponents = HaxeAbstractForwardUtil.findAbstractForwardingNamedSubComponents(haxeClass, resolver);
            if (forwardingHaxeNamedComponents != null) {
                for (HaxeNamedComponent namedComponent : forwardingHaxeNamedComponents) {
                    final boolean needFilter = ignorePrivateMembers && !namedComponent.isPublic();
                    if ((extern || !needFilter) &&
                            !namedComponent.isStatic() &&
                            namedComponent.getComponentName() != null &&
                            !isConstructor(namedComponent)) {
                        suggestedVariants.add(namedComponent.getComponentName());
                    }
                }
            }
        }
        // if type parameter, try to find constraints and use  those ?
        if (haxeClass instanceof HaxeGenericListPart genericType) {
            ResultHolder resolved = resolver.resolveTypeParameter(genericType);
            if (resolved != null && !resolved.isUnknown() && resolved.isClassType()) {
                haxeClass = resolved.getClassType().getHaxeClass();
            } else {
                //TODO fix so it only checks if missing
                HaxeGenericDefaultType defaultType = genericType.getGenericDefaultType();
                if (defaultType != null && defaultType.getTypeOrAnonymous() != null) {
                    ResultHolder holder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(defaultType.getTypeOrAnonymous());
                    if (holder.getClassType() != null) {
                        haxeClass = holder.getClassType().getHaxeClass();
                    }
                }
            }
        }
        List<HaxeNamedComponent> components = HaxeNamedSubComponentUtil.getAllNamedSubComponentsInType(haxeClass, resolver);
        for (HaxeNamedComponent namedComponent : components) {
            final boolean needFilter = ignorePrivateMembers && !namedComponent.isPublic();
            if (isAbstractEnum && HaxeAbstractEnumUtil.couldBeAbstractEnumField(namedComponent)) {
                continue;
            }
            boolean ifEnumIsClassReference = true;
            if (haxeClass.isEnum() && !haxeClass.isAbstractType()) {
                ifEnumIsClassReference = (reference.resolve() instanceof HaxeClass);
            }
            if ((extern || !needFilter) &&
                    !namedComponent.isStatic() &&
                    ifEnumIsClassReference &&
                    namedComponent.getComponentName() != null &&
                    !isConstructor(namedComponent)) {
                suggestedVariants.add(namedComponent.getComponentName());
            }
        }
        return suggestedVariants;
    }


    private static boolean isConstructor(HaxeNamedComponent component) {
        return component instanceof HaxeMethodPsiMixin && ((HaxeMethodPsiMixin)component).isConstructor();
    }
    private static HaxeClass findContainingClass(HaxeReference targetReference) {
        return PsiTreeUtil.getStubOrPsiParentOfType(targetReference, HaxeClass.class);
    }
    private static HaxeMethod findContainingMethod(HaxeReference targetReference) {
        return PsiTreeUtil.getStubOrPsiParentOfType(targetReference, HaxeMethod.class);
    }

    private static boolean isStaticAccess(HaxeReference leftReference, PsiElement resolvedPsi) {
        if (leftReference instanceof HaxeReferenceExpressionImpl callie) {
            if (resolvedPsi instanceof HaxeImportAlias alias) {
                HaxeIdentifier identifier = alias.getIdentifier();
                return callie.getLastChild().textMatches(identifier);
            }
            if (resolvedPsi instanceof HaxeClass haxeClass) {
                String name = haxeClass.getName();
                if (name != null) {
                    return callie.getLastChild().textMatches(name);
                }
            }
            if(resolvedPsi instanceof PsiPackage){
                return true;
            }
        }
        return false;
    }
}
