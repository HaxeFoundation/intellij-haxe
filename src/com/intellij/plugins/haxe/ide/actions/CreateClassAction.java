/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.CreateTemplateInPackageAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Properties;

/**
 * @author: Fedor.Korotkov
 */
public class CreateClassAction extends CreateTemplateInPackageAction<PsiFile> {
  public CreateClassAction() {
    super(HaxeBundle.message("action.create.new.class"), HaxeBundle.message("action.create.new.class"), icons.HaxeIcons.Haxe_16, true);
  }

  @Override
  protected boolean isAvailable(DataContext dataContext) {
    final Module module = LangDataKeys.MODULE.getData(dataContext);
    return super.isAvailable(dataContext) && module != null && ModuleType.get(module) == HaxeModuleType.getInstance();
  }

  @Override
  protected PsiElement getNavigationElement(@NotNull PsiFile createdElement) {
    return createdElement.getNavigationElement();
  }

  @Override
  protected boolean checkPackageExists(PsiDirectory directory) {
    return DirectoryIndex.getInstance(directory.getProject()).getPackageName(directory.getVirtualFile()) != null;
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return HaxeBundle.message("progress.creating.class", newName);
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder.setTitle(IdeBundle.message("action.create.new.class"));
    for (FileTemplate fileTemplate : HaxeFileTemplateUtil.getApplicableTemplates()) {
      final String templateName = fileTemplate.getName();
      final String shortName = HaxeFileTemplateUtil.getTemplateShortName(templateName);
      final Icon icon = HaxeFileTemplateUtil.getTemplateIcon(templateName);
      builder.addKind(shortName, icon, templateName);
    }
  }

  @Override
  protected PsiFile doCreate(@NotNull PsiDirectory dir, String className, String templateName) throws IncorrectOperationException {
    String packageName = DirectoryIndex.getInstance(dir.getProject()).getPackageName(dir.getVirtualFile());
    try {
      return createClass(className, packageName, dir, templateName).getContainingFile();
    }
    catch (Exception e) {
      throw new IncorrectOperationException(e.getMessage(), e);
    }
  }

  private static PsiElement createClass(String className, String packageName, @NotNull PsiDirectory directory, final String templateName)
    throws Exception {
    final Properties props = new Properties(FileTemplateManager.getInstance().getDefaultProperties(directory.getProject()));
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, className);
    props.setProperty(FileTemplate.ATTRIBUTE_PACKAGE_NAME, packageName);

    final FileTemplate template = FileTemplateManager.getInstance().getInternalTemplate(templateName);

    return FileTemplateUtil.createFromTemplate(template, className, props, directory, CreateClassAction.class.getClassLoader());
  }
}
