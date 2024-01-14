package com.intellij.plugins.haxe.haxelib.definitions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLDefine;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

@CustomLog
public class HxmlDefinitionsUtil {

  @Nullable
  public static HXMLFile findHxml(Module module, String projectHxml) {
    if (projectHxml == null) return null;

    VirtualFileSystem vfs = VirtualFileManager.getInstance().getFileSystem(LocalFileSystem.PROTOCOL);
    VirtualFile file = vfs.findFileByPath(projectHxml);
    if (file == null) return null;
    PsiFile projectFile = PsiManager.getInstance(module.getProject()).findFile(file);
    if (projectFile instanceof HXMLFile hxmlFile) {
      return hxmlFile;
    }
    return null;
  }

  public static void processHxml(Module module, Map<String, String> defines, HXMLFile hxml) {
    if (hxml == null) return;
    Collection<HXMLDefine> hxmlDefines = PsiTreeUtil.findChildrenOfType(hxml, HXMLDefine.class);
    for (HXMLDefine define : hxmlDefines) {
      HXMLValue value = define.getValue();
      String[] split = value.getText().split("=");
      if (split.length == 1) {
        defines.put(split[0], "true");
      }else if (split.length == 2) {
        defines.put(split[0], split[1]);
      }else {
        log.warn("Define string resulted in more than 2 values after split : " + value);
        defines.put(split[0], "true");
      }
    }
  }

}
