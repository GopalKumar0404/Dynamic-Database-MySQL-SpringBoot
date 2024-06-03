//package com.gopal;
//
//
//import java.lang.reflect.Method;
//public class Main {
//    public static void main(String[] args) {
//        try {
//// Generate and compile Java source files
//
//
//            // Load the compiled classes
//            Class<?> dynamicClass = DynamicClassLoader.loadClass("DynamicClass");
//            Class<?> dynamicInterface = DynamicClassLoader.loadClass("DynamicInterface");
//
//            // Create an instance of the dynamic class
//            Object dynamicInstance = dynamicClass.getDeclaredConstructor().newInstance();
//
//            // Create proxy for DynamicInterface
//            Object proxyInstance = java.lang.reflect.Proxy.newProxyInstance(
//                dynamicClass.getClassLoader(),
//                new Class[]{dynamicInterface},
//                (proxy, method, methodArgs) -> {
//                    if (method.getName().equals("performAction")) {
//                        System.out.println("Performing action from DynamicInterface.");
//                        return null;
//                    } else if (method.getName().equals("baseAction")) {
//                        Method baseActionMethod = dynamicClass.getMethod("baseAction");
//                        return baseActionMethod.invoke(dynamicInstance);
//                    }
//                    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
//                }
//            );
//
//            // Use the interface to perform actions
//            Method performActionMethod = dynamicInterface.getMethod("performAction");
//            performActionMethod.invoke(proxyInstance);
//
//            // Use the class to perform base action
//            Method baseActionMethod = dynamicClass.getMethod("baseAction");
//            baseActionMethod.invoke(proxyInstance);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
//
//
//	//
//	//
//	//import javax.tools.ToolProvider;
//	//import java.io.File;
//	//import java.io.FileWriter;
//	//import java.io.IOException;
//	//import java.lang.reflect.Method;
//	//import java.net.URL;
//	//import java.net.URLClassLoader;
//	//
//	//public class DynamicClassLoaderExample {
//	//    public static void mainss(String[] args) {
//	//        String className = "HelloWorld";
//	//        String sourceCode = """
//	//            public class HelloWorld {
//	//                public void greet() {
//	//                    System.out.println("Hello, World! Gopal Kumar");
//	//                }
//	//            }
//	//        """;
//	//
//	//        // Step 1: Save source in .java file
//	//        File sourceFile = new File(className + ".java");
//	//        try (FileWriter writer = new FileWriter(sourceFile)) {
//	//            writer.write(sourceCode);
//	//        } catch (IOException e) {
//	//            e.printStackTrace();
//	//        }
//	//
//	//        // Step 2: Compile source file
//	//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//	//        int compilationResult = compiler.run(null, null, null, sourceFile.getPath());
//	//        if (compilationResult != 0) {
//	//            System.out.println("Compilation failed.");
//	//            return;
//	//        }
//	//
//	//        // Step 3: Load and instantiate compiled class
//	//        try {
//	//            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(".").toURI().toURL()});
//	//            Class<?> cls = Class.forName(className, true, classLoader);
//	//            Object instance = cls.getDeclaredConstructor().newInstance();
//	//            
//	//            // Step 4: Call method on the instance
//	//            Method method = cls.getMethod("greet");
//	//            method.invoke(instance);
//	//        } catch (Exception e) {
//	//            e.printStackTrace();
//	//        } finally {
//	//            // Clean up the .java and .class files
//	////            if (sourceFile.exists()) {
//	////                sourceFile.delete();
//	////            }
//	////            File classFile = new File(className + ".class");
//	////            if (classFile.exists()) {
//	////                classFile.delete();
//	////            }
//	//        }
//	//    }
//	//}
//
