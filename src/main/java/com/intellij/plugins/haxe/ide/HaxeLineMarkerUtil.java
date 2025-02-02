package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import icons.HaxeIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;


public class HaxeLineMarkerUtil {


    @Nullable
    public static LineMarkerInfo<PsiElement> tryCreateMemberOverrideMarker(final HaxeNamedComponent namedComponent,
                                                                           List<HaxeNamedComponent> superItems) {


        //TODO  support override of fields from interfaces
        if (namedComponent instanceof HaxeMethodDeclaration methodDeclaration) {
            HaxeMethodModel model = methodDeclaration.getModel();
            String methodName = model.getName();

            // ignore constructors
            if (methodName.equals("new")) {
                return null;
            }

            final List<HaxeNamedComponent> filteredSuperItems = ContainerUtil.filter(superItems, item -> componentNameMatches(item, methodName));
            if (filteredSuperItems.isEmpty()) {
                return null;
            }

            boolean fromAbstract = filteredSuperItems.stream()
                    .filter(HaxeMethod.class::isInstance)
                    .map(HaxeMethod.class::cast)
                    .anyMatch(haxeMethod -> haxeMethod.getModel().isAbstract());

            final boolean overrides = model.isOverride();
            final PsiElement element = methodDeclaration.getComponentName().getIdentifier().getFirstChild();
            final Icon icon = overrides ? AllIcons.Gutter.OverridingMethod : AllIcons.Gutter.ImplementingMethod;
            Supplier<String> accessibleNameProvider = () -> overrides ? "Overriding Method" : "Implementing Method";

            HaxeLineMarkerMemberNavigator haxeLineMarkerMemberNavigator = new HaxeLineMarkerMemberNavigator(HaxeComponentType.METHOD, methodName, true, false, !overrides && !fromAbstract) {

                @Override
                protected @NotNull String getTabTitle() {
                    return DaemonBundle.message("navigation.findUsages.title.super.method", componentName);
                }

                @Override
                protected @Nls @NotNull String getPopupTitle(int itemCount) {
                    return DaemonBundle.message("navigation.title.super.method", componentName);
                }
            };

            return new LineMarkerInfo<>(
                    element,
                    element.getTextRange(),
                    icon,
                    psiElement -> overrideTooltipProvider(psiElement, methodName, overrides),
                    haxeLineMarkerMemberNavigator,
                    GutterIconRenderer.Alignment.LEFT,
                    accessibleNameProvider
            );
        }
        return null;
    }

    private static @NotNull String overrideTooltipProvider(PsiElement psiElement, String componentName, boolean overrides) {
        final HaxeClass superHaxeClass = PsiTreeUtil.getParentOfType(psiElement, HaxeClass.class);
        if (superHaxeClass == null) return "null";
        if (overrides) {
            return HaxeBundle.message("overrides.method.in", componentName, superHaxeClass.getQualifiedName());
        }
        return HaxeBundle.message("implements.method.in", componentName, superHaxeClass.getQualifiedName());
    }


    @Nullable
    public static LineMarkerInfo<PsiElement> tryCreateMemberImplementationMarker(final HaxeNamedComponent namedComponent,
                                                                                 List<HaxeNamedComponent> subItems,
                                                                                 final boolean isInterface) {

        //TODO  support override of fields from interfaces
        if (namedComponent instanceof HaxeMethodDeclaration methodDeclaration) {
            HaxeMethodModel model = methodDeclaration.getModel();
            String methodName = model.getName();

            // ignore constructors
            if (methodName.equals("new")) {
                return null;
            }

            final List<HaxeNamedComponent> filteredSubItems = ContainerUtil.filter(subItems, item -> componentNameMatches(item, methodName));
            if (filteredSubItems.isEmpty()) {
                return null;
            }

            int componentCount = filteredSubItems.size();
            final PsiElement element = namedComponent.getComponentName().getIdentifier().getFirstChild();
            Supplier<String> accessibleNameProvider = () -> isInterface ? "Implemented Method" : "Overriden Method";

            HaxeLineMarkerMemberNavigator haxeLineMarkerMemberNavigator = new HaxeLineMarkerMemberNavigator(HaxeComponentType.METHOD, methodName, false, true, isInterface) {

                @Override
                protected @NotNull String getTabTitle() {
                    return "Implementations of " + componentName; // TODO bundle
                }

                @Override
                protected @Nls @NotNull String getPopupTitle(int itemCount) {
                    return isInterface ?
                            DaemonBundle.message("navigation.title.implementation.method", componentName, componentCount) :
                            DaemonBundle.message("navigation.title.overrider.method", componentName, componentCount);
                }
            };

            return new LineMarkerInfo<>(
                    element,
                    element.getTextRange(),
                    isInterface ? AllIcons.Gutter.ImplementedMethod : AllIcons.Gutter.OverridenMethod,
                    element1 -> isInterface
                            ? DaemonBundle.message("method.is.implemented.too.many")
                            : DaemonBundle.message("method.is.overridden.too.many"),
                    haxeLineMarkerMemberNavigator,
                    GutterIconRenderer.Alignment.RIGHT,
                    accessibleNameProvider
            );
        }

        return null;
    }


