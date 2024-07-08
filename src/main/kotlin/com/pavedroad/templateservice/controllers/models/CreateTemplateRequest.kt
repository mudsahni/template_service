package com.pavedroad.templateservice.controllers.models

import com.pavedroad.templateservice.models.Template

data class CreateTemplateRequest (
    val name: String,
    val tags: List<String>,
    val healthy: Boolean = true,
    val description: String,
    val repo: String,
    val organization: String,
    val author: String,
)

fun CreateTemplateRequest.toTemplate(): Template {
    return Template(
        name = name,
        tags = tags,
        healthy = healthy,
        description = description,
        repo = repo,
        organization = organization,
        author = author,
        configs = mapOf<String, String>()
    )
}