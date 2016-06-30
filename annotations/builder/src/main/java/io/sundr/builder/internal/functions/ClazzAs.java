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

import io.sundr.Function;
import io.sundr.FunctionFactory;
import io.sundr.builder.Constants;
import io.sundr.builder.TypedVisitor;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.builder.internal.utils.BuilderUtils;
import io.sundr.codegen.CodegenContext;
import io.sundr.codegen.functions.ClassTo;
import io.sundr.codegen.model.*;
import io.sundr.codegen.utils.StringUtils;
import io.sundr.codegen.utils.TypeUtils;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.sundr.builder.Constants.*;
import static io.sundr.builder.internal.utils.BuilderUtils.*;
import static io.sundr.codegen.model.Attributeable.ALSO_IMPORT;

public class ClazzAs {

    public static final Function<ClassDef, ClassDef> FLUENT_INTERFACE = FunctionFactory.wrap(new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            Set<Method> methods = new LinkedHashSet<Method>();
            Set<TypeDef> nestedClazzes = new LinkedHashSet<TypeDef>();
            ClassDef fluentType = TypeAs.FLUENT_INTERFACE.apply(item);
            ClassDef fluentImplType = TypeAs.FLUENT_IMPL.apply(item);

            //The generic letter is always the last
            final TypeParamDef genericType = fluentType.getParameters().get(fluentType.getParameters().size() - 1);

            for (Property property : item.getProperties()) {
                final TypeRef unwrapped = TypeAs.combine(TypeAs.UNWRAP_ARRAY_OF, TypeAs.UNWRAP_COLLECTION_OF).apply(property.getTypeRef());
                if (property.isStatic()) {
                    continue;
                }
                if (!hasBuildableConstructorWithArgument(item, property) && !hasOrInheritsSetter(item, property)) {
                    continue;
                }

                Property toAdd = new PropertyBuilder(property)
                        .withModifiers(0)
                        .addToAttributes(OUTER_INTERFACE, fluentType)
                        .addToAttributes(OUTER_CLASS, fluentImplType)
                        .addToAttributes(GENERIC_TYPE_REF, genericType.toReference())
                        .build();

                boolean isBuildable = isBuildable(unwrapped);
                boolean isArray = isArray(toAdd.getTypeRef());
                boolean isSet = isSet(toAdd.getTypeRef());
                boolean isList = isList(toAdd.getTypeRef());
                boolean isMap = isMap(toAdd.getTypeRef());
                boolean isCollection = isSet || isList;
                boolean isAbstract = isAbstract(unwrapped);

                Set<Property> descendants = Decendants.PROPERTY_BUILDABLE_DESCENDANTS.apply(toAdd);
                toAdd = new PropertyBuilder(toAdd).addToAttributes(DESCENDANTS, descendants).build();

                if (isArray) {
                    Property asList = arrayAsList(toAdd);
                    methods.add(ToMethod.WITH_ARRAY.apply(toAdd));
                    methods.add(ToMethod.GETTER_ARRAY.apply(toAdd));
                    methods.add(ToMethod.ADD_TO_COLLECTION.apply(asList));
                    methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(asList));
                    toAdd = asList;
                } else if (isSet || isList) {
                    methods.add(ToMethod.ADD_TO_COLLECTION.apply(toAdd));
                    methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(toAdd));
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                    methods.add(ToMethod.WITH_ARRAY.apply(toAdd));
                } else if (isMap) {
                    methods.add(ToMethod.ADD_TO_MAP.apply(toAdd));
                    methods.add(ToMethod.ADD_MAP_TO_MAP.apply(toAdd));
                    methods.add(ToMethod.REMOVE_FROM_MAP.apply(toAdd));
                    methods.add(ToMethod.REMOVE_MAP_FROM_MAP.apply(toAdd));
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                } else {
                    toAdd = new PropertyBuilder(toAdd).addToAttributes(BUILDABLE, isBuildable).build();
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                }

