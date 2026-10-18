package com.intellij.plugins.haxe.v2.buildsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: project xml escaping")
public class ProjectXmlEscapingTest {

  /** (project xml text, the text a conforming parser accepts). */
  static final List<Arguments> ESCAPES = List.of(
    arguments("<log if=\"${haxe_ver < 4.3}\"/>", "<log if=\"${haxe_ver &lt; 4.3}\"/>"),
    arguments("<set unless=\"a && b\"/>", "<set unless=\"a &amp;&amp; b\"/>"),
    arguments("<log info='Use <app/> first'/>", "<log info='Use &lt;app/> first'/>"),
    // references stay; a quote of the other kind does not end the value
    arguments("<log value=\"&lt;a&gt; &#60; it's\"/>", "<log value=\"&lt;a&gt; &#60; it's\"/>"),
    // only attribute values change
    arguments("<!-- if=\"a < b\" --><a>x</a>", "<!-- if=\"a < b\" --><a>x</a>"),
    arguments("<?xml version=\"1.0\"?><a b=\"c\"/>", "<?xml version=\"1.0\"?><a b=\"c\"/>"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("ESCAPES")
  @DisplayName("escapes what xml forbids in attribute values")
  public void escapesWhatXmlForbidsInAttributeValues(String text, String escaped) {
    assertEquals(escaped, ProjectXmlEscaping.escapeAttributeValues(text));
  }
}
