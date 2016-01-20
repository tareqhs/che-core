/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.dto.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import org.eclipse.che.dto.processor.GeneratorData.DtoProperty;
import org.eclipse.che.dto.processor.GeneratorData.DtoTypeInfo;
import org.eclipse.che.dto.processor.GeneratorData.ImplClass;
import org.eclipse.che.dto.server.JsonArrayImpl;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.eclipse.che.dto.shared.DTO;

public class DtoAnalyzer {

    private static final String STRING_QNAME = String.class.getName();
    private static final String LIST_QNAME = List.class.getName();
    private static final String MAP_QNAME = Map.class.getName();

    private final ProcessingEnvironment env;
    private final Messager messager;
    private final Set<PackageElement> dtoPackages;
    private Map<TypeElement, ImplClass> analyzedDtos = new HashMap<>();
    private int nextTypeInfoId = 0;
    // Reusable helpers
    private DeclaredType stringType;
    private DeclaredType objectType;

    public DtoAnalyzer(ProcessingEnvironment env, Set<PackageElement> dtoPackages) {
        this.env = env;
        this.messager = env.getMessager();
        this.dtoPackages = dtoPackages;
        // Initialize helpers
        TypeElement stringElement = env.getElementUtils().getTypeElement(STRING_QNAME);
        this.stringType = env.getTypeUtils().getDeclaredType(stringElement);
        TypeElement objectElement = env.getElementUtils().getTypeElement(Object.class.getName());
        this.objectType = env.getTypeUtils().getDeclaredType(objectElement);
    }

    public ImplClass analyzeInterface(TypeElement interfaceElm) {
        ImplClass result = analyzedDtos.get(interfaceElm);
        if (result != null) {
            return result;
        }
        // Must be inside the allowed packages
        if (!isGeneratedDtoInterface(interfaceElm)) {
            return null;
        }
        // Initial data
        ImplClass implClass = new ImplClass();
        analyzedDtos.put(interfaceElm, result);
        implClass.id = nextTypeInfoId++;
        implClass.dto = interfaceElm;
        implClass.name = interfaceElm.getSimpleName() + "Impl";
        implClass.dtoType = env.getTypeUtils().getDeclaredType(interfaceElm);
        // Analyze the base DTO interface if it exists
        TypeElement baseDto = getBaseDto(interfaceElm);
        if (baseDto != null) {
            implClass.baseImpl = analyzeInterface(baseDto);
        }
        // Inspect all the methods
        for (Element subElement : interfaceElm.getEnclosedElements()) {
            analyzeMemberElement(implClass, subElement);
        }
        // Done
        return implClass;
    }

    private void analyzeMemberElement(ImplClass implClass, Element memberElement) {
        // Only non-default interface methods are considered
        if (memberElement.getKind() != ElementKind.METHOD) {
            return;
        }
        ExecutableElement method = (ExecutableElement) memberElement;
        if (method.isDefault()) {
            return;
        }
        // Kind of method
        String propName;
        if ((propName = getPropetyName(method, "get")) != null) {
            analyzeGetter(propName, method, implClass);
        } else if ((propName = getPropetyName(method, "is")) != null) {
            analyzeIsGetter(propName, method, implClass);
        } else if ((propName = getPropetyName(method, "set")) != null) {
            analyzeSetter(propName, method, implClass);
        } else if ((propName = getPropetyName(method, "with")) != null) {
            analyzeWithSetter(propName, method, implClass);
        }
    }

    private void analyzeGetter(String propName, ExecutableElement methodElement, ImplClass info) {
        DtoProperty property = analyzeCommonGetter(propName, methodElement, info);
        if (property != null) {
            property.getter = methodElement;
        }
    }

    private void analyzeIsGetter(String propName, ExecutableElement methodElement, ImplClass info) {
        // Validate that the type is boxed or unboxed boolean
        TypeMirror type = methodElement.getReturnType();
        if (type.getKind() != TypeKind.BOOLEAN) {
            PrimitiveType unboxedType;
            try {
                unboxedType = env.getTypeUtils().unboxedType(type);
            } catch (IllegalArgumentException e) {
                unboxedType = null;
            }
            if (unboxedType == null || unboxedType.getKind() != TypeKind.BOOLEAN) {
                messager.printMessage(Kind.ERROR, "unsupported 'is' type: " + type, methodElement);
                return;
            }
        }
        DtoProperty property = analyzeCommonGetter(propName, methodElement, info);
        if (property != null) {
            property.isGetter = methodElement;
        }
    }

