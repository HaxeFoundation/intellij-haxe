package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Display protocol: haxe3 position")
public class Haxe3PositionTest {

  @Test
  @DisplayName("parses a character span after a drive path")
  public void parsesACharacterSpanAfterADrivePath() {
    Haxe3Position position = Haxe3Position.parse("C:\\work\\demo\\src\\Main.hx:7: characters 8-13");

    assertEquals(new Haxe3Position("C:\\work\\demo\\src\\Main.hx", 7, false, 8, 13), position);
  }

  @Test
  @DisplayName("parses a whole line span")
  public void parsesAWholeLineSpan() {
    Haxe3Position position = Haxe3Position.parse("/work/demo/src/Circle.hx:12: lines 12-14");

    assertEquals(new Haxe3Position("/work/demo/src/Circle.hx", 12, true, 12, 14), position);
  }

  @Test
  @DisplayName("character span turns byte columns into characters")
  public void characterSpanTurnsByteColumnsIntoCharacters() {
    Haxe3SourceText text = Haxe3SourceText.of("class A {}\nvar s = \"é\"; var t;\n");
    // "é" is two bytes: "t" starts at character 17, byte 18
    Haxe3Position position = Haxe3Position.parse("A.hx:2: characters 18-19");

    Range range = position.rangeIn(text);

    assertEquals(new Range(new Position(1, 17), new Position(1, 18)), range);
  }

  @Test
  @DisplayName("whole lines end at the last line's end without its line break")
  public void wholeLinesEndAtTheLastLinesEndWithoutItsLineBreak() {
    Haxe3SourceText text = Haxe3SourceText.of("class A {\r\n  function f() {\r\n  }\r\n}\r\n");
    Haxe3Position position = Haxe3Position.parse("A.hx:2: lines 2-3");

    Range range = position.rangeIn(text);

    assertEquals(new Range(new Position(1, 0), new Position(2, 3)), range);
  }

  @Test
  @DisplayName("text that is no position reads as null")
  public void textThatIsNoPositionReadsAsNull() {
    assertNull(Haxe3Position.parse("Error: No completion point was found"));
  }
}
