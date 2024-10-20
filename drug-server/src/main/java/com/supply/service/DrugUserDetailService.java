package com.supply.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface DrugUserDetailService {

    UserDetails loadUserByUsernameAndFirmName(String username, String firmName);
}
