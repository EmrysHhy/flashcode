package com.bitejiuyeke.bitemstemplateservice.test;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitemstemplateservice.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {

    // /test/info
    @GetMapping("/info")
    public void info() {
        log.info("接口调用测试");
    }

//    @GetMapping("/result")
//    public R<Void> result(int id) {
//        if (id < 0) {
//            return R.fail();
//        }
//        return R.ok();
//    }

    @GetMapping("/result")
    public R<User> result(int id) {
        if (id < 0) {
            return R.fail(ResultCode.TOKEN_CHECK_FAILED.getCode(), ResultCode.TOKEN_CHECK_FAILED.getMsg());
        }
        User user = new User();
        user.setAge(50);
        user.setName("张三");
        return R.ok(user);
    }

//    @GetMapping("/result")
//    public R<Exam> getQuestion(int id) {
//        if (id < 0) {
//            return R.fail();
//        }
//        return R.ok();
//    }

//    @GetMapping("/exception")
//    public R<Void> exception(int id) {
//        if (id < 0) {
//            throw new ServiceException(ResultCode.INVALID_CODE);
//        }
//        if (id == 1) {
//            throw new ServiceException("id不能为1");
//        }
//        if (id == 1000) {
//            throw new ServiceException("id不能为1000", ResultCode.TEST_CODE.getCode());
//        }
//        return R.ok();
//    }

}