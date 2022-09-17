package com.lt.learningredis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lt.learningredis.entity.User;
import org.springframework.stereotype.Repository;

/**
 * Mapper 接口
 *
 * @author teng
 * @since 2021-12-22
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

}
