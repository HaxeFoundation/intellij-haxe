/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe;

import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import com.intellij.testFramework.fixtures.impl.ModuleFixtureBuilderImpl;
import com.intellij.testFramework.fixtures.impl.ModuleFixtureImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by fedorkorotkov.
 */
abstract public class HaxeCodeInsightFixtureTestCase extends UsefulTestCase {
  private final IdeaTestFixtureFactory testFixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();

  protected CodeInsightTestFixture myFixture;
  private ModuleFixtureBuilder moduleFixtureBuilder;

  protected String myHaxeToolkit = null;

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  protected HaxeCodeInsightFixtureTestCase() {
    super();
    Logger.setUnitTestMode();
    Logger.setFactory(category -> {
      DefaultLogger logger = new DefaultLogger(category);
      logger.setLevel(LogLevel.WARNING);
      return logger;
    });
    //HaxeDebugLogger.configurePrimaryLoggerToSwallowLogs();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    testFixtureFactory.registerFixtureBuilder(MyHaxeModuleFixtureBuilderImpl.class, MyHaxeModuleFixtureBuilderImpl.class);
    final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = testFixtureFactory.createFixtureBuilder(getName());
    myFixture = testFixtureFactory.createCodeInsightFixture(projectBuilder.getFixture());
    moduleFixtureBuilder = projectBuilder.addModule(MyHaxeModuleFixtureBuilderImpl.class);

    if (toAddSourceRoot()) {
      moduleFixtureBuilder.addSourceContentRoot(myFixture.getTempDirPath());
    }
    else {
      moduleFixtureBuilder.addContentRoot(myFixture.getTempDirPath());
    }

    if (usingHaxeToolkit()) {
      moduleFixtureBuilder.addSourceContentRoot(myHaxeToolkit);
    }

    tuneFixture(moduleFixtureBuilder);

    myFixture.setTestDataPath(getTestDataPath());
    myFixture.setUp();
    LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.JDK_1_6);
  }

  protected boolean toAddSourceRoot() {
    return true;
  }

  protected boolean usingHaxeToolkit() {
    return null != myHaxeToolkit;
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
      myFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      myFixture = null;
      super.tearDown();
    }
  }


  /**
   * Return relative path to the test data. Path is relative to the
   * {@link PathManager#getHomePath()}
   *
   * @return relative path to the test data.
   */
  @NonNls
  abstract protected String getBasePath();

  /**
   * Return absolute path to the test data. Not intended to be overridden.
   *
   * @return absolute path to the test data.
   */
  @NonNls
  //protected String getTestDataPath() {
  //  return PathManager.getHomePath().replace(File.separatorChar, '/') + getBasePath();
  //}
  public String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }

  @SuppressWarnings("rawtypes")
  protected void tuneFixture(final ModuleFixtureBuilder moduleBuilder) throws Exception {
  }

  protected Project getProject() {
    return myFixture.getProject();
  }

  protected PsiManagerEx getPsiManager() {
    return PsiManagerEx.getInstanceEx(getProject());
  }

  public PsiElementFactory getElementFactory() {
    return JavaPsiFacade.getElementFactory(getProject());
  }

  protected Module getModule() {
    return myFixture.getModule();
  }

  /**
   * Use reflection to load an annotator inspection class.
   * Specific to annotation tests, but placed here just to avoid adding yet another single-function base class.
   * <p>
   * When we don't support versions of the plugin prior to v2016.1, we can revert the code to importing
   * the classes directly and get rid of this function.
   *
   * @return - An annotator-based inspection class instance.
   */
  protected InspectionProfileEntry getAnnotatorBasedInspection() {
    HighlightVisitorBasedInspection inspection = new HighlightVisitorBasedInspection();
    inspection.setHighlightErrorElements(false);
    inspection.setRunAnnotators(true);
    return inspection;
  }

  public void setTestStyleSettings() {
    setTestStyleSettings(2);
  }

  public void setTestStyleSettings(int indent) {
    Project project = getProject();
    CodeStyleSettings currSettings = CodeStyleSettingsManager.getSettings(project);
    assertNotNull(currSettings);
    CodeStyleSettings tempSettings = currSettings.clone();
    CodeStyleSettings.IndentOptions indentOptions = tempSettings.getIndentOptions(HaxeFileType.HAXE_FILE_TYPE);
    indentOptions.INDENT_SIZE = indent;
    assertNotNull(indentOptions);
    CodeStyleSettingsManager.getInstance(project).setTemporarySettings(tempSettings);
  }

  public String[] useToolkitFiles(String... files) {
    List<String> tkFiles = HaxeTestUtils.useToolkitFiles(this, HaxeTestUtils.LATEST, files);
    return tkFiles.toArray(new String[tkFiles.size()]);
  }

  public void useHaxeToolkit() {
    useHaxeToolkit(HaxeTestUtils.LATEST);
  }

  public void useHaxeToolkit(String version) {
    String relativeParent = HaxeTestUtils.getAbsoluteToolkitPath(version);
    assert (null != relativeParent);
    myHaxeToolkit = relativeParent;
  }

  public CodeInsightTestFixture getFixture() {
    return myFixture;
  }

  public static class MyHaxeModuleFixtureBuilderImpl extends ModuleFixtureBuilderImpl {
    public MyHaxeModuleFixtureBuilderImpl(final TestFixtureBuilder<? extends IdeaProjectTestFixture> testFixtureBuilder) {
      super(new HaxeModuleType(), testFixtureBuilder);
    }

    @NotNull
    @Override
    protected ModuleFixture instantiateFixture() {
      return new ModuleFixtureImpl(this);
    }
  }
}
