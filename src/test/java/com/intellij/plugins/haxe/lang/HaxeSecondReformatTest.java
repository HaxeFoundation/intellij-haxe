package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatDefaultProfile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The platform's second reformat: a reformat that kept custom line breaks
 * (KEEP_LINE_BREAKS) reports them, and repeating it offers to drop them by
 * rerunning with the flag off. These tests cover both passes under the
 * hxformat defaults profile.
 */
@DisplayName("Formatting: second reformat")
public class HaxeSecondReformatTest extends HaxeLightFixtureTestCase {
  // a short array literal joins on either pass under the profile's item
  // rules (the tool's noWrap for items totalling at most 80), as do call
  // arguments under its fill; a kept break stays only where no rule decides
  private static final String WRAPPED_ARRAY_SOURCE = """
    class Main {
    	function draw(s:Style) {
    		var stops = [
    			s.first,
    			s.second,
    			s.third
    		];
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  @Test
  @DisplayName("haxe takes part in the second reformat flow")
  public void testHaxeTakesPartInTheSecondReformatFlow() {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(HaxeLanguage.INSTANCE);

    assertTrue(provider.usesCommonKeepLineBreaks(), "the platform only records kept breaks for languages that opt in");
  }

  @Test
  @DisplayName("short array joins on either pass")
  public void testShortArrayJoinsOnEitherPass() {
    String joined = """
      class Main {
      	function draw(s:Style) {
      		var stops = [s.first, s.second, s.third];
      	}
      }
      """;

    String firstPass = reformat(HxformatDefaultProfile::apply, WRAPPED_ARRAY_SOURCE);
    String secondPass = reformat(HaxeSecondReformatTest::secondPass, WRAPPED_ARRAY_SOURCE);

    assertEquals(joined, firstPass);
    assertEquals(joined, secondPass);
  }

  @Test
  @DisplayName("second pass rewraps a joined line past the margin")
  public void testSecondPassRewrapsAJoinedLinePastTheMargin() {
    String source = """
      class Main {
      	function copy():Options {
      		return new Options(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue,
      			ninthValue,
      			tenthValue,
      			eleventhValue,
      			twelfthValue);
      	}
      }
      """;

    String formatted = reformat(HaxeSecondReformatTest::secondPass, source);

    assertEquals("""
      class Main {
      	function copy():Options {
      		return new Options(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue,
      			eleventhValue, twelfthValue);
      	}
      }
      """, formatted);
  }

  @Test
  @DisplayName("second pass keeps object literal and anonymous type layouts")
  public void testSecondPassKeepsObjectLiteralAndAnonymousTypeLayouts() {
    String source = """
      typedef Base = {
      	var id:Int;
      	var name:String;
      }

      class Main {
      	function make() {
      		var point = {
      			x: 1,
      			y: 2
      		};
      		var values = [
      			1,
      			2
      		];
      	}
      }
      """;

    String formatted = reformat(HaxeSecondReformatTest::secondPass, source);

    assertEquals("""
      typedef Base = {
      	var id:Int;
      	var name:String;
      }

      class Main {
      	function make() {
      		var point = {
      			x: 1,
      			y: 2
      		};
      		var values = [1, 2];
      	}
      }
      """, formatted);
  }

  @Test
  @DisplayName("second pass keeps next line braces on a switch")
  public void testSecondPassKeepsNextLineBracesOnASwitch() {
    String source = """
      class Main {
      	function pick(type:Int) {
      		switch (type)
      		{
      			case 1:
      				trace(1);
      		}
      	}
      }
      """;

    String formatted = reformat(HaxeSecondReformatTest::allmanSecondPass, source);

    assertEquals("""
      class Main
      {
      	function pick(type:Int)
      	{
      		switch (type)
      		{
      			case 1:
      				trace(1);
      		}
      	}
      }
      """, formatted);
  }

  @Test
  @DisplayName("second pass leaves haxe-formatter output alone")
  public void testSecondPassLeavesHaxeFormatterOutputAlone() throws IOException {
    // the tool's own output holds no custom breaks: every break it kept is
    // one its rules produce, so dropping ours must reproduce it exactly
    List<Path> rules;
    try (Stream<Path> directories = Files.list(Path.of(getTestDataPath()))) {
      rules = directories
        .filter(Files::isDirectory)
        .filter(HaxeSecondReformatTest::usesDefaultConfig)
        .sorted()
        .toList();
    }

    List<String> changed = new ArrayList<>();
    for (Path rule : rules) {
      String formatted = Files.readString(rule.resolve("hxformat.hx")).replace("\r\n", "\n");
      String secondPass = reformat(HaxeSecondReformatTest::secondPass, formatted);
      if (!secondPass.strip().equals(formatted.strip())) changed.add(rule.getFileName().toString());
    }

    assertTrue(changed.isEmpty(), "fixtures the second pass changed: " + changed);
  }

  /** A fixture generated with the tool's defaults - no hxformat.json beside it. */
  private static boolean usesDefaultConfig(Path rule) {
    return !Files.exists(rule.resolve("hxformat.json"));
  }

  /** The hxformat defaults with custom line breaks dropped, as the platform reruns the reformat. */
  private static void secondPass(CodeStyleSettings settings) {
    HxformatDefaultProfile.apply(settings);
    settings.getCommonSettings(HaxeLanguage.INSTANCE).KEEP_LINE_BREAKS = false;
  }

  /** The second pass under the openfl-braces fixture config (lineEnds.leftCurly=both). */
  private static void allmanSecondPass(CodeStyleSettings settings) {
    secondPass(settings);
    HaxeCodeStyleTweaks.allmanBraces(settings);
  }
}
