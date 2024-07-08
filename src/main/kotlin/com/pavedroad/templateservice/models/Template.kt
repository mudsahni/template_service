package com.pavedroad.templateservice.models

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*

@Document(collection="templates")
data class Template (
    @Id
    @Indexed(unique = true)
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var description: String,
    val author: String,
    val organization: String,
    val repo: String,
    var tags: List<String>,
    var healthy: Boolean,
    var configs: Map<String, String>
)