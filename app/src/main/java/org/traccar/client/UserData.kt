package org.traccar.client

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id") val id: Long,
    @SerializedName("phone") val phone: String?,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("password") val password: String?,
    var token: String? = null // Add mutable token field
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(phone)
        parcel.writeString(firstName)
        parcel.writeString(lastName)
        parcel.writeString(password)
        parcel.writeString(token)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UserData> {
        override fun createFromParcel(parcel: Parcel): UserData = UserData(parcel)
        override fun newArray(size: Int): Array<UserData?> = arrayOfNulls(size)
    }
}