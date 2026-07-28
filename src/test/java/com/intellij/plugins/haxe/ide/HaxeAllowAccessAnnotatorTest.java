package com.intellij.plugins.haxe.ide;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@DisplayName("Annotation: allow access annotator")
public class HaxeAllowAccessAnnotatorTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        // for use when idempotence check problems occur and we need consistent results.
        //Registry.get("platform.random.idempotence.check.rate").setValue(1, getTestRootDisposable());
        useHaxeToolkit();
        super.setUp();
        setTestStyleSettings(2);
    }

    @Override
    protected String getBasePath() {
        return "/annotation.access/";
    }

    private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                        @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                        String... additionalFiles)
            throws Exception {
        myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{"accesscontrol/"+getTestName(false) + ".hx"}, additionalFiles));
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
    @DisplayName("static access")
    public void testStaticAccess() throws Exception {
        doTest();
    }

    @Test
    @DisplayName("meta access")
    public void testMetaAccess() throws Exception {
        doTest("accesscontrol/AccessMetaTestClass.hx");
    }


    @Test
    @DisplayName("test method allow on property")
    public void testTestMethodAllowOnProperty() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test class allow on field")
    public void testTestClassAllowOnField() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test class allow on class")
    public void testTestClassAllowOnClass() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }
    @Test
    @DisplayName("test module level")
    public void testTestModuleLevel() throws Exception {
        doTest("accesscontrol/PrivateStaticMembers.hx");
    }


}
