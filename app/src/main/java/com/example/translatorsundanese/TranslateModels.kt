package com.example.translatorsundanese

data class TranslateData(
    val origin: String,
    val result: String,
    val targets: List<String> = emptyList()
)

data class TranslateResponse(
    val status: Boolean,
    val message: String,
    val data: TranslateData?
)
