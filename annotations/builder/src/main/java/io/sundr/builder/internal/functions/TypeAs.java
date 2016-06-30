/*
 * Copyright 2015 The original authors.
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

package io.sundr.builder.internal.functions;

import io.sundr.FunctionFactory;
import io.sundr.Function;
import io.sundr.builder.Constants;
import io.sundr.builder.internal.BuilderContext;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.codegen.model.*;
import io.sundr.codegen.utils.TypeUtils;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import static io.sundr.builder.Constants.BOXED_PRIMITIVE_TYPES;
import static io.sundr.builder.Constants.PRIMITIVE_TYPES;
import static io.sundr.builder.Constants.Q;
import static io.sundr.builder.internal.utils.BuilderUtils.findBuildableSuperClass;
import static io.sundr.builder.internal.utils.BuilderUtils.findBuildableSuperClassRef;
import static io.sundr.builder.internal.utils.BuilderUtils.getNextGeneric;
import static io.sundr.codegen.utils.TypeUtils.classRefOf;


public class TypeAs {

    public static final Function<ClassDef, ClassDef> FLUENT_INTERFACE = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            BuilderContext ctx = BuilderContextManager.getContext();
            ClassDef fluent = SHALLOW_FLUENT.apply(item);

            List<TypeParamDef> parameters = new ArrayList<TypeParamDef>(item.getParameters());
            List<TypeRef> superClassParameters = new ArrayList<TypeRef>();

            TypeParamDef nextParameter = getNextGeneric(item, parameters);

            ClassRef builableSuperClassRef = findBuildableSuperClassRef(item);

            ClassDef buildableSuperClass = findBuildableSuperClass(item);
            if (builableSuperClassRef != null) {
                superClassParameters.addAll(builableSuperClassRef.getArguments());
            }

            TypeParamDef parameterFluent = new TypeParamDefBuilder(nextParameter).addToBounds(fluent.toInternalReference()).build();
            parameters.add(parameterFluent);
            superClassParameters.add(parameterFluent.toReference());

            ClassDef superClass = buildableSuperClass != null
                    ? SHALLOW_FLUENT.apply(buildableSuperClass)
                    : ctx.getFluentInterface();

            return new ClassDefBuilder(item)
                    .withKind(Kind.INTERFACE)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName(item.getName() + "Fluent")
                    .withPackageName(item.getPackageName())
                    .withParameters(parameters)
                    .withExtendsList(superClass.toReference(superClassParameters.toArray(new TypeRef[superClassParameters.size()])))
                    .withImplementsList()
                    .withInnerTypes()
                    .build();
        }
    };

    public static final Function<ClassDef, ClassDef> FLUENT_IMPL = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            BuilderContext ctx = BuilderContextManager.getContext();
            ClassDef fluent = SHALLOW_FLUENT.apply(item);

            List<TypeParamDef> parameters = new ArrayList<TypeParamDef>(item.getParameters());
            List<TypeRef> superClassParameters = new ArrayList<TypeRef>();
            TypeParamDef nextParameter = getNextGeneric(item, parameters);

            ClassRef builableSuperClassRef = findBuildableSuperClassRef(item);
            if (builableSuperClassRef != null) {
                superClassParameters.addAll(builableSuperClassRef.getArguments());
            }

            TypeParamDef parameterFluent = new TypeParamDefBuilder(nextParameter).addToBounds(fluent.toInternalReference()).build();
            parameters.add(parameterFluent);
            superClassParameters.add(parameterFluent.toReference());

            ClassDef buildableSuperClass = findBuildableSuperClass(item);

            ClassDef superClass = buildableSuperClass != null
                    ? FLUENT_IMPL.apply(buildableSuperClass)
                    : ctx.getBaseFluentClass();

            return new ClassDefBuilder(item)
                    .withKind(Kind.CLASS)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName(item.getName() + "FluentImpl")
                    .withPackageName(item.getPackageName())
                    .withParameters(parameters)
                    .withExtendsList(superClass.toReference(superClassParameters.toArray(new TypeRef[superClassParameters.size()])))
                    .withImplementsList(SHALLOW_FLUENT.apply(item).toInternalReference())
                    .withInnerTypes()
                    .build();
        }

    };


    public static final Function<ClassDef, ClassDef> SHALLOW_FLUENT = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            List<TypeParamDef> parameters = new ArrayList<TypeParamDef>(item.getParameters());
            parameters.add(getNextGeneric(item));

            return new ClassDefBuilder(item)
                    .withKind(Kind.INTERFACE)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName(item.getName() + "Fluent")
                    .withParameters(parameters)
                    .withInnerTypes()
                    .build();
        }
    };


    public static final Function<ClassDef, ClassRef> FLUENT_REF = new Function<ClassDef, ClassRef>() {
        public ClassRef apply(ClassDef item) {
            List<TypeRef> parameters = new ArrayList<TypeRef>();
            for (TypeParamDef param : item.getParameters()) {
                parameters.add(param.toReference());
            }
            parameters.add(Q);
           return SHALLOW_FLUENT.apply(item).toReference(parameters.toArray(new TypeRef[parameters.size()]));
        }
    };

    public static final Function<ClassDef, ClassDef> BUILDER = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            ClassDef builder = SHALLOW_BUILDER.apply(item);
            ClassDef fluent = FLUENT_IMPL.apply(item);

            List<TypeRef> parameters = new ArrayList<TypeRef>();
            for (TypeParamDef param : item.getParameters()) {
                parameters.add(param.toReference());
            }
            parameters.add(builder.toInternalReference());
            return new ClassDefBuilder(item)
                    .withKind(Kind.CLASS)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName(item.getName() + "Builder")
                    .withParameters(item.getParameters())
                    .withInnerTypes()
                    .withExtendsList(fluent.toReference(parameters.toArray(new TypeRef[parameters.size()])))
                    .withImplementsList(BuilderContextManager.getContext().getVisitableBuilderInterface().toReference(item.toInternalReference(), builder.toInternalReference()))
                    .build();

        }
    };

    public static final Function<ClassDef, ClassDef> EDITABLE = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            List<TypeParamDef> parameters = new ArrayList<TypeParamDef>();
            for (TypeParamDef generic : item.getParameters()) {
                parameters.add(generic);
            }
            return new ClassDefBuilder(item)
                    .withKind(Kind.CLASS)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName("Editable" + item.getName())
                    .withParameters(parameters)
                    .withExtendsList(item.toInternalReference())
                    .withImplementsList(BuilderContextManager.getContext().getEditableInterface().toReference(SHALLOW_BUILDER.apply(item).toInternalReference()))
                    .withInnerTypes()
                    .withProperties()
                    .withMethods()
                    .withConstructors()
                    .build();

        }
    };

    public static final Function<ClassDef, ClassDef> SHALLOW_BUILDER = new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            return new ClassDefBuilder(item)
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withName(item.getName() + "Builder")
                    .withInnerTypes()
                    .build();
        }
    };

    public static final Function<TypeRef, ClassRef> VISITABLE_BUILDER = new Function<TypeRef, ClassRef>() {
        public ClassRef apply(TypeRef item) {
            TypeRef baseType = TypeAs.combine(UNWRAP_COLLECTION_OF, UNWRAP_ARRAY_OF).apply(item);
            if (baseType instanceof ClassRef) {
                baseType = new ClassRefBuilder((ClassRef)baseType).withArguments().build();
            }
            WildcardRef wildcardRef = new WildcardRefBuilder().addToBounds(baseType).build();
            return BuilderContextManager.getContext().getVisitableBuilderInterface().toReference(wildcardRef, Q);
        }
    };


    public static final Function<TypeRef, TypeRef> LIST_OF = FunctionFactory.cache(new Function<TypeRef, TypeRef>() {
        public TypeRef apply(TypeRef item) {
            return Constants.LIST.toReference(item);
            //TODO: Need a home for: .withDefaultImplementation(Constants.ARRAY_LIST)
        }

    });

    public static final Function<TypeRef, TypeRef> ARRAY_AS_LIST = FunctionFactory.cache(new Function<TypeRef, TypeRef>() {
        public TypeRef apply(TypeRef item) {
            return LIST_OF.apply(UNWRAP_ARRAY_OF.apply(item));

        }
    });

    public static final Function<TypeRef, TypeRef> ARRAY_LIST_OF = new Function<TypeRef, TypeRef>() {
        public TypeRef apply(TypeRef item) {
            return classRefOf(Constants.ARRAY_LIST, item);
        }

    };

    public static final Function<TypeRef, TypeRef> UNWRAP_COLLECTION_OF = new Function<TypeRef, TypeRef>() {
        public TypeRef apply(TypeRef type) {
            if (type instanceof ClassRef) {
                ClassRef classRef = (ClassRef) type;
                if (CollectionTypes.IS_COLLECTION.apply(classRef)) {
                    return classRef.getArguments().get(0);
                }
            }
            return type;
        }
    };

    public static final Function<TypeRef, TypeRef> UNWRAP_ARRAY_OF = new Function<TypeRef, TypeRef>() {

        public TypeRef apply(TypeRef item) {
            if (item instanceof PrimitiveRef) {
                return new PrimitiveRefBuilder((PrimitiveRef) item).withDimensions(0).build();
            } else if (item instanceof ClassRef) {
                return new ClassRefBuilder((ClassRef) item).withDimensions(0).build();
            } else if (item instanceof TypeParamRef) {
                return new TypeParamRefBuilder((TypeParamRef) item).withDimensions(0).build();
            } else {
                return item;
            }
        }
    };



    public static final Function<TypeRef, TypeRef> BOXED_OF = FunctionFactory.cache(new Function<TypeRef, TypeRef>() {
        public TypeRef apply(TypeRef type) {
            int index=0;
            for (TypeRef primitive : PRIMITIVE_TYPES) {
                if (primitive.equals(type)) {
                    return BOXED_PRIMITIVE_TYPES[index];
                }
                index++;
            }
            return type;

        }
    });


    public static Function<TypeRef, TypeRef> ARRAY_OF = new Function<TypeRef, TypeRef>(){

        public TypeRef apply(TypeRef type) {
            if (type instanceof ClassRef) {
                return new ClassRefBuilder((ClassRef) type)
                        .withDimensions(1)
                        .build();

            } else if (type instanceof PrimitiveRef) {
                return new PrimitiveRefBuilder((PrimitiveRef) type)
                        .withDimensions(1)
                        .build();

            } else if (type instanceof TypeParamRef) {
                return new TypeParamRefBuilder((TypeParamRef) type)
                        .withDimensions(1)
                        .build();
            } else {
                throw new IllegalStateException("A property type should be either class, primitive or param referemce.");
            }
        }
    };


    public static <T> Function<T,T> combine(final Function<T, T>... functions) {
        return new Function<T, T>() {
            public T apply(T item) {
                T result = item;
                for (Function<T, T> f : functions) {
                    result = f.apply(result);
                }
                return result;
            }
        };
    }
}
