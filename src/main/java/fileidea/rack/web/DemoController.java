package fileidea.rack.web;

import fileidea.rack.config.DemoData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(DemoData.class)
public class DemoController {

    private final DemoData demoData;

    public DemoController(DemoData demoData) {
        this.demoData = demoData;
    }

    @GetMapping("/api/demo")
    public Map<String, Long> demo() {
        return Map.of(
                "sellerId", demoData.sellerId(),
                "storeId", demoData.storeId()
        );
    }
}
