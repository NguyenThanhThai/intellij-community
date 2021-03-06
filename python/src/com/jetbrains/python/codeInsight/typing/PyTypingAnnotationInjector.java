/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.jetbrains.python.codeInsight.typing;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.codeInsight.PyInjectionUtil;
import com.jetbrains.python.codeInsight.PyInjectorBase;
import com.jetbrains.python.codeInsight.functionTypeComments.PyFunctionTypeAnnotationDialect;
import com.jetbrains.python.documentation.doctest.PyDocstringLanguageDialect;
import com.jetbrains.python.psi.PyAnnotation;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects fragments for type annotations either in string literals (quoted annotations containing forward references) or
 * in type comments starting with <tt># type:</tt>.
 *
 * @author vlan
 */
public class PyTypingAnnotationInjector extends PyInjectorBase {
  public static final Pattern RE_TYPING_ANNOTATION = Pattern.compile("\\s*\\S+(\\[.*\\])?\\s*");

  @Override
  protected PyInjectionUtil.InjectionResult registerInjection(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    final PyInjectionUtil.InjectionResult result = super.registerInjection(registrar, context);
    if (result == PyInjectionUtil.InjectionResult.EMPTY && context.getContainingFile() instanceof PyFile && 
        context instanceof PsiComment && context instanceof PsiLanguageInjectionHost) {
      return registerCommentInjection(registrar, (PsiLanguageInjectionHost)context);
    }
    return result;
  }

  @Nullable
  @Override
  public Language getInjectedLanguage(@NotNull PsiElement context) {
    if (context instanceof PyStringLiteralExpression) {
      final PyStringLiteralExpression expr = (PyStringLiteralExpression)context;
      if (PsiTreeUtil.getParentOfType(context, PyAnnotation.class, true) != null && isTypingAnnotation(expr.getStringValue())) {
        return PyDocstringLanguageDialect.getInstance();
      }
    }
    return null;
  }

  @NotNull
  private static PyInjectionUtil.InjectionResult registerCommentInjection(@NotNull MultiHostRegistrar registrar,
                                                                          @NotNull PsiLanguageInjectionHost host) {
    final String text = host.getText();
    final Matcher m = PyTypingTypeProvider.TYPE_COMMENT_PATTERN.matcher(text);
    if (m.matches()) {
      final String annotationText = m.group(1);
      if (annotationText != null) {
        final int start = m.start(1);
        final int end = m.end(1);
        if (start < end && allowInjectionInComment(host)) {
          Language language = null;
          if ("ignore".equals(annotationText)) {
            language = null;
          }
          else if (isFunctionTypeComment(host)) {
            language = PyFunctionTypeAnnotationDialect.INSTANCE;
          }
          else if (isTypingAnnotation(annotationText)) {
            language = PyDocstringLanguageDialect.getInstance();
          }
          if (language != null) {
            registrar.startInjecting(language);
            registrar.addPlace("", "", host, TextRange.create(start, end));
            registrar.doneInjecting();
            return new PyInjectionUtil.InjectionResult(true, true);
          }
        }
      }
    }
    return PyInjectionUtil.InjectionResult.EMPTY;
  }

  private static boolean isFunctionTypeComment(@NotNull PsiElement comment) {
   final PyFunction function = PsiTreeUtil.getParentOfType(comment, PyFunction.class);
    return function != null && function.getTypeComment() == comment;
  }

  private static boolean isTypingAnnotation(@NotNull String s) {
    return RE_TYPING_ANNOTATION.matcher(s).matches();
  }

  private static boolean allowInjectionInComment(@NotNull PsiLanguageInjectionHost host) {
    // XXX: Don't inject PyDocstringLanguage during completion inside comments due to an exception related to finding ShredImpl's
    // hostElementPointer
    return CompletionUtil.getOriginalOrSelf(host) == host;
  }
}
