package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;

public class HaxeSdkInputFilter extends DefaultFileTypeSpecificInputFilter {
  public static HaxeSdkInputFilter INSTANCE = new HaxeSdkInputFilter();

  private HaxeSdkInputFilter() {
    super(HaxeFileType.HAXE_FILE_TYPE);
  }

  @Override
  public boolean acceptInput(VirtualFile file) {
    // ignore std stubs for different platforms
    return !"_std".equals(file.getParent().getName());
  }
}
