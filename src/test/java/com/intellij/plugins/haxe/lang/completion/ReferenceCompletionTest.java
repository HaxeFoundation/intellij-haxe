/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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


import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class ReferenceCompletionTest extends HaxeCompletionTestBase {
  public ReferenceCompletionTest() {
    super("completion", "references");
  }

  @Test
  public void testTest1() throws Throwable {
    doTest();
  }

  @Test
  public void testTest2() throws Throwable {
    doTest();
  }

  @Test
  public void testTest3() throws Throwable {
    doTest();
  }

  @Test
  public void testTest4() throws Throwable {
    doTest();
  }

  @Test
  public void testTest5() throws Throwable {
    doTest();
  }

  @Test
  public void testTest6() throws Throwable {
    doTest();
  }

  @Test
  public void testTest7() throws Throwable {
    doTest();
  }

  @Test
  public void testTest8() throws Throwable {
    doTest();
  }

  @Test
  public void testTest9() throws Throwable {
    doTest();
  }

  @Test
  public void testSelfMethod() throws Throwable {
    doTest();
  }

  @Test
  public void testThisMembers() throws Throwable {
    doTest();
  }

  @Test
  public void testSuperMembers() throws Throwable {
    doTest();
  }

  @Test
  public void testClassName() throws Throwable {
    myFixture.configureByFiles("ClassName.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("ClassName.txt");
  }

  @Test
  public void testClassName2() throws Throwable {
    doTest();
  }

  @Test
  public void testImportInStatement() throws Throwable {
    myFixture.configureByFiles("ImportInStatement.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("ImportInStatement.txt");
  }

  @Test
  public void testImportStatic() throws Throwable {
    myFixture.configureByFiles("ImportStaticStatement.hx", "com/util/StringUtil.hx");
    doTestVariantsInner("ImportStaticStatement.txt");
  }

  @Test
  public void testPackageCompletionInPackageStatement1() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    configureFileByText("Baz.hx", "package <caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "com");
  }

  @Test
  public void testPackageCompletionInPackageStatement2() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "package com.b<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "bar", "baz");
  }

  @Test
  public void testPackageCompletionInImportStatement1() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "import <caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "com");
  }

  @Test
  public void testPackageCompletionInImportStatement2() {
    myFixture.addFileToProject("com/bar/Bar.hx", "");
    myFixture.addFileToProject("com/baz/Baz.hx", "");
    configureFileByText("Foo.hx", "import com.b<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "bar", "baz");
  }

  @Test
  public void testPackageCompletionInImportStatement3() {
    myFixture.addFileToProject("com/foo/Bar.hx", "package com.foo;\nclass Bar {}");
    myFixture.addFileToProject("com/foo/Baz.hx", "package com.foo;\nclass Baz {}");
    configureFileByText("Foo.hx", "import com.foo.B<caret>");
    myFixture.completeBasic();
    checkCompletion(CheckType.INCLUDES, "Bar", "Baz");
  }

  @Test
  public void testPrivateMethod() throws Throwable {
    myFixture.configureByFiles("PrivateMethod.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PrivateMethod.txt");
  }

  @Test
  public void testPublicGetter() throws Throwable {
    myFixture.configureByFiles("PublicGetter.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PublicGetter.txt");
  }

  @Test
  public void testSelfPrivateMethod() throws Throwable {
    doTest();
  }

  @Test
  public void testStdType1() throws Throwable {
    myFixture.configureByFiles("StdType1.hx", "std/String.hx");
    doTestVariantsInner("StdType1.txt");
  }

  @Test
  public void testStdType2() throws Throwable {
    myFixture.configureByFiles("StdType2.hx", "std/String.hx", "std/Array.hx");
    doTestVariantsInner("StdType2.txt");
  }

  @Test
  public void testUsingUtil1() throws Throwable {
    myFixture.configureByFiles("UsingUtil1.hx", "com/util/MathUtil.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("UsingUtil1.txt");
  }

  @Test
  public void testUsingUtil2() throws Throwable {
    myFixture.configureByFiles("UsingUtil2.hx", "com/util/MathUtil.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("UsingUtil2.txt");
  }

  @Test
  public void testUsingUtil3() throws Throwable {
    myFixture.configureByFiles("UsingUtil3.hx", "com/util/Tools.hx", "com/util/StringUtil.hx", "com/util/MathUtil.hx", "std/String.hx",
                               "std/StdTypes.hx");
    doTestVariantsInner("UsingUtil3.txt");
  }

  //https://github.com/TiVo/intellij-haxe/issues/28
  @Test
  public void testTypedefOptionalField() throws Throwable {
    myFixture.configureByFiles("TypedefOptionalField.hx");
    doTestVariantsInner("TypedefOptionalField.txt");
  }

  //https://github.com/TiVo/intellij-haxe/issues/262
  @Test
  public void testStaticMember() throws Throwable {
    doTest();
  }

  //https://github.com/TiVo/intellij-haxe/issues/262
  @Test
  public void testStaticField() throws Throwable {
    doTest();
  }

  @Test
  public void testMethodLocalVar() throws Throwable {
    doTestInclude();
  }

  @Test
  public void testMethodArg() throws Throwable {
    doTestInclude();
  }

// Auto-detection is too slow in medium to large code bases (e.g. HashLink),
// so it was (temporarily?) removed in favor of showing only specific tagged types.
//
// @Test   public void testAutodetectMethod() throws Throwable {
//    doTestInclude();
//  }
//
// @Test   public void testAutodetectMethod2() throws Throwable {
//    myFixture.configureByFiles("StdType2.hx", "std/String.hx", "std/Array.hx");
//    doTestInclude();
//  }
//
// @Test   public void testAutodetectConstant() throws Throwable {
//    doTestInclude();
//  }
//
// @Test   public void testAutodetectProperties() throws Throwable {
//    doTestInclude();
//  }
//
// @Test   public void testAbstractThisSemantics() throws Throwable {
//    doTestInclude();
//  }

  @Test
  public void testStringCode() throws Throwable {
    doTestInclude();
  }

  @Test
  public void testLocalTypedef() throws Throwable {
    myFixture.configureByFiles("com/testing/LocalTypedef.hx", "com/util/Bar.hx");
    doTestVariantsInner("com/testing/LocalTypedef.txt");
  }

  @Test
  public void testGenericFromSuperPackage() throws Throwable {
    myFixture.configureByFiles("generic1/clients/Client.hx", "generic1/GenericInSuperPackage.hx");
    doTestVariantsInner("generic1/clients/Client.txt");
  }

  @Test
  public void testLowerCase() throws Throwable {
    myFixture.configureByFiles("LowerCase.hx", "com/util/Bar.hx", "std/String.hx", "std/Array.hx", "std/StdTypes.hx");
    doTestVariantsInner("LowerCase.txt");
  }

  @Test
  public void testRootPackageName() throws Throwable {
    myFixture.configureByFiles("com/testing/RootPackageName.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("com/testing/RootPackageName.txt");
    myFixture.configureByFiles("com/testing/RootPackageName2.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("com/testing/RootPackageName2.txt");
    myFixture.configureByFiles("com/testing/RootPackageName3.hx", "com/testing/subs/PackageData.hx", "std/String.hx", "std/StdTypes.hx");
    doTestVariantsInner("com/testing/RootPackageName3.txt");
  }

  @Test
  public void testAnonymousExtends() throws Throwable {
    doTestInclude();
  }

  @Test
  public void testAnonymousChain() throws Throwable {
    doTestInclude();
  }

  @Test
  public void testAnonymousGenericChain() throws Throwable {
    myFixture.configureByFiles("std/String.hx");
    doTestInclude();
  }

  @Test
  public void testAnonymousIterator() throws Throwable {
    myFixture.configureByFiles("std/String.hx", "std/StdTypes.hx");
    doTestInclude();
  }

  @Test
  public void testGenericInMap() throws Throwable {
    myFixture.configureByFiles("std/String.hx", "std/StdTypes.hx");
    doTestInclude();
  }

  @Test
  public void testGenericAnonymousFieldInMap() throws Throwable {
    myFixture.configureByFiles("std/String.hx", "std/StdTypes.hx");
    doTestInclude();
  }

  @Test
  public void testGenericToGenericReference() throws Throwable {
    myFixture.configureByFiles("std/String.hx", "std/StdTypes.hx");
    doTestInclude();
  }

  // FIXME Generic params declared by methods must be considered to make this test works.
  //public void testGenericToGenericInnerReference() throws Throwable {
  //  myFixture.configureByFiles("std/String.hx", "std/StdTypes.hx");
  //  doTestInclude();
  //}

  @Test
  public void testImportGenericSubType() throws Throwable {
    myFixture.configureByFiles("ImportGenericSubType.hx", "generic1/ClassWithGenericSubClass.hx", "std/StdTypes.hx");
    doTestVariantsInner("ImportGenericSubType.txt");
  }

  @Test
  public void testNullTypedef() throws Throwable {
    myFixture.configureByFiles("NullTypedef.hx", "std/StdTypes.hx", "std/String.hx");
    doTestVariantsInner("NullTypedef.txt");
  }

  @Test
  public void testRefTypedef() throws Throwable {
    myFixture.configureByFiles("RefTypedef.hx", "std/String.hx");
    doTestVariantsInner("RefTypedef.txt");
  }

  @Test
  public void testAbstractEnumFields() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testAbstractEnumFields2() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testAbstractEnumFields3() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testAbstractForward() throws Throwable {
    doTestInclude("com/util/UnderlyingType.hx");
  }

  @Test
  public void testAbstractForward1() throws Throwable {
    doTestInclude("com/util/UnderlyingType.hx");
  }

  @Test
  public void testAbstractForward2() throws Throwable {
    doTestInclude("com/util/UnderlyingType.hx");
  }

  @Test
  public void testAbstractForward3() throws Throwable {
    doTestInclude("com/util/UnderlyingType.hx");
  }

  @Test
  public void testAbstractForward4() throws Throwable {
    doTestInclude("std/Array.hx");
  }

  @Test
  public void testAbstractForward5() throws Throwable {
    doTestInclude("std/Array.hx");
  }

  @Test
  public void testAbstractViaNull() throws Throwable {
    doTestInclude("std/StdTypes.hx", "std/String.hx");
  }

  @Test
  public void testAbstractWithGenericUnderlyingType() throws Throwable {
    // Issue #772
    doTestInclude("std/StdTypes.hx", "std/String.hx");
  }

  //public void testUsingStringTools() throws Throwable {
  //  myFixture.configureByFiles("UsingStringTools.hx", "std/StringTools.hx", "std/String.hx", "std/StdTypes.hx");
  //  doTestVariantsInner("UsingStringTools.txt");
  //}

  // @TODO: Temporarily disabled. Not being recognized for an unknown reason.
  /*
 @Test   public void testExtensionMethod1() throws Throwable {
    doTestInclude("ExtensionMethodExt.hx");
  }
  */

  @Test
  public void testExtensions1() throws Throwable {
    myFixture.configureByFiles("Extensions1.hx", "extensions/Stuff.hx");
    doTestVariantsInner("Extensions1.txt");
  }

  @Test
  public void testExtensions2() throws Throwable {
    myFixture.configureByFiles("Extensions2.hx", "extensions/Stuff.hx");
    doTestVariantsInner("Extensions2.txt");
  }

  @Test
  public void testExtensions3() throws Throwable {
    myFixture.configureByFiles("Extensions3.hx", "extensions/Stuff.hx");
    doTestVariantsInner("Extensions3.txt");
  }

  @Test
  public void testExtensions4() throws Throwable {
    myFixture.configureByFiles("Extensions4.hx", "extensions/Stuff.hx");
    doTestVariantsInner("Extensions4.txt");
  }

  @Test
  public void testExtensions5() throws Throwable {
    myFixture.configureByFiles("Extensions5.hx", "extensions/Stuff.hx");
    doTestVariantsInner("Extensions5.txt");
  }

  @Test
  public void testForLoopVariable1() throws Throwable {
    doTest();
  }

  @Test
  public void testForLoopVariable2() throws Throwable {
    doTest();
  }

  @Test
  public void testForLoopVariable3() throws Throwable {
    doTestInclude("std/StdTypes.hx", "std/Array.hx");
  }

  @Test
  public void testForLoopVariable4() throws Throwable {
    doTest();
  }


  @Test
  public void testNonQualifiedAbstractEnumFields() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testNonQualifiedAbstractEnumFields2() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testNonQualifiedAbstractEnumFields3() throws Throwable {
    doTestInclude("com/util/SampleAbstractEnum.hx");
  }

  @Test
  public void testInnerEnum() throws Throwable {
    doTest();
  }

  @Test
  public void testParenthesizedExpression() throws Throwable {
    doTestInclude("std/StdTypes.hx", "std/String.hx");
  }

  @Test
  public void testTypeParameterFromArgument() throws Throwable {
    doTestInclude("std/StdTypes.hx", "std/Array.hx", "std/Vector.hx", "std/String.hx");
  }
}
