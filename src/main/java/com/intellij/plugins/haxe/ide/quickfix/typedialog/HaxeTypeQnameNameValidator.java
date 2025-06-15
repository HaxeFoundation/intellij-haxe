package com.intellij.plugins.haxe.ide.quickfix.typedialog;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.NlsSafe;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class HaxeTypeQnameNameValidator implements InputValidator {
    private final static Predicate<String> qNameTest = Pattern.compile("([^<>:()\\[\\]])+(\\.[^<>:()\\[\\]]+)*").asMatchPredicate();

    @Override
    public boolean checkInput(@NlsSafe String inputString) {
        return isValid(inputString);
    }

    @Override
    public boolean canClose(@NlsSafe String inputString) {
        return isValid(inputString);
    }

    private static boolean isValid(String inputString) {
//        PsiDirectoryFactory.getInstance(myProject).isValidPackageName()
        if(!qNameTest.test(inputString)) return false;
        int index = inputString.lastIndexOf(".");
        return Character.isUpperCase(inputString.charAt(index+1));
    }
}
