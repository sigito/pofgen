package org.idea.plugin.pofgen

import com.intellij.psi.{PsiType, PsiPrimitiveType, PsiMethod, PsiField}
import com.intellij.psi.util.PropertyUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.codeStyle.{SuggestedNameInfo, VariableKind, JavaCodeStyleManager}
import java.util

/**
 * @author sigito
 */
class SerializableField(val psiField: PsiField, val index: Int) {
  val typeName = psiField.getType.getCanonicalText
  val name: String = psiField.getName
  val getter: PsiMethod = PropertyUtil.findGetterForField(psiField)
  val setter: PsiMethod = PropertyUtil.findSetterForField(psiField)
  val indexName: String = {
    val codeStyleManager = JavaCodeStyleManager.getInstance(psiField.getProject)
    val suggestedNames: Array[String] = codeStyleManager.suggestVariableName(VariableKind.STATIC_FINAL_FIELD, name, null, null).names

    if (!suggestedNames.isEmpty) suggestedNames(0)
    else name.toUpperCase
  }
}