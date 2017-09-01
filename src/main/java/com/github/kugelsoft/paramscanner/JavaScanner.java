package com.github.kugelsoft.paramscanner;

import com.github.kugelsoft.paramscanner.visitors.ScanClassVisitor;
import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A class that scans jar files and finds calls to a method
 * 
 * @author Rodrigo de Bona Sartor
 */
public class JavaScanner {
	
	private String jarPath;

	/**
	 * Create a new JavaScanner with the JarFile containing at <b>jarPath</b>
	 * @param jarPath OS path to the jar file that contains the class
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
	 */
	public JavaScanner(String jarPath) {
		this.jarPath = jarPath;
	}

	public HashMap<String, JavaClass> scanAllClasses() {
		ScanClassVisitor scanClassVisitor = new ScanClassVisitor();
		return scanAllClasses(scanClassVisitor, this.jarPath);
	}

	private HashMap<String, JavaClass> scanAllClasses(ScanClassVisitor scanClassVisitor, String path) {
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(path);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();

				String name = entry.getName();
				if (name.endsWith(".class") && !name.endsWith("_.class")) {
					InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry), 1024);
					ClassReader reader = new ClassReader(stream);

					reader.accept(scanClassVisitor, 0);

					stream.close();
				} else if (name.startsWith("kugel") && !name.contains("/") && name.endsWith(".jar")) { // escanear dependencias que estão empacotadas em JAR como o kugel-report
					File file = unzipFile(path, name);
					scanAllClasses(scanClassVisitor, file.getAbsolutePath());
					file.delete();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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

	private File unzipFile(String path, String name) {
		int index = name.lastIndexOf(".");
		try {
			File file = File.createTempFile(name.substring(0, index), name.substring(index));
			System.out.println("Unzipping " + path + "\\" + name + " to " + file.getAbsolutePath());
			boolean found = false;

			ZipInputStream zis = new ZipInputStream(new FileInputStream(path));
			ZipEntry ze = zis.getNextEntry();
			while( ze != null ){
				if (ze.getName().equals(name)) {
					found = true;

					FileOutputStream fos = new FileOutputStream(file);
					int len;
					byte[] buffer = new byte[1024 * 5000];
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}

					fos.close();
					break;
				}
				ze = zis.getNextEntry();
			}
			zis.close();

			if (found) {
				System.out.println("Unzip finished");
			} else {
				System.out.println("File not found");
			}

			return file;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
