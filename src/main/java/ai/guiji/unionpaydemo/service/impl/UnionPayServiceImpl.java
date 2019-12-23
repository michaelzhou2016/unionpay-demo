package ai.guiji.unionpaydemo.service.impl;

import ai.guiji.unionpaydemo.config.SDKConfig;
import ai.guiji.unionpaydemo.service.AcpService;
import ai.guiji.unionpaydemo.service.UnionPayService;
import ai.guiji.unionpaydemo.utils.CertUtil;
import ai.guiji.unionpaydemo.constant.SDKConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ai.guiji.unionpaydemo.constant.SDKConstants.UTF_8_ENCODING;

/**
 * author: zhouliliang
 * Date: 2019/12/23 15:49
 * Description:
 */
@Slf4j
@Service
public class UnionPayServiceImpl implements UnionPayService {

    @PostConstruct
    public void init() {
        System.out.println("银联支付初始化");
        SDKConfig.getConfig().loadPropertiesFromSrc(); //从classpath加载acp_sdk.properties文件
        CertUtil.init();
    }

    //申请二维码（主扫）https://open.unionpay.com/tjweb/acproduct/APIList?apiservId=468&acpAPIId=726
    @Override
    public Map<String, String> applyQrCode(Map<String, String> params) {
        String txnAmt = params.get("txnAmt");
        String orderId = params.get("orderId");
        String txnTime = params.get("txnTime");

        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", SDKConfig.getConfig().getVersion());            //版本号 全渠道默认值
        contentData.put("encoding", UTF_8_ENCODING);     //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "01");                        //交易类型 01:消费
        contentData.put("txnSubType", "07");                    //交易子类 07：申请消费二维码
        contentData.put("bizType", "000000");                    //填写000000
        contentData.put("channelType", "08");                    //渠道类型 08手机

        /***商户接入参数***/
        contentData.put("merId", SDKConfig.getConfig().getMerchantId());                       //商户号码，请改成自己申请的商户号或者open上注册得来的777商户号测试
        contentData.put("accessType", "0");                        //接入类型，商户接入填0 ，不需修改（0：直连商户， 1： 收单机构 2：平台商户）
        contentData.put("orderId", orderId);                    //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", txnTime);                    //订单发送时间，取系统时间，格式为YYYYMMDDhhmmss，必须取当前时间，否则会报txnTime无效
        contentData.put("txnAmt", txnAmt);                        //交易金额 单位为分，不能带小数点
        contentData.put("currencyCode", "156");                 //境内商户固定 156 人民币

        //后台通知地址（需设置为【外网】能访问 http https均可），支付成功后银联会自动将异步通知报文post到商户上送的该地址，失败的交易银联不会发送后台通知
        //后台通知参数详见open.unionpay.com帮助中心 下载  产品接口规范  网关支付产品接口规范 消费交易 商户通知
        //注意:1.需设置为外网能访问，否则收不到通知    2.http https均可  3.收单后台通知后需要10秒内返回http200或302状态码
        //    4.如果银联通知服务器发送通知后10秒内未收到返回状态码或者应答码非http200，那么银联会间隔一段时间再次发送。总共发送5次，每次的间隔时间为0,1,2,4分钟。
        //    5.后台通知地址如果上送了带有？的参数，例如：http://abc/web?a=b&c=d 在后台通知处理程序验证签名之前需要编写逻辑将这些字段去掉再验签，否则将会验签失败
        contentData.put("backUrl", SDKConfig.getConfig().getBackUrl());

        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData, UTF_8_ENCODING);             //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestAppUrl = SDKConfig.getConfig().getBackRequestUrl();                                 //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData, requestAppUrl, UTF_8_ENCODING);  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
//        if (!rspData.isEmpty()) {
//            if (AcpService.validate(rspData, "UTF-8")) {
//                log.info("验证签名成功");
//                String respCode = rspData.get("respCode");
//                if ("00".equals(respCode)) {
//                    //成功
//                } else {
//                    //其他应答码为失败请排查原因或做失败处理
//                }
//            } else {
//                log.error("验证签名失败");
//            }
//        } else {
//            //未返回正确的http状态
//            log.error("未获取到返回报文或返回http状态码非200");
//        }

