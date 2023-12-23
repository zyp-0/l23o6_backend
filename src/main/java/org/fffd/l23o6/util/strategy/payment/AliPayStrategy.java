package org.fffd.l23o6.util.strategy.payment;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipayLogger;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.fffd.l23o6.pojo.entity.OrderEntity;
import org.fffd.l23o6.pojo.entity.UserEntity;
import org.fffd.l23o6.pojo.enum_.OrderStatus;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Properties;

public class AliPayStrategy extends PaymentStrategy{

    public static final AliPayStrategy INSTANCE = new AliPayStrategy();
    private static String GATEWAYURL;
    private static String APP_ID;
    private static String PRIVATE_KEY;
    private static String ALIPAY_PUBLIC_KEY;
    private static String NOTIFY_URL;
    private static String SIGN_TYPE;
    private static String FORMAT;
    private static String CHARSET;


    private AliPayStrategy() {
        YamlPropertiesFactoryBean yamlProFb = new YamlPropertiesFactoryBean();
        yamlProFb.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yamlProFb.getObject();
        APP_ID = properties.getProperty("alipay.appId");
        PRIVATE_KEY = properties.getProperty("alipay.appPrivateKey");
        ALIPAY_PUBLIC_KEY = properties.getProperty("alipay.alipayPublicKey");
        NOTIFY_URL = properties.getProperty("alipay.notifyUrl");
        GATEWAYURL = properties.getProperty("alipay.gatewayUrl");
        SIGN_TYPE = properties.getProperty("alipay.signType");
        FORMAT = properties.getProperty("alipay.format");
        CHARSET = properties.getProperty("alipay.charset");

    }
    @Override
    public void pay(OrderEntity orderEntity, UserEntity userEntity) {
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAYURL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderEntity.getId()*1001);
        bizContent.put("total_amount", 90/* orderEntity.getPrice() */);
        bizContent.put("subject", orderEntity.getSeat());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("qr_pay_mode","2:");
        request.setBizContent(bizContent.toJSONString());
        System.err.println(request);
        try {
            AlipayLogger.setJDKDebugEnabled(true);
            var response = alipayClient.pageExecute(request);
            // userEntity.setCredit(userEntity.getCredit() + calOrderCredit(orderEntity.getPrice()));
            orderEntity.setStatus(OrderStatus.PAID);
        }catch (Exception e){
            e.printStackTrace();
        }

        if (orderEntity.getDiscountStrategy().equals("不使用积分")) {
            // 保存相关信息用于取消订单
            orderEntity.setUsedMoney((double) orderEntity.getMoney());
            orderEntity.setUsedCredits(0L);
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
        }
    }

}
