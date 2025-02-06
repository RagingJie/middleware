package com.study.redis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAspectJAutoProxy(exposeProxy = true) // exposeProxy：这是一个布尔类型的参数，默认值为 false。当设置为 true 时，表示当前的代理对象将会被暴露给外部，这样可以在内部方法调用时也能够获取到代理对象而不是目标对象本身。这在某些场景下非常有用，例如当你需要在一个对象的方法内部调用另一个方法并且希望该调用也能触发相应的切面逻辑（如事务管理、日志记录等），因为默认情况下，内部方法调用不会经过代理对象，因此也不会触发这些切面逻辑。
@EnableTransactionManagement // 开启事务
@MapperScan("com.study.redis.mapper")
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

}
