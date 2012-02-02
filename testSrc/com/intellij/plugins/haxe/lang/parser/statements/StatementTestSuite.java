package com.intellij.plugins.haxe.lang.parser.statements;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author fedor.korotkov
 */
public class StatementTestSuite {
  public static Test suite() {
    final TestSuite testSuite = new TestSuite();
    testSuite.addTestSuite(IfTest.class);
    testSuite.addTestSuite(WhileTest.class);
    testSuite.addTestSuite(ForTest.class);
    testSuite.addTestSuite(ReturnTest.class);
    testSuite.addTestSuite(BreakTest.class);
    testSuite.addTestSuite(ContinueTest.class);
    testSuite.addTestSuite(ExceptionTest.class);
    testSuite.addTestSuite(SwitchTest.class);
    return testSuite;
  }
}
