package com.example.excitech.view.adapter

import androidx.databinding.BindingAdapter
import android.view.View

object CustomBindingAdapter {
    @BindingAdapter("visibleGone")
    @JvmStatic
    fun showHide(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }
}
