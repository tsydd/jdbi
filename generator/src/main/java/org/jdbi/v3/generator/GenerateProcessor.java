/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.generator;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jdbi.v3.sqlobject.SqlObject;

@SupportedAnnotationTypes("org.jdbi.v3.sqlobject.Generate")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenerateProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        final TypeElement gens = annotations.iterator().next();
        final Set<? extends Element> annoTypes = roundEnv.getElementsAnnotatedWith(gens);
        annoTypes.forEach(this::generate);
        return true;
    }

    private void generate(Element e) {
        try {
            generate0(e);
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failure: " + ex, e);
            throw new RuntimeException(ex);
        }
    }

    final Set<ElementKind> ACCEPTABLE = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);
    private void generate0(Element e) throws IOException {
        processingEnv.getMessager().printMessage(Kind.WARNING, String.format("[jdbi] generating for %s", e));
        if (!ACCEPTABLE.contains(e.getKind())) {
            throw new IllegalStateException("Generate on non-class: " + e);
        }

        final TypeElement te = (TypeElement) e;
        final String implName = te.getSimpleName() + "Impl";
        final JavaFileObject file = processingEnv.getFiler().createSourceFile(implName, e);
        try (Writer out = file.openWriter()) {
            final TypeSpec.Builder builder = TypeSpec.classBuilder(implName);
            final TypeName superName = TypeName.get(te.asType());
            if (te.getKind() == ElementKind.CLASS) {
                builder.superclass(superName);
            } else {
                builder.addSuperinterface(superName);
            }
            builder.addSuperinterface(SqlObject.class);
            builder.addField(InvocationHandler.class, "handler", Modifier.FINAL, Modifier.PRIVATE);
            builder.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(InvocationHandler.class, "handler")
                    .addCode("this.handler = handler;\n")
                    .build());

            builder.addMethod(generateMethod(builder, getHandle()));

            te.getEnclosedElements().stream()
                .filter(ee -> ee.getKind() == ElementKind.METHOD)
                .filter(ee -> ee.getModifiers().contains(Modifier.ABSTRACT))
                .map(ee -> generateMethod(builder, ee))
                .forEach(builder::addMethod);

            JavaFile.builder(packageName(te), builder.build()).build().writeTo(out);
        }
    }

    private MethodSpec generateMethod(TypeSpec.Builder typeBuilder, Element e) {
        final ExecutableElement ee = (ExecutableElement) e;
        final Builder builder = MethodSpec.overriding(ee);
        final String params = ee.getParameters().stream().
                map(VariableElement::getSimpleName)
                .collect(Collectors.joining(","));
        String methodField = "_" + e.getSimpleName() + "Method";
        typeBuilder.addField(Method.class, methodField, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);
        typeBuilder.addStaticBlock(CodeBlock.of("$L = $L.class.getMethod($S, $L);\n",
                methodField,
                ee.getEnclosingElement().getSimpleName(),
                e.getSimpleName(),
                ee.getParameters().stream().map()));
        processingEnv.getElementUtils().
        return builder.addCode("$L handler.invoke(this, $L, new Object[] {$L});\n",
                ee.getReturnType().getKind() == TypeKind.VOID ? "" : ("return (" + ee.getReturnType().toString() + ")"),
                methodField, params)
            .build();
    }

    private Element getHandle() {
        return processingEnv.getElementUtils().getTypeElement(SqlObject.class.getName()).getEnclosedElements()
                .stream()
                .filter(e -> e.getSimpleName().toString().equals("getHandle"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no Handle.getHandle found"));
    }

    private String packageName(Element e) {
        return processingEnv.getElementUtils().getPackageOf(e).toString();
    }
}
