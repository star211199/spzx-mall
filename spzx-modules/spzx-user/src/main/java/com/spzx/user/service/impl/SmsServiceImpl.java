package com.spzx.user.service.impl;

import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.core.utils.HttpUtils;
import com.spzx.user.service.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信服务实现类
 * 注意：敏感配置请通过环境变量或配置文件注入，不要硬编码
 */
@Slf4j
@Service
public class SmsServiceImpl implements ISmsService {

    @Value("${sms.host:https://gyyyx1.market.alicloudapi.com}")
    private String smsHost;

    @Value("${sms.path:/sms/smsSend}")
    private String smsPath;

    @Value("${sms.appcode:}")
    private String smsAppcode;

    @Value("${sms.signId:2e65b1bb3d054466b82f0c9d125465e2}")
    private String smsSignId;

    @Override
    public void send(String phone, String templateId, Map<String, Object> param) {
        // 校验敏感配置
        if (smsAppcode == null || smsAppcode.isEmpty()) {
            log.error("短信服务配置异常：appcode未配置");
            throw new ServiceException("短信服务配置异常，请联系管理员");
        }

        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            log.warn("短信发送失败：手机号格式不正确 - {}", phone);
            throw new ServiceException("手机号格式不正确");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + smsAppcode);

        Map<String, String> querys = new HashMap<>();
        querys.put("mobile", phone);
        querys.put("templateId", templateId);
        querys.put("smsSignId", smsSignId);
        querys.put("param", "**code**:" + param.get("code") + ",**minute**:5");

        Map<String, String> bodys = new HashMap<>();

        try {
            HttpResponse response = HttpUtils.doPost(smsHost, smsPath, "POST", headers, querys, bodys);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                log.info("短信发送成功，手机号：{}，模板ID：{}", phone, templateId);
            } else {
                log.error("短信发送失败，手机号：{}，状态码：{}", phone, statusCode);
                throw new ServiceException("短信发送失败，请稍后重试");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常，手机号：{}", phone, e);
            throw new ServiceException("短信发送异常，请稍后重试");
        }
    }
}
