package com.checkers.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GameController {

    @RequestMapping(value = "/")
    public String home(){
        return "home";
    }
}
