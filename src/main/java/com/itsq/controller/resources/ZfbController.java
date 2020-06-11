package com.itsq.controller.resources;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.itsq.common.base.BaseController;
/*
import com.itsq.config.AlipayConfig;
*/

import com.itsq.common.bean.Response;
import com.itsq.config.AlipayConfig;
import com.itsq.pojo.entity.RechargeRecord;
import com.itsq.service.resources.RechargeRecordService;
import com.itsq.utils.StringUtils;
import com.itsq.utils.alipay.AlipayUtils;
import com.itsq.utils.http.MoneyChangeUtils;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述：
 *
 * @author JIAQI
 * @date 2019/11/17 - 15:18
 */
@Slf4j
@RestController
@RequestMapping("/zfb")
@AllArgsConstructor
@CrossOrigin
@Api(tags = "支付宝模块")
public class ZfbController extends BaseController {



   @Autowired
    private RechargeRecordService rechargeRecordService;
    @Autowired
    private MoneyChangeUtils moneyChangeUtils;

    @RequestMapping("huidiao")
    public String toHuidiao(HttpServletRequest request){
        System.out.println("=========================================>");
        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = StringUtils.toMap(request);

            //校验签名
            if (!AlipaySignature.rsaCheckV1(params, AlipayUtils.alipay_public_key, "UTF-8", "RSA2")) {
                log.error("【支付宝支付异步通知】签名验证失败, response={}", params);
                throw new RuntimeException("【支付宝支付异步通知】签名验证失败");
            }
            //交易状态

            String order = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            if(tradeStatus.equals("TRADE_SUCCESS")){
                RechargeRecord rechargeRecord = rechargeRecordService.selectRechargeRecord(order);
                rechargeRecord.setTradeStatus(tradeStatus);
                rechargeRecordService.updateRechargeRecord(rechargeRecord);
            }
            System.out.println(order + tradeStatus);

            if (!tradeStatus.equals("TRADE_FINISHED") &&
                    !tradeStatus.equals("TRADE_SUCCESS")) {
                throw new RuntimeException("【支付宝支付异步通知】发起支付, trade_status != SUCCESS | FINISHED");
            }
            log.info("【支付成功】");




            //TODO 相应订单业务处理
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failure";
        }


    @PostMapping(value = "/pagePay")
    @ApiOperation(value = "统一支付", notes = "", httpMethod = "POST")
    public Response pagePay(Model model, Integer amount,Integer playerId) throws Exception {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayUtils.gatewayUrl,AlipayUtils.app_id,AlipayUtils.private_key,"json",AlipayUtils.input_charset,AlipayUtils.alipay_public_key,"RSA2");
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl("  http://yuanqiwl.natapp1.cc/zfb/huidiao");
        String outTradeNo = StringUtils.getOutTradeNo();

        BigDecimal bd = new BigDecimal(amount*Double.valueOf(moneyChangeUtils.getRequest3()));
        bd = bd.setScale(2,BigDecimal.ROUND_HALF_UP);

       /* JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",);
        jsonObject.put("product_code",outTradeNo);
        jsonObject.put("total_amount",bd+"");
        jsonObject.put("subject","网站充值");
        jsonObject.put("timeout_express","90m");*/

        request.setBizContent("{\"out_trade_no\":\""+ outTradeNo +"\","
                + "\"total_amount\":\""+ bd +"\","
                + "\"subject\":\"网站充值\","
                + "\"body\":\""+ bd +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        //request.setBizContent(jsonObject.toJSONString());


        String result=alipayClient.pageExecute(request).getBody();
        RechargeRecord rechargeRecord=new RechargeRecord();

        rechargeRecord.setTradeNo(outTradeNo);
        rechargeRecord.setAmount(new BigDecimal(amount));
        rechargeRecord.setType(2);
        rechargeRecord.setPlayersId(playerId);
        rechargeRecordService.addRechargeRecord(rechargeRecord);
        return Response.success(result);
    }

