# RedisCase
详细介绍了Redis的基础与高级的语法，包括集群，哨兵，雪崩，击穿等等，并且还包含了一些Redis的典型案例

# Redis

## 1. Redis入门 

### 1.1 Redis是什么 

- 基于内存的`K/V`存储中间件

- NoSQL键值对数据库

`Redis`不仅仅是数据库，它还可用作**消息队列**等等。

![image.png](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171721295.png)

﻿### 1.2 SQL与NoSQL的对比

![image-20220917172748643](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171727701.png)

- 事务特征
    - 非关系型数据库往往不支持事务，或者不能严格保证ACID的特性，只能实现基本的一致性。

- 存储方式
    - 关系型数据库基于磁盘进行存储，会有大量的磁盘IO，对性能有一定影响
    - 非关系型数据库，他们的操作更多的是依赖于内存来操作，内存的读写速度会非常快，性能自然会好一些

* 扩展性
    * 关系型数据库集群模式一般是主从，主从数据一致，起到**数据备份**的作用，称为**垂直扩展**。
    * 非关系型数据库可以将数据拆分，存储在不同机器上，可以保存海量数据，**解决内存大小**有限的问题。称为**水平扩展**。
    * 关系型数据库因为表之间存在关联关系，如果做水平扩展会给数据查询带来很多麻烦

### 1.3 Redis的特征

- 键值（key-value）型，value支持多种不同数据结构，功能丰富
- 单线程，**每个命令具备原子性**
- 低延迟，速度快（基于内存、IO多路复用、良好的编码）
- 支持数据持久化
- 支持主从集群、分片集群
- 支持多语言客户端

### 1.4 Redis的安装

建议在Linux下进行安装，直接到官网进行安装即可，注意安装完毕后注意修改`redis.conf`文件，设置`bind ip`，`requirepass`等

## 2 Redis常见命令

Redis是典型的key-value数据库，key一般是字符串，而value包含很多不同的数据类型：

![image-20220917175600954](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171756006.png)

- redis官网命令集：https://redis.io/commands/，中文版：http://www.redis.cn/commands.html

- redis-cli help 命令查看，help [command] 可以查看某个具体命令、help @xxx 可以查看某个分组下的命令、

### 2.1 Redis通用命令

通用指令是部分数据类型的，都可以使用的指令，常见的有：

- `KEYS`：查看符合模板的所有key
- `DEL`：删除一个指定的key
- `EXISTS`：判断key是否存在
- `EXPIRE`：给一个key设置有效期，有效期到期时该key会被自动删除
- `TTL`：查看一个KEY的剩余有效期

通过`help [command] `可以查看一个命令的具体用法，例如：

```sh
# 查看keys命令的帮助信息：
127.0.0.1:6379> help keys

KEYS pattern
summary: Find all keys matching the given pattern
since: 1.0.0
group: generic
```

### 2.2 String类型

`String`类型，也就是字符串类型，是Redis中最简单的存储类型。

其`value`是字符串，不过根据**字符串的格式**不同，又可以分为3类：

- `string`：普通字符串
- `int`：整数类型，可以做自增、自减操作
- `floa`t：浮点类型，可以做自增、自减操作

不管是哪种格式，底层都是字节数组形式存储，只不过是编码方式不同。

单key的value最大不能超过512M。

![image-20220917180548742](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171805786.png)

#### 2.2.1 String的常见命令

- SET：**添加或者修改**已经存在的一个String类型的键值对
- GET：根据key获取String类型的value
- MSET：批量添加多个String类型的键值对
- MGET：根据多个key获取多个String类型的value
- INCR：让一个整型的key自增1
- INCRBY:让一个整型的key自增并指定步长，例如：incrby num 2 让num值自增2，也可使用负数
- INCRBYFLOAT：让一个浮点类型的数字自增并指定步长
- SETNX：添加一个String类型的键值对，**前提是这个key不存在，否则不执行**
- SETEX：添加一个String类型的键值对，并且**指定有效期**

#### 2.2.2 Key结构

Redis没有类似MySQL中的Table的概念，我们该如何区分不同类型的key呢？

例如，需要存储用户、商品信息到redis，有一个用户id是1，有一个商品id恰好也是1，此时如果使用id作为key，那就会冲突了，该怎么办？

我们可以通过给key添加前缀加以区分，不过这个前缀不是随便加的，有一定的规范：

