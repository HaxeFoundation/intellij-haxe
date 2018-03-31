/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.index.HaxeSymbolIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

public class HaxeSymbolContributorTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/indexers/symbols/";
  }

  public HaxeSymbolContributorTest() {}

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
      if(line.length() > 0) {
        includeLines.add(line);
      }
    }
    checkSymbols(includeLines);
  }

  protected void checkSymbols(List<String> list) {
    String[] symbols = HaxeSymbolIndex.getAllSymbols(GlobalSearchScope.projectScope(myFixture.getProject()));
    list.removeAll(Arrays.asList(symbols));
    if (!list.isEmpty()) {
      System.out.println("Symbols list:");
      for (String s : symbols) {
        System.out.println(s);
      }
      assertTrue("Missing symbols: " + list, list.isEmpty());
    }
  }

  public void testBasicSymbols() throws Throwable {
    doTest();
  }
}