package com.pavedroad.templateservice.services

import com.pavedroad.templateservice.controllers.models.CreateTemplateResponse
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateRequest
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateResponse
import com.pavedroad.templateservice.models.GeneratedService
import com.pavedroad.templateservice.models.Template
import com.pavedroad.templateservice.repository.ServiceRepository
import com.pavedroad.templateservice.repository.TemplateRepository
import org.springframework.scheduling.annotation.Async
import java.util.*
import java.util.concurrent.CompletableFuture

interface TemplateService {

    val githubUsername: String
    val githubToken: String
    val gcpConfig: String
    val templateRepository: TemplateRepository
    val serviceRepository: ServiceRepository

    fun saveTemplate(template: Template): CreateTemplateResponse
    fun generateFromTemplate(templateRequest: GenerateFromTemplateRequest): CompletableFuture<GenerateFromTemplateResponse>
    fun getAllTemplates(): List<Template>
    fun getAllServices(): List<GeneratedService>
    fun getTemplateConfigs(id: String): Map<String, String>
    fun getServiceById(id: String): Optional<GeneratedService>
    fun triggerGithubAction(service: GeneratedService)
}