package com.xml.guard.transform

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class StringFogTransform : AsmClassVisitorFactory<InstrumentationParameters.None> {


    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {

        return object : ClassVisitor(Opcodes.ASM7, nextClassVisitor) {


            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                println("visit version=$version access=$access name=$name signature=$signature superName=$superName interfaces=$interfaces")
                super.visit(version, access, name, signature, superName, interfaces)
            }


            override fun visitField(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                value: Any?
            ): FieldVisitor? {
                println("visitField access=$access name=$name descriptor=$descriptor signature=$signature value=$value")
                val visitField = super.visitField(access, name, descriptor, signature, "haha")
                println("visitField=$")
                return visitField
            }

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                println("visitMethod access=$access name=$name descriptor=$descriptor signature=$signature exceptions=$exceptions")
                val vm = super.visitMethod(access, name, descriptor, signature, exceptions)

//                if (name == "test") {
//                    return object :MethodVisitor(Opcodes.ASM7, vm){
//
//                        override fun visitCode() {
//                            super.visitCode()
//                            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
//                            mv.visitLdcInsn("Entering method test")
//                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
//                        }
//
//
//                        override fun visitInsn(opcode:Int) {
//                            if (opcode == Opcodes.RETURN) {
//                                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
//                                mv.visitLdcInsn("Exiting method test")
//                                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
//                            }
//                            super.visitInsn(opcode)
//                        }
//                    }
//                }
                return vm
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return  classData.className == "com.ljx.example.entity.Book"
    }
}