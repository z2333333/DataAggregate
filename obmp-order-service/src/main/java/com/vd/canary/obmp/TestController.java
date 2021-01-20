package com.vd.canary.obmp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : wangpan
 * @date : 2020/9/9
 * Time: 9:19
 */
@RestController
@RequestMapping
public class TestController {
//    @Resource
//    private FileBillFeignClient fileBillFeignClient;
//    @Resource
//    private RocketMqClient rocketMqClient;
//    @Resource
//    private MsgSenderService msgSenderService;
//    @Resource
//    private SmsServiceFeign smsServiceFeign;
//
//    @PostMapping("/test")
//    public ResponseBO<ResponseBO> test01() {
//        ResponseBO responseBO = fileBillFeignClient.deleteById("1303501557156835330");
//        responseBO.getMessage();
//        return ResponseUtil.ok(responseBO);
//
//    }
//
//    @PostMapping("/test/idS")
//    public ResponseBO test02() {
//        UpdateFileBillReq updateFileBillReq = new UpdateFileBillReq();
//        List<String> removeIdS = Arrays.asList("1303251660709064705");
//        updateFileBillReq.setRemoveFileIdList(removeIdS);
//        updateFileBillReq.setBillId("1280476299130904577PDF_TO_IMG");
//        updateFileBillReq.setBusinessType(DemandConstants.DemandFileType.ORDER_SUNDRIES_FILE.getCode());
//        ResponseBO responseBO = fileBillFeignClient.updateBillsById(updateFileBillReq);
//        return ResponseUtil.ok();
//    }
//
//    @PostMapping("/test/find")
//    public ResponseBO<List<FileBillVO>> test03() {
//        ResponseBO<List<FileBillVO>> listResponseBO = fileBillFeignClient.listFilesById("1280476299130904577PDF_TO_IMG", DemandConstants.DemandFileType.ORDER_SUNDRIES_FILE.getCode());
//        return ResponseUtil.ok(listResponseBO.getData());
//    }
//
//
//    @PostMapping("/msgAndSmsTest")
//    public ResponseBO<Boolean> msgAndSmsTest() {
//        com.vd.canary.obmp.order.api.request.MsgSenderReq req = new com.vd.canary.obmp.order.api.request.MsgSenderReq();
//        MsgSendUserBO msgSendUserBO = new MsgSendUserBO();
//        msgSendUserBO.setUserId("IMPL0000000000201804200313175429");
//        MsgSendUserBO msgSendUserBO1 = new MsgSendUserBO();
//        msgSendUserBO.setUserId("MsgSendUserBO");
//        req.setToUserList(Arrays.asList(msgSendUserBO, msgSendUserBO1));
//        req.setMsgTemplateCode("WJ0000004");
//        req.setSmsTemplateCode("SMS_204286344");
//        List<ParameterBO> parameterBOs = Lists.newArrayList();
//        ParameterBO parameterBO = new ParameterBO();
//        parameterBO.setParameter("销售订单");
//        parameterBO.setParameterName("DocType");
//        parameterBOs.add(parameterBO);
//        parameterBO = new ParameterBO();
//        parameterBO.setParameter("SC2005180001");
//        parameterBO.setParameterName("DocNo");
//        parameterBOs.add(parameterBO);
//        req.setMsgParameterList(parameterBOs);
//        msgSenderService.senderMsgAndSms(req);
//        return ResponseUtil.ok();
//    }
//
//    @PostMapping("/smsTest")
//    public ResponseBO<Boolean> smsTest() {
//        HashMap<String, String> objectHashMap = Maps.newHashMap();
//
//        List<ParameterBO> parameterBOs = com.google.common.collect.Lists.newArrayList();
//        ParameterBO parameterBO = new ParameterBO();
//        parameterBO.setParameter("销售订单");
//        parameterBO.setParameterName("DocType");
//        parameterBOs.add(parameterBO);
//        ParameterBO parameterCodeBO = new ParameterBO();
//        parameterCodeBO.setParameter("SC2005180001");
//        parameterCodeBO.setParameterName("DocNo");
//        parameterBOs.add(parameterBO);
//        parameterBOs.forEach(list -> objectHashMap.put(list.getParameterName(), list.getParameter()));
//        SendSmsReq sendSmsReq = new SendSmsReq();
////        sendSmsReq.setPhoneNumbers(phoneNumbers);
//        sendSmsReq.setPhoneNumbers("15779163737");
//        sendSmsReq.setTemplateCode("SMS_204286344");
//        sendSmsReq.setParams(objectHashMap);
//
//        //短息服务自定义错误通过code+message识别，短信息服务商错误通过 sendSmsRespResponseBO.getError()识别
//        ResponseBO<?> sendSmsRespResponseBO = smsServiceFeign.sendSms(sendSmsReq);
//        return ResponseUtil.ok(sendSmsRespResponseBO.getSuccess());
//    }
//
//    @PostMapping("/senderMsgOfApp")
//    public ResponseBO<Boolean> senderMsgOfApp() {
//        MsgSenderOfAppReq msgSenderOfAppReq = new MsgSenderOfAppReq();
//        msgSenderOfAppReq.setMsgTemplateType("1");
//        msgSenderOfAppReq.setSalesContractCode("HT2005180001");
//        msgSenderOfAppReq.setSalesContractId("1262189485437767681");
//        msgSenderService.senderMsgOfApp(msgSenderOfAppReq);
//        return ResponseUtil.ok();
//    }
}
