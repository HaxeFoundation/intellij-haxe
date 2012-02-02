package com.intellij.plugins.haxe.lang.parser.declarations;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author fedor.korotkov
 */
public class DeclarationTestSuite {
  public static Test suite() {
    final TestSuite testSuite = new TestSuite();
    testSuite.addTestSuite(ClassDeclarationTest.class);
    testSuite.addTestSuite(FunctionDeclarationTest.class);
    testSuite.addTestSuite(ImportDeclarationTest.class);
    testSuite.addTestSuite(PackageDeclarationTest.class);
    testSuite.addTestSuite(VarOrConstDeclarationTest.class);
    testSuite.addTestSuite(EnumDeclarationTest.class);
    testSuite.addTestSuite(TypedefDeclarationTest.class);
    return testSuite;
  }
}
