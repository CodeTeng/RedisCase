package com.lt.learningredis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.constant.SystemConstants;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.dto.UserDTO;
import com.lt.learningredis.entity.Blog;
import com.lt.learningredis.entity.User;
import com.lt.learningredis.mapper.BlogMapper;
import com.lt.learningredis.mapper.UserMapper;
import com.lt.learningredis.service.IBlogService;
import com.lt.learningredis.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author teng
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        // 1. 查询blog
        Blog blog = this.getById(id);
        if (Objects.isNull(blog)) {
            return Result.fail("查询笔记不存在");
        }
        // 2. 查询blog有关的用户
        queryBlogUser(blog);
        // 3. 查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query().orderByDesc("liked").page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userMapper.selectById(userId);
        if (Objects.isNull(user)) {
            throw new RuntimeException("查询用户失败");
        }
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void isBlogLiked(Blog blog) {
        // 1. 获取当前用户
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            // 用户未登录 无需查询是否点赞
            return;
        }
        Long userId = userDTO.getId();
        // 2. 判断该用户是否已经点赞
        String key = RedisConstants.CACHE_BLOG_LIKED + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 判断该用户是否已经点赞
        String key = RedisConstants.CACHE_BLOG_LIKED + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3. 如果未点赞，可以点赞
            // 3.1 数据库点赞
            boolean isSuccess = this.update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                // 3.2 保存用户到redis的set中--->保存到SortedSet
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4. 已点赞，取消点赞
            // 4.1 数据库点赞-1
            boolean isSuccess = this.update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                // 4.2 用户从redis中移除
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.CACHE_BLOG_LIKED + id;
        // 1. 查询top5的点赞用户
        Set<String> top = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top == null || top.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2. 解析出其中的用户id
        List<Long> userIdList = top.stream().map(Long::valueOf).collect(Collectors.toList());
        String userIdStr = StrUtil.join(",", userIdList);
        // 3. 根据用户id查询用户 where id in (5, 1) order by filed(id, 5, 1)
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", userIdList).last("order by field(id," + userIdStr + ")");
        List<UserDTO> userDTOList = userMapper.selectList(queryWrapper)
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        // 4. 返回
        return Result.ok(userDTOList);
    }
}
