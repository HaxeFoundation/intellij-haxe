package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.navigation.PsiTargetNavigator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.index.HaxeInheritanceDefinitionsUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.ide.HaxeLineMarkerUtil.componentNameMatches;
import static com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil.uniqueNamedSubComponents;

@CustomLog
abstract class HaxeLineMarkerMemberNavigator implements GutterIconNavigationHandler<PsiElement> {

    protected final HaxeComponentType componentType;
    protected final String componentName;
    protected final boolean searchSuper;
    protected final boolean searchSub;
    private final boolean searchInterfaces;

    public HaxeLineMarkerMemberNavigator(HaxeComponentType componentType, String componentName, boolean searchSuper, boolean searchSub, boolean searchInterfaces) {
        super();
        this.componentType = componentType;
        this.componentName = componentName;
        this.searchSuper = searchSuper;
        this.searchSub = searchSub;
        this.searchInterfaces = searchInterfaces;
    }

    @Override
    public void navigate(MouseEvent event, PsiElement element) {

        HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class);
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


    private @NotNull List<HaxeNamedComponent> findRelatedComponents(HaxeClass haxeClass) {
        List<HaxeNamedComponent> relatedComponents = new ArrayList<>();
        if (searchSub) relatedComponents.addAll(findRelatedSubComponents(haxeClass, componentName));
        if (searchSuper) relatedComponents.addAll(findRelatedSuperComponents(haxeClass, componentName, searchInterfaces));
        return relatedComponents;
    }

    private static NavigatablePsiElement[] mapToNavigablePsiArray(List<HaxeNamedComponent> relatedComponents) {
        return HaxeResolveUtil.getComponentNames(relatedComponents).toArray(NavigatablePsiElement[]::new);
    }

    @NotNull
    private static List<HaxeNamedComponent> findRelatedSubComponents(HaxeClass haxeClass, String componentName) {
        final Collection<HaxeClass> subClasses = HaxeInheritanceDefinitionsUtil.getItemsByQNameFirstLevelChildrenOnly(haxeClass);

        return subClasses.stream()
                .map(HaxeNamedSubComponentUtil::getNamedSubComponentsFromClassType)
                .flatMap(Collection::stream)
                .filter(haxeNamedComponent -> componentNameMatches(haxeNamedComponent, componentName))
                .toList();

    }

    @NotNull
    private static List<HaxeNamedComponent> findRelatedSuperComponents(HaxeClass haxeClass, String componentName, boolean searchInterfaces) {
        boolean componentIsInInterface = haxeClass.getModel().isInterface();
        final List<HaxeClass> supers = new ArrayList<>();
        if(componentIsInInterface) {
            // interfaces can only extend other interfaces so it only makes sense to look at ExtendsList
            supers.addAll(HaxeResolveUtil.tryResolveClassesByQName(haxeClass.getHaxeExtendsList()));
        }else {
            supers.addAll(collectHierarchy(haxeClass, searchInterfaces));
        }

        final List<HaxeNamedComponent> superItems = uniqueNamedSubComponents(HaxeNamedSubComponentUtil.getAllNamedSubComponentsFromClassTypes(supers));
        return superItems.stream()
                .filter(haxeNamedComponent -> componentNameMatches(haxeNamedComponent, componentName))
                .toList();
    }

    private static final RecursionGuard<HaxeClass> hierarchyRecursionGuard = RecursionManager.createGuard("hierarchyRecursionGuard");
    private static @NotNull List<HaxeClass> collectHierarchy(HaxeClass haxeClass, boolean searchInterfaces) {
        List<HaxeClass>  combined = new ArrayList<>();
        combined.addAll(HaxeResolveUtil.tryResolveClassesByQName(searchInterfaces ? haxeClass.getHaxeImplementsList() : haxeClass.getHaxeExtendsList()));

        // search  subclasses
        List<HaxeClass> haxeClasses = HaxeResolveUtil.tryResolveClassesByQName(haxeClass.getHaxeExtendsList());
        for (HaxeClass aClass : haxeClasses) {
            List<HaxeClass> subClasses = hierarchyRecursionGuard.doPreventingRecursion(aClass, true, () -> collectHierarchy(aClass, searchInterfaces));
            if(subClasses!= null)combined.addAll(subClasses);
        }

        return combined;
    }


}
