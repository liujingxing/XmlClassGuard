package com.xml.guard.transform;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * User: ljx
 * Date: 2023/7/18
 * Time: 18:06
 */
public class MyClassWriter extends ClassWriter {
    public MyClassWriter() {
        super(ClassWriter.COMPUTE_MAXS);
    }

    public void generateClass() {
        visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/DynamicClass", null, "java/lang/Object", null);

        // 添加字段
        visitField(Opcodes.ACC_PRIVATE, "name", "Ljava/lang/String;", null, null).visitEnd();

        // 添加构造方法
        MethodVisitor constructorVisitor = visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructorVisitor.visitCode();
        constructorVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        constructorVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructorVisitor.visitInsn(Opcodes.RETURN);
        constructorVisitor.visitMaxs(0, 0);
        constructorVisitor.visitEnd();

        // 添加setName方法
        MethodVisitor setNameVisitor = visitMethod(Opcodes.ACC_PUBLIC, "setName", "(Ljava/lang/String;)V", null, null);
        setNameVisitor.visitCode();
        setNameVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        setNameVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        setNameVisitor.visitFieldInsn(Opcodes.PUTFIELD, "com/example/DynamicClass", "name", "Ljava/lang/String;");
        setNameVisitor.visitInsn(Opcodes.RETURN);
        setNameVisitor.visitMaxs(0, 0);
        setNameVisitor.visitEnd();

        // 添加getName方法
        MethodVisitor getNameVisitor = visitMethod(Opcodes.ACC_PUBLIC, "getName", "()Ljava/lang/String;", null, null);
        getNameVisitor.visitCode();
        getNameVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        getNameVisitor.visitFieldInsn(Opcodes.GETFIELD, "com/example/DynamicClass", "name", "Ljava/lang/String;");
        getNameVisitor.visitInsn(Opcodes.ARETURN);
        getNameVisitor.visitMaxs(0, 0);
        getNameVisitor.visitEnd();

        visitEnd();
    }
}

