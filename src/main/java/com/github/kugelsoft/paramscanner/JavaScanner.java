package com.github.kugelsoft.paramscanner;

import com.github.kugelsoft.paramscanner.visitors.ScanClassVisitor;
import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class that scans jar files and finds calls to a method
 * 
 * @author Rodrigo de Bona Sartor
 */
public class JavaScanner implements Closeable {
	
	private JarFile jarFile;
	
	/**
	 * Create a new JavaScanner with the JarFile containing at <b>jarPath</b>
	 * @param jarPath OS path to the jar file that contains the class
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
	 */
	public JavaScanner(String jarPath) throws IOException {
		this.jarFile = new JarFile(jarPath);
	}

	public HashMap<String, JavaClass> scanAllClasses() throws IOException {
		ScanClassVisitor scanClassVisitor = new ScanClassVisitor();
		Enumeration<JarEntry> entries = this.jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			String name = entry.getName();
			if (name.endsWith(".class") && !name.endsWith("_.class")) {
				InputStream stream = new BufferedInputStream(this.jarFile.getInputStream(entry), 1024);
				ClassReader reader = new ClassReader(stream);

				reader.accept(scanClassVisitor, 0);

				stream.close();
			}
		}
		HashMap<String, JavaClass> classHashMap = scanClassVisitor.getClassMap();
		scanOverrideMethods(classHashMap.values());
		return classHashMap;
	}

	private void scanOverrideMethods(Collection<JavaClass> classes) {
		for (JavaClass javaClass : classes) {
			List<JavaMethod> methods;
			do {
				methods = new ArrayList<>(javaClass.getMethods());
				scanOverrideMethodsClass(methods);
			} while (methods.size() != javaClass.getMethods().size());
		}
	}

	private void scanOverrideMethodsClass(List<JavaMethod> methods) {
		for (JavaMethod javaMethod : methods) {
			List<JavaMethod> methodsOverride = findOverrideMethods(javaMethod.getJavaClass().getInterfacesAndSuperClass(), javaMethod);
			for (JavaMethod javaMethodOverride : methodsOverride) {
				addMethodsOverride(javaMethod, javaMethodOverride.getJavaClass(), javaMethodOverride.getCallers());
			}
		}
	}

	private void addMethodsOverride(JavaMethod javaMethod, JavaClass javaMethodOverrideClass, Set<JavaMethod> callers) {
		for (JavaMethod methodOverrideCaller : callers) {
			if (methodOverrideCaller.getJavaClass().equals(javaMethodOverrideClass)) {
				JavaMethod methodCaller = javaMethod.getJavaClass().addMethod(methodOverrideCaller.getMethodName(), methodOverrideCaller.getMethodDesc());
				javaMethod.getCallers().add(methodCaller);
			}
		}
	}

	private List<JavaMethod> findOverrideMethods(List<JavaClass> javaClasses, JavaMethod javaMethod) {
		List<JavaMethod> methods = new ArrayList<>();
		for(JavaClass javaClass : javaClasses) {
			for (JavaMethod method : javaClass.getMethods()) {
				if (method.getMethodName().equals(javaMethod.getMethodName()) && method.getMethodDesc().equals(javaMethod.getMethodDesc())) {
					methods.add(method);
				}
			}
			methods.addAll(findOverrideMethods(javaClass.getInterfacesAndSuperClass(), javaMethod));
		}
		return methods;
	}

	/**
	 * Closes the jar file
	 * @throws IOException if an I/O error has occurred
	 */
	public void close() throws IOException {
		if( this.jarFile != null ){
			this.jarFile.close();
		}
	}

	
}
