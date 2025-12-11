package com.example.project2.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM/dd/yyyy HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

/**
 * Format an [Instant] for UI display using the device locale and timezone.
 *
 * @return formatted date/time string
 */
fun Instant.formatAsDisplay(): String = displayFormatter.format(this)
