package org.idea.plugin.pofgen.generation

import com.intellij.psi.{PsiMethod, PsiClass}

/**
 * @author sigito
 */
class EntityClass(val clazz: PsiClass,
                  val constructor: PsiMethod,
                  val constructorFields: IndexedSeq[Int],
                  val restFields: IndexedSeq[Int],
                  val fields: IndexedSeq[EntityField]
                   ) {
  def name: String = clazz.getName

  def fullName: String = clazz.getQualifiedName
}
