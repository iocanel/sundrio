/*
 *      Copyright 2016 The original authors.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.sundr.codegen.model;

import io.sundr.builder.Editable;
import io.sundr.codegen.model.*;
import io.sundr.codegen.model.Block;
import io.sundr.codegen.model.ClassRef;
import io.sundr.codegen.model.Method;
import io.sundr.codegen.model.MethodBuilder;
import io.sundr.codegen.model.Property;
import io.sundr.codegen.model.TypeParamDef;
import io.sundr.codegen.model.TypeRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditableMethod extends Method implements Editable<MethodBuilder>{


public EditableMethod(Set<ClassRef> annotations , Set<TypeParamDef> parameters , String name , TypeRef returnType , List<Property> arguments , boolean varArgPreferred , Set<ClassRef> exceptions , Block block , int modifiers , Map<String, Object> attributes ){
    super(annotations, parameters, name, returnType, arguments, varArgPreferred, exceptions, block, modifiers, attributes);
}

public MethodBuilder edit(){
    return new MethodBuilder(this);
}


}
    
