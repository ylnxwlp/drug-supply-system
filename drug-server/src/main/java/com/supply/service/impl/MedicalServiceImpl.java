package com.supply.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supply.constant.MessageConstant;
import com.supply.entity.FlashSaleDrug;
import com.supply.entity.FlashSaleInformation;
import com.supply.entity.LoginUser;
import com.supply.exception.FlashSaleException;
import com.supply.mapper.MedicalMapper;
import com.supply.mapper.UserMapper;
import com.supply.service.MedicalService;
import com.supply.vo.FlashSaleDrugVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MedicalServiceImpl implements MedicalService {

    private final MedicalMapper medicalMapper;

    private final UserMapper userMapper;

    private final RedisTemplate<Object, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 获取正在抢购和即将开始抢购的药品信息
     *
     * @return 在抢购和即将开始抢购的药品信息
     */
    public List<FlashSaleDrugVO> getFlashSaleDrugs() {
        List<FlashSaleDrugVO> list = (List<FlashSaleDrugVO>) redisTemplate.opsForValue().get("flashSaleDrugs");
        if (list != null) {
            return list;
        } else {
            RLock lock = redissonClient.getLock("information:FlashSaleDrugs");
            boolean locked = false;
            try {
                locked = lock.tryLock(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("getFlashSaleDrugs方法Redis获取分布式锁异常！{}", LocalDateTime.now());
            }
            if (locked) {
                try {
                    log.info("getFlashSaleDrugs方法中获取分布式锁成功！");
                    //再次检查缓存
                    list = (List<FlashSaleDrugVO>) redisTemplate.opsForValue().get("flashSaleDrugs");
                    if (list != null) {
                        return list;
                    }
                    List<FlashSaleDrug> flashSaleDrugs = medicalMapper.getFlashSaleDrugs(LocalDateTime.now());
                    list = new ArrayList<>();
                    if (flashSaleDrugs != null && !flashSaleDrugs.isEmpty()) {
                        for (FlashSaleDrug flashSaleDrug : flashSaleDrugs) {
                            LocalDateTime beginTime = flashSaleDrug.getBeginTime();
                            LocalDateTime endTime = flashSaleDrug.getEndTime();
                            FlashSaleDrugVO flashSaleDrugVO = FlashSaleDrugVO.builder()
                                    .id(flashSaleDrug.getId())
                                    .drugName(flashSaleDrug.getDrugName())
                                    .beginTime(DateUtil.format(beginTime, DatePattern.NORM_DATETIME_PATTERN))
                                    .endTime(DateUtil.format(endTime, DatePattern.NORM_DATETIME_PATTERN))
                                    .userId(flashSaleDrug.getUserId())
                                    .number(flashSaleDrug.getNumber())
                                    .supplyName(userMapper.getUserInformationById(flashSaleDrug.getUserId()).getFirmName()).build();
                            if (beginTime.isAfter(LocalDateTime.now())) {
                                flashSaleDrugVO.setStatus(2);
                            } else {
                                flashSaleDrugVO.setStatus(1);
                            }
                            list.add(flashSaleDrugVO);
                        }
                    }
                    log.info("秒杀商品信息：{}", list);
                    redisTemplate.opsForValue().set("flashSaleDrugs", list, 30 + RandomUtil.randomInt(10), TimeUnit.SECONDS);
                } finally {
                    lock.unlock();
                }
            }
        }
        return list;
    }

    /**
     * 秒杀
     *
     * @param id 秒杀商品id
     */
    public void flashSale(Long id) {
        Long userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        //判断抢购是否开始
        Object o = redisTemplate.opsForValue().get("flashSaleDrug:" + id);
        FlashSaleDrug flashSaleDrug;
        if (o == null) {
            RLock lock = redissonClient.getLock("flashSale:rebuild:drug:" + id);
            boolean b = false;
            try {
                b = lock.tryLock(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error("获取分布式锁异常！");
            }
            if (b) {
                log.info("获取分布式锁成功！");
                o = redisTemplate.opsForValue().get("flashSaleDrug:" + id);
                if (o == null) {
                    flashSaleDrug = medicalMapper.getFlashSaleDrug(id);
                    redisTemplate.opsForValue().set("flashSaleDrug:" + id, flashSaleDrug, 15, TimeUnit.SECONDS);
                    lock.unlock();
                } else {
                    log.info("发现缓存重建完成，直接使用");
                    lock.unlock();
                    flashSaleDrug = JSON.parseObject(o.toString(), FlashSaleDrug.class);
                }
            } else {
                throw new FlashSaleException(MessageConstant.FLASH_SALE_BUSY);
            }
        } else {
            flashSaleDrug = JSON.parseObject(o.toString(), FlashSaleDrug.class);
        }
        LocalDateTime beginTime = flashSaleDrug.getBeginTime();
        LocalDateTime endTime = flashSaleDrug.getEndTime();
        if (beginTime.isAfter(now) || endTime.isBefore(now)) {
            //不在抢购时间内，抛出异常
            throw new FlashSaleException(MessageConstant.FLASH_SALE_TIME_ERROR);
        }
        //先基于分布式锁获取库存和下单情况进行资格校验
        RLock lock = redissonClient.getLock("flashSale");
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (locked) {
                Number n = (Number) redisTemplate.opsForValue().get("flashSale:drug:" + id);
                Long number = n != null ? n.longValue() : null;
                if (number == null) {
                    //逻辑错误
                    log.error("抢购信息未设置到redis！");
                    throw new FlashSaleException(MessageConstant.FLASH_SALE_SERVER_ERROR);
                }
                if (number > 0) {
                    //进行一人一单校验
                    Boolean isMember = redisTemplate.opsForSet().isMember("flashSale:drug:" + id + ":userId", userId);
                    if (Boolean.TRUE.equals(isMember)) {
                        //多次下单，抛出异常
                        throw new FlashSaleException(MessageConstant.FLASH_SALE_REPETITION);
                    } else {
                        //没有下过单，成功抢购
                        //先扣减库存
                        redisTemplate.opsForValue().decrement("flashSale:drug:" + id);
                        //记录用户id
                        redisTemplate.opsForSet().add("flashSale:drug:" + id + ":userId", userId);
                        //生成订单id
                        long baseTime = LocalDateTime.of(2020, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);
                        int timestampInSeconds = (int) (System.currentTimeMillis() / 1000 - baseTime);
                        Long count = redisTemplate.opsForValue().increment(now.getYear() + ":" + now.getMonth() + ":" + now.getDayOfMonth() + ":" + "flashSale:orderNumber");
                        Long orderNumber = (long) timestampInSeconds << 32 | count;
                        //发送消息到mq
                        Duration duration = Duration.between(beginTime, endTime);
                        Long minutes = duration.toMinutes();
                        FlashSaleInformation flashSaleInformation = FlashSaleInformation.builder()
                                .id(id)
                                .orderNumber(orderNumber)
                                .timeOfDuration(minutes)
                                .userId(userId)
                                .orderTime(now)
                                .build();
                        String s = JSONObject.toJSONString(flashSaleInformation);
                        String exchangeName = "flashSale.direct";
                        String routingKey = "flashSaleDirect";
                        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
                        rabbitTemplate.convertAndSend(exchangeName, routingKey, s, message -> {
                            message.getMessageProperties().setMessageId(correlationData.getId());
                            return message;
                        }, correlationData);
                    }
                } else {
                    //库存不足，直接抛出异常
                    throw new FlashSaleException(MessageConstant.FLASH_SALE_END);
                }
            } else {
                //获取分布式锁失败，返回繁忙信息
                throw new FlashSaleException(MessageConstant.FLASH_SALE_BUSY);
            }
        } catch (InterruptedException e) {
            log.error("flashSale业务中获取分布式锁出现异常！");
            throw new FlashSaleException(MessageConstant.FLASH_SALE_BUSY);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取当前登录用户的ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return loginUser.getUser().getId();
    }
}
