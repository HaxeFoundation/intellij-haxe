package com.intellij.plugins.haxe.ide.editor;

import com.intellij.ide.ui.UISettings;
import com.intellij.lang.Language;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeBreadcrumbsInfoProvider implements BreadcrumbsProvider {

    private static final Language[] LANGUAGES = {HaxeLanguage.INSTANCE};

    @Override
    public Language[] getLanguages() {
        return LANGUAGES;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement element) {
        return switch (element) {
            case HaxeClassDeclaration ignored -> true;
            case HaxeExternClassDeclaration ignored -> true;
            //
            case HaxeInterfaceDeclaration ignored -> true;
            case HaxeExternInterfaceDeclaration ignored -> true;
            //
            case HaxeEnumDeclaration ignored -> true;
            case HaxeAbstractTypeDeclaration ignored -> true;
            case HaxeMethodDeclaration ignored -> true;
            //
            case HaxeLocalFunctionDeclaration ignored -> true;
            case HaxeTypedefDeclaration ignored -> true;
            default -> false;
        };

    }

    @Override
    public @NotNull @NlsSafe String getElementInfo(@NotNull PsiElement element) {
        String name = switch (element) {
            //
            case HaxeClassDeclaration declaration -> declaration.getName();
            case HaxeExternClassDeclaration declaration -> declaration.getName();
            //
            case HaxeInterfaceDeclaration declaration -> declaration.getName();
            case HaxeExternInterfaceDeclaration declaration -> declaration.getName();
            //
            case HaxeEnumDeclaration declaration -> declaration.getName();
            case HaxeAbstractTypeDeclaration declaration -> declaration.getName();
            case HaxeTypedefDeclaration declaration -> declaration.getName();
            //
            case HaxeMethodDeclaration declaration -> declaration.getName();
            case HaxeLocalFunctionDeclaration declaration -> declaration.getName();
            default -> null;
        };
        return name == null ? "<unknown>" : name;
    }

    @Override
    public @Nullable Icon getElementIcon(@NotNull PsiElement element) {
        HaxeComponentType componentType = HaxeComponentType.typeOf(element);
        return componentType == null ? null : componentType.getIcon();
    }

    @Override
    public boolean isShownByDefault() {
        return !UISettings.getInstance().getShowMembersInNavigationBar();
    }
}
