# RedisCase
详细介绍了Redis的基础与高级的语法，包括集群，哨兵，雪崩，击穿等等，并且还包含了一些Redis的典型案例

# Redis基础篇

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

## 3. 优惠卷秒杀

### 3.1 全局唯一ID

每个店铺都可以发布优惠券，当用户抢购时，就会生成订单并保存到`tb_voucher_order`这张表中，而订单表如果使用数据库自增ID就存

在一些问题：

* id的规律性太明显
* 受单表数据量的限制

场景分析一：如果我们的id具有太明显的规则，用户或者说商业对手很容易猜测出来我们的一些敏感信息，比如商城在一天时间内，卖出

了多少单，这明显不合适。

场景分析二：随着我们商城规模越来越大，mysql的单表的容量不宜超过500W，数据量过大之后，我们要进行拆库拆表，但拆分表了之

后，**他们从逻辑上讲他们是同一张表，所以他们的id是不能一样的， 于是乎我们需要保证id的唯一性**。

**全局ID生成器**，是一种在分布式系统下用来生成全局唯一ID的工具，一般要满足下列特性：

![image-20220920202728811](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202027865.png)

为了增加ID的安全性，我们可以不直接使用Redis自增的数值，而是拼接一些其它信息：

![image-20220920202749249](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202027289.png)

ID的组成部分

- 符号位：1bit，永远为0
- 时间戳：31bit，以秒为单位，可以使用69年
- 序列号：32bit，秒内的计数器，支持每秒产生2^32个不同ID

核心代码：

```java
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳-2022-1-1-0-0-0
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成全局唯一ID
     *
     * @param keyPrefix 业务前缀
     * @return 全局唯一ID
     */
    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2. 生成序列号
        // 2.1. 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2. 自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        // 3. 拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
```

测试代码：

```java
@SpringBootTest
public class RedisIdWorkerTest {

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    /**
     * 测试全局唯一ID
     */
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id=" + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }
        latch.await();  // 阻塞 --> 让main线程进行阻塞 等待其他线程执行完毕
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
}
```

> 知识小贴士：
>
> 关于`countdownlatch`。`countdownlatch`名为信号枪：主要的作用是**同步协调在多线程的等待于唤醒问题**
>
> `CountDownLatch` 中有两个最重要的方法: 1. `countDown`    2. `await`
>
> `await`方法是阻塞方法，我们担心分线程没有执行完时，`main`线程就先执行，所以**使用`await`可以让`main`线程阻塞**，那么什么时候`main`线程不再阻塞呢？当`CountDownLatch` 内部维护的变量变为0时，就不再阻塞，直接放行。那么什么时候`CountDownLatch`   维护的变量变为0呢，我们只需要调用一次`countDown` ，内部变量就减少1，我们让分线程和变量绑定， 执行完一个分线程就减少一个变量，当分线程全部走完，`CountDownLatch`维护的变量就是0，此时`await`就不再阻塞，统计出来的时间也就是所有分线程执行完后的时间。

### 3.2 添加优惠卷

我们这里将优惠卷分为两类，分别是平价券和特价券。平价券可以任意购买，而特价券需要秒杀抢购。

![image-20220920203625047](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202036090.png)

对应的数据库的表分别是：

`tb_voucher`：优惠券的基本信息，优惠金额、使用规则等

`tb_seckill_voucher`：优惠券的库存、开始抢购时间，结束抢购时间。特价优惠券才需要填写这些信息

新增秒杀卷核心代码：

```java
@Transactional(rollbackFor = Exception.class)
public void addSeckillVoucher(Voucher voucher) {
    // 添加优惠卷
    this.save(voucher);
    // 添加秒杀信息
    SeckillVoucher seckillVoucher = new SeckillVoucher();
    seckillVoucher.setVoucherId(voucher.getId());
    seckillVoucher.setStock(voucher.getStock());
    seckillVoucher.setBeginTime(voucher.getBeginTime());
    seckillVoucher.setEndTime(voucher.getEndTime());
    seckillVoucherMapper.insert(seckillVoucher);
    // 保存秒杀库存到redis中
    stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
}
```

添加测试

```json
POST http://localhost:8081/voucher/seckill
authorization: 7e847c18f86645ef8aaa0a299a4daee2
Content-Type: application/json

{
  "shopId": 1,
  "title": "100元代金券",
  "subTitle": "周一至周五均可使用",
  "rules": "全程通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食",
  "payValue": 8000,
  "actualValue": 10000,
  "type": 1,
  "stock": 100,
  "beginTime": "2022-09-20T10:09:17",
  "endTime": "2022-09-20T23:09:17"
}
```

### 3.3 实现优惠券秒杀下单

![image-20220920205520066](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202055183.png)

秒杀下单应该思考的内容：

下单时需要判断两点：

* 秒杀是否开始或结束，如果尚未开始或已经结束则无法下单
* 库存是否充足，不足则无法下单

![image-20220920211335836](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202113959.png)

核心代码：

```java
public Result seckillVoucher(Long voucherId) {
    // 1. 查询优惠卷信息
    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    // 2. 判断秒杀是否开始
    if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
        // 秒杀未开始
        return Result.fail("秒杀尚未开始！");
    }
    // 3. 判断秒杀是否结束
    if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
        // 秒杀已结束
        return Result.fail("秒杀已结束");
    }
    // 4. 判断库存是否充足
    if (voucher.getStock() < 1) {
        return Result.fail("库存不足！");
    }
    // 5. 扣减库存
    boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).update();
    if (!isSuccess) {
        return Result.fail("库存不足！");
    }
    // 6. 创建订单
    VoucherOrder voucherOrder = new VoucherOrder();
    // 6.1 订单id
    long orderId = redisIdWorker.nextId("order");
    voucherOrder.setId(orderId);
    // 6.2 用户id
    Long userId = UserHolder.getUser().getId();
    voucherOrder.setUserId(userId);
    // 6.3 代金券id
    voucherOrder.setVoucherId(voucherId);
    // 6.5 保存订单
    this.save(voucherOrder);
    // 7. 返回订单id
    return Result.ok(orderId);
}
```

### 3.4 优惠券秒杀---超卖问题

关于上述代码是有问题的，如下图所示，出现了超卖现象。

![image-20220920213421619](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202134791.png)

超卖问题是典型的多线程安全问题，针对这一问题的常见解决方案就是**加锁**，而对于加锁，我们通常有两种解决方案：见下图：

![image-20220920215023067](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202150130.png)

**悲观锁：**

悲观锁可以实现**对于数据的串行化执行**，比如**syn、lock**都是悲观锁的代表，同时，悲观锁中又可以再细分为公平锁，非公平锁，可重入

锁，等等。

**乐观锁：**

乐观锁的关键是**判断之前查询得到的数据是否有被修改过**，常见的方式有两种：

![image-20220920215147987](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202151086.png)

![image-20220920215154673](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202151729.png)

乐观锁解决超卖方案一核心代码：

```java
boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .eq("stock", voucher.getStock()).update();
```

