package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiFile;
import com.intellij.application.options.CodeStyle;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor;
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formats a file by the project's OWN hxformat.json (haxe-formatter config)
 * when one exists, the way EditorConfig support overrides settings per file.
 * The nearest config above the file wins, matching the CLI's upward search.
 * The modifier applies the hxformat default profile and then the config's
 * keys onto TRANSIENT settings, so no scheme is created or changed. A scheme
 * opts out through {@link HaxeCodeStyleSettings#USE_PROJECT_HXFORMAT}.
 */
public class HxformatSettingsModifier implements CodeStyleSettingsModifier {

  @Override
  public boolean modifySettings(@NotNull TransientCodeStyleSettings settings, @NotNull PsiFile file) {
    if (!(file instanceof HaxeFile)) return false;
    if (!settings.getCustomSettings(HaxeCodeStyleSettings.class).USE_PROJECT_HXFORMAT) return false;
    VirtualFile configFile = configFor(file);
    if (configFile == null) return false;

    HxformatConfigs configs = HxformatConfigs.getInstance(file.getProject());
    settings.addDependency(configs.tracker());
    JsonNode root = configs.parsed(configFile);
    if (root == null) return false;
    // disableFormatting turns the tool off for the folder; the closest IDE
    // equivalent is falling back to the plain scheme settings
    if (root.path("disableFormatting").asBoolean(false)) return false;

    HxformatDefaultProfile.apply(settings);
    HxformatJsonMapper.apply(settings, root);
    return true;
  }

  @Override
  public boolean mayOverrideSettingsOf(@NotNull Project project) {
    return CodeStyle.getSettings(project).getCustomSettings(HaxeCodeStyleSettings.class).USE_PROJECT_HXFORMAT;
  }

  @Override
  public String getName() {
    return HaxeCodeStyleBundle.message("hxformat.modifier.name");
  }

  @Override
  public @Nullable CodeStyleStatusBarUIContributor getStatusBarUiContributor(@NotNull TransientCodeStyleSettings transientSettings) {
    PsiFile file = transientSettings.getPsiFile();
    if (file == null) return null;
    VirtualFile configFile = configFor(file);
    return configFile == null ? null : new HxformatStatusBarContributor(configFile);
  }

  /** The hxformat.json governing the file (a copy resolves through its original), or null. */
  @Nullable
  private static VirtualFile configFor(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    return virtualFile == null ? null : HxformatConfigs.findConfig(file.getProject(), virtualFile);
  }
}
