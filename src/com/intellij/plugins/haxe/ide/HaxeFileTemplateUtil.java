package com.intellij.plugins.haxe.ide;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.nmml.NMMLFileType;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFileTemplateUtil {
  private final static String HAXE_TEMPLATE_PREFIX = "haXe ";

  public static List<FileTemplate> getApplicableTemplates() {
    return getApplicableTemplates(new Condition<FileTemplate>() {
      @Override
      public boolean value(FileTemplate fileTemplate) {
        return HaxeFileType.DEFAULT_EXTENSION.equals(fileTemplate.getExtension());
      }
    });
  }

  public static List<FileTemplate> getNMMLTemplates() {
    return getApplicableTemplates(new Condition<FileTemplate>() {
      @Override
      public boolean value(FileTemplate fileTemplate) {
        return NMMLFileType.DEFAULT_EXTENSION.equals(fileTemplate.getExtension());
      }
    });
  }

  public static List<FileTemplate> getApplicableTemplates(Condition<FileTemplate> filter) {
    List<FileTemplate> applicableTemplates = new SmartList<FileTemplate>();
    applicableTemplates.addAll(ContainerUtil.findAll(FileTemplateManager.getInstance().getInternalTemplates(), filter));
    applicableTemplates.addAll(ContainerUtil.findAll(FileTemplateManager.getInstance().getAllTemplates(), filter));
    return applicableTemplates;
  }

  public static String getTemplateShortName(String templateName) {
    if (templateName.startsWith(HAXE_TEMPLATE_PREFIX)) {
      return templateName.substring(HAXE_TEMPLATE_PREFIX.length());
    }
    return templateName;
  }

  @NotNull
  public static Icon getTemplateIcon(String name) {
    name = getTemplateShortName(name);
    if ("Class".equals(name)) {
      return icons.HaxeIcons.C_haXe;
    }
    else if ("Interface".equals(name)) {
      return icons.HaxeIcons.I_haXe;
    }
    else if ("Enum".equals(name)) {
      return icons.HaxeIcons.E_haXe;
    }
    return icons.HaxeIcons.HaXe_16;
  }
}
