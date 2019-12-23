package ai.guiji.unionpaydemo.service;

import java.util.Map;

public interface PayService {

    Map<String, String> applyQrCode(Map<String, String> params);
}
