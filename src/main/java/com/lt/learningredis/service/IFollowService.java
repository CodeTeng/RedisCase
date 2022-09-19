package com.lt.learningredis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.Follow;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/19 17:00
 */
public interface IFollowService extends IService<Follow> {
    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long targetUserId);
}
