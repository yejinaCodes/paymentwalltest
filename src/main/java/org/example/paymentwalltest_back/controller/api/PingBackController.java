package org.example.paymentwalltest_back.controller.api;

import com.paymentwall.java.Config;
import com.paymentwall.java.Pingback;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Log4j2
@RequestMapping(("/pingback"))
public class PingBackController {
    //properties에 정한 것 가지고 오기
    @Value("${paymentwall.public-key}")
    private String publicKey;
    @Value("${paymentwall.private-key}")
    private String privateKey;
    //payment에서 post해주기때문에 아래와 같이 매핑해줘야함
    @PostMapping
    public ResponseEntity<String> handlePingback(HttpServletRequest request, @RequestParam Map<String, String[]> params) { //Pingback requiredtype에 맞추기
        log.info("inside pingback");
        try {
            Config.getInstance().setLocalApiType(Config.API_GOODS);
            Config.getInstance().setPublicKey(publicKey);
            Config.getInstance().setPrivateKey(privateKey);

            //Pingback생성하고 검증하기
            Pingback pingback = new Pingback(params, request.getRemoteAddr());
            if (pingback.validate(true)) {
                String goods = pingback.getProductId();
                String userId = pingback.getUserId();
                if (pingback.isDeliverable()) {
                    // deliver Product to user with userId 나중에 refactor하기
//                    paymentService.processDeliveryConfirmation(goods, userId); //need refactoring
                } else if (pingback.isCancelable()) {
                    // withdraw Product from user with userId
                }
                log.info("pingback all good!👍🏼");
                return ResponseEntity.ok("OK"); //후 처리가 잘 완료되었을 경우 OK 반환하기
            } else {
                return ResponseEntity.badRequest().body(pingback.getErrorSummary());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing pingback");
        }
    }
}
