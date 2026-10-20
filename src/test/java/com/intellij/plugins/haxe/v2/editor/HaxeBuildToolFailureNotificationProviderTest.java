package com.intellij.plugins.haxe.v2.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeBuildToolFailures;
import com.intellij.ui.EditorNotificationPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Editor: build tool failure banner")
public class HaxeBuildToolFailureNotificationProviderTest extends HaxeLightFixtureTestCase {

  private static final String NMML_SOURCE = """
    <project>
      <app main="Main" file="Sample"/>
      <haxelib name="nme"/>
    </project>
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("shows the file's failure and goes with the next success")
  public void testShowsTheFilesFailureAndGoesWithTheNextSuccess() {
    VirtualFile nmml = myFixture.configureByText("broken.nmml", NMML_SOURCE).getVirtualFile();
    HaxeBuildToolFailures failures = HaxeBuildToolFailures.getInstance(getProject());
    HaxeBuildToolFailureNotificationProvider provider = new HaxeBuildToolFailureNotificationProvider();
    FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor(nmml);
    assertNotNull(editor);

    assertNull(provider.collectNotificationData(getProject(), nmml), "no failure, no banner");

    failures.record(nmml.getPath(), "nme prepare failed: Library no-such-library-xyz is not installed");
    Function<? super FileEditor, ? extends JComponent> banner = provider.collectNotificationData(getProject(), nmml);
    assertNotNull(banner, "a failure puts the banner up");
    EditorNotificationPanel panel = assertInstanceOf(EditorNotificationPanel.class, banner.apply(editor));
    assertEquals("nme prepare failed: Library no-such-library-xyz is not installed", panel.getText());

    failures.record(nmml.getPath(), null);
    assertNull(provider.collectNotificationData(getProject(), nmml), "a success takes it down");
  }
}
