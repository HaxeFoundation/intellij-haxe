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
package com.intellij.plugins.haxe.ide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.*;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

@DisplayName("Navigation: goto symbol contributor")
public class HaxeGotoSymbolContributorTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/indexers/symbols/";
  }

  public HaxeGotoSymbolContributorTest() {
  }

  protected void doTest(String... extraFiles) throws Throwable {
    final List<String> files = new ArrayList<String>();
    files.add(getTestName(false) + ".hx");
    Collections.addAll(files, extraFiles);
    myFixture.configureByFiles(files.toArray(new String[0]));
    final VirtualFile virtualFile = myFixture.copyFileToProject(getTestName(false) + ".txt");
    String text = new String(virtualFile.contentsToByteArray());

    List<String> includeLines = new ArrayList<String>();
    for (String line : text.split("\n")) {
      line = line.trim();
      if (!line.isEmpty()) {
        includeLines.add(line);
      }
    }
    checkSymbols(includeLines);
  }

  protected void checkSymbols(List<String> list) {
    final Set<String> symbolSet = new LinkedHashSet<>();
    StubIndex stubIndex = StubIndex.getInstance();

    symbolSet.addAll(HaxeClassNameUnifiedIndex.getAllKeys(myFixture.getProject()));

    for (String name : HaxeClassMethodNameUnifiedIndex.getAllKeys(myFixture.getProject())) {
      if (!"new".equals(name)) symbolSet.add(name);
    }
    symbolSet.addAll(HaxeModuleMethodNameUnifiedIndex.getAllKeys(myFixture.getProject()));
    symbolSet.addAll(HaxeStaticMethodNameUnifiedIndex.getAllKeys(myFixture.getProject()));

    symbolSet.addAll(HaxeClassFieldNameUnifiedIndex.getAllKeys(myFixture.getProject()));
    symbolSet.addAll(HaxeStaticFieldNameUnifiedIndex.getAllKeys(myFixture.getProject()));
    symbolSet.addAll(HaxeModuleFieldNameUnifiedIndex.getAllKeys(myFixture.getProject()));

    String[] symbols = ArrayUtil.toStringArray(symbolSet);
    list.removeAll(Arrays.asList(symbols));
    if (!list.isEmpty()) {
      System.out.println("Symbols list:");
      for (String s : symbols) {
        System.out.println(s);
      }
      assertTrue(list.isEmpty(), "Missing symbols: " + list);
    }
  }

  @Test
  @DisplayName("basic symbols")
  public void testBasicSymbols() throws Throwable {
    doTest();
  }
}