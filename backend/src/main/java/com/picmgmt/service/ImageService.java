package com.picmgmt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.vo.ImageVO;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    ImageVO upload(MultipartFile file, Long categoryId, String description, String tags,
                   String visibility, String visibleUsernames);

    void delete(Long imageId);

    ImageVO update(Long imageId, String imageName, Long categoryId, String description, String tags,
                   String visibility, String visibleUsernames);

    Page<ImageVO> page(ImageQueryDTO dto);

    ImageVO getById(Long imageId);

    Page<ImageVO> getSquare(Integer page, Integer limit);
}
