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

package io.sundr.builder.internal.processor;

import io.sundr.builder.Constants;
import io.sundr.builder.annotations.ExternalBuildables;
import io.sundr.builder.annotations.Inline;
import io.sundr.builder.internal.BuilderContext;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.builder.internal.functions.ClazzAs;
import io.sundr.builder.internal.utils.BuilderUtils;
import io.sundr.codegen.functions.ElementTo;
import io.sundr.codegen.model.ClassDef;
import io.sundr.codegen.model.ClassDefBuilder;
import io.sundr.codegen.utils.ModelUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

import static io.sundr.builder.Constants.VALIDATION_ENABLED;

@SupportedAnnotationTypes("io.sundr.builder.annotations.ExternalBuildables")
public class ExternalBuildableProcessor extends AbstractBuilderProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        Filer filer = processingEnv.getFiler();

        //First pass register all externals
        for (TypeElement annotation : annotations) {
            for (Element element : env.getElementsAnnotatedWith(annotation)) {
                ExternalBuildables generated = element.getAnnotation(ExternalBuildables.class);
                BuilderContext ctx = BuilderContextManager.create(elements, types, generated.generateBuilderPackage(), generated.builderPackage());
                for (String name : generated.value()) {
                    TypeElement typeElement = elements.getTypeElement(name);
                    ClassDef b = ctx.getBuildableRepository().register(ElementTo.TYPEDEF.apply(typeElement));
                    ctx.getDefinitionRepository().register(b);
                    ctx.getBuildableRepository().register(b);
                }
                for (TypeElement ref : BuilderUtils.getBuildableReferences(ctx, generated)) {
                    ClassDef b = ElementTo.TYPEDEF.apply(ModelUtils.getClassElement(ref));
                    ctx.getDefinitionRepository().register(b);
                    ctx.getBuildableRepository().register(b);
                }
            }
        }

        for (TypeElement annotation : annotations) {
            for (Element element : env.getElementsAnnotatedWith(annotation)) {
                ExternalBuildables generated = element.getAnnotation(ExternalBuildables.class);
                for (String name : generated.value()) {
                    TypeElement typeElement = elements.getTypeElement(name);
                    if (typeElement == null) {
                        processingEnv
                                .getMessager()
                                .printMessage(Diagnostic.Kind.WARNING, "Type:" + name + " doesn't exists. Ignoring...");
                        continue;
                    }
                    BuilderContext ctx = BuilderContextManager.getContext();
                    ClassDef typeDef = new ClassDefBuilder(ElementTo.TYPEDEF.apply(ModelUtils.getClassElement(element))).addToAttributes(VALIDATION_ENABLED, generated.validationEnabled()).build();
                    generateLocalDependenciesIfNeeded();
                    try {
                        generateFromClazz(ClazzAs.FLUENT_INTERFACE.apply(typeDef),
                                Constants.DEFAULT_SOURCEFILE_TEMPLATE_LOCATION);

                        if (generated.editableEnabled()) {
                            generateFromClazz(ClazzAs.EDITABLE_BUILDER.apply(typeDef),
                                    Constants.DEFAULT_SOURCEFILE_TEMPLATE_LOCATION);

                            generateFromClazz(ClazzAs.EDITABLE.apply(typeDef),
                                    Constants.DEFAULT_SOURCEFILE_TEMPLATE_LOCATION);
                        } else {
                            generateFromClazz(ClazzAs.BUILDER.apply(typeDef),
                                    Constants.DEFAULT_SOURCEFILE_TEMPLATE_LOCATION);
                        }


                        for (final Inline inline : generated.inline()) {
                            generateFromClazz(inlineableOf(ctx, typeDef, inline),
                                    Constants.DEFAULT_SOURCEFILE_TEMPLATE_LOCATION);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }
}
