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

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.LineSeparator;
import org.apache.sanselan.util.IOUtils;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeCompletionTestBase extends HaxeCodeInsightFixtureTestCase {
  enum CheckType {EQUALS, INCLUDES, EXCLUDES}

  private final String myPath;

  public HaxeCompletionTestBase(String... path) {
    myPath = getPath(path);
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      result.append("/");
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getBasePath() {
    return myPath;
  }

  protected PsiFile configureFileByText(String fname, String text) {
    String lineSeparator = LineSeparator.getSystemLineSeparator().getSeparatorString();
    PsiFile newFile = myFixture.configureByText(fname, text);
    newFile.getVirtualFile().setDetectedLineSeparator(lineSeparator);
    return newFile;
  }

  protected void doTest() throws Throwable {
    myFixture.configureByFile(getTestName(false) + ".hx");
    doTestVariantsInner(getTestName(false) + ".txt");
  }

  protected void doTest(char charToType) {
    myFixture.configureByFile(getTestName(false) + ".hx");
    myFixture.type(charToType);
    myFixture.checkResultByFile(getTestName(false) + ".txt");
  }

  protected void doTestInclude(String... extraFiles) throws Throwable {
    final List<String> files = new ArrayList<String>();
    files.add(getTestName(false) + ".hx");
    Collections.addAll(files, extraFiles);
    myFixture.configureByFiles(files.toArray(new String[0]));
    final VirtualFile virtualFile = myFixture.copyFileToProject(getTestName(false) + ".txt");
    String text = new String(virtualFile.contentsToByteArray());
    List<String> includeLines = new ArrayList<String>();
    List<String> excludeLines = new ArrayList<String>();
    boolean include = true;
    for (String line : text.split("\n")) {
      line = line.trim();
      if (line.equals(":INCLUDE")) {
        include = true;
      } else if (line.equals(":EXCLUDE")) {
        include = false;
      } else if (line.length() == 0) {

      } else {
        if (include) {
          includeLines.add(line);
        } else {
          excludeLines.add(line);
        }
      }
      //System.out.println(line);
    }
    //System.out.println(text);
    myFixture.complete(CompletionType.BASIC, 1);
    checkCompletion(CheckType.INCLUDES, includeLines);
    checkCompletion(CheckType.EXCLUDES, excludeLines);
  }

  protected void doTestVariantsInner(String fileName) throws Throwable {
    final VirtualFile virtualFile = myFixture.copyFileToProject(fileName);
    final Scanner in = new Scanner(virtualFile.getInputStream());

    final CompletionType type = CompletionType.valueOf(in.next());
    final int count = in.nextInt();
    final CheckType checkType = CheckType.valueOf(in.next());

    final List<String> variants = new ArrayList<String>();
    while (in.hasNext()) {
      final String variant = StringUtil.strip(in.next(), CharFilter.NOT_WHITESPACE_FILTER);
      if (variant.length() > 0) {
        variants.add(variant);
      }
    }

    myFixture.complete(type, count);
    checkCompletion(checkType, variants);
  }

  protected void checkCompletion(CheckType checkType, String... variants) {
    checkCompletion(checkType, new ArrayList<String>(Arrays.asList(variants)));
  }

  protected void checkCompletion(CheckType checkType, List<String> variants) {
    List<String> stringList = myFixture.getLookupElementStrings();
    LookupElement[] elements = myFixture.getLookupElements();

    if (stringList == null) {
      stringList = Collections.emptyList();
    }

    if (elements == null) {
      elements = new LookupElement[0];
    }

    for (LookupElement element : elements) {
      PsiElement element1 = element.getPsiElement();
      if (element1 instanceof NavigationItem) {
        //System.out.println(((NavigationItem)element1).getPresentation().getPresentableText());
        stringList.add(((NavigationItem)element1).getPresentation().getPresentableText());
      }
    }

    /*
    System.out.println(variants.size());
    System.out.println(variants);
    System.out.println(stringList.size());
    System.out.println(stringList);
    */

    if (checkType == CheckType.EQUALS) {
      UsefulTestCase.assertSameElements(stringList, variants);
    }
    else if (checkType == CheckType.INCLUDES) {
      variants.removeAll(stringList);
      if (!variants.isEmpty()) {
        System.out.println("Completion list:");
        for (String s : stringList) {
          System.out.println(s);
        }
      }
      assertTrue("Missing variants: " + variants, variants.isEmpty());
    }
    else if (checkType == CheckType.EXCLUDES) {
      variants.retainAll(stringList);
      if (!variants.isEmpty()) {
        System.out.println("Completion list:");
        for (String s : stringList) {
          System.out.println(s);
        }
      }
      assertTrue("Unexpected variants: " + variants, variants.isEmpty());
    }
  }
}
