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
package com.intellij.plugins.haxe.editor;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.FQNameCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

import static com.intellij.util.ui.UIUtil.ComponentStyle.SMALL;
import static com.intellij.util.ui.UIUtil.FontColor.BRIGHTER;

/**
 * Created by as3boyan on 09.10.14.
 */
public class HaxeRestoreReferencesDialog extends DialogWrapper {
  private final String[] myNamedElements;
  private JList myList;
  private String[] mySelectedElements = new String[]{};
  //private boolean myContainsClassesOnly = true;

  public HaxeRestoreReferencesDialog(final Project project, final String[] elements) {
    super(project, true);
    myNamedElements = elements;
    /*
    for (Object element : elements) {
      if (!(element instanceof PsiClass)) {
        myContainsClassesOnly = false;
        break;
      }
    }
    */
    /*
    if (myContainsClassesOnly) {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title"));
    }
    else {
      setTitle(CodeInsightBundle.message("dialog.import.on.paste.title2"));
    }
    */
    setTitle(CodeInsightBundle.message("dialog.import.on.paste.title"));
    init();

    myList.setSelectionInterval(0, myNamedElements.length - 1);
  }

  @Override
  protected void doOKAction() {
    Object[] values = myList.getSelectedValues();
    mySelectedElements = new String[values.length];
    System.arraycopy(values, 0, mySelectedElements, 0, values.length);
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP));
    myList = new JBList(myNamedElements);
    myList.setCellRenderer(new FQNameCellRenderer());
    panel.add(ScrollPaneFactory.createScrollPane(myList), BorderLayout.CENTER);

    panel.add(new JBLabel(CodeInsightBundle.message("dialog.paste.on.import.text"), SMALL, BRIGHTER), BorderLayout.NORTH);

    final JPanel buttonPanel = new JPanel(new VerticalFlowLayout());
    final JButton okButton = new JButton(CommonBundle.getOkButtonText());
    getRootPane().setDefaultButton(okButton);
    buttonPanel.add(okButton);
    final JButton cancelButton = new JButton(CommonBundle.getCancelButtonText());
    buttonPanel.add(cancelButton);

    panel.setPreferredSize(new Dimension(500, 400));

    return panel;
  }


  @Override
  protected String getDimensionServiceKey(){
    return "#com.intellij.codeInsight.editorActions.RestoreReferencesDialog";
  }

  public String[] getSelectedElements(){
    return mySelectedElements;
  }
}
