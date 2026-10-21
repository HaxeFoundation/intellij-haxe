package com.intellij.plugins.haxe.v2.toolwindow.tree;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.*;
import com.intellij.ui.treeStructure.Tree;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A chooser row's value is a click target of its own; the label, and a row without a chooser, only select. */
@DisplayName("Tool window: tree renderer")
public class HaxeToolWindowTreeRendererTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("chooser value answers a click")
  public void testChooserValueAnswersAClick() {
    List<Object> tags = fragmentTags(new EnvLanguageLevelNode("container", "Haxe 4.3"));

    // label, gap, value, dropdown marker
    assertEquals(Arrays.asList(null, null, RowValueLink.INSTANCE, RowValueLink.INSTANCE), tags);
  }

  @Test
  @DisplayName("server failure link stays apart from the value")
  public void testServerFailureLinkStaysApartFromTheValue() {
    var server = new CompilationServerNode("container", "on", true, true, true, true, "unreachable");

    List<Object> tags = fragmentTags(server);

    var failureLink = new ServerFailureLink("container");
    assertEquals(Arrays.asList(null, null, RowValueLink.INSTANCE, RowValueLink.INSTANCE, failureLink), tags);
  }

  @Test
  @DisplayName("row without chooser has no click target")
  public void testRowWithoutChooserHasNoClickTarget() {
    var tool = new ToolNode("container", "lix", List.of("lix", "download"), null, "lix download", false);

    List<Object> tags = fragmentTags(tool);

    assertEquals(Arrays.asList(null, null), tags);
  }

  @NotNull
  private static List<Object> fragmentTags(@NotNull Object userObject) {
    HaxeToolWindowTreeRenderer renderer = new HaxeToolWindowTreeRenderer();
    renderer.getTreeCellRendererComponent(new Tree(), new DefaultMutableTreeNode(userObject), false, false, true, 0, false);
    return IntStream.range(0, renderer.getFragmentCount())
      .mapToObj(renderer::getFragmentTag)
      .toList();
  }
}
