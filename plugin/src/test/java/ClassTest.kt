import com.xml.guard.transform.MyClassWriter
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File

/**
 * User: ljx
 * Date: 2023/7/18
 * Time: 15:33
 */
class ClassTest {

    @Test
    fun test() {
        val writer = ClassWriter(0)
        writer.visit(
            Opcodes.V1_7,
            Opcodes.ACC_PUBLIC,
            "com/example/test/TestData",
            null,
            "java/lang/Object",
            null
        )
        writer.visitField(Opcodes.ACC_PUBLIC, "valInt", Type.INT_TYPE.descriptor, null, 0)
        writer.visitField(
            Opcodes.ACC_PUBLIC, "valStr", Type.getDescriptor(
                String::class.java
            ), null, null
        )
        writer.visitEnd()
        val classBytes = writer.toByteArray()
        File("TestData.class").writeBytes(classBytes)
    }

    @Test
    fun test1() {
        val myClassWriter = MyClassWriter()
        myClassWriter.generateClass()
        File("DynamicClass.class").writeBytes(myClassWriter.toByteArray())
    }
}