![image-20220920215838781](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202158836.png)

上述方案也是存在很大问题，当库存数量大于0时，由于还可以进行卖出，但是同一线程进来比较时发现不一致，于是导致不会进行卖出。

改进核心代码：

```java
boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0).update();
```

之前的方式要修改前后都保持一致，但是这样我们分析过，成功的概率太低，所以我们的乐观锁需要变一下，改成stock大于0 即可

**两种方案对比**

![image-20220920221025467](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202210516.png)

### 3.5 优惠券秒杀---一人一单

需求：修改秒杀业务，要求**同一个优惠券，一个用户只能下一单。**

![image-20220920220908571](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202209646.png)

核心代码

```java
// 实现一人一单
// 5. 根据优惠卷id和用户id查询订单
Long userId = UserHolder.getUser().getId();
Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
// 5.1 判断是否存在
if (count > 0) {
    // 用户已经购买过
    return Result.fail("不可再次抢购！");
}
```

**存在问题：**现在的问题还是和之前一样，并发过来，查询数据库，都不存在订单，所以我们还是需要加锁，但是乐观锁比较适合更新数

据，而现在是插入数据，所以我们需要使用悲观锁操作。

**注意：**在这里提到了非常多的问题，我们需要慢慢的来思考，首先我们的初始方案是封装了一个createVoucherOrder方法，同时为了确

保他线程安全，在方法上添加了一把synchronized锁

```java
@Transactional
public synchronized Result createVoucherOrder(Long voucherId) {
	Long userId = UserHolder.getUser().getId();
    // 5.1.查询订单
    int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    // 5.2.判断是否存在
    if (count > 0) {
        // 用户已经购买过了
        return Result.fail("用户已经购买过一次！");
    }

    // 6.扣减库存
    boolean success = seckillVoucherService.update()
        .setSql("stock = stock - 1") // set stock = stock - 1
        .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
        .update();
    if (!success) {
        // 扣减失败
        return Result.fail("库存不足！");
    }

    // 7.创建订单
    VoucherOrder voucherOrder = new VoucherOrder();
    // 7.1.订单id
    long orderId = redisIdWorker.nextId("order");
    voucherOrder.setId(orderId);
    // 7.2.用户id
    voucherOrder.setUserId(userId);
    // 7.3.代金券id
    voucherOrder.setVoucherId(voucherId);
    save(voucherOrder);

    // 7.返回订单id
    return Result.ok(orderId);
}
```

但是这样添加锁，锁的粒度太粗了，在使用锁过程中，控制**锁粒度**是一个非常重要的事情，因为如果锁的粒度太大，会导致每个线程

进来都会锁住，所以我们需要去控制锁的粒度，以下这段代码需要修改为：

```java
@Transactional
public  Result createVoucherOrder(Long voucherId) {
	Long userId = UserHolder.getUser().getId();
	synchronized(userId.toString().intern()){
         // 5.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次！");
        }

        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足！");
        }

        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2.用户id
        voucherOrder.setUserId(userId);
        // 7.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 7.返回订单id
        return Result.ok(orderId);
    }
}
```

`intern()` 这个方法是从常量池中拿到数据，如果我们直接使用`userId.toString()`他拿到的对象实际上是不同的对象，new出来的

对象，我们使用锁必须保证锁必须是同一把，所以我们需要使用intern()方法。

但是以上代码还是存在问题，问题的原因在于**当前方法被spring的事务控制**，如果你**在方法内部加锁，可能会导致当前方法事务还没有提**

**交，但是锁已经释放也会导致问题**，所以我们选择将当前方法整体包裹起来，确保事务不会出现问题：如下：在seckillVoucher 方法中，

添加以下逻辑，这样就能保证事务的特性，同时也控制了锁的粒度。

![image-20220920223630699](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202236750.png)

但是以上做法依然有问题，因为你调用的方法，其实是**this.的方式调用的，事务想要生效，还得利用代理来生效**，所以这个地方，我们需

要获得原始的事务对象， 来操作事务。

```java
synchronized (userId.toString().intern()) {
    // 获取代理对象(事务) ---> 使事务生效
    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    // 将锁放在这里为了先执行事务 再释放锁
    return proxy.createVoucherOrder(voucherId);
}
```

### 3.6 集群环境下的并发问题

通过加锁可以解决在单机情况下的一人一单安全问题，但是在集群模式下就不行了。

我们将服务启动两份，端口分别为8081和8082

然后修改nginx的conf目录下的nginx.conf文件，配置反向代理和负载均衡

然后我们用一个用户发送请求，会发现锁失效。

**有关锁失效原因分析**

由于现在我们**部署了多个tomcat**，每个**tomcat都有一个属于自己的jvm**，那么假设在服务器A的tomcat内部，有两个线程，这两个线程由于使用的是同一份代码，那么他们的锁对象是同一个，是可以实现互斥的，但是如果现在是服务器B的tomcat内部，又有两个线程，但是他们的锁对象写的虽然和服务器A一样，但是锁对象却不是同一个，所以线程3和线程4可以实现互斥，但是却无法和线程1和线程2实现互斥，这就是集群环境下，syn锁失效的原因，在这种情况下，我们就需要使用分布式锁来解决这个问题。

![image-20220920225904516](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202259606.png)

## 4. 分布式锁

### 4.1 基本原理和实现方式对比

分布式锁：**满足分布式系统或集群模式下多进程可见并且互斥的锁**。

分布式锁的核心思想就是让大家都使用同一把锁，只要大家使用的是同一把锁，那么我们就能锁住线程，不让线程进行，让程序串行执

行，这就是分布式锁的核心思路。

![image-20220920233301312](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202333391.png)

分布式锁应满足的条件

![image-20220920233340327](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202333393.png)

> 注意：这个地方说的可见性并不是并发编程中指的内存可见性，只是说多个进程之间都能感知到变化的意思

常见的分布式锁

![image-20220920233504884](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202335998.png)

### 4.2 Redis分布式锁的实现核心思路

![image-20220920234238889](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209202342050.png)

### 4.3 实现分布式锁版本一

**锁的基本接口**

```java
public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true代表获取锁成功; false代表获取锁失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
```

**分布式锁的实现---SimpleRedisLock**

利用setnx方法进行加锁，同时增加过期时间，防止死锁，此方法可以保证加锁和增加过期时间具有原子性

```java
public class SimpleRedisLock implements ILock {

    private String name;

    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程的标识
        long threadId = Thread.currentThread().getId();
        // 获取锁
        Boolean isSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(isSuccess);
    }

    @Override
    public void unlock() {
        // 释放锁
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
```

业务代码修改：