    private DtoProperty analyzeCommonGetter(String propName, ExecutableElement methodElement, ImplClass info) {
        // Getters should have no arguments
        if (!methodElement.getParameters().isEmpty()) {
            messager.printMessage(Kind.ERROR, "unsupported getter signature", methodElement);
            return null;
        }
        // Add a property
        TypeMirror propType = methodElement.getReturnType();
        return addProperty(propName, propType, methodElement, info);
    }

    private void analyzeSetter(String propName, ExecutableElement methodElement, ImplClass info) {
        DtoProperty property = analyzeCommonSetter(propName, methodElement, info);
        if (property == null) {
            return;
        }
        // Setters should return void
        if (methodElement.getReturnType().getKind() != TypeKind.VOID) {
            messager.printMessage(Kind.ERROR, "'with' setters should return the DTO type", methodElement);
            return;
        }
        property.setter = methodElement;
    }

    private void analyzeWithSetter(String propName, ExecutableElement methodElement, ImplClass info) {
        DtoProperty property = analyzeCommonSetter(propName, methodElement, info);
        if (property == null) {
            return;
        }
        // 'with' setters should return return DTO type
        if (!env.getTypeUtils().isSameType(info.dtoType, methodElement.getReturnType())) {
            messager.printMessage(Kind.ERROR, "'with' setters should return the DTO type", methodElement);
            return;
        }
        property.withSetter = methodElement;
    }

    private DtoProperty analyzeCommonSetter(String propName, ExecutableElement methodElement, ImplClass info) {
        // Setters should have a single argument
        if (methodElement.getParameters().size() != 1) {
            messager.printMessage(Kind.ERROR, "Setters should have a single parameter", methodElement);
            return null;
        }
        // Add a property
        TypeMirror propType = methodElement.getParameters().get(0).asType();
        return addProperty(propName, propType, methodElement, info);
    }

    private DtoProperty addProperty(String propName, TypeMirror fieldType, ExecutableElement methodElement,
            ImplClass info) {
        DtoProperty property = info.properties.get(propName);
        if (property != null) {
            if (env.getTypeUtils().isSameType(property.typeInfo.type, fieldType)) {
                return property;
            }
            messager.printMessage(Kind.ERROR, "Unexpected type: " + fieldType, methodElement);
            return null;
        }
        // Add a new property
        property = new DtoProperty();
        property.fieldName = propName;
        property.jsonFieldName = property.fieldName;
        property.helperName = "H_" + propName;
        // Validate the type of this property
        DtoTypeInfo typeInfo = analyzeType(fieldType);
        if (typeInfo == null) {
            messager.printMessage(Kind.ERROR, "Invalid type for DTO property: " + fieldType, methodElement);
            return null;
        }
        property.typeInfo = typeInfo;
        if (typeInfo.listValue != null || typeInfo.mapValue != null) {
            property.ensureName = getVerbCapitalized("ensure", propName);
            property.clearName = getVerbCapitalized("clear", propName);
            if (typeInfo.listValue != null) {
                property.addName = getVerbCapitalized("add", propName);
            }
            if (typeInfo.mapValue != null) {
                property.putName = getVerbCapitalized("put", propName);
            }
        }
        // Done
        info.properties.put(propName, property);
        return property;
    }

