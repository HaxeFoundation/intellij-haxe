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
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.metadata.HaxeMetadataLanguage;
import com.intellij.plugins.haxe.metadata.parser.HaxeMetadataParserDefinition;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.testFramework.junit5.RunInEdt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Jupiter front for the platform's JUnit3-style {@link ParsingTestCase}: the
 * engine below keeps the platform logic (data-file lookup by test name, PSI
 * dump comparison), while this class drives its lifecycle from jupiter hooks.
 */
@RunInEdt(writeIntent = true)
abstract public class HaxeParsingTestBase {
  private final Engine engine;

  public HaxeParsingTestBase(String... path) {
    engine = new Engine(getPath(path));
  }

  @BeforeEach
  final void startParsingEngine(TestInfo info) throws Exception {
    // the JUnit3 test name drives the data-file lookup (testExtends -> Extends.hx)
    engine.setName(info.getTestMethod().orElseThrow().getName());
    engine.start();
  }

  @AfterEach
  final void stopParsingEngine() throws Exception {
    engine.stop();
  }

  protected void doTest(boolean checkResult) {
    engine.doTest(checkResult);
  }

  protected void doTest(boolean checkResult, boolean ensureNoErrorElements) {
    engine.doTest(checkResult, ensureNoErrorElements);
  }

  protected Project getProject() {
    return engine.getProject();
  }

  private static String getPath(String... args) {
    return String.join("/", args);
  }

  private static final class Engine extends ParsingTestCase {
    Engine(String path) {
      super(path, HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition(), new HaxeMetadataParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      HaxeAstFactory astFactory = new HaxeAstFactory();
      addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeLanguage.INSTANCE, astFactory);
      addExplicitExtension(LanguageASTFactory.INSTANCE, HaxeMetadataLanguage.INSTANCE, astFactory);
      registerMetadataParser();
    }

    private void registerMetadataParser() {
      // Get the metadata parser added because only the first language definition is added by the super.setUp call.
      // This is basically what configureFromParserDefinition does, but without overriding the globals.
      HaxeMetadataParserDefinition metaParser = new HaxeMetadataParserDefinition();
      addExplicitExtension(LanguageParserDefinitions.INSTANCE, HaxeMetadataLanguage.INSTANCE, metaParser);
    }

    @Override
    protected String getTestDataPath() {
      return HaxeTestUtils.BASE_TEST_DATA_PATH;
    }

    @Override
    protected boolean skipSpaces() {
      return true;
    }

    void start() throws Exception {
      setUp();
    }

    void stop() throws Exception {
      tearDown();
    }

    @Override
    public void doTest(boolean checkResult) {
      super.doTest(checkResult);
    }

    @Override
    public void doTest(boolean checkResult, boolean ensureNoErrorElements) {
      super.doTest(checkResult, ensureNoErrorElements);
    }
  }

}
