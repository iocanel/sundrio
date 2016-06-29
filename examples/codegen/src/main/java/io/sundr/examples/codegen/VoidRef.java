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

import io.sundr.builder.annotations.Buildable;

import java.util.Collections;
import java.util.Map;

@Buildable
public class VoidRef extends AbstractTypeRef {

    public VoidRef() {
        this(Collections.<String, Object>emptyMap());
    }

    public VoidRef(Map<String, Object> attributes) {
        super(attributes);
    }

    public int getDimensions() {
        return 0;
    }

    public TypeRef withDimensions(int dimensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "void";
    }

    public boolean isAssignableFrom(TypeRef ref) {
        return false;
    }

}
