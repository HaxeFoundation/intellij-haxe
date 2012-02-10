package com.intellij.plugins.haxe.lang.completion;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author: Fedor.Korotkov
 */
public class CompletionTestSuite {
  public static Test suite() {
    final TestSuite testSuite = new TestSuite();
    testSuite.addTestSuite(KeywordCompletionTest.class);
    return testSuite;
  }
}
