package org.fffd.l23o6.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import io.github.lyc8503.spring.starter.incantation.exception.BizException;
import io.github.lyc8503.spring.starter.incantation.exception.CommonErrorType;
import io.github.lyc8503.spring.starter.incantation.pojo.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fffd.l23o6.dao.OrderDao;
import org.fffd.l23o6.dao.TrainDao;
import org.fffd.l23o6.dao.UserDao;
import org.fffd.l23o6.pojo.entity.OrderEntity;
import org.fffd.l23o6.pojo.entity.TrainEntity;
import org.fffd.l23o6.pojo.entity.UserEntity;
import org.fffd.l23o6.pojo.enum_.OrderStatus;
import org.fffd.l23o6.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/v1/")
@RequiredArgsConstructor
public class AliPayController {
    private final OrderService orderService;
    private final OrderDao orderDao;
    private final UserDao userDao;
    private final TrainDao trainDao;
    private final String APP_ID = "9021000123601512";
    private final String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxC+4rmwm+nsIzMJpqFxw5ioa10ldi+QXMuho26NcIVNMZOxnC7O2f9KbokvkFT9Ll5j8ZUHyvUGDUKT9XqvFYghf695Ktm8H56nocOfI7gLl13I4xYFDmQPvHiEp+IjxD+lyU6misgvQbBTeX5SW7G0znVx65NeEwOvnUXlI69dgqgistjQjhNs6fxSkBZJzmi/JMgWxRZdgdaFY4Rrk5/a7gGsdcOzrnPt94gI0O048Q359R2srGSN1oAw1YJmiM81Y5SvIcrtKx6mX3wzlepvPJdcdv02H8vCFK8R0/L5wf0CW1kpJ/m+jsTydRD6OZuS79Tle1oQr8SUve1B/7AgMBAAECggEBAI9kQT7x75+Ch3jcOYQJOaPk7Rvfw0T2uVrJs6ebR2WJBfweYMv+BqsRhAlD2AoUT5RBugMvq/x1libfTV0cpyHvI6rwzsxrJVzbKpEn4WeN5ydtPYECKCOEqCc/3E1yPuszn75AaAyodzpXLGKKhdeX0d5gMhMBb2QXr/Xvepwi2xlYw7koaV5zbi6FI/c1CbYd7uuHRcxWV2IYwvqfIKXV6MKF8OyjcPqIAGvmBBGYT2L4CoC4D8q3l+h3PL66ULK4/6fU3I5YC3QO8o841laKX5PHN1Pvbt/MpTJJJ006xT+Eapp/B26SqYDR8yIL4PM6tGXSyE6iDQH80FINEOkCgYEA9DE9Lecw1e7CfSezmvTsc9DL3LrCKj1+D1hfGiOoTytY9YqIEbW7txeWmB98jQN2V4juG4Acr/a8TSoIA+/YmazsWB7lLvGk+JC6MXjtMPvI40Reky6wqHnqssGYJwwfvhk+PkWdU6sVBIvVFd4iN2MVm4Ms1PHWUE/Z5srD1O8CgYEAuZuFS0hMYIUwthFAFHu7K/B+jw27JHxXi02h4xgnTxtAvsDUEZajaNTY08ZP9S9T0SWkz/6zKyR5tRM4HTwmKnMAAwrtes7DD6F0PD2JGmwiQaB+9LrfxwsQuVnpX7wHriukISyD0sNdwaEwjSitY0Qo1SFJGigh+Cb0ZHO4nbUCgYEAvVOf14M0PeoMKPUrL61N1s3AAbda610Z69PciGu1BwzYwCdUMEby75X+UJu4+awBiQTFd9TwaQ2oGTvJUyQWj6+e3wO1NZxtB7tOYSUc4amVq39KxFqi5T0BVu76hOvFgKIZvDZFMKWoegnggD/lz4OnMReXHtMSEtqWW3ZCN0kCgYB1VjsbD5axQiI8R4WCLYJzUbNAjoarlyvk9ewM290l5m206cRnZhriEAUPRISR1Ryg0LJzh2oPMRz7+y1zVe4u0crFIahmBcZM4F3SI3jESm4RnPwFjQohE/67nnqpuf4E/rPhAPpJSJbB7Tv7USbj5VgG/Obw8UQgU8gWpQ2mLQKBgDFRgya1OcUJzjA2sj+vxxFYYSif9UOlnvz7ZOg3fwY51xpcuDhT0BNvzhTAl9BQUHgABJv3qbnldpjombfg6WR3EBsnLvkBw/Jzfg3doddOySbsKnm+h597uJF46xQ8iVPKxGBmhna6UCmN6f5qWc4JkUBCcnYgjJBL6tisHx1L";
    private final String CHARSET = "UTF-8";
    private final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAij4kWSo2VfZDOEpgffryBO36SVAkxEF0Z3ErOJe2FfmhcaTKQ2ud09uNVibgRo5/GWwMbPho+0Oadq95JrLenPT1k/9mX+gL2S9Hkci/o7N10vnuJm7kBIvwYakq6ztBvr9VtbCW8yBkVL/YYXy+LyFvBrEOxwK7CG+Z+rIedNCJhac+qn2iPTFQSiSfOAGhZFkp2dj/tL48PneP3Y5V2vprHf7lNvQn+YhololQSLt5UhagM4JFNqy/GOQz1bSnrNNfqceWTgXhga869GQPYtzn2TcdRE+5t2lxjQor7vwrR+xybqPsQpJl2TXzztLgsE+juI2uYfUkJjgApXMeKQIDAQAB";
    //这是沙箱接口路径,正式路径为https://openapi.alipay.com/gateway.do
    private final String GATEWAY_URL ="https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private final String FORMAT = "JSON";
    //签名方式
    private final String SIGN_TYPE = "RSA2";
    //支付宝异步通知路径,付款完毕后会异步调用本项目的方法,必须为公网地址
    private final String NOTIFY_URL = "http://egmekq.natappfree.cc/alipay/notify";
    //支付宝同步通知路径,也就是当付款完毕后跳转本项目的页面,可以不是公网地址
    private final String RETURN_URL = "http://egmekq.natappfree.cc/alipay/returnUrl";