```java
public Result seckillVoucher(Long voucherId) {
    // 1. 查询优惠卷信息
    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    // 2. 判断秒杀是否开始
    if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
        // 秒杀未开始
        return Result.fail("秒杀尚未开始！");
    }
    // 3. 判断秒杀是否结束
    if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
        // 秒杀已结束
        return Result.fail("秒杀已结束");
    }
    // 4. 判断库存是否充足
    if (voucher.getStock() < 1) {
        return Result.fail("库存不足！");
    }
    Long userId = UserHolder.getUser().getId();
    // 改用自己创建的分布式锁进行测试
    // 创建锁对象
    SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
    // 获取锁
    boolean isLock = lock.tryLock(1200L);
    // 判断是否获取锁成功
    if (!isLock) {
        // 获取失败 返回错误或重试
        return Result.fail("不允许重复下单！");
    }
    try {
        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        return proxy.createVoucherOrder(voucherId);
    } finally {
        // 释放锁
        lock.unlock();
    }
}
```

### 4.4 Redis分布式锁误删

逻辑说明：

持有锁的线程在锁的内部出现了阻塞，导致他的锁自动释放，这时其他线程，线程2来尝试获得锁，就拿到了这把锁，然后线程2在持有锁

执行过程中，线程1反应过来，继续执行，而线程1执行过程中，走到了删除锁逻辑，此时就会把本应该属于线程2的锁进行删除，这就是

误删别人锁的情况说明。

解决方案：

解决方案就是在每个线程释放锁的时候，去判断一下当前这把锁是否属于自己，如果属于自己，则不进行锁的删除，假设还是上边的情

况，线程1卡顿，锁自动释放，线程2进入到锁的内部执行逻辑，此时线程1反应过来，然后删除锁，但是线程1，一看当前这把锁不是属

于自己，于是不进行删除锁逻辑，当线程2走到删除锁逻辑时，如果没有卡过自动释放锁的时间点，则判断当前这把锁是属于自己的，于

是删除这把锁。

![image-20220921143458047](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211435168.png)

修改：

需求：修改之前的分布式锁实现，满足：在获取锁时存入线程标示（可以用UUID表示）

**在释放锁时先获取锁中的线程标示，判断是否与当前线程标示一致**

* 如果一致则释放锁
* 如果不一致则不释放锁

核心逻辑：在存入锁时，放入自己线程的标识，在删除锁时，判断当前这把锁的标识是不是自己存入的，如果是，则进行删除，如果不

是，则不进行删除。

![image-20220921144518291](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211445339.png)

核心代码：

加锁：

```java
 public boolean tryLock(long timeoutSec) {
     // 获取线程的标识
     String threadId = ID_PREFIX + Thread.currentThread().getId();
     // 获取锁
     Boolean isSuccess = stringRedisTemplate.opsForValue()
         .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
     return BooleanUtil.isTrue(isSuccess);
 }
```

释放锁：

```java
public void unlock() {
    // 获取线程标识
    String threadId = ID_PREFIX + Thread.currentThread().getId();
    // 获取锁中的标识
    String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    // 判断是否一致
    if (threadId.equals(id)) {
        // 释放锁
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
```

**有关代码实操说明：**

在我们修改完此处代码后，我们重启工程，然后启动两个线程，第一个线程持有锁后，手动释放锁，第二个线程此时进入到锁内部，再

放行第一个线程，此时第一个线程由于锁的value值并非是自己，所以不能释放锁，也就无法删除别人的锁，此时第二个线程能够正确释

放锁，通过这个案例初步说明我们解决了锁误删的问题。

![image-20220921145324594](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211453921.png)

![image-20220921145351602](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211453902.png)

### 4.5 分布式锁的原子性问题

更为极端的误删逻辑说明：

线程1现在持有锁之后，在执行业务逻辑过程中，他正准备删除锁，而且已经走到了条件判断的过程中。比如他**已经拿到了当前这把锁确**

**实是属于他自己的，正准备删除锁，此时发生阻塞，但是此时线程2进来，当线程1阻塞结束后，他直接就会执行删除锁那行代码，即还是**

**存在分布式锁误删的情况**，相当于条件判断并没有起到作用，这就是**删锁时的原子性问题**，之所以有这个问题，是因为**线程1的拿锁，比**

**锁，删锁，实际上并不是原子性的**，我们要防止刚才的情况发生，必须确保操作是原子性的。

![image-20220921150002141](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211500248.png)

### 4.7 Lua脚本解决多条命令原子性问题

Redis提供了Lua脚本功能，**在一个脚本中编写多条Redis命令，确保多条命令执行时的原子性。**

Lua是一种编程语言，它的基本语法参考网站：https://www.runoob.com/lua/lua-tutorial.html

这里重点介绍Redis提供的调用函数，我们可以使用lua去操作redis，又能保证他的原子性。

Redis提供的调用函数

```lua
redis.call('命令名称', 'key', '其它参数', ...)
```

写好脚本以后，需要用Redis命令来调用脚本，调用脚本的常见命令如下：

![image-20220921155902615](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211559661.png)

如果脚本中的key、value不想写死，可以作为参数传递。key类型参数会放入KEYS数组，其它参数会放入ARGV数组，在脚本中可以从

KEYS和ARGV数组获取这些参数：

![image-20220921155922690](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211559747.png)

用Lua脚本改造的的代码：

```lua
-- 这里的 KEYS[1] 就是锁的key，这里的ARGV[1] 就是当前线程标示
-- 获取锁中的标示，判断是否与当前线程标示一致
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 一致 删除
    return redis.call('delete', KEYS[1])
end
-- 不一致 直接返回
return 0
```

### 4.8 利用Java代码调用Lua脚本改造分布式锁

我们的`RedisTemplate`中，可以利用`execute`方法去执行`lua`脚本，参数对应关系就如下图

![image-20220921160256748](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211602865.png)

核心代码：

```java
private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

public void unlock() {
    // 调用lua脚本解决原子性问题
    stringRedisTemplate.execute(
        UNLOCK_SCRIPT,
        Collections.singletonList(KEY_PREFIX + name),
        ID_PREFIX + Thread.currentThread().getId()
    );
}
```

基于Redis的分布式锁实现思路：

- 利用`set nx ex`获取锁，并设置过期时间，保存线程标示

- 释放锁时先判断线程标示是否与自己一致，一致则删除锁，并且采用lua脚本保证原子性

特性：

- 利用`set nx`满足互斥性

- 利用`set ex`保证故障时锁依然能释放，避免死锁，提高安全性

- 利用Redis集群保证高可用和高并发特性

## 5. 分布式锁-redisson

### 5.1 redisson功能介绍

基于`setnx`实现的分布式锁存在下面的问题：

![image-20220921180020068](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211800144.png)

**重入问题**：重入问题是指**获得锁的线程可以再次进入到相同的锁的代码块中**，可重入锁的意义在于防止死锁，比如HashTable这样的代码

中，他的方法都是使用synchronized修饰的，假如他在一个方法内，调用另一个方法，那么此时如果是不可重入的，不就死锁了吗？所以

可重入锁他的主要意义是防止死锁，我们的synchronized和Lock锁都是可重入的。

**不可重试**：是指目前的分布式只能尝试一次，我们认为合理的情况是，**当线程在获得锁失败后，他应该能再次尝试获得锁**。

**超时释放：**我们在加锁时增加了过期时间，这样的我们可以防止死锁，但是如果卡顿的时间超长，虽然我们采用了lua表达式防止删锁的

