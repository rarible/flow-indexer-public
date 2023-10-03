package com.rarible.flow.api.controller.internal

import com.rarible.flow.api.service.AdminService
import com.rarible.flow.core.domain.ItemId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminItemController(val adminService: AdminService) {

    @DeleteMapping(
        value = ["/admin/items/{itemId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun deleteItemById(
        @PathVariable("itemId") itemId: String
    ): ResponseEntity<Void> {
        adminService.deleteItemById(ItemId.parse(itemId))
        return ResponseEntity.noContent().build()
    }
}
