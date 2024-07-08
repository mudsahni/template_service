package com.pavedroad.templateservice.repository

import com.pavedroad.templateservice.models.GeneratedService
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : MongoRepository<GeneratedService, String> {
    // Additional query methods can be defined here if needed
}
