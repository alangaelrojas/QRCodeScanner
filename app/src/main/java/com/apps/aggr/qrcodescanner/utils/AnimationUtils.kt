package com.apps.aggr.qrcodescanner.utils

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import com.apps.aggr.qrcodescanner.R

fun View.animateUp(context: Context){
    val animation = AnimationUtils.loadAnimation(context, R.anim.move_top)
    this.startAnimation(animation)
}