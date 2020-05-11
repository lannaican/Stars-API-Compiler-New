package com.star.api.compiler;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.star.annotation.APIService;
import com.star.annotation.SocketService;

import java.io.IOException;
import java.util.HashSet;
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
        try {
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
            //获取Socket
            Set<? extends Element> socketElements = roundEnv.getElementsAnnotatedWith(SocketService.class);
            for (Element element : socketElements) {
                String serviceClass = ((TypeElement)element).getQualifiedName().toString();
                SocketService service = element.getAnnotation(SocketService.class);
                //新建代理类
                TypeSpec.Builder builder = TypeSpec.classBuilder(service.value()).addModifiers(Modifier.PUBLIC);
                List<? extends Element> enclosedElements = element.getEnclosedElements();
                for (Element encloseElement : enclosedElements) {
                    ExecutableElement executableElement = (ExecutableElement) encloseElement;
                    builder.addMethod(buildSocketMethod(serviceClass, executableElement));
                }
                // 创建Java文件
                JavaFile javaFile = JavaFile.builder("com.star.api.auto", builder.build()).build();
                javaFile.writeTo(filer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 指定支持的注解
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(APIService.class.getCanonicalName());
        set.add(SocketService.class.getCanonicalName());
        return set;
    }

    /**
     * 生成方法
     */
    private MethodSpec buildMethod(String serviceClass, ExecutableElement element) {
        ClassName service = ClassName.bestGuess(serviceClass);
        ClassName manager = ClassName.get("com.star.api", "APIManager");
        String methodName = element.getSimpleName().toString();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        builder.addStatement("$T manager = $T.getInstance()", manager, manager);
        builder.addStatement("$T service = ($T)manager.getService($T.class)", service, service, service);
        StringBuilder content = new StringBuilder()
                .append("return service.")
                .append(methodName)
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
        builder.addStatement(content.toString(), service);
        builder.returns(TypeName.get(element.getReturnType()));
        return builder.build();
    }

    /**
     * 生成方法
     */
    private MethodSpec buildSocketMethod(String serviceClass, ExecutableElement element) {
        ClassName service = ClassName.bestGuess(serviceClass);
        ClassName manager = ClassName.get("com.star.api.socket", "SocketManager");
        ClassName socket = ClassName.get("com.star.api.socket", "Socket");
        String methodName = element.getSimpleName().toString();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
        builder.addStatement("$T manager = $T.getInstance()", manager, manager);
        builder.addStatement("$T o = manager.getSocket($T.class)", socket, service);
        builder.addStatement("if(o == null) return");
        builder.addStatement("$T service = ($T)o.getService()", service, service);
        builder.addStatement("if(service == null) return");
        StringBuilder content = new StringBuilder()
                .append("service.")
                .append(methodName)
                .append("(");
        for (VariableElement variableElement : element.getParameters()) {
            ParameterSpec spec = ParameterSpec.get(variableElement);
            builder.addParameter(spec);
            content.append(variableElement.getSimpleName()).append(",");
        }
        if (content.lastIndexOf(",") == content.length() - 1) {
            content.deleteCharAt(content.length() - 1);
        }
        content.append(")");
        builder.addStatement(content.toString());
        builder.returns(TypeName.get(element.getReturnType()));
        return builder.build();
    }
}
