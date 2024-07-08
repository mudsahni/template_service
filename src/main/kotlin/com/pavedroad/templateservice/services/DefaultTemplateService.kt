package com.pavedroad.templateservice.services

import org.springframework.boot.web.client.RestTemplateBuilder
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pavedroad.templateservice.controllers.models.CreateTemplateResponse
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateRequest
import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateResponse
import com.pavedroad.templateservice.logging.LogEvent
import com.pavedroad.templateservice.models.GeneratedService
import com.pavedroad.templateservice.models.Template
import com.pavedroad.templateservice.repository.ServiceRepository
import com.pavedroad.templateservice.repository.TemplateRepository
import jakarta.annotation.PostConstruct
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.kohsuke.github.GitHub
import org.kohsuke.github.HttpException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Optional
import java.util.concurrent.CompletableFuture

@Service
class DefaultTemplateService(
    @Value("\${github.username}")
    override val githubUsername: String,
    @Value("\${github.token}")
    override val githubToken: String,
    @Value("\${gcp.service-account-json}")
    override val gcpConfig: String,
    @Autowired override val templateRepository: TemplateRepository,
    @Autowired override val serviceRepository: ServiceRepository,
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${python.venv}")
    val pythonVenv: String,
    @Value("\${encrypt.script}")
    val encryptionScriptPath: String,
    val resourceLoader: DefaultResourceLoader,
    @Autowired val sseService: SSEService
) : TemplateService {
    private lateinit var github: GitHub
    val COOKIECUTTER_JSON_FILE_PATH = "cookiecutter.json"

    private val restTemplate: RestTemplate = restTemplateBuilder.build()
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
    @PostConstruct
    fun init() {
        github = GitHub.connectUsingOAuth(githubToken)
    }

    fun logEvent(level: String, message: String) {
        log.info(message)
        val logEvent = LogEvent(
            timestamp = System.currentTimeMillis(),
            level = level,
            logger = this::class.java.name,
            message = message
        )
        try {
            sseService.sendEvent("template-service", logEvent)
        } catch (e: IOException) {
            log.error("Error sending event. ${e}")
        }
    }

    fun readJsonFile(): String {
        val resource = resourceLoader.getResource("classpath:secret.json")
        resource.inputStream.use { inputStream ->
            return BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
        }
    }

    override fun getTemplateConfigs(id: String): Map<String, String> {
        sseService.sendEvent("", LogEvent(System.currentTimeMillis(), "crap", "crapper", "crappy"))
        return templateRepository.findById(id).get().configs
    }
    override fun saveTemplate(template: Template): CreateTemplateResponse {
        return try {
            val templateRepo = github.getRepository("${template.organization}/${template.repo}")
            val fileContent = templateRepo.getFileContent(COOKIECUTTER_JSON_FILE_PATH)
            val jsonContent = InputStreamReader(fileContent.read()).readText()
            val mapper = jacksonObjectMapper()
            val typeRef = object : TypeReference<Map<String, String>>() {}
            val configs = mapper.readValue(jsonContent, typeRef)
            template.configs = configs
            val savedTemplate = templateRepository.save(template)
            CreateTemplateResponse(true, "Template has been saved. Template id=${savedTemplate.id}")
        } catch (e: HttpException) {
            if (e.responseCode == 404) {
                log.error("Repo was not found.")
               CreateTemplateResponse(false, "Repo was not found.")
            } else {
                log.error("Error fetching ${COOKIECUTTER_JSON_FILE_PATH} file: ${e.message}")
                CreateTemplateResponse(false, "Template was not saved. Error fetching ${COOKIECUTTER_JSON_FILE_PATH} file. e=${e.message}")
            }
        } catch (e: Exception) {
            log.error("Error: ${e.message}")
            CreateTemplateResponse(false, "Template was not saved. e=${e.message}")
        }

}
    @Async
    override fun generateFromTemplate(templateRequest: GenerateFromTemplateRequest): CompletableFuture<GenerateFromTemplateResponse> {
        return CompletableFuture.supplyAsync {
            try {
                // Define the base output directory
                val baseOutputDir = "./"

                // Run cookiecutter on the cloned repo
                val res = runCookieCutterCommand(templateRequest, baseOutputDir)
                if (!res) {
                    val projectGenerationFailureMessage = "Project was not successfully created."
                    logEvent("error", projectGenerationFailureMessage)
                    throw Exception(projectGenerationFailureMessage)
                }
                val projectGeneratedMessage = "Successfully generated a project from the template."
                logEvent("info", projectGeneratedMessage)
                val newRepo = github.createRepository(templateRequest.newRepoName)
                    .description("Copy of ${templateRequest.repoOwner}/${templateRequest.repoName}")
                    .create()

                // Initialize a Git repository and push to GitHub
                val projectDir = File("./${templateRequest.newRepoName}")
                Git.init().setDirectory(projectDir).call().use { git ->
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage("Initial commit").call()
                    git.remoteAdd().setName("origin").setUri(URIish(newRepo.httpTransportUrl)).call()
                    git.push().setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(githubUsername, githubToken)
                    ).call()
                }
                val pushedToGithubMessage = "Successfully pushed the project to GitHub."
                log.info(pushedToGithubMessage)
                logEvent("info", pushedToGithubMessage)

                val service = serviceRepository.save(
                    GeneratedService(
                        name = templateRequest.newRepoName,
                        template = templateRequest.templateId,
                        owner = templateRequest.newOwner,
                        repo = newRepo.htmlUrl.toString(),
                        costCenter = "1000"
                    )
                )
                log.info("Successfully saved the generated service details.")
                logEvent("info", "Successfully saved the generated service details.")
                logEvent("info", "ProjectId: ${service.id}")

                // Delete the generated project directory
                deleteDirectoryRecursively(projectDir)
                log.info("Successfully deleted the generated project directory.")
                logEvent("debug", "Successfully deleted the generated project directory.")

                val gcpConfigJson = readJsonFile()

                try {
                    createGitHubSecret(
                        newRepo.ownerName,
                        newRepo.name,
                        "GCP_SERVICE_ACCOUNT_CONFIG",
                        gcpConfigJson,
                        githubToken
                    )
                } catch (e: Exception) {
                    log.error("Failed creating github secret for gcp. e: $e")
                }

                try {
                    createGitHubSecret(
                        newRepo.ownerName,
                        newRepo.name,
                        "PAT",
                        githubToken,
                        githubToken
                    )
                } catch (e: Exception) {
                    log.error("Failed creating github secret for pat. e: $e")
                }


                logEvent("info", "Github Repository: ${newRepo.htmlUrl}")
                logEvent("info", "Project generation is complete.")
                GenerateFromTemplateResponse(
                    true,
                    "Repository copied and modified: ${newRepo.htmlUrl}",
                    service.id
                )
            } catch (e: Exception) {
                logEvent("error","Generation from template failed. e: ${e.message}")
                GenerateFromTemplateResponse(false, "Error: ${e.message}", null)
            }
        }
    }

    override fun getAllTemplates(): List<Template> {
       return templateRepository.findAll()
    }

    override fun getAllServices(): List<GeneratedService> {
        return serviceRepository.findAll()
    }

    override fun getServiceById(id: String): Optional<GeneratedService> {
        return serviceRepository.findById(id)
    }

    fun createGitHubSecret(owner: String, repo: String, secretName: String, secretValue: String, token: String) {
        val (publicKey, keyId) = getPublicKey(owner, repo, token)
        val encryptedSecret = runPythonEncryptionScript(pythonVenv, encryptionScriptPath, secretValue, publicKey)
        val url = "https://api.github.com/repos/$owner/$repo/actions/secrets/$secretName"

        val headers = HttpHeaders().apply {
            set("Accept", "application/vnd.github+json")
            set("Authorization", "Bearer ${token}")
            set("X-GitHub-Api-Version", "2022-11-28")
        }

        val body = mapOf(
            "encrypted_value" to encryptedSecret,
            "key_id" to keyId
        )

        val request = HttpEntity(body, headers)

        val response = restTemplate.exchange(url, HttpMethod.PUT, request, String::class.java)

        log.info(response.body)
    }
    fun getPublicKey(owner: String, repo: String, token: String): Pair<String, String> {
        val url = "https://api.github.com/repos/$owner/$repo/actions/secrets/public-key"
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${token}")
            set("Accept", "application/vnd.github+json")
            set("X-GitHub-Api-Version", "2022-11-28")
        }

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange(url, HttpMethod.GET, request, Map::class.java)
        if (response.body != null) {
            log.info("Public key has been fetched. ${response.body?.keys}")
        } else {
            log.error("Response body is null.")
        }
        return Pair(
            response.body?.get("key") as String,
            response.body?.get("key_id") as String
        )
    }

    override fun triggerGithubAction(service: GeneratedService) {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${githubToken}")
            set("Accept", "application/vnd.github+json")
            set("X-GitHub-Api-Version", "2022-11-28")
        }
        val body = mapOf(
            "ref" to "master"
        )

        val url = "https://api.github.com/repos/${service.owner}/${service.name}/actions/workflows/deploy.yml/dispatches"
        log.info("Running the trigger for this url :$url")
        val request = HttpEntity(body, headers)

        val response = restTemplate.exchange(url, HttpMethod.POST, request, String::class.java)
        log.info(response.body)

    }

}

