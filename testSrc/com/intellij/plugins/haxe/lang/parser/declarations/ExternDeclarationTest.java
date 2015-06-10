/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.lang.parser.declarations;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.psi.PsiClass;

public class ExternDeclarationTest extends DeclarationTestBase {
  public ExternDeclarationTest() {
    super("extern");
  }

  public void testSimple() throws Throwable {
    doTest(true);
    HaxeFile file = (HaxeFile)myFile;
    assertNotNull(file);
    PsiClass[] psiClasses = file.getClasses();
    assertTrue(psiClasses.length == 1);
    AbstractHaxePsiClass psiClass = (AbstractHaxePsiClass)psiClasses[0];
    assertTrue(psiClass.isExtern());
  }

  public void testInterface() throws Throwable {
    doTest(true);
  }
}
