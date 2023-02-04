package com.xml.guard.utils

/**
 * User: ljx
 * Date: 2023/2/4
 * Time: 20:16
 */
object AgpVersion {

    val agpVersion: String? by lazy {
        var version: String? = null
        try {
            val clazz = Class.forName("com.android.builder.Version")
            val field = clazz.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            field.setAccessible(true)
            version = field.get(null) as String
        } catch (ignore: ClassNotFoundException) {
        } catch (ignore: NoSuchFieldException) {
        }
        if (version == null) {
            try {
                val clazz = Class.forName("com.android.builder.model.Version")
                val field = clazz.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
                field.setAccessible(true)
                version = field.get(null) as String
            } catch (ignore: ClassNotFoundException) {
            } catch (ignore: NoSuchFieldException) {
            }
        }
        version
    }

    //agp版本比较，当前版本大于version2，返回 >0; 等于，返回=0; 否则，返回 <0
    fun versionCompare(version2: String): Int {
        val agpVersion = agpVersion ?: return -1
        return versionCompare(agpVersion, version2)
    }

    private fun versionCompare(version1: String, version2: String): Int {
        val versionArr1 = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val versionArr2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val minLen = Math.min(versionArr1.size, versionArr2.size)
        var diff = 0
        for (i in 0 until minLen) {
            val v1 = versionArr1[i]
            val v2 = versionArr2[i]
            diff = v1.length - v2.length
            if (diff == 0) {
                diff = v1.compareTo(v2)
            }
            if (diff != 0) {
                break
            }
        }
        diff = if (diff != 0) diff else versionArr1.size - versionArr2.size
        return diff
    }
}