    private DtoTypeInfo analyzeType(TypeMirror type) {
        final Types typeUtils = env.getTypeUtils();
        DtoTypeInfo typeInfo = new DtoTypeInfo();
        typeInfo.type = type;
        typeInfo.refType = type;
        typeInfo.id = nextTypeInfoId++;
        // Primitive types
        if (type.getKind().isPrimitive()) {
            typeInfo.isPrimitive = true;
            typeInfo.refType = typeUtils.getDeclaredType(typeUtils.boxedClass((PrimitiveType) type));
            setPrimitiveCapName(type, typeInfo);
            return typeInfo;
        }
        // Strings and boxed types
        PrimitiveType unboxedType;
        try {
            unboxedType = typeUtils.unboxedType(type);
        } catch (IllegalArgumentException e) {
            unboxedType = null;
        }
        if (unboxedType != null) {
            typeInfo.isBoxed = true;
            setPrimitiveCapName(unboxedType, typeInfo);
            return typeInfo;
        }
        // String
        if (typeUtils.isSameType(stringType, type)) {
            typeInfo.isBoxed = true;
            typeInfo.primitiveTypeCap = "String";
            return typeInfo;
        }
        // Object -> any
        if (typeUtils.isSameType(objectType, type)) {
            typeInfo.isAny = true;
            return typeInfo;
        }
        // Other types
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        DeclaredType declaredType = (DeclaredType) type;
        Element declaredElm = declaredType.asElement();
        if (declaredElm == null) {
            return null;
        }
        // Enumeration types
        if (declaredElm.getKind() == ElementKind.ENUM) {
            typeInfo.isEnum = true;
            return typeInfo;
        }
        if (declaredElm.getKind() != ElementKind.INTERFACE) {
            return null;
        }
        TypeElement declaredTypeElm = (TypeElement) declaredElm;
        // DTO references
        if (isDtoInterface(declaredTypeElm)) {
            typeInfo.dtoRef = declaredTypeElm;
            typeInfo.dtoImpl = analyzeInterface(declaredTypeElm);
            return typeInfo;
        }
        // Lists
        if (LIST_QNAME.equals(declaredTypeElm.getQualifiedName().toString())) {
            TypeMirror typeParam = (TypeMirror) declaredType.getTypeArguments().get(0);
            typeInfo.listValue = analyzeType(typeParam);
            if (typeInfo.listValue == null) {
                return null;
            }
            typeInfo.defaultImplType = createParameterizedType(ArrayList.class, typeParam);
            typeInfo.getterWrapperType = createParameterizedType(JsonArrayImpl.class, typeParam);
            return typeInfo;
        }
        // Maps
        if (MAP_QNAME.equals(declaredTypeElm.getQualifiedName().toString())) {
            // Only String keys are allowed
            TypeMirror keyTypeParam = (TypeMirror) declaredType.getTypeArguments().get(0);
            if (!typeUtils.isSameType(stringType, keyTypeParam)) {
                return null;
            }
            TypeMirror valueTypeParam = (TypeMirror) declaredType.getTypeArguments().get(1);
            typeInfo.mapValue = analyzeType(valueTypeParam);
            if (typeInfo.mapValue == null) {
                return null;
            }
            typeInfo.defaultImplType = createParameterizedType(HashMap.class, keyTypeParam, valueTypeParam);
            typeInfo.getterWrapperType = createParameterizedType(JsonStringMapImpl.class, valueTypeParam);
            typeInfo.mapEntryType = createParameterizedType(Entry.class, keyTypeParam, valueTypeParam);
            return typeInfo;
        }
        return null;
    }

    private static void setPrimitiveCapName(TypeMirror type, DtoTypeInfo typeInfo) {
        final String primName = type.toString();
        typeInfo.primitiveTypeCap = Character.toUpperCase(primName.charAt(0)) + primName.substring(1);
    }

    ///////// Type helpers

    private TypeElement getBaseDto(TypeElement interfaceElm) {
        // TODO currently extending DTO interfaces is only supported if both the base and the derived types are
        // generated in during the same compilation
        Optional<TypeElement> baseDto = interfaceElm.getInterfaces().stream()
                // Convert to TypeElement
                .map(t -> (TypeElement) ((DeclaredType) t).asElement()) //
                // Find first one annotated with DTO
                .filter(this::isGeneratedDtoInterface).findFirst();
        return baseDto.orElse(null);
    }

    private DeclaredType createParameterizedType(Class<?> typeClass, TypeMirror... typeParams) {
        TypeElement typeElement = env.getElementUtils().getTypeElement(typeClass.getCanonicalName());
        return env.getTypeUtils().getDeclaredType(typeElement, typeParams);
    }

    public boolean isDtoInterface(TypeElement typeElement) {
        // Must be annotated with DTO annotation
        return typeElement.getAnnotation(DTO.class) != null;
    }

    public boolean isGeneratedDtoInterface(TypeElement typeElement) {
        if (isDtoInterface(typeElement)) {
            // Must be inside the allowed packages
            Element ancestorElm = typeElement;
            while ((ancestorElm = ancestorElm.getEnclosingElement()) != null) {
                if (dtoPackages.contains(ancestorElm)) {
                    return true;
                }
            }
        }
        return false;
    }

    //////// Naming helpers

    private static String getPropetyName(ExecutableElement method, String prefix) {
        final String name = method.getSimpleName().toString();
        // Starts with the prefix
        if (name.length() > prefix.length() && name.startsWith(prefix)) {
            final char firstChar = name.charAt(prefix.length());
            // The remainder starts with an upper-case letter
            if (Character.isUpperCase(firstChar)) {
                return Character.toLowerCase(firstChar) + name.substring(prefix.length() + 1);
            }
        }
        return null;
    }

    private static String getVerbCapitalized(String verb, String propName) {
        return verb + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    }

}
