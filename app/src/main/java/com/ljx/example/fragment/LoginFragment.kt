package com.ljx.example.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * User: ljx
 * Date: 2023/6/16
 * Time: 10:54
 */
class LoginFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserFragmentDirections.toLoginFragment()
    }
}
