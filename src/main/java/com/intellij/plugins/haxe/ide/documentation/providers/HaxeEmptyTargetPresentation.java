package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.platform.backend.presentation.TargetPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class HaxeEmptyTargetPresentation implements TargetPresentation {
    @Override
    public @Nullable Icon getLocationIcon() {
        return null;
    }

    @Override
    public @Nullable String getLocationText() {
        return "";
    }

    @Override
    public @Nullable TextAttributes getContainerTextAttributes() {
        return null;
    }

    @Override
    public @Nullable String getContainerText() {
        return "";
    }

    @Override
    public @Nullable TextAttributes getPresentableTextAttributes() {
        return null;
    }

    @Override
    public @NotNull String getPresentableText() {
        return "";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public @Nullable Color getBackgroundColor() {
        return null;
    }
}