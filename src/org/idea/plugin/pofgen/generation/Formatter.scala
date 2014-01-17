package org.idea.plugin.pofgen.generation

import com.intellij.psi.codeStyle.{CodeStyleManager, JavaCodeStyleManager}
import com.intellij.psi.PsiElement

/**
 * @author sigito
 */
object Formatter {
  def format(element: PsiElement, context: GenerationContext) = {
    // process code formatting
    JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(element)
    JavaCodeStyleManager.getInstance(context.project).optimizeImports(element.getContainingFile)
    CodeStyleManager.getInstance(context.project).reformat(element)
  }
}
