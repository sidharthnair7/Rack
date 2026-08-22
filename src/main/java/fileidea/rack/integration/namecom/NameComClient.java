package fileidea.rack.integration.namecom;

import java.util.List;

public interface NameComClient {

    List<String> search(String query);

    boolean available(String domain);

    void register(String domain);

    void createDnsRecord(String domain, String host, String type, String answer);

    void createSubdomain(String domain, String host);

    void createUrlForward(String domain, String host, String destination);
}
