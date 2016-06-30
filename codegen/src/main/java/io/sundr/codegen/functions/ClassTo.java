/*
 * Copyright 2016 The original authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.sundr.codegen.functions;

import io.sundr.FunctionFactory;
import io.sundr.Function;
import io.sundr.codegen.DefinitionRepository;
import io.sundr.codegen.model.ClassRef;
import io.sundr.codegen.model.ClassRefBuilder;
import io.sundr.codegen.model.Kind;
import io.sundr.codegen.model.Method;
import io.sundr.codegen.model.MethodBuilder;
import io.sundr.codegen.model.PrimitiveRefBuilder;
import io.sundr.codegen.model.Property;
import io.sundr.codegen.model.PropertyBuilder;
import io.sundr.codegen.model.ClassDef;
import io.sundr.codegen.model.ClassDefBuilder;
import io.sundr.codegen.model.TypeParamDef;
import io.sundr.codegen.model.TypeParamDefBuilder;
import io.sundr.codegen.model.TypeParamRefBuilder;
import io.sundr.codegen.model.TypeRef;
import io.sundr.codegen.model.VoidRefBuilder;
import io.sundr.codegen.model.WildcardRefBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClassTo {

    private static final String ARGUMENT_PREFIX = "arg";

    public static final Function<Class, Kind> KIND = FunctionFactory.cache(new Function<Class, Kind>() {
        public Kind apply(Class item) {
            if (item.isAnnotation()) {
                return Kind.ANNOTATION;
            } else if (item.isEnum()) {
                return Kind.ENUM;
            } else if (item.isInterface()) {
                return Kind.INTERFACE;
            } else {
                return Kind.CLASS;
            }
        }
    });

    public static final Function<Type, TypeRef> TYPEREF = FunctionFactory.cache(new Function<Type, TypeRef>() {
        public TypeRef apply(Type item) {
            if (item == null) {
                return new VoidRefBuilder().build();
            } else if (item instanceof WildcardType) {
                return new WildcardRefBuilder().build();
            } else if (item instanceof TypeVariable) {
                return new TypeParamRefBuilder().withName(((TypeVariable) item).getName()).build();
            } else if (item instanceof GenericArrayType) {
                Type target = item;
                int dimensions = 0;
                while (target instanceof GenericArrayType) {
                    target = ((GenericArrayType) target).getGenericComponentType();
                    dimensions++;
                }
                TypeRef targetRef = TYPEREF.apply(target);
                return targetRef.withDimensions(dimensions + targetRef.getDimensions());

            } else if (item instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) item;
                Type rawType = parameterizedType.getRawType();
                List<TypeRef> arguments = new ArrayList<TypeRef>();
                for (Type arg : parameterizedType.getActualTypeArguments()) {
                    arguments.add(TYPEREF.apply(arg));
                }
                return new ClassRefBuilder((ClassRef) TYPEREF.apply(rawType))
                        .withArguments(arguments)
                        .build();
            } else if (Object.class.equals(item)) {
                return ClassDef.OBJECT_REF;
            } else if (item instanceof Class) {
                Class c = (Class) item;
                if (c.isPrimitive()) {
                    return new PrimitiveRefBuilder().withName(c.getName()).build();
                } else {
                    return new ClassRefBuilder()
                            .withDefinition(TYPEDEF.apply(c))
                            .build();
                }
            }
            throw new IllegalArgumentException("Can't convert type:"+item+" to a TypeRef");
        }
    });


    private static final Function<Class, ClassDef> INTERNAL_TYPEDEF = new Function<Class, ClassDef>() {
        public ClassDef apply(Class item) {

            if (Object.class.equals(item)) {
                return ClassDef.OBJECT;
            }
            Kind kind = KIND.apply(item);
            Set<ClassRef> extendsList = new LinkedHashSet<ClassRef>();
            Set<ClassRef> implementsList = new LinkedHashSet<ClassRef>();
            Set<Property> properties = new LinkedHashSet<Property>();
            Set<Method> methods = new LinkedHashSet<Method>();
            List<TypeParamDef> parameters = new ArrayList<TypeParamDef>();

            if (item.getSuperclass() != null && kind == Kind.INTERFACE) {
                extendsList.add((ClassRef) TYPEREF.apply(item.getSuperclass()));
            }
            methods.addAll(getMethods(item));
            properties.addAll(getProperties(item));

            for (Class interfaceClass : item.getInterfaces()) {
                TypeRef ref = TYPEREF.apply(interfaceClass);
                if (ref instanceof ClassRef) {
                    implementsList.add((ClassRef) ref);
                }
            }

            for (TypeVariable typeVariable : item.getTypeParameters()) {
                List<ClassRef> bounds = new ArrayList<ClassRef>();
                for (Type boundType : typeVariable.getBounds()) {
                    TypeRef typeRef = TYPEREF.apply(boundType);
                    if (typeRef instanceof ClassRef) {
                        bounds.add((ClassRef)typeRef);
                    }
                }
                parameters.add(new TypeParamDefBuilder()
                        .withName(typeVariable.getName())
                        .withBounds(bounds)
                        .build());
            }

            return DefinitionRepository.getRepository().register(new ClassDefBuilder()
                    .withKind(kind)
                    .withName(item.getSimpleName())
                    .withPackageName(item.getPackage() != null ? item.getPackage().getName() : null)
                    .withModifiers(item.getModifiers())
                    .withParameters(parameters)
                    .withMethods(methods)
                    .withProperties(properties)
                    .withExtendsList(extendsList)
                    .withImplementsList(implementsList)
                    .build());
        }
    };

    private static final Function<Class, ClassDef> INTERNAL_SHALLOW_TYPEDEF = new Function<Class, ClassDef>() {

        public ClassDef apply(Class item) {
            if (Object.class.equals(item)) {
                return ClassDef.OBJECT;
            }
            Kind kind = KIND.apply(item);

            return new ClassDefBuilder()
                    .withKind(kind)
                    .withName(item.getSimpleName())
                    .withPackageName(item.getPackage() != null ? item.getPackage().getName() : null)
                    .withModifiers(item.getModifiers())
                    .withParameters()
                    .build();
        }
    };

    public static final Function<Class, ClassDef> TYPEDEF = FunctionFactory.cache(INTERNAL_TYPEDEF).withFallback(INTERNAL_SHALLOW_TYPEDEF).withMaximumRecursionLevel(5).withMaximumNestingDepth(5);

    private static Function<Type, TypeParamDef> TYPEPARAMDEF = FunctionFactory.cache(new Function<Type, TypeParamDef>() {

        public TypeParamDef apply(Type item) {
            if (item instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) item;
                String name = typeVariable.getName();
                List<ClassRef> bounds = new ArrayList<ClassRef>();

                for (Type b : typeVariable.getBounds()) {
                    if (b instanceof Class) {
                        Class c = (Class) b;
                        bounds.add((ClassRef) TYPEREF.apply(c));
                    }
                }
                return new TypeParamDefBuilder().withName(name).withBounds(bounds).build();
            }
            return null;
        }
    });

    private static Set<Property> getProperties(Class item) {
        Set<Property> properties = new HashSet<Property>();
        for (Field field : item.getDeclaredFields()) {
            List<ClassRef> annotationRefs = new ArrayList<ClassRef>();
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                //An annotation can't be a primitive or a void type, so its safe to cast.
                annotationRefs.add((ClassRef) TYPEREF.apply(annotation.annotationType()));
            }
            field.getDeclaringClass();
            properties.add(new PropertyBuilder()
                    .withName(field.getName())
                    .withModifiers(field.getModifiers())
                    .withAnnotations(new LinkedHashSet<ClassRef>(annotationRefs))
                    .withTypeRef(TYPEREF.apply(field.getDeclaringClass()))
                    .build());
        }
        return properties;
    }

    private static Set<Method> getMethods(Class item) {
        Set<Method> methods = new HashSet<Method>();
        for (java.lang.reflect.Method method : item.getDeclaredMethods()) {
            List<ClassRef> annotationRefs = new ArrayList<ClassRef>();
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                //An annotation can't be a primitive or a void type, so its safe to cast.
                annotationRefs.add((ClassRef) TYPEREF.apply(annotation.annotationType()));
            }

            List<Property> arguments = new ArrayList<Property>();
            for (int i = 1; i <= method.getGenericParameterTypes().length; i++) {
                Type argumentType = method.getGenericParameterTypes()[i - 1];
                arguments.add(new PropertyBuilder()
                        .withName(ARGUMENT_PREFIX + i)
                        .withTypeRef(TYPEREF.apply(argumentType))
                        .build());
            }

            Set<TypeParamDef> parameters = new LinkedHashSet<TypeParamDef>();
            for (Type type : method.getGenericParameterTypes()) {

                TypeParamDef typeParamDef = TYPEPARAMDEF.apply(type);
                if (typeParamDef != null) {
                    parameters.add(typeParamDef);
                }
            }

            methods.add(new MethodBuilder()
                    .withName(method.getName())
                    .withModifiers(method.getModifiers())
                    .withReturnType(TYPEREF.apply(method.getReturnType()))
                    .withArguments(arguments)
                    .withParameters(parameters)
                    .withAnnotations(new LinkedHashSet<ClassRef>(annotationRefs))
                    .build());
        }
        return methods;
    }
}
