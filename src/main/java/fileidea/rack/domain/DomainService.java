package fileidea.rack.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.NotFoundException;
import fileidea.rack.config.NameComProperties;
import fileidea.rack.integration.llm.CopyGenerator;
import fileidea.rack.integration.namecom.NameComClient;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;

import java.util.List;

@Service
public class DomainService {

    private static final Logger log = LoggerFactory.getLogger(DomainService.class);

    private final NameComClient nameCom;
    private final NameComProperties props;
    private final StoreRepository stores;
    private final CopyGenerator copy;

    public DomainService(
            NameComClient nameCom,
            NameComProperties props,
            StoreRepository stores,
            CopyGenerator copy
    ) {
        this.nameCom = nameCom;
        this.props = props;
        this.stores = stores;
        this.copy = copy;
    }

    public List<String> search(String query) {
        String seed = copy.storeName(query);
        if (!props.hasCreds()) {
            return List.of(seed + ".com", seed + ".store", seed + ".shop");
        }
        List<String> live = nameCom.search(seed);
        return live.isEmpty() ? List.of(seed + ".com") : live;
    }

    @Transactional
    public String register(Long storeId, String domain) {
        Store store = stores.findById(storeId).orElseThrow(() -> new NotFoundException("store " + storeId));
        if (props.hasCreds()) {
            if (!nameCom.available(domain)) {
                throw new IllegalArgumentException(domain + " is not available");
            }
            nameCom.register(domain);
            if (props.storefrontIp() != null && !props.storefrontIp().isBlank()) {
                nameCom.createDnsRecord(domain, "", "A", props.storefrontIp());
                nameCom.createSubdomain(domain, "www");
            }
            nameCom.createUrlForward(domain, "shop", "/shop/" + storeId);
        } else {
            log.info("name.com credentials missing — storing {} locally; storefront is /shop/{}", domain, storeId);
        }
        store.setDomain(domain);
        stores.save(store);
        return domain;
    }
}