Redis的key允许有多个单词形成层级结构，多个单词之间用':'隔开，格式如下：

```
项目名:业务名:类型:id
```

这个格式并非固定，也可以根据自己的需求来删除或添加词条。这样以来，我们就可以把不同类型的数据区分开了。从而避免了key的冲突问题。

例如我们的项目名称叫 teng，有user和product两种不同类型的数据，我们可以这样定义key：

- user相关的key：**teng:user:1**

- product相关的key：**teng:product:1**

如果Value是一个Java对象，例如一个User对象，则可以将对象序列化为JSON字符串后存储：

| **KEY**        | **VALUE**                                  |
| -------------- | ------------------------------------------ |
| teng:user:1    | {"id":1,  "name": "Jack", "age": 21}       |
| teng:product:1 | {"id":1,  "name": "小米11", "price": 4999} |

### 2.3 Hash类型

Hash类型，也叫散列，其value是一个无序字典，类似于Java中的`HashMap`结构。

String结构是将对象序列化为JSON字符串后存储，当需要修改对象某个字段时很不方便：

![image-20220917181202086](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171812121.png)

Hash结构可以将对象中的每个字段独立存储，可以针对单个字段做CRUD：

![image-20220917181227388](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171812430.png)

常见命令：

- HSET key field value：添加或者修改hash类型key的field的值

- HGET key field：获取一个hash类型key的field的值

- HMSET：批量添加多个hash类型key的field的值

- HMGET：批量获取多个hash类型key的field的值

- HGETALL：获取一个hash类型的key中的所有的field和value
- HKEYS：获取一个hash类型的key中的所有的field
- HINCRBY:让一个hash类型key的字段值自增并指定步长
- HSETNX：添加一个hash类型的key的field值，前提是这个field不存在，否则不执行

### 2.4 List类型

Redis中的List类型与Java中的`LinkedList`类似，可以看做是一个双向链表结构。既可以支持正向检索和也可以支持反向检索。

特征也与LinkedList类似：

- 有序
- 元素可以重复
- 插入和删除快
- 查询速度一般

常用来存储一个有序数据，例如：朋友圈点赞列表，评论列表等。

![image-20220917181515097](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171815194.png)

### 2.5 Set类型

Redis的Set结构与Java中的`HashSet`类似，可以看做是一个value为null的HashMap。因为也是一个hash表，因此具备与HashSet类似的特征：

- 无序

- 元素不可重复

- 查找快

- 支持交集、并集、差集等功能

![image-20220917181720515](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171817570.png)

### 2.6 SortedSet类型

Redis的SortedSet是一个可排序的set集合，与Java中的`TreeSet`有些类似，但底层数据结构却差别很大。SortedSet中的每一个元素都带有一个`score`属性，可以基于score属性对元素排序，底层的实现是一个跳表（SkipList）加 hash表。

SortedSet具备下列特性：

- 可排序
- 元素不重复
- 查询速度快

因为SortedSet的可排序特性，经常被用来实现排行榜这样的功能。

![image-20220917182008484](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171820538.png)

- **升序**获取sorted set 中的指定元素的排名：ZRANK key member

- **降序**获取sorted set 中的指定元素的排名：ZREVRANK key memeber


练习：

将班级的下列学生得分存入Redis的SortedSet中：

Jack 85, Lucy 89, Rose 82, Tom 95, Jerry 78, Amy 92, Miles 76

并实现下列功能：

- 删除Tom同学

- 获取Amy同学的分数

- 获取Rose同学的排名

- 查询80分以下有几个学生

- 给Amy同学加2分

- 查出成绩前3名的同学

- 查出成绩80分以下的所有同学

![image-20220917191246908](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171912957.png)

## 3. Redis的Java客户端

### 3.1 主流客户端

在Redis官网中提供了各种语言的客户端，地址：https://redis.io/clients

对于Java，推荐以下三种

![image-20220917191949850](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171919918.png)

- Jedis和Lettuce：这两个主要是提供了Redis命令对应的API，方便我们操作Redis，而SpringDataRedis又对这两种做了抽象和封装，因此我们后期会直接以SpringDataRedis来学习。（注意，Jedis是线程不安全的，常配合连接池进行使用）
- Redisson：是在Redis基础上实现了分布式的可伸缩的java数据结构，例如Map.Queue等，而且支持跨进程的同步机制：Lock.Semaphore等待，比较适合用来实现特殊的功能需求。

