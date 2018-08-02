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
package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.ide.util.importProject.LibrariesDetectionStep;
import com.intellij.ide.util.importProject.ModulesDetectionStep;
import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkForModuleStep;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.ide.util.projectWizard.importSources.util.CommonSourceRootDetectionUtil;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.util.NullableFunction;
import com.intellij.util.StringBuilderSpinAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeProjectStructureDetector extends ProjectStructureDetector {
  public static final NullableFunction<CharSequence, String> PACKAGE_NAME_FETCHER = new NullableFunction<CharSequence, String>() {
    public String fun(final CharSequence charSequence) {
      Lexer lexer = LanguageParserDefinitions.INSTANCE.forLanguage(HaxeLanguage.INSTANCE).createLexer(null);
      lexer.start(charSequence);
      return readPackageName(charSequence, lexer);
    }
  };

  @NotNull
  @Override
  public DirectoryProcessingResult detectRoots(@NotNull File dir,
                                               @NotNull File[] children,
                                               @NotNull File base,
                                               @NotNull List<DetectedProjectRoot> result) {
    for (File child : children) {
      if (child.isFile() && child.getName().endsWith(HaxeFileType.DEFAULT_EXTENSION)) {
        Pair<File, String> root =
          CommonSourceRootDetectionUtil.IO_FILE.suggestRootForFileWithPackageStatement(child, base, PACKAGE_NAME_FETCHER, false);
        if (root != null) {
          result.add(new HaxeModuleSourceRoot(root.getFirst()));
          return DirectoryProcessingResult.skipChildrenAndParentsUpTo(root.getFirst());
        }
        else {
          return DirectoryProcessingResult.SKIP_CHILDREN;
        }
      }
    }
    return DirectoryProcessingResult.PROCESS_CHILDREN;
  }

  @Override
  public List<ModuleWizardStep> createWizardSteps(ProjectFromSourcesBuilder builder, ProjectDescriptor projectDescriptor, Icon stepIcon) {
    HaxeModuleInsight moduleInsight =
      new HaxeModuleInsight(new DelegatingProgressIndicator(), builder.getExistingModuleNames(), builder.getExistingProjectLibraryNames());
    final List<ModuleWizardStep> steps = new ArrayList<ModuleWizardStep>();
    steps.add(
      new LibrariesDetectionStep(builder, projectDescriptor, moduleInsight, stepIcon, "reference.dialogs.new.project.fromCode.page1"));
    steps.add(
      new ModulesDetectionStep(this, builder, projectDescriptor, moduleInsight, stepIcon, "reference.dialogs.new.project.fromCode.page2"));
    HaxeSdkType type = HaxeSdkType.getInstance();
    type.ensureSdk();
    steps.add(new ProjectJdkForModuleStep(builder.getContext(), type));
    return steps;
  }

  @Nullable
  public static String readPackageName(final CharSequence charSequence, final Lexer lexer) {
    skipWhiteSpaceAndComments(lexer);
    if (!HaxeTokenTypes.KPACKAGE.equals(lexer.getTokenType())) {
      return "";
    }
    lexer.advance();
    skipWhiteSpaceAndComments(lexer);

    return readQualifiedName(charSequence, lexer, false);
  }

  @Nullable
  static String readQualifiedName(final CharSequence charSequence, final Lexer lexer, boolean allowStar) {
    final StringBuilder buffer = StringBuilderSpinAllocator.alloc();
    try {
      while (true) {
        if (lexer.getTokenType() != HaxeTokenTypes.ID && !(allowStar && lexer.getTokenType() != HaxeTokenTypes.OMUL)) break;
        buffer.append(charSequence, lexer.getTokenStart(), lexer.getTokenEnd());
        if (lexer.getTokenType() == HaxeTokenTypes.OMUL) break;
        lexer.advance();
        if (lexer.getTokenType() != HaxeTokenTypes.ODOT) break;
        buffer.append('.');
        lexer.advance();
      }
      String packageName = buffer.toString();
      if (StringUtil.endsWithChar(packageName, '.')) return null;
      return packageName;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  public static void skipWhiteSpaceAndComments(Lexer lexer) {
    while (HaxeTokenTypeSets.COMMENTS.contains(lexer.getTokenType()) || HaxeTokenTypeSets.WHITESPACES.contains(lexer.getTokenType())) {
      lexer.advance();
    }
  }
}
