package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.HaxeUnresolvedSymbolInspection;
import com.intellij.util.ArrayUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeavyHaxeQuickFixTest extends HaxeCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        useHaxeToolkit();
        super.setUp();
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/unresolved/heavy/method/";
    }

    public void testCreateMethodForArrayLiteral() throws Exception {
        doTestQuickFix("Create method 'unresolvedMethod'");
    }

    public void testCreateMethodForObjectLiteral() throws Exception {
        doTestQuickFix("Create method 'myMethod'");
    }

    public void testGenerateMethodInOtherClass() throws Exception {
        doTestQuickFix("Create method 'testMethodInDifferentClass'", "OtherClassForGeneration");
    }


    protected void doTestQuickFix(String actionToPerform) throws Exception {
        String testName = getTestName(false);
        doTestQuickFix(actionToPerform, testName, testName);
    }
    protected void doTestQuickFix(String actionToPerform, String resultName) throws Exception {
        String testName = getTestName(false);
        doTestQuickFix(actionToPerform, testName, resultName);
    }

    protected void doTestQuickFix(String actionToPerform, String testName, String resultName) throws Exception {

        String fileWithQuickFix = testName + ".hx";
        Set<String> fileSet = new HashSet<>();
        fileSet.add(fileWithQuickFix);
        fileSet.add(resultName + ".hx");
        fileSet.add(resultName + "_preview.hx");

        myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
        myFixture.configureByFiles(fileSet.toArray(String[]::new));
        myFixture.enableInspections(getAnnotatorBasedInspection());


        List<IntentionAction> allQuickFixes = myFixture.getAllQuickFixes(fileWithQuickFix);
        // Hackish way to filter out  "hidden" unresolved Symbol quickfixes (the same quickfix is used for both warning and info Problem descriptor)
        HashSet<IntentionAction> intentionActions = new HashSet<>(allQuickFixes);
        for (final IntentionAction action : intentionActions) {
            String actionText = action.getText();
            if (actionToPerform.equals(actionText)) {
                System.out.println("Applying Quickfix " + actionText);
                String previewText = myFixture.getIntentionPreviewText(actionText);
                myFixture.checkResult(resultName + "_preview.hx", previewText, true);
                myFixture.launchAction(action);
            }
            else {
                System.out.println("Ignoring Quickfix " + actionText + ", not matching " + actionToPerform);
            }
        }
        FileDocumentManager.getInstance().saveAllDocuments();
        myFixture.checkResultByFile(resultName+".hx", resultName + "_expected.hx", true);
    }


}
