package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
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
import com.intellij.plugins.haxe.ide.projectStructure.HaxeModuleConfigurationExtensionPoint;
import com.intellij.psi.PsiFile;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
  private JPanel myAdditionalComponentPanel;

  private final Module myModule;
  private final CompilerModuleExtension myExtension;

  private final List<UnnamedConfigurable> cofigurables = new ArrayList<UnnamedConfigurable>();

  public HaxeConfigurationEditor(Module module, CompilerModuleExtension extension) {
    myModule = module;
    myExtension = extension;
    addActionListeners();

    HaxeTarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());

    initExtensions();

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
  }

  private void initExtensions() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    final HaxeModuleConfigurationExtensionPoint[] extensionPoints = HaxeModuleConfigurationExtensionPoint.EP_NAME.getExtensions();

    if (extensionPoints.length > 0) {
      final GridLayoutManager layoutManager = new GridLayoutManager(extensionPoints.length, 1);
      myAdditionalComponentPanel.setLayout(layoutManager);
    }
    for (HaxeModuleConfigurationExtensionPoint extensionPoint : extensionPoints) {
      final GridConstraints gridConstraints = new GridConstraints();
      gridConstraints.setFill(GridConstraints.FILL_HORIZONTAL);

      final UnnamedConfigurable configurable = extensionPoint.createConfigurable(settings);
      cofigurables.add(configurable);
      myAdditionalComponentPanel.add(configurable.createComponent(), gridConstraints);
    }
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
    boolean result = !settings.getMainClass().equals(myMainClassFieldWithButton.getText());
    result = result || settings.getTarget() != myTargetComboBox.getSelectedItem();
    result = result || !settings.getArguments().equals(myAppArguments.getText());
    result = result || (settings.isExcludeFromCompilation() ^ myExcludeFromCompilationCheckBox.isSelected());
    result = result || !settings.getOutputFileName().equals(myFileNameTextField.getText());
    for (UnnamedConfigurable configurable : cofigurables) {
      result = result || configurable.isModified();
    }
    final String url = myExtension.getCompilerOutputUrl();
    final String urlCandidate = VfsUtil.pathToUrl(myFolderTextField.getText());
    result = result || !urlCandidate.equals(url);
    return result;
  }

  public void reset() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    myMainClassFieldWithButton.setText(settings.getMainClass());
    myAppArguments.setText(settings.getArguments());
    myTargetComboBox.setSelectedItem(settings.getTarget());
    myExcludeFromCompilationCheckBox.setSelected(settings.isExcludeFromCompilation());
    myFileNameTextField.setText(settings.getOutputFileName());
    for (UnnamedConfigurable configurable : cofigurables) {
      configurable.reset();
    }

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
    for (UnnamedConfigurable configurable : cofigurables) {
      try {
        configurable.apply();
      }
      catch (ConfigurationException ignored) {
      }
    }

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
