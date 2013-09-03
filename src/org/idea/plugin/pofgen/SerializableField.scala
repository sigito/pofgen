package org.idea.plugin.pofgen

import com.intellij.psi.codeStyle.{VariableKind, JavaCodeStyleManager}
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.{PsiMethod, PsiField}

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
    val suggestedNames = codeStyleManager.suggestVariableName(VariableKind.STATIC_FINAL_FIELD, name, null, null).names

    if (!suggestedNames.isEmpty) suggestedNames(0)
    else name.toUpperCase
  }
}