package com.apps.aggr.qrcodescanner.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Prueba(
        val nombre: String,
        val apellido: String
): Parcelable

@Parcelize
data class FamiliaLinea(
        val linea : String,
        val familia : String
): Parcelable
