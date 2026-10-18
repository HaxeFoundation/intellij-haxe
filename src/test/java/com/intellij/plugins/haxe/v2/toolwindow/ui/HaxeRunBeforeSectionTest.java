package com.intellij.plugins.haxe.v2.toolwindow.ui;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.UIUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.ListCellRenderer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The list's label and existence lookups read build files, so they run once per reference, not on every repaint. */
@DisplayName("Tool window: run before section")
public class HaxeRunBeforeSectionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a reference is looked up once however often its row paints")
  public void testAReferenceIsLookedUpOnceHoweverOftenItsRowPaints() {
    List<ActionReference> references = List.of(new ActionReference(null, "prepare"), new ActionReference("/p/lib/build.hxml", "docs"));
    AtomicInteger labelLookups = new AtomicInteger();
    AtomicInteger existenceLookups = new AtomicInteger();
    HaxeRunBeforeSection section = new HaxeRunBeforeSection(
      name -> List.of(),
      reference -> { labelLookups.incrementAndGet(); return reference.action(); },
      reference -> { existenceLookups.incrementAndGet(); return reference.buildFile() != null; },
      buildFile -> null,
      (name, reference) -> null,
      references);
    JBList<?> list = UIUtil.findComponentOfType(section.getComponent(), JBList.class);
    assertNotNull(list, "the section holds the list");

    paintEveryRow(list, references);
    paintEveryRow(list, references);

    assertEquals(references.size(), labelLookups.get(), "one label lookup per reference");
    assertEquals(references.size(), existenceLookups.get(), "one existence lookup per reference");
  }

  @SuppressWarnings("unchecked")
  private static void paintEveryRow(JBList<?> list, List<ActionReference> references) {
    ListCellRenderer<ActionReference> renderer = (ListCellRenderer<ActionReference>)list.getCellRenderer();
    JBList<ActionReference> typed = (JBList<ActionReference>)list;
    for (int index = 0; index < references.size(); index++) {
      renderer.getListCellRendererComponent(typed, references.get(index), index, false, false);
    }
  }
}