### 3.2 SpringDataRedis

SpringData是Spring中数据操作的模块，包含对各种数据库的集成，其中对Redis的集成模块就叫做SpringDataRedis

官网地址：https://spring.io/projects/spring-data-redis

* 提供了对不同Redis客户端的整合（Lettuce和Jedis）
* 提供了**RedisTemplate**统一API来操作Redis
* 支持Redis的**发布订阅**模型
* 支持Redis**哨兵**和Redis**集群**
* 支持基于Lettuce的**响应式编程**
* 支持基于JDK.JSON.字符串.Spring对象的**数据序列化及反序列化**
* 支持基于Redis的JDKCollection实现

SpringDataRedis中提供了RedisTemplate工具类，其中封装了各种对Redis的操作。并且将不同数据类型的操作API封装到了不同的类型中：

![image-20220917192605432](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171926478.png)

> 注意：要在Spring Data Redis 中使用Lettuce线程池的话，要额外引入apache commons-pool2依赖

相关依赖

```xml
<!--redis依赖-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--common-pool-->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
<!--Jackson依赖-->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

配置文件

```yml
spring:
  redis:
    host: 192.168.150.101
    port: 6379
    password: 806823
    lettuce:
      pool:
        max-active: 8  #最大连接
        max-idle: 8   #最大空闲连接
        min-idle: 0   #最小空闲连接
        max-wait: 100ms #连接等待时间
```

### 3.3 RedisTemplate 序列化

RedisTemplate 默认使用 JDK 原生序列化器，可读性差，内存占用大，因此可以用以下两种方式来改变序列化机制：

1. **自定义 RedisTemplate**，指定 key 和 value 的序列化器
2. **使用自带的 StringRedisTemplate**，key 和 value 都默认使用 String 序列化器，仅支持写入 String 类型的 key 和 value。因此需要自己将对象序列化成 String 来写入Redis，从 Redis读出数据时也要手动反序列化。

# Redis实战篇

![image-20220917214317341](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172143403.png)

## 1. 短信登录

### 1.1 基于Session实现登录流程

![image-20220917230605854](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172306930.png)

### 1.2 实现发送短信验证码

![image-20220917230802381](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172308455.png)

核心代码：

```java
public Result sendCode(String phone, HttpSession session) {
    // 1. 校验手机号
    if (RegexUtils.isPhoneInvalid(phone)) {
        return Result.fail("非法的手机号码");
    }
    // 2. 生成验证码
    String code = RandomUtil.randomNumbers(6);
    // 3. 保存验证码到session中
    session.setAttribute(SystemConstants.USER_SESSION_CODE, code);
    // 4. 模拟发送验证码
    log.debug("短信验证码为：{}", code);
    return Result.ok();
}
```

### 1.3 实现登录、注册功能

![image-20220917231459359](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172314410.png)

核心代码：

```java
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
    // 3. 校验验证码
    String sessionCode = (String) session.getAttribute(SystemConstants.USER_SESSION_CODE);
    if (!code.equals(sessionCode)) {
        return Result.fail("验证码错误");
    }
    // 4. 根据手机号查询用户
    User user = this.query().eq("phone", phone).one();
    // 5. 若不存在 进行注册
    if (Objects.isNull(user)) {
        user = createUserWithPhone(phone);
    }
    // 6. 若存在，将用户保存到session中
    session.setAttribute(SystemConstants.USER_SESSION_USER, BeanUtil.copyProperties(user, UserDTO.class));
    return Result.ok();
}
```

### 1.4 实现登录拦截功能

![image-20220917234850290](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209172348345.png)

拦截器代码：

```java
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 1. 获取session
    HttpSession session = request.getSession();
    // 2.获取session中的用户
    Object user = session.getAttribute(SystemConstants.USER_SESSION_USER);
    // 3. 判断用户是否存在
    if (user == null) {
        // 4. 不存在，拦截
        response.setStatus(401);
        return false;
    }
    // 5. 存在 保存用户信息到ThreadLocal
    UserHolder.saveUser((UserDTO) user);
    // 6. 放行
    return true;
}
```

相关配置

```java
public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                );
    }
