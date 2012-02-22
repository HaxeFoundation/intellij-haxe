package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.psi.PsiFile;
import com.intellij.ui.RawCommandLineEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeConfigurationEditor {
  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myMainClassFieldWithButton;
  private RawCommandLineEditor myAppArguments;
  private JComboBox myTargetComboBox;
  private JCheckBox myExcludeFromCompilationCheckBox;
  private JLabel myTargetLabel;
  private JLabel myMainClassLabel;
  private JLabel myParametersLabel;

  private final Module module;

  public HaxeConfigurationEditor(Module module) {
    this.module = module;
    addActionListeners();

    myMainClassLabel.setLabelFor(myMainClassFieldWithButton.getTextField());
    myParametersLabel.setLabelFor(myAppArguments.getTextField());
  }

  private void addActionListeners() {
    myMainClassFieldWithButton.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TreeFileChooser fileChooser = TreeFileChooserFactory.getInstance(module.getProject()).createFileChooser(
          HaxeBundle.message("choose.haxe.main.class"),
          null,
          HaxeFileType.HAXE_FILE_TYPE,
          new TreeFileChooser.PsiFileFilter() {
            public boolean accept(PsiFile file) {
              return true;
            }
          });

        fileChooser.showDialog();

        PsiFile selectedFile = fileChooser.getSelectedFile();
        if (selectedFile != null) {
          setChosenFile(selectedFile.getVirtualFile());
        }
      }
    });

    HaxeTarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
  }

  private void setChosenFile(VirtualFile virtualFile) {
    String qualifier = DirectoryIndex.getInstance(module.getProject()).getPackageName(virtualFile.getParent());
    qualifier = qualifier != null && qualifier.length() != 0 ? qualifier + '.' : "";
    myMainClassFieldWithButton.setText(qualifier + FileUtil.getNameWithoutExtension(virtualFile.getName()));
  }

  public boolean isModified() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    assert settings != null;
    final HaxeModuleSettings newSettings = new HaxeModuleSettings(
      myMainClassFieldWithButton.getText(),
      (HaxeTarget)myTargetComboBox.getSelectedItem(),
      myAppArguments.getText(),
      myExcludeFromCompilationCheckBox.isSelected()
    );
    return !settings.equals(newSettings);
  }

  public void reset() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    assert settings != null;
    myMainClassFieldWithButton.setText(settings.getMainClass());
    myAppArguments.setText(settings.getArguments());
    myTargetComboBox.setSelectedItem(settings.getTarget());
    myExcludeFromCompilationCheckBox.setSelected(settings.isExcludeFromCompilation());
  }

  public void apply() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    assert settings != null;
    settings.setMainClass(myMainClassFieldWithButton.getText());
    settings.setArguments(myAppArguments.getText());
    settings.setTarget((HaxeTarget)myTargetComboBox.getSelectedItem());
    settings.setExcludeFromCompilation(myExcludeFromCompilationCheckBox.isSelected());
  }

  public JComponent getMainPanel() {
    return myMainPanel;
  }
}
