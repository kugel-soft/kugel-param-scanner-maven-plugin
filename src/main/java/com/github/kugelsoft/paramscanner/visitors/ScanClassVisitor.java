package com.github.kugelsoft.paramscanner.visitors;

import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class ScanClassVisitor extends ClassVisitor {

	private final HashMap<String, JavaClass> classMap;

	private final ThisMethodVisitor methodVisitor;

	public ScanClassVisitor() {
		super(Opcodes.ASM5);
		this.classMap = new HashMap<>();
		this.methodVisitor = new ThisMethodVisitor();
	}

	public HashMap<String, JavaClass> getClassMap() {
		return classMap;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.methodVisitor.setActualClassName(name);
		JavaClass javaClass = getJavaClass(name);
		if (superName != null && !superName.equals("")) {
			JavaClass superClass = getJavaClass(superName);
			javaClass.setSuperClass(superClass);
		}
		for (String interf : interfaces) {
			JavaClass interfClass = getJavaClass(interf);
			javaClass.getInterfaces().add(interfClass);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		this.methodVisitor.setActualMethodName(name);
		this.methodVisitor.setActualMethodDesc(desc);
		
		return this.methodVisitor;
	}
	
	public Set<JavaMethod> getCallers() {
		return this.methodVisitor.getCallers();
	}

	class ThisMethodVisitor extends MethodVisitor {
		
		private Set<JavaMethod> callers;

		private String callerClassName;
		private String callerMethodName;
		private String callerMethodDesc;
		
		ThisMethodVisitor() {
			super(Opcodes.ASM5);
			this.callers = new TreeSet<>();
		}

		void setActualClassName(String actualClassName) {
			this.callerClassName = actualClassName;
		}
		
		void setActualMethodName(String actualMethodName) {
			this.callerMethodName = actualMethodName;
		}
		
		void setActualMethodDesc(String actualMethodDesc) {
			this.callerMethodDesc = actualMethodDesc;
		}
		
		Set<JavaMethod> getCallers() {
			return callers;
		}
		
		@Override
		public void visitMethodInsn(int opcode, String calleeClassName, String calleeMethodName, String calleeMethodDesc, boolean itf) {
			if (calleeClassName.startsWith("com/kugel/")) {
				JavaClass calleeClass = getJavaClass(calleeClassName);
				JavaMethod calleeMethod = calleeClass.addMethod(calleeMethodName, calleeMethodDesc);

				JavaClass callerClass = getJavaClass(callerClassName);
				JavaMethod callerMethod = callerClass.addMethod(callerMethodName, callerMethodDesc);

				calleeMethod.getCallers().add(callerMethod);
			}
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (type.startsWith("com/kugel/domain/param/") && !type.equals("com/kugel/domain/param/Parametro")) {
				JavaClass calleeClass = getJavaClass(type);
				JavaMethod calleeMethod = calleeClass.addMethod("classReference", "");

				JavaClass callerClass = getJavaClass(callerClassName);
				JavaMethod callerMethod = callerClass.addMethod(callerMethodName, callerMethodDesc);

				calleeMethod.getCallers().add(callerMethod);
			}
		}
	}

	private JavaClass getJavaClass(String calleeClassName) {
		JavaClass javaClass = this.classMap.get(calleeClassName);
		if (javaClass == null) {
			javaClass = new JavaClass(calleeClassName);
			this.classMap.put(calleeClassName, javaClass);
		}
		return javaClass;
	}
}
