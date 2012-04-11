package com.intellij.plugins.haxe.ide.actions;

import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.CreateFromTemplateAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class CreateNMMLFile extends CreateFromTemplateAction {
  public CreateNMMLFile(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  @Override
  protected PsiElement createFile(String name, String templateName, PsiDirectory dir) {
    return null;
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return null;
  }
}
