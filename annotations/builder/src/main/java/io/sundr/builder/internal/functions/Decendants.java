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

package io.sundr.builder.internal.functions;

import io.sundr.FunctionFactory;
import io.sundr.Function;
import io.sundr.builder.internal.BuildableRepository;
import io.sundr.builder.internal.BuilderContext;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.codegen.model.ClassRef;

import io.sundr.codegen.model.ClassRefBuilder;
import io.sundr.codegen.model.Kind;
import io.sundr.codegen.model.Property;
import io.sundr.codegen.model.PropertyBuilder;
import io.sundr.codegen.model.ClassDef;
import io.sundr.codegen.model.TypeRef;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.sundr.builder.Constants.DESCENDANT_OF;
import static io.sundr.builder.Constants.GENERATED;
import static io.sundr.builder.internal.utils.BuilderUtils.BUILDABLE;
import static io.sundr.codegen.utils.TypeUtils.classRefOf;
import static io.sundr.codegen.utils.StringUtils.deCaptializeFirst;
import static io.sundr.builder.internal.functions.CollectionTypes.IS_COLLECTION;

public class Decendants {

    public static final Function<ClassDef, Set<ClassDef>> BUILDABLE_DECENDANTS = FunctionFactory.cache(new Function<ClassDef, Set<ClassDef>>() {
        public Set<ClassDef> apply(ClassDef item) {
            if (item.equals(ClassDef.OBJECT)) {
                return new LinkedHashSet<ClassDef>();
            }

            Set<ClassDef> result = new LinkedHashSet<ClassDef>();
            BuilderContext ctx = BuilderContextManager.getContext();
            BuildableRepository repository = ctx.getBuildableRepository();

            for (ClassDef type : repository.getBuildables()) {

                if (type.getKind() == Kind.CLASS &&  !type.isAbstract() && isDescendant(type, item) && !type.equals(item) && !type.getAttributes().containsKey(GENERATED)) {
                    result.add(type);
                }
            }
            return result;
        }
    });


    /**
     * Find all buildable descendant equivalents of a property.
     *
     * @param property
     * @return
     */
    public static Function<Property, Set<Property>> PROPERTY_BUILDABLE_DESCENDANTS = FunctionFactory.wrap(new Function<Property, Set<Property>>() {
        public Set<Property> apply(Property property) {
            Set<Property> result = new LinkedHashSet<Property>();
            TypeRef baseType = property.getTypeRef();

            if (IS_COLLECTION.apply(baseType)) {
                TypeRef unwrapped = TypeAs.UNWRAP_COLLECTION_OF.apply(baseType);
                if (unwrapped instanceof  ClassRef) {
                    ClassRef candidate = (ClassRef) unwrapped;

                    for (ClassDef descendant : BUILDABLE_DECENDANTS.apply(candidate.getDefinition())) {
                        ClassRef collectionType = new ClassRefBuilder((ClassRef)baseType).withArguments(descendant.toInternalReference()).build();
                        String propertyName = deCaptializeFirst(descendant.getName()) + property.getNameCapitalized();

                        result.add(new PropertyBuilder(property)
                                .withName(propertyName)
                                .withTypeRef(collectionType)
                                .addToAttributes(DESCENDANT_OF, property)
                                .addToAttributes(BUILDABLE, true)
                                .build());
                    }
                }
            } else if (baseType instanceof  ClassRef) {
                ClassRef candidate = (ClassRef) baseType;
                for (ClassDef descendant : BUILDABLE_DECENDANTS.apply(candidate.getDefinition())) {
                    String propertyName =  deCaptializeFirst(descendant.getName() + property.getNameCapitalized());

                    result.add(new PropertyBuilder(property)
                            .withName(propertyName)
                            .withTypeRef(classRefOf(descendant))
                            .addToAttributes(DESCENDANT_OF, property)
                            .addToAttributes(BUILDABLE, true)
                            .build());
                }
            }
            return result;
        }
    });

    /**
     * Checks if a type is an descendant of an other type
     *
     * @param item      The base type.
     * @param candidate The candidate type.
     * @return true if candidate is a descendant of base type.
     */
    public static boolean isDescendant(ClassDef item, ClassDef candidate) {
        if (item == null || candidate == null) {
            return false;
        } else if (candidate.isAssignableFrom(item)) {
            return true;
        }
        return false;
    }
}
