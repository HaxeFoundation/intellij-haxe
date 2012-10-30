package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.util.indexing.FileBasedIndex;

public class HaxeSdkInputFilter implements FileBasedIndex.InputFilter {
  public static HaxeSdkInputFilter INSTANCE = new HaxeSdkInputFilter();

  private HaxeSdkInputFilter() {
  }

  @Override
  public boolean acceptInput(VirtualFile file) {
    // ignore std stubs for different platforms
    return file.getFileType() == HaxeFileType.HAXE_FILE_TYPE && !"_std".equals(file.getParent().getName());
  }
}
