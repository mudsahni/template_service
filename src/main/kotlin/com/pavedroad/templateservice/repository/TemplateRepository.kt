package com.pavedroad.templateservice.repository

import com.pavedroad.templateservice.models.Template
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TemplateRepository : MongoRepository<Template, String> {
    // Additional query methods can be defined here if needed
}
