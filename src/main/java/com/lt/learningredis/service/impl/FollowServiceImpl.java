package com.lt.learningredis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.dto.UserDTO;
import com.lt.learningredis.entity.Follow;
import com.lt.learningredis.mapper.FollowMapper;
import com.lt.learningredis.mapper.UserMapper;
import com.lt.learningredis.service.IFollowService;
import com.lt.learningredis.utils.UserHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/19 17:01
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 获取当前用户id
        Long userId = UserHolder.getUser().getId();
        // 1. 判断是关注还是取关
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        if (isFollow) {
            // 关注 新增 并且放入redis中
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = this.save(follow);
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, StrUtil.toString(followUserId));
            }
        } else {
            // 取关 删除 并从redis中删除
            boolean isSuccess = this.remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Long count = this.query().eq("user_id", userId)
                .eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long targetUserId) {
        // 1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        String myUserKey = RedisConstants.FOLLOW_USER_KEY + userId;
        String targetUserKey = RedisConstants.FOLLOW_USER_KEY + targetUserId;
        // 2. 求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(myUserKey, targetUserKey);
        if (intersect == null || intersect.isEmpty()) {
            // 无共同关注
            return Result.ok(Collections.emptyList());
        }
        // 3. 解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).toList();
        // 4. 查询用户
        List<UserDTO> userDTOList = userMapper.selectBatchIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class)).toList();
        return Result.ok(userDTOList);
    }
}
