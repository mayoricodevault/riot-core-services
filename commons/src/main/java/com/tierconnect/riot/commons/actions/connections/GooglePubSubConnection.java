package com.tierconnect.riot.commons.actions.connections;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Google pub/sub connection parameters. Credential.
 * It overrides "equals" and "hashcode" in order to compare two instances by their credential values.
 * Created by vramos on 6/20/17.
 */
public class GooglePubSubConnection implements Serializable{
    public String type;
    @JsonProperty("project_id")
    public String projectId;
    @JsonProperty("private_key_id")
    public String privateKeyId;
    @JsonProperty("private_key")
    public String privateKey;
    @JsonProperty("client_email")
    public String clientEmail;
    @JsonProperty("client_id")
    public String clientId;
    @JsonProperty("auth_uri")
    public String authUri;
    @JsonProperty("token_uri")
    public String tokenUri;
    @JsonProperty("auth_provider_x509_cert_url")
    public String authProviderX509CertUrl;
    @JsonProperty("client_x509_cert_url")
    public String clientX509CertUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GooglePubSubConnection that = (GooglePubSubConnection) o;
        return Objects.equal(type, that.type) &&
                Objects.equal(projectId, that.projectId) &&
                Objects.equal(privateKeyId, that.privateKeyId) &&
                Objects.equal(privateKey, that.privateKey) &&
                Objects.equal(clientEmail, that.clientEmail) &&
                Objects.equal(clientId, that.clientId) &&
                Objects.equal(authUri, that.authUri) &&
                Objects.equal(tokenUri, that.tokenUri) &&
                Objects.equal(authProviderX509CertUrl, that.authProviderX509CertUrl) &&
                Objects.equal(clientX509CertUrl, that.clientX509CertUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, projectId, privateKeyId, privateKey, clientEmail, clientId, authUri, tokenUri, authProviderX509CertUrl, clientX509CertUrl);
    }
}
