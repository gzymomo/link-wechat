package com.linkwechat.fegin;

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
import com.linkwechat.fallback.QwCustomerFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author danmo
 * @description ??????????????????
 * @date 2022/3/13 20:54
 **/
@FeignClient(value = "${wecom.serve.linkwe-wecom}", fallback = QwCustomerFallbackFactory.class)
public interface QwCustomerClient {

    /**
     * ????????????????????????????????????????????????
     *
     * @param query corpid
     * @return
     */
    @PostMapping("/customer/getFollowerList")
    public AjaxResult<WeFollowUserListVo> getFollowUserList(@RequestBody WeBaseQuery query);

    /**
     * ???????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @PostMapping("/customer/addContactWay")
    public AjaxResult<WeAddWayVo> addContactWay(@RequestBody WeAddWayQuery query);

    /**
     * ?????????????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @PostMapping("/customer/getContactWay")
    public AjaxResult<WeContactWayVo> getContactWay(@RequestBody WeContactWayQuery query);

    /**
     * ?????????????????????????????????????????????
     *
     * @param query ??????
     * @return
     */
    @PostMapping("/customer/getContactWayList")
    public AjaxResult<WeContactWayListVo> getContactWayList(@RequestBody WeContactWayQuery query);

    /**
     * ??????????????????
     *
     * @param query ??????
     * @return
     */
    @PostMapping("/customer/updateContactWay")
    public AjaxResult<WeResultVo> updateContactWay(@RequestBody WeAddWayQuery query);

    /**
     * ??????????????????
     *
     * @param query ??????
     * @return
     */
    @PostMapping("/customer/delContactWay")
    public AjaxResult<WeResultVo> delContactWay(@RequestBody WeContactWayQuery query);


    /**
     * ?????????????????????
     *
     * @param query
     * @return WeGroupChatListVo
     */
    @PostMapping("/customer/groupchat/list")
    public AjaxResult<WeGroupChatListVo> getGroupChatList(@RequestBody WeGroupChatListQuery query);

    /**
     * ?????????????????????
     *
     * @param query
     * @return WeGroupChatDetailVo
     */
    @PostMapping("/customer/groupchat/get")
    public AjaxResult<WeGroupChatDetailVo> getGroupChatDetail(@RequestBody WeGroupChatDetailQuery query);


    /**
     * ?????????????????????
     *
     * @param query
     * @return
     */
    @GetMapping("/customer/getCorpTagList")
    public AjaxResult<WeCorpTagListVo> getCorpTagList(@SpringQueryMap WeCorpTagListQuery query);

    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/addCorpTag")
    public AjaxResult<WeCorpTagVo> addCorpTag(@RequestBody WeAddCorpTagQuery query);


    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @DeleteMapping("/customer/delCorpTag")
    public AjaxResult<WeResultVo> delCorpTag(@RequestBody WeCorpTagListQuery query);


    /**
     * ??????????????????
     *
     * @param weMarkTagQuery
     * @return
     */
    @PostMapping("/customer/makeCustomerLabel")
    public AjaxResult makeCustomerLabel(@RequestBody WeMarkTagQuery weMarkTagQuery);

    /**
     * ???????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/transferCustomer")
    public AjaxResult<WeTransferCustomerVo> transferCustomer(@RequestBody WeTransferCustomerQuery query);


    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/transferResult")
    public AjaxResult<WeTransferCustomerVo> transferResult(@RequestBody WeTransferCustomerQuery query);

    /**
     * ??????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/getCustomerDetail")
    public AjaxResult<WeCustomerDetailVo> getCustomerDetail(@RequestBody WeCustomerQuery query);

    /**
     * ????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/getBatchCustomerDetail")
    public AjaxResult<WeBatchCustomerDetailVo> getBatchCustomerDetail(@RequestBody WeBatchCustomerQuery query);


    /**
     * ??????????????????
     *
     * @param query
     * @return WeAddCustomerMsgVo
     */
    @PostMapping("/customer/group/msg/add")
    public AjaxResult<WeAddCustomerMsgVo> addMsgTemplate(@RequestBody WeAddCustomerMsgQuery query);

