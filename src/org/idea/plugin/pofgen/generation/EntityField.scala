package org.idea.plugin.pofgen.generation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.codeStyle.{VariableKind, JavaCodeStyleManager}
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi._
import scala.Some

/**
 * @author sigito
 */
class EntityField(val psiField: PsiField, val index: Int, val needSetter: Boolean = true) {
  private val log: Logger = Logger.getInstance(getClass)

  val typeName = psiField.getType.getCanonicalText

  val name: String = psiField.getName

  val getter: PsiMethod = PropertyUtil.findGetterForField(psiField)

  // can be empty if final field and set
  val setter: Option[PsiMethod] =
    if (needSetter) try {
      Option(PropertyUtil.findSetterForField(psiField))
    } catch {
      case e: Exception =>
        log.warn(s"Setter for field $psiField not found.", e)
        None
    }
    else None

  val indexName: String = {
    // ask IntelliJ for suggestions
    val codeStyleManager = JavaCodeStyleManager.getInstance(psiField.getProject)
    val suggestedNames = codeStyleManager.suggestVariableName(VariableKind.STATIC_FINAL_FIELD, name, null, null).names

    // task first one if any exists
    suggestedNames.headOption.getOrElse(name.toUpperCase)
  }
}