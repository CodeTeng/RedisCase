package com.lt.learningredis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.Blog;


/**
 * @author teng
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);
}
