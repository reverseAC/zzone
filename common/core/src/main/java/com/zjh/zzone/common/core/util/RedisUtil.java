package com.zjh.zzone.common.core.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类
 * 封装常用 Redis 操作
 */
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;
    private final HashOperations<String, String, Object> hashOps;
    private final ListOperations<String, Object> listOps;
    private final SetOperations<String, Object> setOps;
    private final ZSetOperations<String, Object> zSetOps;

    @Autowired
    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.hashOps = redisTemplate.opsForHash();
        this.listOps = redisTemplate.opsForList();
        this.setOps = redisTemplate.opsForSet();
        this.zSetOps = redisTemplate.opsForZSet();
    }

    // ==================== 通用操作 ====================

    /**
     * 设置 key 过期时间
     * @param key 键
     * @param timeout 时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            if (timeout > 0) {
                return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Redis expire error", e);
        }
    }

    /**
     * 获取 key 过期时间
     * @param key 键
     * @return 剩余时间(秒)
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -1 : expire;
    }

    /**
     * 判断 key 是否存在
     * @param key 键
     * @return true=存在 false=不存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new RuntimeException("Redis hasKey error", e);
        }
    }

    /**
     * 删除 key
     * @param key 键
     * @return 删除是否成功
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            throw new RuntimeException("Redis delete error", e);
        }
    }

    /**
     * 批量删除 key
     * @param keys 键集合
     * @return 成功删除的数量
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count == null ? 0 : count;
        } catch (Exception e) {
            throw new RuntimeException("Redis batch delete error", e);
        }
    }

    /**
     * 获取 key 的类型
     * @param key 键
     * @return 数据类型
     */
    public DataType type(String key) {
        return redisTemplate.type(key);
    }

    // ==================== String 操作 ====================

    /**
     * 设置键值对
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        try {
            valueOps.set(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Redis set error", e);
        }
    }

    /**
     * 设置键值对并设置过期时间
     * @param key 键
     * @param value 值
     * @param timeout 时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            valueOps.set(key, value, timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException("Redis set with expire error", e);
        }
    }

    /**
     * 获取值
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            return valueOps.get(key);
        } catch (Exception e) {
            throw new RuntimeException("Redis get error", e);
        }
    }

    /**
     * 值递增
     * @param key 键
     * @param delta 增量(大于0)
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        try {
            Long value = valueOps.increment(key, delta);
            return value == null ? 0 : value;
        } catch (Exception e) {
            throw new RuntimeException("Redis increment error", e);
        }
    }

    /**
     * 值递减
     * @param key 键
     * @param delta 减量(大于0)
     * @return 递减后的值
     */
    public long decrement(String key, long delta) {
        try {
            Long value = valueOps.decrement(key, delta);
            return value == null ? 0 : value;
        } catch (Exception e) {
            throw new RuntimeException("Redis decrement error", e);
        }
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 键值对
     * @param key 键
     * @param field 字段
     * @param value 值
     */
    public void hSet(String key, String field, Object value) {
        try {
            hashOps.put(key, field, value);
        } catch (Exception e) {
            throw new RuntimeException("Redis hSet error", e);
        }
    }

    /**
     * 批量设置 Hash 键值对
     * @param key 键
     * @param map 字段-值映射
     */
    public void hSetAll(String key, Map<String, Object> map) {
        try {
            hashOps.putAll(key, map);
        } catch (Exception e) {
            throw new RuntimeException("Redis hSetAll error", e);
        }
    }

    /**
     * 获取 Hash 字段值
     * @param key 键
     * @param field 字段
     * @return 值
     */
    public Object hGet(String key, String field) {
        try {
            return hashOps.get(key, field);
        } catch (Exception e) {
            throw new RuntimeException("Redis hGet error", e);
        }
    }

    /**
     * 获取 Hash 所有字段值
     * @param key 键
     * @return 字段-值映射
     */
    public Map<String, Object> hGetAll(String key) {
        try {
            return hashOps.entries(key);
        } catch (Exception e) {
            throw new RuntimeException("Redis hGetAll error", e);
        }
    }

    /**
     * 删除 Hash 字段
     * @param key 键
     * @param fields 字段数组
     * @return 删除的字段数量
     */
    public long hDelete(String key, Object... fields) {
        try {
            Long count = hashOps.delete(key, fields);
            return count == null ? 0 : count;
        } catch (Exception e) {
            throw new RuntimeException("Redis hDelete error", e);
        }
    }

    /**
     * 判断 Hash 中是否存在字段
     * @param key 键
     * @param field 字段
     * @return true=存在 false=不存在
     */
    public boolean hExists(String key, String field) {
        try {
            return Boolean.TRUE.equals(hashOps.hasKey(key, field));
        } catch (Exception e) {
            throw new RuntimeException("Redis hExists error", e);
        }
    }

    /**
     * Hash 字段值递增
     * @param key 键
     * @param field 字段
     * @param delta 增量
     * @return 递增后的值
     */
    public long hIncrement(String key, String field, long delta) {
        try {
            Long value = hashOps.increment(key, field, delta);
            return value == null ? 0 : value;
        } catch (Exception e) {
            throw new RuntimeException("Redis hIncrement error", e);
        }
    }

    // ==================== List 操作 ====================

    /**
     * 获取列表指定范围内的元素
     * @param key 键
     * @param start 开始索引
     * @param end 结束索引 (0到-1表示所有元素)
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            return listOps.range(key, start, end);
        } catch (Exception e) {
            throw new RuntimeException("Redis lRange error", e);
        }
    }

    /**
     * 获取列表长度
     * @param key 键
     * @return 列表长度
     */
    public long lSize(String key) {
        try {
            Long size = listOps.size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            throw new RuntimeException("Redis lSize error", e);
        }
    }

    /**
     * 通过索引获取列表元素
     * @param key 键
     * @param index 索引
     * @return 元素
     */
    public Object lIndex(String key, long index) {
        try {
            return listOps.index(key, index);
        } catch (Exception e) {
            throw new RuntimeException("Redis lIndex error", e);
        }
    }

    /**
     * 列表左侧插入元素
     * @param key 键
     * @param value 值
     * @return 插入后列表长度
     */
    public long lLeftPush(String key, Object value) {
        try {
            Long size = listOps.leftPush(key, value);
            return size == null ? 0 : size;
        } catch (Exception e) {
            throw new RuntimeException("Redis lLeftPush error", e);
        }
    }

    /**
     * 列表右侧插入元素
     * @param key 键
     * @param value 值
     * @return 插入后列表长度
     */
    public long lRightPush(String key, Object value) {
        try {
            Long size = listOps.rightPush(key, value);
            return size == null ? 0 : size;
        } catch (Exception e) {
            throw new RuntimeException("Redis lRightPush error", e);
        }
    }

    /**
     * 列表左侧弹出元素
     * @param key 键
     * @return 弹出的元素
     */
    public Object lLeftPop(String key) {
        try {
            return listOps.leftPop(key);
        } catch (Exception e) {
            throw new RuntimeException("Redis lLeftPop error", e);
        }
    }

    // ==================== Set 操作 ====================

    /**
     * 向集合添加元素
     * @param key 键
     * @param values 值数组
     * @return 成功添加的数量
     */
    public long sAdd(String key, Object... values) {
        try {
            Long count = setOps.add(key, values);
            return count == null ? 0 : count;
        } catch (Exception e) {
            throw new RuntimeException("Redis sAdd error", e);
        }
    }

    /**
     * 获取集合所有元素
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        try {
            return setOps.members(key);
        } catch (Exception e) {
            throw new RuntimeException("Redis sMembers error", e);
        }
    }

    /**
     * 判断元素是否在集合中
     * @param key 键
     * @param value 值
     * @return true=存在 false=不存在
     */
    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(setOps.isMember(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Redis sIsMember error", e);
        }
    }

    /**
     * 获取集合大小
     * @param key 键
     * @return 集合大小
     */
    public long sSize(String key) {
        try {
            Long size = setOps.size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            throw new RuntimeException("Redis sSize error", e);
        }
    }

    // ==================== ZSet 操作 ====================

    /**
     * 向有序集合添加元素
     * @param key 键
     * @param value 值
     * @param score 分数
     * @return 是否添加成功
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            return Boolean.TRUE.equals(zSetOps.add(key, value, score));
        } catch (Exception e) {
            throw new RuntimeException("Redis zAdd error", e);
        }
    }

    /**
     * 获取有序集合指定范围内的元素
     * @param key 键
     * @param start 开始位置
     * @param end 结束位置
     * @return 元素集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return zSetOps.range(key, start, end);
        } catch (Exception e) {
            throw new RuntimeException("Redis zRange error", e);
        }
    }

    /**
     * 获取有序集合大小
     * @param key 键
     * @return 集合大小
     */
    public long zSize(String key) {
        try {
            Long size = zSetOps.size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            throw new RuntimeException("Redis zSize error", e);
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量获取键值
     * @param keys 键集合
     * @return 键值映射
     */
    public Map<String, Object> multiGet(Collection<String> keys) {
        try {
            if (CollectionUtils.isEmpty(keys)) {
                return Collections.emptyMap();
            }
            List<Object> values = valueOps.multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new HashMap<>(keys.size());
            int index = 0;
            for (String key : keys) {
                if (index < values.size()) {
                    result.put(key, values.get(index));
                }
                index++;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Redis multiGet error", e);
        }
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁
     * @param lockKey 锁键
     * @param requestId 请求标识
     * @param expireTime 过期时间(秒)
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        try {
            return Boolean.TRUE.equals(valueOps.setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException("Redis tryLock error", e);
        }
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁键
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String requestId) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(lockKey),
                    requestId
            );
            return result != null && result > 0;
        } catch (Exception e) {
            throw new RuntimeException("Redis releaseLock error", e);
        }
    }
}
