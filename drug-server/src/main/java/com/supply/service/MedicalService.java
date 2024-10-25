package com.supply.service;

import com.supply.vo.FlashSaleDrugVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MedicalService {

    List<FlashSaleDrugVO> getFlashSaleDrugs();

    void flashSale(Long id);
}
