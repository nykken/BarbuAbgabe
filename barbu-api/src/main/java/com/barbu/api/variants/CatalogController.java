package com.barbu.api.variants;

import com.barbu.catalog.GameCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "Variants", description = "Read-only catalog of available game variants and their rules.")
@RestController
@RequestMapping("/api/variants")
public class CatalogController {
    @Operation(summary = "List available variants", description = "Returns all variants with their contracts and deck configuration.")
    @GetMapping
    public List<VariantInfoDTO> getVariants() {
        return GameCatalog.all().stream()
                .map(VariantMapper::mapVariant)
                .toList();
    }
}


