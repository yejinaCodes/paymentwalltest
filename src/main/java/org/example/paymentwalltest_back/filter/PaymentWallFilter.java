package org.example.paymentwalltest_back.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

//component세팅하면 spring에서 자동으로 설정해줌.
//@Component
@Log4j2
public class PaymentWallFilter implements Filter {
    //frontend 요청이랑 paymentwall에서의 pingback요청만 허용하기
    private static final String PAYMENT_IP_PREFIX = "216.127.71.";
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request; //httpservletrequest이 servletrequest를 상속하기 때문에 형 변환 필요
        String uri = httpRequest.getRequestURI();
        String clientIp = request.getRemoteAddr();
        String origin = httpRequest.getHeader("Origin"); //어느 도메인인지 확인하기

//        log.info("Request clientIp: {}", clientIp);
//        log.info("Request uri: {}", uri);
        log.info(clientIp);
        log.info(uri);

        //frontend 요청인지 확인하기
        if(uri.startsWith("/api/")){
            if(origin == null || !origin.equals(FRONTEND_ORIGIN)){
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(403);
                return;
            }
        }
        //pingback 요청일 경우
        else if("/api/payment/pingback".equals(uri)){
            if(!clientIp.startsWith(PAYMENT_IP_PREFIX)){
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
