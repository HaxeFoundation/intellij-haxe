package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Completion and validation for hxformat.json (the haxe-formatter /
 * HaxeCheckstyle formatter config) from a bundled schema authored against
 * formatter 1.18.0's config typedefs.
 */
public class HxformatJsonSchemaProviderFactory implements JsonSchemaProviderFactory {

  private static final String SCHEMA_RESOURCE_PATH = "/schema/hxformat/schema.json";

  @Override
  public @NotNull List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
    return List.of(new HxformatSchemaProvider());
  }

  private static class HxformatSchemaProvider implements JsonSchemaFileProvider {
    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return HxformatConfigs.HXFORMAT_FILE_NAME.equals(file.getName());
    }

    @Override
    public @NotNull String getName() {
      return HaxeCodeStyleBundle.message("hxformat.schema.name");
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
      return JsonSchemaProviderFactory.getResourceFile(HxformatJsonSchemaProviderFactory.class, SCHEMA_RESOURCE_PATH);
    }

    @Override
    public @NotNull SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }

    @Override
    public @NotNull JsonSchemaVersion getSchemaVersion() {
      return JsonSchemaVersion.SCHEMA_7;
    }
  }
}
