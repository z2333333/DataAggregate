package com.vd.canary.obmp.order.operation.demand.api;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.obmp.aggregate.annotation.DataAggregate;
import com.vd.canary.obmp.order.api.operation.demand.request.*;
import com.vd.canary.obmp.order.api.operation.demand.response.*;
import com.vd.canary.obmp.order.api.request.order.DemandTrackListReq;
import com.vd.canary.obmp.order.api.request.order.DemandTrackSaveReq;
import com.vd.canary.obmp.order.api.response.vo.order.DemandTrackListResp;
import com.vd.canary.obmp.order.operation.demand.service.DemandService;
import com.vd.canary.obmp.order.service.order.DemandTrackService;
import com.vd.canary.service.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * 需求单 Api
 *
 * @author : xingdongyang
 * @date : 2020-07-08
 */
@Slf4j
@RestController
@RequestMapping("/demand")
public class DemandController extends BaseController {
    @Resource
    private DemandService demandService;
    @Autowired
    private DemandTrackService demandTrackService;

    /**
     * 需求单分页查询
     */
    @PostMapping(value = "/list")
    public ResponsePageBO<DemandPageVo> pageDemand(@RequestBody DemandPageReq req) {
        return demandService.pageDemand(req);
    }

    /**
     * 需求单详情展示
     */
    @DataAggregate
    @PostMapping(value = "/detail")
    public ResponseBO<DemandDetailVo> detailDemand(@RequestBody @Valid DemandDetailReq req) {
        return demandService.detailDemand(req);
    }

    /**
     * 需求单接单操作
     */
    @PostMapping(value = "/receipt")
    public ResponseBO<Boolean> receiptDemand(@RequestBody @Valid DemandDetailReq req) {
        return demandService.receiptDemand(req);
    }

    /**
     * 需求单验单操作
     */
    @Deprecated
    @PostMapping(value = "/check")
    public ResponseBO<Boolean> checkDemand(@RequestBody @Valid DemandDetailReq req) {
        return demandService.checkDemand(req);
    }

    /**
     * 需求单关闭操作
     */
    @PostMapping(value = "/close")
    public ResponseBO<Boolean> closeDemand(@RequestBody @Valid DemandCloseReq req) {
        return demandService.closeDemand(req);
    }

    /**
     * 需求单退回操作
     */
    @PostMapping(value = "/reject")
    public ResponseBO<Boolean> rejectDemand(@RequestBody @Valid DemandRejectReq req) {
        return demandService.rejectDemand(req);
    }

    /**
     * 需求单删除商品操作
     */
    @PostMapping(value = "/deleteGoods")
    public ResponseBO<Boolean> deleteGoods(@RequestBody @Valid DemandGoodsReq req) {
        if (Objects.isNull(req) || CollectionUtils.isEmpty(req.getDemandLineIds())) {
            throw new BusinessException(401, "请选择需要删除的商品");
        }
        return demandService.deleteGoods(req);
    }

    /**
     * 需求单历史版本查询
     */
    @PostMapping(value = "/version")
    public ResponseBO<List<DemandVersionVo>> demandVersion(@RequestBody @Valid DemandDetailReq req) {
        return demandService.demandVersion(req);
    }

    /**
     * 运营中台自制需求单 新增或者编辑
     */
    @PostMapping(value = "/save")
    public ResponseBO<DemandIdResp> saveDemand(@RequestBody @Valid DemandSaveReq req) {
        return demandService.saveDemand(req);
    }

    /**
     * 运营中台编辑页面提交提交需求单
     */
    @PostMapping(value = "/editSubmit")
    public ResponseBO<DemandIdResp> editSubmitDemand(@RequestBody @Valid DemandSaveReq req) {
        return demandService.editSubmitDemand(req);
    }


    /**
     * 运营中台列表页面提交需求单
     */
    @PostMapping(value = "/listSubmit")
    public ResponseBO<DemandListSubmitResp> listSubmitDemand(@RequestBody @Valid DemandListSubmitReq req) {
        return demandService.listSubmitDemand(req);
    }

    /**
     * 需求单跟进列表
     */
    @PostMapping(value = "/trackList")
    public ResponseBO<List<DemandTrackListResp>> trackList(@RequestBody @Valid DemandTrackListReq req){
        return demandTrackService.queryTrackList(req);
    }

    /**
     * 需求单跟进
     */
    @PostMapping(value = "/saveTrack")
    public ResponseBO saveTrack(@RequestBody @Valid DemandTrackSaveReq req){
        return demandTrackService.saveDemandTrack(req);
    }

    /**
     * 作废
     */
    @PostMapping(value = "/invalid")
    public ResponseBO invalid(@RequestBody @Valid DemandIdReq req) {
        return demandService.invalid(req);
    }
}