```

> 注意：可以使用`threadlocal`来做到**线程隔离**，每个线程操作自己的一份数据
>
> 在`threadLocal`中，无论是他的`put`方法和他的`get`方法， 都是先从获得当前用户的线程，然后从线程中取出线程的成员变量`map`，只要线程不一样，`map`就不一样，所以可以通过这种方式来做到**线程隔离**

### 1.5 session共享问题

![image-20220918002157478](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180021584.png)

### 1.6 基于Redis实现共享session登录

#### 1.6.1 设计key的结构

首先我们要思考一下利用redis来存储数据，那么到底使用哪种结构呢？由于存入的数据比较简单，我们可以考虑使用String，或者是使用

哈希，如下图，如果使用String，注意他的value，用多占用一点空间，如果使用哈希，则他的value中只会存储他数据本身，如果不是特

别在意内存，其实使用String就可以。

![image-20220918003115508](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180031591.png)

#### 1.6.2 设计key的小细节

所以我们可以使用String结构，就是一个简单的key，value键值对的方式，但是关于key的处理，session他是每个用户都有自己的

session，但是redis的key是共享的，就不能使用code了

在设计这个key的时候，我们之前讲过需要满足两点

- key要具有唯一性
- key要方便携带

如果我们采用phone：手机号这个的数据来存储当然是可以的，但是如果把这样的敏感数据存储到redis中并且从页面中带过来毕竟不太

合适，所以我们在后台生成一个随机串token，然后让前端带来这个token就能完成我们的整体逻辑了。

### 1.7 基于Redis实现短信登录

![image-20220918003644509](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180036702.png)

```java
stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
```

![image-20220918003234219](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180032297.png)

核心代码：

```java
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
```

### 1.8 解决状态登录刷新问题

#### 1.8.1 初始方案问题

在这个方案中，他确实可以使用对应路径的拦截，同时刷新登录token令牌的存活时间，但是现在这个拦截器他**只是拦截需要被拦截的路**

**径，假设当前用户访问了一些不需要拦截的路径，那么这个拦截器就不会生效，所以此时令牌刷新的动作实际上就不会执行**，所以这个方

案他是存在问题的。

![image-20220918011847052](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180118126.png)

#### 1.8.2 优化方案

既然之前的拦截器无法对不需要拦截的路径生效，那么我们可以添加一个拦截器，在**第一个拦截器中拦截所有的路径**，把**第二个拦截器做**

**的事情放入到第一个拦截器中，同时刷新令牌**，因为第一个拦截器有了threadLocal的数据，所以此时第二个拦截器只需要判断拦截器中

的user对象是否存在即可，完成整体刷新功能。

![image-20220918011955189](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209180119249.png)

核心代码：

```java
public class RefreshTokenInterceptor implements HandlerInterceptor {
    
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // 放行
            return true;
        }
        // 2.基于TOKEN获取redis中的用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        // 5.将查询到的hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6.存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        // 7.刷新token有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
