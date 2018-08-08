/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2018 AS3Boyan
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

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Vector;

public class HXMLSelector {
  private JPanel contentPane;
  private JList<String> hxmlList;
  private JLabel label;

  public HXMLSelector(Vector<String> hxmls) {
    hxmlList.setListData(hxmls);
    if (hasItems()) {
      hxmlList.setSelectedIndex(0);
    }
    else {
      label.setText("No suitable hxml files found (searched project root for hxml files with compilation target specified).");
    }
  }

  public JPanel getContentPane() {
    return contentPane;
  }

  public boolean hasItems() {
    return hxmlList.getModel().getSize() > 0;
  }

  @Nullable
  public String getSelected() {
    if (hxmlList.isSelectionEmpty() && hasItems()) {
      hxmlList.setSelectedIndex(0);
    }
    return hxmlList.getSelectedValue();
  }
}
