package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
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
  private JTextField myFileNameTextField;
  private TextFieldWithBrowseButton myFolderTextField;
  private JLabel myFolderLabel;

  private final Module myModule;
  private final CompilerModuleExtension myExtension;

  public HaxeConfigurationEditor(Module module, CompilerModuleExtension extension) {
    myModule = module;
    myExtension = extension;
    addActionListeners();

    myMainClassLabel.setLabelFor(myMainClassFieldWithButton.getTextField());
    myParametersLabel.setLabelFor(myAppArguments.getTextField());
    myFolderLabel.setLabelFor(myFolderTextField.getTextField());
  }

  private void addActionListeners() {
    myMainClassFieldWithButton.getButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TreeFileChooser fileChooser = TreeFileChooserFactory.getInstance(myModule.getProject()).createFileChooser(
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

    myFolderTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final VirtualFile folder =
          FileChooser.chooseFile(myModule.getProject(), FileChooserDescriptorFactory.createSingleFolderDescriptor());
        if (folder != null) {
          myFolderTextField.setText(folder.getPresentableUrl());
        }
      }
    });

    myTargetComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myFileNameTextField.getText().isEmpty()) {
          myFileNameTextField.setText(getCurrentExtension(FileUtil.getNameWithoutExtension(myFileNameTextField.getText())));
        }
      }
    });

    HaxeTarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
  }

  private void setChosenFile(VirtualFile virtualFile) {
    String qualifier = DirectoryIndex.getInstance(myModule.getProject()).getPackageName(virtualFile.getParent());
    qualifier = qualifier != null && qualifier.length() != 0 ? qualifier + '.' : "";
    myMainClassFieldWithButton.setText(qualifier + FileUtil.getNameWithoutExtension(virtualFile.getName()));

    if (myFileNameTextField.getText().isEmpty()) {
      myFileNameTextField.setText(getCurrentExtension(FileUtil.getNameWithoutExtension(virtualFile.getName())));
    }
  }

  private String getCurrentExtension(String fileName) {
    final HaxeTarget target = (HaxeTarget)myTargetComboBox.getSelectedItem();
    if (target != null) {
      return target.getTargetFileNameWithExtension(fileName);
    }
    return fileName;
  }

  public boolean isModified() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    final HaxeModuleSettings newSettings = new HaxeModuleSettings(
      myMainClassFieldWithButton.getText(),
      (HaxeTarget)myTargetComboBox.getSelectedItem(),
      myAppArguments.getText(),
      myExcludeFromCompilationCheckBox.isSelected(),
      myFileNameTextField.getText()
    );
    final String url = myExtension.getCompilerOutputUrl();
    final String urlCandidate = VfsUtil.pathToUrl(myFolderTextField.getText());

    return !settings.equals(newSettings) || !urlCandidate.equals(url);
  }

  public void reset() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    myMainClassFieldWithButton.setText(settings.getMainClass());
    myAppArguments.setText(settings.getArguments());
    myTargetComboBox.setSelectedItem(settings.getTarget());
    myExcludeFromCompilationCheckBox.setSelected(settings.isExcludeFromCompilation());
    myFileNameTextField.setText(settings.getOutputFileName());

    final String url = myExtension.getCompilerOutputUrl();
    myFolderTextField.setText(VfsUtil.urlToPath(url));
  }

  public void apply() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    settings.setMainClass(myMainClassFieldWithButton.getText());
    settings.setArguments(myAppArguments.getText());
    settings.setTarget((HaxeTarget)myTargetComboBox.getSelectedItem());
    settings.setExcludeFromCompilation(myExcludeFromCompilationCheckBox.isSelected());
    settings.setOutputFileName(myFileNameTextField.getText());

    final String url = myExtension.getCompilerOutputUrl();
    final String urlCandidate = VfsUtil.pathToUrl(myFolderTextField.getText());

    if (!urlCandidate.equals(url)) {
      myExtension.setCompilerOutputPath(urlCandidate);
      myExtension.commit();
    }
  }

  public JComponent getMainPanel() {
    return myMainPanel;
  }
}
