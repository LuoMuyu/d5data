package com.luomuyu.d5data;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@SpringBootApplication
public class JavaApplication {

    public static TimedCache<String, String> timedCache = CacheUtil.newTimedCache(1800000);
    public static void main(String[] args) {
        timedCache.schedulePrune(30000);
        SpringApplication.run(JavaApplication.class, args);
    }

}

@Controller
class GetController {
    @GetMapping("/")
    public String HelloWorld(@CookieValue(value = "username", defaultValue = "") String username, @CookieValue(value = "token", defaultValue = "") String token) {
        if ((!token.equals("")) && (username != null) && (token.equals(JavaApplication.timedCache.get(username)))) {
            return "index";
        }else {
            return "redirect:/login";
        }
    }

    @RequestMapping("/login")
    public String HelloWorld2(@CookieValue(value = "username", defaultValue = "") String username, @CookieValue(value = "token", defaultValue = "") String token) {
        if ((!token.equals("")) && (username != null) && (token.equals(JavaApplication.timedCache.get(username)))) {
            return "redirect:/";
        }else {
            return "login";
        }
    }
}

@RestController
class PostController {
    @PostMapping("/login")
    public HashMap login(HttpServletResponse response, @CookieValue(value = "token", defaultValue = "") String token, @RequestParam(value = "username", required = false) String username, @RequestParam(value = "password", required = false) String password) {
        HashMap<String, Object> model = new HashMap<>();
        if ((!token.equals("")) && (username != null) && (token.equals(JavaApplication.timedCache.get(username)))) {
            model.put("code", 1);
            model.put("message", "已登陆!");
        } else {
            if (username == null) {
                model.put("code", -1);
                model.put("message", "用户名为空!");
                model.put("token", null);
            } else if (password == null) {
                model.put("code", -2);
                model.put("message", "密码为空!");
                model.put("token", null);
            } else if ((username.equals("test")) && (password.equals("123456"))) {
                if (JavaApplication.timedCache.get(username) != null) {
                    token = JavaApplication.timedCache.get(username);
                } else {
                    token = new Token().CreateToken();
                    JavaApplication.timedCache.put(username, token);
                }
                model.put("code", 0);
                model.put("message", "登陆成功!");
                model.put("token", token);
                Cookies cookies = new Cookies();
                cookies.setCookie(response, "username", username, 1800);
                cookies.setCookie(response, "token", token, 1800);
            } else {
                model.put("code", -412);
                model.put("message", "用户名或密码错误!");
                model.put("token", null);
            }
        }
        model.put("time", new Time().GetTimeStamp());
        return model;
    }

    @PostMapping("/logout")
    public HashMap login(HttpServletResponse response, @CookieValue(value = "username", defaultValue = "") String username, @CookieValue(value = "token", defaultValue = "") String token) {
        HashMap<String, Object> model = new HashMap<>();
        if ((!token.equals("")) && (username != null) && (token.equals(JavaApplication.timedCache.get(username)))) {
            JavaApplication.timedCache.put(username, token, 1);
            Cookies cookies = new Cookies();
            cookies.setCookie(response, "username", username, -1);
            cookies.setCookie(response, "token", token, -1);
            model.put("code", 0);
            model.put("message", "注销成功!");
        } else {
            model.put("code", -1);
            model.put("message", "未登录!");
        }
        model.put("time", new Time().GetTimeStamp());
        return model;
    }
}

class Cookies {
    public void setCookie(HttpServletResponse response, String key, String value, int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}

class Token {
    public String CreateToken() {
        return RandomUtil.randomString(32);
    }
}

class Time {
    public long GetTimeStamp() {
        return System.currentTimeMillis();
    }
}
