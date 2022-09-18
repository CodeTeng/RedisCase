package com.lt.learningredis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.dto.LoginFormDTO;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.dto.UserDTO;
import com.lt.learningredis.entity.User;
import com.lt.learningredis.mapper.UserMapper;
import com.lt.learningredis.service.IUserService;
import com.lt.learningredis.utils.RegexUtils;
import com.lt.learningredis.constant.SystemConstants;
import com.lt.learningredis.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * 服务实现类
 *
 * @author teng
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();
        // 1. 校验表单
        if (StrUtil.isBlank(phone)) {
            return Result.fail("手机号不能为空");
        }
        if (StrUtil.isBlank(code)) {
            return Result.fail("验证码不能为空");
        }
        // 2. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 3. 校验验证码--->从redis中进行获取
        // String sessionCode = (String) session.getAttribute(SystemConstants.USER_SESSION_CODE);
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (!code.equals(redisCode)) {
            return Result.fail("验证码错误");
        }
        // 4. 根据手机号查询用户
        User user = this.query().eq("phone", phone).one();
        // 5. 若不存在 进行注册
        if (Objects.isNull(user)) {
            user = createUserWithPhone(phone);
        }
        // 6. 若存在，将用户保存到session中--->保存到redis中---记得脱敏数据
        // session.setAttribute(SystemConstants.USER_SESSION_USER, BeanUtil.copyProperties(user, UserDTO.class));
        // 6.1 生成token
        String token = UUID.randomUUID().toString(true);
        // 6.2 将user对象转为Hash进行存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName, filedValue) -> filedValue.toString()));
        // 6.3 存到redis中
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 6.4 设置token的有效期
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 7. 返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setPhone(phone);
        user.setIcon("https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172335185.png");
        // 保存到数据库中
        this.save(user);
        return user;
    }

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("非法的手机号码");
        }
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3. 保存验证码到session中-->保存到redis中
        // session.setAttribute(SystemConstants.USER_SESSION_CODE, code);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 4. 模拟发送验证码
        log.debug("短信验证码为：{}", code);
        return Result.ok();
    }

    @Override
    public Result sign() {
        // 1. 获取当前登录用户
        UserDTO userDTO = UserHolder.getUser();
        Long userId = userDTO.getId();
        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        // 4. 获取今天是本月的第几天
        int day = now.getDayOfMonth();
        // 5. 写入redis
        stringRedisTemplate.opsForValue().setBit(key, day - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 得到该用户本月到今天所有的签到数据
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int day = now.getDayOfMonth();
        // BITFIELD sign:6:202209 GET u18 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 3. 从后开始遍历bit位
        int count = 0;
        while (true) {
            // 让这个数字与1做&运算，得到最后一个数字的bit位
            if ((num & 1) == 0) {
                // 为0
                break;
            } else {
                // 不为0 说明已签到
                count++;
            }
            // 让数字右移一位
            num >>>= 1;
        }
        return Result.ok(count);
    }
}
