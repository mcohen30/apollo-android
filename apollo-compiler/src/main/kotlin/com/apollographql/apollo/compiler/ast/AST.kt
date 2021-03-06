package com.apollographql.apollo.compiler.ast

internal class CustomTypes(map: Map<String, String>) : Map<String, String> by map

internal data class TypeRef(val name: String, val packageName: String = "")

internal sealed class FieldType {

  sealed class Scalar : FieldType() {
    object String : Scalar()
    object Int : Scalar()
    object Boolean : Scalar()
    object Float : Scalar()
    data class Enum(val typeRef: TypeRef) : Scalar()
    data class Custom(
        val schemaType: kotlin.String,
        val mappedType: kotlin.String,
        val customEnumConst: kotlin.String,
        val customEnumType: TypeRef
    ) : Scalar()
  }

  data class Fragments(val name: String, val fields: List<Field>) : FieldType() {
    data class Field(
        val name: String,
        val type: TypeRef,
        val isOptional: Boolean
    )
  }

  data class Object(val typeRef: TypeRef) : FieldType()

  data class InlineFragment(val typeRef: TypeRef, val fragmentRefs: List<TypeRef>) : FieldType()

  data class Array(val rawType: FieldType) : FieldType()
}

internal data class OperationType(
    val name: String,
    val type: Type,
    val operationId: String,
    val queryDocument: String,
    val variables: InputType,
    val data: TypeRef,
    val nestedObjects: Map<TypeRef, ObjectType>,
    val filePath: String
) {
  enum class Type {
    QUERY, MUTATION, SUBSCRIPTION
  }
}

internal sealed class ObjectType {

  abstract val className: String

  abstract val fields: List<Field>

  data class Object(
      override val className: String,
      val schemaName: String,
      override val fields: List<Field>,
      val fragmentsType: ObjectType?
  ) : ObjectType()

  internal data class InlineFragmentSuper(
      override val className: String
  ) : ObjectType() {
    override val fields: List<Field> = emptyList()
  }

  data class InlineFragment(
      override val className: String,
      override val fields: List<Field>,
      val superInterface: TypeRef,
      val possibleTypes: List<String>
  ) : ObjectType()

  data class Field(
      val name: String,
      val responseName: String,
      val schemaName: String,
      val type: FieldType,
      val description: String,
      val isOptional: Boolean,
      val isDeprecated: Boolean,
      val deprecationReason: String,
      val arguments: Map<String, Any>,
      val conditions: List<Condition>
  ) {
    sealed class Condition {
      data class Directive(val variableName: String, val inverted: Boolean) : Condition()
      data class Type(val type: String) : Condition()
    }
  }
}

internal data class EnumType(
    val name: String,
    val description: String,
    val values: List<Value>
) {
  data class Value(
      val constName: String,
      val value: String,
      val description: String,
      val isDeprecated: Boolean,
      val deprecationReason: String
  )
}

internal data class InputType(
    val name: String,
    val description: String,
    val fields: List<Field>
) {

  data class Field(
      val name: String,
      val schemaName: String,
      val type: FieldType,
      val description: String,
      val isOptional: Boolean,
      val defaultValue: Any?
  )
}

internal data class FragmentType(
    val name: String,
    val definition: String,
    val possibleTypes: List<String>,
    val fields: List<ObjectType.Field>,
    val nestedObjects: Map<TypeRef, ObjectType>
)

internal data class Schema(
    val enums: List<EnumType>,
    val customTypes: CustomTypes,
    val inputTypes: List<InputType>,
    val fragments: List<FragmentType>,
    val operations: List<OperationType>
) {
  fun accept(visitor: SchemaVisitor) {
    customTypes.takeIf { it.isNotEmpty() }?.also(visitor::visit)
    enums.forEach(visitor::visit)
    inputTypes.forEach(visitor::visit)
    fragments.forEach(visitor::visit)
    operations.forEach(visitor::visit)
  }
}

internal interface SchemaVisitor {
  fun visit(customTypes: CustomTypes)

  fun visit(enumType: EnumType)

  fun visit(inputType: InputType)

  fun visit(fragmentType: FragmentType)

  fun visit(operationType: OperationType)
}