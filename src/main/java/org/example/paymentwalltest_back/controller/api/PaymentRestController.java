package org.example.paymentwalltest_back.controller.api;

import com.paymentwall.java.Config;
import com.paymentwall.java.Pingback;
import com.paymentwall.java.Widget;
import com.paymentwall.java.WidgetBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.Response;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@Log4j2
@RequestMapping("/api/payment")
public class PaymentRestController {
    //properties에 정한 것 가지고 오기
    @Value("${paymentwall.public-key}")
    private String publicKey;
    @Value("${paymentwall.private-key}")
    private String privateKey;

    @GetMapping("/widgetUrl")
    public ResponseEntity<Map<String, String>> getWidgetUrl() {
        log.info("inside backend processing widget url");
        try {
            Config.getInstance().setLocalApiType(Config.API_GOODS);
            Config.getInstance().setPublicKey(publicKey); //project에 있는 project Key
            Config.getInstance().setPrivateKey(privateKey); //project에 있는 secret Key

            WidgetBuilder widgetBuilder = new WidgetBuilder("yejintest1", "pw_1");

            widgetBuilder.setExtraParams(new LinkedHashMap<String, String>() {
                {
                    put("email", "user@hostname.com");
                    put("history[registration_date]", "registered_date_of_user");
                    put("ps", "all"); // Replace the value of 'ps' with specific payment system short code for Widget API uni
                    put("success_url", "http://localhost:5173/success");
                }
            });
            Widget widget = widgetBuilder.build();

            Map<String, String> response = new HashMap<>(); //JSON으로 보내야함.
            response.put("url", widget.getUrl());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //deliveryconfirm해주기
    @PostMapping("/deliveryConfirmation")
    public ResponseEntity<String> confirmDelivery() {
        log.info("inside delivery Confirmation");
        HttpPost post = new HttpPost("https://api.paymentwall.com/api/delivery");

        //API 키 헤더 설정
        post.addHeader("X-ApiKey", privateKey);

        //URL 파라미터 설정 예시 paymentwall api sample...다음에 refactoring
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("payment_id", "b199488072"));
        urlParameters.add(new BasicNameValuePair("merchant_reference_id", "w199488072"));
        urlParameters.add(new BasicNameValuePair("type", "digital"));
        urlParameters.add(new BasicNameValuePair("status", "order_placed"));
        urlParameters.add(new BasicNameValuePair("estimated_delivery_datetime", "2025/02/18 15:00:00 +0300"));
        urlParameters.add(new BasicNameValuePair("refundable", "true"));
        urlParameters.add(new BasicNameValuePair("details", "Item was delivered to the user account"));

        try{
            // 엔티티 설정
            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post);
            String responseBody = EntityUtils.toString(response.getEntity());
            return ResponseEntity.ok(responseBody);
        }catch (IOException e) {
            log.error("Delivery confirmation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delivery confirmation failed");
        }
    }
}
