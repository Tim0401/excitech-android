package com.example.excitech.service.model

import java.util.*

data class Audio(
        val name: String,
        val path: String,
        val durationMs: Long,
        val durationText: String,
        val lastModified: String
)