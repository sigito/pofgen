package org.idea.plugin.pofgen.generation

import com.intellij.psi._

/**
 * @author sigito
 */
class SerializerGenerator(entityClass: PsiClass, fields: IndexedSeq[PsiField], context: GenerationContext) {
  def generate(): PsiClass = {
    val (constructor, parameterFields, restFields) = selectConstructor(getConstructors)

    val entityFields = fields.zipWithIndex map {
      case (field, index) => new EntityField(field, index, restFields.contains(index))
    }
    val serializer = new PofSerializer(context, new EntityClass(entityClass, constructor, parameterFields, restFields, entityFields))
    // create file
    serializer.createClass()
  }

  private def getConstructors: Seq[PsiMethod] = {
    // available non-private constructors, default(if exists) goes last
    val constructors = entityClass.getConstructors.filterNot(
      // exclude private constructors
      _.getModifierList.hasModifierProperty(PsiModifier.PRIVATE)
    ).sortBy(_.getParameterList.getParametersCount)(Ordering.Int.reverse)
    constructors
  }

  private def selectConstructor(constructors: Seq[PsiMethod]): (PsiMethod, IndexedSeq[Int], IndexedSeq[Int]) = {
    // set with constructor
    var parameterFields = IndexedSeq.empty[Int]
    // accessed in any other way
    var otherFields: IndexedSeq[Int] = fields.indices

    val matchedConstructor = constructors.find {
      constructor =>
        parameterFields = IndexedSeq.empty[Int]
        otherFields = fields.indices

        val parameters = constructor.getParameterList.getParameters
        // check if every parameter has matching entity field
        parameters forall {
          parameter =>
            fields.zipWithIndex.find {
              case (field, index) =>
                parameter.getName == field.getName && parameter.getType.isAssignableFrom(field.getType)
            } match {
              case Some((field, index)) =>
                // remember field
                parameterFields :+= index
                otherFields = otherFields.filterNot(_ == index)
                true
              case None => false
            }
        }
    }

    matchedConstructor match {
      case Some(constructor) => (constructor, parameterFields, otherFields)
      case None =>
        // create mock default constructor and return
        val defaultConstructor = context.elementFactory.createConstructor(entityClass.getName, entityClass)
        (defaultConstructor, IndexedSeq.empty, fields.indices)
    }
  }
}

