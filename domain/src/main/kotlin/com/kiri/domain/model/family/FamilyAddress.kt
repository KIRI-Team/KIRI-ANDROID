package com.kiri.domain.model.family

data class FamilyAddress(val postalCode: String, val address: String, val detailAddress: String,
    val regionCode: String, val latitude: Double, val longitude: Double) {
    fun validate() {
        require(postalCode.matches(Regex("[0-9]{5}")) && address.isNotBlank() && regionCode.matches(Regex("[0-9]{10}")))
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    }
}
