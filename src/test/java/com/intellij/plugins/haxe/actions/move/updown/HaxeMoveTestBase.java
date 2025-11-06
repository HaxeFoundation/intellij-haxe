package com.intellij.plugins.haxe.actions.move.updown;

import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementDownAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementUpAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import lombok.CustomLog;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@CustomLog
public abstract class HaxeMoveTestBase extends LightPlatformCodeInsightTestCase {

    protected void doTest() throws Exception {
        doTest("hx", 1, 1);
    }

    protected void doTest(boolean down, boolean up) throws Exception {
        doTest("hx", down ? 1 : 0, up ? 1 : 0);
    }

    protected void doTest(int down, int up) throws Exception {
        doTest("hx", down, up);
    }

    protected void doTest(String ext, int downCount, int upCount) throws Exception {
        final String baseName = getBasePath() + '/' + getTestName(false);
        final String fileName = baseName + "." + ext;

        configureByFile(fileName);
        for (int i = 0; i < upCount; i++) {
            log.info("Performing up move #" + i);
            @NonNls String afterFileName = baseName + "_afterUp." + ext;
            EditorActionHandler handler = new MoveStatementUpAction().getHandler();
            performAction(fileName, handler, afterFileName, i);
        }

        configureByFile(fileName);
        for (int i = 0; i < downCount; i++) {
            log.info("Performing down move #" + i);
            @NonNls String afterFileName = baseName + "_afterDown." + ext;
            EditorActionHandler handler = new MoveStatementDownAction().getHandler();
            performAction(fileName, handler, afterFileName, i);
        }
    }

    protected void doTestAndAlsoCheckReversed(String ext, int actionCount) throws Exception {
        final String baseName = getBasePath() + '/' + getTestName(false);
        final String fileName = baseName + "." + ext;

        configureByFile(fileName);

        for (int i = 0; i < actionCount + 1; i++) {
            log.info("Performing down move #" + i);
            @NonNls String afterFileName = baseName + "_after" + countValue(i) + "." + ext;
            EditorActionHandler handler = new MoveStatementDownAction().getHandler();
            performAction(fileName, handler, afterFileName, i);
        }
        for (int i = actionCount - 1; i > 0; i--) {
            log.info("Performing up move #" + i);
            @NonNls String afterFileName = baseName + "_after" + countValue(i) + "." + ext;
            EditorActionHandler handler = new MoveStatementUpAction().getHandler();
            performAction(fileName, handler, afterFileName, actionCount - i);
        }
    }

    private String countValue(int i) {
        return "_" + i;
    }

    private void performAction(final String fileName, final EditorActionHandler handler, final String afterFileName, int actionCount) throws Exception {
        if (handler.isEnabled(getEditor(), null, null)) {
            WriteCommandAction.runWriteCommandAction(null, () -> handler.execute(getEditor(), null, null));
        }
        checkResultByFile(afterFileName);
    }

    protected abstract String getBasePath();

    @NotNull
    @Override
    protected String getTestDataPath() {
        return HaxeTestUtils.BASE_TEST_DATA_PATH;
    }


}
