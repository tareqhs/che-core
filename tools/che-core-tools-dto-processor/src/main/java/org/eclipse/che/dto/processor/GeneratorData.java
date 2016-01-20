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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class GeneratorData {

    public String packageName;
    public String containerClass;
    public List<ImplClass> classes = new ArrayList<>();

    public static class ImplClass {
        public int id;
        public TypeElement dto;
        public DeclaredType dtoType;
        public String name;
        public Map<String, DtoProperty> properties = new TreeMap<>();
        public ImplClass baseImpl;
    }

    public static class DtoProperty {
        public String fieldName;
        public String jsonFieldName;
        public String helperName;
        public String ensureName;
        public String clearName;
        public String putName;
        public String addName;
        public ExecutableElement getter;
        public ExecutableElement isGetter;
        public ExecutableElement setter;
        public ExecutableElement withSetter;
        public DtoTypeInfo typeInfo;
    }

    public static class DtoTypeInfo {
        public int id;
        public TypeMirror type;
        public TypeMirror refType;
        public boolean isPrimitive;
        public boolean isBoxed;
        public boolean isEnum;
        public boolean isAny;
        public String primitiveTypeCap;
        public TypeElement dtoRef;
        public ImplClass dtoImpl;
        public TypeMirror defaultImplType;
        public TypeMirror getterWrapperType;
        public DtoTypeInfo listValue;
        public DtoTypeInfo mapValue;
        public DeclaredType mapEntryType;
    }

}
