package com.vd.canary.obmp.order.controller;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.util.ResponseUtil;
import com.vd.canary.obmp.order.api.request.*;
import com.vd.canary.obmp.order.api.request.order.PomPurchaseContractSignReq;
import com.vd.canary.obmp.order.api.request.order.PomPurchaseContractUpdateReq;
import com.vd.canary.obmp.order.api.response.*;
import com.vd.canary.obmp.order.api.response.vo.order.PomPurchaseContractGenerateResp;
import com.vd.canary.obmp.order.api.response.vo.order.PomPurchaseContractHistoryResp;
import com.vd.canary.obmp.order.service.order.PurchaseService;
import com.vd.canary.utils.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author zx
 * @date 2020/9/24 18:09
 */
@Slf4j
@RestController()
@RequestMapping("/purchase")
public class PurchaseController {

    @Resource
    PurchaseService purchaseService;

    /**
     * 更新采购单
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<com.vd.canary.obmp.order.api.response.vo.order.PomPurchaseContractGenerateResp>
     */
    @PostMapping("/update")
    @Transactional
    public ResponseBO<PomPurchaseContractGenerateResp> updatePurchaseContract(@Valid @RequestBody PomPurchaseContractUpdateReq req){
        return purchaseService.updatePurchaseContract(req);
    }

    /**
     * 指定采购经理
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/accept")
    public ResponseBO<Boolean> acceptPurchaseContract(@Valid @RequestBody PomPurchaseContractAcceptReq req){
        return purchaseService.assignPurchaseContract(req);
    }

    /**
     * 批量提交采购订单
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/submit")
    public ResponseBO<Boolean> submitPurchaseContract(@Valid @RequestBody PomPurchaseContractHeadComReq req){
        return purchaseService.submitPurchaseContract(req);
    }

    /**
     * 采购订单列表
     *
     * @param purchaseOrderListReq
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.PurchaseOrderListResp>
     */
    //@DataAggregate()
    @PostMapping("/list")
    public ResponsePageBO<PurchaseOrderListResp> purchaseOrderList(@Valid @RequestBody PurchaseOrderListReq purchaseOrderListReq){
        return purchaseService.purchaseOrderList(purchaseOrderListReq);
    }

    /**
     * 采购订单详情
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<com.vd.canary.obmp.order.api.response.PomPurchaseContractDetailResp>
     */
    //@DataAggregate()
    @PostMapping("/info")
    public ResponseBO<PomPurchaseContractDetailResp> purchaseOrderDetail(@Valid @RequestBody PomPurchaseContractDetailReq req){
        return purchaseService.purchaseOrderDetail(req);
    }

    /**
     * 采购订单发货批次
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<com.vd.canary.obmp.order.api.response.ShipmentPlanResp>
     */
    @PostMapping("/shipmentPlan")
    public ResponseBO<ShipmentPlanResp> pomPurchaseContractShipmentPlanList(@Valid @RequestBody PomPurchaseContractDetailReq req){
        return purchaseService.pomPurchaseContractShipmentPlanList(req);
    }

    /**
     * 采购订单历史
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<com.vd.canary.obmp.order.api.response.vo.order.PomPurchaseContractHistoryResp>
     */
    @PostMapping("/histories")
    public ResponseBO<PomPurchaseContractHistoryResp> pomPurchaseContractHistoryList(@Valid @RequestBody PomPurchaseContractDetailReq req){
        return purchaseService.pomPurchaseContractHistoryList(req);
    }

    /**
     * 采购订单线下签约
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/sign")
    public ResponseBO<Boolean> signPurchaseContract(@Valid @RequestBody PomPurchaseContractSignReq req){
        return purchaseService.signPurchaseContract(req);
    }

    /**
     * 采购订单批量完结
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/finish")
    public ResponseBO<Boolean> finishPurchaseContract(@Valid @RequestBody PomPurchaseContractHeadComReq req){
        return purchaseService.finishPurchaseContract(req);
    }

    /**
     * 采购订单批量打开
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/open")
    public ResponseBO<Boolean> openPurchaseContract(@Valid @RequestBody PomPurchaseContractHeadComReq req){
        return purchaseService.openPurchaseContract(req);
    }

    /**
     * 采购订单批量撤回
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/revoke")
    public ResponseBO<Boolean> revokePurchaseContract(@Valid @RequestBody PomPurchaseContractHeadComReq req){
        return purchaseService.revokePurchaseContract(req);
    }

    /**
     * 根据销售单获取关联采购单简略详情列表（中台用）
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<com.vd.canary.obmp.order.api.response.PomPurchaseContractBriefDetailResp>
     */
    //@DataAggregate()
    @PostMapping("/briefInfo")
    public ResponseBO<List<PomPurchaseContractBriefDetailResp>> purchaseOrderBriefDetail(@Valid @RequestBody PomPurchaseContractBriefDetailReq req) {
        PomPurchaseContractBriefDetailAllReq briefDetailAllReq = BeanUtil.convert(req, PomPurchaseContractBriefDetailAllReq.class);
        briefDetailAllReq.setShowAttachmentNum(true);
        List<PomPurchaseBriefDetailAllResp> briefDetailResp = purchaseService.purchaseOrderBriefDetail(briefDetailAllReq);
        return ResponseUtil.ok(BeanUtil.convert(briefDetailResp, PomPurchaseContractBriefDetailResp.class));
    }

    /***
     * 根据采购单号列表获取关联采购单简略详情列表（财务用）
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.util.List < com.vd.canary.obmp.order.api.response.PomPurchaseBriefDetailForFinanceResp>>
     */
    @PostMapping("/briefInfoFinance")
    public ResponseBO<List<PomPurchaseBriefDetailForFinanceResp>> purchaseOrderBriefDetail(@Valid @RequestBody PomPurchaseBriefForFinanceDetailReq req) {
        PomPurchaseContractBriefDetailAllReq briefDetailAllReq = BeanUtil.convert(req, PomPurchaseContractBriefDetailAllReq.class);
        briefDetailAllReq.setShowSalesContractCodePrimary(true);
        List<PomPurchaseBriefDetailAllResp> briefDetailResp = purchaseService.purchaseOrderBriefDetail(briefDetailAllReq);
        return ResponseUtil.ok(BeanUtil.convert(briefDetailResp, PomPurchaseBriefDetailForFinanceResp.class));
    }

    /**
     * 采购单批量关闭
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @PostMapping("/shut")
    public ResponseBO<Boolean> shutPurchaseContract(@Valid @RequestBody PomPurchaseContractHeadComReq req){
        //当前不存在批量
        req.getPurchaseContractHeadIdList().forEach(pomHeadId -> {
            PomPurchaseContractShutReq shutReq = new PomPurchaseContractShutReq();
            shutReq.setOrderId(pomHeadId);
            shutReq.setType(1);
            shutReq.setReason("中台关闭采购单");
            purchaseService.shutPurchaseContract(shutReq);
        });
        return ResponseUtil.ok();
    }

}
