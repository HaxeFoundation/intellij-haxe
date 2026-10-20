package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Requirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The manifest sits above a classpath root; its dependencies block gives the edges, an empty version meaning any. */
@DisplayName("Build tools: haxelib manifest")
public class HaxelibManifestTest {

  private static final String MANIFEST_JSON = """
    {"name": "haxeparser", "version": "4.3.0", "dependencies": {"hxparse": "", "hscript": "2.4.0"}}
    """;

  @TempDir
  Path repository;

  @Test
  @DisplayName("the manifest above a checkout's classpath root is found and its dependencies read")
  public void testTheManifestAboveACheckoutsClasspathRootIsFoundAndItsDependenciesRead() throws IOException {
    Path checkout = Files.createDirectories(repository.resolve("haxeparser/git"));
    Path manifest = Files.writeString(checkout.resolve("haxelib.json"), MANIFEST_JSON);
    Path sources = Files.createDirectories(checkout.resolve("src"));

    Path found = HaxelibManifest.find(List.of(sources.toString()));

    assertEquals(manifest, found);
    assertEquals(Arrays.asList(new Requirement("hxparse", null), new Requirement("hscript", "2.4.0")),
                 HaxelibManifest.requirements(found));
  }

  @Test
  @DisplayName("no manifest within reach and an unreadable one both give nothing")
  public void testNoManifestWithinReachAndAnUnreadableOneBothGiveNothing() throws IOException {
    Path sources = Files.createDirectories(repository.resolve("plain/src"));
    Path broken = Files.writeString(repository.resolve("haxelib.json"), "not json");

    assertNull(HaxelibManifest.find(List.of(sources.resolve("deeper/still/more/levels").toString())));
    assertEquals(List.of(), HaxelibManifest.requirements(broken));
  }
}
