package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.lang.completion.CompletionTestSuite;
import com.intellij.plugins.haxe.lang.parser.ParserTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author: Fedor.Korotkov
 */
public class LanguageTestSuite {
  public static Test suite() {
    final TestSuite testSuite = new TestSuite();
    testSuite.addTest(ParserTestSuite.suite());
    testSuite.addTest(CompletionTestSuite.suite());
    testSuite.addTestSuite(HaxeFormatterTest.class);
    return testSuite;
  }
}
