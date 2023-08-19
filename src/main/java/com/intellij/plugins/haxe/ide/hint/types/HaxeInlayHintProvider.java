package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class HaxeInlayHintProvider implements InlayHintsProvider<NoSettings> {

  private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("RelatedProblems");

  @NotNull
  @Override
  public SettingsKey<NoSettings> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public String getPreviewText() {
    return null;
  }

  @NotNull
  @Override
  public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
    return new ImmediateConfigurable() {
      @NotNull
      @Override
      public JComponent createComponent(@NotNull ChangeListener listener) {
        JPanel panel = new JPanel();
        panel.setVisible(false);
        return panel;
      }
    };
  }


  @NotNull
  @Override
  public NoSettings createSettings() {
    return new NoSettings();
  }

}
