package com.ritense.valtimoplugins.xential.repository

import com.ritense.valtimoplugins.xential.domain.XentialToken
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface XentialTokenRepository : CrudRepository<XentialToken, UUID>
