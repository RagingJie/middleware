package com.study.redis;

import com.study.redis.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class RedisIdTest {

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void testNextId() throws InterruptedException {
        // 用于让一个或多个线程等待其他线程完成操作。
        // CountDownLatch 通过一个计数器来实现同步，计数器的初始值由构造函数指定
        // 线程通过调用 countDown() 方法减少计数器的值，而其他线程可以通过 await() 方法阻塞，直到计数器减到 0。
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("operator");
                System.out.println("id => " + id);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }

        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("耗时 => " + (end - begin));
    }
}
