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
package com.intellij.plugins.haxe.ide;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.buildsystem.nmml.NMMLFileType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Properties;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFileTemplateUtil {
  private final static String HAXE_TEMPLATE_PREFIX = "Haxe";

  // Internal file-template names: must match the <internalFileTemplate> entries
  // in plugin.xml and the fileTemplates/internal/*.ft file names.
  public static final String CLASS_TEMPLATE = "Haxe Class";
  public static final String INTERFACE_TEMPLATE = "Haxe Interface";
  public static final String ENUM_TEMPLATE = "Haxe Enum";
  public static final String ABSTRACT_TEMPLATE = "Haxe Abstract";

  public static final List<String> TYPE_TEMPLATE_NAMES = List.of(
    CLASS_TEMPLATE,
    INTERFACE_TEMPLATE,
    ENUM_TEMPLATE,
    ABSTRACT_TEMPLATE
  );

  public static List<FileTemplate> getApplicableTemplates(@NotNull Project project) {
    return getApplicableTemplates(project, fileTemplate -> HaxeFileType.DEFAULT_EXTENSION.equals(fileTemplate.getExtension()));
  }

  public static List<FileTemplate> getNMMLTemplates(@NotNull Project project) {
    return getApplicableTemplates(project, fileTemplate -> NMMLFileType.DEFAULT_EXTENSION.equals(fileTemplate.getExtension()));
  }

  public static List<FileTemplate> getApplicableTemplates(@NotNull Project project, Condition<FileTemplate> filter) {
    List<FileTemplate> applicableTemplates = new SmartList<FileTemplate>();
    applicableTemplates.addAll(ContainerUtil.findAll(FileTemplateManager.getInstance(project).getInternalTemplates(), filter));
    applicableTemplates.addAll(ContainerUtil.findAll(FileTemplateManager.getInstance(project).getAllTemplates(), filter));
    return applicableTemplates;
  }

  public static String getTemplateShortName(String templateName) {
    if (templateName.startsWith(HAXE_TEMPLATE_PREFIX)) {
      // template names follow the platform's spaced convention ("Haxe Class")
      return templateName.substring(HAXE_TEMPLATE_PREFIX.length()).trim();
    }
    return templateName;
  }

  @NotNull
  public static Icon getTemplateIcon(String name) {
    name = getTemplateShortName(name);
    if ("Class".equals(name)) {
      return HaxeIcons.Class;
    }
    else if ("Interface".equals(name)) {
      return HaxeIcons.Interface;
    }
    else if ("Enum".equals(name)) {
      return HaxeIcons.Enum;
    }
    else if ("Abstract".equals(name)) {
      return HaxeIcons.Abstract;
    }
    else if ("Module".equals(name)) {
      return HaxeIcons.Module;
    }

    return HaxeIcons.HAXE_LOGO;
  }

  public static PsiElement createType(String className,
                                      String generics,
                                      String packageName,
                                      PsiDirectory directory,
                                      String templateName,
                                      @org.jetbrains.annotations.Nullable java.lang.ClassLoader classLoader) throws Exception {

    Project project = directory.getProject();
    final Properties props = new Properties(FileTemplateManager.getInstance(project).getDefaultProperties());
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, className + generics);
    props.setProperty(FileTemplate.ATTRIBUTE_PACKAGE_NAME, packageName);

    final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName);

    return FileTemplateUtil.createFromTemplate(template, className, props, directory, classLoader);
  }
}
