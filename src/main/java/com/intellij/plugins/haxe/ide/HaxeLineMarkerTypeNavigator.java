
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.navigation.PsiTargetNavigator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeInheritanceDefinitionsUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.*;

@CustomLog
abstract class HaxeLineMarkerTypeNavigator implements GutterIconNavigationHandler<PsiElement> {

    protected final HaxeComponentType componentType;
    protected final String componentName;
    protected final boolean searchSuper;
    protected final boolean searchSub;
    private final boolean typeDefs;

    public HaxeLineMarkerTypeNavigator(HaxeComponentType componentType, String componentName, boolean searchSuper, boolean searchSub, boolean typeDefs) {
        super();
        this.componentType = componentType;
        this.componentName = componentName;
        this.searchSuper = searchSuper;
        this.searchSub = searchSub;
        this.typeDefs = typeDefs;
    }

    @Override
    public void navigate(MouseEvent event, PsiElement element) {

        HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeClass.class);
        if (haxeClass == null) {
            log.warn("No HaxeClass for navigation");
            return;
        }

        Project project = element.getProject();
        List<HaxeNamedComponent> relatedComponents = findRelatedComponents(haxeClass);

        int itemCount = relatedComponents.size();

        NavigatablePsiElement[] psiElements = mapToNavigablePsiArray(relatedComponents);
        String popupTitle = getPopupTitle(itemCount);
        String tabTitle = getTabTitle();

        new PsiTargetNavigator<>(psiElements)
                .tabTitle(tabTitle)
                .navigate(event, popupTitle, project);
    }


    @NotNull
    protected abstract String getTabTitle();

    @Nls
    @NotNull
    protected abstract String getPopupTitle(int itemCount);

    @NotNull
    private List<HaxeNamedComponent> findRelatedComponents(HaxeClass haxeClass) {
        List<HaxeNamedComponent> relatedComponents = new ArrayList<>();
        if (!typeDefs) {
            if (searchSub) relatedComponents.addAll(findRelatedSubTypes(haxeClass));
            if (searchSuper) relatedComponents.addAll(findRelatedSuperTypes(haxeClass));
        }else {
            if (searchSub) relatedComponents.addAll(findTypedefSource(haxeClass));
            if (searchSuper) relatedComponents.addAll(findTypedefTarget(haxeClass));
        }

        return relatedComponents;
    }

    @NotNull
    private static NavigatablePsiElement[] mapToNavigablePsiArray(List<HaxeNamedComponent> relatedComponents) {
        return HaxeResolveUtil.getComponentNames(relatedComponents).toArray(NavigatablePsiElement[]::new);
    }

    @NotNull
    private static List<HaxeNamedComponent> findRelatedSubTypes(HaxeClass haxeClass) {
        return HaxeResolveUtil.tryResolveClassesByQName(haxeClass.getHaxeExtendsList())
                .stream()
                .map(HaxeNamedComponent.class::cast)
                .toList();
    }

    @NotNull
    private static List<HaxeNamedComponent> findRelatedSuperTypes(HaxeClass haxeClass) {
        Collection<HaxeClass> classCollection = HaxeInheritanceDefinitionsUtil.getItemsByQNameFirstLevelChildrenOnly(haxeClass);
        Set<HaxeClass> classSet = new HashSet<>();
        classCollection.forEach( hx -> {
            classSet.add(hx);
            classSet.addAll(collectSupers(hx));
        });
        return classSet
                .stream()
                .map(HaxeNamedComponent.class::cast)
                .toList();

    }

    private static final RecursionGuard<HaxeClass> collectSupersRecursionGuard = RecursionManager.createGuard("collectSupersRecursionGuard");

    @NotNull
    private static Collection<? extends HaxeClass> collectSupers(HaxeClass hx) {
        List<HaxeClass> result = collectSupersRecursionGuard.doPreventingRecursion(hx, true, () -> {
            List<HaxeClass> hierarchyList = new ArrayList<>();
            Collection<HaxeClass> haxeClasses = HaxeInheritanceDefinitionsUtil.getItemsByQNameFirstLevelChildrenOnly(hx);
            haxeClasses.forEach(aClass -> hierarchyList.addAll(collectSupers(aClass)));
            hierarchyList.addAll(haxeClasses);

            return hierarchyList;
        });

        return result != null ? result : List.of();
    }

    @NotNull
    private static List<HaxeNamedComponent> findTypedefTarget(HaxeClass haxeClass) {
        HaxeClass underlyingType = resolveTypedefTargetClass(haxeClass);
        if(underlyingType == null) return List.of();
        return List.of(underlyingType);
    }
    @NotNull
    private static List<HaxeNamedComponent> findTypedefSource(HaxeClass haxeClass) {
        return HaxeInheritanceDefinitionsUtil.getItemsByQNameFirstLevelChildrenOnly(haxeClass)
                .stream()
                .filter(HaxeTypedefDeclaration.class::isInstance)
                .map(HaxeNamedComponent.class::cast)
                .toList();
    }

    private static @Nullable HaxeClass resolveTypedefTargetClass(HaxeClass haxeClass) {
        if( haxeClass instanceof HaxeTypedefDeclaration typedefDeclaration) {
            HaxeTypeOrAnonymous typeOrAnonymous = typedefDeclaration.getTypeOrAnonymous();
            if (typeOrAnonymous != null) {
                ResultHolder typeFromTypeOrAnonymous = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous);
                SpecificHaxeClassReference classType = typeFromTypeOrAnonymous.getClassType();
                if(classType != null ) {
                    return classType.getHaxeClass();
                }
            }
        }
        return null;
    }


}
