package com.github.kugelsoft.paramscanner.vo;

import java.util.HashSet;
import java.util.Objects;
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
		return Objects.hash(javaClass.getName(), methodName, methodDesc);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != JavaMethod.class) {
			return false;
		}
		JavaMethod other = (JavaMethod) obj;
		return Objects.equals(javaClass.getName(), other.javaClass.getName()) &&
				Objects.equals(methodName, other.methodName) &&
				Objects.equals(methodDesc, other.methodDesc);
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
