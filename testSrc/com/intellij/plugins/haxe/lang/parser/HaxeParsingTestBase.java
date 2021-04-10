/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.mock.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.build.ClassWrapper;
import com.intellij.plugins.haxe.build.IdeaTarget;
import com.intellij.plugins.haxe.build.MethodWrapper;
import com.intellij.plugins.haxe.build.UnsupportedMethodException;
import com.intellij.plugins.haxe.lang.RegexLanguageInjector;
import com.intellij.plugins.haxe.metadata.HaxeMetadataLanguage;
import com.intellij.plugins.haxe.metadata.parser.HaxeMetadataParserDefinition;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import java.util.ArrayList;
import java.util.List;

abstract public class HaxeParsingTestBase extends ParsingTestCase {
  public HaxeParsingTestBase(String... path) {
    super(getPath(path), HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition(), new HaxeMetadataParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    HaxeAstFactory astFactory = new HaxeAstFactory();
    addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeLanguage.INSTANCE, astFactory);
    addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeMetadataLanguage.INSTANCE, astFactory);

    if (!IdeaTarget.IS_VERSION_19_3_COMPATIBLE) {
      registerMetadataParser();
    }

    // Work around @NotNull bug down in the test fixture.  Since no InjectedLanguageManager
    // was registered, null was passed to a @NotNull function.  This affected testSimple().
    registerExtensionPoint(getExtensionArea(myProject), MockMultiHostInjector.MULTIHOST_INJECTOR_EP_NAME, MockMultiHostInjector.class);
    registerExtensionPoint(getExtensionArea(null), RegexLanguageInjector.EXTENSION_POINT_NAME, RegexLanguageInjector.class); // Might as well use the real one.
    registerInjectedLanguageManager();
    // End workaround.
  }

  private void registerInjectedLanguageManager() {
    ClassWrapper<InjectedLanguageManagerImpl> wrapper = new ClassWrapper<>(InjectedLanguageManagerImpl.class.getCanonicalName());
    InjectedLanguageManagerImpl manager;

    myProject.registerService(DumbService.class, MockDumbService.class);

    if (IdeaTarget.IS_VERSION_19_3_COMPATIBLE) {
      manager = wrapper.newInstance(myProject);
    } else {
      manager = wrapper.newInstance(myProject, new MockDumbService(myProject));
    }
    myProject.registerService(InjectedLanguageManager.class, manager);
  }

  private <T> void registerMetadataParser() {
    // Get the metadata parser added because only the first language definition is added by the super.setUp call.
    // This is basically what configureFromParserDefinition does, but without overriding the globals.
    HaxeMetadataParserDefinition metaParser = new HaxeMetadataParserDefinition();
    addExplicitExtension(LanguageParserDefinitions.INSTANCE, HaxeMetadataLanguage.INSTANCE, metaParser);

    //registerComponentInstance(((MockApplicationEx)ApplicationManager.getApplication()).getPicoContainer(), FileTypeManager.class,
    //                          new MockFileTypeManager(new MockLanguageFileType(HaxeMetadataLanguage.INSTANCE, HaxeFileType.DEFAULT_EXTENSION)));
    MutablePicoContainer picoContainer = ((MockApplication)ApplicationManager.getApplication()).getPicoContainer();
    MockFileTypeManager typeManager =
      new MockFileTypeManager(new MockLanguageFileType(HaxeMetadataLanguage.INSTANCE, HaxeFileType.DEFAULT_EXTENSION));

    try {
      MethodWrapper<Object> rci = new MethodWrapper<>(HaxeParsingTestBase.class, true, "registerComponentInstance",
                                            MutablePicoContainer.class, Class.class, Object.class);
      rci.invoke(this, picoContainer, FileTypeManager.class, typeManager);
    } catch (UnsupportedMethodException ex) {
      assertEmpty(ex.getLocalizedMessage());
    }
  }

  private static ExtensionsAreaImpl getExtensionArea(@Nullable("null means root") AreaInstance areaInstance) {
    ExtensionsArea area = Extensions.getArea(areaInstance);
    assert area instanceof ExtensionsAreaImpl: "Unexpected return type from Extensions.getArea()";
    return (ExtensionsAreaImpl)area;
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      if (result.length() > 0) {
        result.append("/");
      }
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }

  public static class MockMultiHostInjector implements MultiHostInjector {
    // Be careful with this.  It is the same extension point name as (and thus will conflict with) MultiHostInjector uses.
    public static final ExtensionPointName<MockMultiHostInjector> MULTIHOST_INJECTOR_EP_NAME = ExtensionPointName.create("com.intellij.multiHostInjector");

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {

    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
      return new ArrayList<>();
    }
  }
}
