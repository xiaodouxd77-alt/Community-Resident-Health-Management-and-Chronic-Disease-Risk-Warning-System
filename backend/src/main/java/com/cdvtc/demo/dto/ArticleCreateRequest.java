package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleCreateRequest {

    @NotBlank(message = "请输入文章标题")
    @Size(max = 100, message = "文章标题长度不能超过100")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @NotBlank(message = "请填写文章正文")
    @Size(max = 20000, message = "正文长度不能超过20000")
    private String content;

    @NotBlank(message = "请选择文章分类")
    private String category;

    @Size(max = 255, message = "封面地址长度不能超过255")
    private String coverUrl;
}
