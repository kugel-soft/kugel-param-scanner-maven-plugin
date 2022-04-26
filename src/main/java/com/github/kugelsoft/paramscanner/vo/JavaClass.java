package com.github.kugelsoft.paramscanner.vo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaClass implements Comparable<JavaClass> {

	private JavaClass superClass;
	private List<JavaClass> interfaces;
	private final String name;
	private final Set<JavaMethod> methods;
	private final Set<JavaClass> classesUsesAsField;

	public JavaClass(String name) {
		this.name = name;
		this.methods = new HashSet<>();
		this.interfaces = new ArrayList<>();
		this.classesUsesAsField = new HashSet<>();
	}

	public String getName() {
		return name;
	}

	public Set<JavaMethod> getMethods() {
		return methods;
	}

	public JavaClass getSuperClass() {
		return superClass;
	}

	public void setSuperClass(JavaClass superClass) {
		this.superClass = superClass;
	}

	public List<JavaClass> getInterfaces() {
		return interfaces;
	}

	public List<JavaClass> getInterfacesAndSuperClass() {
		List<JavaClass> classList = new ArrayList<>(interfaces);
		if (superClass != null) {
			classList.add(superClass);
		}
		return classList;
	}

	public JavaMethod addMethod(String methodName, String methodDesc) {
		JavaMethod javaMethod = new JavaMethod(this, methodName, methodDesc);
		boolean existe = false;
		for(JavaMethod method : methods) {
			if(method.equals(javaMethod)) {
				javaMethod = method;
				existe = true;
				break;
			}
		}
		if (!existe) {
			methods.add(javaMethod);
		}
		return javaMethod;
	}

	public String getSimpleClassName() {
		String simpleName = this.name;
		int index = simpleName.lastIndexOf("/");
		if( index >= 0 ){
			simpleName = simpleName.substring( index + 1 );
		}
		return simpleName;
	}

	public String getPackageName() {
		String packageName = "";
		int index = this.name.lastIndexOf("/");
		if( index >= 0 ){
			packageName = this.name.substring( 0, index ).replace("/", ".");
		}
		return packageName;
	}

	public Set<JavaClass> getClassesUsesAsField() {
		return classesUsesAsField;
	}

	@Override
	public int compareTo(JavaClass o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JavaClass)) return false;

		JavaClass javaClass = (JavaClass) o;

		return name != null ? name.equals(javaClass.name) : javaClass.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "JavaClass{" +
				"name='" + name + '\'' +
				", methods=" + methods +
				'}';
	}
}
