package com.lt.learningredis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.constant.SystemConstants;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.dto.ScrollResult;
import com.lt.learningredis.dto.UserDTO;
import com.lt.learningredis.entity.Blog;
import com.lt.learningredis.entity.Follow;
import com.lt.learningredis.entity.User;
import com.lt.learningredis.mapper.BlogMapper;
import com.lt.learningredis.mapper.FollowMapper;
import com.lt.learningredis.mapper.UserMapper;
import com.lt.learningredis.service.IBlogService;
import com.lt.learningredis.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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

    @Autowired
    private FollowMapper followMapper;

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

    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2. 保存探店博文
        boolean isSuccess = this.save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败!");
        }
        // 3. 查询作者所有的粉丝 select user_id from tb_follow where follow_user_id = ?
        List<Follow> follows = followMapper.selectList(new QueryWrapper<Follow>().eq("follow_user_id", user.getId()).select("user_id"));
        // 4. 发送给粉丝
        follows.forEach(follow -> {
            String key = RedisConstants.FEED_FOLLOW_KEY + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        });
        // 5. 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = RedisConstants.FEED_FOLLOW_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 3. 解析数据：blogId、score---minTime(时间戳)、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int finalOffset = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            // 3.1 获取blogId集合
            ids.add(Long.valueOf(typedTuple.getValue()));
            // 3.2 获取时间戳(分数)
            long time = typedTuple.getScore().longValue();
            if (minTime == time) {
                finalOffset++;
            } else {
                // 不是最小的 重置
                minTime = time;
                finalOffset = 1;
            }
        }
        // 4. 根据id查询blog
        String idStr = StrUtil.join(",", ids);
        // 不能直接in，因为MySQL会进行优化
        List<Blog> blogs = this.query().in("id", ids).last("order by field(id," + idStr + ")").list();
        blogs.forEach(blog -> {
            // 4.1 查询blog有关的用户
            this.queryBlogUser(blog);
            // 4.2 查询blog是否被点赞
            this.isBlogLiked(blog);
        });
        // 5. 封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(finalOffset);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }
}
