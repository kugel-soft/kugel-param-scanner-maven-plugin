package com.github.kugelsoft.paramscanner.vo;

import java.util.HashSet;
import java.util.Set;

public class JavaMethod implements Comparable<JavaMethod> {
	
	private final JavaClass javaClass;
	private final String methodName;
	private final String methodDesc;
	private final Set<JavaMethod> callers;

	JavaMethod(JavaClass javaClass, String methodName, String methodDesc) {
		this.javaClass = javaClass;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.callers = new HashSet<>();
	}

	public static JavaMethod emptyMethod(JavaClass javaClass) {
		return new JavaMethod(javaClass, "", "");
	}

	public JavaClass getJavaClass() {
		return javaClass;
	}

	public Set<JavaMethod> getCallers() {
		return callers;
	}

	public String getMethodDesc() {
		return methodDesc;
	}
	
	public String getMethodName() {
		return methodName;
	}

	@Override
	public int hashCode() {
		return (javaClass.getName() + "." + methodName + methodDesc).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == JavaMethod.class && obj.hashCode() == this.hashCode();
	}

	public int compareTo(JavaMethod o) {
		int comp = this.javaClass.compareTo(o.javaClass);
		if( comp == 0 ){
			comp = this.methodName.compareTo(o.methodName);
			if( comp == 0 ){
				comp = this.methodDesc.compareTo(o.methodDesc);
			}
		}
		return comp;
	}

	@Override
	public String toString() {
		return "JavaMethod{" +
				" class='" + getJavaClass().getSimpleClassName() + '\'' +
				", methodName='" + methodName + '\'' +
				", methodDesc='" + methodDesc + '\'' +
				'}';
	}
}