时候，误删别人的锁，但是毕竟没有锁住，有安全隐患。

**主从一致性：** 如果Redis提供了主从集群，当我们向集群写数据时，主机需要异步的将数据同步给从机，而万一在同步过去之前，主机宕

机了，就会出现死锁问题。

这时就需要Redisson出场。

**Redisson**是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的Java常用对

象，还提供了许多分布式服务，其中就**包含了各种分布式锁的实现**。

官网地址： [https://redisson.org](https://redisson.org/)

GitHub地址： https://github.com/redisson/redisson

![image-20220921180435541](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211804606.png)

### 5.2 redisson快速入门

引入依赖：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.17.6</version>
</dependency>
```

配置Redisson客户端：

```java
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.150.101:6379")
            .setPassword("123321");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
```

使用

```java
void testRedisson() throws InterruptedException {
    // 获取锁(可重入)，指定锁的名称
    RLock lock = redissonClient.getLock("anyLock");
    // 尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
    boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
    // 判断是否获取成功
    if (isLock) {
        try {
            System.out.println("执行业务");
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
}
```

### 5.3 redisson可重入锁原理

在redisson的分布式锁中，他**采用`hash`结构用来存储锁**，其中大key表示表示这把锁是否存在，用小key表示当前这把锁被哪个线程持

有。

**redisson中tryLock中的lua脚本**

```lua
if (redis.call('exists', KEYS[1]) == 0) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1)
    redis.call('pexpire', KEYS[1], ARGV[1])
    return nil
end
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1)
    redis.call('pexpire', KEYS[1], ARGV[1])
    return nil
end
return redis.call('pttl', KEYS[1])
```

redisson添加锁对应的逻辑

![image-20220921182429498](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211824691.png)

**redisson中unLock中的lua脚本**

```lua
if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then
    return nil;
end ;
local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1);
if (counter > 0) then
    redis.call('pexpire', KEYS[1], ARGV[2]);
    return 0;
else
    redis.call('del', KEYS[1]);
    redis.call('publish', KEYS[2], ARGV[1]);
    return 1;
end ;
return nil;
```

redisson删除锁对应的逻辑

![image-20220921182955338](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211829498.png)

### 5.4 redisson锁重试和WatchDog机制

![image-20220921185015304](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211850378.png)

Redisson分布式锁原理：

- **可重入**：利用**`hash`结构**记录**线程id**和**重入次数**

- **可重试**：利用**信号量**和**PubSub**功能实现等待、唤醒，获取锁失败的重试机制

- **超时续约**：利用**watchDog**，每隔一段时间（releaseTime/3），**重置超时时间**

### 5.5 redisson锁的MutiLock原理

redis主从中出现的问题

此时我们去写命令，写在主机上，主机会将数据同步给从机，但是假设在主机还没有来得及把数据写入到从机去的时候，此时**主机宕机**，

哨兵会发现主机宕机，并且选举一个slave变成master，而此时**新的master中实际上并没有锁信息**，此时锁信息就已经丢掉了。

![image-20220921190807568](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211908637.png)

为了解决这个问题，redission提出来了**MutiLock锁**，使用这把锁咱们就**不使用主从了，每个节点的地位都是一样的**， 这把**锁加锁的逻辑**

**需要写入到每一个主丛节点上，只有所有的服务器都写入成功，此时才是加锁成功，假设现在某个节点挂了，那么他去获得锁的时候，只**

**要有一个节点拿不到，都不能算是加锁成功**，就保证了加锁的可靠性。

![image-20220921190942768](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209211909840.png)

代码实现：

首先配置三个节点

```java
@Bean
public RedissonClient redisClient() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://×××.×××.×××.××:6379").setPassword("××××××");
    return Redisson.create(config);
}

@Bean
public RedissonClient redisClient2() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://×××.×××.×××.××:6380");
    return Redisson.create(config);
}
@Bean
public RedissonClient redisClient3() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://×××.×××.×××.××:6381");
    return Redisson.create(config);
}
```

然后进行创建MultiLock锁即可使用

```java
@Resource
private RedissonClient redissonClient;

@Resource
private RedissonClient redissonClient2;

@Resource
private RedissonClient redissonClient3;

@BeforeEach
void setUp() {
    RLock lock1 = redissonClient.getLock("order");
    RLock lock2 = redissonClient2.getLock("order");
    RLock lock3 = redissonClient3.getLock("order");

    // 创建MultiLock锁
    RLock lock = redissonClient.getMultiLock(lock1, lock2, lock3);
}
```

Redis分布式锁总结

- **不可重Redis分布式锁**
    - 原理：利用`setnx`的互斥性；利用`ex`避免死锁；释放锁时判断线程标示
    - 缺陷：不可重入、无法重试、锁超时失效

- **可重入的Redis分布式锁**

    - 原理：利用`hash`结构，记录线程标示和重入次数；利用`watchDog`延续锁时间；利用**信号量**控制锁重试等待

    - 缺陷：`redis`宕机引起锁失效问题

- **Redisson的multiLock**

    - 原理：多个独立的`Redis`节点，必须**在所有节点都获取重入锁，才算获取锁成功**

    - 缺陷：运维成本高、实现复杂

## 6. 秒杀优化

### 6.1 异步秒杀思路

首先回顾下下单流程

当用户发起请求，此时会请求nginx，nginx会访问到tomcat，而tomcat中的程序，会进行串行操作，分成如下几个步骤

1. 查询优惠卷
2. 判断秒杀库存是否足够
3. 查询订单
4. 校验是否是一人一单
5. 扣减库存
6. 创建订单

![image-20220921210521896](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212105965.png)

在这六步操作中，又有很多操作是要去操作数据库的，而且还是一个线程串行执行， 这样就会导致我们的程序执行的很慢，所以我们需

要异步程序执行，那么如何加速呢？

在这里笔者想给大家分享一下课程内没有的思路，看看有没有小伙伴这么想，比如，我们可以不可以使用异步编排来做，或者说我开启N多线程，N多个线程，一个线程执行查询优惠卷，一个执行判断扣减库存，一个去创建订单等等，然后再统一做返回，这种做法和课程中有哪种好呢？答案是课程中的好，因为如果你采用我刚说的方式，如果访问的人很多，那么线程池中的线程可能一下子就被消耗完了，而且你使用上述方案，最大的特点在于，你觉得时效性会非常重要，但是你想想是吗？并不是，比如我只要确定他能做这件事，然后我后边慢慢做就可以了，我并不需要他一口气做完这件事，所以我们应当采用的是课程中，类似消息队列的方式来完成我们的需求，而不是使用线程池或者是异步编排的方式来完成这个需求。

优化方案：**我们将耗时比较短的逻辑判断放入到redis中，比如是否库存足够，比如是否一人一单，这样的操作，只要这种逻辑可以完**

**成，就意味着我们是一定可以下单完成的，我们只需要进行快速的逻辑判断，根本就不用等下单逻辑走完，我们直接给用户返回成功，** 

**再在后台开一个线程，后台线程慢慢的去执行queue里边的消息即可**

![image-20220921220102000](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212201073.png)

整体思路：当用户下单之后，**判断库存是否充足**只需要导redis中去根据key找对应的value是否大于0即可，如果不充足，则直接结束，如

果充足，继续在redis中**判断用户是否可以下单**，如果set集合中没有这条数据，说明他可以下单，如果set集合中没有这条记录，则将

userId和优惠卷存入到redis中，并且返回0，整个过程需要**保证是原子性**的，我们可以使用lua来操作。

![image-20220921220402350](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212204466.png)

### 6.2 Redis完成秒杀资格判断

需求：

* 新增秒杀优惠券的同时，将优惠券信息保存到Redis中

* 基于Lua脚本，判断秒杀库存、一人一单，决定用户是否抢购成功

* 如果抢购成功，将优惠券id和用户id封装后存入阻塞队列

* 开启线程任务，不断从阻塞队列中获取信息，实现异步下单功能

![image-20220921220546421](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212205481.png)

Lua脚本

```lua
-- 1. 参数列表
-- 1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]

