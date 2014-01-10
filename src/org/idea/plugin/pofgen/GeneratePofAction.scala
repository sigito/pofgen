package org.idea.plugin.pofgen

import com.intellij.openapi.actionSystem._
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.WriteCommandAction.Simple
import com.intellij.psi._
import com.intellij.psi.util.PsiTreeUtil
import org.idea.plugin.pofgen.generation.{GenerationContext, Formatter, SerializerGenerator}
import scala.Some

/**
 * @author sigito
 */
class GeneratePofAction() extends AnAction() {
  override def update(e: AnActionEvent) {
    val psiClass = getPsiClassFromContext(e)
    // todo check if coherence lib available
    e.getPresentation.setEnabled(psiClass.isDefined)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    getPsiClassFromContext(e) match {
      case Some(clazz) =>
        val generateDialog = new GenerateDialog(clazz)
        generateDialog.show()
        if (generateDialog.isOK) executeGenerationAction(clazz, generateDialog.selectedFields)
      case None =>
    }
  }

  private def getPsiClassFromContext(e: AnActionEvent): Option[PsiClass] = {
    val psiFile = Option(e.getData(CommonDataKeys.PSI_FILE))
    val editor = Option(e.getData(CommonDataKeys.EDITOR))

    for {
      file <- psiFile
      ed <- editor
      offset = ed.getCaretModel.getOffset
      elementAt: PsiElement = file.findElementAt(offset)
    } yield PsiTreeUtil.getParentOfType(elementAt, classOf[PsiClass])
  }

  private def executeGenerationAction(entityClazz: PsiClass, fields: IndexedSeq[PsiField]): Unit = {
    val action: WriteCommandAction[_] = new Simple(entityClazz.getProject, entityClazz.getContainingFile) {
      override def run(): Unit = {
        val context = GenerationContext(entityClazz)

        val serializerClazz: PsiClass = new SerializerGenerator(entityClazz, fields, context).generate()

        Formatter.format(serializerClazz, context)

        // create file
        val parent: PsiDirectory = entityClazz.getContainingFile.getParent
        val serializerFile = createClassFile(parent, serializerClazz)

        serializerFile.navigate(true)
      }
    }

    action.execute()
  }

  private def createClassFile(dir: PsiDirectory, clazz: PsiClass): PsiFile = {
    val containingFile = clazz.getContainingFile.setName(s"${clazz.getName}.java")
    dir.add(containingFile).asInstanceOf[PsiFile]
  }
}
