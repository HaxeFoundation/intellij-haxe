package com.intellij.plugins.haxe.ide;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections;
import com.intellij.plugins.haxe.ide.inspections.*;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class HaxeUnusedAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Override
    protected String getBasePath() {
        return "/annotation.unused/";
    }

    private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                        @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                        String... additionalFiles)
            throws Exception {
        myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
        myFixture.enableInspections(getAnnotatorBasedInspection());
        registerInspectionsForTesting(new HaxeSemanticAnnotatorInspections.Registrar(), myFixture.getProject(), unsetInspections);
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
    }

    public void registerInspectionsForTesting(InspectionToolProvider provider, Project project,
                                              @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections) {
        InspectionProfileManager mgr = InspectionProfileManager.getInstance(project);
        InspectionProfileImpl profile = mgr.getCurrentProfile();

        try {
            Class<? extends LocalInspectionTool>[] classes = provider.getInspectionClasses();
            for (Class<? extends LocalInspectionTool> c : classes) {
                if (null != unsetInspections && unsetInspections.contains(c)) continue;

                Constructor<? extends LocalInspectionTool> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                InspectionToolWrapper<?, ?> wrapper = new LocalInspectionToolWrapper(constructor.newInstance());

                Map<String, List<String>> dependencies = new HashMap<>();
                profile.addTool(project, wrapper, dependencies);
                profile.enableTool(wrapper.getShortName(), project);
            }
        } catch (Exception ex) {
            assertNotNull(ex.toString());
        }
    }

    private void doTest(String... additionalFiles) throws Exception {
        doTest(true, false, true, null, additionalFiles);
    }

    @Test
    public void testUnusedFieldsAndVariablesTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFieldInspection.class,
                HaxeUnusedLocalVarInspection.class
        );
        doTest("UnusedFieldsTestOutside.hx");
    }

    @Test
    public void testUnusedMethodsAndFunctionsTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest("UnusedMethodsTestOutside.hx");
    }

    @Test
    public void testUnusedMethodAbstractImplTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest();
    }

    @Test
    public void testUnusedModuleLevelFunctionTest() throws Exception {
        myFixture.enableInspections(
                HaxeUnusedFunctionInspection.class,
                HaxeUnusedMethodInspection.class
        );
        doTest();
    }
}
