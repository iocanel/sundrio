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
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class SimpleClassWithParameterTest extends AbstractProcessorTest {

    ClassDef simpleClassWithParameterDef = Sources.FROM_INPUTSTEAM_TO_SINGLE_TYPEDEF.apply(getClass().getClassLoader().getResourceAsStream("SimpleClassWithParameter.java"));

    @Test
    public void testFluent() {
        ClassDef fluent = ClazzAs.FLUENT_INTERFACE.apply(simpleClassWithParameterDef);
        System.out.println(fluent);

        assertEquals(Kind.INTERFACE, fluent.getKind());
        assertEquals("SimpleClassWithParameterFluent", fluent.getName());
        assertEquals(1, fluent.getExtendsList().size());


        ClassRef superClass = fluent.getExtendsList().iterator().next();
        assertEquals("Fluent", superClass.getDefinition().getName());
        assertEquals(1, superClass.getArguments().size());
        assertEquals("A", superClass.getArguments().iterator().next().toString());
    }


    @Test
    public void testFluentImpl() {
        ClassDef fluentImpl = ClazzAs.FLUENT_IMPL.apply(simpleClassWithParameterDef);
        System.out.println(fluentImpl);

        assertEquals(Kind.CLASS, fluentImpl.getKind());
        assertEquals("SimpleClassWithParameterFluentImpl", fluentImpl.getName());
        assertEquals(1, fluentImpl.getExtendsList().size());

        ClassRef superClass = fluentImpl.getExtendsList().iterator().next();
        assertEquals("BaseFluent", superClass.getDefinition().getName());
        assertEquals(1, superClass.getArguments().size());
        assertEquals("A", superClass.getArguments().iterator().next().toString());
    }

    @Test
    public void testBuilder() {
        ClassDef builder = ClazzAs.BUILDER.apply(simpleClassWithParameterDef);
        System.out.println(builder);

        assertEquals(Kind.CLASS, builder.getKind());
        assertEquals("SimpleClassWithParameterBuilder", builder.getName());
        assertEquals(1, builder.getExtendsList().size());


        ClassRef superClass = builder.getImplementsList().iterator().next();
        assertEquals("VisitableBuilder", superClass.getDefinition().getName());
        assertEquals(2, superClass.getArguments().size());
        Iterator<TypeRef> argIterator = superClass.getArguments().iterator();
        assertEquals("SimpleClassWithParameter<N>", argIterator.next().toString());
        assertEquals("SimpleClassWithParameterBuilder<N>", argIterator.next().toString());

    }

    @Test
    public void testEditable() {
        ClassDef editable = ClazzAs.EDITABLE.apply(simpleClassWithParameterDef);
        System.out.println(editable);

        assertEquals(Kind.CLASS, editable.getKind());
        assertEquals("EditableSimpleClassWithParameter", editable.getName());
        assertEquals(1, editable.getExtendsList().size());

        ClassRef superClass = editable.getExtendsList().iterator().next();
        assertEquals(simpleClassWithParameterDef.toInternalReference(), superClass);
    }


    @Test
    public void testInline() {
        ClassDef inlineable = BuildableProcessor.inlineableOf(builderContext, simpleClassWithParameterDef, inline);
        System.out.println(inlineable);
        assertEquals(Kind.CLASS, inlineable.getKind());
        assertEquals("CallableSimpleClassWithParameter", inlineable.getName());
    }
}
