/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.config.sdk.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
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

  private JPanel myCompletionPanel;
  private JLabel myCompletionLabel;
  private JCheckBox myUseCompilerCheckBox;
  private JCheckBox myRemoveDuplicatesCheckbox;
  private JTextArea myNoteWhenUsingCompilerTextArea;

  public HaxeAdditionalConfigurablePanel() {
    myNekoTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        final VirtualFile file = FileChooser.chooseFile(descriptor, myPanel, null, null);
        if (file != null) {
          setNekoBinPath(FileUtil.toSystemIndependentName(file.getPath()));
        }
      }
    });
    myNekoLabel.setLabelFor(myNekoTextField.getTextField());
    myHaxelibTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        final VirtualFile file = FileChooser.chooseFile(descriptor, myPanel, null, null);
        if (file != null) {
          setHaxelibPath(FileUtil.toSystemIndependentName(file.getPath()));
        }
      }
    });
    myHaxelibLabel.setLabelFor(myHaxelibTextField.getTextField());
    myCompletionLabel.setLabelFor(myUseCompilerCheckBox);

    // Text area for the note.
    myNoteWhenUsingCompilerTextArea.setFocusable(false);
    myNoteWhenUsingCompilerTextArea.setOpaque(true);
    myNoteWhenUsingCompilerTextArea.setBorder(BorderFactory.createEmptyBorder());
    myNoteWhenUsingCompilerTextArea.setBackground(new Color(myCompletionLabel.getBackground().getRGB()));
    Font labelFont = myCompletionLabel.getFont();
    myNoteWhenUsingCompilerTextArea.setFont(new Font(labelFont.getName(), labelFont.getStyle(), labelFont.getSize()));
  }

  public JComponent getPanel() {
    return myPanel;
  }

  public void setNekoBinPath(String path) {
    myNekoTextField.setText(FileUtil.toSystemDependentName(path));
  }

  public String getNekoBinPath() {
    return FileUtil.toSystemIndependentName(myNekoTextField.getText());
  }

  public void setHaxelibPath(String path) {
    myHaxelibTextField.setText(FileUtil.toSystemDependentName(path));
  }

  public String getHaxelibPath() {
    return FileUtil.toSystemIndependentName(myHaxelibTextField.getText());
  }

  public void setUseCompilerCompletionFlag(boolean state) {
    myUseCompilerCheckBox.setSelected(state);
  }

  public boolean getUseCompilerCompletionFlag() {
    return myUseCompilerCheckBox.isSelected();
  }

  public void setRemoveCompletionDuplicatesFlag(boolean state) {
    myRemoveDuplicatesCheckbox.setSelected(state);
  }

  public boolean getRemoveCompletionDuplicatesFlag() {
    return myRemoveDuplicatesCheckbox.isSelected();
  }

}
