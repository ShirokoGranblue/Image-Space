package com.picmgmt.service;

import com.picmgmt.entity.Category;

import java.util.List;

public interface CategoryService {

    Category create(String categoryName);

    void delete(Long categoryId);

    Category update(Long categoryId, String categoryName);

    List<Category> listByUser();
}
