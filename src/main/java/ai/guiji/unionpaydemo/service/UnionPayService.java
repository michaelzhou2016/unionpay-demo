package ai.guiji.unionpaydemo.service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public interface UnionPayService {

    Map<String, String> applyQrCode(Map<String, String> params);

    String notify(HttpServletRequest req) throws IOException;
}
