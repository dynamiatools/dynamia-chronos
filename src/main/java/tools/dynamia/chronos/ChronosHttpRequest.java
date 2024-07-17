package tools.dynamia.chronos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.domain.contraints.NotEmpty;
import tools.dynamia.domain.jpa.SimpleEntityUuid;

import java.time.Duration;

@MappedSuperclass
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChronosHttpRequest extends SimpleEntityUuid {

    @NotEmpty
    private String name;
    @Lob
    @Column(columnDefinition = "longtext")
    private String description;
    @NotEmpty
    @NotNull
    @Column(length = 2000)
    private String serverHost;
    private String serverAuthorization;
    @NotNull
    private Duration serverTimeout = Duration.ofSeconds(30);
    @Lob
    @Column(columnDefinition = "longtext")
    private String requestBody; //optional
    private String contentType = "application/json";
    private boolean active = true;
    @NotNull
    private ChronosHttpMethod httpMethod = ChronosHttpMethod.GET;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public @NotNull String getServerHost() {
        return serverHost;
    }

    public void setServerHost(@NotNull String serverHost) {
        this.serverHost = serverHost;
    }

    public String getServerAuthorization() {
        return serverAuthorization;
    }

    public void setServerAuthorization(String serverAuthorization) {
        this.serverAuthorization = serverAuthorization;
    }

    public @NotNull Duration getServerTimeout() {
        return serverTimeout;
    }

    public void setServerTimeout(@NotNull Duration serverTimeout) {
        this.serverTimeout = serverTimeout;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ChronosHttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(ChronosHttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return getName();
    }
}
