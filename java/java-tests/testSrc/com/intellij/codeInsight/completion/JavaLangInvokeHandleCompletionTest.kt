/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion

import com.intellij.JavaTestUtil
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

/**
 * @author Pavel Dolgov
 */
class JavaLangInvokeHandleCompletionTest : LightFixtureCompletionTestCase() {

  override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_9

  override fun getBasePath() = JavaTestUtil.getRelativeJavaTestDataPath() + "/codeInsight/completion/invokeHandle/"


  fun testVirtual() = doTestFirst(1, "m1", "pm1", "m2")
  fun testVirtualPrefixed() = doTest(1, "m1", "m2", "pm1")

  fun testStatic() = doTest(0, "psm1", "sm1", "sm2")

  fun testGetter() = doTest(0, "f1", "pf1", "f2")
  fun testSetter() = doTest(2, "f1", "pf1", "f2")

  fun testStaticGetter() = doTest(0, "psf1", "sf1", "sf2")
  fun testStaticSetter() = doTest(2, "psf1", "sf1", "sf2")

  fun testVarHandle() = doTest(0, "f1", "pf1", "f2")
  fun testStaticVarHandle() = doTest(0, "psf1", "sf1", "sf2")


  fun testVirtualType() = doTestTypes(0, "MethodType.methodType(String.class)")
  fun testStaticType() = doTestTypes(0, "MethodType.methodType(Object.class, Object.class)")

  fun testGetterType() = doTestTypes(0, "int.class")
  fun testSetterType() = doTestTypes(0, "float.class")

  fun testVarHandleType() = doTestTypes(0, "String.class")
  fun testStaticVarHandleType() = doTestTypes(0, "Object.class")

  fun testVirtualTypeGeneric() = doTestTypes(0, "MethodType.methodType(Object.class, Object.class, String.class)")
  fun testStaticTypeGeneric() = doTestTypes(0, "MethodType.methodType(Object.class, List.class, Object[].class)")

  fun testConstructorType1() = doTestTypes(0, "MethodType.methodType(void.class)")
  fun testConstructorType2() = doTestTypes(0,
                                           "MethodType.methodType(void.class)",
                                           "MethodType.methodType(void.class, int.class)",
                                           "MethodType.methodType(void.class, List.class)",
                                           "MethodType.methodType(void.class, Object[].class)")
  fun testConstructorType3() = doTestTypes(3,
                                           "MethodType.methodType(void.class)",
                                           "MethodType.methodType(void.class, int.class)",
                                           "MethodType.methodType(void.class, List.class)",
                                           "MethodType.methodType(void.class, Object[].class)")


  private fun doTest(index: Int, vararg expected: String) {
    doTest(index, { assertLookupTexts(false, *expected) })
  }

  private fun doTestFirst(index: Int, vararg expected: String) {
    doTest(index, { assertLookupTexts(true, *expected, "clone") })
  }

  private fun doTestTypes(index: Int, vararg expected: String) {
    myFixture.addClass("""
import java.util.List;
public class Types extends Parent {
  String str;
  static Object sObj;

  String strMethod() {return "";}
  static Object sObjMethod(Object o) {return this;}
  <T> T genericMethod(T t, String s) {return t;}
  static <T> T sGenericMethod(List<T> lst, T... ts) {return ts[0];}
}""")
    myFixture.addClass("""
import java.util.List;
public class Constructed<T> {
  Constructed() {}
  Constructed(int n) {}
  Constructed(List<T> a) {}
  Constructed(T... a) {}
}""")

    doTest(index, { assertLookupTexts(true, *expected) })
  }

  private fun assertLookupTexts(compareFirst: Boolean, vararg expected: String) {
    val elements = myFixture.lookupElements
    assertNotNull(elements)
    val lookupTexts = elements!!.map {
      val presentation = LookupElementPresentation()
      it.renderElement(presentation)
      presentation.itemText
    }

    val actual = if (compareFirst) lookupTexts.subList(0, Math.min(expected.size, lookupTexts.size)) else lookupTexts
    assertOrderedEquals(actual, *expected)
  }

  private fun doTest(index: Int, assertion: () -> Unit) {
    myFixture.addClass("""
public class Parent {
  public int pf1;
  private float pf2;
  public static char psf1;
  private static short psf2;

  public void pm1(int n) {}
  private void pm2(float n, double m) {}
  public static void psm1(char n) {}
  private static void psm2(short n) {}
}""")
    myFixture.addClass("""
public class Test extends Parent {
  public int f1;
  private float f2;
  public static char sf1;
  private static short sf2;

  public void m1(int n) {}
  private void m2(float n, double m) {}
  public static void sm1(char n) {}
  private static void sm2(short n) {}
}""")

    configureByFile(getTestName(false) + ".java")
    assertion()
    if (index >= 0) selectItem(lookup.items[index])
    myFixture.checkResultByFile(getTestName(false) + "_after.java")
  }
}
