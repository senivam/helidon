/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.builder.processor;

import java.util.Optional;
import java.util.Set;

import io.helidon.common.processor.classmodel.Field;
import io.helidon.common.processor.classmodel.InnerClass;
import io.helidon.common.processor.classmodel.Javadoc;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.builder.processor.Types.OPTION_CONFIDENTIAL_TYPE;
import static io.helidon.builder.processor.Types.OPTION_REDUNDANT_TYPE;
import static io.helidon.builder.processor.Types.OPTION_SAME_GENERIC_TYPE;
import static io.helidon.common.processor.GeneratorTools.capitalize;

// builder property
record PrototypeProperty(MethodSignature signature,
                         TypeHandler typeHandler,
                         AnnotationDataOption configuredOption,
                         FactoryMethods factoryMethods,
                         boolean equality, // part of equals and hash code
                         boolean toStringValue, // part of toString
                         boolean confidential // if part of toString, do not print the actual value
) {
    // cannot be identifiers - such as field name or method name
    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break",
            "byte", "case", "catch", "char",
            "class", "const", "continue", "default",
            "do", "double", "else", "enum",
            "extends", "final", "finally", "float",
            "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface",
            "long", "native", "new", "package",
            "private", "protected", "public", "return",
            "short", "static", "super", "switch",
            "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile",
            "while", "true", "false", "null"
    );

    static PrototypeProperty create(ProcessingContext processingContext,
                                    TypeInfo blueprint,
                                    TypedElementInfo element,
                                    boolean beanStyleAccessors) {

        boolean isBoolean = element.typeName().boxed().equals(Types.BOXED_BOOLEAN_TYPE);
        String getterName = element.elementName(); // this is always correct
        String name = propertyName(getterName,
                                   isBoolean,
                                   beanStyleAccessors); // name of the property, such as "withDefault", "optional", "list", "map"
        String setterName = setterName(name, beanStyleAccessors);
        if (RESERVED_WORDS.contains(name)) {
            name = "the" + capitalize(name);
        }

        TypeName returnType = element.typeName(); // real return type (String, Optional<String>, List<String>, Map<String, Type>

        boolean sameGeneric = element.hasAnnotation(OPTION_SAME_GENERIC_TYPE);
        // to help with defaults, setters, config mapping etc.
        TypeHandler typeHandler = TypeHandler.create(name, getterName, setterName, returnType, sameGeneric);

        // all information from @ConfiguredOption annotation
        AnnotationDataOption configuredOption = AnnotationDataOption.create(typeHandler, element);
        FactoryMethods factoryMethods = FactoryMethods.create(processingContext,
                                                              blueprint,
                                                              typeHandler);

        boolean confidential = element.hasAnnotation(OPTION_CONFIDENTIAL_TYPE);

        Optional<Annotation> redundantAnnotation = element.findAnnotation(OPTION_REDUNDANT_TYPE);
        boolean toStringValue = !redundantAnnotation.flatMap(it -> it.getValue("stringValue"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        boolean equality = !redundantAnnotation.flatMap(it -> it.getValue("equality"))
                .map(Boolean::parseBoolean)
                .orElse(false);

        return new PrototypeProperty(
                MethodSignature.create(element),
                typeHandler,
                configuredOption,
                factoryMethods,
                equality,
                toStringValue,
                confidential
        );
    }

    Field.Builder fieldDeclaration(boolean isBuilder) {
        return typeHandler.fieldDeclaration(configuredOption(), isBuilder, !isBuilder);
    }

    void setters(InnerClass.Builder classBuilder, TypeName builderType, Javadoc blueprintJavadoc) {
        typeHandler().setters(classBuilder,
                              configuredOption(),
                              factoryMethods(),
                              builderType,
                              blueprintJavadoc);
    }

    String name() {
        return typeHandler.name();
    }

    String getterName() {
        return typeHandler.getterName();
    }

    String setterName() {
        return typeHandler.setterName();
    }

    TypeName typeName() {
        return typeHandler.declaredType();
    }

    TypeName builderGetterType() {
        return typeHandler.builderGetterType(configuredOption.required(),
                                             configuredOption.hasDefault());
    }

    String builderGetter() {
        return typeHandler.generateBuilderGetter(configuredOption.required(),
                                                 configuredOption.hasDefault());
    }

    boolean builderGetterOptional() {
        return typeHandler.builderGetterOptional(configuredOption.required(),
                                                 configuredOption.hasDefault());
    }

    private static String setterName(String name, boolean beanStyleAccessors) {
        if (beanStyleAccessors || RESERVED_WORDS.contains(name)) {
            return "set" + capitalize(name);
        }

        return name;
    }

    private static String propertyName(String getterName, boolean isBoolean, boolean beanStyleAccessors) {
        if (beanStyleAccessors) {
            if (isBoolean) {
                if (getterName.startsWith("is")) {
                    return deCapitalize(getterName.substring(2));
                }
            }
            if (getterName.startsWith("get")) {
                return deCapitalize(getterName.substring(3));
            }
        }
        return getterName;
    }

    private static String deCapitalize(String string) {
        if (string.isBlank()) {
            return string;
        }
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }
}