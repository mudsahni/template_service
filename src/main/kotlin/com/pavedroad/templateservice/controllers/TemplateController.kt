package com.pavedroad.templateservice.controllers

import com.pavedroad.templateservice.controllers.models.CreateTemplateRequest
import com.pavedroad.templateservice.controllers.models.CreateTemplateResponse
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateRequest
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateResponse
import com.pavedroad.templateservice.models.GeneratedService
import com.pavedroad.templateservice.models.Template
import com.pavedroad.templateservice.services.TemplateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1")
interface TemplateController {

    val templateService: TemplateService
    fun createTemplate(createTemplateRequest: CreateTemplateRequest): ResponseEntity<CreateTemplateResponse>
    fun generateFromTemplate(generateFromTemplateRequest: GenerateFromTemplateRequest): ResponseEntity<Unit>
    fun getAllTemplates(): ResponseEntity<List<Template>>
    fun getAllServices(): ResponseEntity<List<GeneratedService>>
    fun getTemplateConfigs(id: String): ResponseEntity<Map<String, String>>
    fun getServiceById(id: String): ResponseEntity<GeneratedService>
}