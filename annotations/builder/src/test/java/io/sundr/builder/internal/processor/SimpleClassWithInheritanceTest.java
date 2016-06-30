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

package io.sundr.builder.internal.processor;

import io.sundr.builder.internal.functions.ClazzAs;
import io.sundr.codegen.functions.Sources;
import io.sundr.codegen.model.ClassRef;
import io.sundr.codegen.model.Kind;
import io.sundr.codegen.model.ClassDef;
import io.sundr.codegen.model.TypeRef;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class SimpleClassWithInheritanceTest extends AbstractProcessorTest {

    ClassDef simpleClassDef = Sources.FROM_INPUTSTEAM_TO_SINGLE_TYPEDEF.apply(getClass().getClassLoader().getResourceAsStream("SimpleClass.java"));
    ClassDef simpleClassWithDateDef = Sources.FROM_INPUTSTEAM_TO_SINGLE_TYPEDEF.apply(getClass().getClassLoader().getResourceAsStream("SimpleClassWithDate.java"));

    @Before
    public void setUp() {
        builderContext.getBuildableRepository().register(simpleClassDef);
    }

    @Test
    public void testFluent() {
        ClassDef fluent = ClazzAs.FLUENT_INTERFACE.apply(simpleClassWithDateDef);
        System.out.println(fluent);

        assertEquals(Kind.INTERFACE, fluent.getKind());
        assertEquals("SimpleClassWithDateFluent", fluent.getName());
        assertEquals(1, fluent.getExtendsList().size());

        ClassRef superClass = fluent.getExtendsList().iterator().next();
        assertEquals("SimpleClassFluent", superClass.getDefinition().getName());
        assertEquals(1, superClass.getArguments().size());
        assertEquals("A", superClass.getArguments().iterator().next().toString());
    }


    @Test
    public void testFluentImpl() throws FileNotFoundException {
        ClassDef fluentImpl = ClazzAs.FLUENT_IMPL.apply(simpleClassWithDateDef);
        System.out.println(fluentImpl);

        assertEquals(Kind.CLASS, fluentImpl.getKind());
        assertEquals("SimpleClassWithDateFluentImpl", fluentImpl.getName());
        assertEquals(1, fluentImpl.getExtendsList().size());


        ClassRef superClass = fluentImpl.getExtendsList().iterator().next();
        assertEquals("SimpleClassFluentImpl", superClass.getDefinition().getName());
        assertEquals(1, superClass.getArguments().size());
        assertEquals("A", superClass.getArguments().iterator().next().toString());
    }

    @Test
    public void testBuilder() throws FileNotFoundException {
        ClassDef builder = ClazzAs.BUILDER.apply(simpleClassWithDateDef);
        System.out.println(builder);

        assertEquals(Kind.CLASS, builder.getKind());
        assertEquals("SimpleClassWithDateBuilder", builder.getName());
        assertEquals(1, builder.getExtendsList().size());

        ClassRef superClass = builder.getImplementsList().iterator().next();
        assertEquals(builderContext.getVisitableBuilderInterface().getName(), superClass.getDefinition().getName());
        assertEquals(2, superClass.getArguments().size());
        Iterator<TypeRef> argIterator = superClass.getArguments().iterator();
        assertEquals("SimpleClassWithDate", argIterator.next().toString());
        assertEquals("SimpleClassWithDateBuilder", argIterator.next().toString());
    }

    @Test
    public void testEditable() {
        ClassDef editable = ClazzAs.EDITABLE.apply(simpleClassWithDateDef);
        System.out.println(editable);

        assertEquals(Kind.CLASS, editable.getKind());
        assertEquals("EditableSimpleClassWithDate", editable.getName());
        assertEquals(1, editable.getExtendsList().size());

        ClassRef superClass = editable.getExtendsList().iterator().next();
        assertEquals(simpleClassWithDateDef.toInternalReference(), superClass);
    }


    @Test
    public void testInline() {
        ClassDef inlineable = BuildableProcessor.inlineableOf(builderContext, simpleClassWithDateDef, inline);
        System.out.println(inlineable);
        assertEquals(Kind.CLASS, inlineable.getKind());
        assertEquals("CallableSimpleClassWithDate", inlineable.getName());
    }
}
