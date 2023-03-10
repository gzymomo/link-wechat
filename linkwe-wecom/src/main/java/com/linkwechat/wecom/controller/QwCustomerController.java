package com.linkwechat.wecom.controller;

import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.domain.wecom.query.WeBaseQuery;
import com.linkwechat.domain.wecom.query.customer.UnionidToExternalUserIdQuery;
import com.linkwechat.domain.wecom.query.customer.WeBatchCustomerQuery;
import com.linkwechat.domain.wecom.query.customer.WeCustomerQuery;
import com.linkwechat.domain.wecom.query.customer.groupchat.*;
import com.linkwechat.domain.wecom.query.customer.msg.WeAddCustomerMsgQuery;
import com.linkwechat.domain.wecom.query.customer.msg.WeGetGroupMsgListQuery;
import com.linkwechat.domain.wecom.query.customer.msg.WeGroupMsgListQuery;
import com.linkwechat.domain.wecom.query.customer.msg.WeWelcomeMsgQuery;
import com.linkwechat.domain.wecom.query.customer.state.WeGroupChatStatisticQuery;
import com.linkwechat.domain.wecom.query.customer.state.WeUserBehaviorDataQuery;
import com.linkwechat.domain.wecom.query.customer.tag.WeAddCorpTagQuery;
import com.linkwechat.domain.wecom.query.customer.tag.WeCorpTagListQuery;
import com.linkwechat.domain.wecom.query.customer.tag.WeMarkTagQuery;
import com.linkwechat.domain.wecom.query.customer.tag.WeUpdateCorpTagQuery;
import com.linkwechat.domain.wecom.query.customer.transfer.WeTransferCustomerQuery;
import com.linkwechat.domain.wecom.query.customer.transfer.WeTransferGroupChatQuery;
import com.linkwechat.domain.wecom.query.qr.WeAddWayQuery;
import com.linkwechat.domain.wecom.query.qr.WeContactWayQuery;
import com.linkwechat.domain.wecom.vo.WeResultVo;
import com.linkwechat.domain.wecom.vo.customer.UnionidToExternalUserIdVo;
import com.linkwechat.domain.wecom.vo.customer.WeBatchCustomerDetailVo;
import com.linkwechat.domain.wecom.vo.customer.WeCustomerDetailVo;
import com.linkwechat.domain.wecom.vo.customer.WeFollowUserListVo;
import com.linkwechat.domain.wecom.vo.customer.groupchat.WeGroupChatAddJoinWayVo;
import com.linkwechat.domain.wecom.vo.customer.groupchat.WeGroupChatDetailVo;
import com.linkwechat.domain.wecom.vo.customer.groupchat.WeGroupChatGetJoinWayVo;
import com.linkwechat.domain.wecom.vo.customer.groupchat.WeGroupChatListVo;
import com.linkwechat.domain.wecom.vo.customer.msg.WeAddCustomerMsgVo;
import com.linkwechat.domain.wecom.vo.customer.msg.WeGroupMsgListVo;
import com.linkwechat.domain.wecom.vo.customer.state.WeGroupChatStatisticVo;
import com.linkwechat.domain.wecom.vo.customer.state.WeUserBehaviorDataVo;
import com.linkwechat.domain.wecom.vo.customer.tag.WeCorpTagListVo;
import com.linkwechat.domain.wecom.vo.customer.tag.WeCorpTagVo;
import com.linkwechat.domain.wecom.vo.customer.transfer.WeTransferCustomerVo;
import com.linkwechat.domain.wecom.vo.qr.WeAddWayVo;
import com.linkwechat.domain.wecom.vo.qr.WeContactWayListVo;
import com.linkwechat.domain.wecom.vo.qr.WeContactWayVo;
import com.linkwechat.wecom.service.IQwCustomerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author danmo
 * @description ??????????????????
 * @date 2022/3/20 14:54
 **/
@Api(tags = "??????????????????")
@RestController
@RequestMapping("customer")
public class QwCustomerController {

    @Autowired
    private IQwCustomerService qwCustomerService;

