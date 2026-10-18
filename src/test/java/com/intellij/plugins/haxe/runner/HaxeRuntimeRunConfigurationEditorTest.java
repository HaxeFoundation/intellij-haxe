package com.intellij.plugins.haxe.runner;

import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.util.ui.UIUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The editor builds from its designer form; the adapter row belongs to a runtime whose Debug needs one. */
@DisplayName("Run configurations: runtime editor")
public class HaxeRuntimeRunConfigurationEditorTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a runtime with a debug adapter shows the adapter row, one without hides it")
  public void testARuntimeWithADebugAdapterShowsTheAdapterRowOneWithoutHidesIt() {
    HaxeRuntimeRunConfigurationEditor node = new HaxeRuntimeRunConfigurationEditor(getProject(), "hint", AdapterPin.JS_DEBUG);
    HaxeRuntimeRunConfigurationEditor neko = new HaxeRuntimeRunConfigurationEditor(getProject(), "hint", null);

    JLabel nodeAdapterLabel = adapterLabel(node.getComponent());
    JLabel nekoAdapterLabel = adapterLabel(neko.getComponent());

    assertNotNull(nodeAdapterLabel, "the form binds the adapter row");
    assertTrue(nodeAdapterLabel.isVisible(), "Node.js debugs through js-debug: the row shows");
    assertNotNull(nekoAdapterLabel);
    assertFalse(nekoAdapterLabel.isVisible(), "neko needs no adapter: the row hides");
  }

  private static JLabel adapterLabel(JComponent editor) {
    String text = UIUtil.removeMnemonic(HaxeDebuggerBundle.message("runtime.runner.editor.debug.adapter"));
    return UIUtil.findComponentsOfType(editor, JLabel.class).stream()
      .filter(label -> text.equals(label.getText()))
      .findFirst()
      .orElse(null);
  }
}
