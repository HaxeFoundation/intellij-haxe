package com.intellij.plugins.haxe.v2.wizard;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.plugins.haxe.HaxeWizardBundle;
import icons.HaxeIcons;

/// Surfaces the project-generator templates (`fileTemplates/j2ee/Haxe Project *.ft`)
/// as the "Haxe project" group under Settings | Editor | File and Code
/// Templates | Other — users can customize what the Haxe Template generator
/// scaffolds.
///
/// The `j2ee` directory name is the PLATFORM'S fixed location for
/// plugin template groups (the storage behind the "Other" tab) and, per the
/// platform docs, "historical and not specifically tied to J2EE technology" —
/// FileTemplateManager scans only its five fixed folders, so a
/// nicer-named directory would simply not load.
public class HaxeProjectTemplatesFactory implements FileTemplateGroupDescriptorFactory {

  private static final String[] TEMPLATES = {
    "Haxe Project Main.hx",
    "Haxe Project Build.hxml",
    "Haxe Project Dev.hxml",
    "Haxe Project Lime.xml",
    "Haxe Project NME.nmml",
    "Haxe Project Lime Main.hx",
    "Haxe Project OpenFL Main.hx",
    "Haxe Project NME Main.hx",
    "Haxe Project Haxelib.json",
    "Haxe Project Haxelib Class.hx",
  };

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    String message = HaxeWizardBundle.message("haxe.wizard.template.group");
    FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(message, HaxeIcons.HAXE_LOGO);

    for (String template : TEMPLATES) {
      group.addTemplate(new FileTemplateDescriptor(template));
    }

    return group;
  }
}
