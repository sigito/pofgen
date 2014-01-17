package org.idea.plugin.pofgen

import com.intellij.psi.{PsiMethod, PsiField}
import com.intellij.psi.util.PropertyUtil

/**
 * @author sigito
 */
package object util {

  implicit class PsiFieldAsBean(val field: PsiField) extends AnyVal {
    def getter: PsiMethod = PropertyUtil.findGetterForField(field)

    def setter: PsiMethod = PropertyUtil.findSetterForField(field)
  }

}