    /**
     * ????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/customer/group/msg/getList")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgList(@RequestBody WeGroupMsgListQuery query);

    /**
     * ????????????????????????????????????QwCustomerServiceImpl
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/customer/group/msg/getTask")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgTask(@RequestBody WeGetGroupMsgListQuery query);

    /**
     * ????????????????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/customer/group/msg/getSendResult")
    public AjaxResult<WeGroupMsgListVo> getGroupMsgSendResult(@RequestBody WeGetGroupMsgListQuery query);

    /**
     * ????????????????????????
     *
     * @param query
     * @return WeGroupMsgListVo
     */
    @PostMapping("/customer/sendWelcomeMsg")
    public AjaxResult<WeResultVo> sendWelcomeMsg(@RequestBody WeWelcomeMsgQuery query);

    /**
     * ??????????????????
     *
     * @param query
     * @return WeUserBehaviorDataVo
     */
    @PostMapping("/customer/getUserBehaviorData")
    public AjaxResult<WeUserBehaviorDataVo> getUserBehaviorData(@RequestBody WeUserBehaviorDataQuery query);


    /**
     * ????????????????????????????????????????????????
     *
     * @param query
     * @return WeGroupChatStatisticVo
     */
    @PostMapping("/customer/getGroupChatStatistic")
    public AjaxResult<WeGroupChatStatisticVo> getGroupChatStatistic(@RequestBody WeGroupChatStatisticQuery query);

    /**
     * ????????????????????????????????????????????????
     *
     * @param query
     * @return WeGroupChatStatisticVo
     */
    @PostMapping("/customer/getGroupChatStatisticByDay")
    public AjaxResult<WeGroupChatStatisticVo> getGroupChatStatisticByDay(@RequestBody WeGroupChatStatisticQuery query);

    /**
     * ???????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @PostMapping("/customer/addJoinWayForGroupChat")
    public AjaxResult<WeGroupChatAddJoinWayVo> addJoinWayForGroupChat(@RequestBody WeGroupChatAddJoinWayQuery joinWayQuery);


    /**
     * ?????????????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @GetMapping("/customer/getJoinWayForGroupChat")
    public AjaxResult<WeGroupChatGetJoinWayVo> getJoinWayForGroupChat(@SpringQueryMap WeGroupChatJoinWayQuery joinWayQuery);

    /**
     * ?????????????????????????????????
     *
     * @param joinWayQuery
     * @return
     */
    @PostMapping("/customer/delJoinWayForGroupChat")
    public AjaxResult<WeResultVo> delJoinWayForGroupChat(@RequestBody WeGroupChatJoinWayQuery joinWayQuery);

    /**
     * ???????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/resignedTransferCustomer")
    public AjaxResult<WeTransferCustomerVo> resignedTransferCustomer(@RequestBody WeTransferCustomerQuery query);

    /**
     * ??????????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/transferGroupChat")
    public AjaxResult<WeTransferCustomerVo> transferGroupChat(@RequestBody WeTransferGroupChatQuery query);


    /**
     * ?????????????????????????????????
     *
     * @param query
     * @return
     */
    @PostMapping("/customer/updateJoinWayForGroupChat")
    public AjaxResult<WeResultVo> updateJoinWayForGroupChat(@RequestBody WeGroupChatUpdateJoinWayQuery query);


    @PostMapping("/customer/unionIdToExternalUserId3rd")
    public AjaxResult<UnionidToExternalUserIdVo> unionIdToExternalUserId3rd(@RequestBody UnionidToExternalUserIdQuery query);

    /**
     * ??????????????????????????????
     * @param query
     * @return
     */
    @PostMapping("/customer/editCorpTag")
    public AjaxResult<WeResultVo> editCorpTag(@RequestBody WeUpdateCorpTagQuery query);
}
