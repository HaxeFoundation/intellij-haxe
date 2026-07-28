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
package com.intellij.plugins.haxe.lang.parser.declarations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Created by as3boyan on 09.08.14.
 */
@DisplayName("Parsing: property declaration")
public class PropertyDeclarationTest extends DeclarationTestBase {
  public PropertyDeclarationTest() {
    super("property");
  }

  @Test
  @DisplayName("property")
  public void testProperty() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("static initializer")
  public void testStaticInitializer() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("static initializer without semicolon bare")
  public void testStaticInitializerWithoutSemicolonBare() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("static initializer without semicolon")
  public void testStaticInitializerWithoutSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("missing semicolon")
  public void testMissingSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("final meta on property")
  public void testFinalMetaOnProperty() throws Throwable {
    doTest(true);
  }
}
