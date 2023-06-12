package com.intellij.plugins.haxe.ide.actions.haxelib.loadproject;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import com.intellij.plugins.haxe.ide.projectStructure.processor.HaxeOpenHxmlProjectProcessor;
import com.intellij.projectImport.ProjectOpenProcessor;

public class LoadAsHxmlProjectAction extends LoadProjectActionBase implements DumbAware {

  @Override
  boolean isProjectFile(VirtualFile file) {
    return HaxeProjectFileDetectionUtil.isHxmlProject(file);
  }

  @Override
  void performProjectImport(Project project, VirtualFile file) {
    ProjectOpenProcessor extension = ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtension(HaxeOpenHxmlProjectProcessor.class);
    extension.importProjectAfterwards(project, file);
  }
}