    /**
     * ????????????????????????????????????????????????
     *
     * @param query corpid
     * @return
     */
    @ApiOperation(value = "????????????????????????????????????????????????", httpMethod = "POST")
    @PostMapping("/getFollowerList")
    public AjaxResult<WeFollowUserListVo> getFollowUserList(@RequestBody WeBaseQuery query) {
        WeFollowUserListVo followUserList = qwCustomerService.getFollowUserList(query);
        return AjaxResult.success(followUserList);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @ApiOperation(value = "???????????????????????????????????????", httpMethod = "POST")
    @PostMapping("/addContactWay")
    public AjaxResult<WeAddWayVo> addContactWay(@RequestBody WeAddWayQuery query) {
        WeAddWayVo weAddWayVo = qwCustomerService.addContactWay(query);
        return AjaxResult.success(weAddWayVo);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @ApiOperation(value = "?????????????????????????????????????????????", httpMethod = "POST")
    @PostMapping("/getContactWay")
    public AjaxResult<WeContactWayVo> getContactWay(@RequestBody WeContactWayQuery query) {
        WeContactWayVo weContactWayVo = qwCustomerService.getContactWay(query);
        return AjaxResult.success(weContactWayVo);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @ApiOperation(value = "?????????????????????????????????????????????", httpMethod = "POST")
    @PostMapping("/getContactWayList")
    public AjaxResult<WeContactWayListVo> getContactWayList(@RequestBody WeContactWayQuery query) {
        WeContactWayListVo weContactWayVo = qwCustomerService.getContactWayList(query);
        return AjaxResult.success(weContactWayVo);
    }

    /**
     * ??????????????????
     *
     * @param query ??????
     * @return
     */
    @ApiOperation(value = "??????????????????", httpMethod = "POST")
    @PostMapping("/updateContactWay")
    public AjaxResult<WeResultVo> updateContactWay(@RequestBody WeAddWayQuery query) {
        WeResultVo resultVo = qwCustomerService.updateContactWay(query);
        return AjaxResult.success(resultVo);
    }

    /**
     * ??????????????????
     *
     * @param query ??????
     * @return
     */
    @ApiOperation(value = "??????????????????", httpMethod = "POST")
    @PostMapping("/delContactWay")
    public AjaxResult<WeResultVo> delContactWay(@RequestBody WeContactWayQuery query) {
        WeResultVo resultVo = qwCustomerService.delContactWay(query);
        return AjaxResult.success(resultVo);
    }

    /**
     * ?????????????????????
     *
     * @param query
     * @return WeGroupChatListVo
     */
    @ApiOperation(value = "?????????????????????", httpMethod = "POST")
    @PostMapping("/groupchat/list")
    public AjaxResult<WeGroupChatListVo> getGroupChatList(@RequestBody WeGroupChatListQuery query) {
        WeGroupChatListVo groupChatList = qwCustomerService.getGroupChatList(query);
        return AjaxResult.success(groupChatList);
    }

    /**
     * ?????????????????????
     *
     * @param query
     * @return WeGroupChatDetailVo
     */
    @ApiOperation(value = "?????????????????????", httpMethod = "POST")
    @PostMapping("/groupchat/get")
    public AjaxResult<WeGroupChatDetailVo> getGroupChatDetail(@RequestBody WeGroupChatDetailQuery query) {
        WeGroupChatDetailVo groupChatDetail = qwCustomerService.getGroupChatDetail(query);
        return AjaxResult.success(groupChatDetail);
    }

    /**
     * ?????????????????????
     *
     * @param query
     * @return
     */
    @GetMapping("/getCorpTagList")
    public AjaxResult<WeCorpTagListVo> getCorpTagList(WeCorpTagListQuery query) {
        return AjaxResult.success(qwCustomerService.getCorpTagList(query));
    }

    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/addCorpTag")
    public AjaxResult<WeCorpTagVo> addCorpTag(@RequestBody WeAddCorpTagQuery query) {

        return AjaxResult.success(
                qwCustomerService.addCorpTag(query)
        );

    }


    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @DeleteMapping("/delCorpTag")
    public AjaxResult<WeResultVo> delCorpTag(@RequestBody WeCorpTagListQuery query) {

        return AjaxResult.success(
                qwCustomerService.delCorpTag(query)
        );
    }


    /**
     * ??????????????????
     *
     * @param weMarkTagQuery
     * @return
     */
    @PostMapping("/makeCustomerLabel")
    public AjaxResult makeCustomerLabel(@RequestBody WeMarkTagQuery weMarkTagQuery) {
        qwCustomerService.makeCustomerLabel(weMarkTagQuery);

        return AjaxResult.success();
    }


    /**
     * ???????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/transferCustomer")
    public AjaxResult<WeTransferCustomerVo> transferCustomer(@RequestBody WeTransferCustomerQuery query) {

        return AjaxResult.success(qwCustomerService.transferCustomer(query));
    }


    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/transferResult")
    public AjaxResult<WeTransferCustomerVo> transferResult(@RequestBody WeTransferCustomerQuery query) {

        return AjaxResult.success(
                qwCustomerService.transferResult(query)
        );
    }


    /**
     * ??????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/getCustomerDetail")
    public AjaxResult<WeCustomerDetailVo> getCustomerDetail(@RequestBody WeCustomerQuery query) {
        return AjaxResult.success(qwCustomerService.getCustomerDetail(query));
    }

    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/getBatchCustomerDetail")
    public AjaxResult<WeBatchCustomerDetailVo> getBatchCustomerDetail(@RequestBody WeBatchCustomerQuery query) {
        return AjaxResult.success(qwCustomerService.getBatchCustomerDetail(query));

    }

    /**
     * ??????????????????
     *
     * @param query
     * @return WeAddCustomerMsgVo
     */
    @PostMapping("/group/msg/add")
    public AjaxResult<WeAddCustomerMsgVo> addMsgTemplate(@RequestBody WeAddCustomerMsgQuery query) {
        return AjaxResult.success(qwCustomerService.addMsgTemplate(query));
    }

    /**
     * ????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/group/msg/getList")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgList(@RequestBody WeGroupMsgListQuery query) {
        return AjaxResult.success(qwCustomerService.getGroupMsgList(query));
    }

    /**
     * ????????????????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/group/msg/getTask")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgTask(@RequestBody WeGetGroupMsgListQuery query) {
        return AjaxResult.success(qwCustomerService.getGroupMsgTask(query));
    }

    /**
     * ????????????????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/group/msg/getSendResult")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgSendResult(@RequestBody WeGetGroupMsgListQuery query) {
        return AjaxResult.success(qwCustomerService.getGroupMsgSendResult(query));
    }

    /**
     * ????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/sendWelcomeMsg")
    public AjaxResult<WeResultVo> sendWelcomeMsg(@RequestBody WeWelcomeMsgQuery query) {
        return AjaxResult.success(qwCustomerService.sendWelcomeMsg(query));
    }

    /**
     * ??????????????????
     *
     * @param query
     * @return WeUserBehaviorDataVo
     */
    @PostMapping("/getUserBehaviorData")
    public AjaxResult<WeUserBehaviorDataVo> getUserBehaviorData(@RequestBody WeUserBehaviorDataQuery query) {
        return AjaxResult.success(qwCustomerService.getUserBehaviorData(query));
    }


    /**
     * ????????????????????????????????????????????????
     *
     * @param query
     * @return WeGroupChatStatisticVo
     */
    @PostMapping("/getGroupChatStatistic")
    public AjaxResult<WeGroupChatStatisticVo> getGroupChatStatistic(@RequestBody WeGroupChatStatisticQuery query) {
        return AjaxResult.success(qwCustomerService.getGroupChatStatistic(query));
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param query
     * @return WeGroupChatStatisticVo
     */
    @PostMapping("/getGroupChatStatisticByDay")
    public AjaxResult<WeGroupChatStatisticVo> getGroupChatStatisticByDay(@RequestBody WeGroupChatStatisticQuery query) {
        return AjaxResult.success(qwCustomerService.getGroupChatStatisticByDay(query));
    }


    /**
     * ???????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @PostMapping("/addJoinWayForGroupChat")
    public AjaxResult<WeGroupChatAddJoinWayVo> addJoinWayForGroupChat(@RequestBody WeGroupChatAddJoinWayQuery joinWayQuery) {

        return AjaxResult.success(
                qwCustomerService.addJoinWayForGroupChat(joinWayQuery)
        );
    }


    /**
     * ?????????????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @GetMapping("/getJoinWayForGroupChat")
    public AjaxResult<WeGroupChatGetJoinWayVo> getJoinWayForGroupChat(WeGroupChatJoinWayQuery joinWayQuery) {

        return AjaxResult.success(
                qwCustomerService.getJoinWayForGroupChat(joinWayQuery)
        );
    }


    /**
     * ?????????????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @DeleteMapping("/delJoinWayForGroupChat")
    public AjaxResult<WeResultVo> delJoinWayForGroupChat(WeGroupChatJoinWayQuery joinWayQuery) {

        return AjaxResult.success(
                qwCustomerService.delJoinWayForGroupChat(joinWayQuery)
        );

    }

    /**
     * ???????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/resignedTransferCustomer")
    public AjaxResult<WeTransferCustomerVo> resignedTransferCustomer(@RequestBody WeTransferCustomerQuery query) {
        return AjaxResult.success(
                qwCustomerService.resignedTransferCustomer(query)
        );
    }


    /**
     * ??????????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/transferGroupChat")
    public AjaxResult<WeTransferCustomerVo> transferGroupChat(@RequestBody WeTransferGroupChatQuery query) {
        return AjaxResult.success(
                qwCustomerService.transferGroupChat(query)
        );

    }

    /**
     * ?????????????????????????????????
     * @param query
     * @return
     */
    @PostMapping("/updateJoinWayForGroupChat")
    public AjaxResult<WeResultVo> updateJoinWayForGroupChat(@RequestBody WeGroupChatUpdateJoinWayQuery query){

        return AjaxResult.success(
                qwCustomerService.updateJoinWayForGroupChat(query)
        );
    }


    /**
     * ??????????????????????????????
     * @param query
     * @return
     */
    @PostMapping("/editCorpTag")
    public AjaxResult<WeResultVo> editCorpTag(@RequestBody WeUpdateCorpTagQuery query){

        return AjaxResult.success(
                qwCustomerService.editCorpTag(query)
        );
    }



}
