package com.wiily.pscosmeticos.PsAPI.controller;

import com.wiily.pscosmeticos.PsAPI.domain.category.*;
import com.wiily.pscosmeticos.PsAPI.domain.returns.ApiResponse;
import com.wiily.pscosmeticos.PsAPI.infra.config.AppProperties;
import com.wiily.pscosmeticos.PsAPI.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoryController {
    @Autowired
    CategoryRepository repository;
    @Autowired
    AppProperties properties;
    @Autowired
    ImageService imageService;

    @GetMapping
    public ResponseEntity<Page<ReturnCategoryData>> getCategories(@PageableDefault(size = 12) Pageable pageable) {
        var page = repository.findAll(pageable).map(ReturnCategoryData::new);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> createCategory(@RequestPart(name = "dados") @Valid CreateCategoryData categoryData,
                                                 @RequestPart(name = "imagem") MultipartFile image,
                                                 UriComponentsBuilder uriBuilder) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("Image file is empty");
        }
        var category = new Category(categoryData);
        String imageName = imageService.imageProcessor(image, category);
        category.setImageUrl(imageName);
        repository.save(category);
        var uri = uriBuilder.path("/api/v1/categorias").buildAndExpand(category.getId()).toUri();
        return ResponseEntity.created(uri).body(new ApiResponse(true, new ReturnCategoryCreationData(category)));
    }

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<Object> deleteCategory(@PathVariable Long id) throws IOException {
        var category = repository.getReferenceById(id);
        String path = properties.getStorage().getImageCategoryRoot() + imageService.getImagePath(category);
        Files.delete(Path.of(path));
        repository.delete(category);
        return ResponseEntity.ok().body(new ApiResponse(true, null));
    }


}
