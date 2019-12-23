package ai.guiji.unionpaydemo.service;

import java.util.Map;

public interface UnionPayService {

    Map<String, String> applyQrCode(Map<String, String> params);
}