                if (isMap) {
                    //
                } else if (isBuildable && !isAbstract) {
                    methods.add(ToMethod.WITH_NEW_NESTED.apply(toAdd));
                    methods.add(ToMethod.WITH_NEW_LIKE_NESTED.apply(toAdd));
                    if (!isCollection && !isArray) {
                        methods.add(ToMethod.EDIT_NESTED.apply(toAdd));
                    }
                    methods.addAll(ToMethod.WITH_NESTED_INLINE.apply(toAdd));
                    nestedClazzes.add(PropertyAs.NESTED_INTERFACE.apply(toAdd));
                } else if (!descendants.isEmpty() && isCollection) {
                    for (Property descendant : descendants) {
                        if (isCollection(descendant.getTypeRef())) {
                            methods.add(ToMethod.ADD_TO_COLLECTION.apply(descendant));
                            methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(descendant));
                        }

                        methods.add(ToMethod.WITH_NEW_NESTED.apply(descendant));
                        methods.add(ToMethod.WITH_NEW_LIKE_NESTED.apply(descendant));
                        methods.addAll(ToMethod.WITH_NESTED_INLINE.apply(descendant));
                        nestedClazzes.add(PropertyAs.NESTED_INTERFACE.apply(descendant));
                    }
                }
            }

            return new ClassDefBuilder(fluentType)
                    .withInnerTypes(nestedClazzes)
                    .withMethods(methods)
                    .build();

        }
    });

    public static final Function<ClassDef, ClassDef> FLUENT_IMPL = FunctionFactory.wrap(new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            Set<Method> constructors = new LinkedHashSet<Method>();
            Set<Method> methods = new LinkedHashSet<Method>();
            Set<TypeDef> nestedClazzes = new LinkedHashSet<TypeDef>();
            final Set<Property> properties = new LinkedHashSet<Property>();
            ClassDef fluentType = TypeAs.FLUENT_INTERFACE.apply(item);
            final ClassDef fluentImplType = TypeAs.FLUENT_IMPL.apply(item);

            //The generic letter is always the last
            final TypeParamDef genericType = fluentImplType.getParameters().get(fluentImplType.getParameters().size() - 1);

            Method emptyConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .build();

            Method instanceConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(item.toReference())
                    .withName("instance").and()
                    .withNewBlock()
                    .withStatements(toInstanceConstructorBody(item, ""))
                    .endBlock()
                    .build();

            constructors.add(emptyConstructor);
            constructors.add(instanceConstructor);

            for (final Property property : item.getProperties()) {
                final TypeRef unwrapped = TypeAs.combine(TypeAs.UNWRAP_ARRAY_OF, TypeAs.UNWRAP_COLLECTION_OF).apply(property.getTypeRef());

                if (property.isStatic()) {
                    continue;
                }
                if (!hasBuildableConstructorWithArgument(item, property) && !hasOrInheritsSetter(item, property)) {
                    continue;
                }


                final boolean isBuildable = isBuildable(unwrapped);
                final boolean isArray = isArray(property.getTypeRef());
                final boolean isSet = isSet(property.getTypeRef());
                final boolean isList = isList(property.getTypeRef());
                final boolean isMap = isMap(property.getTypeRef());
                final boolean isCollection = isSet || isList;
                final boolean isAbstract = isAbstract(unwrapped);

                Property toAdd = new PropertyBuilder(property)
                        .withModifiers(TypeUtils.modifiersToInt(Modifier.PRIVATE))
                        .addToAttributes(OUTER_INTERFACE, fluentType)
                        .addToAttributes(OUTER_CLASS, fluentImplType)
                        .addToAttributes(GENERIC_TYPE_REF, genericType.toReference())
                        .accept(new TypedVisitor<PropertyBuilder>() {
                            public void visit(PropertyBuilder builder) {
                                if (isArray || isList) {
                                    ClassRef listRef =  ARRAY_LIST.toReference(unwrapped);
                                    builder.addToAttributes(INIT, "new " + listRef+ "()")
                                            .addToAttributes(ALSO_IMPORT, listRef);
                                } else if (isSet) {
                                    ClassRef setRef = LINKED_HASH_SET.toReference(unwrapped);
                                    builder.addToAttributes(INIT, "new " + setRef + "()")
                                            .addToAttributes(ALSO_IMPORT, setRef);
                                } else if (isMap) {
                                    List<TypeRef> arguments = ((ClassRef)property.getTypeRef()).getArguments();
                                    ClassRef mapRef = LINKED_HASH_MAP.toReference(arguments.toArray(new TypeRef[arguments.size()]));
                                    builder.addToAttributes(INIT, "new " + mapRef  + "()")
                                            .addToAttributes(ALSO_IMPORT, mapRef);
                                }
                            }
                        }).build();

                Set<Property> descendants = Decendants.PROPERTY_BUILDABLE_DESCENDANTS.apply(toAdd);
                toAdd = new PropertyBuilder(toAdd).addToAttributes(DESCENDANTS, descendants).build();

                if (isArray) {
                    Property asList = arrayAsList(toAdd);
                    methods.add(ToMethod.WITH_ARRAY.apply(toAdd));
                    methods.add(ToMethod.GETTER_ARRAY.apply(toAdd));
                    methods.add(ToMethod.ADD_TO_COLLECTION.apply(asList));
                    methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(asList));
                    toAdd = asList;
                } else if (isSet || isList) {
                    methods.add(ToMethod.ADD_TO_COLLECTION.apply(toAdd));
                    methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(toAdd));
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                    methods.add(ToMethod.WITH_ARRAY.apply(toAdd));
                } else if (isMap) {
                    methods.add(ToMethod.ADD_TO_MAP.apply(toAdd));
                    methods.add(ToMethod.ADD_MAP_TO_MAP.apply(toAdd));
                    methods.add(ToMethod.REMOVE_FROM_MAP.apply(toAdd));
                    methods.add(ToMethod.REMOVE_MAP_FROM_MAP.apply(toAdd));
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                } else {
                    methods.add(ToMethod.GETTER.apply(toAdd));
                    methods.add(ToMethod.WITH.apply(toAdd));
                }

                if (isMap) {
                    properties.add(toAdd);
                } else if (isBuildable && !isAbstract) {
                    methods.add(ToMethod.WITH_NEW_NESTED.apply(toAdd));
                    methods.add(ToMethod.WITH_NEW_LIKE_NESTED.apply(toAdd));
                    if (!isCollection && !isArray) {
                        methods.add(ToMethod.EDIT_NESTED.apply(toAdd));
                    }
                    methods.addAll(ToMethod.WITH_NESTED_INLINE.apply(toAdd));
                    nestedClazzes.add(PropertyAs.NESTED_CLASS.apply(toAdd));
                    properties.add(buildableField(toAdd));
                } else if (descendants.isEmpty()) {
                    properties.add(toAdd);
                } else if (isCollection) {
                    properties.add(buildableField(toAdd));
                    for (Property descendant : descendants) {
                        if (isCollection(descendant.getTypeRef())) {
                            methods.add(ToMethod.ADD_TO_COLLECTION.apply(descendant));
                            methods.add(ToMethod.REMOVE_FROM_COLLECTION.apply(descendant));
                        }
                        methods.add(ToMethod.WITH_NEW_NESTED.apply(descendant));
                        methods.add(ToMethod.WITH_NEW_LIKE_NESTED.apply(descendant));
                        methods.addAll(ToMethod.WITH_NESTED_INLINE.apply(descendant));
                        nestedClazzes.add(PropertyAs.NESTED_CLASS.apply(descendant));
                    }

                } else {
                    properties.add(buildableField(toAdd));
                }
            }

            Method equals = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withReturnType(ClassTo.TYPEREF.apply(boolean.class))
                    .addNewArgument().withName("o").withTypeRef(Constants.OBJECT.toReference()).endArgument()
                    .withName("equals")
                    .withNewBlock()
                    .withStatements(toEquals(fluentImplType, properties))
                    .endBlock()
                    .build();

            methods.add(equals);

            return new ClassDefBuilder(fluentImplType)
                    .withConstructors(constructors)
                    .withProperties(properties)
                    .withInnerTypes(nestedClazzes)
                    .withMethods(methods)
                    .build();
        }
    });


    public static final Function<ClassDef, ClassDef> BUILDER = FunctionFactory.wrap(new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            final Modifier[] modifiers = item.isAbstract()
                    ? new Modifier[]{Modifier.PUBLIC, Modifier.ABSTRACT}
                    : new Modifier[]{Modifier.PUBLIC};

            ClassDef builderType = TypeAs.BUILDER.apply(item);
            ClassRef instanceRef = item.toInternalReference();

            ClassRef fluent = TypeAs.FLUENT_REF.apply(item);

            Set<Method> constructors = new LinkedHashSet<Method>();
            Set<Method> methods = new LinkedHashSet<Method>();
            Set<Property> fields = new LinkedHashSet<Property>();

            Property fluentProperty = new PropertyBuilder().withTypeRef(fluent).withName("fluent").build();
            Property validationEnabledProperty = new PropertyBuilder().withTypeRef(BOOLEAN_REF).withName("validationEnabled").build();

            fields.add(fluentProperty);
            fields.add(validationEnabledProperty);

            Method emptyConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withNewBlock()
                    .addNewStringStatementStatement("this(true);")
                    .endBlock()
                    .build();

            Method validationConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                        .withTypeRef(BOOLEAN_REF)
                        .withName("validationEnabled")
                    .and()
                    .withNewBlock()
                    .addNewStringStatementStatement(hasDefaultConstructor(item) ? "this(new " + item.getName() + "(), validationEnabled);" : "this.fluent = this; this.validationEnabled=validationEnabled;")
                    .endBlock()
                    .build();


            Method fluentConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(fluent)
                    .withName("fluent")
                    .and()
                    .withNewBlock()
                    .addNewStringStatementStatement("this(fluent, true);")
                    .endBlock()
                    .build();

            Method fluentAndValidationConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(fluent)
                    .withName("fluent")
                    .and()
                    .addNewArgument()
                    .withTypeRef(BOOLEAN_REF)
                    .withName("validationEnabled")
                    .and()
                    .withNewBlock()
                    .addNewStringStatementStatement(hasDefaultConstructor(item) ? "this(fluent, new " + item.getName() + "(), validationEnabled);" : "this.fluent = fluent; this.validationEnabled=validationEnabled;")
                    .endBlock()
                    .build();

            Method instanceAndFluentCosntructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(fluent)
                    .withName("fluent")
                    .and()
                    .addNewArgument()
                    .withTypeRef(instanceRef)
                    .withName("instance").and()
                    .withNewBlock()
                    .addNewStringStatementStatement("this(fluent, instance, true);")
                    .endBlock()
                    .build();

            List<Statement> instanceAndFluentConstructorStatements = toInstanceConstructorBody(item, "fluent");
            instanceAndFluentConstructorStatements.add(new StringStatement("this.validationEnabled = validationEnabled; "));

            Method instanceAndFluentAndValidationCosntructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(fluent)
                    .withName("fluent")
                    .and()
                    .addNewArgument()
                    .withTypeRef(instanceRef)
                    .withName("instance").and()
                    .addNewArgument()
                    .withTypeRef(BOOLEAN_REF)
                    .withName("validationEnabled")
                    .and()
                    .withNewBlock()
                    .withStatements(instanceAndFluentConstructorStatements)
                    .endBlock()
                    .build();

            Method instanceConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(instanceRef)
                    .withName("instance").and()
                    .withNewBlock()
                        .addNewStringStatementStatement("this(instance,true);")
                    .endBlock()
                    .build();

            List<Statement> instanceConstructorStatements = toInstanceConstructorBody(item, "this");
            instanceConstructorStatements.add(new StringStatement("this.validationEnabled = validationEnabled; "));

            Method instanceAndValidationConstructor = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .addNewArgument()
                    .withTypeRef(instanceRef)
                    .withName("instance").and()
                    .addNewArgument()
                    .withTypeRef(BOOLEAN_REF)
                    .withName("validationEnabled")
                    .and()
                    .withNewBlock()
                    .withStatements(instanceConstructorStatements)
                    .endBlock()
                    .build();

            constructors.add(emptyConstructor);
            constructors.add(validationConstructor);
            constructors.add(fluentConstructor);
            constructors.add(fluentAndValidationConstructor);
            constructors.add(instanceAndFluentCosntructor);
            constructors.add(instanceAndFluentAndValidationCosntructor);
            constructors.add(instanceConstructor);
            constructors.add(instanceAndValidationConstructor);

            Method build = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(modifiers))
                    .withReturnType(instanceRef)
                    .withName("build")
                    .withNewBlock()
                    .withStatements(toBuild(item, item))
                    .endBlock()
                    .build();
            methods.add(build);

            //  private <T> void validate(T item) {}
            Boolean validationGloballyEnabled = item.getAttributes().containsKey(VALIDATION_ENABLED) && (Boolean) item.getAttributes().get(VALIDATION_ENABLED);
            Method validate = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PRIVATE))
                    .withParameters(T)
                    .withReturnType(Constants.VOID)
                    .addNewArgument()
                        .withName("item")
                        .withTypeRef(T_REF)
                    .endArgument()
                    .withName("validate")
                    .withNewBlock()
                        .withStatements(toValidate(item,validationGloballyEnabled))
                    .endBlock()
                    .addToAttributes(ALSO_IMPORT, validationGloballyEnabled ? Constants.VALIDATION_REFS : Collections.emptyList())
                    .build();

            methods.add(validate);

            Method equals = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(Modifier.PUBLIC))
                    .withReturnType(ClassTo.TYPEREF.apply(boolean.class))
                    .addNewArgument().withName("o").withTypeRef(Constants.OBJECT.toReference()).endArgument()
                    .withName("equals")
                    .withNewBlock()
                    .withStatements(toEquals(builderType, fields))
                    .endBlock()
                    .build();

            methods.add(equals);

            return new ClassDefBuilder(builderType)
                    .withModifiers(TypeUtils.modifiersToInt(modifiers))
                    .withProperties(fields)
                    .withConstructors(constructors)
                    .withMethods(methods)
                    .build();
        }

    });

    public static final Function<ClassDef, ClassDef> EDITABLE_BUILDER = FunctionFactory.wrap(new Function<ClassDef, ClassDef>() {
        public ClassDef apply(final ClassDef item) {
            final Modifier[] modifiers = item.isAbstract()
                    ? new Modifier[]{Modifier.PUBLIC, Modifier.ABSTRACT}
                    : new Modifier[]{Modifier.PUBLIC};

            final ClassDef editable = EDITABLE.apply(item);
            return new ClassDefBuilder(BUILDER.apply(item)).accept(new TypedVisitor<MethodBuilder>() {
                public void visit(MethodBuilder builder) {
                    if (builder.getName() != null && builder.getName().equals("build")) {
                        builder.withModifiers(TypeUtils.modifiersToInt(modifiers));
                        builder.withReturnType(editable.toInternalReference());
                        builder.withNewBlock()
                                .withStatements(toBuild(editable, editable))
                                .endBlock();
                    }
                }
            }).build();
        }
    });

    public static final Function<ClassDef, ClassDef> EDITABLE = FunctionFactory.wrap(new Function<ClassDef, ClassDef>() {
        public ClassDef apply(ClassDef item) {
            Modifier[] modifiers = item.isAbstract()
                    ? new Modifier[]{Modifier.PUBLIC, Modifier.ABSTRACT}
                    : new Modifier[]{Modifier.PUBLIC};

            ClassDef editableType = TypeAs.EDITABLE.apply(item);
            ClassDef builderType = TypeAs.BUILDER.apply(item);

            Set<Method> constructors = new LinkedHashSet<Method>();
            Set<Method> methods = new LinkedHashSet<Method>();

            for (Method constructor : item.getConstructors()) {
                constructors.add(superConstructorOf(constructor, editableType));
            }

            Method edit = new MethodBuilder()
                    .withModifiers(TypeUtils.modifiersToInt(modifiers))
                    .withReturnType(builderType.toInternalReference())
                    .withName("edit")
                    .withNewBlock()
                        .addNewStringStatementStatement("return new " + builderType.getName() + "(this);")
                    .endBlock()
                    .build();

            methods.add(edit);

            //We need to treat the editable classes as buildables themselves.
            return CodegenContext.getContext().getDefinitionRepository().register(
                    BuilderContextManager.getContext().getBuildableRepository().register(new ClassDefBuilder(editableType)
                            .withModifiers(TypeUtils.modifiersToInt(modifiers))
                            .withConstructors(constructors)
                            .withMethods(methods)
                            .addToAttributes(BUILDABLE, true)
                            .addToAttributes(GENERATED, true) // We want to know that its a generated type...
                            .build())
            );
        }
    });

    private static Property arrayAsList(Property property) {
        return new PropertyBuilder(property)
                .withTypeRef(TypeAs.ARRAY_AS_LIST.apply(TypeAs.BOXED_OF.apply(property.getTypeRef())))
                .build();
    }

    private static List<Statement> toInstanceConstructorBody(ClassDef clazz, String fluent) {
        Method constructor = findBuildableConstructor(clazz);
        List<Statement> statements = new ArrayList<Statement>();
        String ref = fluent;

        //We may use a reference to fluent or we may use directly "this". So we need to check.
        if (fluent != null && !fluent.isEmpty()) {
            statements.add(new StringStatement("this.fluent = " + fluent + "; "));
        } else {
            ref = "this";
        }

        for (Property property : constructor.getArguments()) {
            Method getter = findGetter(clazz, property);
            if (getter != null) {
                String cast = property.getTypeRef() instanceof TypeParamRef ? "(" + property.getTypeRef().toString() + ")" : "";
                statements.add(new StringStatement(new StringBuilder().append(ref).append(".with").append(property.getNameCapitalized()).append("(").append(cast).append("instance.").append(getter.getName()).append("()); ").toString()));
            } else {
                throw new IllegalStateException("Could not find getter for property:" + property + " in class:" + clazz);
            }
        }

        ClassDef target = clazz;
        //Iterate parent objects and check for properties with setters but not ctor arguments.
        while (target != null && !OBJECT.equals(target) && BuilderUtils.isBuildable(target)) {
            for (Property property : target.getProperties()) {
                if (!hasBuildableConstructorWithArgument(target, property) && hasSetter(target, property)) {
                    String withName = "with" + property.getNameCapitalized();
                    String getterName = BuilderUtils.findGetter(target, property).getName();
                    statements.add(new StringStatement(new StringBuilder().append(ref).append(".").append(withName).append("(instance.").append(getterName).append("());\n").toString()));
                }
            }

            if (!target.getExtendsList().isEmpty()) {
                target = BuilderContextManager.getContext().getBuildableRepository().getBuildable(target.getExtendsList().iterator().next());
            } else {
                return statements;
            }
        }
        return statements;
    }

    private static List<Statement> toBuild(final ClassDef clazz, final ClassDef instanceType) {
        Method constructor = findBuildableConstructor(clazz);
        List<Statement> statements = new ArrayList<Statement>();

        statements.add(new StringStatement(new StringBuilder()
                .append(instanceType.getName()).append(" buildable = new ").append(instanceType.getName()).append("(")
                .append(StringUtils.join(constructor.getArguments(), new Function<Property, String>() {
                    public String apply(Property item) {
                        String prefix = isBoolean(item.getTypeRef()) ? "is" : "get";
                        //String cast = genericTypes.contains(item.getTypeRef().getFullyQualifiedName()) ? "("+item.getType().getFullyQualifiedName()+")" : "";
                        return "fluent." + prefix + item.getNameCapitalized() + "()";
                    }
                }, ","))
                .append(");")
                .toString()));


        ClassDef target = clazz;

        //Iterate parent objects and check for properties with setters but not ctor arguments.
        while (target != null && !OBJECT.equals(target) && BuilderUtils.isBuildable(target)) {
            for (Property property : target.getProperties()) {
                if (!hasBuildableConstructorWithArgument(target, property) && hasSetter(target, property)) {
                    String setterName = "set" + property.getNameCapitalized();
                    String getterName = BuilderUtils.findGetter(target, property).getName();
                    statements.add(new StringStatement(new StringBuilder()
                            .append("buildable.").append(setterName).append("(fluent.").append(getterName).append("());")
                            .toString()));

                }
            }
            target = BuilderContextManager.getContext().getBuildableRepository().getBuildable(target.getExtendsList().iterator().next());
        }

        statements.add(new StringStatement("validate(buildable);"));
        statements.add(new StringStatement("return buildable;"));
        return statements;
    }


    private static List<Statement> toEquals(ClassDef type, Collection<Property> properties) {
        List<Statement> statements = new ArrayList<Statement>();

        String simpleName = type.getName();
        ClassRef superClass = type.getExtendsList().isEmpty() ? ClassDef.OBJECT_REF : type.getExtendsList().iterator().next();
        statements.add(new StringStatement("if (this == o) return true;"));
        statements.add(new StringStatement("if (o == null || getClass() != o.getClass()) return false;"));

        //If base fluent is the superclass just skip.
        if (!Constants.BASE_FLUENT.getFullyQualifiedName().equals(superClass.getDefinition().getFullyQualifiedName())) {
            statements.add(new StringStatement("if (!super.equals(o)) return false;"));
        }
        statements.add(new StringStatement(new StringBuilder().append(simpleName).append(" that = (").append(simpleName).append(") o;").toString()));

        for (Property property : properties) {
            String name = property.getName();
            if (BuilderUtils.isPrimitive(property.getTypeRef())) {
                statements.add(new StringStatement(new StringBuilder().append("if (").append(name).append(" != ").append("that.").append(name).append(") return false;").toString()));
            } else if (property.getTypeRef() instanceof ClassRef && Decendants.isDescendant(type, ((ClassRef) property.getTypeRef()).getDefinition())) {
                statements.add(new StringStatement(new StringBuilder()
                        .append("if (").append(name).append(" != null &&").append(name).append(" != this ? !").append(name).append(".equals(that.").append(name).append(") :")
                        .append("that.").append(name).append(" != null &&").append(name).append(" != this ) return false;").append("\n")
                        .toString()));
            } else {
                statements.add(new StringStatement(new StringBuilder().append("if (").append(name).append(" != null ? !").append(name).append(".equals(that.").append(name).append(") :")
                        .append("that.").append(name).append(" != null) return false;").toString()));

            }
        }

        statements.add(new StringStatement("return true;"));
        return statements;
    }


    private static List<Statement> toValidate(ClassDef type, boolean enabled) {
        List<Statement> statements = new ArrayList<Statement>();
        if (enabled) {
            statements.add(new StringStatement("if (!validationEnabled) { return; }"));
            statements.add(new StringStatement("Validator validator = null;"));
            statements.add(new StringStatement("try {"));
            statements.add(new StringStatement("    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();"));
            statements.add(new StringStatement("    validator = factory.getValidator();"));
            statements.add(new StringStatement("} catch(ValidationException e) {return;}"));
            statements.add(new StringStatement("Set<ConstraintViolation<T>> violations = validator.validate(item);"));
            statements.add(new StringStatement("if (!violations.isEmpty()) {"));
            statements.add(new StringStatement("throw new ConstraintViolationException(violations);"));
            statements.add(new StringStatement(" }"));
        }
        return statements;
    }


    private static Method superConstructorOf(Method constructor, ClassDef constructorType) {
        return new MethodBuilder(constructor)
                .withReturnType(constructorType.toReference())
                .withNewBlock()
                .addNewStringStatementStatement("super(" + StringUtils.join(constructor.getArguments(), new Function<Property, String>() {
                    public String apply(Property item) {
                        return item.getName();
                    }
                }, ", ") + ");")
                .endBlock()
                .build();
    }

    private static Property buildableField(Property property) {
        TypeRef typeRef = property.getTypeRef();
        TypeRef unwrapped = TypeAs.combine(TypeAs.UNWRAP_COLLECTION_OF, TypeAs.UNWRAP_ARRAY_OF).apply(typeRef);
        ClassRef classRef = (ClassRef) typeRef;
        ClassRef builderType = TypeAs.VISITABLE_BUILDER.apply(unwrapped);


        if (isList(classRef)) {
            ClassRef listRef =  ARRAY_LIST.toReference(builderType);
            return new PropertyBuilder(property).withTypeRef(LIST.toReference(builderType))
                    .addToAttributes(INIT, " new " + listRef + "()")
                    .addToAttributes(ALSO_IMPORT, Arrays.asList(listRef, builderType))
                    .build();
        } else if (isSet(classRef)) {
            ClassRef setRef = LINKED_HASH_SET.toReference(builderType);
            return new PropertyBuilder(property).withTypeRef(SET.toReference(builderType))
                    .addToAttributes(INIT, " new " + setRef+ "()")
                    .addToAttributes(ALSO_IMPORT,  Arrays.asList(setRef, builderType))
                    .build();
        } else {
            return new PropertyBuilder(property).withTypeRef(builderType)
                    .addToAttributes(ALSO_IMPORT, builderType)
                    .build();
        }
    }
}