```

## 2. 商户查询缓存

### 2.1 添加商户缓存

![image-20220918105241145](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181052283.png)

核心代码：

```java
public Result queryShopById(Long id) {
    // 1. 从redis中查询
    String cacheShopJson = redisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
    // 2. 判断是否存在
    if (StrUtil.isNotBlank(cacheShopJson)) {
        Shop shop = JSONUtil.toBean(cacheShopJson, Shop.class);
        return Result.ok(shop);
    }
    // 3. 不存在，根据id从数据库查询
    Shop shop = this.getById(id);
    // 4. 没有返回未查询到
    if (Objects.isNull(shop)) {
        return Result.fail("店铺不存在！");
    }
    // 5. 数据库中查询到，返回并存入缓存
    redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop));
    return Result.ok(shop);
}
```

### 2.2 缓存更新策略

缓存更新是redis为了节约内存而设计出来的一个东西，主要是因为内存数据宝贵，当我们向redis插入太多数据，此时就可能会导致缓存

中的数据过多，所以redis会对部分数据进行更新，或者把他叫为淘汰更合适。

![image-20220918113332150](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181133220.png)

**主动更新策略**

![image-20220918113734705](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181137794.png)

![image-20220918114102593](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181141655.png)

两种操作方案都有数据不一致性问题

![image-20220918115048005](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181150064.png)

![image-20220918114911644](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181149715.png)

**缓存更新策略的最佳实践方案：**

1. 低一致性需求：**使用Redis自带的内存淘汰机制**
2. 高一致性需求：**主动更新，并以超时剔除作为兜底方案**
    1. 读操作：
        - 缓存命中则直接返回
        - 缓存未命中则查询数据库，并写入缓存，设定超时时间
    2. 写操作：
        - **先写数据库，然后再删除缓存**
        - 要确保数据库与缓存操作的**原子性**

### 2.3 实现商铺和缓存与数据库双写一致

核心思路如下：

修改ShopController中的业务逻辑，满足下面的需求：

- 根据id查询店铺时，如果缓存未命中，则查询数据库，将数据库结果写入缓存，并设置超时时间

- 根据id修改店铺时，先修改数据库，再删除缓存

核心代码：

**查询修改---设置redis缓存时添加过期时间**

```java
stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
```

**更新修改---先修改数据库，再删除缓存**

```java
@Transactional
public Result updateShop(Shop shop) {
    // 1. 修改数据库
    this.updateById(shop);
    // 2. 删除缓存
    stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
    return Result.ok();
}
```

### 2.4 缓存穿透

**缓存穿透**是指客户端**请求的数据在缓存中和数据库中都不存在，这样缓存永远不会生效，这些请求都会打到数据库**。

![image-20220918133842487](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181338577.png)

缓存空对象逻辑

![image-20220918140857130](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181408196.png)

核心代码：

```java
public Result queryShopById(Long id) {
        // 1. 从redis中查询
    String cacheShopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
    // 2. 判断是否存在
    if (StrUtil.isNotBlank(cacheShopJson)) {
        Shop shop = JSONUtil.toBean(cacheShopJson, Shop.class);
        return Result.ok(shop);
    }
    // 命中的是否是空值
    if (cacheShopJson != null) {
        return Result.fail("店铺信息不存在！");
    }
    // 3. 不存在，根据id从数据库查询
    Shop shop = this.getById(id);
    // 4. 没有返回未查询到
    if (Objects.isNull(shop)) {
        // 将空值写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
        return Result.fail("店铺不存在！");
    }
    // 5. 数据库中查询到，返回并存入缓存，并且设置超时时间
    stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    return Result.ok(shop);
}
```

小总结

**缓存穿透产生的原因是什么？**

* 用户请求的数据在缓存中和数据库中都不存在，不断发起这样的请求，给数据库带来巨大压力

**缓存穿透的解决方案有哪些？**

* 缓存空对象值
* 布隆过滤
* 增强id的复杂度，避免被猜测id规律
* 做好数据的基础格式校验
* 加强用户权限校验
* 做好热点参数的限流

### 2.5 缓存雪崩

**缓存雪崩**是指在同一时段大量的缓存key同时失效或者Redis服务宕机，导致大量请求到达数据库，带来巨大压力。

![image-20220918143023951](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181430070.png)

### 2.6 缓存击穿

**缓存击穿问题**也叫**热点Key**问题，就是一个被**高并发访问**并且**缓存重建业务较复杂**的key突然失效了，无数的请求访问会在瞬间给数据库

带来巨大的冲击。

![image-20220918144408451](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181444537.png)

解决方案逻辑

![image-20220918144628573](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181446690.png)

二者对比

![image-20220918144644853](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181446905.png)

#### 2.6.1 互斥锁解决方案

![image-20220918145120850](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181451926.png)

操作锁代码：

```java
/**
 * 获取互斥锁
 */
private boolean tryLock(String key) {
    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
    // 不要直接返回，因为有自动拆箱-防止空指针
    return BooleanUtil.isTrue(flag);
}

/**
 * 释放互斥锁
 */
