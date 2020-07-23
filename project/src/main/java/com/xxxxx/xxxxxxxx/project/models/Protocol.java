/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/9 20:17
 */

package com.xxxxx.xxxxxxxx.project.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Protocol {
    public enum StatusCode {
        // Main design idea:
        //    We only encode for the statuses being clear and stable or requiring some
        //      kinds of interactive reaction from the users.
        //    For the statuses related to detailed implementations, we use vague codes
        //      with detailed messages, people should never depend on those statuses.
        //    We also extend the HTTP codes to fit in more complicating situations.

        // No extension, keep the same as HTTP codes
        OK(200, "Success"),
        Unauthorized(401, "User authentication failed"),
        NoPermission(403, "Permission denied"),
        // HTTP 404
        NotFound(10000, "Destination not exists"),
        BadURI(10001, "Invalid URI"),
        NotSupported(10002, "Not supported function"),
        // HTTP 400
        BadRequest(11000, "Request parameter error"),
        Corrupted(11001, "Request body corrupted"),
        InvalidArgument(11002, "Invalid arguments"),
        NoSummary(11500, "Project termination without any summary document"),
        // HTTP 5xx
        UpstreamError(12000, "Upstream service error"),
        SystemError(13000, "System environment error"),
        RuntimeError(14000, "Runtime error"),
        InternalError(15000, "Server internal error");

        private final int value;
        private final String defaultCause;

        StatusCode(int value, String defaultCause) {
            this.value = value;
            this.defaultCause = defaultCause;
        }

        public int value() {
            return value;
        }
        public String text(String cause) {
            if (cause == null || cause.isEmpty()) {
                cause = defaultCause;
            }
            return String.format("[%s(%d)] %s", this.name(), this.value, cause);
        }
    }

    public static class Request {
        @JsonProperty("id")
        @Getter(AccessLevel.PUBLIC)
        @Setter(AccessLevel.PUBLIC)
        private String id;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        @JsonProperty("code")
        @Getter(AccessLevel.PUBLIC)
        private Integer code;

        @JsonProperty("msg")
        private String msg;

        @JsonProperty("data")
        @Getter(AccessLevel.PUBLIC)
        private ResponseData data;

        public Response() { setStatus(StatusCode.OK); }
        public Response(StatusCode code) { setStatus(code); }
        public Response(StatusCode code, String cause) {
            setStatus(code, cause);
        }

        public Response withBody(ResponseData body) {
            data = body;
            return this;
        }

        @JsonIgnore
        public void setStatus(StatusCode code) {
            setStatus(code, "");
        }

        @JsonIgnore
        public void setStatus(StatusCode code, String cause) {
            this.code = code.value();
            this.msg = code.text(cause);
        }
        public Boolean isStatus(StatusCode code) {
            return code.value() == this.code;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public interface ResponseData {
    }

    // -----------------------------------------------------------------

    public static class SetAttrValueRequest extends Request {
        @JsonProperty("result")
        @Getter(AccessLevel.PUBLIC)
        private List<String> result = new ArrayList<>();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SetAttrValueResponse implements ResponseData {
        @JsonProperty("id")
        @Getter(AccessLevel.PUBLIC)
        @Setter(AccessLevel.PUBLIC)
        private String id;

        @JsonProperty("result")
        @Setter(AccessLevel.PUBLIC)
        private Map<String, Attr> result = new HashMap<>();
    }
}
