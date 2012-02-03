package com.intellij.plugins.haxe.components;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.compilation.MakeHaxeCompile;
import org.jetbrains.annotations.NotNull;

public class HaxeCompilerLoader extends AbstractProjectComponent {
  public HaxeCompilerLoader(Project project) {
    super(project);
  }

  @NotNull
  public String getComponentName() {
    return "haXeCompilerLoader";
  }

  public void projectOpened() {
    //CompilerManager compilerManager = CompilerManager.getInstance(myProject);
    //compilerManager.addCompilableFileType(HaxeFileType.HAXE_FILE_TYPE);
    //compilerManager.addCompiler(new MakeHaxeCompile());
  }
}
