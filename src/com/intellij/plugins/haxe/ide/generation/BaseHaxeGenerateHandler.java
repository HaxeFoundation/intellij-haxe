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
package com.intellij.plugins.haxe.ide.generation;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.HaxeNamedElementNode;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public abstract class BaseHaxeGenerateHandler implements LanguageCodeInsightActionHandler {
  @Override
  public boolean isValidFor(Editor editor, PsiFile file) {
    return file instanceof HaxeFile;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    final HaxeClass haxeClass =
      PsiTreeUtil.getParentOfType(file.findElementAt(editor.getCaretModel().getOffset()), HaxeClassDeclaration.class);
    if (haxeClass == null) return;

    final List<HaxeNamedComponent> candidates = new ArrayList<HaxeNamedComponent>();
    collectCandidates(haxeClass, candidates);

    List<HaxeNamedElementNode> selectedElements = Collections.emptyList();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      selectedElements = ContainerUtil.map(candidates, new Function<HaxeNamedComponent, HaxeNamedElementNode>() {
        @Override
        public HaxeNamedElementNode fun(HaxeNamedComponent namedComponent) {
          return new HaxeNamedElementNode(namedComponent);
        }
      });
    }
    else if (!candidates.isEmpty()) {
      final MemberChooser<HaxeNamedElementNode> chooser =
        createMemberChooserDialog(project, haxeClass, candidates, getTitle());
      chooser.show();
      selectedElements = chooser.getSelectedElements();
    }

    final BaseCreateMethodsFix createMethodsFix = createFix(haxeClass);
    doInvoke(project, editor, file, selectedElements, createMethodsFix);
  }

  protected void doInvoke(final Project project,
                          final Editor editor,
                          final PsiFile file,
                          final Collection<HaxeNamedElementNode> selectedElements,
                          final BaseCreateMethodsFix createMethodsFix) {
    Runnable runnable = new Runnable() {
      public void run() {
        createMethodsFix.addElementsToProcessFrom(selectedElements);
        createMethodsFix.beforeInvoke(project, editor, file);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            try {
              createMethodsFix.invoke(project, editor, file);
            }
            catch (IncorrectOperationException ex) {
              Logger.getInstance(getClass().getName()).error(ex);
            }
          }
        });
      }
    };

    if (CommandProcessor.getInstance().getCurrentCommand() == null) {
      CommandProcessor.getInstance().executeCommand(project, runnable, getClass().getName(), null);
    }
    else {
      runnable.run();
    }
  }

  protected abstract BaseCreateMethodsFix createFix(HaxeClass haxeClass);

  protected abstract String getTitle();

  abstract void collectCandidates(HaxeClass aClass, List<HaxeNamedComponent> candidates);

  @Nullable
  protected JComponent getOptionsComponent(HaxeClass jsClass, final Collection<HaxeNamedComponent> candidates) {
    return null;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  protected MemberChooser<HaxeNamedElementNode> createMemberChooserDialog(final Project project,
                                                                          final HaxeClass haxeClass,
                                                                          final Collection<HaxeNamedComponent> candidates,
                                                                          String title) {
    final MemberChooser<HaxeNamedElementNode> chooser = new MemberChooser<HaxeNamedElementNode>(
      ContainerUtil.map(candidates, new Function<HaxeNamedComponent, HaxeNamedElementNode>() {
        @Override
        public HaxeNamedElementNode fun(HaxeNamedComponent namedComponent) {
          return new HaxeNamedElementNode(namedComponent);
        }
      }).toArray(new HaxeNamedElementNode[candidates.size()]), false, true, project, false) {

      protected void init() {
        super.init();
        myTree.addTreeSelectionListener(new TreeSelectionListener() {
          public void valueChanged(final TreeSelectionEvent e) {
            setOKActionEnabled(myTree.getSelectionCount() > 0);
          }
        });
      }

      protected JComponent createCenterPanel() {
        final JComponent superComponent = super.createCenterPanel();
        final JComponent optionsComponent = getOptionsComponent(haxeClass, candidates);
        if (optionsComponent == null) {
          return superComponent;
        }
        else {
          final JPanel panel = new JPanel(new BorderLayout());
          panel.add(superComponent, BorderLayout.CENTER);
          panel.add(optionsComponent, BorderLayout.SOUTH);
          return panel;
        }
      }
    };

    chooser.setTitle(title);
    chooser.setCopyJavadocVisible(false);
    return chooser;
  }
}
