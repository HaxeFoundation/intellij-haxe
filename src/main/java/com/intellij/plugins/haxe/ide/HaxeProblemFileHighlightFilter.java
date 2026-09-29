package com.intellij.plugins.haxe.ide;

import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;

/**
 * Opts haxe files into {@code WolfTheProblemSolver} tracking: without this
 * filter the wolf ignores them entirely — daemon errors never wave the file
 * name in the Project view, and problems reported from external sources (the
 * compiler diagnostics marker) are silently dropped.
 */
public final class HaxeProblemFileHighlightFilter implements Condition<VirtualFile> {
  @Override
  public boolean value(VirtualFile virtualFile) {
    return FileTypeRegistry.getInstance().isFileOfType(virtualFile, HaxeFileType.INSTANCE);
  }
}
