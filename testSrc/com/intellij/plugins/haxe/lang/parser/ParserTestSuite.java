package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.lang.parser.declarations.DeclarationTestSuite;
import com.intellij.plugins.haxe.lang.parser.expressions.ExpressionTest;
import com.intellij.plugins.haxe.lang.parser.statements.StatementTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author fedor.korotkov
 */
public class ParserTestSuite {
  public static Test suite() {
    final TestSuite testSuite = new TestSuite();
    testSuite.addTest(DeclarationTestSuite.suite());
    testSuite.addTest(StatementTestSuite.suite());
    testSuite.addTestSuite(ExpressionTest.class);
    return testSuite;
  }
}
