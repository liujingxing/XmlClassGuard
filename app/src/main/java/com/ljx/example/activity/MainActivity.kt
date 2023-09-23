package com.ljx.example.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ljx.example.IMyAidlInterface
import com.ljx.example.KtTopTest1
import com.ljx.example.R
import com.ljx.example.i
import com.ljx.example.j
import com.ljx.example.k
import com.ljx.example.ktTopFun
import com.ljx.example.m
import com.ljx.example.toB

class MainActivity : AppCompatActivity() {

    val list = mutableListOf<IMyAidlInterface>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ktTopFun()
        val i = i
        val j = j
        val k = k
        val m = m
        m.toB
        KtTopTest1()
    }

    fun test(main: com.ljx.example.activity.MainActivity) {
        Log.d("LJX", main::class.java.name)
    }
}