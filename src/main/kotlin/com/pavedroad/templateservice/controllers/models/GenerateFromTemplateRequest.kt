package com.pavedroad.templateservice.controllers.models

data class GenerateFromTemplateRequest(
    val repoOwner: String,
    val repoName: String,
    val newOwner: String,
    val newRepoName: String,
    val config: Map<String, String>,
    val templateId: String
)