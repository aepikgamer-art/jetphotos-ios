package dev.jetphotos.android

import com.google.gson.annotations.SerializedName

data class JetApiResponse(
    @SerializedName("Reg") val registration: String = "",
    @SerializedName("Images") val images: List<JetPhoto> = emptyList()
)

data class JetPhoto(
    @SerializedName("Image") val image: String = "",
    @SerializedName("Link") val link: String = "",
    @SerializedName("Thumbnail") val thumbnail: String = "",
    @SerializedName("DateTaken") val dateTaken: String = "",
    @SerializedName("DateUploaded") val dateUploaded: String = "",
    @SerializedName("Location") val location: String = "",
    @SerializedName("Photographer") val photographer: String = "",
    @SerializedName("Aircraft") val aircraft: String = "",
    @SerializedName("Serial") val serial: String = "",
    @SerializedName("Airline") val airline: String = ""
) {
    val thumbnailUrl: String
        get() = if (thumbnail.startsWith("//")) "https:$thumbnail" else thumbnail
}