    @Nullable
    public static LineMarkerInfo<PsiElement> createTypeImplementationMarker(final HaxeClass componentWithDeclarationList,
                                                                            final List<HaxeClass> items) {

        final HaxeComponentName componentName = componentWithDeclarationList.getComponentName();
        if (componentName == null) {
            return null;
        }
        String declarationName = componentWithDeclarationList.getName();
        final PsiElement element = componentName.getIdentifier().getFirstChild();
        Supplier<String> accessibleNameProvider = () -> componentWithDeclarationList instanceof HaxeInterfaceDeclaration ? "Implemented Method" : "Overriden Method";

        HaxeLineMarkerTypeNavigator lineMarkerTypeNavigator = new HaxeLineMarkerTypeNavigator(HaxeComponentType.CLASS, declarationName, true, false, false) {

            @Override
            protected @NotNull String getTabTitle() {
                return "Subclasses of " + componentName; // TODO bundle
            }

            @Override
            protected @Nls @NotNull String getPopupTitle(int itemCount) {
                return DaemonBundle.message("navigation.title.subclass", componentName, itemCount, "");
            }
        };

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                componentWithDeclarationList instanceof HaxeInterfaceDeclaration
                        ? AllIcons.Gutter.ImplementedMethod
                        : AllIcons.Gutter.OverridenMethod,
                item -> DaemonBundle.message("method.is.implemented.too.many"),
                lineMarkerTypeNavigator,
                GutterIconRenderer.Alignment.RIGHT,
                accessibleNameProvider
        );
    }

    @Nullable
    public static LineMarkerInfo<PsiElement> createTypedefMarker(final HaxeClass componentWithDeclarationList, boolean searchSuper) {
        final HaxeComponentName componentName = componentWithDeclarationList.getComponentName();
        if (componentName == null) return null;

        boolean isTypeDef = componentWithDeclarationList.isTypeDef();

        String declarationName = componentWithDeclarationList.getName();
        final PsiElement element = componentName.getIdentifier().getFirstChild();
        Icon icon = isTypeDef ? AllIcons.Gutter.ImplementedMethod : HaxeIcons.TYPEDEF_GUTTER;

        boolean isTypedef = componentWithDeclarationList instanceof HaxeTypedefDeclaration;
        String accessibleNameProviderText = isTypedef ? "Go to Implementation" : "Go to Typedef";
        String tooltipProviderText = isTypeDef ? HaxeBundle.message("haxe.gutter.typedef.implementation") : HaxeBundle.message("haxe.gutter.typedef");

        HaxeLineMarkerTypeNavigator lineMarkerTypeNavigator = new HaxeLineMarkerTypeNavigator(HaxeComponentType.CLASS, declarationName, searchSuper, !searchSuper, true) {

            @Override
            protected @NotNull String getTabTitle() {
                return "Subclasses of " + componentName; // TODO bundle
            }

            @Override
            protected @Nls @NotNull String getPopupTitle(int itemCount) {
                return DaemonBundle.message("navigation.title.subclass", componentName, itemCount, "");
            }
        };

        return new LineMarkerInfo<>(element, element.getTextRange(), icon,
                item -> tooltipProviderText,
                lineMarkerTypeNavigator,
                GutterIconRenderer.Alignment.RIGHT,
                () -> accessibleNameProviderText
        );
    }


    public static boolean componentNameMatches(@NotNull HaxeNamedComponent haxeNamedComponent, @NotNull String componentName) {
        HaxeComponentName psi = haxeNamedComponent.getComponentName();
        return psi != null && psi.textMatches(componentName);
    }
}
