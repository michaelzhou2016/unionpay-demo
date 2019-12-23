package ai.guiji.unionpaydemo.service.impl;

import ai.guiji.unionpaydemo.config.SDKConfig;
import ai.guiji.unionpaydemo.service.AcpService;
import ai.guiji.unionpaydemo.service.PayService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * author: zhouliliang
 * Date: 2019/12/23 15:49
 * Description:
 */
@Service
public class PayServiceImpl implements PayService {

    @Override
    public Map<String, String> applyQrCode(Map<String, String> params) {
        String merId = params.get("merId");
        String txnAmt = params.get("txnAmt");
        String orderId = params.get("orderId");
        String txnTime = params.get("txnTime");

        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", SDKConfig.getConfig().getVersion());            //版本号 全渠道默认值
        contentData.put("encoding", "UTF-8");     //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "01");                        //交易类型 01:消费
        contentData.put("txnSubType", "07");                    //交易子类 07：申请消费二维码
        contentData.put("bizType", "000000");                    //填写000000
        contentData.put("channelType", "08");                    //渠道类型 08手机

        /***商户接入参数***/
        contentData.put("merId", merId);                        //商户号码，请改成自己申请的商户号或者open上注册得来的777商户号测试
        contentData.put("accessType", "0");                        //接入类型，商户接入填0 ，不需修改（0：直连商户， 1： 收单机构 2：平台商户）
        contentData.put("orderId", orderId);                    //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", txnTime);                    //订单发送时间，取系统时间，格式为YYYYMMDDhhmmss，必须取当前时间，否则会报txnTime无效
        contentData.put("txnAmt", txnAmt);                        //交易金额 单位为分，不能带小数点
        contentData.put("currencyCode", "156");                 //境内商户固定 156 人民币
        contentData.put("backUrl", SDKConfig.getConfig().getBackUrl());

        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData, "UTF-8");             //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestAppUrl = SDKConfig.getConfig().getBackRequestUrl();                                 //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData, requestAppUrl, "UTF-8");  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过
        return rspData;
    }
}
