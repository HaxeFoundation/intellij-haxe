package com.intellij.plugins.haxe.ide;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeIcons;
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
    List<FileTemplate> applicableTemplates = new SmartList<FileTemplate>();

    Condition<FileTemplate> filter = new Condition<FileTemplate>() {
      @Override
      public boolean value(FileTemplate fileTemplate) {
        return HaxeFileType.DEFAULT_EXTENSION.equals(fileTemplate.getExtension());
      }
    };

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
      return HaxeIcons.CLASS_ICON;
    }
    else if ("Interface".equals(name)) {
      return HaxeIcons.INTERFACE_ICON;
    }
    else if ("Enum".equals(name)) {
      return HaxeIcons.ENUM_ICON;
    }
    return HaxeIcons.HAXE_ICON_16x16;
  }
}
