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
package com.intellij.plugins.haxe.model;

import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class FqInfoTest {
  @Test
  public void testValid() throws Exception {
    assertEquals(
      "Simple package + class name",
      "FqInfo(hello:World:World)",
      Objects.toString(FqInfo.parse("hello.World"))
    );
    assertEquals(
      "Composed package + class name",
      "FqInfo(hello.oh.my:World:World)",
      Objects.toString(FqInfo.parse("hello.oh.my.World"))
    );
    assertEquals(
      "Full set of package + file + internal class",
      "FqInfo(hello.oh.my:World:InternalClass)",
      Objects.toString(FqInfo.parse("hello.oh.my.World.InternalClass"))
    );
    assertEquals(
      "Simple class name without package",
      "FqInfo(:World:World)",
      Objects.toString(FqInfo.parse("World"))
    );
    assertEquals(
      "Internal class name without package",
      "FqInfo(:World:InternalClass)",
      Objects.toString(FqInfo.parse("World.InternalClass"))
    );
  }

  @Test
  public void testInvalid() throws Exception {
    assertEquals(
      "Just the package",
      "null",
      Objects.toString(FqInfo.parse("hello"))
    );
    assertEquals(
      "Non upper-cased identifier after the class name",
      "null",
      Objects.toString(FqInfo.parse("World.internalClass"))
    );
    assertEquals(
      "Empty fqname",
      "null",
      Objects.toString(FqInfo.parse(""))
    );
  }
}
