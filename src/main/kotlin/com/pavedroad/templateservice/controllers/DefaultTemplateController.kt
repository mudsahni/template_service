package com.pavedroad.templateservice.controllers

import com.pavedroad.templateservice.controllers.models.*
import com.pavedroad.templateservice.logging.LogEvent
import com.pavedroad.templateservice.models.GeneratedService
import com.pavedroad.templateservice.models.Template
import com.pavedroad.templateservice.services.SSEService
import com.pavedroad.templateservice.services.TemplateService
import org.apache.coyote.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultTemplateController(
    @Autowired override val templateService: TemplateService,
    @Autowired val sseService: SSEService
): TemplateController {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping("/create")
    override fun createTemplate(
        @RequestBody createTemplateRequest: CreateTemplateRequest
    ): ResponseEntity<CreateTemplateResponse> {
        val response = templateService.saveTemplate(createTemplateRequest.toTemplate())
        return if (response.success) {
            ResponseEntity(response, HttpStatus.CREATED)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }


    @PostMapping("/generate")
    override fun generateFromTemplate(
        @RequestBody generateFromTemplateRequest: GenerateFromTemplateRequest
    ): ResponseEntity<Unit> {
        log.info("Generating repository....")
        sseService.sendEvent(
            "template-service",
            LogEvent(
                System.currentTimeMillis(),
                "info",
                "TemplateService",
                "Starting project generation...."
            )
        )
        templateService.generateFromTemplate(generateFromTemplateRequest)
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/template")
    override fun getAllTemplates(): ResponseEntity<List<Template>> {
        return ResponseEntity(templateService.getAllTemplates(), HttpStatus.OK)
    }

    @GetMapping("/service")
    override fun getAllServices(): ResponseEntity<List<GeneratedService>> {
        return ResponseEntity(templateService.getAllServices(), HttpStatus.OK)
    }

    @GetMapping("/config")
    override fun getTemplateConfigs(@RequestParam("templateId") templateId: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity(templateService.getTemplateConfigs(templateId), HttpStatus.OK)
    }

    @GetMapping("/service/{id}")
    override fun getServiceById(@PathVariable id: String): ResponseEntity<GeneratedService> {
        val service = templateService.getServiceById(id)
        return if (service.isPresent) {
            ResponseEntity.ok(service.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/service/{id}/trigger")
    fun triggerServiceGithubAction(@PathVariable id: String): ResponseEntity<Unit> {
        val service = templateService.getServiceById(id)
        if (service.isEmpty) {
            sseService.sendEvent("template-service", LogEvent(
                System.currentTimeMillis(),
                "error",
                "TemplateService",
                "Project with ${id} not found."
            ))
            return ResponseEntity.notFound().build()
        } else {
            templateService.triggerGithubAction(service.get())
            sseService.sendEvent("template-service", LogEvent(
                System.currentTimeMillis(),
                "info",
                "TemplateService",
                "Publishing triggered."
            ))

            return ResponseEntity.accepted().build()
        }
    }
}