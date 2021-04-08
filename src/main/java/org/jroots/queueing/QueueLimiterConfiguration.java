package org.jroots.queueing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;

public class QueueLimiterConfiguration extends Configuration {
    @NotEmpty
    private String template;

    @NotEmpty
    private String defaultName = "Stranger";

    @NotEmpty
    private String inboundSqsUrl;

    @NotEmpty
    private String outboundSqsUrl;

    @NotEmpty
    private String limitsTableName;

    @NotEmpty
    private String hazelcastClusterIp;

    @JsonProperty
    public String getTemplate() {
        return template;
    }

    @JsonProperty
    public void setTemplate(String template) {
        this.template = template;
    }

    @JsonProperty
    public String getDefaultName() {
        return defaultName;
    }

    @JsonProperty
    public void setDefaultName(String name) {
        this.defaultName = name;
    }

    @JsonProperty
    public String getInboundSqsUrl() {
        return inboundSqsUrl;
    }

    @JsonProperty
    public void setInboundSqsUrl(String inboundSqsUrl) {
        this.inboundSqsUrl = inboundSqsUrl;
    }

    @JsonProperty
    public String getOutboundSqsUrl() {
        return outboundSqsUrl;
    }

    @JsonProperty
    public void setOutboundSqsUrl(String outboundSqsUrl) {
        this.outboundSqsUrl = outboundSqsUrl;
    }

    public String getLimitsTableName() {
        return limitsTableName;
    }

    public void setLimitsTableName(String limitsTableName) {
        this.limitsTableName = limitsTableName;
    }

    public String getHazelcastClusterIp() {
        return hazelcastClusterIp;
    }

    public void setHazelcastClusterIp(String hazelcastClusterIp) {
        this.hazelcastClusterIp = hazelcastClusterIp;
    }
}