-- 2. 数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务
-- 3.1 判断库存是否充足
if (tonumber(redis.call('GET', stockKey)) <= 0) then
    -- 3.2 库存不足 返回1
    return 1
end
-- 3.2 判断用户是否下单
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3 存在 重复下单 返回2
    return 2
end
-- 3.4 扣库存
redis.call('incrby', stockKey, -1)
-- 3.5 下单（保存用户）
redis.call('sadd', orderKey, userId)
-- 成功 返回0
return 0
```

新增秒杀优惠卷改造：

```java
@Transactional(rollbackFor = Exception.class)
public void addSeckillVoucher(Voucher voucher) {
    // 添加优惠卷
    this.save(voucher);
    // 添加秒杀信息
    SeckillVoucher seckillVoucher = new SeckillVoucher();
    seckillVoucher.setVoucherId(voucher.getId());
    seckillVoucher.setStock(voucher.getStock());
    seckillVoucher.setBeginTime(voucher.getBeginTime());
    seckillVoucher.setEndTime(voucher.getEndTime());
    seckillVoucherMapper.insert(seckillVoucher);
    // 保存秒杀库存到redis中
    stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
}
```

核心代码：

```java
@Override
public Result seckillVoucher(Long voucherId) {
    //获取用户
    Long userId = UserHolder.getUser().getId();
    long orderId = redisIdWorker.nextId("order");
    // 1.执行lua脚本
    Long result = stringRedisTemplate.execute(
            SECKILL_SCRIPT,
            Collections.emptyList(),
            voucherId.toString(), userId.toString(), String.valueOf(orderId)
    );
    int r = result.intValue();
    // 2.判断结果是否为0
    if (r != 0) {
        // 2.1.不为0 ，代表没有购买资格
        return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
    }
    //TODO 保存阻塞队列
    // 3.返回订单id
    return Result.ok(orderId);
}
```

### 6.3 基于阻塞队列实现秒杀优化

修改下单动作，现在我们去下单时，是通过lua表达式去原子执行判断逻辑，如果判断我出来不为0 ，则要么是库存不足，要么是重复下

单，返回错误信息，如果是0，则把下单的逻辑保存到队列中去，然后异步执行。

核心代码：

```java
@Autowired
private ISeckillVoucherService seckillVoucherService;

@Autowired
private RedisIdWorker redisIdWorker;

@Autowired
private StringRedisTemplate stringRedisTemplate;

@Autowired
private RedissonClient redissonClient;

private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
}

private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

@PostConstruct
private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
}

private class VoucherOrderHandler implements Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                // 1. 获取队列中的订单信息
                VoucherOrder voucherOrder = orderTasks.take();
                // 2. 创建订单
                handleVoucherOrder(voucherOrder);
            } catch (Exception e) {
                log.error("处理订单异常", e);
            }
        }
    }
}

private void handleVoucherOrder(VoucherOrder voucherOrder) {
    // 1. 获取用户
    Long userId = voucherOrder.getUserId();
    // 2. 创建锁对象
    RLock lock = redissonClient.getLock("lock:order:" + userId);
    // 3. 获取锁
    boolean isLock = lock.tryLock();
    // 4. 判断是否获取锁成功
    if (!isLock) {
        // 获取锁失败 返回错误或重试
        log.error("不允许重复下单");
    }
    try {
        proxy.createVoucherOrder(voucherOrder);
    } finally {
        // 释放锁
        lock.unlock();
    }
}

private IVoucherOrderService proxy;

@Override
public Result seckillVoucher(Long voucherId) {
    // 1. 执行Lua脚本
    Long userId = UserHolder.getUser().getId();
    Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
    // 2. 判断结果是否为0
    int r = result.intValue();
    if (r != 0) {
        // 2.1 不为0 代表没有购买资格
        return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
    }
    // 2.2 为0 有购买资格 把下单信息(用户id,订单id,优惠卷id)存入阻塞队列
    long orderId = redisIdWorker.nextId("order");
    VoucherOrder voucherOrder = new VoucherOrder();
    // 2.3 订单id
    voucherOrder.setId(orderId);
    // 2.4 用户id
    voucherOrder.setUserId(userId);
    // 2.5 代金券id
    voucherOrder.setVoucherId(voucherId);
    // 2.6 保存阻塞队列
    orderTasks.add(voucherOrder);

    // 3. 获取代理对象
    proxy = (IVoucherOrderService) AopContext.currentProxy();
    // 4. 返回订单id
    return Result.ok(orderId);
}


