package com.vd.canary.obmp.order.controller.order;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.obmp.aggregate.annotation.DataAggregate;
import com.vd.canary.obmp.order.api.request.*;
import com.vd.canary.obmp.order.api.request.order.InquiryCountReq;
import com.vd.canary.obmp.order.api.response.*;
import com.vd.canary.obmp.order.api.response.operation.InquiryImportExcelResp;
import com.vd.canary.obmp.order.api.response.vo.ExportFileVO;
import com.vd.canary.obmp.order.api.response.vo.InquiryHeadVO;
import com.vd.canary.obmp.order.api.response.vo.InquiryLineVO;
import com.vd.canary.obmp.order.api.response.vo.order.InquiryCountResp;
import com.vd.canary.obmp.order.service.InquiryLineService;
import com.vd.canary.obmp.order.service.InquiryService;
import com.vd.canary.service.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @Author huangjinghua
 * @Description // 需方询价接口
 * @Date 4:29 下午 2020/4/1
 * @Param
 * @return
 **/
@Slf4j
@RestController()
@RequestMapping("/inquiry")
public class InquiryController extends BaseController {

	@Resource
    InquiryLineService inquiryLineService;

	@Resource
	InquiryService inquiryService;

    /**
     * 查询询报价单详情
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/queryDetail")
    public ResponseBO<InquiryDetailQueryResp> queryDetail(@RequestBody @Valid InquiryDetailQueryReq req) {
        return inquiryService.queryDetail(req);
    }

    /**
     * 供方及报价信息
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.InquiryDetailSupplierQuotesInfoResp>
     */
    @DataAggregate()
    @PostMapping(value = "/supplierQuotesInfo")
    public ResponseBO<InquiryDetailSupplierQuotesInfoResp> supplierQuotesInfo(@RequestBody @Valid InquiryDetailSupplierQuotesInfoReq req) {
        return inquiryService.supplierQuotesInfo(req);
    }

    /***
     * 报价记录(报价商品列表)
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.InquiryDetailQuotesRecordResp>
     */
    @PostMapping(value = "/quotesRecord")
    public ResponsePageBO<InquiryDetailQuotesRecordResp> quotesRecord(InquiryDetailQuotesRecordReq req) {
        return inquiryService.quotesRecord(req);
    }

	/**
	 * 需方询价单导出(Excel格式)
	 * @param inquiryDetailQueryReq
	 * @return
	 */
	@PostMapping(value = "/exportInquiryExcel")
	public ResponseBO<ExportFileVO> exportInquiryExcel(@RequestBody InquiryDetailQueryReq inquiryDetailQueryReq){
		return inquiryLineService.exportInquiryExcel(inquiryDetailQueryReq);
	}

	/**
	 * 需方询价单导出(PDF格式)
	 * @param inquiryDetailQueryReq
	 * @return
	 */
	@PostMapping(value = "/exportInquiryPdf")
	public ResponseBO<ExportFileVO> exportInquiryPdf(@RequestBody InquiryDetailQueryReq inquiryDetailQueryReq){
		return inquiryLineService.exportInquiryPdf(inquiryDetailQueryReq);
	}


	/**
	 * 新增/修改询价单
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/saveInquiry")
	public ResponseBO<InquirySaveResp> saveInquiry(@RequestBody @Valid InquirySaveReq req){
		return inquiryService.saveInquiry(req);
	}

	/**
	 * 新增/修改询价单(商城）
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/saveMallInquiry")
	public ResponseBO<InquirySaveResp> saveMallInquiry(@RequestBody @Valid InquiryMallSaveReq req){
		return inquiryService.saveMallInquiry(req);
	}

	/**
	 * 批量提交询价单
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/submit")
	public ResponseBO<InquirySubmitResp> submit(@RequestBody InquirySubmitReq req){
		return inquiryService.submit(req);
	}

	/**
	 * 需方询价单导入(excel格式)
	 * @param multipartRequest inquiryHeadId
	 * @return
	 */
	@PostMapping(value = "/importInquiryLineExcel")
	@Deprecated
	public ResponseBO<InquiryImportExcelResp> importInquiryLineExcel(@RequestBody MultipartHttpServletRequest multipartRequest){
		return inquiryLineService.importInquiryLineExcel(multipartRequest);
	}

