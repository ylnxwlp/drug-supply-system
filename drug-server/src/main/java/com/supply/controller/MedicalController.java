package com.supply.controller;

import com.supply.constant.MessageConstant;
import com.supply.result.Result;
import com.supply.service.MedicalService;
import com.supply.vo.FlashSaleDrugVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical")
@Tag(name = "医护端部分接口")
@Slf4j
@RequiredArgsConstructor
public class MedicalController {

    private final MedicalService medicalService;

    @GetMapping("/getFlashSaleDrug")
    @Operation(summary = "获取正在抢购和即将开始抢购的药品信息接口")
    @PreAuthorize("hasAuthority('medical:getFlashSaleDrugs')")
    public Result<List<FlashSaleDrugVO>> getFlashSaleDrugs() {
        List<FlashSaleDrugVO> list = medicalService.getFlashSaleDrugs();
        if (list != null) {
            return Result.success(list);
        } else {
            return Result.error(MessageConstant.INTERNET_ERROR);
        }
    }

    @PostMapping("/flashSale")
    @Operation(summary = "秒杀接口")
    @PreAuthorize("hasAuthority('medical:flashSale')")
    public Result<Object> flashSale(@RequestParam Long id) {
        medicalService.flashSale(id);
        return Result.success();
    }
}