    double[] powerTier = new double[]{0, 0.001, 0.0015, 0.002, 0.0025, 0.003};
    Long[] creditTier = new Long[]{0L, 1000L, 3000L, 10000L, 50000L};


    @GetMapping("alipay/{orderId}")
    public String alipay(@PathVariable("orderId") Long orderId, HttpServletResponse httpResponse) throws IOException {
        //实例化客户端,填入所需参数
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //在公共参数中设置回跳和通知地址
        request.setNotifyUrl(NOTIFY_URL);
        //根据订单编号,查询订单和客户相关信息
        OrderEntity order = orderDao.findById(orderId).get();
        UserEntity user = userDao.findById(order.getUserId()).get();
        TrainEntity train = trainDao.findById(order.getTrainId()).get();
        double money = calMoney(order, user);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = order.getId().toString();
        //付款金额，必填
        String total_amount = String.valueOf(money);
        //订单名称，必填
        String subject = train.getName() + " " + order.getSeat();
        //商品描述，可空
        String body = "";
        request.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);// 直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();

        // 支付完成后状态变为“已支付”, 保存一下订单和用户信息
        order.setStatus(OrderStatus.PAID);
        orderDao.save(order);
        userDao.save(user);

        // 更新客户积分
        orderService.checkOrderStatus(orderId);
        return form;
    }

    @PostMapping("/notify")  // 注意这里必须是POST接口
    public void Notify(HttpServletResponse response, HttpServletRequest request) throws Exception {
        System.out.println("----------------------------notify_url------------------------");
        // 商户订单号
        String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "GBK");
        // 付款金额
        String total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"), "GBK");
        // 支付宝交易号
        String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "GBK");
        // 交易说明
        String cus = new String(request.getParameter("body").getBytes("ISO-8859-1"), "GBK");
        // 交易状态
        String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "GBK");
        if (trade_status.equals("TRADE_SUCCESS")) {//支付成功商家操作
            // 支付完成后状态变为“已支付”, 保存一下订单和用户信息
            Long orderId = Long.parseLong(out_trade_no);
            OrderEntity order = orderDao.findById(orderId).get();
            UserEntity user = userDao.findById(order.getUserId()).get();

            order.setStatus(OrderStatus.PAID);
            orderDao.save(order);
            userDao.save(user);

            // 更新客户积分
            orderService.checkOrderStatus(orderId);
        }
    }

    private double calMoney(OrderEntity orderEntity, UserEntity userEntity) {
        if (orderEntity.getDiscountStrategy().equals("不使用积分")) {
            // 保存相关信息用于取消订单
            orderEntity.setUsedMoney((double) orderEntity.getMoney());
            orderEntity.setUsedCredits(0L);
            return (double) orderEntity.getMoney();
        } else {
            Long originalCredits = userEntity.getCredits();
            Long credits = userEntity.getCredits();
            Long money = orderEntity.getMoney();
            double discountMoney = 0;
            double remainedMoney = money;
            // 调整积分
            if (credits > 50000) {
                discountMoney = (credits - 50000) * 0.003;
                if (discountMoney > money) {
                    userEntity.setCredits(credits - Math.round(money / 0.003));
                    remainedMoney = 0;
                } else {
                    credits = 50000L;
                    remainedMoney -= discountMoney;
                    for (int i = 4; i >= 1; i--) {
                        discountMoney += powerTier[i] * (creditTier[i] - creditTier[i - 1]);
                        if (discountMoney > remainedMoney) {
                            remainedMoney = 0;
                            userEntity.setCredits(credits - Math.round(remainedMoney / powerTier[i]));
                            break;
                        } else {
                            credits = creditTier[i - 1];
                            remainedMoney -= discountMoney;
                        }
                    }
                    if (credits == 0L) {
                        userEntity.setCredits(credits);
                    }
                }
            } else if (credits <= 50000 && credits >= 1000){
                int idx = 4;
                for (int i = 4; i >= 2; i--) {
                    if (creditTier[i] >= credits && credits >= creditTier[i - 1]) {
                        idx = i;
                        break;
                    }
                }
                for (int i = idx; i >= 1; i--) {
                    discountMoney += powerTier[i] * (creditTier[i] - creditTier[i - 1]);
                    if (discountMoney > remainedMoney) {
                        remainedMoney = 0;
                        userEntity.setCredits(credits - Math.round(remainedMoney / powerTier[i]));
                        break;
                    } else {
                        credits = creditTier[i - 1];
                        remainedMoney -= discountMoney;
                    }
                }
                if (credits == 0L) {
                    userEntity.setCredits(credits);
                }
            } else {
                // 积分不足以使用抵扣
            }

            // 保存相关信息用于取消订单
            orderEntity.setUsedMoney(remainedMoney);
            orderEntity.setUsedCredits(originalCredits - userEntity.getCredits());
            return remainedMoney;
        }
    }

}
