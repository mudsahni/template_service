package com.pavedroad.templateservice.services

import com.pavedroad.templateservice.controllers.models.GenerateFromTemplateRequest
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit


private val log = LoggerFactory.getLogger("CommandExtensions")

// Function to delete a directory recursively
fun deleteDirectoryRecursively(directory: File) {
    if (directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            deleteDirectoryRecursively(file)
        }
    }
    directory.delete()
}

fun String.runCommand(): Boolean {
    log.info("This is the command: ${this}")
    return try {
        val process = Runtime.getRuntime().exec(this)
        // Capture standard output
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))

        // Log standard output
        var s: String?
        while (stdInput.readLine().also { s = it } != null) {
            log.info("Standard output: $s")
        }

        // Log errors
        while (stdError.readLine().also { s = it } != null) {
            log.error("Error output: $s")
        }


        val exitCode = process.waitFor()
        log.info("Command exit code: $exitCode")
        exitCode == 0
    } catch (e: IOException) {
        log.error("Error executing command: ${e.message}")
        false
    } catch (e: InterruptedException) {
        log.error("Command execution interrupted: ${e.message}")
        false
    }
}

fun runCookieCutterCommand(templateRequest: GenerateFromTemplateRequest, outputDir: String): Boolean {
    val configs = templateRequest.config.entries.joinToString(" ") { "${it.key}=${it.value}" }
    val cookiecutterCommand =
        "cookiecutter https://github.com/${templateRequest.repoOwner}/${templateRequest.repoName} " +
                "--no-input " +
                configs + " " +
//            "group_id=${templateRequest.config.groupId} " +
//            "artifact_id=${templateRequest.config.artifactId} " +
//            "artifact_id_slug=${templateRequest.config.artifactId} " +
//            "project_description=${templateRequest.config.projectDescription} " +
//            "author_name=${templateRequest.config.authorName} " +
                "output_dir=${outputDir} "
    return cookiecutterCommand.runCommand()
}

fun runPythonEncryptionScript(pythonVenv: String, scriptPath: String, secret: String, key: String): String {
    val command = mutableListOf(
        "$pythonVenv/bin/python3",  // Use the Python from the virtual environment
        scriptPath
    )
    command.add(key)
    command.add(secret)

    val processBuilder = ProcessBuilder(command)
    processBuilder.redirectErrorStream(true)

    // Set the working directory if needed
    // processBuilder.directory(File("/path/to/working/directory"))

    val process = processBuilder.start()

    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val output = StringBuilder()

    var line: String?
    while (reader.readLine().also { line = it } != null) {
        output.append(line)
    }

    if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroy()
        throw RuntimeException("Execution timed out")
    }

    return output.toString()
}