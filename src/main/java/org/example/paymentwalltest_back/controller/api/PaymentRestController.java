package org.example.paymentwalltest_back.controller.api;

import com.paymentwall.java.Config;
import com.paymentwall.java.Charge;
import com.paymentwall.java.Pingback;
import com.paymentwall.java.Product;
import com.paymentwall.java.ProductBuilder;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.security.MessageDigest;
import java.net.URL;
import java.net.URLEncoder;

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
    //to check whether ip or country_code argument works with higher
    @GetMapping("/checkout")
    public ResponseEntity<Map<String, String>> testCheckout(@RequestParam(required=false, defaultValue = "SG") String countryCode){
        log.info("inside checkout api");

        WidgetBuilder widgetBuilder = new WidgetBuilder("user12345", "pw");
        widgetBuilder.setProduct(
	        new ProductBuilder("product301") {{
		        setAmount(0.99);
		        setCurrencyCode("USD");
		        setName("100 coins");
		        setProductType(Product.TYPE_FIXED);
	        }}.build()
        );  
        widgetBuilder.setExtraParams(
	        new LinkedHashMap<String, String>() {{
		        put("email", "user@hostname.com");
                put("country_code", countryCode);
	        }}
        );
        Widget w = widgetBuilder.build();
        Map<String, String> response = new HashMap<>(); //JSON으로 보내야함.
        response.put("url", w.getUrl());
        return ResponseEntity.ok(response);
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

    @GetMapping("/check-payment-status")
    public ResponseEntity<String> checkPaymentStatus(@RequestParam(required=true) String merchant_order_id,
                                                     @RequestParam(required=false, defaultValue = "user40012") String uid,
                                                     @RequestParam(required=false, defaultValue = "paymentStatusHandler") String callback){

        String projectKey = "307be73d19ed10d185fb0c116d38fc3b";
        String secretKey = "2017ec5142817b8a7f09d99e2a8320b1";

        try{
            //Request parameters
            Map<String, String> params = new TreeMap<>(); //Tree map for alphabetical order. not linked map
            params.put("key", projectKey);
            params.put("merchant_order_id", merchant_order_id);
            params.put("uid", uid);
            params.put("callback", callback);
            params.put("sign_version", "2");

            //Calculate signature
            StringBuilder baseString = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                baseString.append(entry.getKey()).append("=").append(entry.getValue());
            }
            baseString.append(secretKey);

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(baseString.toString().getBytes(StandardCharsets.UTF_8)); //know which hash is being used here

            //Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String signature = hexString.toString();

            //String signature = DatatypeConverter.printHexBinary(hash).toLowerCase();

            params.put("sign", signature);

            //Build query string
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                queryString.append("=");
                queryString.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            //Create connection
            URL url = new URL("https://api.paymentwall.com/api/rest/payment/?" + queryString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            //Get response
            int responseCode = connection.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return ResponseEntity.status(responseCode).body(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error checking payment status!!: " + e.getMessage());
        }
        }
    }






    //check payment status api using merchant_order_id
