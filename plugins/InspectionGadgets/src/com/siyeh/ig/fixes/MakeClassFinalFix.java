/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.siyeh.ig.fixes;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Query;
import com.intellij.util.containers.MultiMap;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.psi.PsiModifier.ABSTRACT;
import static com.intellij.psi.PsiModifier.FINAL;

/**
* @author Bas Leijdekkers
*/
public class MakeClassFinalFix extends InspectionGadgetsFix {

  private final String className;

  public MakeClassFinalFix(PsiClass aClass) {
    className = aClass.getName();
  }

  @Override
  @NotNull
  public String getName() {
    return InspectionGadgetsBundle.message(
      "make.class.final.fix.name", className);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return "Make class final";
  }

  @Override
  protected void doFix(Project project, ProblemDescriptor descriptor) throws IncorrectOperationException {
    final PsiElement element = descriptor.getPsiElement();
    final PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    if (containingClass == null) {
      return;
    }
    final PsiModifierList modifierList = containingClass.getModifierList();
    if (modifierList == null) {
      return;
    }
    if (!isOnTheFly()) {
      if (ClassInheritorsSearch.search(containingClass).findFirst() != null) {
        return;
      }
      doMakeFinal(modifierList);
      return;
    }
    final MultiMap<PsiElement, String> conflicts = new MultiMap<>();
    final Query<PsiClass> search = ClassInheritorsSearch.search(containingClass);
    search.forEach(aClass -> {
      conflicts.putValue(containingClass, InspectionGadgetsBundle
        .message("0.will.no.longer.be.overridable.by.1", RefactoringUIUtil.getDescription(containingClass, false),
                 RefactoringUIUtil.getDescription(aClass, false)));
      return true;
    });
    final boolean conflictsDialogOK;
    if (!conflicts.isEmpty()) {
      ConflictsDialog conflictsDialog = new ConflictsDialog(element.getProject(), conflicts, () -> doMakeFinal(modifierList));
      conflictsDialogOK = conflictsDialog.showAndGet();
    } else {
      conflictsDialogOK = true;
    }
    if (conflictsDialogOK) {
      doMakeFinal(modifierList);
    }
  }

  private static void doMakeFinal(PsiModifierList modifierList) {
    WriteAction.run(() -> {
      modifierList.setModifierProperty(FINAL, true);
      modifierList.setModifierProperty(ABSTRACT, false);
    });
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
    return currentFile;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
