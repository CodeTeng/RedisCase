package com.lt.learningredis.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.dto.UserDTO;
import com.lt.learningredis.entity.Blog;
import com.lt.learningredis.service.IBlogService;
import com.lt.learningredis.constant.SystemConstants;
import com.lt.learningredis.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author teng
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 保存笔记
     *
     * @param blog 笔记
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 实现笔记点赞
     *
     * @param id 笔记id
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.fail("点赞失败");
        }
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 分页查询
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 根据id查询
     *
     * @param id 笔记id
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.fail("查询失败");
        }
        return blogService.queryBlogById(id);
    }

    /**
     * 点赞排行榜
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.fail("查询失败");
        }
        return blogService.queryBlogLikes(id);
    }
}
