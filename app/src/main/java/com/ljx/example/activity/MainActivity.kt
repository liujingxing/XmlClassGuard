package com.ljx.example.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ljx.example.R
import com.ljx.example.ktTopFun

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ktTopFun()
    }

    fun test(main: com.ljx.example.activity.MainActivity) {
        Log.d("LJX", main::class.java.name)
    }
}