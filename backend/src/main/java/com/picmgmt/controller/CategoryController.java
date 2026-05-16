package com.picmgmt.controller;

import com.picmgmt.common.Result;
import com.picmgmt.entity.Category;
import com.picmgmt.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "分类模块")
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Category> create(@RequestBody Map<String, String> body) {
        return Result.ok(categoryService.create(body.get("categoryName")));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "修改分类名称")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(categoryService.update(id, body.get("categoryName")));
    }

    @Operation(summary = "获取当前用户的分类列表")
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listByUser());
    }
}
