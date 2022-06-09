package com.astar.smartsocket.core

import android.content.Context
import androidx.annotation.StringRes

interface ResourceProvider {

    fun string(@StringRes resId: Int): String

    fun string(@StringRes resId: Int, vararg params: Any): String

    class Base(private val context: Context) : ResourceProvider {

        override fun string(resId: Int): String = context.getString(resId)

        override fun string(resId: Int, vararg params: Any) = context.getString(resId, params)
    }
}