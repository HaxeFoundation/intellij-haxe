package com.intellij.plugins.haxe.buildsystem.haxelib;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;

public class HaxelibJsonSchemaProviderFactory implements JsonSchemaProviderFactory {


  @Override
  public @NotNull List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
    return List.of(new haxelibSchemaProvider());
  }

  private static class haxelibSchemaProvider implements JsonSchemaFileProvider {

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
      return file.getName().equalsIgnoreCase("haxelib.json");
    }

    @Override
    public @NotNull @Nls String getName() {
      return HaxeBundle.message("haxelib.project.file.name");
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
      URL resource = HaxelibJsonSchemaProviderFactory.class.getResource("/schema/haxelib/schema.json");
      return VfsUtil.findFileByURL(resource);
    }

    @Override
    public @NotNull SchemaType getSchemaType() {
      return SchemaType.embeddedSchema;
    }
  }
}
