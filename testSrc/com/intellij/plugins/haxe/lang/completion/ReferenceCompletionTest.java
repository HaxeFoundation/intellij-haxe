/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.completion;


/**
 * @author: Fedor.Korotkov
 */
public class ReferenceCompletionTest extends HaxeCompletionTestBase {
  public ReferenceCompletionTest() {
    super("completion", "references");
  }

  public void testTest1() throws Throwable {
    doTest();
  }

  public void testTest2() throws Throwable {
    doTest();
  }

  public void testTest3() throws Throwable {
    doTest();
  }

  public void testTest4() throws Throwable {
    doTest();
  }

  public void testTest5() throws Throwable {
    doTest();
  }

  public void testTest6() throws Throwable {
    doTest();
  }

  public void testTest7() throws Throwable {
    doTest();
  }

  public void testTest8() throws Throwable {
    doTest();
  }

  public void testTest9() throws Throwable {
    doTest();
  }

  public void testSelfMethod() throws Throwable {
    doTest();
  }

  public void testThisMembers() throws Throwable {
    doTest();
  }

  public void testClassName() throws Throwable {
    myFixture.configureByFiles("ClassName.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("ClassName.txt");
  }
  public void testClassName2() throws Throwable {
    doTest();
  }

  public void testImportInStatement() throws Throwable {
    myFixture.configureByFiles("ImportInStatement.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("ImportInStatement.txt");
  }

  public void testPackageCompletionInPackageStatement1() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    configureFileByText("Baz.hx", "package <caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "com");
  }

  public void testPackageCompletionInPackageStatement2() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "package com.b<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "bar", "baz");
  }

  public void testPackageCompletionInImportStatement1() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "import <caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "com");
  }

  public void testPackageCompletionInImportStatement2() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "import com.b<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "bar", "baz");
  }

  public void testPackageCompletionInImportStatement3() {
    myFixture.addFileToProject("com/foo/Bar.hx", "package com.foo;\nclass Bar {}");
    myFixture.addFileToProject("com/foo/Baz.hx", "package com.foo;\nclass Baz {}");
    configureFileByText("Foo.hx", "import com.foo.B<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "Bar", "Baz");
  }

  public void testPrivateMethod() throws Throwable {
    myFixture.configureByFiles("PrivateMethod.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PrivateMethod.txt");
  }

  public void testPublicGetter() throws Throwable {
    myFixture.configureByFiles("PublicGetter.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PublicGetter.txt");
  }

  public void testSelfPrivateMethod() throws Throwable {
    doTest();
  }

  public void testStdType1() throws Throwable {
    myFixture.configureByFiles("StdType1.hx", "std/String.hx");
    doTestVariantsInner("StdType1.txt");
  }

  public void testStdType2() throws Throwable {
    myFixture.configureByFiles("StdType2.hx", "std/String.hx", "std/Array.hx");
    doTestVariantsInner("StdType2.txt");
  }

  public void testUsingUtil1() throws Throwable {
    myFixture.configureByFiles("UsingUtil1.hx", "com/util/MathUtil.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("UsingUtil1.txt");
  }

  public void testUsingUtil2() throws Throwable {
    myFixture.configureByFiles("UsingUtil2.hx", "com/util/MathUtil.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("UsingUtil2.txt");
  }

  //https://github.com/TiVo/intellij-haxe/issues/28
  public void testTypedefOptionalField() throws Throwable {
    myFixture.configureByFiles("TypedefOptionalField.hx");
    doTestVariantsInner("TypedefOptionalField.txt");
  }

  //https://github.com/TiVo/intellij-haxe/issues/262
  public void testStaticMember() throws Throwable {
    doTest();
  }

  //https://github.com/TiVo/intellij-haxe/issues/262
  public void testStaticField() throws Throwable {
    doTest();
  }

  public void testAutodetectMethod() throws Throwable {
    doTestInclude();
  }

  public void testAutodetectMethod2() throws Throwable {
    doTestInclude();
  }

  public void testAutodetectProperties() throws Throwable {
    doTestInclude();
  }

  public void testAbstractThisSemantics() throws Throwable {
    doTestInclude();
  }

  public void testStringCode() throws Throwable {
    doTestInclude();
  }

  // @TODO: Temporarily disabled. Not being recognized for an unknown reason.
  /*
  public void testExtensionMethod1() throws Throwable {
    doTestInclude("ExtensionMethodExt.hx");
  }
  */
}
