package com.study.redis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.core.annotation.Order;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User implements Serializable {

    private static final long serialVersionUID = 12321893768213L;

    private String name;
    private Integer age;
    private String sex;

}
