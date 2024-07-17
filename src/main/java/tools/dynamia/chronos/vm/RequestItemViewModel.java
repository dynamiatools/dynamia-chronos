package tools.dynamia.chronos.vm;

import org.springframework.http.HttpMethod;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import tools.dynamia.chronos.ChronosHttpMethod;
import tools.dynamia.chronos.ChronosHttpRequestExecutor;
import tools.dynamia.chronos.ChronosHttpResponse;
import tools.dynamia.chronos.domain.RequestItem;
import tools.dynamia.chronos.domain.UserRole;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.domain.util.LabelValue;
import tools.dynamia.zk.AbstractViewModel;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.util.ZKUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Init(superclass = true)
public class RequestItemViewModel extends AbstractViewModel<RequestItem> {

    private LoggingService logger = new SLF4JLoggingService(RequestItemViewModel.class);
    private ChronosHttpResponse response;
    private List<LabelValue> headers;
    private List<LabelValue> responseHeaders;
    private ProjectsViewModel projectsVM;
    private EntityTreeNode<SimpleEntityUuid> node;
    private UserRole role;


    @Override
    protected void afterInitDefaults() {
        System.out.println(getModel());
        this.projectsVM = (ProjectsViewModel) ZKUtil.getExecutionArg("viewModel");
        this.node = (EntityTreeNode<SimpleEntityUuid>) ZKUtil.getExecutionArg("node");
        this.role = (UserRole) ZKUtil.getExecutionArg("role");

        loadHeaders();
    }

    private void loadHeaders() {
        headers = new ArrayList<>();
        addHeader("User-Agent", "Dynamia Chronos");
        addHeader("Accept", "*/*");
        addHeader("Connection", "keep-alive");
        addHeader("Accept-Encoding", "gzip, deflate, br");

        if (getModel().getServerAuthorization() != null && !getModel().getServerAuthorization().isBlank()) {
            addHeader("Authorization", getModel().getServerAuthorization());
        }
        addHeader("Content-Type", Objects.requireNonNullElse(getModel().getContentType(), "application/json"));


        var requestHeader = getModel().getHeaders();
        if (requestHeader != null) {
            requestHeader.forEach(this::addHeader);
        }
    }

    @Command
    public void send() {
        getModel().setHeaders(getHeadersMap());
        save();

        var executor = new ChronosHttpRequestExecutor(getModel(), loadVariables(), logger::info);

        executor.setOnResponse(r -> {
            responseHeaders = r.headers().map().entrySet().stream()
                    .map(e -> new LabelValue(e.getKey(), e.getValue().getFirst()))
                    .toList();

        });

        response = executor.execute();
        beutify();
        notifyChanges();
    }

    @Command
    public void save() {
        crudService().executeWithinTransaction(() -> {
            setModel(crudService().save(getModel()));
        });
        node.setEntity(getModel());
        node.setLabel(getModel().getName());
        node.setBadge(getModel().getHttpMethod().toString());

    }

    private Map<String, String> getHeadersMap() {
        return headers == null ? Map.of() : headers.stream().collect(Collectors.toMap(LabelValue::getLabel, lv -> lv.getValue().toString()));
    }

    @Command
    public void addHeader() {
        addHeader("", "");
        notifyChanges();
    }

    public void addHeader(String key, String value) {
        if (headers.stream().noneMatch(h -> h.getLabel().equals(key))) {
            headers.add(new LabelValue(key, value));
        }
    }

    private void beutify() {
        try {
            if (response != null && response.getResponse() != null && !response.getResponse().isBlank()) {
                //maybe is json
                if (response.getResponse().startsWith("{") && response.getResponse().endsWith("}")) {
                    Object json = StringPojoParser.parseJsonToPojo(response.getResponse(), Object.class);
                    response.setResponse(StringPojoParser.convertPojoToJson(json));
                }

            }
        } catch (Exception e) {
            System.out.println("fail");
        }
    }

    private List<Variable> loadVariables() {
        return null;
    }

    public ChronosHttpResponse getResponse() {
        return response;
    }

    public List<ChronosHttpMethod> getMethods() {
        return Stream.of(ChronosHttpMethod.values()).toList();
    }

    public List<LabelValue> getHeaders() {
        return headers;
    }

    public List<LabelValue> getResponseHeaders() {
        return responseHeaders;
    }
}
