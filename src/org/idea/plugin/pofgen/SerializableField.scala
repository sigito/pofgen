package org.idea.plugin.pofgen

import com.intellij.psi.{PsiMethod, PsiField}
import com.intellij.psi.util.PropertyUtil

/**
 * @author sigito
 */
class SerializableField(val psiField: PsiField, val index: Int) {
  val typeName = psiField.getType.getCanonicalText
  val name: String = psiField.getName
  val getter: PsiMethod = PropertyUtil.findGetterForField(psiField)
  val setter: PsiMethod = PropertyUtil.findSetterForField(psiField)
  val indexName: String = psiField.getName.toUpperCase
}