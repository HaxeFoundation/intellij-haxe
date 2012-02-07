package com.intellij.plugins.haxe.ide.projectStructure.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ui.configuration.CommonContentEntriesEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeContentEntriesEditor implements ModuleConfigurationEditor {
  private final CommonContentEntriesEditor myEntriesEditor;
  private final Module myModule;

  public HaxeContentEntriesEditor(ModuleConfigurationState state) {
    myModule = state.getRootModel().getModule();
    myEntriesEditor = new CommonContentEntriesEditor(myModule.getName(), state, true, true) {
      @Override
      protected List<ContentEntry> addContentEntries(VirtualFile[] files) {
        List<ContentEntry> entries = super.addContentEntries(files);
        addContentEntryPanels(entries.toArray(new ContentEntry[entries.size()]));
        return entries;
      }
    };
    myEntriesEditor.getComponent().setBorder(new EmptyBorder(0, 0, 0, 0));
  }

  @Override
  public void saveData() {
  }

  @Override
  public void moduleStateChanged() {
  }

  @Nls
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.module.editor.name");
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myEntriesEditor.createComponent();
  }

  @Override
  public boolean isModified() {
    return myEntriesEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myEntriesEditor.apply();
  }

  @Override
  public void reset() {
    myEntriesEditor.reset();
  }

  @Override
  public void disposeUIResources() {
    myEntriesEditor.disposeUIResources();
  }
}
