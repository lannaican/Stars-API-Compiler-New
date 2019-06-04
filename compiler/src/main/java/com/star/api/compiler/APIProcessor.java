package com.star.api.compiler;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.star.annotation.APIService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(Processor.class)
public class APIProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //获取Service
        Set<? extends Element> serviceElements = roundEnv.getElementsAnnotatedWith(APIService.class);
        for (Element element : serviceElements) {
            String serviceClass = ((TypeElement)element).getQualifiedName().toString();
            APIService service = element.getAnnotation(APIService.class);
            //新建代理类
            TypeSpec.Builder builder = TypeSpec.classBuilder(service.value()).addModifiers(Modifier.PUBLIC);
            List<? extends Element> enclosedElements = element.getEnclosedElements();
            for (Element encloseElement : enclosedElements) {
                ExecutableElement executableElement = (ExecutableElement) encloseElement;
                builder.addMethod(buildMethod(serviceClass, executableElement));
            }
            // 创建Java文件
            JavaFile javaFile = JavaFile.builder("com.star.api.auto", builder.build()).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 指定支持的注解
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(APIService.class.getCanonicalName());
    }

    /**
     * 生成方法
     */
    private MethodSpec buildMethod(String serviceClass, ExecutableElement element) {
        String name = element.getSimpleName().toString();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(name);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        StringBuilder content = new StringBuilder()
                .append("$T manager = $T.getInstance();")
                .append("\n")
                .append("return (($T)manager.getService($T.class)).")
                .append(name)
                .append("(");
        for (VariableElement variableElement : element.getParameters()) {
            ParameterSpec spec = ParameterSpec.get(variableElement);
            builder.addParameter(spec);
            content.append(variableElement.getSimpleName()).append(",");
        }
        if (content.lastIndexOf(",") == content.length() - 1) {
            content.deleteCharAt(content.length() - 1);
        }
        content.append(").setResolver(manager.getResolver($T.class))");
        ClassName service = ClassName.bestGuess(serviceClass);
        ClassName manager = ClassName.get("com.star.api", "APIManager");
        builder.addStatement(content.toString(), manager, manager, service, service, service);
        builder.returns(TypeName.get(element.getReturnType()));
        return builder.build();
    }
}
