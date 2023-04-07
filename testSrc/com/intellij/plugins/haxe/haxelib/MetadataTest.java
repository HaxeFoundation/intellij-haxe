/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.haxelib;

import com.google.gson.*;
import com.intellij.testFramework.UsefulTestCase;
import org.junit.Test;

import java.util.List;

import static com.intellij.plugins.haxe.haxelib.HaxelibMetadata.*;

public class MetadataTest extends UsefulTestCase {

  public static final String NAME_DATA = "useless_lib";
  public static final String URL_DATA = "https://github.com/jasononeil/useless/";
  public static final String LICENSE_DATA = "MIT";
  public static final String TAGS_DATA1 = "cross";
  public static final String TAGS_DATA2 = "useless";
  public static final String DESCRIPTION_DATA = "This library is useless in the same way on every platform.";
  public static final String VERSION_DATA = "1.0.0";
  public static final String CLASSPATH_DATA = "src";
  public static final String RELEASENOTE_DATA = "Initial release, everything is working correctly.";
  public static final String CONTRIBUTOR1 = "Juraj";
  public static final String CONTRIBUTOR2 = "Jason";
  public static final String CONTRIBUTOR3 = "Nicolas";
  public static final String LIB_NAME1 = "tink_macro";
  public static final String LIB_VERSION1 = "";
  public static final String LIB_NAME2 = "nme";
  public static final String LIB_VERSION2 = "3.5.5";

  private JsonElement createTestElement() {

    JsonObject el = new JsonObject();
    el.addProperty(NAME, NAME_DATA);
    el.addProperty(URL, URL_DATA);
    el.addProperty(LICENSE, LICENSE_DATA);

    JsonArray tags = new JsonArray();
    tags.add(new JsonPrimitive(TAGS_DATA1));
    tags.add(new JsonPrimitive(TAGS_DATA2));
    el.add(TAGS, tags);

    el.addProperty(DESCRIPTION, DESCRIPTION_DATA);
    el.addProperty(VERSION, VERSION_DATA);
    el.addProperty(CLASSPATH, CLASSPATH_DATA);
    el.addProperty(RELEASENOTE, RELEASENOTE_DATA);

    JsonArray contributors = new JsonArray();
    contributors.add(new JsonPrimitive(CONTRIBUTOR1));
    contributors.add(new JsonPrimitive(CONTRIBUTOR2));
    contributors.add(new JsonPrimitive(CONTRIBUTOR3));
    el.add(CONTRIBUTORS, contributors);

    JsonObject dependencies = new JsonObject();
    dependencies.addProperty(LIB_NAME1, LIB_VERSION1);
    dependencies.addProperty(LIB_NAME2, LIB_VERSION2);
    el.add(DEPENDENCIES, dependencies);

    return el;
  }

  private String getTestString() {
    JsonElement testelem = createTestElement();

    Gson gson = new Gson();
    String out = gson.toJson(testelem);
    return out;
  }

  private HaxelibMetadata getTestMetadata() {
    String testdata = getTestString();
    HaxelibMetadata md = new HaxelibMetadata(testdata);
    return md;
  }

  private <T> void assertContains(List<T> list, T... elements) {
    assertEquals(list.size(), elements.length);
    for (T t : elements) {
      assertTrue(list.contains(t));
    }
  }


  @Test
  public void testParsing() throws Exception {
    HaxelibMetadata md = getTestMetadata();
    assertEquals(createTestElement(), md.getTestInterface().getRoot());
  }

  @Test
  public void testAccessors() throws Exception {
    HaxelibMetadata md = getTestMetadata();
    assertEquals(NAME_DATA, md.getName());
    assertEquals(URL_DATA, md.getUrl());
    assertEquals(LICENSE_DATA, md.getLicense());
    assertContains(md.getTags(), TAGS_DATA1, TAGS_DATA2);
    assertEquals(DESCRIPTION_DATA, md.getDescription());
    assertEquals(VERSION_DATA, md.getVersion());
    assertEquals(CLASSPATH_DATA, md.getClasspath());
    assertEquals(RELEASENOTE_DATA, md.getReleasenote());
    assertContains(md.getContributors(), CONTRIBUTOR1, CONTRIBUTOR2, CONTRIBUTOR3);
    assertContains(md.getDependencies(),
                   new HaxelibMetadata.Dependency(LIB_NAME1, LIB_VERSION1),
                   new HaxelibMetadata.Dependency(LIB_NAME2, LIB_VERSION2));
  }
}
