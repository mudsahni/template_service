package com.pavedroad.templateservice.controllers.models

data class GenerateFromTemplateResponse (
    val success: Boolean,
    val message: String,
    val serviceId: String?
)