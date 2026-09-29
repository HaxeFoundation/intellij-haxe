package com.intellij.plugins.haxe.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatJsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The bundled hxformat.json schema resolves from the plugin classpath and targets only that file. */
@DisplayName("Formatting: hxformat json schema provider")
public class HxformatJsonSchemaProviderTest extends HaxeLightFixtureTestCase {

  private static final List<String> CONFIG_SECTIONS =
    List.of("emptyLines", "indentation", "lineEnds", "sameLine", "whitespace", "wrapping");

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("provider targets only hxformat.json")
  public void testProviderTargetsOnlyHxformatJson() {
    JsonSchemaFileProvider provider = soleProvider();
    VirtualFile config = myFixture.addFileToProject("hxformat.json", "{}").getVirtualFile();
    VirtualFile other = myFixture.addFileToProject("haxelib.json", "{}").getVirtualFile();

    assertTrue(provider.isAvailable(config));
    assertFalse(provider.isAvailable(other));
  }

  @Test
  @DisplayName("bundled schema resolves and declares every config section")
  public void testBundledSchemaResolvesAndDeclaresEveryConfigSection() throws Exception {
    VirtualFile schemaFile = soleProvider().getSchemaFile();
    assertNotNull(schemaFile, "the schema resource must resolve from the plugin classpath");

    String schemaText = new String(schemaFile.contentsToByteArray(), StandardCharsets.UTF_8);
    JsonNode root = new ObjectMapper().readTree(schemaText);
    for (String section : CONFIG_SECTIONS) {
      assertTrue(root.path("properties").has(section), section + " must be declared");
    }
    assertTrue(root.path("properties").has("disableFormatting"));
    assertTrue(root.path("properties").has("excludes"));
  }

  private JsonSchemaFileProvider soleProvider() {
    List<JsonSchemaFileProvider> providers = new HxformatJsonSchemaProviderFactory().getProviders(getProject());
    assertEquals(1, providers.size());
    return providers.get(0);
  }
}
