package com.supply.consumer;

import com.alibaba.fastjson.JSON;
import com.supply.entity.FlashSale;
import com.supply.entity.FlashSaleInformation;
import com.supply.mapper.MedicalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.DatePattern;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class FlashSaleConsumer {

    private final RedisTemplate<Object, Object> redisTemplate;

    private final MedicalMapper medicalMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "flashSale.queue"),
            exchange = @Exchange(name = "flashSale.direct"),
            key = {"flashSaleDirect"}
    ))
    public void listenFinishQueue(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String messageBody = new String(message.getBody());
        log.info("flashSale.queue的消息ID：{}, 消息内容：{}, 时间：{}",
                messageId, messageBody, DateUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN));
        // 持久化信息进数据库
        FlashSaleInformation flashSaleInformation = JSON.parseObject(messageBody, FlashSaleInformation.class);
        Object o = redisTemplate.opsForValue().get("message:" + messageId);
        FlashSale flashSale = FlashSale.builder()
                .flashSaleDrugId(flashSaleInformation.getId())
                .orderNumber(flashSaleInformation.getOrderNumber().toString())
                .userId(flashSaleInformation.getUserId())
                .status(2)
                .orderTime(flashSaleInformation.getOrderTime())
                .build();
        if (o == null) {
            //未被消费，直接进行持久化
            medicalMapper.storeFlashSaleInformation(flashSale);
            redisTemplate.opsForValue().set("message:" + messageId, 1, flashSaleInformation.getTimeOfDuration() + 5L, TimeUnit.MINUTES);
        }
    }
}
