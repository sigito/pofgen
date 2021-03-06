package org.idea.plugin.pofgen.generation

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{JavaPsiFacade, PsiManager, PsiElementFactory, PsiClass}

/**
 * @author sigito
 */
class GenerationContext(
                         val project: Project,
                         val manager: PsiManager,
                         val elementFactory: PsiElementFactory
                         ) {
  def findClass(name: String): PsiClass =
    JavaPsiFacade.getInstance(project).findClass(name, GlobalSearchScope.allScope(project))
}

object GenerationContext {
  def apply(clazz: PsiClass): GenerationContext = {
    val project: Project = clazz.getProject
    new GenerationContext(project, clazz.getManager, JavaPsiFacade.getElementFactory(project))
  }
}
