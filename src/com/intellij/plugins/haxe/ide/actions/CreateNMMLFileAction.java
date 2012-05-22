package com.intellij.plugins.haxe.ide.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.PsiDirectory;

/**
 * @author: Fedor.Korotkov
 */
public class CreateNMMLFileAction extends CreateFileFromTemplateAction implements DumbAware {
  public CreateNMMLFileAction() {
    super(HaxeBundle.message("create.nmml.file.action"), HaxeBundle.message("create.nmml.file.action.description"),
          HaxeIcons.NMML_ICON_16x16);
  }

  @Override
  protected boolean isAvailable(DataContext dataContext) {
    final Module module = LangDataKeys.MODULE.getData(dataContext);
    return super.isAvailable(dataContext) && module != null && ModuleType.get(module) == HaxeModuleType.getInstance();
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder.setTitle(HaxeBundle.message("create.nmml.file.action"));
    for (FileTemplate fileTemplate : HaxeFileTemplateUtil.getNMMLTemplates()) {
      final String templateName = fileTemplate.getName();
      final String shortName = HaxeFileTemplateUtil.getTemplateShortName(templateName);
      builder.addKind(shortName, HaxeIcons.NMML_ICON_16x16, templateName);
    }
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return HaxeBundle.message("create.nmml.file.action");
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CreateNMMLFileAction;
  }
}
