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
package com.intellij.plugins.haxe.config.ui;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.ui.AddDeleteListPanel;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeProjectSettingsForm {
  private JPanel myPanel;
  private MyAddDeleteListPanel myAddDeleteListPanel;

  public JComponent getPanel() {
    return myPanel;
  }

  public boolean isModified(HaxeProjectSettings settings) {
    final List<String> oldList = Arrays.asList(settings.getUserCompilerDefinitions());
    final List<String> newList = Arrays.asList(myAddDeleteListPanel.getItems());
    final boolean isEqual = oldList.size() == newList.size() && new HashSet<>(oldList).containsAll(newList);
    return !isEqual;
  }

  public void applyEditorTo(HaxeProjectSettings settings) {
    settings.setUserCompilerDefinitions(myAddDeleteListPanel.getItems());
  }

  public void resetEditorFrom(HaxeProjectSettings settings) {
    myAddDeleteListPanel.removeALlItems();
    for (String item : settings.getUserCompilerDefinitions()) {
      myAddDeleteListPanel.addItem(item);
    }
  }

  private void createUIComponents() {
    myAddDeleteListPanel = new MyAddDeleteListPanel(HaxeBundle.message("haxe.conditional.compilation.defined.macros"));
  }

  private class MyAddDeleteListPanel extends AddDeleteListPanel<String> {
    public MyAddDeleteListPanel(final String title) {
      super(title, Collections.<String>emptyList());
    }

    public void addItem(String item) {
      myListModel.addElement(item);
    }

    public void removeALlItems() {
      myListModel.removeAllElements();
    }

    public String[] getItems() {
      final Object[] itemList = getListItems();
      final String[] result = new String[itemList.length];
      for (int i = 0; i < itemList.length; i++) {
        result[i] = itemList[i].toString();
      }
      return result;
    }

    @Override
    protected String findItemToAdd() {
      final StringValueDialog dialog = new StringValueDialog(myAddDeleteListPanel, false);
      dialog.show();
      if (!dialog.isOK()) {
        return null;
      }
      final String stringValue = dialog.getStringValue();
      return stringValue != null && stringValue.isEmpty() ? null : stringValue;
    }
  }
}
