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
package com.intellij.plugins.haxe.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Navigation: go to implementation")
public class HaxeGoToImplementationTest extends HaxeLightFixtureTestCase {
  // two matching overrides (js, neko) plus one whose package statement makes
  // the qualified name differ (lua declares other.Rope) - it must not match
  private static final String[] STD_ROPE_FILES = {
    "std/StdTypes.hx", "std/Rope.hx", "std/js/_std/Rope.hx", "std/neko/_std/Rope.hx", "std/lua/_std/Rope.hx"};

  @Override
  protected String getBasePath() {
    return "/gotoImplementation/";
  }

  private void doTest(int expectedLength, String... extraFiles) throws Throwable {
    List<String> files = new ArrayList<>();
    files.add(getTestName(false) + ".hx");
    Collections.addAll(files, extraFiles);
    myFixture.configureByFiles(files.toArray(new String[0]));
    GotoTargetHandler.GotoData data = CodeInsightTestUtil.gotoImplementation(myFixture.getEditor(), myFixture.getFile());

    assertNotNull(data, myFixture.getFile().toString());
    // TODO: listen updater task?
    assertEquals(expectedLength, data.targets.length, () -> "targets: " + Arrays.toString(data.targets));
  }

  @Test
  @DisplayName("gti 1 - interface to implementing class")
  public void testGti1() throws Throwable {
    doTest(2);
  }

  @Test
  @DisplayName("gti 2 - interface method to implementation")
  public void testGti2() throws Throwable {
    doTest(1);
  }

  @Test
  @DisplayName("gti 3 - interface with multiple implementing classes")
  public void testGti3() throws Throwable {
    doTest(2);
  }

  @Test
  @DisplayName("gti 4 - interface field to implementations")
  public void testGti4() throws Throwable {
    doTest(2);
  }

  @Test
  @DisplayName("extern std class to target overrides")
  public void testExternStdClass() throws Throwable {
    // the platform lists the queried extern itself, plus the js and neko overrides; lua's
    // same-named type declares another package and must not appear
    doTestTargetFiles("std/Rope.hx", "std/js/_std/Rope.hx", "std/neko/_std/Rope.hx");
  }

  @Test
  @DisplayName("extern std member to target overrides")
  public void testExternStdMember() throws Throwable {
    doTestTargetFiles("std/Rope.hx", "std/js/_std/Rope.hx", "std/neko/_std/Rope.hx");
  }

  @Test
  @DisplayName("extern outside std has no overrides")
  public void testExternOutsideStd() throws Throwable {
    // only the platform's self entry - no overrides offered for a non-std extern
    doTestTargetFiles("ExternOutsideStd.hx");
  }

  private void doTestTargetFiles(String... expectedFiles) throws Throwable {
    List<String> files = new ArrayList<>();
    files.add(getTestName(false) + ".hx");
    Collections.addAll(files, STD_ROPE_FILES);
    myFixture.configureByFiles(files.toArray(new String[0]));
    GotoTargetHandler.GotoData data = CodeInsightTestUtil.gotoImplementation(myFixture.getEditor(), myFixture.getFile());

    assertNotNull(data, myFixture.getFile().toString());
    List<String> targetFiles = Arrays.stream(data.targets)
      .map(HaxeGoToImplementationTest::sourceRootRelativePath)
      .sorted()
      .toList();
    assertEquals(Arrays.stream(expectedFiles).sorted().toList(), targetFiles);
  }

  private static String sourceRootRelativePath(PsiElement target) {
    String path = target.getContainingFile().getVirtualFile().getPath();
    // light-fixture files live under the temp source root ("/src/")
    return path.substring(path.indexOf("/src/") + "/src/".length());
  }

}
