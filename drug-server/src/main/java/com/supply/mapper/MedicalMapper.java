package com.supply.mapper;

import com.supply.entity.FlashSale;
import com.supply.entity.FlashSaleDrug;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MedicalMapper {

    /**
     * 获取所有符合条件的抢购药品信息
     *
     * @param now 当前时间
     * @return 抢购药品信息
     */
    @Select("select * from flash_sale_drug where (begin_time > #{now} or (begin_time < #{now} and end_time > #{now})) and number > 0")
    List<FlashSaleDrug> getFlashSaleDrugs(LocalDateTime now);

    /**
     * 获取当前抢购药品信息
     *
     * @param id 药品id
     * @return 抢购药品信息
     */
    @Select("select * from flash_sale_drug where id = #{id}")
    FlashSaleDrug getFlashSaleDrug(Long id);

    /**
     * 秒杀信息存储
     *
     * @param flashSale 秒杀信息
     */
    @Insert("insert into flash_sale(flash_sale_drug_id, user_id, order_number, order_time, status) VALUES (#{flashSaleDrugId},#{userId},#{orderNumber},#{orderTime},#{status})")
    void storeFlashSaleInformation(FlashSale flashSale);

    /**
     * 扣减库存
     * @param id 商品库存
     */
    @Update("update flash_sale_drug set number = number - 1 where id = #{id}")
    void inventoryDeduction(Long id);
}
