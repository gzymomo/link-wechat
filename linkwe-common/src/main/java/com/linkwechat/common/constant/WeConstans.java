package com.linkwechat.common.constant;


/**
 * @description: 企业微信相关常量
 * @author: HaoN
 * @create: 2020-08-26 17:01
 **/
public class WeConstans {

    /**
     * 微信授权token
     */
    public static final String WX_AUTH_ACCESS_TOKEN = "wx_auth_access_token";

    public static final String WX_AUTH_REFRESH_ACCESS_TOKEN = "wx_auth_refresh_access_token";

    /**
     * 微信通用token
     */
    public static final String WX_ACCESS_TOKEN = "wx_access_token";

    /**
     * 企微应用token
     */
    public static final String WE_COMMON_ACCESS_TOKEN = "we_common_access_token:{}";

    public static final String WE_AGENT_ACCESS_TOKEN = "we_agent_access_token:{}:{}";

    /**
     * 获取外部联系人相关 token
     */
    public static final String WE_CONTACT_ACCESS_TOKEN = "we_contact_access_token:{}";

    public static final String WE_ADDRESS_BOOK_ACCESS_TOKEN = "we_address_book_access_token:{}";

    /**
     * 供应商相关token
     */
    public static final String WE_PROVIDER_ACCESS_TOKEN = "we_provider_access_token:{}";

    /**
     * 会话存档相关token
     */
    public static final String WE_CHAT_ACCESS_TOKEN = "we_chat_access_token:{}";

    /**
     * 客服token
     */
    public static final String WE_KF_ACCESS_TOKEN = "we_kf_access_token:{}";

    /**
     * 对外收款token
     */
    public static final String WE_BILL_ACCESS_TOKEN = "we_bill_access_token:{}";


    /**
     * 同步功能提示语
     */
    public static final String SYNCH_TIP = "后台开始同步数据，请稍后关注进度";


    /**
     * 单人活码
     */
    public static final Integer SINGLE_EMPLE_CODE_TYPE = 1;


    /**
     * 通过二维码联系场景
     */
    public static final Integer QR_CODE_EMPLE_CODE_SCENE = 2;


    /**
     * 不存在外部联系人的关系
     */
    public static final Integer NOT_EXIST_CONTACT = 84061;

    public static final String COMMA = ",";


    /**
     * 业务id类型1:组织机构id,2:成员id
     */
    public static final Integer USE_SCOP_BUSINESSID_TYPE_USER = 2;
    public static final Integer USE_SCOP_BUSINESSID_TYPE_ORG = 1;
    public static final Integer USE_SCOP_BUSINESSID_TYPE_ALL = 3;

    /**
     * 客户流失通知开关 0:关闭 1:开启
     */
    public static final String DEL_FOLLOW_USER_SWITCH_CLOSE = "0";
    public static final String DEL_FOLLOW_USER_SWITCH_OPEN = "1";

    /**
     * 任务裂变用户活码state前缀
     */
    public static final String FISSION_PREFIX = "fis-";

    /**
     * 活码前缀
     */
    public static final String WE_QR_CODE_PREFIX = "we_qr";


    /**
     * 门店活码前缀
     */
    public static final String WE_STORE_CODE_PREFIX = "we_sc";


    /**
     * 门店导购员或群前缀
     */
    public static final String WE_STORE_CODE_CONFIG_PREFIX = "we_sc_conf";


    /**
     * 新客拉群
     */
    public static final String WE_QR_XKLQ_PREFIX = "we_xklq";


    /**
     * 二维码地址
     */
    public static final String JOINCORPQR = "joinCorpQr";

    /**
     * 二维码有效时间（默认6天）
     */
    public static final Integer JOINCORPQR_EFFETC_TIME = 8640;
}
