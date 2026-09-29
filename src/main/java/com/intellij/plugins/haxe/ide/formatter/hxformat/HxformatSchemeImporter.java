package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Imports a haxe-formatter (HaxeCheckstyle) hxformat.json as a code style
 * scheme: the hxformat DEFAULTS first, then the file's overrides. Config keys
 * that cannot be honored are listed in the post-import message.
 */
public class HxformatSchemeImporter implements SchemeImporter<CodeStyleScheme> {

  private static final Logger LOG = Logger.getInstance(HxformatSchemeImporter.class);
  private static final int REPORTED_KEYS_LIMIT = 10;

  // the extension is a singleton: this carries the LAST import's result from
  // importScheme to the immediately following getAdditionalImportInfo call
  private List<String> lastUnsupported = List.of();

  @Override
  public String @NotNull [] getSourceExtensions() {
    return new String[]{"json"};
  }

  @Override
  public @Nullable CodeStyleScheme importScheme(@NotNull Project project,
                                               @NotNull VirtualFile selectedFile,
                                               @NotNull CodeStyleScheme currentScheme,
                                               @NotNull SchemeFactory<? extends CodeStyleScheme> schemeFactory) throws SchemeImportException {
    lastUnsupported = List.of();
    JsonNode root = readJson(selectedFile);
    CodeStyleScheme scheme = schemeFactory.createNewScheme(selectedFile.getNameWithoutExtension());
    HxformatDefaultProfile.apply(scheme.getCodeStyleSettings());
    lastUnsupported = HxformatJsonMapper.apply(scheme.getCodeStyleSettings(), root);
    // the post-import balloon is short-lived - the log keeps the full list
    for (String key : lastUnsupported) {
      LOG.warn("hxformat.json import: unsupported setting " + key + " (from " + selectedFile.getPath() + ")");
    }
    return scheme;
  }

  @Override
  public @Nullable String getAdditionalImportInfo(@NotNull CodeStyleScheme scheme) {
    if (lastUnsupported.isEmpty()) {
      return HaxeCodeStyleBundle.message("hxformat.import.complete");
    }
    List<String> shown = lastUnsupported.subList(0, Math.min(REPORTED_KEYS_LIMIT, lastUnsupported.size()));
    String keys = String.join(", ", shown);
    if (lastUnsupported.size() > shown.size()) {
      keys += ", …";
    }
    return HaxeCodeStyleBundle.message("hxformat.import.partial", lastUnsupported.size(), keys);
  }

  @NotNull
  private static JsonNode readJson(@NotNull VirtualFile file) throws SchemeImportException {
    try {
      return HxformatConfigs.readJsonTree(file);
    }
    catch (IOException e) {
      throw new SchemeImportException(HaxeCodeStyleBundle.message("hxformat.import.parse.error", e.getMessage()));
    }
  }
}
