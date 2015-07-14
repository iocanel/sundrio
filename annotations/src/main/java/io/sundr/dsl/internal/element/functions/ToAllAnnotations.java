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

package io.sundr.dsl.internal.element.functions;

import io.sundr.Function;
import io.sundr.dsl.annotations.All;
import io.sundr.dsl.annotations.Any;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.LinkedHashSet;
import java.util.Set;

public class ToAllAnnotations implements Function<ExecutableElement, Set<AnnotationMirror>> {

    private final TypeElement ALL;


    public ToAllAnnotations(Elements elements) {
        ALL = elements.getTypeElement(All.class.getCanonicalName());
    }

    public Set<AnnotationMirror> apply(ExecutableElement element) {
        Set<AnnotationMirror> annotationMirrors = new LinkedHashSet<AnnotationMirror>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement().equals(ALL)) {
                annotationMirrors.add(mirror);
            }
            //Also look for use on custom annotations
            for (AnnotationMirror innerMirror : mirror.getAnnotationType().asElement().getAnnotationMirrors()) {
                if (innerMirror.getAnnotationType().asElement().equals(ALL)) {
                    annotationMirrors.add(innerMirror);
                }
            }
        }
        return annotationMirrors;
    }
}
