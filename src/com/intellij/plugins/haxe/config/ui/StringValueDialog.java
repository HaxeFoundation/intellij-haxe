package com.intellij.plugins.haxe.config.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author: Fedor.Korotkov
 */
public class StringValueDialog extends DialogWrapper {
  private JTextField myTextField;
  private JPanel myMainPanel;

  public StringValueDialog(@NotNull Component parent, boolean canBeParent) {
    super(parent, canBeParent);

    setTitle(HaxeBundle.message("haxe.conditional.compilation.title"));

    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  public String getStringValue() {
    return myTextField.getText();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTextField;
  }
}
