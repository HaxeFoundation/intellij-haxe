package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.haxe3.Haxe3Wire.*;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Display protocol: haxe3 xml")
public class Haxe3XmlTest {

  @Test
  @DisplayName("reads a field list with escaped type and doc text")
  public void readsAFieldListWithEscapedTypeAndDocText() throws Exception {
    String answer = """
      <list>
      <i n="scaled" k="method"><t>factor : Float -&gt; Null&lt;String&gt;</t><d> Scales &amp; rounds. </d></i>
      </list>
      """;

    FieldEntry entry = Haxe3Xml.read(answer).fields().getFirst();

    assertEquals(new MemberField("scaled", "factor : Float -> Null<String>", " Scales & rounds. "), entry);
  }

  @Test
  @DisplayName("binds each field kind to its record")
  public void bindsEachFieldKindToItsRecord() throws Exception {
    String answer = """
      <list>
      <i n="radius" k="var"><t>Float</t><d></d></i>
      <i n="StringMap" k="type"><t></t><d> Strings to values. </d></i>
      <i n="ds" k="package"><t></t><d></d></i>
      <i n="@:keep" k="metadata"><t></t><d>Keeps the field.</d></i>
      <i n="@TIME @TOTAL" k="timer"><t>0.007s</t><d></d></i>
      </list>
      """;

    List<FieldEntry> entries = Haxe3Xml.read(answer).fields();

    assertEquals(List.of(new MemberField("radius", "Float", ""),
                         new TypeField("StringMap", " Strings to values. "),
                         new PackageField("ds"),
                         new MetadataField("@:keep", "Keeps the field."),
                         new UnlistedField()), entries);
  }

  @Test
  @DisplayName("keeps a bare ampersand in position text")
  public void keepsABareAmpersandInPositionText() throws Exception {
    String answer = "<list>\n<pos>C:\\work\\r&d demo\\src\\Main.hx:3: characters 2-6</pos>\n</list>\n";

    List<String> positions = Haxe3Xml.read(answer).positions();

    assertEquals(List.of("C:\\work\\r&d demo\\src\\Main.hx:3: characters 2-6"), positions);
  }

  @Test
  @DisplayName("reads every root of a multi-root answer")
  public void readsEveryRootOfAMultiRootAnswer() throws Exception {
    String answer = """
      <type d=" Number form. ">
      value : Int -&gt; String
      </type>
      <type d=" Text form. ">
      value : String -&gt; String
      </type>
      """;

    List<TypeEntry> types = Haxe3Xml.read(answer).types();

    assertEquals(2, types.size());
    assertEquals("value : String -> String", types.get(1).type().strip());
  }

  @Test
  @DisplayName("reads toplevel entries with a multi-line doc attribute")
  public void readsToplevelEntriesWithAMultiLineDocAttribute() throws Exception {
    String answer = """
      <il>
      <i k="local" t="Int">count</i>
      <i k="type" p="haxe.Json" d="
      	Json encoding.
      ">Json</i>
      </il>
      """;

    List<ToplevelEntry> entries = Haxe3Xml.read(answer).toplevelEntries();

    assertEquals(List.of(new LocalToplevel("Int", "count"), new TypeToplevel("haxe.Json", "\n\tJson encoding.\n", "Json")), entries);
  }

  @Test
  @DisplayName("reads an empty list as no entries")
  public void readsAnEmptyListAsNoEntries() throws Exception {
    Answer answer = Haxe3Xml.read("<list>\n</list>\n");

    assertTrue(answer.fields().isEmpty());
    assertTrue(answer.positions().isEmpty());
  }

  @Test
  @DisplayName("fails on a payload that is not xml as a malformed payload")
  public void failsOnAPayloadThatIsNotXmlAsAMalformedPayload() {
    assertThrows(MalformedPayloadException.class, () -> Haxe3Xml.read("<list><i n=\"broken\""));
  }
}