private void unlock(String key) {
    stringRedisTemplate.delete(key);
}
```

核心代码：

```java
private Shop queryWithMutex(Long id) {
    // 1. 从redis中查询缓存
    String key = RedisConstants.CACHE_SHOP_KEY + id;
    String cacheShopJson = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isNotBlank(cacheShopJson)) {
        // 命中 直接返回
        return JSONUtil.toBean(cacheShopJson, Shop.class);
    }
    // 判断是否为空值
    if (cacheShopJson != null) {
        return null;
    }
    // 2. 获取互斥锁
    String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    Shop shop = null;
    try {
        boolean isLock = tryLock(lockKey);
        if (!isLock) {
            // 获取失败 休眠重试
            Thread.sleep(50);
            return queryWithMutex(id);
        }
        // 成功 根据id查询数据库
        shop = this.getById(id);
        // 3. 判断数据库中是否存在
        if (Objects.isNull(shop)) {
            // 不存在 存入空对象 防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4. 查询到 写入redis中 并设置过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
        // 5. 释放互斥锁
        unlock(lockKey);
    }
    return shop;
}
```

#### 2.6.2 逻辑过期解决方案

![image-20220918145129881](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181451960.png)

核心代码：

```java
private Shop queryWithLogicalExpire(Long id) {
    String key = RedisConstants.CACHE_SHOP_KEY + id;
    // 1. 从redis中查询
    String redisDataJson = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isBlank(redisDataJson)) {
        // 2. 不存在 直接返回空
        return null;
    }
    // 3. 命中缓存 判断是否过期
    RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);
    JSONObject data = (JSONObject) redisData.getData();
    Shop shop = JSONUtil.toBean(data, Shop.class);
    LocalDateTime expireTime = redisData.getExpireTime();
    if (expireTime.isAfter(LocalDateTime.now())) {
        // 未过期
        return shop;
    }
    // 4. 过期 获取互斥锁
    String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    boolean isLock = tryLock(lockKey);
    if (isLock) {
        // 获取成功 开启独立线程，实现缓存重建
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                this.saveShopToRedis(id, 20L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        });
    }
    return shop;
}
```

### 2.7 缓存工具封装

详情见 `CacheClient.java` 中查找

## 4. 达人探店

### 4.1 点赞功能

初始代码

```java
@GetMapping("/likes/{id}")
public Result queryBlogLikes(@PathVariable("id") Long id) {
    //修改点赞数量
    blogService.update().setSql("liked = liked +1 ").eq("id",id).update();
    return Result.ok();
}
```

问题分析：这种方式会**导致一个用户无限点赞**，明显是不合理的

造成这个问题的原因是，我们现在的逻辑，发起请求只是给数据库+1，所以才会出现这个问题

完善点赞功能

需求：

* 同一个用户只能点赞一次，再次点击则取消点赞
* 如果当前用户已经点赞，则点赞按钮高亮显示（前端已实现，判断字段Blog类的isLike属性）

实现步骤：

* 给Blog类中添加一个isLike字段，标示是否被当前用户点赞
* 修改点赞功能，**利用Redis的set集合判断是否点赞过**，未点赞过则点赞数+1，已点赞过则点赞数-1
* 修改根据id查询Blog的业务，判断当前登录用户是否点赞过，赋值给isLike字段
* 修改分页查询Blog业务，判断当前登录用户是否点赞过，赋值给isLike字段

核心代码：

```java
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
```

### 4.2 点赞排行榜

![image-20220918192208034](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209181922207.png)

核心代码：

```java
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
    // 3. 根据用户id查询用户 where id in (6, 1) order by filed(id, 6, 1)
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.in("id", userIdList).last("order by field(id," + userIdStr + ")");
    List<UserDTO> userDTOList = userMapper.selectList(queryWrapper)
        .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
    // 4. 返回
    return Result.ok(userDTOList);
}
```

> 注意：为什么SQL语句里面要用filed...这是因为MySQL在查询时会进行优化，会将in(6,1)优化为in(1,6)，所以我们需要自定义field

## 11. 用户签到

### 11.1 BitMap用法

我们按月来统计用户签到信息，签到记录为1，未签到则记录为0.

把**每一个bit位**对应当月的每一天，形成了映射关系。用0和1标示业务状态，这种思路就称为**位图（BitMap）**。这样我们就用极小的空

间，来实现了大量数据的表示

Redis中是利用string类型数据结构实现BitMap，因此**最大上限是512M**，转换为bit则是 2^32个bit位。

![image-20220918210945655](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182109709.png)

BitMap的操作命令有：

* SETBIT：向指定位置（offset）存入一个0或1
* GETBIT ：获取指定位置（offset）的bit值
* BITCOUNT ：统计BitMap中值为1的bit位的数量
* BITFIELD ：操作（查询、修改、自增）BitMap中bit数组中的指定位置（offset）的值
* BITFIELD_RO ：获取BitMap中bit数组，并以十进制形式返回
* BITOP ：将多个BitMap的结果做位运算（与 、或、异或）
* BITPOS ：查找bit数组中指定范围内第一个0或1出现的位置

![image-20220918211955557](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182119668.png)

### 11.2 签到功能

![image-20220918212225587](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182122647.png)

核心代码：

```java
public Result sign() {
    // 1. 获取当前登录用户
    UserDTO userDTO = UserHolder.getUser();
    String nickName = userDTO.getNickName();
    // 2. 获取日期
    LocalDateTime now = LocalDateTime.now();
    // 3. 拼接key
    String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String key = RedisConstants.USER_SIGN_KEY + nickName + keySuffix;
    // 4. 获取今天是本月的第几天
    int day = now.getDayOfMonth();
    // 5. 写入redis
    stringRedisTemplate.opsForValue().setBit(key, day - 1, true);
    return Result.ok();
}
```

### 11.3 签到统计

**问题1：**什么叫做连续签到天数？

从最后一次签到开始向前统计，直到遇到第一次未签到为止，计算总的签到次数，就是连续签到天数。

![image-20220918214349314](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182143374.png)

Java逻辑代码：获得当前这个月的最后一次签到数据，定义一个计数器，然后不停的向前统计，直到获得第一个非0的数字即可，每得到

一个非0的数字计数器+1，直到遍历完所有的数据，就可以获得当前月的签到总天数了

**问题2：**如何得到本月到今天为止的所有签到数据？

`BITFIELD key GET u[day] 0`

假设今天是10号，那么我们就可以从当前月的第一天开始，获得到当前这一天的位数，是10号，那么就是10位，去拿这段时间的数据，

就能拿到所有的数据了，那么这10天里边签到了多少次呢？统计有多少个1即可。

**问题3：如何从后向前遍历每个bit位？**

注意：bitMap返回的数据是10进制，哪假如说返回一个数字8，那么我哪儿知道到底哪些是0，哪些是1呢？我们只需要让得到的10进制

数字**和1做与运算**就可以了，因为1只有遇见1才是1，其他数字都是0 ，我们把签到结果和1进行与操作，每与一次，就把**签到结果向右移**

**动一位**，依次内推，我们就能完成逐个遍历的效果了。

![image-20220918214450863](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182144924.png)

核心代码：

```java
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
```

## 12. UV统计

### 12.1 HyperLogLog用法

* UV：全称Unique Visitor，也叫**独立访客量**，是指通过互联网访问、浏览这个网页的自然人。1天内同一个用户多次访问该网站，只记录1次。
* PV：全称Page View，也叫**页面访问量或点击量**，用户每访问网站的一个页面，记录1次PV，用户多次打开页面，则记录多次PV。往往用来衡量网站的流量。

通常来说UV会比PV大很多，所以衡量同一个网站的访问量，我们需要综合考虑很多因素，所以我们只是单纯的把这两个值作为一个参考

值。

UV统计在服务端做会比较麻烦，因为要判断该用户是否已经统计过了，需要将统计过的用户信息保存。但是如果每个访问的用户都保存

到Redis中，数据量会非常恐怖，那怎么处理呢？

`Hyperloglog(HLL)`是从`Loglog`算法派生的**概率算法**，**用于确定非常大的集合的基数，而不需要存储其所有值**。

相关算法原理大家可以参考：https://juejin.cn/post/6844903785744056333#heading-0

Redis中的HLL是基于string结构实现的，单个HLL的内存**永远小于16kb**，**内存占用低**的令人发指！作为代价，其测量结果是概率性的，**有**

**小于0.81％的误差**。不过对于UV统计来说，这完全可以忽略。

![image-20220918205056291](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209182050360.png)

### 12.2 实现UV统计

测试思路：我们直接利用单元测试，向HyperLogLog中添加100万条数据，看看内存占用和统计效果如何

```java
void testHyperLogLog() {
    String[] values = new String[1000];
    int j = 0;
    for (int i = 0; i < 1000000; i++) {
        j = i % 1000;
        values[j] = "user_" + i;
        if (j == 999) {
            // 发送到redis中
            stringRedisTemplate.opsForHyperLogLog().add("hll", values);
        }
    }
    // 统计数量
    Long size = stringRedisTemplate.opsForHyperLogLog().size("hll");
    System.out.println(size);
}
```

经过测试：我们会发生他的误差是在允许范围内，并且内存占用极小


