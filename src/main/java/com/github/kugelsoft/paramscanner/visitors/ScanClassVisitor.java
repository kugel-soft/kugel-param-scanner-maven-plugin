package com.github.kugelsoft.paramscanner.visitors;

import com.github.kugelsoft.paramscanner.vo.JavaClass;
import com.github.kugelsoft.paramscanner.vo.JavaMethod;
import org.objectweb.asm.*;

import java.util.HashMap;

public class ScanClassVisitor extends ClassVisitor {

	private final HashMap<String, JavaClass> classMap;

	private JavaClass lastJavaClass;

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
		this.lastJavaClass = javaClass;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		this.methodVisitor.setActualMethodName(name);
		this.methodVisitor.setActualMethodDesc(desc);
		
		return this.methodVisitor;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		String typeClassName = desc.substring(1).replace(";", "");
		if (typeClassName.startsWith("com/kugel/")) {
			JavaClass callerClass = this.methodVisitor.getCallerClass();
			JavaClass typeClass = getJavaClass(typeClassName);
			typeClass.getClassesUsesAsField().add(callerClass);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.contains("javax/ws/rs/Path")) {
			this.lastJavaClass.setTemAnnotationPath(true);
		}
		return super.visitAnnotation(desc, visible);
	}

	class ThisMethodVisitor extends MethodVisitor {
		
		private String callerClassName;
		private String callerMethodName;
		private String callerMethodDesc;
		private JavaClass callerClass;

		ThisMethodVisitor() {
			super(Opcodes.ASM5);
		}

		void setActualClassName(String actualClassName) {
			this.callerClassName = actualClassName;
			this.callerClass = getJavaClass(callerClassName);
		}
		
		void setActualMethodName(String actualMethodName) {
			this.callerMethodName = actualMethodName;
		}
		
		void setActualMethodDesc(String actualMethodDesc) {
			this.callerMethodDesc = actualMethodDesc;
		}
		
		public JavaClass getCallerClass() {
			return callerClass;
		}

		@Override
		public void visitMethodInsn(int opcode, String calleeClassName, String calleeMethodName, String calleeMethodDesc, boolean itf) {
			if (calleeClassName.startsWith("com/kugel/")) {
				JavaClass calleeClass = getJavaClass(calleeClassName);
				JavaMethod calleeMethod = calleeClass.addMethod(calleeMethodName, calleeMethodDesc);

				JavaMethod callerMethod = callerClass.addMethod(callerMethodName, callerMethodDesc);

				calleeMethod.getCallers().add(callerMethod);
			}
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (type.startsWith("com/kugel/domain/param/") && !type.equals("com/kugel/domain/param/Parametro")) {
				JavaClass calleeClass = getJavaClass(type);
				JavaMethod calleeMethod = calleeClass.addMethod("classReference", "");

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
