package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
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
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.projectStructure.HaxeModuleConfigurationExtensionPoint;
import com.intellij.psi.PsiFile;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
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
  private JTextField myOutputFileNameTextField;
  private TextFieldWithBrowseButton myFolderTextField;
  private JLabel myFolderLabel;
  private JPanel myAdditionalComponentPanel;
  private TextFieldWithBrowseButton myFileChooserTextField;
  private JBRadioButton myHxmlFileRadioButton;
  private JBRadioButton myNmmlFileRadioButton;
  private JBRadioButton myUserPropertiesRadioButton;
  private JLabel myFileChooserLabel;
  private JPanel myCompilerOptions;
  private JPanel myCommonPanel;
  private JPanel myFileChooserPanel;
  private String customPathToHxmlFile = "";
  private String customPathToNmmlFile = "";

  private HaxeTarget selectedHaxeTarget = HaxeTarget.NEKO;
  private NMETarget selectedNmeTarget = NMETarget.FLASH;

  private final Module myModule;
  private final CompilerModuleExtension myExtension;

  private final List<UnnamedConfigurable> cofigurables = new ArrayList<UnnamedConfigurable>();
  private int compilerOptionsComponentIndex = 0;

  public HaxeConfigurationEditor(Module module, CompilerModuleExtension extension) {
    myModule = module;
    myExtension = extension;
    addActionListeners();

    initExtensions();

    myMainClassLabel.setLabelFor(myMainClassFieldWithButton.getTextField());
    myParametersLabel.setLabelFor(myAppArguments.getTextField());
    myFolderLabel.setLabelFor(myFolderTextField.getTextField());

    ButtonGroup group = new ButtonGroup();
    group.add(myHxmlFileRadioButton);
    group.add(myNmmlFileRadioButton);
    group.add(myUserPropertiesRadioButton);
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
          myFolderTextField.setText(FileUtil.toSystemDependentName(folder.getPath()));
        }
      }
    });

    myTargetComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myOutputFileNameTextField.getText().isEmpty()) {
          myOutputFileNameTextField.setText(getCurrentExtension(FileUtil.getNameWithoutExtension(myOutputFileNameTextField.getText())));
        }
        if (myTargetComboBox.getSelectedItem() instanceof HaxeTarget) {
          selectedHaxeTarget = (HaxeTarget)myTargetComboBox.getSelectedItem();
        }
        if (myTargetComboBox.getSelectedItem() instanceof NMETarget) {
          selectedNmeTarget = (NMETarget)myTargetComboBox.getSelectedItem();
        }
      }
    });

    final ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!myHxmlFileRadioButton.isSelected()) {
          customPathToHxmlFile = FileUtil.toSystemIndependentName(myFileChooserTextField.getText());
        }
        if (!myNmmlFileRadioButton.isSelected()) {
          customPathToNmmlFile = FileUtil.toSystemIndependentName(myFileChooserTextField.getText());
        }
        updateComponents();
        updateTargetCombo();
      }
    };
    myHxmlFileRadioButton.addActionListener(listener);
    myNmmlFileRadioButton.addActionListener(listener);
    myUserPropertiesRadioButton.addActionListener(listener);

    myFileChooserTextField.getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final VirtualFile moduleFile = myModule.getModuleFile();
        assert moduleFile != null;
        final boolean isNMML = myNmmlFileRadioButton.isSelected();
        final VirtualFile file = FileChooser.chooseFile(getMainPanel(), new FileChooserDescriptor(true, false, false, true, false, false) {
          public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {

            return super.isFileVisible(file, showHiddenFiles) &&
                   (file.isDirectory() || (isNMML ? "nmml" : "hxml").equalsIgnoreCase(file.getExtension()));
          }
        }, moduleFile.getParent());
        if (file != null) {
          if (isNMML) {
            customPathToNmmlFile = FileUtil.toSystemIndependentName(file.getPath());
          }
          else {
            customPathToHxmlFile = FileUtil.toSystemIndependentName(file.getPath());
          }
          updateComponents();
        }
      }
    });
  }

  private void updateComponents() {
    final String fileName = myNmmlFileRadioButton.isSelected() ?
                            customPathToNmmlFile :
                            myUserPropertiesRadioButton.isSelected() ? "" : customPathToHxmlFile;
    myFileChooserTextField.setText(FileUtil.toSystemDependentName(fileName));
    myFileChooserTextField.setEnabled(!myUserPropertiesRadioButton.isSelected());

    myFileChooserLabel.setText(myNmmlFileRadioButton.isSelected()
                               ? HaxeBundle.message("haxe.configuration.nmml.file")
                               : HaxeBundle.message("haxe.configuration.hxml.file"));

    updateUserProperties();
    updateFileChooser();
  }

  private void updateFileChooser() {
    boolean contains = false;
    Component[] components = myCommonPanel.getComponents();
    for (Component component : components) {
      if (component == myFileChooserPanel) {
        contains = true;
        break;
      }
    }
    if (!myUserPropertiesRadioButton.isSelected() && !contains) {
      final GridConstraints constraints = new GridConstraints();
      constraints.setRow(1);
      constraints.setFill(GridConstraints.FILL_HORIZONTAL);
      myCommonPanel.add(myFileChooserPanel, constraints);
    }
    else if (myUserPropertiesRadioButton.isSelected() && contains) {
      myCommonPanel.remove(myFileChooserPanel);
    }
  }

  private void updateUserProperties() {
    boolean contains = false;
    Component[] components = myMainPanel.getComponents();
    for (Component component : components) {
      if (component == myCompilerOptions) {
        contains = true;
        break;
      }
    }
    if (myUserPropertiesRadioButton.isSelected() && !contains) {
      final GridConstraints constraints = new GridConstraints();
      constraints.setRow(2);
      constraints.setFill(GridConstraints.FILL_HORIZONTAL);
      myMainPanel.add(myCompilerOptions, constraints);
    }
    else if (!myUserPropertiesRadioButton.isSelected() && contains) {
      myMainPanel.remove(myCompilerOptions);
    }
  }

  private void updateTargetCombo() {
    ((DefaultComboBoxModel)myTargetComboBox.getModel()).removeAllElements();
    if (myNmmlFileRadioButton.isSelected()) {
      NMETarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
      myTargetComboBox.setSelectedItem(selectedNmeTarget);
    }
    else {
      HaxeTarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
      myTargetComboBox.setSelectedItem(selectedHaxeTarget);
    }
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

    if (myFileChooserTextField.getText().isEmpty()) {
      myFileChooserTextField.setText(getCurrentExtension(FileUtil.getNameWithoutExtension(virtualFile.getName())));
    }
  }

  private String getCurrentExtension(String fileName) {
    if (selectedHaxeTarget != null) {
      return selectedHaxeTarget.getTargetFileNameWithExtension(fileName);
    }
    return fileName;
  }

  public boolean isModified() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;

    final String url = myExtension.getCompilerOutputUrl();
    final String urlCandidate = VfsUtil.pathToUrl(myFolderTextField.getText());
    boolean result = !urlCandidate.equals(url);

    result = result || settings.getNmeTarget() != selectedNmeTarget;
    if (myNmmlFileRadioButton.isSelected()) {
      result = result || !FileUtil.toSystemIndependentName(myFileChooserTextField.getText()).equals(settings.getNmmlPath());
    }

    result = result || !settings.getMainClass().equals(myMainClassFieldWithButton.getText());
    result = result || settings.getHaxeTarget() != selectedHaxeTarget;

    if (myHxmlFileRadioButton.isSelected()) {
      result = result || !FileUtil.toSystemIndependentName(myFileChooserTextField.getText()).equals(settings.getHxmlPath());
    }
    result = result || !settings.getArguments().equals(myAppArguments.getText());
    result = result || (settings.isExcludeFromCompilation() ^ myExcludeFromCompilationCheckBox.isSelected());
    result = result || !settings.getOutputFileName().equals(myOutputFileNameTextField.getText());

    result = result || getCurrentBuildConfig() != settings.getBuildConfig();

    for (UnnamedConfigurable configurable : cofigurables) {
      result = result || configurable.isModified();
    }

    return result;
  }

  public void reset() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    myMainClassFieldWithButton.setText(settings.getMainClass());
    myAppArguments.setText(settings.getArguments());
    selectedHaxeTarget = settings.getHaxeTarget();
    selectedNmeTarget = settings.getNmeTarget();
    myExcludeFromCompilationCheckBox.setSelected(settings.isExcludeFromCompilation());
    myOutputFileNameTextField.setText(settings.getOutputFileName());
    for (UnnamedConfigurable configurable : cofigurables) {
      configurable.reset();
    }

    final String url = myExtension.getCompilerOutputUrl();
    myFolderTextField.setText(VfsUtil.urlToPath(url));
    customPathToHxmlFile = settings.getHxmlPath();
    customPathToNmmlFile = settings.getNmmlPath();

    myHxmlFileRadioButton.setSelected(settings.isUseHxmlToBuild());
    myNmmlFileRadioButton.setSelected(settings.isUseNmmlToBuild());
    myUserPropertiesRadioButton.setSelected(settings.isUseUserPropertiesToBuild());
    updateComponents();
    updateTargetCombo();
  }

  public void apply() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    settings.setMainClass(myMainClassFieldWithButton.getText());
    settings.setArguments(myAppArguments.getText());
    if (myNmmlFileRadioButton.isSelected()) {
      settings.setNmeTarget((NMETarget)myTargetComboBox.getSelectedItem());
    }
    else {
      settings.setHaxeTarget((HaxeTarget)myTargetComboBox.getSelectedItem());
    }
    settings.setExcludeFromCompilation(myExcludeFromCompilationCheckBox.isSelected());
    settings.setOutputFileName(myOutputFileNameTextField.getText());

    if (myHxmlFileRadioButton.isSelected()) {
      settings.setHxmlPath(FileUtil.toSystemIndependentName(myFileChooserTextField.getText()));
    }

    if (myNmmlFileRadioButton.isSelected()) {
      settings.setNmmlPath(FileUtil.toSystemIndependentName(myFileChooserTextField.getText()));
    }

    settings.setBuildConfig(getCurrentBuildConfig());
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

  private int getCurrentBuildConfig() {
    int buildConfig = HaxeModuleSettings.USE_PROPERTIES;
    if (myHxmlFileRadioButton.isSelected()) {
      buildConfig = HaxeModuleSettings.USE_HXML;
    }
    else if (myNmmlFileRadioButton.isSelected()) {
      buildConfig = HaxeModuleSettings.USE_NMML;
    }
    return buildConfig;
  }

  public JComponent getMainPanel() {
    return myMainPanel;
  }
}
