package com.linkwechat.wecom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linkwechat.common.core.domain.entity.WeCorpAccount;

/**
 * 企业id相关配置Service接口
 * 
 * @author ruoyi
 * @date 2020-08-24
 */
public interface IWeCorpAccountService extends IService<WeCorpAccount>
{


    /**
     * 修改企业id相关配置
     * 
     * @param wxCorpAccount 企业id相关配置
     * @return 结果
     */
     void updateWeCorpAccount(WeCorpAccount wxCorpAccount);



    /**
     * 获取有效的企业id
     *
     * @return 结果
     */
     WeCorpAccount findValidWeCorpAccount();


    /**
     * 启用有效的企业微信账号
     * @param corpId
     */
     int startVailWeCorpAccount(String corpId);

    /**
     * 客户流失通知开关
     * @param status 开关状态
     * @return
     */
     void startCustomerChurnNoticeSwitch(String status);

    /**
     * 客户流失通知开关查询
     */
     String getCustomerChurnNoticeSwitch();



}