@Override
@Transactional(rollbackFor = Exception.class)
public void createVoucherOrder(VoucherOrder voucherOrder) {
    // 5. 实现一人一单
    Long userId = voucherOrder.getUserId();
    Long voucherId = voucherOrder.getVoucherId();
    // 5.1 查询订单
    Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    // 5.2 判断是否存在
    if (count > 0) {
        // 用户已经购买过
        log.error("不可再次抢购！");
    }
    // 6. 扣减库存
    boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
    if (!isSuccess) {
        log.error("库存不足！");
    }
    // 7 保存订单
    this.save(voucherOrder);
}
```

**小总结：**

秒杀业务的优化思路是什么？

* 先利用Redis完成库存余量、一人一单判断，完成抢单业务
* 再将下单业务放入阻塞队列，利用独立线程异步下单
* 基于阻塞队列的异步秒杀存在哪些问题？
    * 内存限制问题
    * 数据安全问题

## 7. Redis消息队列

### 7.1 认识消息队列

什么是消息队列：字面意思就是存放消息的队列。最简单的消息队列模型包括3个角色：

* **消息队列**：存储和管理消息，也被称为消息代理（Message Broker）
* **生产者**：发送消息到消息队列
* **消费者**：从消息队列获取消息并处理消息

![image-20220921231953931](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212319004.png)

使用队列的好处在于**解耦：**所谓解耦，举一个生活中的例子就是：快递员(生产者)把快递放到快递柜里边(Message Queue)去，我们(消费

者)从快递柜里边去拿东西，这就是一个**异步**，如果耦合，那么这个快递员相当于直接把快递交给你，这事固然好，但是万一你不在家，

那么快递员就会一直等你，这就浪费了快递员的时间，所以这种思想在我们日常开发中，是非常有必要的。

这种场景在我们秒杀中就变成了：我们**下单之后，利用redis去进行校验下单条件，再通过队列把消息发送出去，然后再启动一个线程去**

**消费这个消息**，完成解耦，同时也加快我们的响应速度。

这里我们可以使用一些现成的mq，比如kafka，rabbitmq等等，但是呢，如果没有安装mq，我们也可以直接使用redis提供的mq方案。

Redis提供了三种不同的方式来实现消息队列：

- **list结构**：基于List结构模拟消息队列

- **PubSub**：基本的点对点消息模型

- **Stream**：比较完善的消息队列模型

### 7.2 基于List实现消息队列

消息队列（Message Queue），字面意思就是存放消息的队列。而Redis的list数据结构是一个双向链表，很容易模拟出队列效果。

队列是入口和出口不在一边，因此我们可以利用：`LPUSH`结合`RPOP`、或者`RPUSH`结合`LPOP`来实现。

不过要注意的是，当队列中没有消息时RPOP或LPOP操作会返回null，并不像JVM的阻塞队列那样会阻塞并等待消息。因此这里应该使用

`BRPOP`或者`BLPOP`来实现阻塞效果。

![image-20220921232350888](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209212323960.png)

基于List的消息队列有哪些优缺点？

优点：

* 利用Redis存储，不受限于JVM内存上限
* 基于Redis的持久化机制，数据安全性有保证
* 可以满足消息有序性

缺点：

* 无法避免消息丢失
* 只支持单消费者

### 7.3 基于PubSub的消息队列

PubSub（发布订阅）是Redis2.0版本引入的消息传递模型。顾名思义，消费者可以订阅一个或多个channel，生产者向对应channel发送

消息后，所有订阅者都能收到相关消息。

`SUBSCRIBE channel [channel]` ：订阅一个或多个频道

`PUBLISH channel msg` ：向一个频道发送消息

`PSUBSCRIBE pattern[pattern]` ：订阅与pattern格式匹配的所有频道

![image-20220922153917549](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221539665.png)

基于PubSub的消息队列有哪些优缺点？

优点：

* 采用发布订阅模型，支持多生产、多消费

缺点：

* 不支持数据持久化
* 无法避免消息丢失
* 消息堆积有上限，超出时数据丢失

### 7.4 基于Stream的消息队列

Stream 是 Redis 5.0 引入的一种新数据类型，可以实现一个功能非常完善的消息队列。

发送消息的命令：

![image-20220922160031352](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221600421.png)

例如：

![image-20220922160043863](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221600917.png)

读取消息的方式之一：XREAD

![image-20220922160058253](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221600332.png)

在业务开发中，我们可以循环的调用XREAD阻塞方式来查询最新消息，从而**实现持续监听队列**的效果，伪代码如下

![image-20220922160129983](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221601038.png)

注意：当我们指定起始ID为$时，代表读取最新的消息，如果我们处理一条消息的过程中，又有超过1条以上的消息到达队列，则下次获取

时也只能获取到最新的一条，会出现**漏读消息**的问题。

STREAM类型消息队列的XREAD命令特点：

* 消息可回溯
* 一个消息可以被多个消费者读取
* 可以阻塞读取
* 有消息漏读的风险

### 7.5 基于Stream的消息队列-消费者组

消费者组（Consumer Group）：将多个消费者划分到一个组中，监听同一个队列。具备下列特点：

![image-20220922162443373](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221624433.png)

**创建消费者组**

```redis
XGROUP CREATE key groupName ID [MKSTREAM]
```

key：队列名称

groupName：消费者组名称

ID：起始ID标示，$代表队列中最后一个消息，0则代表队列中第一个消息

MKSTREAM：队列不存在时自动创建队列

 **删除指定的消费者组**

```redis
XGROUP DESTORY key groupName
```

 给指定的消费者组添加消费者

```redis
XGROUP CREATECONSUMER key groupname consumername
```

 删除消费者组中的指定消费者

```redis
XGROUP DELCONSUMER key groupname consumername
```

**从消费者组读取消息**：

```redis
XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] ID [ID ...]
```

* group：消费者组名称
* consumer：消费者名称，如果消费者不存在，会自动创建一个消费者
* count：本次查询的最大数量
* BLOCK milliseconds：当没有消息时最长等待时间
* NOACK：无需手动ACK，获取到消息后自动确认，一般不设置
* STREAMS key：指定队列名称
* ID：获取消息的起始ID
    - ">"：从下一个未消费的消息开始
    - 其它：根据指定id从pending-list中获取已消费但未确认的消息，例如0，是从pending-list中的第一个消息开始

消费者监听消息的基本思路：

![image-20220922163036065](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221630154.png)

STREAM类型消息队列的XREADGROUP命令特点：

- 消息可回溯

- 可以多消费者争抢消息，加快消费速度

- 可以阻塞读取

- 没有消息漏读的风险

- 有消息确认机制，保证消息至少被消费一次

**小总结**

![image-20220922163144391](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209221631454.png)

### 7.6 基于Redis的Stream结构作为消息队列，实现异步秒杀下单

需求：

* 创建一个Stream类型的消息队列，名为stream.orders
* 修改之前的秒杀下单Lua脚本，在认定有抢购资格后，直接向stream.orders中添加消息，内容包含voucherId、userId、orderId
* 项目启动时，开启一个线程任务，尝试获取stream.orders中的消息，完成下单

修改lua表达式

```lua
-- 1. 参数列表
-- 1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2. 数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务
-- 3.1 判断库存是否充足
if (tonumber(redis.call('GET', stockKey)) <= 0) then
    -- 3.2 库存不足 返回1
    return 1
end
-- 3.2 判断用户是否下单
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3 存在 重复下单 返回2
    return 2
end
-- 3.4 扣库存
redis.call('incrby', stockKey, -1)
-- 3.5 下单（保存用户）
redis.call('sadd', orderKey, userId)
-- 3.6 发送消息到队列中，XADD stream.orders * k1 v1 k2 v2
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
-- 成功 返回0
return 0
```

核心代码：

```java
private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
}

private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

@PostConstruct
private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
}

private void handleVoucherOrder(VoucherOrder voucherOrder) {
    // 1. 获取用户
    Long userId = voucherOrder.getUserId();
    // 2. 创建锁对象
    RLock lock = redissonClient.getLock("lock:order:" + userId);
    // 3. 获取锁
    boolean isLock = lock.tryLock();
    // 4. 判断是否获取锁成功
    if (!isLock) {
        // 获取锁失败 返回错误或重试
        log.error("不允许重复下单");
    }
    try {
        proxy.createVoucherOrder(voucherOrder);
    } finally {
        // 释放锁
        lock.unlock();
    }
}

private IVoucherOrderService proxy;

