/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.config.*;
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
  private TextFieldWithBrowseButton myHxmlFileChooserTextField;
  private JBRadioButton myHxmlFileRadioButton;
  private JBRadioButton myNmmlFileRadioButton;
  private JBRadioButton myOpenFLFileRadioButton;
  private JBRadioButton myUserPropertiesRadioButton;
  private JPanel myCompilerOptions;
  private JPanel myCommonPanel;
  private JTextField myDefinedMacroses;
  private JButton myEditMacrosesButton;
  private JPanel myHxmlFileChooserPanel;
  private RawCommandLineEditor myNMEArguments;
  private TextFieldWithBrowseButton myNMEFileChooserTextField;
  private JPanel myNMEFilePanel;
  private JPanel myOpenFLFilePanel;
  private JPanel myBuildFilePanel;
  private JPanel myCompilerOptionsWrapper;
  private TextFieldWithBrowseButton myOpenFLFileChooserTextField;

  private HaxeTarget selectedHaxeTarget = HaxeTarget.NEKO;
  private NMETarget selectedNmeTarget = NMETarget.FLASH;
  private OpenFLTarget selectedOpenFLTarget = OpenFLTarget.FLASH;

  private final Module myModule;
  private final CompilerModuleExtension myExtension;

  private final List<UnnamedConfigurable> configurables = new ArrayList<UnnamedConfigurable>();

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
    group.add(myOpenFLFileRadioButton);
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
          FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), myModule.getProject(), null);
        if (folder != null) {
          myFolderTextField.setText(FileUtil.toSystemDependentName(folder.getPath()));
        }
      }
    });

    myTargetComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myTargetComboBox.getSelectedItem() instanceof HaxeTarget) {
          selectedHaxeTarget = (HaxeTarget)myTargetComboBox.getSelectedItem();
        }
        if (myTargetComboBox.getSelectedItem() instanceof NMETarget) {
          selectedNmeTarget = (NMETarget)myTargetComboBox.getSelectedItem();
        }
        if (myTargetComboBox.getSelectedItem() instanceof OpenFLTarget) {
          selectedOpenFLTarget = (OpenFLTarget)myTargetComboBox.getSelectedItem();
        }
        if (!myOutputFileNameTextField.getText().isEmpty()) {
          myOutputFileNameTextField.setText(getCurrentExtension(FileUtil.getNameWithoutExtension(myOutputFileNameTextField.getText())));
        }
      }
    });

    final ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateComponents();
        updateTargetCombo();
      }
    };
    myHxmlFileRadioButton.addActionListener(listener);
    myNmmlFileRadioButton.addActionListener(listener);
    myUserPropertiesRadioButton.addActionListener(listener);
    myOpenFLFileRadioButton.addActionListener(listener);

    ActionListener fileChooserListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final VirtualFile moduleFile = myModule.getModuleFile();
        assert moduleFile != null;
        final boolean isNMML = myNmmlFileRadioButton.isSelected();
        final boolean isOpenFL = myOpenFLFileRadioButton.isSelected();
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, true, false, false) {
          public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
            return super.isFileVisible(file, showHiddenFiles) &&
                   (file.isDirectory()
                    || (isNMML ? "nmml" : "hxml").equalsIgnoreCase(file.getExtension())
                    || (isOpenFL && "project.xml".equalsIgnoreCase(file.getName()))
                    || (isOpenFL && "Project.xml".equalsIgnoreCase(file.getName()))
                   );
          }
        };
        final VirtualFile file = FileChooser.chooseFile(descriptor, getMainPanel(), null, moduleFile.getParent());
        if (file != null) {
          String path = FileUtil.toSystemIndependentName(file.getPath());
          if (isNMML) {
            myNMEFileChooserTextField.setText(path);
          }
          else if(isOpenFL)
          {
            myOpenFLFileChooserTextField.setText(path);
          }
          else
          {
            myHxmlFileChooserTextField.setText(path);
          }
          updateComponents();
        }
      }
    };

    myHxmlFileChooserTextField.getButton().addActionListener(fileChooserListener);
    myNMEFileChooserTextField.getButton().addActionListener(fileChooserListener);
    myOpenFLFileChooserTextField.getButton().addActionListener(fileChooserListener);

    myEditMacrosesButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Project project = myModule.getProject();
        final HaxeSettingsConfigurable configurable = new HaxeSettingsConfigurable(project);
        final SingleConfigurableEditor editor = new SingleConfigurableEditor(
          project,
          configurable,
          ShowSettingsUtilImpl.createDimensionKey(configurable),
          false
        );
        editor.show();
        updateMacroses();
      }
    });
  }

  private void updateComponents() {
    updateUserProperties();
    updateFileChooser();
    updateMacroses();
  }

  private void updateMacroses() {
    final String[] userCompilerDefinitions = HaxeProjectSettings.getInstance(myModule.getProject()).getUserCompilerDefinitions();
    myDefinedMacroses.setText(StringUtil.join(userCompilerDefinitions, ","));
  }

  private void updateFileChooser() {
    boolean containsHxml = false;
    boolean containsNME = false;
    boolean containsOpenFL = false;
    Component[] components = myBuildFilePanel.getComponents();
    for (Component component : components) {
      if (component == myHxmlFileChooserPanel) {
        containsHxml = true;
      }
      if (component == myNMEFilePanel) {
        containsNME = true;
      }
      if (component == myOpenFLFilePanel) {
        containsOpenFL = true;
      }
    }
    if (!myHxmlFileRadioButton.isSelected() && containsHxml) {
      myBuildFilePanel.remove(myHxmlFileChooserPanel);
    }
    if (!myNmmlFileRadioButton.isSelected() && containsNME) {
      myBuildFilePanel.remove(myNMEFilePanel);
    }

    if (!myOpenFLFileRadioButton.isSelected() && containsOpenFL) {
      myBuildFilePanel.remove(myOpenFLFilePanel);
      myBuildFilePanel.remove(myOpenFLFileChooserTextField);
    }

    final GridConstraints constraints = new GridConstraints();
    constraints.setRow(0);
    constraints.setFill(GridConstraints.FILL_HORIZONTAL);
    if (myHxmlFileRadioButton.isSelected() && !containsHxml) {
      myBuildFilePanel.add(myHxmlFileChooserPanel, constraints);
    }
    if (myNmmlFileRadioButton.isSelected() && !containsNME) {
      myBuildFilePanel.add(myNMEFilePanel, constraints);
    }

    if (myOpenFLFileRadioButton.isSelected() && !containsOpenFL) {
      myBuildFilePanel.add(myOpenFLFilePanel, constraints);
    }
  }

  private void updateUserProperties() {
    boolean contains = false;
    Component[] components = myCompilerOptionsWrapper.getComponents();
    for (Component component : components) {
      if (component == myCompilerOptions) {
        contains = true;
        break;
      }
    }
    if (myUserPropertiesRadioButton.isSelected() && !contains) {
      final GridConstraints constraints = new GridConstraints();
      constraints.setRow(0);
      constraints.setFill(GridConstraints.FILL_HORIZONTAL);
      myCompilerOptionsWrapper.add(myCompilerOptions, constraints);
    }
    else if (!myUserPropertiesRadioButton.isSelected() && contains) {
      myCompilerOptionsWrapper.remove(myCompilerOptions);
    }
  }

  private void updateTargetCombo() {
    ((DefaultComboBoxModel)myTargetComboBox.getModel()).removeAllElements();
    if (myNmmlFileRadioButton.isSelected()) {
      NMETarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
      myTargetComboBox.setSelectedItem(selectedNmeTarget);
    }
    else if (myOpenFLFileRadioButton.isSelected()) {
      OpenFLTarget.initCombo((DefaultComboBoxModel)myTargetComboBox.getModel());
      myTargetComboBox.setSelectedItem(selectedOpenFLTarget);
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
    for (int i = 0; i < extensionPoints.length; i++) {
      HaxeModuleConfigurationExtensionPoint extensionPoint = extensionPoints[i];
      final GridConstraints gridConstraints = new GridConstraints();
      gridConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
      gridConstraints.setRow(i);

      final UnnamedConfigurable configurable = extensionPoint.createConfigurable(settings);
      configurables.add(configurable);
      myAdditionalComponentPanel.add(configurable.createComponent(), gridConstraints);
    }
  }

  private void setChosenFile(VirtualFile virtualFile) {
    VirtualFile parent = virtualFile.getParent();
    String qualifier = parent == null ? null : DirectoryIndex.getInstance(myModule.getProject()).getPackageName(parent);
    qualifier = qualifier != null && qualifier.length() != 0 ? qualifier + '.' : "";
    myMainClassFieldWithButton.setText(qualifier + FileUtil.getNameWithoutExtension(virtualFile.getName()));
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
    final String urlCandidate = VfsUtilCore.pathToUrl(myFolderTextField.getText());
    boolean result = !urlCandidate.equals(url);

    result = result || settings.getNmeTarget() != selectedNmeTarget;
    result = result || !FileUtil.toSystemIndependentName(myNMEFileChooserTextField.getText()).equals(settings.getNmmlPath());

    result = result || settings.getOpenFLTarget() != selectedOpenFLTarget;

    result = result || !settings.getMainClass().equals(myMainClassFieldWithButton.getText());
    result = result || settings.getHaxeTarget() != selectedHaxeTarget;

    result = result || !FileUtil.toSystemIndependentName(myHxmlFileChooserTextField.getText()).equals(settings.getHxmlPath());

    result = result || !FileUtil.toSystemIndependentName(myOpenFLFileChooserTextField.getText()).equals(settings.getOpenFLXmlPath());

    result = result || !settings.getArguments().equals(myAppArguments.getText());
    result = result || !settings.getNmeFlags().equals(myNMEArguments.getText());
    result = result || (settings.isExcludeFromCompilation() ^ myExcludeFromCompilationCheckBox.isSelected());
    result = result || !settings.getOutputFileName().equals(myOutputFileNameTextField.getText());
    result = result || !settings.getOutputFolder().equals(myFolderTextField.getText());

    result = result || getCurrentBuildConfig() != settings.getBuildConfig();

    for (UnnamedConfigurable configurable : configurables) {
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
    selectedOpenFLTarget = settings.getOpenFLTarget();
    myExcludeFromCompilationCheckBox.setSelected(settings.isExcludeFromCompilation());
    myOutputFileNameTextField.setText(settings.getOutputFileName());
    myFolderTextField.setText(settings.getOutputFolder());
    for (UnnamedConfigurable configurable : configurables) {
      configurable.reset();
    }

    final String url = myExtension.getCompilerOutputUrl();
    myFolderTextField.setText(VfsUtil.urlToPath(url));
    myHxmlFileChooserTextField.setText(settings.getHxmlPath());
    myOpenFLFileChooserTextField.setText(settings.getOpenFLXmlPath());
    myNMEFileChooserTextField.setText(settings.getNmmlPath());
    myNMEArguments.setText(settings.getNmeFlags());

    myHxmlFileRadioButton.setSelected(settings.isUseHxmlToBuild());
    myNmmlFileRadioButton.setSelected(settings.isUseNmmlToBuild());
    myUserPropertiesRadioButton.setSelected(settings.isUseUserPropertiesToBuild());
    myOpenFLFileRadioButton.setSelected(settings.isUseOpenFLToBuild());
    updateComponents();
    updateTargetCombo();
  }

  public void apply() {
    final HaxeModuleSettings settings = HaxeModuleSettings.getInstance(myModule);
    assert settings != null;
    settings.setMainClass(myMainClassFieldWithButton.getText());
    settings.setArguments(myAppArguments.getText());
    settings.setNmeFlags(myNMEArguments.getText());
    if (myNmmlFileRadioButton.isSelected()) {
      settings.setNmeTarget((NMETarget)myTargetComboBox.getSelectedItem());
    }
    else if (myOpenFLFileRadioButton.isSelected()) {
      settings.setOpenFLTarget((OpenFLTarget)myTargetComboBox.getSelectedItem());
    }
    else {
      settings.setHaxeTarget((HaxeTarget)myTargetComboBox.getSelectedItem());
    }
    settings.setExcludeFromCompilation(myExcludeFromCompilationCheckBox.isSelected());
    settings.setOutputFileName(myOutputFileNameTextField.getText());
    settings.setOutputFolder(myFolderTextField.getText());

    settings.setHxmlPath(FileUtil.toSystemIndependentName(myHxmlFileChooserTextField.getText()));
    settings.setOpenFLXMLPath(FileUtil.toSystemIndependentName(myOpenFLFileChooserTextField.getText()));
    settings.setNmmlPath(FileUtil.toSystemIndependentName(myNMEFileChooserTextField.getText()));

    settings.setBuildConfig(getCurrentBuildConfig());
    for (UnnamedConfigurable configurable : configurables) {
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
    else if (myOpenFLFileRadioButton.isSelected()) {
      buildConfig = HaxeModuleSettings.USE_OPENFL;
    }
    return buildConfig;
  }

  public JComponent getMainPanel() {
    return myMainPanel;
  }
}
