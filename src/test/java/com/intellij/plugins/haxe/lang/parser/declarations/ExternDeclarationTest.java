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
package com.intellij.plugins.haxe.lang.parser.declarations;

import com.intellij.core.CoreInjectedLanguageManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.mock.MockDumbService;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.plugins.haxe.lang.RegexLanguageInjector;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

public class ExternDeclarationTest extends DeclarationTestBase {
  public ExternDeclarationTest() {
    super("extern");
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    // Work around @NotNull bug down in the test fixture.  Since no InjectedLanguageManager
    // was registered, null was passed to a @NotNull function.  This affected testSimple().
    registerExtensionPoint(getExtensionArea(getProject()), MockMultiHostInjector.MULTIHOST_INJECTOR_EP_NAME, MockMultiHostInjector.class);
    registerExtensionPoint(getExtensionArea(null), RegexLanguageInjector.EXTENSION_POINT_NAME,
                           RegexLanguageInjector.class); // Might as well use the real one.
    registerInjectedLanguageManager();
    // End workaround.
  }

  private static ExtensionsAreaImpl getExtensionArea(@Nullable("null means root") AreaInstance areaInstance) {
    ExtensionsArea area = Extensions.getArea(areaInstance);
    assert area instanceof ExtensionsAreaImpl : "Unexpected return type from Extensions.getArea()";
    return (ExtensionsAreaImpl)area;
  }

  private void registerInjectedLanguageManager() {
    getProject().registerService(DumbService.class, MockDumbService.class);
    getProject().registerService(InjectedLanguageManager.class, CoreInjectedLanguageManager.class);
  }

  @Test
  public void testSimple() throws Throwable {
    doTest(true);
    HaxeFile file = (HaxeFile)myFile;
    assertNotNull(file);
    PsiClass[] psiClasses = file.getClasses();
    assertEquals(1, psiClasses.length);
    AbstractHaxePsiClass psiClass = (AbstractHaxePsiClass)psiClasses[0];
    assertTrue(psiClass.isExtern());
  }

  @Test
  public void testInterface() throws Throwable {
    doTest(true);
  }

  @Test
  public void testFinal() throws Throwable {
    doTest(true);
  }

  @Test
  public void testExternInline() throws Throwable {
    doTest(true);
  }
}
