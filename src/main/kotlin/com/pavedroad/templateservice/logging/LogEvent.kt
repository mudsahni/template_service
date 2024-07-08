package com.pavedroad.templateservice.logging

data class LogEvent(
    val timestamp: Long,
    val level: String,
    val logger: String,
    val message: String
)