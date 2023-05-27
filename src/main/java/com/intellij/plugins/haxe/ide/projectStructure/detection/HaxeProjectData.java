package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class HaxeProjectData {

  private String projectFile;

  public HaxeProjectData(String projectFile) {
    this.projectFile = projectFile;
  }

  public String getName() {
    return getProjectRoot().getName();
  }

  public String getProjectRootPath() {
    return new File(projectFile).getParent();
  }

  public VirtualFile getProjectRoot() {
    return VfsUtil.findFile(Path.of(getProjectRootPath()), true);
  }
  public VirtualFile getProjectFile() {
    return VfsUtil.findFile(Path.of(getProjectFilePath()), true);
  }

  public List<VirtualFile> getSourcePaths() {
    VirtualFile root = getProjectRoot();
    VirtualFile projectFile = VfsUtil.findFile(Path.of(getProjectFilePath()), true);

    List<String> paths = HaxeProjectFileDetectionUtil.sourcePaths(projectFile);
    return paths.stream().map(root::findChild).filter(Objects::nonNull).toList();

  }

  public HaxeConfiguration getProjectType() {
    VirtualFile file = getProjectFile();
    if (HaxeProjectFileDetectionUtil.isLimeProject(file)
        || HaxeProjectFileDetectionUtil.isOpenFLProject(file)) {
      return HaxeConfiguration.OPENFL;
    }
    if (HaxeProjectFileDetectionUtil.isHxmlProject(file)) {
      return HaxeConfiguration.HXML;
    }
    if (HaxeProjectFileDetectionUtil.isNMMLProject(file)) {
      return HaxeConfiguration.NMML;
    }

    return HaxeConfiguration.CUSTOM;
  }

  public String getProjectFilePath() {
    return projectFile;
  }

  public String getOutputFolder() {
    String path = getProjectRootPath();
    assert  path.length() > 0;
    return path + "/out";
  }
}