private class VoucherOrderHandler implements Runnable {
    String queueName = "stream.orders";

    @Override
    public void run() {
        while (true) {
            try {
                // 1. 获取消息队列中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams stream.orders >
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                    Consumer.from("g1", "c1"),
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                    StreamOffset.create(queueName, ReadOffset.lastConsumed())
                );
                // 2. 判断消息获取是否成功
                if (list == null || list.isEmpty()) {
                    // 2.1 获取失败，说明没有消息，继续下一次循环
                    continue;
                }
                // 3. 解析消息中的订单信息
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                // 4. 获取成功 可以下单 创建订单
                handleVoucherOrder(voucherOrder);
                // 5. ACK确认 xack stream.orders g1 id
                stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
            } catch (Exception e) {
                log.error("处理订单异常", e);
                handlePendingList();
            }
        }
    }

    private void handlePendingList() {
        while (true) {
            try {
                // 1. 获取pending-list中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams stream.orders 0
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                    Consumer.from("g1", "c1"),
                    StreamReadOptions.empty().count(1),
                    StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                // 2. 判断消息获取是否成功
                if (list == null || list.isEmpty()) {
                    // 2.1 获取失败，说明pending-list没有异常消息，结束循环
                    break;
                }
                // 3. 解析消息中的订单信息
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                // 4. 获取成功 可以下单 创建订单
                handleVoucherOrder(voucherOrder);
                // 5. ACK确认 xack stream.orders g1 id
                stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
            } catch (Exception e) {
                log.error("处理pending-list订单异常", e);
            }
        }
    }
}

/**
 * 利用redis消息队列实现
 *
 * @param voucherId 优惠卷id
 */
@Override
public Result seckillVoucher(Long voucherId) {
    // 1. 执行Lua脚本
    Long userId = UserHolder.getUser().getId();
    long orderId = redisIdWorker.nextId("order");
    Long result = stringRedisTemplate.execute(
        SECKILL_SCRIPT,
        Collections.emptyList(),
        voucherId.toString(), userId.toString(), String.valueOf(orderId)
    );
    // 2. 判断结果是否为0
    int r = result.intValue();
    if (r != 0) {
        // 2.1 不为0 代表没有购买资格
        return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
    }
    // 3. 获取代理对象
    proxy = (IVoucherOrderService) AopContext.currentProxy();
    // 4. 返回订单id
    return Result.ok(orderId);
}
```

## 8. 达人探店

### 8.1 点赞功能

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

### 8.2 点赞排行榜

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

## 9. 好友关注

### 9.1 关注和取关

![image-20220919165327729](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191653890.png)

涉及到的数据库表

```sql
create table tb_follow
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint unsigned                     not null comment '用户id',
    follow_user_id bigint unsigned                     not null comment '关联的用户id',
    create_time    timestamp default CURRENT_TIMESTAMP not null comment '创建时间'
)
    collate = utf8mb4_general_ci;
