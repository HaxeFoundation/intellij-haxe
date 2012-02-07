package com.intellij.plugins.haxe.config.sdk.ui;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class NekoConfigurablePanel {
  private TextFieldWithBrowseButton myNekoTextField;
  private JPanel myPanel;

  public JComponent getPanel() {
    return myPanel;
  }

  public void setNekoBinPath(String path) {
    myNekoTextField.setText(path);
  }

  public String getNekoBinPath() {
    return myNekoTextField.getText();
  }
}
