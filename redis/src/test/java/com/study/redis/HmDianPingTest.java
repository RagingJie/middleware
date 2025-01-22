package com.study.redis;

import com.study.redis.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HmDianPingTest {

    @Autowired
    private ShopServiceImpl shopService;

    @Test
    public void test1() {
        shopService.saveShopToRedis(1L, 10L);
    }

}
