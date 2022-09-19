package com.lt.learningredis.controller;


import com.lt.learningredis.dto.Result;
import com.lt.learningredis.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * @author teng
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注用户
     *
     * @param followUserId 关注用户的id
     * @param isFollow     是否已经关注
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        if (followUserId == null || followUserId <= 0) {
            return Result.fail("关注失败！");
        }
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 是否关注了用户
     *
     * @param followUserId 关注用户的id
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        if (followUserId == null || followUserId <= 0) {
            return Result.fail("查询失败！");
        }
        return followService.isFollow(followUserId);
    }

    /**
     * 共同关注
     *
     * @param targetUserId 目标用户的id
     * @return 两人共同关注的人
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            return Result.fail("查询失败！");
        }
        return followService.followCommons(targetUserId);
    }
}
