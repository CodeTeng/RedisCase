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
     * 保存笔记并发送给粉丝
     *
     * @param blog 笔记
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
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

    /**
     * 查看个人探店笔记
     *
     * @param id      用户id
     * @param current 当前页
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "id") Long id,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        if (id == null || id <= 0) {
            return Result.fail("查询失败");
        }
        Page<Blog> page = blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    /**
     * 获取关注中的Blog信息
     *
     * @param max    上一次查询的最小时间戳
     * @param offset 偏移量
     * @return 小于指定时间戳的Blog笔记集合和本次查询推送的最小时间戳、偏移量
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        if (max == null || max <= 0) {
            return Result.fail("获取失败！");
        }
        return blogService.queryBlogOfFollow(max, offset);
    }
}
