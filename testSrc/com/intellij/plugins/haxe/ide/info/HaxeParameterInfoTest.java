package com.intellij.plugins.haxe.ide.info;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext;
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext;
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterInfoTest extends LightCodeInsightTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/paramInfo/");
  }

  private void doTest(String infoText, int highlightedParameterIndex) throws Exception {
    configureByFile(getTestName(false) + ".hx");

    HaxeParameterInfoHandler parameterInfoHandler = new HaxeParameterInfoHandler();
    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myEditor, myFile);
    PsiElement elt = parameterInfoHandler.findElementForParameterInfo(createContext);
    assertNotNull(elt);
    parameterInfoHandler.showParameterInfo(elt, createContext);
    Object[] items = createContext.getItemsToShow();
    assertTrue(items != null);
    assertTrue(items.length > 0);
    MockParameterInfoUIContext context = new MockParameterInfoUIContext<PsiElement>(elt);
    parameterInfoHandler.updateUI((HaxeFunctionDescription)items[0], context);
    assertEquals(infoText, parameterInfoHandler.myParametersListPresentableText);

    // index check
    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myEditor, myFile);
    final PsiElement element = parameterInfoHandler.findElementForUpdatingParameterInfo(updateContext);
    parameterInfoHandler.updateParameterInfo(element, updateContext);
    assertEquals(highlightedParameterIndex, updateContext.getCurrentParameter());
  }

  public void testParamInfo1() throws Throwable {
    doTest("p1:Int, p2, p3:Node", 0);
  }

  public void testParamInfo2() throws Throwable {
    doTest("p1:Int, p2, p3:Node", 2);
  }
}
