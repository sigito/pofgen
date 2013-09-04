package org.idea.plugin.pofgen.generation

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._
import com.intellij.psi.codeStyle.{CodeStyleManager, JavaCodeStyleManager}
import com.intellij.psi.util.ClassUtil

/**
 * @author sigito
 */
class SerializerGenerator(val clazz: PsiClass, val fields: Seq[SerializableField]) {
  val SERIALIZER_CLASS_NAME: String = "com.tangosol.io.pof.PofSerializer"
  val SERIALIZER_NAME_SUFFIX: String = "PofSerializer"

  private val project: Project = clazz.getProject
  private val manager: PsiManager = clazz.getManager
  private val javaPsiFacade: JavaPsiFacade = JavaPsiFacade.getInstance(project)
  private val elementFactory: PsiElementFactory = JavaPsiFacade.getElementFactory(project)

  def generate(): Unit = {
    // load serializer class
    val serializer: PsiClass = createSerializer(clazz)
    // add read/write indexes for fields
    addIndexes(serializer, fields)
    // add write method
    serializer.add(writeMethod(serializer, clazz, fields))
    // add read method
    serializer.add(readMethod(serializer, clazz, fields))

    val serializerFile = createFile(serializer)

    // process code formatting
    JavaCodeStyleManager.getInstance(project).shortenClassReferences(serializerFile)
    CodeStyleManager.getInstance(project).reformat(serializerFile)
    serializerFile.navigate(true)
  }

  private def createFile(serializer: PsiClass): PsiFile = {
    val parent: PsiDirectory = clazz.getContainingFile.getParent
    val containingFile = serializer.getContainingFile.setName(s"${serializer.getName}.java")
    parent.add(containingFile).asInstanceOf[PsiFile]
  }

  private def createSerializer(clazz: PsiClass): PsiClass = {
    // load create new class for serializer
    val serializerClass = elementFactory.createClass(clazz.getName + SERIALIZER_NAME_SUFFIX)

    // implement PofSerializer
    val implementsReferenceElement = elementFactory.createReferenceFromText(SERIALIZER_CLASS_NAME, serializerClass)
    serializerClass.getImplementsList.add(implementsReferenceElement)
    serializerClass.getModifierList.setModifierProperty(PsiModifier.PUBLIC, true)
    serializerClass
  }

  private def addIndexes(serializerClass: PsiClass, fields: Seq[SerializableField]): Unit =
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

  private def writeMethod(serializer: PsiClass, clazz: PsiClass, fields: Seq[SerializableField]): PsiMethod = {
    val writerClass = ClassUtil.findPsiClassByJVMName(clazz.getManager, "com.tangosol.io.pof.PofWriter")
    val code = new StringBuilder("public void serialize(com.tangosol.io.pof.PofWriter pofWriter, java.lang.Object o) throws java.io.IOException {")
    // declare serialize object instance and cast
    val instanceClassName: String = clazz.getQualifiedName
    val instanceName = StringUtil.decapitalize(clazz.getName)
    code ++= instanceClassName ++= " " ++= instanceName
    // cast and assign input object to our class type
    code.append(" = (") ++= instanceClassName ++= ") " ++= "o;"

    // write every field
    fields.foreach(PofSerializerUtils.addWriteMethod(code, writerClass, "pofWriter", instanceName, _))

    // write remainder
    code.append("pofWriter").append(".writeRemainder(null);")

    code.append("}")
    elementFactory.createMethodFromText(code.toString(), serializer)

  }

  private def readMethod(serializer: PsiClass, clazz: PsiClass, fields: Seq[SerializableField]): PsiMethod = {
    val readerClass = ClassUtil.findPsiClassByJVMName(clazz.getManager, "com.tangosol.io.pof.PofReader")

    val instanceName = StringUtil.decapitalize(clazz.getName)
    val instanceClassName: String = clazz.getQualifiedName

    val code = new StringBuilder("public ") ++= instanceClassName ++= " deserialize(com.tangosol.io.pof.PofReader pofReader) throws java.io.IOException {"

    // declare deserialize object instance and initialize
    code ++= instanceClassName ++= " " ++= instanceName
    code.append(" = new ") ++= instanceClassName ++= "();"

    // read every field
    fields.foreach(PofSerializerUtils.addReadMethod(code, readerClass, "pofReader", instanceName, _))

    // read remainder
    code.append("pofReader").append(".readRemainder();")

    code.append("return ") ++= instanceName ++= ";"
    code.append("}")
    elementFactory.createMethodFromText(code.toString(), serializer)
  }
}