	/**
	 * 批量撤回询价单
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/revoke")
	public ResponseBO<InquiryRevokeResp> revoke(@RequestBody InquiryRevokeReq req){
		return inquiryService.revoke(req);
	}

	/**
	 * 批量作废询价单
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/invalid")
	public ResponseBO<InquiryInvalidResp> invalid(@RequestBody InquiryInvalidReq req){
		return inquiryService.invalid(req);
	}

	/**
	 * 查询询价单明细信息
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/queryLineList")
	public ResponsePageBO<InquiryLineVO> queryLineList(@RequestBody InquiryLineListQueryReq req){
		return inquiryService.queryLineList(req);
	}

	/**
	 * 商城询价单列表查询接口
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/queryMallList")
	public ResponsePageBO<InquiryHeadVO> queryMallList(@RequestBody InquiryListMallQueryReq req){
		return inquiryService.queryMallList(req);
	}

    /**
     * 需方saas询价列表
     * @param req
     * @return
     */
//    @PostMapping(value = "/querySaasList")
//    public ResponsePageBO<DemandSaasHeadVO> querySaasList(@RequestBody @Valid DemandSaasInfoReq req){
//        return inquiryService.querySaasList(req);
//    }

    /**
     *@Description  需方saas获取待报价和已报价的数量
     *@Param
     *@Return
     *@Author wpf
     *@Date
     */
    @PostMapping(value = "/count")
    public ResponseBO<InquiryCountResp> count(@RequestBody @Valid InquiryCountReq req){
        return inquiryService.count(req);
    }


	/**
	 * 提交核价询价单 只更改状态（关闭关联的待报价供方询报价单、系统自动分配核价专员）
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "提交核价询价单", httpMethod = "POST",notes = "此接口只需要inquiryHeadId字段")
	@PostMapping(value = "/submit/nuclearPrice")
	public ResponseBO submitNuclearPrice(@RequestBody InquirySubmitNuclearReq req){
		return inquiryService.submitNuclearPrice(req);
	}
	/**
	 * 提交核价询价单之后可以 -》退回 只更改状态
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "核价退回", httpMethod = "POST",notes = "此接口只需要inquiryHeadId字段")
	@PostMapping(value = "/refused/nuclearPrice")
	public ResponseBO refusedNuclearPrice(@RequestBody InquirySubmitNuclearReq req){
		return inquiryService.refusedNuclearPrice(req);
	}

	/**
	 * 提交核价询价单之后可以 -》加价 （关联供方、销售价格、当需求单对应的所有需方询报价单都核价完成，系统自动创建销售订单）
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "核价加价", httpMethod = "POST",notes = "")
	@PostMapping(value = "/markup/nuclearPrice")
	public ResponseBO markupNuclearPrice(@RequestBody InquirySubmitNuclearReq req){
		return inquiryService.markupNuclearPrice(req);
	}

    /**
     * 查询询价单列表
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/queryList")
    public ResponsePageBO<InquiryHeadVO> queryList(@RequestBody InquiryListQueryReq req) {
        return inquiryService.queryList(req);
    }

    /**
     * 分配采购经理
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/assignProcurementManager")
    public ResponseBO<Boolean> assignPurchaseManager(@RequestBody InquiryAssignReq req) {
        return inquiryService.assignPurchaseManager(req);
    }

    /**
     * 采购经理接单
     * @param req
     * @return
     */
    @PostMapping(value = "/accept")
    public ResponseBO<Boolean> accept(@RequestBody InquirySubmitReq req) {
        return inquiryService.accept(req);
    }

	/**
	 * 退回需求单
	 * @param req
	 * @return
	 */
	@PostMapping(value = "/returnDemand")
	public ResponseBO<Boolean> returnDemand(@RequestBody InquirySubmitNuclearReq req) {
		return inquiryService.returnDemand(req);
	}

	@PostMapping(value = "/inquiryVersionList")
	ResponseBO<List<InquiryVersionVo>> inquiryVersionList(@RequestBody InquiryDetailQueryReq inquiryDetailQueryReq){
		return inquiryService.inquiryVersionList(inquiryDetailQueryReq);
	}

	@PostMapping(value = "/inquiryVersionDetail")
	ResponseBO inquiryVersionDetail(@RequestBody OrderVersionReq orderVersionReq){
		return inquiryService.inquiryVersionDetail(orderVersionReq);
	}
}
