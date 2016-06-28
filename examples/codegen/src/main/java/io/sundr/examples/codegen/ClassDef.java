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

package io.sundr.examples.codegen;

import io.sundr.builder.TypedVisitor;
import io.sundr.builder.annotations.Buildable;
import io.sundr.codegen.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Buildable
public class ClassDef extends ModifierSupport implements TypeDef<ClassDef, ClassDefBuilder> {
    public static ClassDef OBJECT = new ClassDefBuilder()
            .withPackageName("java.lang")
            .withName("Object")
            .build();

    private final Kind kind;
    private final String packageName;
    private final String name;

    private final Set<ClassRef> annotations;
    private final Set<ClassRef> extendsList;
    private final Set<ClassRef> implementsList;
    private final List<TypeParamDef> parameters;

    private final Set<Property> properties;
    private final Set<Method> constructors;
    private final Set<Method> methods;
    private final TypeDef outerType;
    private final Set<TypeDef> innerTypes;

    public ClassDef(Kind kind, String packageName, String name, Set<ClassRef> annotations, Set<ClassRef> extendsList, Set<ClassRef> implementsList, List<TypeParamDef> parameters, Set<Property> properties, Set<Method> constructors, Set<Method> methods, TypeDef outerType, Set<TypeDef> innerTypes, int modifiers, Map<String, Object> attributes) {
        super(modifiers, attributes);
        this.kind = kind != null ? kind : Kind.CLASS;
        this.packageName = packageName;
        this.name = name;
        this.annotations = annotations;
        this.extendsList = extendsList;
        this.implementsList = implementsList;
        this.parameters = parameters;
        this.properties = properties;
        this.constructors = adaptConstructors(constructors, this);
        this.methods = methods;
        this.outerType = outerType;
        this.innerTypes = setOuterType(innerTypes, this);
    }

    /**
     * The method adapts constructor method to the current class.
     * It unsets any name that may be presetn in the method.
     * It also sets as a return type a reference to the current type.
     * @param methods
     * @param target
     * @return
     */
    private static Set<Method> adaptConstructors(Set<Method> methods, ClassDef target) {
        Set<Method> adapted = new LinkedHashSet<Method>();
        for (Method m : methods) {
            adapted.add(new MethodBuilder(m)
                    .withName(null)
                    .withReturnType(target.toUnboundedReference())
                    .build());
        }
        return adapted;
    }

    private static Set<TypeDef> setOuterType(Set<TypeDef> types, TypeDef outer) {
        Set<TypeDef> updated = new LinkedHashSet<TypeDef>();
        for (TypeDef typeDef : types) {
            if (typeDef.getOuterType().equals(outer)) {
                updated.add(typeDef);
            } else {
                updated.add(typeDef.edit().withOuterType(outer).build());
            }
        }
        return updated;
    }

    /**
     * Returns the fully qualified name of the type.
     *
     * @return
     */
    public String getFullyQualifiedName() {
        StringBuilder sb = new StringBuilder();
        if (packageName != null && !packageName.isEmpty()) {
            sb.append(getPackageName()).append(".");
        }

        if (outerType != null) {
            sb.append(outerType.getName()).append(".");
        }
        sb.append(getName());

        return sb.toString();
    }

    public boolean isAssignableFrom(ClassDef o) {
        if (this == o || this.equals(o)) {
            return true;
        }

        if (packageName == null && "java.lang".equals(o.packageName) && name.equalsIgnoreCase(o.name)) {
            return true;
        }
        if (o.packageName == null && "java.lang".equals(packageName) && name.equalsIgnoreCase(o.name)) {
            return true;
        }

        for (ClassRef e : o.getExtendsList()) {
            if (isAssignableFrom(e.getDefinition())) {
                return true;
            }
        }

        for (ClassRef i : o.getImplementsList()) {
            if (isAssignableFrom(i.getDefinition())) {
                return true;
            }
        }


        return false;
    }

    public Kind getKind() {
        return kind;
    }

    public Set<ClassRef> getAnnotations() {
        return annotations;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public Set<ClassRef> getExtendsList() {
        return extendsList;
    }

    public Set<ClassRef> getImplementsList() {
        return implementsList;
    }

    public List<TypeParamDef> getParameters() {
        return parameters;
    }

    public Set<Property> getProperties() {
        return properties;
    }

    public Set<Method> getConstructors() {
        return constructors;
    }

    public Set<Method> getMethods() {
        return methods;
    }

    public TypeDef getOuterType() {
        return outerType;
    }

    public Set<TypeDef> getInnerTypes() {
        return innerTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassDef classDef = (ClassDef) o;

        if (packageName != null ? !packageName.equals(classDef.packageName) : classDef.packageName != null) return false;
        return name != null ? name.equals(classDef.name) : classDef.name == null;

    }

    @Override
    public int hashCode() {
        int result = packageName != null ? packageName.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * Creates a {@link ClassRef} for the current definition with the specified arguments.
     * @param arguments The arguements to be passed to the reference.
     * @return
     */
    public ClassRef toReference(TypeRef... arguments) {
        List<TypeRef> actualArguments = new ArrayList<TypeRef>();
        for (int i = 0; i < parameters.size(); i++) {
            if (i < arguments.length) {
                actualArguments.add(arguments[i]);
            } else {
                actualArguments.add(new WildcardRef());
            }
        }
        return new ClassRefBuilder()
                .withDefinition(this)
                .withArguments(actualArguments)
                .build();
    }

    /**
     * Creates a {@link ClassRef} for internal use inside the scope of the type (methods, properties etc).
     * It uses as arguments the same 'letters' as the parameters definition.
     * @return
     */
    public ClassRef toInternalReference() {
        List<TypeRef> arguments = new ArrayList<TypeRef>();
        for (TypeParamDef parameter : parameters) {
            arguments.add(parameter.toReference());
        }
        return new ClassRefBuilder()
                .withDefinition(this)
                .withArguments(arguments)
                .build();
    }

    /**
     * Creates a {@link ClassRef} without bounds.
     * @return
     */
    public ClassRef toUnboundedReference() {
        return new ClassRefBuilder()
                .withDefinition(this)
                .withArguments(new TypeRef[0])
                .build();
    }

    public Set<ClassRef> getImports() {
        final Set<ClassRef> imports = new LinkedHashSet<ClassRef>();
        new TypeDefBuilder(this).accept(new TypedVisitor<ClassRefBuilder>() {
            public void visit(ClassRefBuilder builder) {
                imports.add(builder.build());
            }
        });
        return imports;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isPublic()) {
            sb.append("public ");
        } else if (isProtected()) {
            sb.append("protected ");
        } else if (isPrivate()) {
            sb.append("private ");
        }
        if (isStatic()) {
            sb.append("static ");
        }

        if (isFinal()) {
            sb.append("final ");
        }

        sb.append(kind.name().toLowerCase()).append(" ");
        sb.append(name);

        if (parameters != null && !parameters.isEmpty()) {
            sb.append("<");
            sb.append(StringUtils.join(parameters, ","));
            sb.append(">");
        }

        if (extendsList != null && !extendsList.isEmpty()
                && (extendsList.size() != 1 || !extendsList.contains(OBJECT.toReference()))) {
            sb.append(" extends ");
            sb.append(StringUtils.join(extendsList, ","));
        }

        if (implementsList != null && !implementsList.isEmpty()) {
            sb.append(" implements ");
            sb.append(StringUtils.join(implementsList, ","));
        }

        return sb.toString();
    }
}
