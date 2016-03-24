/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jps.haxe.build;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import com.intellij.plugins.haxe.util.HaxeCommonCompilerUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.haxe.model.module.JpsHaxeModuleSettings;
import org.jetbrains.jps.haxe.model.sdk.JpsHaxeSdkAdditionalData;
import org.jetbrains.jps.haxe.model.sdk.JpsHaxeSdkType;
import org.jetbrains.jps.haxe.util.JpsHaxeUtil;
import org.jetbrains.jps.incremental.*;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.serialization.JpsModelSerializationDataService;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleLevelBuilder extends ModuleLevelBuilder {
  private static final Logger LOG = Logger.getInstance(HaxeModuleLevelBuilder.class);
  @NonNls private static final String BUILDER_NAME = "haxe";
  private final boolean myDebugBuilder;

  protected HaxeModuleLevelBuilder(boolean debugBuilder) {
    super(BuilderCategory.SOURCE_PROCESSOR);
    myDebugBuilder = debugBuilder;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return BUILDER_NAME;
  }

  @Override
  public ExitCode build(CompileContext context,
                        ModuleChunk chunk,
                        DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
                        OutputConsumer outputConsumer)
    throws ProjectBuildException, IOException {
    boolean doneSomething = false;

    // Don't do this.  HaxeCompiler already does it, and doing it here just
    // does it again.
//    for (final JpsModule module : chunk.getModules()) {
//      if (module.getModuleType() == JpsHaxeModuleType.INSTANCE) {
//        doneSomething |= processModule(context, dirtyFilesHolder, module);
//      }
//    }

    return doneSomething ? ExitCode.OK : ExitCode.NOTHING_DONE;
  }

  @Override
  public List<String> getCompilableFileExtensions() {
    return Collections.emptyList();
  }

  private boolean processModule(final CompileContext context,
                                final DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> holder,
                                final JpsModule module) {
    final boolean isDebugRunner = "HaxeDebugRunner".equals(context.getBuilderParameter("RUNNER_ID"));
    if (isDebugRunner ^ myDebugBuilder) {
      return false;
    }
    final JpsHaxeModuleSettings moduleSettings = JpsHaxeUtil.getModuleSettings(module);
    if (moduleSettings == null) {
      context.processMessage(new CompilerMessage(
        BUILDER_NAME, BuildMessage.Kind.ERROR, "can't find module settings for " + module.getName())
      );
      return false;
    }
    final JpsSdk<JpsHaxeSdkAdditionalData> jpsSdk = module.getSdk(JpsHaxeSdkType.INSTANCE);
    if (jpsSdk == null) {
      context.processMessage(new CompilerMessage(
        BUILDER_NAME, BuildMessage.Kind.ERROR, "can't find module sdk for " + module.getName())
      );
      return false;
    }

    context.processMessage(new ProgressMessage(HaxeCommonBundle.message("haxe.module.compilation.progress.message", module.getName())));

    boolean compiled = HaxeCommonCompilerUtil.compile(new HaxeCommonCompilerUtil.CompilationContext() {
      private String myErrorRoot;

      @NotNull
      @Override
      public HaxeModuleSettingsBase getModuleSettings() {
        return moduleSettings;
      }

      @Override
      public String getModuleName() {
        return module.getName();
      }

      @Override
      public String getCompilationClass() {
        return getModuleSettings().getMainClass();
      }

      @Override
      public String getOutputFileName() {
        return getModuleSettings().getOutputFileName();
      }

      @Override
      public Boolean getIsTestBuild() {
        return false;
      }

      @Override
      public void errorHandler(String message) {
        context.processMessage(new CompilerMessage(BUILDER_NAME, BuildMessage.Kind.ERROR, message));
      }
      @Override
      public void warningHandler(String message) {
        context.processMessage(new CompilerMessage(BUILDER_NAME, BuildMessage.Kind.WARNING, message));
      }

      @Override
      public void infoHandler(String message) {
        context.processMessage(new CompilerMessage(BUILDER_NAME, BuildMessage.Kind.INFO, message));
      }

      @Override
      public void log(String message) {
        LOG.debug(message);
      }

      @Override
      public boolean isDebug() {
        return isDebugRunner;
      }

      @Override
      public String getSdkHomePath() {
        return jpsSdk.getHomePath();
      }

      @Override
      public String getHaxelibPath() {
        return jpsSdk.getSdkProperties().getHaxelibPath();
      }

      @Override
      public String getSdkName() {
        return jpsSdk.getVersionString();
      }

      @Override
      public List<String> getSourceRoots() {
        return ContainerUtil.map(module.getSourceRoots(), new Function<JpsModuleSourceRoot, String>() {
          @Override
          public String fun(JpsModuleSourceRoot root) {
            return JpsPathUtil.urlToPath(root.getUrl());
          }
        });
      }

      @Override
      public String getCompileOutputPath() {
        final String outputRootUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
        return JpsPathUtil.urlToPath(outputRootUrl);
      }

      @Override
      public void setErrorRoot(String root) {
        myErrorRoot = root;
      }

      @Override
      public String getErrorRoot() {
        return (myErrorRoot != null) ? myErrorRoot : getWorkingDirectoryPath();
      }

      @Override
      public void handleOutput(String[] lines) {
        /*for (String error : lines) {
          final HaxeCompilerError compilerError = HaxeCompilerError.create(StringUtil.notNullize(getErrorRoot()), error);
          context.processMessage(new CompilerMessage(
            BUILDER_NAME,
            BuildMessage.Kind.WARNING,
            compilerError != null ? compilerError.getErrorMessage() : error,
            compilerError != null ? compilerError.getPath() : null,
            -1L, -1L, -1L,
            compilerError != null ? (long)compilerError.getLine() : -1L,
            compilerError != null ? (long)compilerError.getColumn() : -1L
          ));
        }*/
      }

      @Override
      public HaxeTarget getHaxeTarget() {
        return getModuleSettings().getHaxeTarget();
      }

      @Override
      public String getModuleDirPath() {
        return getWorkingDirectoryPath();
      }

      @Nullable
      public String getWorkingDirectoryPath() {
        final File baseDirectory = JpsModelSerializationDataService.getBaseDirectory(module);
        return baseDirectory != null ? baseDirectory.getPath() : null;
      }
    });

    if (!compiled) {
      context.processMessage(new CompilerMessage(BUILDER_NAME, BuildMessage.Kind.ERROR, "compilation failed"));
    }

    return compiled;
  }
}
