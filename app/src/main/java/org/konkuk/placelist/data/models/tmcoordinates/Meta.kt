package org.konkuk.placelist.data.models.tmcoordinates


import com.google.gson.annotations.SerializedName

data class Meta(
    @SerializedName("total_count")
    val totalCount: Int?
)