```

### 9.2 共同关注

实现共同关注，我们需要首先查看其他人的首页，能查看他人的个人信息和探店笔记。

![image-20220919171535861](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191715956.png)

共同关注功能

![image-20220919171637422](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191716485.png)

当然是使用我们之前学习过的**set集合**咯，在set集合中，**有交集并集补集的api**，我们可以把两人的关注的人分别放入到一个set集合中，

然后再通过api去查看这两个set集合中的交集数据。

我们先来改造当前的关注列表，改造原因是因为我们需要在用户关注了某位用户后，需要将数据放入到set集合中，方便后续进行共同关

注，同时当取消关注时，也需要从set集合中进行删除。

核心代码：

```java
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
```

### 9.3 关注推送

#### 9.3.1 Feed流实现方案

当我们关注了用户后，这个用户发了动态，那么我们应该**把这些数据推送给用户**，这个需求，其实我们又把他叫做**Feed流**，关注推送也

叫做Feed流，直译为投喂。为用户持续的提供“沉浸式”的体验，通过无限下拉刷新获取新的信息。例如抖音。

对于**传统的模式**的内容解锁：我们是需要用户去通过**搜索引擎**或者是其他的方式去解锁想要看的内容

![image-20220919180912066](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191809112.png)

对于新型的**Feed流**的的效果：不需要我们用户再去推送信息，而是系统分析用户到底想要什么，然后直接把内容推送给用户，从而使用

户能够更加的节约时间，不用主动去寻找。

![image-20220919180931447](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191809493.png)

Feed流产品有两种常见模式：

**Timeline**：不做内容筛选，简单的按照内容发布时间排序，常用于好友或关注。例如朋友圈。

* 优点：信息全面，不会有缺失。并且实现也相对简单
* 缺点：信息噪音较多，用户不一定感兴趣，内容获取效率低

**智能排序**：利用智能算法屏蔽掉违规的、用户不感兴趣的内容。推送用户感兴趣信息来吸引用户

* 优点：投喂用户感兴趣信息，用户粘度很高，容易沉迷
* 缺点：如果算法不精准，可能起到反作用

我们本次针对好友的操作，采用的就是Timeline的方式，只需要拿到我们关注用户的信息，然后按照时间排序即可，因此采用Timeline的

模式。该模式的实现方案有三种：

* 拉模式
* 推模式
* 推拉结合

**拉模式**：也叫做读扩散

该模式的核心含义就是：当张三和李四和王五发了消息后，都会保存在自己的邮箱中，假设赵六要读取信息，那么他会从读取他自己的收

件箱，此时系统会从他关注的人群中，把他关注人的信息全部都进行拉取，然后在进行排序。

优点：比较节约空间，因为赵六在读信息时，并没有重复读取，而且读取完之后可以把他的收件箱进行清楚。

缺点：比较延迟，当用户读取数据时才去关注的人里边去读取数据，假设用户关注了大量的用户，那么此时就会拉取海量的内容，对服务器压力巨大。

![image-20220919181235518](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191812586.png)

**推模式**：也叫做写扩散。

推模式是没有写邮箱的，当张三写了一个内容，此时会主动的把张三写的内容发送到他的粉丝收件箱中去，假设此时李四再来读取，就不用再去临时拉取了。

优点：时效快，不用临时拉取

缺点：内存压力大，假设一个大V写信息，很多人关注他， 就会写很多分数据到粉丝那边去

![image-20220919181307146](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191813231.png)

**推拉结合模式**：也叫做读写混合，兼具推和拉两种模式的优点。

推拉模式是一个折中的方案，站在**发件人**这一段，如果是个**普通的人**，那么我们采用写扩散的方式，直接把数据写入到他的粉丝中去，因

为普通的人他的粉丝关注量比较小，所以这样做没有压力；如果是**大V**，那么他是直接将数据先写入到一份到发件箱里边去，然后再直接

写一份到活跃粉丝收件箱里边去。现在站在**收件人**这端来看，如果是**活跃粉丝**，那么大V和普通的人发的都会直接写入到自己收件箱里边

来，而如果是**普通的粉丝**，由于他们上线不是很频繁，所以等他们上线时，再从发件箱里边去拉信息。

![image-20220919181519269](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191815347.png)

三者对比

![image-20220919181659898](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191816955.png)

#### 9.3.2 基于推模式-推送到粉丝收件箱

需求：

* 修改新增探店笔记的业务，在保存blog到数据库的同时，推送到粉丝的收件箱
* 收件箱满足可以根据时间戳排序，必须用Redis的数据结构实现
* 查询收件箱数据时，可以实现分页查询

**Feed流中的数据会不断更新，所以数据的角标也在变化，因此不能采用传统的分页模式。**

传统了分页在feed流是不适用的，因为我们的数据会随时发生变化

假设在t1 时刻，我们去读取第一页，此时page = 1 ，size = 5 ，那么我们拿到的就是10~6 这几条记录，假设现在t2时候又发布了一条记录，此时t3 时刻，我们来读取第二页，读取第二页传入的参数是page=2 ，size=5 ，那么此时读取到的第二页实际上是从6 开始，然后是6~2 ，那么我们就读取到了重复的数据，所以feed流的分页，不能采用原始方案来做。

![image-20220919184644335](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191846405.png)

Feed流的滚动分页

我们需要记录每次操作的最后一条，然后从这个位置开始去读取数据

举个例子：我们从t1时刻开始，拿第一页数据，拿到了10~6，然后记录下当前最后一次拿取的记录，就是6，t2时刻发布了新的记录，此时这个11放到最顶上，但是不会影响我们之前记录的6，此时t3时刻来拿第二页，第二页这个时候拿数据，还是从6后一点的5去拿，就拿到了5-1的记录。我们这个地方可以采用**sortedSet**来做，可以进行范围查询，并且还可以记录当前获取数据时间戳最小值，就可以实现滚动分页了。

![image-20220919184701425](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191847493.png)

推送粉丝到redis核心代码：

```java
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
```

#### 9.3.3 实现分页查询收邮箱

需求：在个人主页的“关注”卡片中，查询并展示推送的Blog信息：

具体操作如下：

1、每次查询完成后，我们要分析出查询出数据的最小时间戳，这个值会作为下一次查询的条件

2、我们需要找到与上一次查询相同的查询个数作为偏移量，下次查询时，跳过这些查询过的数据，拿到我们需要的数据

综上：我们的请求参数中就需要携带 lastId：上一次查询的最小时间戳 和偏移量这两个参数。

这两个参数第一次会由前端来指定，以后的查询就根据后台结果作为条件，再次传递到后台。

![image-20220919184846521](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209191848592.png)

![image-20220919203852954](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209192038035.png)

核心代码：

```java
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
```

## 10. 附近商户

### 10.1 GEO数据结构的基本用法

GEO就是Geolocation的简写形式，代表**地理坐标**。Redis在3.2版本中加入了对GEO的支持，允许存储地理坐标信息，帮助我们根据经纬

度来检索数据。常见的命令有：

* **GEOADD**：添加一个地理空间信息，包含：经度（longitude）、纬度（latitude）、值（member）
* **GEODIST**：计算指定的两个点之间的距离并返回
* GEOHASH：将指定member的坐标转为hash字符串形式并返回
* **GEOPOS**：返回指定member的坐标
* GEORADIUS：指定圆心、半径，找到该圆内包含的所有member，并按照与圆心之间的距离排序后返回。6.以后已废弃
* **GEOSEARCH**：在指定范围内搜索member，并按照与指定点之间的距离排序后返回。范围可以是圆形或矩形。6.2.新功能
* GEOSEARCHSTORE：与GEOSEARCH功能一致，不过可以把结果存储到一个指定的key。 6.2.新功能

### 10.2 实现附近商户功能

![image-20220919094545851](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209190945104.png)

当我们点击美食之后，会出现一系列的商家，商家中可以按照多种排序方式，我们此时关注的是距离，这个地方就需要使用到我们的

GEO，向后台传入当前app收集的地址(我们此处是写死的) ，以当前坐标作为圆心，同时绑定相同的店家类型type，以及分页信息，把这

几个条件传入后台，后台查询出对应的数据再返回。

![image-20220919094747035](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209190947126.png)

我们要做的事情是：将数据库表中的数据导入到redis中去，redis中的GEO，GEO在redis中就一个menber和一个经纬度，我们把x和y轴

传入到redis做的经纬度位置去，但我们不能把所有的数据都放入到menber中去，毕竟作为redis是一个内存级数据库，如果存海量数

据，redis还是力不从心，所以我们在这个地方存储他的id即可。

但是这个时候还有一个问题，就是在redis中并没有存储type，所以我们无法根据type来对数据进行筛选，所以我们可以按照商户类型做

分组，类型相同的商户作为同一组，以typeId为key存入同一个GEO集合中即可。

将数据存入redis代码

```java
void loadShopData() {
    // 1. 查询店铺信息
    List<Shop> shopList = shopService.list();
    // 2. 把店铺分组，按照typeId分组，typeId一致的放到一个集合
    Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
    // 3. 存入redis中 geoadd key 经度 维度 member
    for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
        // 3.1.获取类型id
        Long typeId = entry.getKey();
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        // 3.2.获取同类型的店铺的集合
        List<Shop> value = entry.getValue();
        List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
        for (Shop shop : value) {
            // 效率比较低--->改为批量插入
            // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(), new Point(shop.getX(), shop.getY())));
        }
        stringRedisTemplate.opsForGeo().add(key, locations);
    }
}
```

业务核心代码：

```java
public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
    // 1. 判断是否需要根据坐标查询
    if (x == null || y == null) {
        // 不需要坐标查询，按数据库查询
        Page<Shop> page = this.query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
    // 2. 计算分页参数
    int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
    int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
    // 3. 查询redis、按照距离排序、分页。结果：shopId,distance
    String key = RedisConstants.SHOP_GEO_KEY + typeId;
    // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(key,
                                                                                                      GeoReference.fromCoordinate(x, y), new Distance(5, Metrics.KILOMETERS),
                                                                                                      RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
    // 4. 解析出id
    if (results == null) {
        return Result.ok(Collections.emptyList());
    }
    List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
    if (list.size() <= from) {
        //  没有下一页了，结束
        return Result.ok(Collections.emptyList());
    }
    // 4.1 截取 from ~ end的部分
    List<Long> ids = new ArrayList<>(list.size());
    Map<String, Distance> distanceMap = new HashMap<>(list.size());
    list.stream().skip(from).forEach(result -> {
        // 4.2 获取店铺id
        String shopIdStr = result.getContent().getName();
        ids.add(Long.valueOf(shopIdStr));
        // 4.3 获取距离
        Distance distance = result.getDistance();
        distanceMap.put(shopIdStr, distance);
    });
    // 5. 根据id查询Shop
    String idStr = StrUtil.join(",", ids);
    List<Shop> shops = this.query().in("id", ids).last("order by field(id," + idStr + ")").list();
    for (Shop shop : shops) {
        shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
    }
    // 6. 返回
    return Result.ok(shops);
}
```

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


