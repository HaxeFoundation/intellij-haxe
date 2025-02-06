package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.PsiElement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class AssignExplanation {
    final List<String> missingMembers = new ArrayList<>();
    final List<String> missingModel = new ArrayList<>();
    final Map<String, String> wrongTypeMembers = new HashMap<>();
    final Map<PsiElement, String> wrongTypePsi = new HashMap<>();

    public void addMissingModel(String name) {
        missingModel.add(name);
    }

    public boolean hasMissingModel() {
        return !missingModel.isEmpty();
    }

    public void addMissingMember(String name) {
        missingMembers.add(name);
    }

    public void addWrongTypeMember(String have, String wants, PsiElement psi) {
        wrongTypeMembers.put(have, wants);
        wrongTypePsi.put(psi, "have '" + have + "' wants '" + wants + "'");
    }

    public boolean hasMissingMembers() {
        return !missingMembers.isEmpty();
    }

    public boolean hasWrongTypeMembers() {
        return !wrongTypeMembers.isEmpty();
    }

    // TODO use map to generate errors
    public Map<PsiElement, String> getWrongTypeMap() {
        return wrongTypePsi;
    }


    public String createMissingMembersMessage() {
        return Strings.join(getMissingMembers(), ", ");
    }

    public String createWrongTypeMembersMessage() {
        return getWrongTypeMembers().entrySet().stream()
                //TODO bundle
                .map(entry -> "Incompatible type:  have '" + entry.getKey() + "' wants '" + entry.getValue() + "'")
                .collect(Collectors.joining(", "));
    }

    public void clearErrors() {
        missingMembers.clear();
        wrongTypeMembers.clear();
    }
}
