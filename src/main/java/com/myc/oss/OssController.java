package com.myc.oss;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OssController {

    @GetMapping("/hello")
    public String hello() {

        return "helloWorld";

    }


}
