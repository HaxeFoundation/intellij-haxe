package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.haxe3.Haxe3Completion.Query;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Display protocol: haxe3 completion")
public class Haxe3CompletionTest {

  private static final String CIRCLE_FIELDS_ANSWER = """
    <list>
    <i n="radius" k="var"><t>Float</t><d> Distance from the centre to the rim. </d></i>
    <i n="area" k="method"><t>Void -&gt; Float</t><d></d></i>
    <i n="@TIME @TOTAL" k="timer"><t>0.007s</t><d></d></i>
    </list>
    """;

  @Test
  @DisplayName("query after a dot asks right after it")
  public void queryAfterADotAsksRightAfterIt() {
    Query query = Query.at("var r = shape.circle.ra", 23);

    assertEquals(new Query(21, true, "ra", "shape.circle"), query);
  }

  @Test
  @DisplayName("query elsewhere asks at the caret")
  public void queryElsewhereAsksAtTheCaret() {
    Query query = Query.at("trace(lab", 9);

    assertEquals(new Query(9, false, "lab", ""), query);
  }

  @Test
  @DisplayName("field list maps vars and methods to class fields and leaves out timers")
  public void fieldListMapsVarsAndMethodsToClassFieldsAndLeavesOutTimers() throws Exception {
    CompletionList list = Haxe3Completion.fieldList(CIRCLE_FIELDS_ANSWER, Query.at("circle.", 7));

    List<String> names = list.items().stream().map(CompletionItem::name).toList();
    CompletionItem area = list.items().get(1);
    assertEquals(List.of("radius", "area"), names);
    assertEquals(0, list.modeKind());
    assertTrue(area.isField());
    assertTrue(area.type().isFunction());
    assertEquals(" Distance from the centre to the rim. ", list.items().getFirst().doc());
  }

  @Test
  @DisplayName("field list of a package path qualifies its types")
  public void fieldListOfAPackagePathQualifiesItsTypes() throws Exception {
    String answer = """
      <list>
      <i n="StringMap" k="type"><t></t><d></d></i>
      <i n="io" k="package"><t></t><d></d></i>
      </list>
      """;

    CompletionList list = Haxe3Completion.fieldList(answer, Query.at("haxe.ds.", 8));

    assertEquals("haxe.ds.StringMap", list.items().getFirst().detail());
    assertTrue(list.items().getFirst().isType());
    assertEquals("haxe.ds.io", list.items().get(1).detail());
  }

  @Test
  @DisplayName("toplevel list maps the kinds and drops the partial name's own entry")
  public void toplevelListMapsTheKindsAndDropsThePartialNamesOwnEntry() throws Exception {
    String answer = """
      <il>
      <i k="local" t="Unknown&lt;0&gt;">cou</i>
      <i k="local" t="Int">count</i>
      <i k="member" t="Float" d=" Instance state. ">speed</i>
      <i k="enum" t="level : Int -&gt; Mode">Slow</i>
      <i k="literal">null</i>
      <i k="type" p="haxe.Json">Json</i>
      </il>
      """;

    CompletionList list = Haxe3Completion.toplevelList(answer, Query.at("var x = cou", 11));

    List<String> kinds = list.items().stream().map(CompletionItem::kind).toList();
    assertEquals(List.of("Local", "ClassField", "EnumField", "Literal", "Type"), kinds);
    assertEquals("count", list.items().getFirst().name());
    assertEquals("haxe.Json", list.items().getLast().detail());
    assertEquals(2, list.modeKind());
  }
}