    /**
     * 当面付——扫码支付
     *
     * @param model
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/alipay")
    @ApiOperation(value = "扫码支付", notes = "", httpMethod = "POST")
    public String dopay(Model model, Integer amount,Integer playerId) throws Exception {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.gatewayUrl, AlipayConfig.app_id, AlipayConfig.private_key, "json", AlipayConfig.input_charset, AlipayConfig.alipay_public_key, "RSA2");
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();//创建API对应的request类
        //设置模型参数
        AlipayTradeAppPayModel PayModel = new AlipayTradeAppPayModel();
        /*商户订单号(必填)*/
        String outTradeNo = StringUtils.getOutTradeNo();
        PayModel.setOutTradeNo(outTradeNo);
        /*订单总金额(单位元 必填)*/
        BigDecimal bd = new BigDecimal(amount*Double.valueOf(moneyChangeUtils.getRequest3()));
        bd = bd.setScale(2,BigDecimal.ROUND_HALF_UP);
        PayModel.setTotalAmount(bd+"");
        /*订单标题 (必填)*/
        PayModel.setSubject("游戏");
        /*订单允许的最晚付款时间 (选填)*/
        PayModel.setTimeoutExpress("90m");
        //将参数对手打包到请求中
        request.setBizModel(PayModel);
        //设置回调地址
        request.setNotifyUrl(AlipayConfig.notify_url);
        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        //JSONObject jsonObject = JSON.parseObject(response.getBody()).getJSONObject("alipay_trade_precreate_response");
        // String qr_code = (String) jsonObject.get("qr_code");
        System.out.println(response.getQrCode());
        model.addAttribute("code_url", response.getQrCode());
        RechargeRecord rechargeRecord=new RechargeRecord();

        rechargeRecord.setTradeNo(outTradeNo);
        rechargeRecord.setAmount(new BigDecimal(amount));
        rechargeRecord.setType(2);
        rechargeRecord.setPlayersId(playerId);
        rechargeRecordService.addRechargeRecord(rechargeRecord);

      //  QRCodeGenerator.generateQRCodeImage(response.getQrCode(),350,350,"C:/img/ewm/"+outTradeNo+".png");


        return response.getQrCode();
    }

    /**
     * 异步回调
     * *注意：此方法会被调用两次
     * 一次是扫码的时候
     * 一次是支付成功的时候
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/notify_url")
    @ResponseBody
    public String notifyUrl(HttpServletRequest request) {
        System.out.println("=========================================>");
        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = StringUtils.toMap(request);

            //校验签名
            if (!AlipaySignature.rsaCheckV1(params, AlipayConfig.alipay_public_key, "UTF-8", "RSA2")) {
                log.error("【支付宝支付异步通知】签名验证失败, response={}", params);
                throw new RuntimeException("【支付宝支付异步通知】签名验证失败");
            }
            //交易状态

            String order = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            if(tradeStatus.equals("TRADE_SUCCESS")){
                RechargeRecord rechargeRecord = rechargeRecordService.selectRechargeRecord(order);
                rechargeRecord.setTradeStatus(tradeStatus);
                rechargeRecordService.updateRechargeRecord(rechargeRecord);
            }
            System.out.println(order + tradeStatus);

            if (!tradeStatus.equals("TRADE_FINISHED") &&
                    !tradeStatus.equals("TRADE_SUCCESS")) {
                throw new RuntimeException("【支付宝支付异步通知】发起支付, trade_status != SUCCESS | FINISHED");
            }
            log.info("【支付成功】");




            //TODO 相应订单业务处理
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failure";
    }

    //可用此方法进行简单接口调试
    public static void main(String[] args) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.gatewayUrl, AlipayConfig.app_id, AlipayConfig.private_key, "json", AlipayConfig.input_charset, AlipayConfig.alipay_public_key, "RSA2");
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();//创建API对应的request类

        Map<String, Object> reqData = new HashMap<>();
        reqData.put("out_trade_no", "tradeprecreate" + StringUtils.getOutTradeNo());
        reqData.put("subject", "小编机器人纠错");
        reqData.put("total_amount", "0.01");
        reqData.put("store_id", "123556");
        reqData.put("timeout_express", "1m");
        //把订单信息转换为json对象的字符串
        request.setBizContent(JSON.toJSONString(reqData));
        //设置回调地址
        request.setNotifyUrl("http://www.cqkj.nat300.com/zfb/notify_url");
        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            System.out.println(response.getBody());
            System.out.println(response.getCode());
            System.out.println(response.getQrCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 支付接口，跳转到支付界面，支付成功后 回调接口
     *
     * @param httpResponse
     * @throws IOException
     */
    @GetMapping("/pay")
    public void pay(HttpServletResponse httpResponse) throws IOException {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.gatewayUrl, AlipayConfig.app_id, AlipayConfig.private_key,
                "json", AlipayConfig.input_charset, AlipayConfig.alipay_public_key, "RSA2");
        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(AlipayConfig.return_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_url);
        //商品价格总额
        String total_amount = "100000";
        //商品名称
        String subject = "测试沙箱环境支付";
        //商品描述，可以为空
        String body = "";
        //填充业务参数
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setBody(body);
        model.setGoodsType("1");
        model.setOutTradeNo(StringUtils.getOutTradeNo());
        model.setTotalAmount(total_amount);
        model.setSubject(subject);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizModel(model);

        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (Exception e) {
            e.printStackTrace();
        }

        httpResponse.setContentType("text/html;charset=utf-8");
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    /**
     * 回调页面
     *
     * @return
     */
    @GetMapping("/success")
    public String success() {
        return "支付成功";
    }
}