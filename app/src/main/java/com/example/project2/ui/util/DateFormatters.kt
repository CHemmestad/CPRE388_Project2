package com.example.project2.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM/dd/yyyy HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

fun Instant.formatAsDisplay(): String = displayFormatter.format(this)
