package com.vd.canary.obmp.order.service;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.obmp.order.api.request.*;
import com.vd.canary.obmp.order.api.request.order.InquiryCountReq;
import com.vd.canary.obmp.order.api.response.*;
import com.vd.canary.obmp.order.api.response.vo.ExportFileVO;
import com.vd.canary.obmp.order.api.response.vo.InquiryHeadVO;
import com.vd.canary.obmp.order.api.response.vo.InquiryLineVO;
import com.vd.canary.obmp.order.api.response.vo.order.InquiryCountResp;
import com.vd.canary.obmp.order.repository.entity.order.DemandHeadEntity;
import com.vd.canary.obmp.order.repository.entity.order.DemandLineEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface InquiryService {

    /**
     * 查询询价单详情
     * @param req
     * @return
     */
    ResponseBO<InquiryDetailQueryResp> queryDetail(InquiryDetailQueryReq req);

    /**
     * 供方及报价信息
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.InquiryDetailSupplierQuotesInfoResp>
     */
    ResponseBO<InquiryDetailSupplierQuotesInfoResp> supplierQuotesInfo(InquiryDetailSupplierQuotesInfoReq req);

    /***
     * 报价记录(报价商品列表)
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.InquiryDetailQuotesRecordResp>
     */
    ResponsePageBO<InquiryDetailQuotesRecordResp> quotesRecord(InquiryDetailQuotesRecordReq req);

    /**
     * 查询询价单列表
     * @param req
     * @return
     */
    ResponsePageBO<InquiryHeadVO> queryList(InquiryListQueryReq req);


    /**
     * 从需求单生成询价单
     *
     * @param headEntity     需求单信息
     * @param lineEntityList 需求单商品信息
     */
    void createInquiry(DemandHeadEntity headEntity, List<DemandLineEntity> lineEntityList);

    /**
     * 新增/修改询价单
     * @param req
     * @return
     */
    ResponseBO<InquirySaveResp> saveInquiry(@RequestBody InquirySaveReq req);

    /**
     * 新增/修改询价单(商城）
     * @param req
     * @return
     */
    ResponseBO<InquirySaveResp> saveMallInquiry(InquiryMallSaveReq req);


    /**
     * 批量提交询价单
     * @param req
     * @return
     */
    ResponseBO<InquirySubmitResp> submit(InquirySubmitReq req);


    /**
     * 批量撤回询价单
     * @param req
     * @return
     */
    ResponseBO<InquiryRevokeResp> revoke(InquiryRevokeReq req);


    /**
     * 批量作废询价单
     * @param req
     * @return
     */
    ResponseBO<InquiryInvalidResp> invalid(InquiryInvalidReq req);


    /**
     * 询价单导出(Excel格式)
     * @param req
     * @return
     */
    ResponseBO<ExportFileVO> exportExcel(InquiryDetailQueryReq req);

    /**
     * 询价单导出(PDF格式)
     * @param req
     * @return
     */
    ResponseBO<ExportFileVO> exportPDF(InquiryDetailQueryReq req);

    ResponsePageBO<InquiryLineVO> queryLineList(InquiryLineListQueryReq req);

    ResponsePageBO<InquiryHeadVO> queryMallList(InquiryListMallQueryReq req);
    /**
     *@Description  需方saas询价列表
     *@Param
     *@Return
     *@Author wpf
     *@Date
     */
//    ResponsePageBO<DemandSaasHeadVO> querySaasList(DemandSaasInfoReq req);
    /**
     *@Description 需方saas获取待报价和已报价的数量
     *@Param
     *@Return
     *@Author wpf
     *@Date
     */
    ResponseBO<InquiryCountResp> count(InquiryCountReq req);

    ResponseBO submitNuclearPrice(InquirySubmitNuclearReq req);

    ResponseBO refusedNuclearPrice(InquirySubmitNuclearReq req);

    ResponseBO markupNuclearPrice(InquirySubmitNuclearReq req);

    ResponseBO<Boolean> assignPurchaseManager(InquiryAssignReq req);

    ResponseBO<Boolean> accept(InquirySubmitReq req);

    ResponseBO<Boolean> returnDemand(InquirySubmitNuclearReq req);

    /**
     * 内部接口，需求单关联删除商品
     */
    ResponseBO<Boolean> deleteInquiry(InquiryDeleteReq req);

    /**
     * 内部接口，需求单关联关闭询价单
     */

    ResponseBO<Boolean> closeInquiry(InquiryDeleteReq req);

    void insertVersionByDemand(String demandId);
     ResponseBO<List<InquiryVersionVo>> inquiryVersionList(InquiryDetailQueryReq inquiryDetailQueryReq);

     ResponseBO inquiryVersionDetail(OrderVersionReq orderVersionReq);

    }
