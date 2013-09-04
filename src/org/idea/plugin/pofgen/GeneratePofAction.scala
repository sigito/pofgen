package org.idea.plugin.pofgen

import com.intellij.openapi.actionSystem.{LangDataKeys, PlatformDataKeys, AnActionEvent, AnAction}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.WriteCommandAction.Simple
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._
import com.intellij.psi.codeStyle.{CodeStyleManager, JavaCodeStyleManager}
import com.intellij.psi.util.{ClassUtil, PsiTreeUtil}
import org.idea.plugin.pofgen.generation.{SerializableField, PofSerializerUtils}

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
    val psiFile = Option(e.getData(LangDataKeys.PSI_FILE))
    val editor = Option(e.getData(PlatformDataKeys.EDITOR))

    for {
      file <- psiFile
      ed <- editor
      offset = ed.getCaretModel.getOffset
      elementAt: PsiElement = file.findElementAt(offset)
    } yield PsiTreeUtil.getParentOfType(elementAt, classOf[PsiClass])
  }

  private def executeGenerationAction(clazz: PsiClass, fields: Seq[PsiField]): Unit = {
    val action: WriteCommandAction[_] = new Simple(clazz.getProject, clazz.getContainingFile) {
      override def run(): Unit = generateSerializer(clazz, fields.zipWithIndex.map {
        case (field, index) => new SerializableField(field, index)
      })
    }

    action.execute()
  }
}
