package com.adsamcik.tracker.shared.base.extension

fun Double.toPercent(): Double = this * ONE_TO_PERCENTAGE

fun Double.toIntPercent(): Int = this.toPercent().toInt()

private const val ONE_TO_PERCENTAGE = 100.0
