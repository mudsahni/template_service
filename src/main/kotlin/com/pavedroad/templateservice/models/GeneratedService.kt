package com.pavedroad.templateservice.models

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.util.UUID

@Document(collection="services")
data class GeneratedService (
    @Id
    @Indexed(unique = true)
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val template: String,
    val owner: String,
    val repo: String,
    val costCenter: String
)