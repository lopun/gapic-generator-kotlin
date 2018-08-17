/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.kotlin.generator

import com.google.api.kotlin.config.ProtobufTypeMapper
import com.google.api.kotlin.types.GrpcTypes
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

/**
 * Container for the [file], [message], and [field] and repeated [index] (if applicable)
 * of a proto and it's corresponding [kotlinType].
 */
internal data class ProtoFieldInfo(
    val file: DescriptorProtos.FileDescriptorProto,
    val message: DescriptorProtos.DescriptorProto,
    val field: DescriptorProtos.FieldDescriptorProto,
    val index: Int = -1,
    val kotlinType: TypeName
)

// -----------------------------------------------------------------
// Misc. helpers for dealing with proto type descriptors
// -----------------------------------------------------------------

/** Checks if this methods is a LRO. */
internal fun DescriptorProtos.MethodDescriptorProto.isLongRunningOperation() =
    this.outputType == ".google.longrunning.Operation"

/** Checks if this proto field is a map type. */
internal fun DescriptorProtos.FieldDescriptorProto.isMap(typeMap: ProtobufTypeMapper): Boolean {
    if (this.hasTypeName()) {
        if (typeMap.hasProtoEnumDescriptor(this.typeName)) {
            return false
        }
        return typeMap.getProtoTypeDescriptor(this.typeName).options.mapEntry
    }
    return false
}

/** Checks if this proto field is a repeated type. */
internal fun DescriptorProtos.FieldDescriptorProto.isRepeated() =
    this.label == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED

/** Extracts the key and value Kotlin type names of a protobuf map field using the [typeMap]. */
internal fun DescriptorProtos.FieldDescriptorProto.describeMap(typeMap: ProtobufTypeMapper): Pair<DescriptorProtos.FieldDescriptorProto, DescriptorProtos.FieldDescriptorProto> {
    val mapType = typeMap.getProtoTypeDescriptor(this.typeName)

    // extract key / value type information
    val keyType = mapType.fieldList.find { it.name == "key" }
        ?: throw IllegalStateException("${this.typeName} is not a map type (key type not found)")
    val valueType = mapType.fieldList.find { it.name == "value" }
        ?: throw IllegalStateException("${this.typeName} is not a map type (value type not found)")

    return Pair(keyType, valueType)
}

/** Get the comments of a field in message in this proto file, or null if not available. */
internal fun DescriptorProtos.FileDescriptorProto.getParameterComments(fieldInfo: ProtoFieldInfo): String? {
    // find the magic numbers
    val messageNumber = this.messageTypeList.indexOf(fieldInfo.message)
    val fieldNumber = fieldInfo.message.fieldList.indexOf(fieldInfo.field)

    // location is [4, messageNumber, 2, fieldNumber]
    return this.sourceCodeInfo.locationList.filter {
        it.pathCount == 4 &&
            it.pathList[0] == 4 && // message types
            it.pathList[1] == messageNumber &&
            it.pathList[2] == 2 && // fields
            it.pathList[3] == fieldNumber
    }.map { it.leadingComments }.firstOrNull()
}

/** Get the comments of a service method in this protofile, or null if not available */
internal fun DescriptorProtos.FileDescriptorProto.getMethodComments(
    service: DescriptorProtos.ServiceDescriptorProto,
    method: DescriptorProtos.MethodDescriptorProto
): String? {
    // find the magic numbers
    val serviceNumber = this.serviceList.indexOf(service)
    val methodNumber = service.methodList.indexOf(method)

    // location is [6, serviceNumber, 2, methodNumber]
    return this.sourceCodeInfo.locationList.filter {
        it.pathCount == 4 &&
            it.pathList[0] == 6 && // 6 is for service
            it.pathList[1] == serviceNumber &&
            it.pathList[2] == 2 && // 2 is for method (rpc)
            it.pathList[3] == methodNumber &&
            it.hasLeadingComments()
    }.map { it.leadingComments }.firstOrNull()
}

/** Get the kotlin class name of this field using the [typeMap] */
internal fun DescriptorProtos.FieldDescriptorProto.asClassName(typeMap: ProtobufTypeMapper): ClassName {
    return if (this.hasTypeName()) {
        typeMap.getKotlinType(this.typeName)
    } else {
        when (this.type) {
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> String::class.asTypeName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> BOOLEAN
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> DOUBLE
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> FLOAT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 -> INT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 -> INT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> INT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32 -> INT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32 -> INT
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64 -> LONG
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> LONG
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 -> LONG
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64 -> LONG
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64 -> LONG
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> GrpcTypes.ByteString
            else -> throw IllegalStateException("unexpected or non-primitive type: ${this.type}")
        }
    }
}

internal fun DescriptorProtos.FieldDescriptorProto.isMessageType() =
    this.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
