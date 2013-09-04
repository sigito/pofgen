package org.idea.plugin.pofgen

import com.intellij.openapi.actionSystem.{LangDataKeys, PlatformDataKeys, AnActionEvent, AnAction}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.WriteCommandAction.Simple
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._
import com.intellij.psi.codeStyle.{CodeStyleManager, JavaCodeStyleManager}
import com.intellij.psi.util.PsiTreeUtil

/**
 * @author sigito
 */
class GeneratePofAction() extends AnAction() {
  val POF_SERIALIZER: String = "com.tangosol.io.pof.PofSerializer"
  val POF_SERIALIZER_SUFFIX: String = "PofSerializer"

  val SERIALIZE_METHOD: String = "serialize"
  //  val POF_WRITER: String = "com.tangosol.io.pof.PofWriter pofWriter"
  val POF_WRITER: String = "pofWriter"

  val DESERIALIZE_METHOD: String = "deserialize"
  //  val POF_READER: String = "com.tangosol.io.pof.PofReader pofReader"
  val POF_READER: String = "pofReader"

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

  private def generateSerializer(clazz: PsiClass, fields: Seq[SerializableField]): Unit = {
    implicit val elementFactory = JavaPsiFacade.getElementFactory(clazz.getProject)
    // load serializer class
    val serializer: PsiClass = createSerializer(clazz)
    // add read/write indexes for fields
    addIndexes(serializer, fields)
    // add write method
    serializer.add(writeMethod(serializer, clazz, fields))
    // add read method
    serializer.add(readMethod(serializer, clazz, fields))

    // create file
    val parent: PsiDirectory = clazz.getContainingFile.getParent
    val containingFile = serializer.getContainingFile.setName(s"${serializer.getName}.java")
    val serializerFile = parent.add(containingFile).asInstanceOf[PsiFile]

    // process code formatting
    JavaCodeStyleManager.getInstance(clazz.getProject).shortenClassReferences(serializerFile)
    CodeStyleManager.getInstance(clazz.getProject).reformat(serializerFile)
    serializerFile.navigate(true)
  }

  private def createSerializer(clazz: PsiClass)(implicit elementFactory: PsiElementFactory): PsiClass = {
    // load create new class for serializer
    val serializerClass = elementFactory.createClass(clazz.getName + POF_SERIALIZER_SUFFIX)

    // implement PofSerializer
    val implementsReferenceElement = elementFactory.createReferenceFromText(POF_SERIALIZER, serializerClass)
    serializerClass.getImplementsList.add(implementsReferenceElement)
    serializerClass.getModifierList.setModifierProperty(PsiModifier.PUBLIC, true)
    serializerClass
  }

  private def addIndexes(serializerClass: PsiClass, fields: Seq[SerializableField])(implicit elementFactory: PsiElementFactory): Unit =
    fields.foreach {
      field =>
      // create static field
        val indexConstant = elementFactory.createField(field.indexName, PsiType.INT)
        indexConstant.getModifierList.setModifierProperty(PsiModifier.PRIVATE, true)
        indexConstant.getModifierList.setModifierProperty(PsiModifier.STATIC, true)
        indexConstant.getModifierList.setModifierProperty(PsiModifier.FINAL, true)
        indexConstant.setInitializer(elementFactory.createExpressionFromText(field.index.toString, serializerClass))
        // add constant field to serializer class
        serializerClass.add(indexConstant)
    }

  private def writeMethod(serializer: PsiClass, clazz: PsiClass, fields: Seq[SerializableField])(implicit elementFactory: PsiElementFactory): PsiMethod = {
    val code = new StringBuilder("public void serialize(com.tangosol.io.pof.PofWriter pofWriter, java.lang.Object o) throws java.io.IOException {")

    // declare serialize object instance and cast
    val instanceClassName: String = clazz.getQualifiedName
    val instanceName = StringUtil.decapitalize(clazz.getName)
    code ++= instanceClassName ++= " " ++= instanceName
    // cast and assign input object to our class type
    code.append(" = (") ++= instanceClassName ++= ") " ++= "o;"

    // write every field
    fields.foreach(PofSerializerUtils.addWriteMethod(code, instanceName, _, "pofWriter"))

    // write remainder
    code.append("pofWriter").append(".writeRemainder(null);")

    code.append("}")
    elementFactory.createMethodFromText(code.toString(), serializer)
  }

  private def readMethod(serializer: PsiClass, clazz: PsiClass, fields: Seq[SerializableField])(implicit elementFactory: PsiElementFactory): PsiMethod = {
    val instanceName = StringUtil.decapitalize(clazz.getName)
    val instanceClassName: String = clazz.getQualifiedName

    val code = new StringBuilder("public ") ++= instanceClassName ++= " deserialize(com.tangosol.io.pof.PofReader pofReader) throws java.io.IOException {"

    // declare deserialize object instance and initialize
    code ++= instanceClassName ++= " " ++= instanceName
    code.append(" = new ") ++= instanceClassName ++= "();"

    // read every field
    fields.foreach(PofSerializerUtils.addReadMethod(code, instanceName, _, "pofReader"))

    // read remainder
    code.append("pofReader").append(".readRemainder();")

    code.append("return ") ++= instanceName ++= ";"
    code.append("}")
    elementFactory.createMethodFromText(code.toString(), serializer)
  }
}
