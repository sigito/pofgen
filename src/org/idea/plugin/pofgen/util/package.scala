package org.idea.plugin.pofgen

import com.intellij.psi.{PsiMethod, PsiField}
import com.intellij.refactoring.psi.PropertyUtils

/**
 * @author sigito
 */
package object util {

  implicit class PsiFieldAsBean(val field: PsiField) extends AnyVal {
    def getter: PsiMethod = PropertyUtils.findGetterForField(field)

    def setter: PsiMethod = PropertyUtils.findSetterForField(field)
  }

}
