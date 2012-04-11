package com.intellij.plugins.haxe.config.sdk.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAdditionalConfigurablePanel {
  private TextFieldWithBrowseButton myNekoTextField;
  private JPanel myPanel;
  private JLabel myNekoLabel;
  private TextFieldWithBrowseButton myHaxelibTextField;
  private JLabel myHaxelibLabel;

  public HaxeAdditionalConfigurablePanel() {
    myNekoTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final VirtualFile file = FileChooser.chooseFile(myPanel, new FileChooserDescriptor(true, false, false, false, false, false));
        if (file != null) {
          setNekoBinPath(FileUtil.toSystemIndependentName(file.getPath()));
        }
      }
    });
    myNekoLabel.setLabelFor(myNekoTextField.getTextField());
    myHaxelibTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final VirtualFile file = FileChooser.chooseFile(myPanel, new FileChooserDescriptor(true, false, false, false, false, false));
        if (file != null) {
          setHaxelibPath(FileUtil.toSystemIndependentName(file.getPath()));
        }
      }
    });
    myHaxelibLabel.setLabelFor(myHaxelibTextField.getTextField());
  }

  public JComponent getPanel() {
    return myPanel;
  }

  public void setNekoBinPath(String path) {
    myNekoTextField.setText(path);
  }

  public String getNekoBinPath() {
    return myNekoTextField.getText();
  }

  public void setHaxelibPath(String path) {
    myHaxelibTextField.setText(path);
  }

  public String getHaxelibPath() {
    return myHaxelibTextField.getText();
  }
}
