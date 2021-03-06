package com.geekerit.springbootalipay.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.geekerit.springbootalipay.config.AlipayProperties;
import com.geekerit.springbootalipay.constants.AlipayConstants;
import com.geekerit.springbootalipay.domain.AlipayRefundDTO;
import com.geekerit.springbootalipay.domain.AlipayWapDTO;
import com.geekerit.springbootalipay.enums.AlipayEnum;
import com.geekerit.springbootalipay.utils.AlipayUtil;
import com.geekerit.springbootalipay.utils.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Aaryn
 */
@Controller
@RequestMapping(value = "/pay")
@Api(value = "手机网站支付")
public class WapPayController {


    public static final Logger logger = LoggerFactory.getLogger(WapPayController.class);

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayUtil alipayUtil;


    @RequestMapping(value = "/alipay",method = RequestMethod.POST)
    @ApiOperation(value = "生成支付请求")
    public void pay(@RequestBody AlipayWapDTO alipayWapDTO, HttpServletResponse httpResponse) throws Exception {
        //创建API对应的request
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        //在公共参数中设置回跳和通知地址
        alipayRequest.setReturnUrl(alipayProperties.getReturnUrl());
        alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 设置业务参数
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setBody(alipayWapDTO.getBody());
        model.setSubject(alipayWapDTO.getSubject());
        model.setOutTradeNo(DateUtil.nowTimeString());
        model.setTimeoutExpress(alipayWapDTO.getTimeoutExpire());
        model.setTotalAmount((String.valueOf(alipayWapDTO.getTotalAmount())));
        model.setProductCode(AlipayEnum.WAPPAY.getTitle());
        logger.info("枚举类获取的信息为{}",AlipayEnum.WAPPAY.toString());
        //填充业务参数
        alipayRequest.setBizModel(model);
        String form = "";
        try {
            //调用SDK生成表单
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + alipayProperties.getCharset());
        //直接将完整的表单html输出到页面
        httpResponse.getWriter().write(form);
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    @RequestMapping(value = "/returnUrl",method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "支付宝手机网站支付同步回调地址")
    public String returnUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean verifyResult = alipayUtil.checkSign(request);
        // 验签通过业务处理
        if (verifyResult) {
            String outTradeNo = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
            logger.info("商品订单号{}", outTradeNo);
            //支付宝交易号
            String tradeNo = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");
            logger.info("交易号{}", tradeNo);
            return "wayPaySuccess";
        }
        // 验签失败
        return "wayPayFail";
    }

    @RequestMapping(value = "/info/{outTradeNo}",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "订单查询")
    public void getTradeInfo(@PathVariable(value = "outTradeNo") String outTradeNo) throws Exception{
        //创建API对应的request类
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 设置业务参数
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);
        //通过alipayClient调用API，获得对应的response类
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        logger.info("订单查询内容{}",response.getBody());
        //根据response中的结果继续业务逻辑处理
        String msg = response.getMsg();
        if (!msg.equals(AlipayConstants.QUERY_MSG_SUCCESS)){
            logger.info("订单查询失败，检查是否存在此订单信息");
        }
        logger.info("订单查询成功，开始处理业务");

    }

    @RequestMapping(value = "/refund",method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "订单退款")
    public String refundTrade(@RequestBody AlipayRefundDTO alipayRefundDTO) throws Exception{
        AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();

        AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();

        refundModel.setOutTradeNo(alipayRefundDTO.getOutTradeNo());
        refundModel.setTradeNo(alipayRefundDTO.getTradeNo());
        refundModel.setOutRequestNo(alipayRefundDTO.getOutRequestNo());
        refundModel.setRefundAmount(alipayRefundDTO.getRefundAmount());

        refundRequest.setBizModel(refundModel);

        AlipayTradeRefundResponse refundResponse = alipayClient.execute(refundRequest);

        if (null != refundResponse){
            logger.info("退款信息为",refundResponse.getBody());
        }

        return refundResponse.getBody();
    }



}
