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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class describing the haxelib JSON metadata.
 */
public class HaxelibMetadata {

  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

  public static class Dependency {
    private String name;
    private String version;

    public Dependency(String name, String version) {
      this.name = name;
      this.version = version;
    }

    public String getName() {
      return name;
    }
    public String getVersion() {
      return version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Dependency)) return false;

      Dependency that = (Dependency)o;

      if (name != null ? !name.equals(that.name) : that.name != null) return false;
      return version != null ? version.equals(that.version) : that.version == null;
    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (version != null ? version.hashCode() : 0);
      return result;
    }
  }

  public static HaxelibMetadata EMPTY_METADATA = new HaxelibMetadata(true);

  public static String NAME = "name";
  public static String URL = "url";
  public static String LICENSE = "license";
  public static String TAGS = "tags";
  public static String DESCRIPTION = "description";
  public static String VERSION = "version";
  public static String CLASSPATH = "classPath";
  public static String RELEASENOTE = "releasenote";
  public static String CONTRIBUTORS = "contributors";
  public static String DEPENDENCIES = "dependencies";

  private JsonObject root;
  private String path;

  /** Private constructor for EMPTY_METADATA */
  private HaxelibMetadata(boolean makeEmpty) {
    root = null;
    path = "";
  }

  /** Test constructor */
  public HaxelibMetadata(@NotNull String json) {
    root = parse(json);
    path = "";
  }

  private HaxelibMetadata(@NotNull VirtualFile jsonFile) {
    try {
      root = parse(new String(jsonFile.contentsToByteArray()));
    } catch (IOException e) {
      root = null;
    }
    path = jsonFile.getParent().getPath();
  }

  @NotNull
  public static HaxelibMetadata load(@NotNull VirtualFile libRoot) {
    String mdfile = HaxeFileUtil.joinPath(libRoot.getUrl(), "haxelib.json");
    VirtualFile metadatafile = VirtualFileManager.getInstance().findFileByUrl(mdfile);
    if (null == metadatafile || !metadatafile.exists()) {
      return EMPTY_METADATA;
    }
    return new HaxelibMetadata(metadatafile);
  }


  private JsonObject parse(String jsonData) {
    if (null == jsonData || jsonData.isEmpty()) {
      LOG.debug("Empty json metadata.");
      return null;
    }

    JsonParser parser = new JsonParser();
    JsonElement root = parser.parse(jsonData);
    if (!root.isJsonObject()) {
      LOG.debug("Unexpected JSON type (expected JsonObject).");
      return null;
    }
    return root.getAsJsonObject();
  }

  @Nullable
  public String getName() {
    return getString(NAME);
  }

  @Nullable
  public String getUrl() {
    return getString(URL);
  }

  @Nullable
  public String getLicense() {
    return getString(LICENSE);
  }

  @Nullable
  public List<String> getTags() {
    return getStringList(TAGS);
  }

  @Nullable
  public String getDescription() {
    return getString(DESCRIPTION);
  }

  @Nullable
  public String getVersion() {
    return getString(VERSION);
  }

  @Nullable
  public String getClasspath() {
    String internal = getString(CLASSPATH);
    return internal;
  }

  @Nullable
  public String getReleasenote() {
    return getString(RELEASENOTE);
  }

  @Nullable
  public List<String> getContributors() {
    return getStringList(CONTRIBUTORS);
  }

  @Nullable
  public List<Dependency> getDependencies() {
    if (null != root) {
      JsonElement elem = root.get(DEPENDENCIES);
      if (null != elem) {
        JsonObject dependencies = elem.getAsJsonObject(); // Can't fail.
        List<Dependency> list = new ArrayList<Dependency>();
        for (Map.Entry<String, JsonElement> entry : dependencies.entrySet()) {
          String name = entry.getKey();
          String version = entry.getValue().getAsString();
          list.add(new Dependency(name, version));
        }
        return list;
      }
    }
    return null;
  }

  public boolean isValid() { return null != root; }

  @Nullable
  private String getString(String attr) {
    if (null != root) {
      JsonElement elem = root.get(attr);
      if (null != elem ) {
        return elem.getAsString();
      }
    }
    return null;
  }

  @Nullable
  private List<String> getStringList(String attr) {
    if (null != root) {
      JsonArray ary = root.get(attr).getAsJsonArray();
      List<String> list = new ArrayList<String>(ary.size());
      for (JsonElement el : ary) {
        list.add(el.getAsString());
      }
      return list;
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxelibMetadata metadata = (HaxelibMetadata)o;

    return root != null ? root.equals(metadata.root) : metadata.root == null;
  }

  @Override
  public int hashCode() {
    return root != null ? root.hashCode() : 0;
  }

  /**
   * Test helpers. Do not use for normal code.
   */
  public TestInterface getTestInterface() {
    return new TestInterface();
  }

  public class TestInterface {
    public JsonObject getRoot() {
      return root;
    }
  }
}