        return rspData;
    }

    @Override
    public String notify(HttpServletRequest req) throws IOException {
        log.info("notify接收后台通知开始");

        String encoding = req.getParameter(SDKConstants.param_encoding);
        // 获取银联通知服务器发送的后台通知参数
        Map<String, String> reqParam = getAllRequestParam(req);

        Map<String, String> valideData = null;
        if (null != reqParam && !reqParam.isEmpty()) {
            Iterator<Map.Entry<String, String>> it = reqParam.entrySet().iterator();
            valideData = new HashMap<String, String>(reqParam.size());
            while (it.hasNext()) {
                Map.Entry<String, String> e = it.next();
                String key = e.getKey();
                String value = e.getValue();

                valideData.put(key, value);
            }
        }

        //重要！验证签名前不要修改reqParam中的键值对的内容，否则会验签不过
        if (!AcpService.validate(valideData, encoding)) {
            log.error("验证签名结果[失败].");
            //验签失败，需解决验签问题

        } else {
            log.info("验证签名结果[成功].");
            //【注：为了安全验签成功才应该写商户的成功处理逻辑】交易成功，更新商户订单状态

            String orderId = valideData.get("orderId"); //获取后台通知的数据，其他字段也可用类似方式获取

            String respCode = valideData.get("respCode");
            String txnAmt = valideData.get("txnAmt");

            String queryId = valideData.get("queryId"); //查询流水号
            String traceTime = valideData.get("traceTime");
            String payCardType = valideData.get("payCardType"); //支付卡类型
            String paymentMethodMethod; //PayPaymentMethod里面的method字段
            if (StringUtils.isEmpty(payCardType)) {
                paymentMethodMethod = "UNION";  //对之前代码做兼容，如果没有支付卡类型的情况走默认
            } else {
                paymentMethodMethod = "UNION-" + payCardType;
            }

            //判断respCode=00、A6后，对涉及资金类的交易，请再发起查询接口查询，确定交易成功后更新数据库。
            if ("00".equals(respCode)) {  // 00 交易成功

                //todo 若交易成功
            } else if ("A6".equals(respCode)) {  // A6 部分成功

            }

        }

        log.info("notify接收后台通知结束");
        //返回给银联服务器http 200  状态码
        return "ok";
    }

    @Override
    public void query(Map<String, String> params) {
        String orderId = params.get("orderId");
        String txnTime = params.get("txnTime");

        Map<String, String> data = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        data.put("version", SDKConfig.getConfig().getVersion());                 //版本号
        data.put("encoding", UTF_8_ENCODING);          //字符集编码 可以使用UTF-8,GBK两种方式
        data.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        data.put("txnType", "00");                             //交易类型 00-默认
        data.put("txnSubType", "00");                          //交易子类型  默认00
        data.put("bizType", "000201");                         //业务类型

        /***商户接入参数***/
        data.put("merId", SDKConfig.getConfig().getMerchantId());                               //商户号码，请改成自己申请的商户号或者open上注册得来的777商户号测试
        data.put("accessType", "0");                           //接入类型，商户接入固定填0，不需修改

        /***要调通交易以下字段必须修改***/
        data.put("orderId", orderId);                            //****商户订单号，每次发交易测试需修改为被查询的交易的订单号
        data.put("txnTime", txnTime);                            //****订单发送时间，每次发交易测试需修改为被查询的交易的订单发送时间

        /**请求参数设置完毕，以下对请求参数进行签名并发送http post请求，接收同步应答报文------------->**/

        Map<String, String> reqData = AcpService.sign(data, UTF_8_ENCODING);            //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String url = SDKConfig.getConfig().getSingleQueryUrl();                                //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.singleQueryUrl
        Map<String, String> rspData = AcpService.post(reqData, url, UTF_8_ENCODING); //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        if (!rspData.isEmpty()) {
            if (AcpService.validate(rspData, UTF_8_ENCODING)) {
                log.info("验证签名成功");
                if (("00").equals(rspData.get("respCode"))) {//如果查询交易成功
                    String origRespCode = rspData.get("origRespCode");
                    if (("00").equals(origRespCode)) {
                        //交易成功，更新商户订单状态
                        //TODO
                    } else if (("03").equals(origRespCode) ||
                            ("04").equals(origRespCode) ||
                            ("05").equals(origRespCode)) {
                        //订单处理中或交易状态未明，需稍后发起交易状态查询交易 【如果最终尚未确定交易是否成功请以对账文件为准】
                        //TODO
                    } else {
                        //其他应答码为交易失败
                        //TODO
                    }
                } else if (("34").equals(rspData.get("respCode"))) {
                    //订单不存在，可认为交易状态未明，需要稍后发起交易状态查询，或依据对账结果为准

                } else {//查询交易本身失败，如应答码10/11检查查询报文是否正确
                    //TODO
                }
            } else {
                log.error("验证签名失败");
                //TODO 检查验证签名失败的原因
            }
        } else {
            //未返回正确的http状态
            log.error("未获取到返回报文或返回http状态码非200");
        }
    }

    /**
     * 获取请求参数中所有的信息
     *
     * @param request
     * @return
     */
    private Map<String, String> getAllRequestParam(final HttpServletRequest request) {
        Map<String, String> res = new HashMap<String, String>();
        Enumeration<?> temp = request.getParameterNames();
        if (null != temp) {
            while (temp.hasMoreElements()) {
                String en = (String) temp.nextElement();
                String value = request.getParameter(en);
                res.put(en, value);
                //在报文上送时，如果字段的值为空，则不上送<下面的处理为在获取所有参数数据时，判断若值为空，则删除这个字段>
                //System.out.println("ServletUtil类247行  temp数据的键=="+en+"     值==="+value);
                if (null == res.get(en) || "".equals(res.get(en))) {
                    res.remove(en);
                }
            }
        }

        return res;
    }
}
