package com.adaptiveplanner.planner.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForbiddenController {
    @RequestMapping("/forbidden")
    public String forbidden() {
        return "forbidden";
    }
}
