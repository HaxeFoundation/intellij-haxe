package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeOptionsPreviewPanelBase;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;

/**
 * The hxformat tab: whether a project's own hxformat.json overrides the
 * scheme's Haxe formatting per file (see {@link HxformatSettingsModifier}).
 * The tab has no preview, so nothing watches its toggle.
 */
public class HxformatCodeStylePanel extends HaxeOptionsPreviewPanelBase {

  private final JBCheckBox useProjectConfig =
    new JBCheckBox(HaxeCodeStyleBundle.message("hxformat.panel.use.project.config"));

  public HxformatCodeStylePanel(CodeStyleSettings settings) {
    super(settings);
    JBLabel description = new JBLabel(HaxeCodeStyleBundle.message("hxformat.panel.description"));
    description.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    description.setForeground(UIUtil.getContextHelpForeground());
    JPanel form = FormBuilder.createFormBuilder()
      .addComponent(useProjectConfig)
      .addComponent(description)
      .getPanel();
    initPanel(form);
  }

  @Override
  protected String getTabTitle() {
    return HaxeCodeStyleBundle.message("hxformat.panel.tab.title");
  }

  @Override
  public void apply(@NotNull CodeStyleSettings settings) {
    haxeSettings(settings).USE_PROJECT_HXFORMAT = useProjectConfig.isSelected();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    return haxeSettings(settings).USE_PROJECT_HXFORMAT != useProjectConfig.isSelected();
  }

  @Override
  protected void resetImpl(@NotNull CodeStyleSettings settings) {
    useProjectConfig.setSelected(haxeSettings(settings).USE_PROJECT_HXFORMAT);
  }

  @Override
  protected @Nullable String getPreviewText() {
    return null;
  }
}
