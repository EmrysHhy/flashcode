package com.bitejiuyeke.bitecommonredis.service;

import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisService {

    @Autowired
    private RedisTemplate redisTemplate;

    //*********************** 基本操作 **************************
    public Boolean expire(final String key, final long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }


    //redis设置数据有效时间（可指定时间单位）
    public Boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true=存在；false=不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 根据提供的键模式查找 Redis 中匹配的键
     *
     * @param pattern 要查找的键的模式
     * @return 键列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }


    /**
     * 重命名key
     *
     * @param oldKey 原来key
     * @param newKey 新key
     */
    public void renameKey(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 删除单个数据
     *
     * @param key 缓存的键值
     * @return 是否成功  true=删除成功；false=删除失败
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除多个数据
     *
     * @param collection 多个数据对应的缓存的键值
     * @return 是否删除了对象 true=删除成功；false=删除失败
     */
    public boolean deleteObject(final Collection collection) {
        return redisTemplate.delete(collection) > 0;
    }


    //*********************** 操作String类型 **************************
    //修饰符  返回值类型 函数名  参数列表  函数体
    //    缓存String数据     key value  test aaa
    //（将数据转为Json字符串存入）
    /**
     * 缓存String数据 （将数据转为Json字符串存入）
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param <T> 对象类型
     */
    public <T> void setCacheObject(final String key, final T value) {

        redisTemplate.opsForValue().set(key, value);
    }

//    缓存String数据，并设置有效时间
//    （将数据转为Json字符串存入）
    /**
     * 缓存String数据，并设置有效时间 （将数据转为Json字符串存入）
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间单位
     * @param <T> 对象类型
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

//    缓存String数据，如果该键不存在则存储，若已存在则不存储。
    /**
     * 缓存String数据，如果该键不存在则存储，若已存在则不存储。
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param <T> 对象类型
     * @return 是否缓存了对象   如果key已经存在，则返回false，否则返回true
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 缓存String数据，如果该键不存在则存储,并设置数据有效时间，若已存在则不存储。
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间单位
     * @param <T> 对象类型
     * @return 是否缓存了对象   如果key已经存在，则返回false，否则返回true
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

//    获得缓存的数据（将缓存的数据反序列化为指定类型返回）
    //key : test  value
    /**
     * 获得缓存的数据（将缓存的数据反序列化为指定类型返回）
     * @param key 缓存键值
     * @param clazz 对应数据的类
     * @return 缓存键值对应的数据
     * @param <T> 对应数据的类型
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        //返回值类型 = 想要获取的数据的类型
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Object o = valueOperations.get(key);
        if (o == null) {
            return null;
        }
        String jsonStr = JsonUtil.obj2String(o);
        return JsonUtil.string2Obj(jsonStr, clazz);
    }

    /**
     * 获得缓存的数据 （将缓存的数据反序列化为指定类型返回，支持复杂的泛型）
     * @param key 缓存键值
     * @param valueTypeRef 类型模板
     * @return 缓存键值对应的数据
     * @param <T> 对象类型
     */
    public <T> T getCacheObject(final String key, TypeReference<T> valueTypeRef) {
        //返回值类型 = 想要获取的数据的类型
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Object o = valueOperations.get(key);
        if (o == null) {
            return null;
        }
        String jsonStr = JsonUtil.obj2String(o);
        return JsonUtil.string2Obj(jsonStr, valueTypeRef);
    }


    //*********************** 操作List类型 **************************
    //修饰符  返回值类型 函数名  参数列表  函数体
    //缓存List数据（插入整个list数据，并保持原顺序）F
    /**
     * 缓存List数据
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 添加元素后 Redis 列表的长度
     * @param <T> 对象类型
     */
    public <T> Long setCacheList(final String key, final List<T> dataList) {
//        redisTemplate.opsForList().leftPushAll()
        //rightPushAll 的返回值指的是：添加之后，Redis中当前操作的这个list结构的长度
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 :count;
    }

    /**
     * 从List结构左侧插入数据（头插、入队）
     * @param key key
     * @param value 缓存的对象
     * @param <T> 值类型
     */
    public <T> void leftPushForList(final String key, final T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从List结构右侧插入数据（尾插、插入单个数据）
     * @param key key
     * @param value 缓存的对象
     * @param <T> 值类型
     */
    public <T> void rightPushForList(final String key, final T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 删除左侧第一个数据 （头删）
     * @param key   key
     */
    public void leftPopForList(final String key) {
        redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 删除右侧第一个数据 （尾删）
     * @param key   key
     */
    public void rightPopForList(final String key) {
        redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 移除List第一个匹配的元素
     *
     * @param key key
     * @param value 值
     * @param <T> 值类型
     */
    public <T> void removeForList(final String key, T value) {
        redisTemplate.opsForList().remove(key, 1L, value);
        //删除的方向： > 0 从左往右   <0 从右往左
        //key : redis key
        ///count :删除的方向 & 删除个数（count绝对值的大小  1 -1 =》 1   2 -2 =》2）   count > 0 从左往右删除  count < 0 从右往左删除
        //count = 0 代表全部删除
    }

    /**
     * 移除List中匹配的所有列表元素
     *
     * @param key key
     * @param value 值
     * @param <T> 值类型
     */
    public <T> void removeAllForList(final String key, T value) {
        redisTemplate.opsForList().remove(key, 0, value);
    }

    /**
     * 移除指定列表中的所有元素
     *
     * @param key key
     */
    public void removeForAllList(final String key) {
        redisTemplate.opsForList().trim(key, -1, 0);
        // 裁剪列表。    list   0      9          10个元素    start: 3  end: 5
        // key
        //0 代表第一个元素  1 ：第二个元素  2 第三个元素 依次类推    -1 最后一个元素  -2 倒数第二个元素  依次类推
        //start ： 起始索引（下标）
        //end : 结束索引（下标）
    }

    /**
     *  修改指定下标数据
     * @param key        key
     * @param index     下标
     * @param newValue  修改后新值
     * @param <T>       值类型
     */
    public <T> void setElementAtIndex(final String key, int index, T newValue) {
        redisTemplate.opsForList().set(key, index, newValue);
    }

    /**
     * 获得缓存的list对象
     * @param key key 缓存的键值
     * @param clazz 对象的类
     * @return 列表
     * @param <T> 对象类型
     */
//    Set data = redisTemplate.opsForZSet().range(key, 0, -1);
    //range ：获取指定范围的数据
    //有序性
    public <T> List<T> getCacheList(final String key, Class<T> clazz) {
        List list = redisTemplate.opsForList().range(key, 0, -1);
        return JsonUtil.string2List(JsonUtil.obj2String(list), clazz);
        //key  : redis的key
        //0 代表第一个元素  1 ：第二个元素  2 第三个元素 依次类推    -1 最后一个元素  -2 倒数第二个元素  依次类推
        //start ： 起始索引（下标）
        //end : 结束索引（下标）
    }

    /**
     * 获得缓存的list对象 （支持复杂的泛型嵌套）
     * @param key key信息
     * @param typeReference 类型模板
     * @return list对象
     * @param <T> 对象类型
     */
    public <T> List<T> getCacheList(final String key, TypeReference<List<T>> typeReference) {
        List list = redisTemplate.opsForList().range(key, 0, -1);
        List<T> res = JsonUtil.string2Obj(JsonUtil.obj2String(list), typeReference);
        return res;
    }

    /**
     * 根据范围获取List
     *
     * @param key key
     * @param start 开始位置
     * @param end 结束位置
     * @param clazz 类信息
     * @return List列表
     * @param <T> 类型
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, Class<T> clazz) {
        List range = redisTemplate.opsForList().range(key, start, end);
        return JsonUtil.string2List(JsonUtil.obj2String(range), clazz);
    }

    /**
     * 根据范围获取List（支持复杂的泛型嵌套 ）
     *
     * @param key key
     * @param start 开始
     * @param end 结果
     * @param typeReference 类型模板
     * @return list列表
     * @param <T> 类型信息
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, TypeReference<List<T>> typeReference) {
        List range = redisTemplate.opsForList().range(key, start, end);
        return JsonUtil.string2Obj(JsonUtil.obj2String(range), typeReference);
    }

    /**
     * 获取指定列表长度
     * @param key key信息
     * @return 列表长度
     */
    public long getCacheListSize(final String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0L : size;
    }
    /**
     * 保留列表末尾 maxLen 条；不足则全部保留
     */
    public void trimList(final String key, final int maxLen) {
        if (maxLen <= 0) {
            return;
        }
        redisTemplate.opsForList().trim(key, -maxLen, -1);
    }

    //************************ 操作Set类型 ***************************
    /**
     * set添加元素（批量添加或添加单个元素）
     * @param key key
     * @param member 元素信息
     */
    public void addMember(final String key, Object... member) {
        redisTemplate.opsForSet().add(key, member);
    }

    /**
     * 删除元素
     * @param key key
     * @param member 元素信息
     */
    public void deleteMember(final String key, Object... member) {
        redisTemplate.opsForSet().remove(key, member);
    }


    /**
     * 获取set数据（支持复杂的泛型嵌套）
     * @param key key
     * @param typeReference 类型模板
     * @return set数据
     * @param <T> 类型信息
     */
    public <T> Set<T> getCacheSet(final String key, TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForSet().members(key);
        return JsonUtil.string2Obj(JsonUtil.obj2String(data), typeReference);
    }

    //************************ 操作ZSet类型 ***************************
    /**
     * 添加元素
     * @param key key
     * @param value 值
     * @param seqNo 分数
     */
    public void addMemberZSet(String key, Object value, double seqNo) {
        redisTemplate.opsForZSet().add(key, value, seqNo);
    }

    /**
     * 删除元素
     * @param key    key
     * @param value  值
     */
    public void delMemberZSet(String key, Object value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 根据排序分值删除
     *
     * @param key key
     * @param minScore 最小分
     * @param maxScore 最大分
     */
    public void removeZSetByScore(final String key, double minScore, double maxScore) {
        redisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
    }


    /**
     * 获取有序集合数据（支持复杂的泛型嵌套）
     *
     * @param key key信息
     * @param typeReference 类型模板
     * @return 有序集合
     * @param <T> 对象类型
     */
    public <T> Set<T> getCacheZSet(final String key, TypeReference<LinkedHashSet<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().range(key, 0, -1);
        return JsonUtil.string2Obj(JsonUtil.obj2String(data), typeReference);
    }

    /**
     * 降序获取有序集合（支持复杂的泛型嵌套）
     * @param key key信息
     * @param typeReference 类型模板
     * @return 降序的有序集合
     * @param <T> 对象类型信息
     */
    public <T> Set<T> getCacheZSetDesc(final String key, TypeReference<LinkedHashSet<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        return JsonUtil.string2Obj(JsonUtil.obj2String(data), typeReference);
    }

    //************************ 操作Hash类型 ***************************
    /**
     * 缓存Map数据
     * @param key key
     * @param dataMap map
     * @param <T> 对象类型
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 往Hash中存入单个数据
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     * @param <T> 对象类型
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获取缓存的map数据（支持复杂的泛型嵌套）
     * @param key key
     * @param typeReference 类型模板
     * @return hash对应的map
     * @param <T> 对象类型
     */
    public <T> Map<String, T> getCacheMap(final String key, TypeReference<Map<String, T>> typeReference) {
        Map data= redisTemplate.opsForHash().entries(key);
        return JsonUtil.string2Obj(JsonUtil.obj2String(data), typeReference);
    }

    /**
     * 获取Hash中的单个数据
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     * @param <T> 对象类型
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取Hash中的多个数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @param typeReference 对象模板
     * @return 获取的多个数据的集合
     * @param <T> 对象类型
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<String> hKeys, TypeReference<List<T>> typeReference) {
        List data = redisTemplate.opsForHash().multiGet(key, hKeys);

        return JsonUtil.string2Obj(JsonUtil.obj2String(data), typeReference);
    }


    //******************************** LUA脚本 ***********************************
    /**
     * 删除指定值对应的 Redis 中的键值（compare and delete）
     *
     * @param key   缓存key
     * @param value value
     * @return 是否完成了比较并删除
     */
    public boolean cad(String key, String value) {
        if (key.contains(StringUtils.SPACE) || value.contains(StringUtils.SPACE)) {
            return false;
        }

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        // 通过lua脚本原子验证令牌和删除令牌
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                value);
        return !Objects.equals(result, 0L);
    }
    //******************************** flashcode ***********************************
    public <T> void add(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }
}
