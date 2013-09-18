package org.idea.plugin.pofgen.generation

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._

/**
 * @author sigito
 */
class PofSerializer(context: GenerationContext,
                    entityClass: EntityClass) {
  private val SERIALIZER_CLASS_NAME: String = "com.tangosol.io.pof.PofSerializer"

  private val serializerInterface: PsiClass = context.findClass(SERIALIZER_CLASS_NAME)

  private var serializer: PsiClass = _

  // serialize and deserialize methods
  private val (serialize: PsiMethod, deserialize: PsiMethod) = {
    val methods = serializerInterface.getMethods
    assert(methods.length == 2)

    (if (methods(0).getName == "serialize") methods else methods.reverse) match {
      case Array(serializeMethod, deserializeMethod) => (serializeMethod, deserializeMethod)
    }
  }

  def createClass(): PsiClass = {
    if (serializer != null) return serializer

    // load create new class for serializer
    serializer = context.elementFactory.createClass(s"${entityClass.name}PofSerializer")
    serializer.getModifierList.setModifierProperty(PsiModifier.PUBLIC, true)

    // implement PofSerializer
    serializer.getImplementsList.add(serializerInterface)

    entityClass.fields foreach (f => addIndex(f.indexName, f.index))
    serializer.add(createSerializeMethod())
    serializer.add(createDeserializeMethod())
    serializer
  }

  private def createSerializeMethod(): PsiMethod = {
    val writerClass = context.findClass("com.tangosol.io.pof.PofWriter")
    val code = new StringBuilder("public void serialize(com.tangosol.io.pof.PofWriter pofWriter, java.lang.Object o) throws java.io.IOException {")
    // declare serialize object instance and cast
    val instanceClassName: String = entityClass.fullName
    val instanceName = StringUtil.decapitalize(entityClass.name)
    code ++= instanceClassName ++= " " ++= instanceName
    // cast and assign input object to our class type
    code ++= " = (" ++= instanceClassName ++= ") " ++= "o;"

    // write every field
    entityClass.fields foreach {
      code ++= PofSerializerUtils.writeMethodCall(writerClass, "pofWriter", instanceName, _) ++= ";"
    }

    // write remainder
    code ++= "pofWriter.writeRemainder(null);"

    code ++= "}"
    context.elementFactory.createMethodFromText(code.toString(), serializer)
  }

  private def createDeserializeMethod(): PsiMethod = {
    val readerClass = context.findClass("com.tangosol.io.pof.PofReader")

    val instanceClassName = entityClass.fullName
    val instanceName = StringUtil.decapitalize(entityClass.name)

    val code = new StringBuilder("public ") ++= instanceClassName ++= " deserialize(com.tangosol.io.pof.PofReader pofReader) throws java.io.IOException {"

    entityClass.fields.foreach {
      field =>
        val (readMethod, returnType) = PofSerializerUtils.readMethodCall(readerClass, "pofReader", field)

        code ++= field.typeName ++= " " ++= field.name ++= " = "

        if (!field.psiField.getType.isAssignableFrom(returnType))
        // add cast
          code ++= "(" ++= field.typeName ++= ") "

        code ++= readMethod
    }

    // declare deserialize object instance and initialize
    code ++= instanceClassName ++= " " ++= instanceName ++= " = "
    code ++= "new " ++ entityClass.constructor.getName ++= "("
    entityClass.constructorFields.map(entityClass.fields(_).name).addString(code, ", ")
    code ++= ")"
    code ++= ";"

    // set other fields with setters
    entityClass.restFields.foreach {
      fieldIndex =>
        val field = entityClass.fields(fieldIndex)
        field.setter match {
          case Some(setter) => code ++= instanceName ++= "." ++= setter.getName ++= "(" ++= field.name ++= ");"
          case None => code ++= "// ERROR: no setter for field '" ++= field.name ++ "'"
        }
    }

    // read remainder
    code ++= "pofReader.readRemainder();"

    code ++= "return " ++= instanceName ++= ";"
    code ++= "}"
    context.elementFactory.createMethodFromText(code.toString(), serializer)
  }

  private def addIndex(indexName: String, index: Int): Unit = {
    // create static field
    val indexConstant = context.elementFactory.createField(indexName, PsiType.INT)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.PRIVATE, true)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.STATIC, true)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.FINAL, true)
    indexConstant.setInitializer(context.elementFactory.createExpressionFromText(index.toString, serializer))
    // add constant field to serializer class
    serializer.add(indexConstant)
  }
}
